package br.com.fiap.mobilis.config

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

data class Ambiente(
    val porta: Int,
    val jdbcUrl: String,
    val dbUsuario: String,
    val dbSenha: String,
    val poolMaximo: Int,
    val jwtSecret: String,
    val jwtEmissor: String,
    val jwtExpiracaoHoras: Long,
) {
    companion object {
        fun carregar(): Ambiente {
            carregarArquivoEnv()
            val segredo = obrigatorio("JWT_SECRET")
            require(segredo.length >= 32) {
                "JWT_SECRET precisa ter pelo menos 32 caracteres"
            }
            return Ambiente(
                porta = inteiro("SERVER_PORT", 8081),
                jdbcUrl = obrigatorio("DB_JDBC_URL"),
                dbUsuario = obrigatorio("DB_USER"),
                dbSenha = obrigatorio("DB_PASSWORD"),
                poolMaximo = inteiro("DB_POOL_MAX", 10),
                jwtSecret = segredo,
                jwtEmissor = texto("JWT_ISSUER", "mobilis"),
                jwtExpiracaoHoras = inteiro("JWT_EXPIRATION_HOURS", 8).toLong(),
            )
        }

        private fun carregarArquivoEnv() {
            listOf(Path.of(".env"), Path.of("mobilis-api", ".env"))
                .filter { it.exists() && it.isRegularFile() }
                .forEach { arquivo ->
                    Files.readAllLines(arquivo).forEach { linha -> aplicarLinha(linha) }
                }
        }

        private fun aplicarLinha(linha: String) {
            val texto = linha.trim()
            if (texto.isEmpty() || texto.startsWith("#")) return
            val separador = texto.indexOf('=')
            if (separador <= 0) return
            val chave = texto.substring(0, separador).trim()
            var valor = texto.substring(separador + 1).trim()
            if (valor.length >= 2 && valor.first() == valor.last() && valor.first() in setOf('"', '\'')) {
                valor = valor.substring(1, valor.length - 1)
            }
            if (System.getenv(chave) == null && System.getProperty(chave) == null) {
                System.setProperty(chave, valor)
            }
        }

        private fun ler(chave: String): String? = System.getenv(chave) ?: System.getProperty(chave)

        private fun obrigatorio(chave: String): String =
            ler(chave)?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Variavel de ambiente obrigatoria ausente: $chave")

        private fun texto(chave: String, padrao: String): String =
            ler(chave)?.takeIf { it.isNotBlank() } ?: padrao

        private fun inteiro(chave: String, padrao: Int): Int =
            ler(chave)?.toIntOrNull() ?: padrao
    }
}
