package grimm.tech.secretsanta.web

import grimm.tech.secretsanta.model.Participant
import grimm.tech.secretsanta.model.SantaSettings
import grimm.tech.secretsanta.service.PairingService
import grimm.tech.secretsanta.service.ParticipantService
import grimm.tech.secretsanta.service.SettingsService
import grimm.tech.secretsanta.web.dto.AdminUserResponse
import grimm.tech.secretsanta.web.dto.AdminUserUpdateRequest
import grimm.tech.secretsanta.web.dto.PairingResponse
import grimm.tech.secretsanta.web.dto.SettingsResponse
import grimm.tech.secretsanta.web.dto.SettingsUpdateRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val participantService: ParticipantService,
    private val pairingService: PairingService,
    private val settingsService: SettingsService
) {

    @GetMapping("/users")
    fun listUsers(): List<AdminUserResponse> =
        participantService.listParticipants().map { it.toAdminResponse() }

    @PutMapping("/users/{id}")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AdminUserUpdateRequest
    ): AdminUserResponse = participantService.adminUpdate(id, request).toAdminResponse()

    @GetMapping("/settings")
    fun getSettings(): SettingsResponse = settingsService.getSettings().toResponse()

    @PutMapping("/settings")
    fun updateSettings(@RequestBody request: SettingsUpdateRequest): SettingsResponse =
        settingsService.updateSettings(request).toResponse()

    @PostMapping("/pairings")
    fun createPairings(): List<PairingResponse> {
        val participantCount = participantService.listParticipants().size
        if (participantCount < 2) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 2 participants to pair")
        }
        return pairingService.createPairings().map { it.toResponse() }
    }

    @GetMapping("/pairings")
    fun listPairings(): List<PairingResponse> = pairingService.listPairings().map { it.toResponse() }

    private fun Participant.toAdminResponse() = AdminUserResponse(
        id = id!!,
        discordId = discordId,
        discordUsername = discordUsername,
        displayName = displayName,
        address = address,
        interests = interests,
        role = role,
        guildMember = guildMember
    )

    private fun SantaSettings.toResponse() = SettingsResponse(
        spendLimit = spendLimit,
        sendDeadline = sendDeadline,
        restrictedGuildId = restrictedGuildId
    )

    private fun grimm.tech.secretsanta.model.Pairing.toResponse() = PairingResponse(
        id = id!!,
        giverId = giver.id!!,
        receiverId = receiver.id!!,
        createdAt = createdAt
    )
}
