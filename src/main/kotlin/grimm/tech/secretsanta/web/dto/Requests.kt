package grimm.tech.secretsanta.web.dto

import grimm.tech.secretsanta.model.Role
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class ProfileUpdateRequest(
    @field:NotBlank(message = "Display name is required")
    val displayName: String,

    @field:Size(max = 1024, message = "Address is too long")
    val address: String? = null,

    val interests: Set<@NotBlank String> = emptySet()
)

data class SettingsUpdateRequest(
    val spendLimit: BigDecimal? = null,
    val sendDeadline: LocalDate? = null,
    val restrictedGuildId: String? = null
)

data class AdminUserUpdateRequest(
    val displayName: String? = null,
    val address: String? = null,
    val interests: Set<String>? = null,
    val role: Role? = null,
    val guildMember: Boolean? = null
)
