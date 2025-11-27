package grimm.tech.secretsanta.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "pairings")
class Pairing(
    @Id
    @GeneratedValue
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "giver_id", nullable = false)
    var giver: Participant,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    var receiver: Participant,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
) {
    // JPA requires a no-arg constructor
    constructor() : this(
        id = null,
        giver = Participant(),
        receiver = Participant(),
        createdAt = Instant.now()
    )
}
