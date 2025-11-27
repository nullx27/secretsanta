package grimm.tech.secretsanta.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AdminProperties::class, JwtProperties::class)
class AppProperties

@ConfigurationProperties(prefix = "app.admin")
data class AdminProperties(
    var discordIds: List<String> = emptyList()
)

@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    var secret: String = "local-dev-secret-change-me",
    var expirationMinutes: Long = 480,
    var cookieName: String = "AUTH_TOKEN"
)
