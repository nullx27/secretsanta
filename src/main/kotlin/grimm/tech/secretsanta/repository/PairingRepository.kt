package grimm.tech.secretsanta.repository

import grimm.tech.secretsanta.model.Pairing
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PairingRepository : JpaRepository<Pairing, UUID> {
    fun findByGiverId(giverId: UUID): Pairing?
}
