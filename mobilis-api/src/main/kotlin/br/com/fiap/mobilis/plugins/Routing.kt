package br.com.fiap.mobilis.plugins

import br.com.fiap.mobilis.config.DatabaseFactory
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            get("/health") {
                call.respond(
                    HealthResponse(
                        status = "ok",
                        banco = "mobilis",
                        usuarios = DatabaseFactory.contar("usuario"),
                        instituicoes = DatabaseFactory.contar("instituicao"),
                        tipos = DatabaseFactory.contar("tipo_equipamento"),
                        equipamentos = DatabaseFactory.contar("equipamento"),
                    ),
                )
            }
        }
    }
}

@Serializable
data class HealthResponse(
    val status: String,
    val banco: String,
    val usuarios: Long,
    val instituicoes: Long,
    val tipos: Long,
    val equipamentos: Long,
)
