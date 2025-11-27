package grimm.tech.secretsanta.service

import grimm.tech.secretsanta.model.Participant
import grimm.tech.secretsanta.model.Role
import grimm.tech.secretsanta.repository.ParticipantRepository
import grimm.tech.secretsanta.web.dto.AdminUserUpdateRequest
import grimm.tech.secretsanta.web.dto.ProfileUpdateRequest
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ParticipantService(
    private val participantRepository: ParticipantRepository
) {

    @Transactional
    fun getOrCreateParticipant(discordId: String, username: String, displayName: String?, guildMember: Boolean): Participant {
        val existing = participantRepository.findByDiscordId(discordId)
        if (existing != null) {
            existing.discordUsername = username
            existing.displayName = existing.displayName ?: displayName
            existing.guildMember = guildMember
            return existing
        }

        val participant = Participant(
            discordId = discordId,
            discordUsername = username,
            displayName = displayName ?: username,
            guildMember = guildMember,
            interests = mutableSetOf()
        )
        return participantRepository.save(participant)
    }

    fun getById(id: UUID): Participant = participantRepository.findById(id).orElseThrow {
        IllegalArgumentException("Participant $id not found")
    }

    @Transactional
    fun updateProfile(participantId: UUID, request: ProfileUpdateRequest): Participant {
        val participant = getById(participantId)
        participant.displayName = request.displayName
        participant.address = request.address?.ifBlank { null }
        participant.interests = request.interests.filter { it.isNotBlank() }.toMutableSet()
        return participant
    }

    fun listParticipants(): List<Participant> = participantRepository.findAll()

    @Transactional
    fun adminUpdate(participantId: UUID, request: AdminUserUpdateRequest): Participant {
        val participant = getById(participantId)
        request.displayName?.let { participant.displayName = it }
        request.address?.let { participant.address = it.ifBlank { null } }
        request.interests?.let { participant.interests = it.filter { interest -> interest.isNotBlank() }.toMutableSet() }
        request.role?.let { participant.role = it }
        request.guildMember?.let { participant.guildMember = it }
        return participant
    }

    @Transactional
    fun syncAdminRole(participant: Participant, adminDiscordIds: Set<String>): Participant {
        val desiredRole = if (adminDiscordIds.contains(participant.discordId)) Role.ADMIN else Role.USER
        if (participant.role != desiredRole) {
            participant.role = desiredRole
            participantRepository.save(participant)
        }
        return participant
    }
}
