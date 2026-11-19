package br.com.fiap.mobilis.domain

import kotlinx.serialization.Serializable

enum class Perfil(val rotulo: String) {
    SOLICITANTE("Solicitante"),
    INSTITUICAO("Instituição"),
    ADMIN("Administração")
}

enum class PapelInstituicao(val rotulo: String) {
    GESTOR("Gestor"),
    OPERADOR("Operador")
}

enum class Modalidade(val rotulo: String) {
    EMPRESTIMO("Empréstimo"),
    DOACAO("Doação")
}

enum class StatusInstituicao(val rotulo: String) {
    PENDENTE("Aguardando aprovação"),
    APROVADA("Aprovada"),
    REPROVADA("Reprovada")
}

enum class StatusEquipamento(val rotulo: String) {
    DISPONIVEL("Disponível"),
    RESERVADO("Reservado"),
    EMPRESTADO("Emprestado"),
    DOADO("Doado"),
    DEVOLUCAO_PENDENTE("Devolução pendente"),
    HIGIENIZACAO("Em higienização/manutenção"),
    BAIXADO("Baixado")
}

enum class StatusPedido(val rotulo: String) {
    AGUARDANDO_ANALISE("Aguardando análise"),
    APROVADO("Aprovado"),
    RECUSADO("Recusado"),
    RETIRADO("Retirado"),
    DEVOLVIDO("Devolvido"),
    CANCELADO("Cancelado"),
    EXPIRADO("Reserva expirada"),
    DOADO("Doação concluída")
}

enum class Conservacao(val rotulo: String) {
    OTIMO("Ótimo"),
    BOM("Bom"),
    REGULAR("Regular")
}

enum class TipoManutencao(val rotulo: String) {
    HIGIENIZACAO("Higienização"),
    REPARO("Reparo")
}

enum class StatusProposta(val rotulo: String) {
    AGUARDANDO("Aguardando resposta"),
    ACEITA("Aceita"),
    RECUSADA("Recusada")
}

enum class StatusConversao(val rotulo: String) {
    AGUARDANDO("Aguardando análise"),
    PERMITIDA("Permitida"),
    NEGADA("Negada")
}

@Serializable
data class Usuario(
    val id: Int,
    val perfil: Perfil,
    val papel: PapelInstituicao? = null,
    val nome: String,
    val documento: String = "",
    val email: String,
    val telefone: String = "",
    val cep: String = "",
    val endereco: String = "",
    val senhaHash: String,
    val instituicaoId: Int? = null,
    val ativo: Boolean = true,
    val emailConfirmado: Boolean = false,
    val aceitouTermoGeral: Boolean = false,
    val aceitouDadosSaude: Boolean = false,
    val notificarPush: Boolean = true,
    val notificarEmail: Boolean = true,
    val tentativasFalhas: Int = 0,
    val bloqueadoAte: Long = 0,
    val versaoTermo: String = "",
    val termoAceitoEm: Long = 0
)

@Serializable
data class Instituicao(
    val id: Int,
    val razaoSocial: String,
    val cnpj: String,
    val endereco: String,
    val cep: String,
    val latitude: Double,
    val longitude: Double,
    val horarioRetirada: String,
    val responsavelNome: String,
    val responsavelEmail: String,
    val telefone: String,
    val status: StatusInstituicao,
    val motivoReprovacao: String? = null,
    val limiteOciosidadeDias: Int = 30
)

@Serializable
data class TipoEquipamento(
    val id: Int,
    val nome: String,
    val descricao: String,
    val orientacao: String,
    val ativo: Boolean = true
)

@Serializable
data class PoliticaTipo(
    val id: Int,
    val instituicaoId: Int,
    val tipoId: Int,
    val modalidades: List<Modalidade>,
    val prazoDias: Int,
    val exigeComprovanteDoacao: Boolean
)

@Serializable
data class PoliticaItem(
    val id: Int,
    val equipamentoId: Int,
    val modalidades: List<Modalidade>,
    val prazoDias: Int,
    val exigeComprovanteDoacao: Boolean
)

@Serializable
data class Equipamento(
    val id: Int,
    val instituicaoId: Int,
    val tipoId: Int,
    val codigo: String,
    val conservacao: Conservacao,
    val observacoes: String,
    val modalidades: List<Modalidade>,
    val fotos: List<String> = emptyList(),
    val status: StatusEquipamento,
    val atualizadoEm: Long,
    val disponivelDesde: Long,
    val pedidoAtualId: Int? = null
)

@Serializable
data class Pedido(
    val id: Int,
    val solicitanteId: Int,
    val instituicaoId: Int,
    val equipamentoId: Int,
    val tipoId: Int,
    val modalidade: Modalidade,
    val status: StatusPedido,
    val justificativa: String,
    val comprovanteNome: String? = null,
    val comprovanteCaminho: String? = null,
    val criadoEm: Long,
    val prazoRetirada: String? = null,
    val prazoDevolucao: String? = null,
    val termoAceito: Boolean = false,
    val motivoRecusa: String? = null,
    val propostaModalidade: Modalidade? = null,
    val propostaStatus: StatusProposta? = null,
    val propostaMotivo: String? = null,
    val conversaoStatus: StatusConversao? = null,
    val conversaoJustificativa: String? = null,
    val conversaoComprovanteNome: String? = null,
    val conversaoComprovanteCaminho: String? = null,
    val conversaoMotivo: String? = null,
    val termoDoacaoAceito: Boolean = false,
    val avaliado: Boolean = false,
    val indicacaoId: Int? = null,
    val validadoPorIndicacao: Boolean = false,
    val naoPrecisaMais: Boolean = false,
    val prorrogado: Boolean = false,
    val prorrogacaoStatus: StatusProposta? = null,
    val atrasoGrave: Boolean = false,
    val estadoNaDevolucao: Conservacao? = null
)

@Serializable
data class EntradaFila(
    val id: Int,
    val solicitanteId: Int,
    val instituicaoId: Int,
    val tipoId: Int,
    val criadoEm: Long,
    val status: String = "AGUARDANDO"
)

@Serializable
data class Manutencao(
    val id: Int,
    val equipamentoId: Int,
    val tipo: TipoManutencao,
    val data: String,
    val responsavel: String,
    val observacoes: String,
    val registradoEm: Long
)

@Serializable
data class Movimentacao(
    val id: Int,
    val equipamentoId: Int,
    val descricao: String,
    val de: String,
    val para: String,
    val em: Long,
    val usuarioNome: String
)

@Serializable
data class Convite(
    val id: Int,
    val instituicaoId: Int,
    val email: String,
    val codigo: String,
    val status: String,
    val criadoEm: Long,
    val expiraEm: Long = 0
)

@Serializable
data class RegistroAuditoria(
    val id: Int,
    val usuarioId: Int,
    val usuarioNome: String,
    val instituicaoId: Int?,
    val acao: String,
    val entidade: String,
    val entidadeId: Int,
    val data: Long,
    val antes: String,
    val depois: String
)

@Serializable
data class Avaliacao(
    val id: Int,
    val pedidoId: Int,
    val instituicaoId: Int,
    val notaAtendimento: Int,
    val notaEquipamento: Int,
    val comentario: String,
    val criadaEm: Long
)

@Serializable
data class Indicacao(
    val id: Int,
    val unidade: String,
    val pacienteNome: String,
    val pacienteCpf: String,
    val tipoId: Int,
    val profissionalNome: String,
    val registroProfissional: String = "",
    val status: String,
    val criadaEm: Long
)

@Serializable
data class NovoSolicitante(
    val nome: String,
    val cpf: String,
    val email: String,
    val telefone: String,
    val cep: String,
    val senhaHash: String
)

@Serializable
data class NovaInstituicao(
    val razaoSocial: String,
    val cnpj: String,
    val endereco: String,
    val cep: String,
    val horarioRetirada: String,
    val responsavelNome: String,
    val telefone: String,
    val email: String,
    val senhaHash: String
)

@Serializable
data class CadastroPendente(
    val email: String,
    val codigo: String,
    val expiraEm: Long,
    val aceitouTermoGeral: Boolean,
    val aceitouDadosSaude: Boolean,
    val solicitante: NovoSolicitante? = null,
    val instituicao: NovaInstituicao? = null
)

@Serializable
data class CodigoAcesso(
    val email: String,
    val codigo: String,
    val expiraEm: Long
)

@Serializable
data class MobilisState(
    val versao: Int = 3,
    val usuarios: List<Usuario> = emptyList(),
    val instituicoes: List<Instituicao> = emptyList(),
    val tipos: List<TipoEquipamento> = emptyList(),
    val politicasTipo: List<PoliticaTipo> = emptyList(),
    val politicasItem: List<PoliticaItem> = emptyList(),
    val equipamentos: List<Equipamento> = emptyList(),
    val pedidos: List<Pedido> = emptyList(),
    val filas: List<EntradaFila> = emptyList(),
    val manutencoes: List<Manutencao> = emptyList(),
    val movimentacoes: List<Movimentacao> = emptyList(),
    val convites: List<Convite> = emptyList(),
    val auditoria: List<RegistroAuditoria> = emptyList(),
    val avaliacoes: List<Avaliacao> = emptyList(),
    val indicacoes: List<Indicacao> = emptyList(),
    val notificacoes: List<Notificacao> = emptyList(),
    val cadastrosPendentes: List<CadastroPendente> = emptyList(),
    val recuperacoes: List<CodigoAcesso> = emptyList(),
    val proximoId: Int = 1
)

data class Sessao(
    val usuarioId: Int,
    val nome: String,
    val email: String,
    val perfil: Perfil,
    val papel: PapelInstituicao?,
    val instituicaoId: Int?,
    val instituicaoStatus: StatusInstituicao?,
    val instituicaoNome: String?
) {
    val rotulo: String
        get() = when (perfil) {
            Perfil.SOLICITANTE -> "Solicitante"
            Perfil.ADMIN -> "Administração"
            Perfil.INSTITUICAO -> if (papel == PapelInstituicao.GESTOR) "Gestor" else "Operador"
        }

    val aguardandoInstituicao: Boolean
        get() = perfil == Perfil.INSTITUICAO && instituicaoStatus != StatusInstituicao.APROVADA
}

data class PoliticaEfetiva(
    val modalidades: List<Modalidade>,
    val prazoDias: Int,
    val exigeComprovanteDoacao: Boolean,
    val origem: String
) {
    val itemPrevalece: Boolean get() = origem == "item"
}

data class ResultadoBusca(
    val equipamento: Equipamento,
    val instituicao: Instituicao,
    val tipo: TipoEquipamento,
    val distanciaKm: Double,
    val politica: PoliticaEfetiva
)

data class OpcaoFila(
    val instituicao: Instituicao,
    val tipo: TipoEquipamento,
    val distanciaKm: Double,
    val jaNaFila: Boolean
)

data class ResultadoPesquisa(
    val itens: List<ResultadoBusca>,
    val filas: List<OpcaoFila>,
    val existemForaDoRaio: Boolean
)

data class Contagem(val rotulo: String, val quantidade: Int)

data class PrevisaoDemanda(
    val tipo: String,
    val fila: Int,
    val disponiveis: Int,
    val criterio: String,
    val suficiente: Boolean = true
)

@Serializable
data class Notificacao(
    val id: Int,
    val usuarioId: Int,
    val tipo: String,
    val titulo: String,
    val texto: String,
    val lida: Boolean = false,
    val criadaEm: Long,
    val chave: String = ""
)

data class ResultadoLogin(
    val estado: MobilisState,
    val usuario: Usuario? = null,
    val mensagemErro: String? = null
)

data class PainelGestao(
    val porStatus: List<Contagem>,
    val porTipo: List<Contagem>,
    val porModalidade: List<Contagem>,
    val proximos: List<Pedido>,
    val atrasos: List<Pedido>,
    val ociosos: List<Equipamento>,
    val previsoes: List<PrevisaoDemanda>,
    val encerramentos: List<Pedido>
)

class RegraNegocio(message: String) : Exception(message)

fun erro(mensagem: String): Nothing = throw RegraNegocio(mensagem)

fun MobilisState.alocar(): Pair<MobilisState, Int> = copy(proximoId = proximoId + 1) to proximoId
