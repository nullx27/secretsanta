package grimm.tech.secretsanta.repository

import grimm.tech.secretsanta.model.SantaSettings
import org.springframework.data.jpa.repository.JpaRepository

interface SantaSettingsRepository : JpaRepository<SantaSettings, Long>
