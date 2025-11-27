package grimm.tech.secretsanta.service

import grimm.tech.secretsanta.model.Participant
import grimm.tech.secretsanta.repository.PairingRepository
import grimm.tech.secretsanta.repository.ParticipantRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class PairingServiceTests @Autowired constructor(
    private val pairingService: PairingService,
    private val participantRepository: ParticipantRepository,
    private val pairingRepository: PairingRepository
) {

    @AfterEach
    fun cleanUp() {
        pairingRepository.deleteAll()
        participantRepository.deleteAll()
    }

    @Test
    fun `pairs all participants without self matches`() {
        val participants = (1..4).map { index ->
            participantRepository.save(
                Participant(
                    discordId = "discord-$index",
                    discordUsername = "user$index"
                )
            )
        }

        val pairings = pairingService.createPairings()

        assertThat(pairings).hasSize(participants.size)
        val giverIds = pairings.map { it.giver.id }.toSet()
        val receiverIds = pairings.map { it.receiver.id }.toSet()
        assertThat(giverIds).hasSize(participants.size)
        assertThat(receiverIds).hasSize(participants.size)
        pairings.forEach { pairing ->
            assertThat(pairing.giver.id).isNotEqualTo(pairing.receiver.id)
        }
    }
}
