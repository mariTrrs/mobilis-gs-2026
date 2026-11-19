package br.com.fiap.mobilis.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager

object Localizacao {
    @SuppressLint("MissingPermission")
    fun ultima(context: Context): Pair<Double, Double>? {
        val gerenciador = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        val melhor = provedores.mapNotNull { provedor ->
            runCatching { gerenciador.getLastKnownLocation(provedor) }.getOrNull()
        }.minByOrNull { it.accuracy }
        return melhor?.let { it.latitude to it.longitude }
    }
}
