package grimm.tech.secretsanta.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver

class CookieBearerTokenResolver(private val cookieName: String) : BearerTokenResolver {
    private val delegate = DefaultBearerTokenResolver()

    override fun resolve(request: HttpServletRequest): String? {
        return delegate.resolve(request)
            ?: request.cookies?.firstOrNull { it.name == cookieName }?.value
    }
}
