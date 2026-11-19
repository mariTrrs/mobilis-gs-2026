package br.com.fiap.mobilis.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private val tabelasDeSaude = setOf("usuario", "instituicao", "tipo_equipamento", "equipamento")

    lateinit var dataSource: HikariDataSource
        private set

    fun iniciar(ambiente: Ambiente) {
        val hikari = HikariConfig().apply {
            jdbcUrl = ambiente.jdbcUrl
            username = ambiente.dbUsuario
            password = ambiente.dbSenha
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = ambiente.poolMaximo
            poolName = "mobilis-pool"
            validate()
        }
        dataSource = HikariDataSource(hikari)
        migrar(ambiente)
        Database.connect(dataSource)
        log.info("PostgreSQL conectado e migracoes aplicadas")
    }

    fun contar(tabela: String): Long {
        require(tabela in tabelasDeSaude) { "Tabela fora da consulta de saude" }
        dataSource.connection.use { conexao ->
            conexao.createStatement().use { comando ->
                comando.executeQuery("SELECT COUNT(*) FROM $tabela").use { resultado ->
                    resultado.next()
                    return resultado.getLong(1)
                }
            }
        }
    }

    fun fechar() {
        if (::dataSource.isInitialized && !dataSource.isClosed) {
            dataSource.close()
        }
    }

    private fun migrar(ambiente: Ambiente) {
        val resultado = Flyway.configure()
            .dataSource(ambiente.jdbcUrl, ambiente.dbUsuario, ambiente.dbSenha)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        log.info("Flyway executou {} migracao(oes)", resultado.migrationsExecuted)
    }
}
