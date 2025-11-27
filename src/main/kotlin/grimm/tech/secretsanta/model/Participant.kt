package grimm.tech.secretsanta.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "participants")
class Participant(
    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(nullable = false, unique = true)
    var discordId: String,

    @Column(nullable = false)
    var discordUsername: String,

    var displayName: String? = null,

    @Column(length = 1024)
    var address: String? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "participant_interests", joinColumns = [JoinColumn(name = "participant_id")])
    @Column(name = "interest")
    var interests: MutableSet<String> = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    var guildMember: Boolean = false
) {
    // JPA requires a no-arg constructor
    constructor() : this(
        id = null,
        discordId = "",
        discordUsername = "",
        displayName = null,
        address = null,
        interests = mutableSetOf(),
        role = Role.USER,
        guildMember = false
    )
}
