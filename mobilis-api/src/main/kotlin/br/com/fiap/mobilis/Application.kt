package br.com.fiap.mobilis

import br.com.fiap.mobilis.config.Ambiente
import br.com.fiap.mobilis.config.DatabaseFactory
import br.com.fiap.mobilis.plugins.configureHttp
import br.com.fiap.mobilis.plugins.configureRouting
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val ambiente = Ambiente.carregar()
    DatabaseFactory.iniciar(ambiente)
    Runtime.getRuntime().addShutdownHook(Thread { DatabaseFactory.fechar() })

    embeddedServer(Netty, port = ambiente.porta, host = "0.0.0.0") {
        configureHttp()
        configureRouting()
    }.start(wait = true)
}
