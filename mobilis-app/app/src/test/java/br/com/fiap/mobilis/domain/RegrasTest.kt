package br.com.fiap.mobilis.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegrasTest {
    @Test
    fun validaCpfCnpjEmailECep() {
        assertNull(Validacao.cpf("52998224725"))
        assertEquals("CPF inválido.", Validacao.cpf("11111111111"))
        assertNull(Validacao.cnpj("11222333000181"))
        assertEquals("CNPJ inválido.", Validacao.cnpj("11222333000180"))
        assertNull(Validacao.email("ana.costa@mobilis.app"))
        assertEquals("CEP deve ter 8 dígitos.", Validacao.cep("123"))
    }

    @Test
    fun tokenSobreviveAssinaturaERecusaAlteracao() {
        val token = Tokens.emitir(7, agora = 1_000)
        assertEquals(7, Tokens.usuarioId(token, agora = 1_000))
        val partes = token.split(".").toMutableList()
        partes[2] = "alterado"
        assertNull(Tokens.usuarioId(partes.joinToString("."), agora = 1_000))
    }

    @Test
    fun politicaDoItemPrevaleceSobreOTipo() {
        val estado = EstadoInicial.criar()
        val cadeira = estado.equipamentos.first { it.codigo == "EQ-0001" }
        val politica = Consultas.politicaEfetiva(estado, cadeira)
        assertTrue(politica.itemPrevalece)
        assertEquals(listOf(Modalidade.EMPRESTIMO), politica.modalidades)
    }

    @Test
    fun baixaDeItemEmprestadoEBloqueada() {
        val estado = EstadoInicial.criar()
        val emprestado = estado.equipamentos.first { it.status == StatusEquipamento.EMPRESTADO }
        val erro = runCatching { Regras.baixar(estado, 4, emprestado.id, "quebra") }.exceptionOrNull()
        assertEquals("Não é possível dar baixa em um item emprestado.", erro?.message)
    }

    @Test
    fun unicoGestorNaoPodeSeDesativar() {
        val estado = EstadoInicial.criar()
        val erro = runCatching { Regras.desativarMembro(estado, 3, 3) }.exceptionOrNull()
        assertEquals("O único gestor não pode ser desativado.", erro?.message)
    }

    @Test
    fun recusaSemMotivoNaoSegue() {
        val estado = EstadoInicial.criar()
        val erro = runCatching { Regras.recusarPedido(estado, 4, 4, " ") }.exceptionOrNull()
        assertEquals("Informe o motivo da recusa.", erro?.message)
    }

    @Test
    fun cancelarDepoisDaRetiradaEBloqueado() {
        val estado = EstadoInicial.criar()
        val erro = runCatching { Regras.cancelarPedido(estado, 1, 1) }.exceptionOrNull()
        assertEquals("Não é possível cancelar depois da retirada.", erro?.message)
    }

    @Test
    fun buscaOrdenaPorDistanciaERespeitaModalidade() {
        val estado = EstadoInicial.criar()
        val origem = Geo.coordenadaDoCep("01310000")
        val pesquisa = Consultas.buscar(estado, 1, null, Modalidade.DOACAO, 25.0, origem.first, origem.second)
        val distancias = pesquisa.itens.map { it.distanciaKm }
        assertEquals(distancias.sorted(), distancias)
        assertTrue(pesquisa.itens.none { it.equipamento.codigo == "EQ-0001" })
    }

    @Test
    fun tipoDuplicadoENomeVazioFalham() {
        val estado = EstadoInicial.criar()
        assertEquals("Informe o nome do tipo.", runCatching { Regras.salvarTipo(estado, 7, 0, "  ", "", "") }.exceptionOrNull()?.message)
        assertEquals("Já existe um tipo com este nome.", runCatching { Regras.salvarTipo(estado, 7, 0, "cadeira de rodas", "", "") }.exceptionOrNull()?.message)
    }

    @Test
    fun perfilAtualizaDadosERecusaSenhaAtualErrada() {
        val estado = EstadoInicial.criar()
        val atualizado = Regras.atualizarPerfil(estado, 1, "Ana Costa Silva", "11987654321", "01310-000", "", "")
        val ana = atualizado.usuarios.first { it.id == 1 }
        assertEquals("Ana Costa Silva", ana.nome)
        assertEquals("11987654321", ana.telefone)
        assertEquals("01310000", ana.cep)
        val senha = runCatching { Regras.alterarSenha(atualizado, 1, "errada", "Nova@2026") }.exceptionOrNull()
        assertEquals("A senha atual não confere.", senha?.message)
        val trocada = Regras.alterarSenha(atualizado, 1, EstadoInicial.SENHA_DEMO, "Nova@2026")
        assertEquals(Seguranca.hashSenha("Nova@2026"), trocada.usuarios.first { it.id == 1 }.senhaHash)
    }

    @Test
    fun migracaoRestauraSenhaDoAdminUmaUnicaVez() {
        val base = EstadoInicial.criar().copy(versao = 2)
        val alterado = Regras.alterarSenha(base, 7, EstadoInicial.SENHA_DEMO, "Outra@2026")
        val migrado = EstadoInicial.restaurarSenhaAdmin(alterado)
        assertEquals(3, migrado.versao)
        assertEquals(Seguranca.hashSenha(EstadoInicial.SENHA_DEMO), migrado.usuarios.first { it.id == 7 }.senhaHash)
        assertEquals("Ana Costa", migrado.usuarios.first { it.id == 1 }.nome)
        val depois = Regras.alterarSenha(migrado, 7, EstadoInicial.SENHA_DEMO, "Outra@2026")
        val mantido = EstadoInicial.restaurarSenhaAdmin(depois)
        assertEquals(Seguranca.hashSenha("Outra@2026"), mantido.usuarios.first { it.id == 7 }.senhaHash)
    }

    @Test
    fun cincoSenhasErradasBloqueiamOLogin() {
        var estado = EstadoInicial.criar()
        repeat(4) {
            val resultado = Regras.autenticar(estado, "admin@mobilis.app", "errada")
            assertEquals("E-mail ou senha incorretos.", resultado.mensagemErro)
            estado = resultado.estado
        }
        val bloqueio = Regras.autenticar(estado, "admin@mobilis.app", "errada")
        assertEquals("Login bloqueado por 15 minutos depois de cinco tentativas incorretas.", bloqueio.mensagemErro)
        val durante = Regras.autenticar(bloqueio.estado, "admin@mobilis.app", EstadoInicial.SENHA_DEMO)
        assertEquals("Login bloqueado por 15 minutos depois de cinco tentativas incorretas.", durante.mensagemErro)
    }
}
