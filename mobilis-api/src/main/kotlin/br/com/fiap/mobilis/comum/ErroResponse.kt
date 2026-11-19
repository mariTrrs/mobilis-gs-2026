package br.com.fiap.mobilis.comum

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
data class ErroResponse(
    val codigo: String,
    val mensagem: String,
)

class ApiException(
    val status: HttpStatusCode,
    val codigo: String,
    val mensagem: String,
) : RuntimeException(mensagem)

fun erro(
    status: HttpStatusCode,
    codigo: String,
    mensagem: String,
): Nothing = throw ApiException(status, codigo, mensagem)
