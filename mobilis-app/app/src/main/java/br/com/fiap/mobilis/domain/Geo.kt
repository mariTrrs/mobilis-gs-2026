package br.com.fiap.mobilis.domain

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private val conhecidos = mapOf(
        "01310000" to (-23.5614 to -46.6558),
        "01302000" to (-23.5505 to -46.6333),
        "01001000" to (-23.5505 to -46.6333)
    )

    fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val raio = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * raio * asin(sqrt(a))
    }

    fun coordenadaDoCep(cep: String): Pair<Double, Double> {
        val digitos = cep.filter { it.isDigit() }.padEnd(8, '0').take(8)
        conhecidos[digitos]?.let { return it }
        val numero = digitos.toIntOrNull() ?: return -23.5505 to -46.6333
        val latitude = -23.75 + (numero % 5000) / 10_000.0
        val longitude = -46.85 + ((numero / 7) % 5000) / 10_000.0
        return latitude to longitude
    }

    fun formatarKm(km: Double): String = String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", km)
}
