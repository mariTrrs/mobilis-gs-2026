package br.com.fiap.mobilis.data

import android.content.Context
import br.com.fiap.mobilis.domain.EstadoInicial
import br.com.fiap.mobilis.domain.MobilisState
import kotlinx.serialization.json.Json
import java.io.File

class LocalStore(context: Context) {
    private val arquivo = File(context.filesDir, "mobilis-estado.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun carregar(): MobilisState {
        if (!arquivo.exists()) {
            val inicial = EstadoInicial.criar()
            salvar(inicial)
            return inicial
        }
        return try {
            val estado = json.decodeFromString<MobilisState>(arquivo.readText())
            val migrado = when {
                estado.versao < 2 || estado.usuarios.isEmpty() -> EstadoInicial.criar()
                else -> EstadoInicial.restaurarSenhaAdmin(estado)
            }
            if (migrado != estado) salvar(migrado)
            migrado
        } catch (_: Exception) {
            EstadoInicial.criar().also { salvar(it) }
        }
    }

    fun salvar(estado: MobilisState) {
        arquivo.writeText(json.encodeToString(MobilisState.serializer(), estado))
    }
}
