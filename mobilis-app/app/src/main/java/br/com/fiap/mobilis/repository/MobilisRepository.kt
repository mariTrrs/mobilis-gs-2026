package br.com.fiap.mobilis.repository

import br.com.fiap.mobilis.data.LocalStore
import br.com.fiap.mobilis.data.TokenStore
import br.com.fiap.mobilis.domain.Conservacao
import br.com.fiap.mobilis.domain.MobilisState
import br.com.fiap.mobilis.domain.Modalidade
import br.com.fiap.mobilis.domain.NovaInstituicao
import br.com.fiap.mobilis.domain.NovoSolicitante
import br.com.fiap.mobilis.domain.PapelInstituicao
import br.com.fiap.mobilis.domain.Perfil
import br.com.fiap.mobilis.domain.RegraNegocio
import br.com.fiap.mobilis.domain.Regras
import br.com.fiap.mobilis.domain.Seguranca
import br.com.fiap.mobilis.domain.Sessao
import br.com.fiap.mobilis.domain.StatusInstituicao
import br.com.fiap.mobilis.domain.TipoManutencao
import br.com.fiap.mobilis.domain.Tokens
import br.com.fiap.mobilis.domain.Usuario
import br.com.fiap.mobilis.domain.Validacao
import br.com.fiap.mobilis.domain.erro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MobilisRepository(
    private val store: LocalStore,
    private val tokenStore: TokenStore
) {
    private val mutex = Mutex()
    private val _estado = MutableStateFlow(MobilisState())
    val estado: StateFlow<MobilisState> = _estado.asStateFlow()

    suspend fun preparar() {
        val novo = withContext(Dispatchers.IO) {
            val base = store.carregar()
            val calculado = Regras.rotina(base)
            if (calculado != base) store.salvar(calculado)
            calculado
        }
        _estado.value = novo
    }
    var codigoDemonstracao: String? = null
        private set

    fun limparCodigo() {
        codigoDemonstracao = null
    }

    fun restaurarSessao(): Sessao? {
        val token = tokenStore.ler() ?: return null
        val id = Tokens.usuarioId(token) ?: run {
            tokenStore.limpar()
            return null
        }
        val usuario = _estado.value.usuarios.find { it.id == id && it.ativo } ?: run {
            tokenStore.limpar()
            return null
        }
        val instituicao = _estado.value.instituicoes.find { it.id == usuario.instituicaoId }
        if (instituicao?.status == StatusInstituicao.REPROVADA) {
            tokenStore.limpar()
            return null
        }
        return montar(usuario)
    }

    fun sair() {
        tokenStore.limpar()
    }

    suspend fun entrar(email: String, senha: String): Result<Sessao> = io {
        val resultado = Regras.autenticar(_estado.value, email, senha)
        if (resultado.estado != _estado.value) {
            _estado.value = resultado.estado
            store.salvar(resultado.estado)
        }
        val usuario = resultado.usuario ?: erro(resultado.mensagemErro ?: "E-mail ou senha incorretos.")
        tokenStore.salvar(Tokens.emitir(usuario.id))
        montar(usuario)
    }

    suspend fun iniciarSolicitante(dados: NovoSolicitante, geral: Boolean, saude: Boolean): Result<String> = alterarMensagem("Enviamos um código de confirmação.") {
        val codigo = publicarCodigo()
        Regras.iniciarCadastroSolicitante(it, dados, codigo, termoGeral = geral, termoSaude = saude)
    }

    suspend fun iniciarInstituicao(dados: NovaInstituicao, geral: Boolean, saude: Boolean): Result<String> = alterarMensagem("Enviamos um código de confirmação.") {
        val codigo = publicarCodigo()
        Regras.iniciarCadastroInstituicao(it, dados, codigo, termoGeral = geral, termoSaude = saude)
    }

    suspend fun confirmar(email: String, codigo: String): Result<String> = alterarMensagem("Cadastro confirmado. Entre com seu e-mail.") {
        Regras.confirmarCadastro(it, email, codigo)
    }

    suspend fun pedirSenha(email: String): Result<String> = alterarMensagem("Se a conta existir, o código foi gerado.") {
        val codigo = publicarCodigo()
        Regras.pedirRecuperacao(it, email, codigo)
    }

    suspend fun novaSenha(email: String, codigo: String, senha: String): Result<String> = alterarMensagem("Senha atualizada. Entre de novo.") {
        Regras.redefinirSenha(it, email, codigo, senha)
    }

    suspend fun aceitarConvite(email: String, codigo: String, nome: String, senha: String, geral: Boolean, saude: Boolean): Result<String> =
        alterarMensagem("Convite aceito. Entre com o e-mail convidado.") {
            Regras.aceitarConvite(it, email, codigo, nome, senha, geral, saude)
        }

    suspend fun aprovarInstituicao(atorId: Int, id: Int) = alterarMensagem("Instituição aprovada.") { Regras.aprovarInstituicao(it, atorId, id) }
    suspend fun reprovarInstituicao(atorId: Int, id: Int, motivo: String) = alterarMensagem("Instituição reprovada.") { Regras.reprovarInstituicao(it, atorId, id, motivo) }
    suspend fun salvarTipo(atorId: Int, id: Int, nome: String, descricao: String, orientacao: String) = alterarMensagem("Tipo salvo.") { Regras.salvarTipo(it, atorId, id, nome, descricao, orientacao) }
    suspend fun desativarTipo(atorId: Int, id: Int) = alterarMensagem("Tipo desativado.") { Regras.desativarTipo(it, atorId, id) }
    suspend fun salvarPoliticaTipo(atorId: Int, tipoId: Int, modalidades: List<Modalidade>, prazo: Int, comprovante: Boolean) =
        alterarMensagem("Política do tipo salva.") { Regras.salvarPoliticaTipo(it, atorId, tipoId, modalidades, prazo, comprovante) }
    suspend fun salvarPoliticaItem(atorId: Int, equipamentoId: Int, modalidades: List<Modalidade>, prazo: Int, comprovante: Boolean) =
        alterarMensagem("Política do item salva. Ela prevalece sobre a do tipo.") { Regras.salvarPoliticaItem(it, atorId, equipamentoId, modalidades, prazo, comprovante) }
    suspend fun removerPoliticaItem(atorId: Int, equipamentoId: Int) = alterarMensagem("O item voltou a seguir a política do tipo.") { Regras.removerPoliticaItem(it, atorId, equipamentoId) }
    suspend fun salvarEquipamento(atorId: Int, id: Int, tipoId: Int, conservacao: Conservacao, observacoes: String, modalidades: List<Modalidade>, fotos: List<String>) =
        alterarMensagem(if (id == 0) "Equipamento cadastrado." else "Equipamento atualizado.") {
            Regras.salvarEquipamento(it, atorId, id, tipoId, conservacao, observacoes, modalidades, fotos)
        }
    suspend fun baixar(atorId: Int, id: Int, motivo: String) = alterarMensagem("Baixa registrada.") { Regras.baixar(it, atorId, id, motivo) }
    suspend fun enviarHigienizacao(atorId: Int, id: Int) = alterarMensagem("Item enviado para higienização.") { Regras.enviarParaHigienizacao(it, atorId, id) }
    suspend fun manutencao(atorId: Int, id: Int, tipo: TipoManutencao, data: String, responsavel: String, observacoes: String) =
        alterarMensagem("Registro salvo.") { Regras.registrarManutencao(it, atorId, id, tipo, data, responsavel, observacoes) }
    suspend fun entrarFila(usuarioId: Int, instituicaoId: Int, tipoId: Int) = alterarMensagem("Você entrou na fila de espera.") { Regras.entrarFila(it, usuarioId, instituicaoId, tipoId) }
    suspend fun sairFila(usuarioId: Int, filaId: Int) = alterarMensagem("Você saiu da fila.") { Regras.sairFila(it, usuarioId, filaId) }
    suspend fun criarPedido(usuarioId: Int, equipamentoId: Int, modalidade: Modalidade, justificativa: String, nome: String?, caminho: String?) =
        alterarMensagem("Reserva solicitada. O prazo para retirada aparece no pedido.") { Regras.criarPedido(it, usuarioId, equipamentoId, modalidade, justificativa, nome, caminho) }
    suspend fun cancelar(usuarioId: Int, pedidoId: Int) = alterarMensagem("Pedido cancelado.") { Regras.cancelarPedido(it, usuarioId, pedidoId) }
    suspend fun propor(usuarioId: Int, pedidoId: Int, modalidade: Modalidade) = alterarMensagem("Proposta enviada. Aguarde a instituição.") { Regras.proporModalidade(it, usuarioId, pedidoId, modalidade) }
    suspend fun responderProposta(atorId: Int, pedidoId: Int, aceitar: Boolean, motivo: String) =
        alterarMensagem(if (aceitar) "Proposta aceita." else "Proposta recusada.") { Regras.responderProposta(it, atorId, pedidoId, aceitar, motivo) }
    suspend fun aprovarPedido(atorId: Int, pedidoId: Int) = alterarMensagem("Pedido aprovado.") { Regras.aprovarPedido(it, atorId, pedidoId) }
    suspend fun recusarPedido(atorId: Int, pedidoId: Int, motivo: String) = alterarMensagem("Pedido recusado.") { Regras.recusarPedido(it, atorId, pedidoId, motivo) }
    suspend fun anexar(usuarioId: Int, pedidoId: Int, nome: String, caminho: String) = alterarMensagem("Comprovante anexado.") { Regras.anexarComprovante(it, usuarioId, pedidoId, nome, caminho) }
    suspend fun aceitarTermo(usuarioId: Int, pedidoId: Int) = alterarMensagem("Termo aceito. Apresente o pedido na retirada.") { Regras.aceitarTermo(it, usuarioId, pedidoId) }
    suspend fun retirada(atorId: Int, codigo: String) = alterarMensagem("Retirada confirmada.") { Regras.confirmarRetirada(it, atorId, codigo) }
    suspend fun prorrogar(usuarioId: Int, pedidoId: Int) = alterarMensagem("Pedido de prorrogação enviado à instituição.") { Regras.pedirProrrogacao(it, usuarioId, pedidoId) }
    suspend fun decidirProrrogacao(atorId: Int, pedidoId: Int, permitir: Boolean, motivo: String) =
        alterarMensagem(if (permitir) "Prorrogação aceita. A data avançou 15 dias." else "Prorrogação recusada. A data foi mantida.") {
            Regras.decidirProrrogacao(it, atorId, pedidoId, permitir, motivo)
        }
    suspend fun devolver(atorId: Int, codigo: String, conservacao: Conservacao) = alterarMensagem("Devolução registrada. O item foi para higienização.") { Regras.registrarDevolucao(it, atorId, codigo, conservacao) }
    suspend fun pedirConversao(usuarioId: Int, pedidoId: Int, texto: String, nome: String?, caminho: String?) =
        alterarMensagem("Pedido de conversão enviado.") { Regras.solicitarConversao(it, usuarioId, pedidoId, texto, nome, caminho) }
    suspend fun analisarConversao(atorId: Int, pedidoId: Int, permitir: Boolean, motivo: String) =
        alterarMensagem(if (permitir) "Conversão permitida. A data de devolução foi removida." else "Conversão negada. A data de devolução foi mantida.") {
            Regras.analisarConversao(it, atorId, pedidoId, permitir, motivo)
        }
    suspend fun aceitarDoacao(usuarioId: Int, pedidoId: Int) = alterarMensagem("Termo de doação aceito.") { Regras.aceitarTermoDoacao(it, usuarioId, pedidoId) }
    suspend fun naoPreciso(usuarioId: Int, pedidoId: Int) = alterarMensagem("A instituição foi avisada.") { Regras.naoPrecisoMais(it, usuarioId, pedidoId) }
    suspend fun avaliar(usuarioId: Int, pedidoId: Int, atendimento: Int, equipamento: Int, comentario: String) =
        alterarMensagem("Avaliação registrada. O gestor vê só as notas, sem o seu nome.") { Regras.avaliar(it, usuarioId, pedidoId, atendimento, equipamento, comentario) }
    suspend fun notificacoes(usuarioId: Int, push: Boolean, email: Boolean) = alterarMensagem("Preferências salvas.") { Regras.salvarNotificacoes(it, usuarioId, push, email) }
    suspend fun atualizarPerfil(usuarioId: Int, nome: String, telefone: String, cep: String, endereco: String, horario: String) =
        alterarMensagem("Perfil atualizado.") { Regras.atualizarPerfil(it, usuarioId, nome, telefone, cep, endereco, horario) }
    suspend fun alterarSenha(usuarioId: Int, atual: String, nova: String) =
        alterarMensagem("Senha alterada.") { Regras.alterarSenha(it, usuarioId, atual, nova) }
    suspend fun excluir(usuarioId: Int) = alterarMensagem("Conta excluída e dados pessoais removidos.") { Regras.excluirConta(it, usuarioId) }
    suspend fun indicacao(atorId: Int, unidade: String, nome: String, cpf: String, tipoId: Int, registro: String) = alterarMensagem("Indicação registrada. A pessoa confirma com o CPF da conta.") { Regras.registrarIndicacao(it, atorId, unidade, nome, cpf, tipoId, registro) }
    suspend fun marcarAvisos(usuarioId: Int) = alterarMensagem("Avisos marcados como lidos.") { Regras.marcarAvisosLidos(it, usuarioId) }
    suspend fun marcarAviso(usuarioId: Int, avisoId: Int) = alterarMensagem("Aviso marcado como lido.") { Regras.marcarAvisoLido(it, usuarioId, avisoId) }
    suspend fun confirmarIndicacao(usuarioId: Int, id: Int) = alterarMensagem("Indicação confirmada.") { Regras.confirmarIndicacao(it, usuarioId, id) }
    suspend fun convidar(atorId: Int, email: String) = alterarMensagem("Convite criado.") {
        val codigo = publicarCodigo()
        Regras.convidar(it, atorId, email, codigo)
    }
    suspend fun papel(atorId: Int, usuarioId: Int, papel: PapelInstituicao) = alterarMensagem("Papel atualizado.") { Regras.alterarPapel(it, atorId, usuarioId, papel) }
    suspend fun desativar(atorId: Int, usuarioId: Int) = alterarMensagem("Pessoa desativada.") { Regras.desativarMembro(it, atorId, usuarioId) }

    fun montar(usuario: Usuario): Sessao {
        val instituicao = _estado.value.instituicoes.find { it.id == usuario.instituicaoId }
        return Sessao(
            usuarioId = usuario.id,
            nome = usuario.nome,
            email = usuario.email,
            perfil = usuario.perfil,
            papel = usuario.papel,
            instituicaoId = usuario.instituicaoId,
            instituicaoStatus = instituicao?.status,
            instituicaoNome = instituicao?.razaoSocial
        )
    }

    private fun publicarCodigo(): String = Seguranca.codigoSeis().also { codigoDemonstracao = it }

    private suspend fun alterarMensagem(mensagem: String, bloco: (MobilisState) -> MobilisState): Result<String> = io {
        mutex.withLock {
            val novo = bloco(_estado.value)
            _estado.value = novo
            store.salvar(novo)
        }
        mensagem
    }

    private suspend fun <T> io(bloco: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(bloco())
        } catch (erroNegocio: RegraNegocio) {
            Result.failure(erroNegocio)
        } catch (falha: Exception) {
            Result.failure(RegraNegocio(falha.message ?: "Algo deu errado. Tente de novo."))
        }
    }
}
