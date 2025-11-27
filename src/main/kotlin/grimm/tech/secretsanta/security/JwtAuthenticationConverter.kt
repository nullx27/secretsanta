package grimm.tech.secretsanta.security

import grimm.tech.secretsanta.model.Role
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JwtAuthenticationConverter : Converter<Jwt, UsernamePasswordAuthenticationToken> {
    override fun convert(jwt: Jwt): UsernamePasswordAuthenticationToken {
        val role = jwt.getClaimAsString("role")?.let { Role.valueOf(it) } ?: Role.USER
        val principal = AppUserPrincipal(
            participantId = UUID.fromString(jwt.getClaimAsString("pid")),
            discordId = jwt.subject,
            username = jwt.getClaimAsString("username"),
            role = role,
            attributes = emptyMap()
        )
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
        return UsernamePasswordAuthenticationToken(principal, "n/a", authorities)
    }
}
