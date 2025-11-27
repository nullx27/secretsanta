package grimm.tech.secretsanta.repository

import grimm.tech.secretsanta.model.Participant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ParticipantRepository : JpaRepository<Participant, UUID> {
    fun findByDiscordId(discordId: String): Participant?
}
