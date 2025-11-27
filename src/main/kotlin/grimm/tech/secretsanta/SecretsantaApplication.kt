package grimm.tech.secretsanta

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SecretsantaApplication

fun main(args: Array<String>) {
    runApplication<SecretsantaApplication>(*args)
}
