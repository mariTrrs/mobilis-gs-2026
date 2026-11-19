package br.com.fiap.mobilis.domain

object Validacao {
    fun digitos(valor: String): String = valor.filter { it.isDigit() }

    fun emailNormalizado(valor: String): String = valor.trim().lowercase()

    fun email(valor: String): String? {
        val texto = valor.trim()
        if (texto.isEmpty()) return "Informe o e-mail."
        val ok = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(texto)
        return if (ok) null else "E-mail em formato inválido."
    }

    fun senha(valor: String): String? =
        if (valor.length < 6) "A senha precisa ter pelo menos 6 caracteres." else null

    fun nome(valor: String, rotulo: String = "o nome"): String? =
        if (valor.trim().length < 3) "Informe $rotulo." else null

    fun cpf(valor: String): String? {
        val digitos = digitos(valor)
        if (digitos.length != 11) return "CPF deve ter 11 dígitos."
        if (digitos.all { it == digitos[0] }) return "CPF inválido."
        val primeiro = digitoCpf(digitos.substring(0, 9), 10)
        val segundo = digitoCpf(digitos.substring(0, 10), 11)
        val valido = digitos[9].digitToInt() == primeiro && digitos[10].digitToInt() == segundo
        return if (valido) null else "CPF inválido."
    }

    fun cnpj(valor: String): String? {
        val digitos = digitos(valor)
        if (digitos.length != 14) return "CNPJ deve ter 14 dígitos."
        if (digitos.all { it == digitos[0] }) return "CNPJ inválido."
        val primeiro = digitoCnpj(digitos.substring(0, 12))
        val segundo = digitoCnpj(digitos.substring(0, 13))
        val valido = digitos[12].digitToInt() == primeiro && digitos[13].digitToInt() == segundo
        return if (valido) null else "CNPJ inválido."
    }

    fun cep(valor: String): String? =
        if (digitos(valor).length == 8) null else "CEP deve ter 8 dígitos."

    fun telefone(valor: String): String? {
        val tamanho = digitos(valor).length
        return if (tamanho in 10..11) null else "Telefone deve ter DDD e número."
    }

    fun obrigatorio(valor: String, rotulo: String): String? =
        if (valor.trim().isEmpty()) "Informe $rotulo." else null

    fun motivo(valor: String, rotulo: String = "o motivo"): String? =
        if (valor.trim().length < 3) "Informe $rotulo." else null

    fun formatarCpf(valor: String): String {
        val digitos = digitos(valor)
        if (digitos.length != 11) return valor
        return "${digitos.substring(0, 3)}.${digitos.substring(3, 6)}.${digitos.substring(6, 9)}-${digitos.substring(9)}"
    }

    fun formatarCnpj(valor: String): String {
        val digitos = digitos(valor)
        if (digitos.length != 14) return valor
        return "${digitos.substring(0, 2)}.${digitos.substring(2, 5)}.${digitos.substring(5, 8)}/${digitos.substring(8, 12)}-${digitos.substring(12)}"
    }

    fun formatarCep(valor: String): String {
        val digitos = digitos(valor)
        if (digitos.length != 8) return valor
        return "${digitos.substring(0, 5)}-${digitos.substring(5)}"
    }

    fun formatarTelefone(valor: String): String {
        val digitos = digitos(valor)
        return when (digitos.length) {
            11 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 7)}-${digitos.substring(7)}"
            10 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 6)}-${digitos.substring(6)}"
            else -> valor
        }
    }

    private fun digitoCpf(base: String, pesoInicial: Int): Int {
        val soma = base.mapIndexed { indice, caractere -> caractere.digitToInt() * (pesoInicial - indice) }.sum()
        val resto = soma % 11
        return if (resto < 2) 0 else 11 - resto
    }

    private fun digitoCnpj(base: String): Int {
        val pesos = if (base.length == 12) {
            intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        } else {
            intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        }
        val soma = base.mapIndexed { indice, caractere -> caractere.digitToInt() * pesos[indice] }.sum()
        val resto = soma % 11
        return if (resto < 2) 0 else 11 - resto
    }
}
