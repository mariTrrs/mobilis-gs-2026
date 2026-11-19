package br.com.fiap.mobilis.data

import android.content.Context
import android.content.SharedPreferences

class TokenStore(context: Context) {
    private val preferencias: SharedPreferences =
        context.getSharedPreferences("mobilis_sessao_local", Context.MODE_PRIVATE)
    private val chave = "jwt"

    fun salvar(token: String) {
        preferencias.edit().putString(chave, token).apply()
    }

    fun ler(): String? = preferencias.getString(chave, null)

    fun limpar() {
        preferencias.edit().remove(chave).apply()
    }
}
