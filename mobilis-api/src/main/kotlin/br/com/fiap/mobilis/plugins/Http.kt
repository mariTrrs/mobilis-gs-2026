package br.com.fiap.mobilis.plugins

import br.com.fiap.mobilis.comum.ApiException
import br.com.fiap.mobilis.comum.ErroResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.net.URI

fun Application.configureHttp() {
    install(CallLogging) {
        level = Level.INFO
        filter { chamada -> chamada.request.path().startsWith("/api/") }
    }

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            },
        )
    }

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowNonSimpleContentTypes = true
        allowOrigins { origem -> origemLocal(origem) }
    }

    install(StatusPages) {
        exception<ApiException> { chamada, causa ->
            chamada.respond(causa.status, ErroResponse(causa.codigo, causa.mensagem))
        }
        exception<IllegalArgumentException> { chamada, causa ->
            chamada.respond(
                HttpStatusCode.BadRequest,
                ErroResponse("REQUISICAO_INVALIDA", causa.message ?: "Requisicao invalida"),
            )
        }
        exception<Throwable> { chamada, causa ->
            chamada.application.environment.log.error("Falha nao tratada", causa)
            chamada.respond(
                HttpStatusCode.InternalServerError,
                ErroResponse("ERRO_INTERNO", "Nao foi possivel concluir a operacao."),
            )
        }
    }
}

private fun origemLocal(origem: String): Boolean {
    val uri = runCatching { URI(origem) }.getOrNull() ?: return false
    val host = uri.host ?: return false
    return uri.scheme == "http" && (host == "localhost" || host == "127.0.0.1")
}
