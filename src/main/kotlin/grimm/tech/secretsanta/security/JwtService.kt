package grimm.tech.secretsanta.security

import grimm.tech.secretsanta.config.JwtProperties
import grimm.tech.secretsanta.model.Role
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Component
class JwtService(
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: JwtDecoder,
    private val jwtProperties: JwtProperties
) {

    fun generateToken(principal: AppUserPrincipal): String {
        val now = Instant.now()
        val expiresAt = now.plus(jwtProperties.expirationMinutes, ChronoUnit.MINUTES)

        val claims = JwtClaimsSet.builder()
            .issuer("secret-santa")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(principal.discordId)
            .claim("pid", principal.participantId.toString())
            .claim("username", principal.username)
            .claim("role", principal.role.name)
            .build()

        val headers = org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build()

        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
    }

    fun decode(token: String) = jwtDecoder.decode(token)

    fun cookieName(): String = jwtProperties.cookieName

    fun expirationSeconds(): Long = jwtProperties.expirationMinutes * 60
}
