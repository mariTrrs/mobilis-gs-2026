package br.com.fiap.mobilis.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Datas {
    private val iso = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    fun hoje(): String = iso.get()!!.format(Date())

    fun maisDias(dias: Int, base: String? = null): String {
        val calendario = Calendar.getInstance()
        if (!base.isNullOrBlank()) {
            calendario.time = iso.get()!!.parse(base) ?: Date()
        }
        calendario.add(Calendar.DAY_OF_YEAR, dias)
        return iso.get()!!.format(calendario.time)
    }

    fun exibir(isoData: String?): String {
        if (isoData.isNullOrBlank()) return "Não informado"
        val partes = isoData.split("-")
        if (partes.size != 3) return isoData
        return "${partes[2]}/${partes[1]}/${partes[0]}"
    }

    fun exibirEpoch(epoch: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(epoch))

    fun interpretar(valor: String): String? {
        val partes = valor.trim().split("/")
        if (partes.size != 3) return null
        if (partes[0].length != 2 || partes[1].length != 2 || partes[2].length != 4) return null
        val dia = partes[0].toIntOrNull() ?: return null
        val mes = partes[1].toIntOrNull() ?: return null
        if (dia !in 1..31 || mes !in 1..12) return null
        return "${partes[2]}-${partes[1]}-${partes[0]}"
    }

    fun vencida(isoData: String?): Boolean = !isoData.isNullOrBlank() && isoData < hoje()

    fun venceEmAte(isoData: String?, dias: Int): Boolean {
        if (isoData.isNullOrBlank() || vencida(isoData)) return false
        return isoData <= maisDias(dias)
    }

    fun diasDesde(epoch: Long): Int =
        ((System.currentTimeMillis() - epoch) / 86_400_000L).toInt().coerceAtLeast(0)

    fun diasDeAtraso(isoData: String?): Int {
        val dataIso = isoData ?: return 0
        if (!vencida(dataIso)) return 0
        val formato = iso.get()!!
        val data = formato.parse(dataIso) ?: return 0
        val hojeData = formato.parse(hoje()) ?: return 0
        return ((hojeData.time - data.time) / 86_400_000L).toInt().coerceAtLeast(0)
    }
}
