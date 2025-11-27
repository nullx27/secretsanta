package grimm.tech.secretsanta.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "santa_settings")
class SantaSettings(
    @Id
    var id: Long = SINGLETON_ID,

    @Column(precision = 10, scale = 2)
    var spendLimit: BigDecimal? = null,

    var sendDeadline: LocalDate? = null,

    var restrictedGuildId: String? = null
) {
    companion object {
        const val SINGLETON_ID = 1L
    }
}
