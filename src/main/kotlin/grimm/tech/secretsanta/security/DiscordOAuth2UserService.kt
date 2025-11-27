package grimm.tech.secretsanta.security

import grimm.tech.secretsanta.config.AdminProperties
import grimm.tech.secretsanta.model.Participant
import grimm.tech.secretsanta.service.ParticipantService
import grimm.tech.secretsanta.service.SettingsService
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class DiscordOAuth2UserService(
    private val participantService: ParticipantService,
    private val settingsService: SettingsService,
    private val adminProperties: AdminProperties,
    webClientBuilder: WebClient.Builder
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private val delegate = DefaultOAuth2UserService()
    private val webClient: WebClient = webClientBuilder.baseUrl("https://discord.com/api").build()

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauthUser = delegate.loadUser(userRequest)
        val discordId = oauthUser.attributes["id"]?.toString()
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_user"), "Discord id missing")
        val username = oauthUser.attributes["username"]?.toString() ?: "discord-user"
        val globalName = oauthUser.attributes["global_name"]?.toString()

        val settings = settingsService.getSettings()
        val isGuildMember = isMemberOfRestrictedGuild(userRequest, settings.restrictedGuildId)
        if (!isGuildMember && settings.restrictedGuildId != null) {
            throw OAuth2AuthenticationException(
                OAuth2Error("access_denied"),
                "You must be a member of guild ${settings.restrictedGuildId}"
            )
        }

        var participant: Participant = participantService.getOrCreateParticipant(
            discordId = discordId,
            username = username,
            displayName = globalName ?: username,
            guildMember = isGuildMember
        )

        participant = participantService.syncAdminRole(participant, adminDiscordIds())

        val id = participant.id
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_user"), "Failed to persist participant")

        return AppUserPrincipal(
            participantId = id,
            discordId = discordId,
            username = username,
            role = participant.role,
            attributes = oauthUser.attributes
        )
    }

    private fun isMemberOfRestrictedGuild(userRequest: OAuth2UserRequest, restrictedGuildId: String?): Boolean {
        val guildId = restrictedGuildId ?: return true
        return try {
            val response: List<Map<String, Any>>? = webClient.get()
                .uri("/users/@me/guilds")
                .headers { it.setBearerAuth(userRequest.accessToken.tokenValue) }
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<Map<String, Any>>>() {})
                .timeout(Duration.ofSeconds(5))
                .onErrorResume { Mono.empty() }
                .block()

            response?.any { guild -> guild["id"]?.toString() == guildId } ?: false
        } catch (ex: Exception) {
            logger.warn("Failed to verify guild membership", ex)
            false
        }
    }

    private fun adminDiscordIds(): Set<String> =
        adminProperties.discordIds
            .flatMap { it.split(",") }
            .mapNotNull { it.trim().takeIf { id -> id.isNotEmpty() } }
            .toSet()

    companion object {
        private val logger = LoggerFactory.getLogger(DiscordOAuth2UserService::class.java)
    }
}
