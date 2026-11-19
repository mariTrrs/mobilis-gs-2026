package br.com.fiap.mobilis.domain

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Seguranca {
    fun hashSenha(senha: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("mobilis-v1:$senha".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun codigoSeis(): String = (100_000 + kotlin.random.Random.nextInt(900_000)).toString()
}

object Base64Url {
    private val tabela = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray()

    fun encode(dados: ByteArray): String {
        val saida = StringBuilder()
        var indice = 0
        while (indice < dados.size) {
            val b0 = dados[indice].toInt() and 0xFF
            val b1 = if (indice + 1 < dados.size) dados[indice + 1].toInt() and 0xFF else 0
            val b2 = if (indice + 2 < dados.size) dados[indice + 2].toInt() and 0xFF else 0
            saida.append(tabela[b0 shr 2])
            saida.append(tabela[((b0 and 0x03) shl 4) or (b1 shr 4)])
            if (indice + 1 < dados.size) saida.append(tabela[((b1 and 0x0F) shl 2) or (b2 shr 6)])
            if (indice + 2 < dados.size) saida.append(tabela[b2 and 0x3F])
            indice += 3
        }
        return saida.toString()
    }

    fun decode(texto: String): ByteArray {
        val mapa = IntArray(128) { -1 }
        tabela.forEachIndexed { indice, caractere -> mapa[caractere.code] = indice }
        val saida = ArrayList<Byte>()
        var indice = 0
        while (indice < texto.length) {
            val c0 = mapa.getOrElse(texto[indice].code) { 0 }
            val c1 = if (indice + 1 < texto.length) mapa.getOrElse(texto[indice + 1].code) { 0 } else 0
            val c2 = if (indice + 2 < texto.length) mapa.getOrElse(texto[indice + 2].code) { 0 } else 0
            val c3 = if (indice + 3 < texto.length) mapa.getOrElse(texto[indice + 3].code) { 0 } else 0
            saida.add(((c0 shl 2) or (c1 shr 4)).toByte())
            if (indice + 2 < texto.length) saida.add((((c1 and 0x0F) shl 4) or (c2 shr 2)).toByte())
            if (indice + 3 < texto.length) saida.add((((c2 and 0x03) shl 6) or c3).toByte())
            indice += 4
        }
        return saida.toByteArray()
    }
}

object Tokens {
    private const val SEGREDO = "mobilis-frontend-local"
    private const val SETE_DIAS = 7L * 24 * 60 * 60 * 1000

    fun emitir(usuarioId: Int, agora: Long = System.currentTimeMillis()): String {
        val header = Base64Url.encode("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val expira = agora + SETE_DIAS
        val payload = Base64Url.encode("""{"sub":"$usuarioId","exp":$expira}""".toByteArray())
        val assinatura = assinar("$header.$payload")
        return "$header.$payload.$assinatura"
    }

    fun usuarioId(token: String, agora: Long = System.currentTimeMillis()): Int? {
        val partes = token.split(".")
        if (partes.size != 3) return null
        if (assinar("${partes[0]}.${partes[1]}") != partes[2]) return null
        val json = Base64Url.decode(partes[1]).toString(Charsets.UTF_8)
        val id = Regex(""""sub":"(\d+)"""").find(json)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val expira = Regex(""""exp":(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        return if (expira < agora) null else id
    }

    private fun assinar(conteudo: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SEGREDO.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64Url.encode(mac.doFinal(conteudo.toByteArray(Charsets.UTF_8)))
    }
}
