package grimm.tech.secretsanta.config

import grimm.tech.secretsanta.security.CookieBearerTokenResolver
import grimm.tech.secretsanta.security.DiscordOAuth2UserService
import grimm.tech.secretsanta.security.JwtAuthenticationConverter
import grimm.tech.secretsanta.security.JwtService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import grimm.tech.secretsanta.config.JwtProperties
import com.nimbusds.jose.jwk.source.ImmutableSecret
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets
import java.time.Duration
import com.nimbusds.jose.proc.SecurityContext
import java.security.MessageDigest

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val discordOAuth2UserService: DiscordOAuth2UserService,
    private val jwtAuthenticationConverter: JwtAuthenticationConverter
) {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        jwtDecoder: JwtDecoder
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/static/**",
                        "/app.js",
                        "/js/**",
                        "/css/**",
                        "/login/success",
                        "/actuator/health",
                        "/error",
                        "/api/auth/**",
                        "/oauth2/**"
                    ).permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth ->
                oauth.userInfoEndpoint { it.userService(discordOAuth2UserService) }
                oauth.successHandler(oauth2SuccessHandler(jwtService, jwtProperties))
            }
            .logout { logout ->
                logout.logoutUrl("/api/auth/logout")
                    .addLogoutHandler { _, response, _ ->
                        val cleared = ResponseCookie.from(jwtService.cookieName(), "")
                            .httpOnly(true)
                            .path("/")
                            .maxAge(0)
                            .sameSite("Lax")
                            .build()
                        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString())
                    }
                    .logoutSuccessHandler { _, response, _ -> response.status = 200 }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .oauth2ResourceServer { oauth2 ->
                oauth2
                    .bearerTokenResolver(CookieBearerTokenResolver(jwtService.cookieName()))
                    .jwt { jwt ->
                        jwt.decoder(jwtDecoder)
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                    }
            }
            .csrf { it.disable() }

        return http.build()
    }

    private fun oauth2SuccessHandler(jwtService: JwtService, jwtProperties: JwtProperties): AuthenticationSuccessHandler = AuthenticationSuccessHandler { _, response, authentication ->
        val principal = authentication.principal as grimm.tech.secretsanta.security.AppUserPrincipal
        val token = jwtService.generateToken(principal)

        val cookie = ResponseCookie.from(jwtService.cookieName(), token)
            .httpOnly(true)
            .path("/")
            .maxAge(Duration.ofMinutes(jwtProperties.expirationMinutes))
            .sameSite("Lax")
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
        response.sendRedirect("/")
    }

    @Bean
    fun jwtDecoder(jwtProperties: JwtProperties): JwtDecoder {
        val secretKey = deriveKey(jwtProperties.secret)
        val validator: OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> =
            DelegatingOAuth2TokenValidator(JwtTimestampValidator())
        return NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
            .also { (it as NimbusJwtDecoder).setJwtValidator(validator) }
    }

    @Bean
    fun jwtEncoder(jwtProperties: JwtProperties): JwtEncoder {
        val secretKey = deriveKey(jwtProperties.secret)
        return NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey))
    }

    private fun deriveKey(secret: String): SecretKeySpec {
        // Ensure a 256-bit key regardless of input length
        val digest = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(digest, "HmacSHA256")
    }
}
