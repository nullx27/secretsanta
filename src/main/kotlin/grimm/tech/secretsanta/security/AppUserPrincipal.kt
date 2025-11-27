package grimm.tech.secretsanta.security

import grimm.tech.secretsanta.model.Role
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User
import java.io.Serializable
import java.util.UUID

data class AppUserPrincipal(
    val participantId: UUID,
    val discordId: String,
    val username: String,
    val role: Role,
    private val attributes: Map<String, Any> = emptyMap()
) : OAuth2User, Serializable {

    override fun getName(): String = discordId

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
}
