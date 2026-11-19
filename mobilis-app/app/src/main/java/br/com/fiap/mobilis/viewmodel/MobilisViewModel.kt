package br.com.fiap.mobilis.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.fiap.mobilis.data.ArquivosLocais
import br.com.fiap.mobilis.data.LocalStore
import br.com.fiap.mobilis.data.TokenStore
import br.com.fiap.mobilis.domain.Conservacao
import br.com.fiap.mobilis.domain.Consultas
import br.com.fiap.mobilis.domain.Modalidade
import br.com.fiap.mobilis.domain.NovaInstituicao
import br.com.fiap.mobilis.domain.NovoSolicitante
import br.com.fiap.mobilis.domain.PapelInstituicao
import br.com.fiap.mobilis.domain.ResultadoPesquisa
import br.com.fiap.mobilis.domain.Seguranca
import br.com.fiap.mobilis.domain.TipoManutencao
import br.com.fiap.mobilis.domain.Validacao
import br.com.fiap.mobilis.repository.MobilisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class RascunhoPessoa(
    val nome: String,
    val cpf: String,
    val email: String,
    val telefone: String,
    val cep: String,
    val senha: String
)

data class RascunhoInstituicao(
    val razaoSocial: String,
    val cnpj: String,
    val endereco: String,
    val cep: String,
    val horario: String,
    val responsavel: String,
    val telefone: String,
    val email: String,
    val senha: String
)

class MobilisViewModel(application: Application) : AndroidViewModel(application) {
    private val repositorio = MobilisRepository(LocalStore(application), TokenStore(application))

    val estado = repositorio.estado
    private val _sessao = MutableStateFlow<br.com.fiap.mobilis.domain.Sessao?>(null)
    val sessao: StateFlow<br.com.fiap.mobilis.domain.Sessao?> = _sessao.asStateFlow()
    private val _pronto = MutableStateFlow(false)
    val pronto: StateFlow<Boolean> = _pronto.asStateFlow()
    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()
    private val _codigo = MutableStateFlow<String?>(null)
    val codigoDemonstracao: StateFlow<String?> = _codigo.asStateFlow()
    private val _processando = MutableStateFlow(false)
    val processando: StateFlow<Boolean> = _processando.asStateFlow()

    var rascunhoPessoa: RascunhoPessoa? = null
    var rascunhoInstituicao: RascunhoInstituicao? = null
    var emailFluxo: String = ""

    init {
        viewModelScope.launch {
            try {
                repositorio.preparar()
                _sessao.value = repositorio.restaurarSessao()
            } finally {
                _pronto.value = true
            }
            repositorio.estado.collect { atual ->
                val sessao = _sessao.value ?: return@collect
                val usuario = atual.usuarios.find { it.id == sessao.usuarioId }
                val instituicao = atual.instituicoes.find { it.id == usuario?.instituicaoId }
                if (usuario == null || !usuario.ativo || instituicao?.status == br.com.fiap.mobilis.domain.StatusInstituicao.REPROVADA) {
                    repositorio.sair()
                    _sessao.value = null
                    return@collect
                }
                _sessao.value = repositorio.montar(usuario)
            }
        }
    }

    fun consumirMensagem() {
        _mensagem.value = null
    }

    fun entrar(email: String, senha: String) {
        viewModelScope.launch {
            _processando.value = true
            val resultado = repositorio.entrar(email, senha)
            _processando.value = false
            resultado.onSuccess { _sessao.value = it }.onFailure { _mensagem.value = it.message }
        }
    }

    fun sair() {
        repositorio.sair()
        _sessao.value = null
    }

    fun guardarPessoa(rascunho: RascunhoPessoa): String? {
        Validacao.nome(rascunho.nome)?.let { return it }
        Validacao.cpf(rascunho.cpf)?.let { return it }
        Validacao.email(rascunho.email)?.let { return it }
        Validacao.telefone(rascunho.telefone)?.let { return it }
        Validacao.cep(rascunho.cep)?.let { return it }
        Validacao.senha(rascunho.senha)?.let { return it }
        rascunhoPessoa = rascunho.copy(
            cpf = Validacao.digitos(rascunho.cpf),
            email = Validacao.emailNormalizado(rascunho.email),
            telefone = Validacao.digitos(rascunho.telefone),
            cep = Validacao.digitos(rascunho.cep)
        )
        rascunhoInstituicao = null
        emailFluxo = rascunhoPessoa!!.email
        return null
    }

    fun guardarInstituicao(rascunho: RascunhoInstituicao): String? {
        Validacao.obrigatorio(rascunho.razaoSocial, "a razão social")?.let { return it }
        Validacao.cnpj(rascunho.cnpj)?.let { return it }
        Validacao.obrigatorio(rascunho.endereco, "o endereço")?.let { return it }
        Validacao.cep(rascunho.cep)?.let { return it }
        Validacao.obrigatorio(rascunho.horario, "o horário de retirada")?.let { return it }
        Validacao.nome(rascunho.responsavel, "o nome do responsável")?.let { return it }
        Validacao.telefone(rascunho.telefone)?.let { return it }
        Validacao.email(rascunho.email)?.let { return it }
        Validacao.senha(rascunho.senha)?.let { return it }
        rascunhoInstituicao = rascunho.copy(
            cnpj = Validacao.digitos(rascunho.cnpj),
            cep = Validacao.digitos(rascunho.cep),
            telefone = Validacao.digitos(rascunho.telefone),
            email = Validacao.emailNormalizado(rascunho.email)
        )
        rascunhoPessoa = null
        emailFluxo = rascunhoInstituicao!!.email
        return null
    }

    fun enviarCodigoCadastro(geral: Boolean, saude: Boolean, onOk: () -> Unit) = executar(onOk) {
        val pessoa = rascunhoPessoa
        val instituicao = rascunhoInstituicao
        when {
            pessoa != null -> repositorio.iniciarSolicitante(
                NovoSolicitante(pessoa.nome.trim(), pessoa.cpf, pessoa.email, pessoa.telefone, pessoa.cep, Seguranca.hashSenha(pessoa.senha)),
                geral,
                saude
            )
            instituicao != null -> repositorio.iniciarInstituicao(
                NovaInstituicao(
                    instituicao.razaoSocial.trim(),
                    instituicao.cnpj,
                    instituicao.endereco.trim(),
                    instituicao.cep,
                    instituicao.horario.trim(),
                    instituicao.responsavel.trim(),
                    instituicao.telefone,
                    instituicao.email,
                    Seguranca.hashSenha(instituicao.senha)
                ),
                geral,
                saude
            )
            else -> Result.failure(IllegalStateException("Preencha o cadastro antes dos termos."))
        }
    }

    fun confirmarCodigo(codigo: String, onOk: () -> Unit) = executar(onOk) { repositorio.confirmar(emailFluxo, codigo) }
    fun pedirSenha(email: String, onOk: () -> Unit) = executar(onOk) {
        emailFluxo = Validacao.emailNormalizado(email)
        repositorio.pedirSenha(emailFluxo)
    }
    fun definirSenha(codigo: String, senha: String, onOk: () -> Unit) = executar(onOk) { repositorio.novaSenha(emailFluxo, codigo, senha) }
    fun aceitarConvite(email: String, codigo: String, nome: String, senha: String, geral: Boolean, saude: Boolean, onOk: () -> Unit) =
        executar(onOk) { repositorio.aceitarConvite(email, codigo, nome, senha, geral, saude) }

    fun aprovarInstituicao(id: Int) = comUsuario { repositorio.aprovarInstituicao(it, id) }
    fun reprovarInstituicao(id: Int, motivo: String) = comUsuario { repositorio.reprovarInstituicao(it, id, motivo) }
    fun salvarTipo(id: Int, nome: String, descricao: String, orientacao: String, onOk: () -> Unit) =
        comUsuario(onOk) { repositorio.salvarTipo(it, id, nome, descricao, orientacao) }
    fun desativarTipo(id: Int) = comUsuario { repositorio.desativarTipo(it, id) }
    fun salvarPoliticaTipo(tipoId: Int, modalidades: List<Modalidade>, prazo: Int, comprovante: Boolean) =
        comUsuario { repositorio.salvarPoliticaTipo(it, tipoId, modalidades, prazo, comprovante) }
    fun salvarPoliticaItem(equipamentoId: Int, modalidades: List<Modalidade>, prazo: Int, comprovante: Boolean) =
        comUsuario { repositorio.salvarPoliticaItem(it, equipamentoId, modalidades, prazo, comprovante) }
    fun removerPoliticaItem(equipamentoId: Int) = comUsuario { repositorio.removerPoliticaItem(it, equipamentoId) }
    fun salvarEquipamento(id: Int, tipoId: Int, conservacao: Conservacao, observacoes: String, modalidades: List<Modalidade>, fotos: List<String>, onOk: () -> Unit) =
        comUsuario(onOk) { repositorio.salvarEquipamento(it, id, tipoId, conservacao, observacoes, modalidades, fotos) }
    fun baixar(id: Int, motivo: String, onOk: () -> Unit) = comUsuario(onOk) { repositorio.baixar(it, id, motivo) }
    fun enviarHigienizacao(id: Int) = comUsuario { repositorio.enviarHigienizacao(it, id) }
    fun manutencao(id: Int, tipo: TipoManutencao, data: String, responsavel: String, observacoes: String) =
        comUsuario { repositorio.manutencao(it, id, tipo, data, responsavel, observacoes) }
    fun entrarFila(instituicaoId: Int, tipoId: Int) = comUsuario { repositorio.entrarFila(it, instituicaoId, tipoId) }
    fun sairFila(filaId: Int) = comUsuario { repositorio.sairFila(it, filaId) }
    fun criarPedido(equipamentoId: Int, modalidade: Modalidade, justificativa: String, nome: String?, caminho: String?, onOk: () -> Unit) =
        comUsuario(onOk) { repositorio.criarPedido(it, equipamentoId, modalidade, justificativa, nome, caminho) }
    fun cancelar(pedidoId: Int) = comUsuario { repositorio.cancelar(it, pedidoId) }
    fun propor(pedidoId: Int, modalidade: Modalidade) = comUsuario { repositorio.propor(it, pedidoId, modalidade) }
    fun responderProposta(pedidoId: Int, aceitar: Boolean, motivo: String) = comUsuario { repositorio.responderProposta(it, pedidoId, aceitar, motivo) }
    fun aprovarPedido(pedidoId: Int) = comUsuario { repositorio.aprovarPedido(it, pedidoId) }
    fun recusarPedido(pedidoId: Int, motivo: String) = comUsuario { repositorio.recusarPedido(it, pedidoId, motivo) }
    fun anexar(pedidoId: Int, nome: String, caminho: String) = comUsuario { repositorio.anexar(it, pedidoId, nome, caminho) }
    fun aceitarTermo(pedidoId: Int) = comUsuario { repositorio.aceitarTermo(it, pedidoId) }
    fun retirada(codigo: String, onOk: () -> Unit = {}) = comUsuario(onOk) { repositorio.retirada(it, codigo) }
    fun prorrogar(pedidoId: Int) = comUsuario { repositorio.prorrogar(it, pedidoId) }
    fun decidirProrrogacao(pedidoId: Int, permitir: Boolean, motivo: String) = comUsuario { repositorio.decidirProrrogacao(it, pedidoId, permitir, motivo) }
    fun devolver(codigo: String, conservacao: Conservacao, onOk: () -> Unit = {}) = comUsuario(onOk) { repositorio.devolver(it, codigo, conservacao) }
    fun pedirConversao(pedidoId: Int, texto: String, nome: String?, caminho: String?) = comUsuario { repositorio.pedirConversao(it, pedidoId, texto, nome, caminho) }
    fun analisarConversao(pedidoId: Int, permitir: Boolean, motivo: String) = comUsuario { repositorio.analisarConversao(it, pedidoId, permitir, motivo) }
    fun aceitarDoacao(pedidoId: Int) = comUsuario { repositorio.aceitarDoacao(it, pedidoId) }
    fun naoPreciso(pedidoId: Int) = comUsuario { repositorio.naoPreciso(it, pedidoId) }
    fun avaliar(pedidoId: Int, atendimento: Int, equipamento: Int, comentario: String) =
        comUsuario { repositorio.avaliar(it, pedidoId, atendimento, equipamento, comentario) }
    fun notificacoes(push: Boolean, email: Boolean) = comUsuario { repositorio.notificacoes(it, push, email) }
    fun atualizarPerfil(nome: String, telefone: String, cep: String, endereco: String, horario: String) =
        comUsuario { repositorio.atualizarPerfil(it, nome, telefone, cep, endereco, horario) }
    fun alterarSenha(atual: String, nova: String, onOk: () -> Unit = {}) =
        comUsuario(onOk) { repositorio.alterarSenha(it, atual, nova) }
    fun excluirConta() = comUsuario {
        val resultado = repositorio.excluir(it)
        if (resultado.isSuccess) {
            repositorio.sair()
            _sessao.value = null
        }
        resultado
    }
    fun indicacao(unidade: String, nome: String, cpf: String, tipoId: Int, registro: String, onOk: () -> Unit) =
        comUsuario(onOk) { repositorio.indicacao(it, unidade, nome, cpf, tipoId, registro) }
    fun marcarAvisos() = comUsuario { repositorio.marcarAvisos(it) }
    fun marcarAviso(avisoId: Int) = comUsuario { repositorio.marcarAviso(it, avisoId) }
    fun confirmarIndicacao(id: Int) = comUsuario { repositorio.confirmarIndicacao(it, id) }
    fun convidar(email: String) = comUsuario { repositorio.convidar(it, email) }
    fun alterarPapel(usuarioId: Int, papel: PapelInstituicao) = comUsuario { repositorio.papel(it, usuarioId, papel) }
    fun desativarMembro(usuarioId: Int) = comUsuario { repositorio.desativar(it, usuarioId) }

    suspend fun pesquisar(
        tipoId: Int?,
        modalidade: Modalidade?,
        raioKm: Double,
        latitude: Double,
        longitude: Double
    ): Result<ResultadoPesquisa> = try {
        withTimeout(3_000) {
            val usuario = _sessao.value?.usuarioId ?: 0
            val resultado = withContext(Dispatchers.Default) {
                Consultas.buscar(estado.value, usuario, tipoId, modalidade, raioKm, latitude, longitude)
            }
            Result.success(resultado)
        }
    } catch (_: TimeoutCancellationException) {
        Result.failure(IllegalStateException("A busca passou de 3 segundos. Reduza o raio ou tente de novo."))
    }

    fun copiarAnexo(uri: Uri, somenteImagem: Boolean = false): Pair<String, String>? =
        ArquivosLocais.copiar(getApplication(), uri, "anexo", somenteImagem)

    fun exportarDados(): String {
        val id = _sessao.value?.usuarioId ?: return ""
        return Consultas.exportarPessoa(estado.value, id)
    }

    fun relatorio(tipoId: Int? = null, modalidade: Modalidade? = null): String {
        val id = _sessao.value?.instituicaoId ?: return ""
        return Consultas.relatorio(estado.value, id, tipoId, modalidade)
    }

    fun relatorioCsv(tipoId: Int? = null, modalidade: Modalidade? = null): String {
        val id = _sessao.value?.instituicaoId ?: return ""
        return Consultas.relatorioCsv(estado.value, id, tipoId, modalidade)
    }

    private fun comUsuario(onOk: () -> Unit = {}, bloco: suspend (Int) -> Result<String>) {
        val usuario = _sessao.value?.usuarioId
        if (usuario == null) {
            _mensagem.value = "Entre novamente para continuar."
            return
        }
        executar(onOk) { bloco(usuario) }
    }

    private fun executar(onOk: () -> Unit = {}, bloco: suspend () -> Result<String>) {
        if (_processando.value) return
        viewModelScope.launch {
            _processando.value = true
            val resultado = bloco()
            repositorio.codigoDemonstracao?.let { _codigo.value = it }
            _processando.value = false
            resultado.onSuccess {
                _mensagem.value = it
                onOk()
            }.onFailure {
                _mensagem.value = it.message ?: "Não foi possível concluir."
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MobilisViewModel(application) as T
            }
    }
}
