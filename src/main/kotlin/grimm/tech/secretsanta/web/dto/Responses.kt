package grimm.tech.secretsanta.web.dto

import grimm.tech.secretsanta.model.Role
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ProfileResponse(
    val id: UUID,
    val discordUsername: String,
    val displayName: String?,
    val address: String?,
    val interests: Set<String>,
    val role: Role,
    val match: MatchInfo?,
    val settings: SettingsResponse
)

data class MatchInfo(
    val name: String?,
    val address: String?,
    val interests: Set<String>
)

data class SettingsResponse(
    val spendLimit: BigDecimal?,
    val sendDeadline: LocalDate?,
    val restrictedGuildId: String?
)

data class PairingResponse(
    val id: UUID,
    val giverId: UUID,
    val receiverId: UUID,
    val createdAt: Instant
)

data class AdminUserResponse(
    val id: UUID,
    val discordId: String,
    val discordUsername: String,
    val displayName: String?,
    val address: String?,
    val interests: Set<String>,
    val role: Role,
    val guildMember: Boolean
)
