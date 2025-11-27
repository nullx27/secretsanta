package grimm.tech.secretsanta.service

import grimm.tech.secretsanta.model.Pairing
import grimm.tech.secretsanta.repository.PairingRepository
import grimm.tech.secretsanta.repository.ParticipantRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PairingService(
    private val participantRepository: ParticipantRepository,
    private val pairingRepository: PairingRepository
) {

    @Transactional
    fun createPairings(): List<Pairing> {
        val participants = participantRepository.findAll()
        if (participants.size < 2) {
            throw IllegalStateException("Need at least 2 participants to pair")
        }

        val shuffled = participants.shuffled()
        val rotated = shuffled.drop(1) + shuffled.first()
        val newPairings = shuffled.zip(rotated).map { (giver, receiver) ->
            Pairing(giver = giver, receiver = receiver)
        }

        pairingRepository.deleteAll()
        return pairingRepository.saveAll(newPairings)
    }

    fun findMatchFor(participantId: UUID): Pairing? = pairingRepository.findByGiverId(participantId)

    fun listPairings(): List<Pairing> = pairingRepository.findAll()
}
