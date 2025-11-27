package grimm.tech.secretsanta.service

import grimm.tech.secretsanta.model.SantaSettings
import grimm.tech.secretsanta.repository.SantaSettingsRepository
import grimm.tech.secretsanta.web.dto.SettingsUpdateRequest
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class SettingsService(
    private val settingsRepository: SantaSettingsRepository
) {

    fun getSettings(): SantaSettings =
        settingsRepository.findById(SantaSettings.SINGLETON_ID).orElseGet {
            settingsRepository.save(SantaSettings())
        }

    @Transactional
    fun updateSettings(request: SettingsUpdateRequest): SantaSettings {
        val existing = getSettings()
        existing.spendLimit = request.spendLimit
        existing.sendDeadline = request.sendDeadline
        existing.restrictedGuildId = request.restrictedGuildId?.takeIf { it.isNotBlank() }
        return settingsRepository.save(existing)
    }
}
