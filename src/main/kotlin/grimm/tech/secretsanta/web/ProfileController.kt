package grimm.tech.secretsanta.web

import grimm.tech.secretsanta.model.Participant
import grimm.tech.secretsanta.security.AppUserPrincipal
import grimm.tech.secretsanta.service.PairingService
import grimm.tech.secretsanta.service.ParticipantService
import grimm.tech.secretsanta.service.SettingsService
import grimm.tech.secretsanta.web.dto.MatchInfo
import grimm.tech.secretsanta.web.dto.ProfileResponse
import grimm.tech.secretsanta.web.dto.ProfileUpdateRequest
import grimm.tech.secretsanta.web.dto.SettingsResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ProfileController(
    private val participantService: ParticipantService,
    private val pairingService: PairingService,
    private val settingsService: SettingsService
) {

    @GetMapping("/me")
    fun currentUser(@AuthenticationPrincipal principal: AppUserPrincipal): ProfileResponse {
        val participant = participantService.getById(principal.participantId)
        val settings = settingsService.getSettings()
        val pairing = pairingService.findMatchFor(participant.id!!)
        val matchParticipant = pairing?.receiver
        return participant.toProfileResponse(matchParticipant, settings)
    }

    @PutMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: ProfileUpdateRequest
    ): ProfileResponse {
        val updated = participantService.updateProfile(principal.participantId, request)
        val settings = settingsService.getSettings()
        val pairing = pairingService.findMatchFor(updated.id!!)
        val matchParticipant = pairing?.receiver
        return updated.toProfileResponse(matchParticipant, settings)
    }

    private fun Participant.toProfileResponse(match: Participant?, settings: grimm.tech.secretsanta.model.SantaSettings) =
        ProfileResponse(
            id = id!!,
            discordUsername = discordUsername,
            displayName = displayName,
            address = address,
            interests = interests,
            role = role,
            match = match?.let {
                MatchInfo(
                    name = it.displayName,
                    address = it.address,
                    interests = it.interests
                )
            },
            settings = SettingsResponse(
                spendLimit = settings.spendLimit,
                sendDeadline = settings.sendDeadline,
                restrictedGuildId = settings.restrictedGuildId
            )
        )
}
