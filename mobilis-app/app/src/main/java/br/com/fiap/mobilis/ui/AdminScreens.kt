package br.com.fiap.mobilis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.mobilis.domain.Consultas
import br.com.fiap.mobilis.domain.Datas
import br.com.fiap.mobilis.domain.PapelInstituicao
import br.com.fiap.mobilis.domain.Perfil
import br.com.fiap.mobilis.domain.StatusInstituicao
import br.com.fiap.mobilis.domain.Validacao
import br.com.fiap.mobilis.ui.componentes.Aviso
import br.com.fiap.mobilis.ui.componentes.BotaoPrincipal
import br.com.fiap.mobilis.ui.componentes.BotaoSecundario
import br.com.fiap.mobilis.ui.componentes.Campo
import br.com.fiap.mobilis.ui.componentes.CartaoAcao
import br.com.fiap.mobilis.ui.componentes.Casca
import br.com.fiap.mobilis.ui.componentes.ColunaTela
import br.com.fiap.mobilis.ui.componentes.EstadoVazio
import br.com.fiap.mobilis.ui.componentes.LinhaDado
import br.com.fiap.mobilis.ui.componentes.PainelCard
import br.com.fiap.mobilis.ui.componentes.TelaPadrao
import br.com.fiap.mobilis.viewmodel.MobilisViewModel

@Composable
fun TelaInicioAdmin(vm: MobilisViewModel, onAba: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    val pendentes = estado.instituicoes.count { it.status == StatusInstituicao.PENDENTE }
    Casca("Administração", "home", pessoa, onAba) { padding ->
        ColunaTela(padding) {
            Text("Olá, ${pessoa.nome}", style = MaterialTheme.typography.headlineSmall)
            Text("A administração aprova instituições, cuida do catálogo de tipos e consulta a auditoria da plataforma.")
            CartaoAcao("Instituições pendentes", "$pendentes aguardando análise") { onAba("instituicoes") }
            CartaoAcao("Catálogo de tipos", "Criar, editar ou desativar") { onAba("catalogo") }
            CartaoAcao("Auditoria", "Ações com o antes e o depois") { onAba("auditoria") }
        }
    }
}

@Composable
fun TelaInstituicoes(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    var filtro by rememberSaveable { mutableStateOf("todos") }
    val lista = estado.instituicoes.filter {
        when (filtro) {
            "pendentes" -> it.status == StatusInstituicao.PENDENTE
            "aprovadas" -> it.status == StatusInstituicao.APROVADA
            "reprovadas" -> it.status == StatusInstituicao.REPROVADA
            else -> true
        }
    }
    Casca("Instituições", "instituicoes", pessoa, onAba) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                br.com.fiap.mobilis.ui.componentes.ChipsUnico(
                    listOf("todos", "pendentes", "aprovadas", "reprovadas"),
                    filtro,
                    {
                        when (it) {
                            "pendentes" -> "Pendentes"
                            "aprovadas" -> "Aprovadas"
                            "reprovadas" -> "Reprovadas"
                            else -> "Todos"
                        }
                    }
                ) { filtro = it }
            }
            if (lista.isEmpty()) item { EstadoVazio("Nenhuma instituição", "A fila muda assim que você aprova ou reprova.") }
            items(lista, key = { it.id }) { instituicao ->
                CartaoAcao(instituicao.razaoSocial, "${instituicao.status.rotulo}. ${Validacao.formatarCnpj(instituicao.cnpj)}") {
                    onAbrir("admin/instituicao/${instituicao.id}")
                }
            }
        }
    }
}

@Composable
fun TelaInstituicaoAdmin(vm: MobilisViewModel, instituicaoId: Int, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val instituicao = estado.instituicoes.find { it.id == instituicaoId }
    var motivo by rememberSaveable { mutableStateOf("") }
    var confirmar by rememberSaveable { mutableStateOf(false) }
    if (instituicao == null) {
        TelaPadrao("Instituição", onVoltar) { Text("Não encontrada.", modifier = Modifier.padding(16.dp)) }
        return
    }
    TelaPadrao(instituicao.razaoSocial, onVoltar) { padding ->
        ColunaTela(padding) {
            LinhaDado("Situação", instituicao.status.rotulo)
            LinhaDado("CNPJ", Validacao.formatarCnpj(instituicao.cnpj))
            LinhaDado("Endereço", instituicao.endereco)
            LinhaDado("CEP", Validacao.formatarCep(instituicao.cep))
            LinhaDado("Horário de retirada", instituicao.horarioRetirada)
            LinhaDado("Responsável", "${instituicao.responsavelNome} · ${instituicao.responsavelEmail}")
            LinhaDado("Telefone", Validacao.formatarTelefone(instituicao.telefone))
            if (!instituicao.motivoReprovacao.isNullOrBlank()) Aviso("Motivo da reprovação: ${instituicao.motivoReprovacao}")
            if (instituicao.status != StatusInstituicao.APROVADA) BotaoPrincipal("Aprovar") { confirmar = true }
            Campo(motivo, { motivo = it }, "Motivo da reprovação")
            BotaoSecundario("Reprovar") { vm.reprovarInstituicao(instituicao.id, motivo) }
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Aprovar instituição") },
            text = { Text("Depois da aprovação, o gestor entra no painel.") },
            confirmButton = { TextButton(onClick = { confirmar = false; vm.aprovarInstituicao(instituicao.id) }) { Text("Aprovar") } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Voltar") } }
        )
    }
}

@Composable
fun TelaCatalogoAdmin(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    Casca("Catálogo", "catalogo", pessoa, onAba) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Itens com prescrição, como concentrador de oxigênio, ficam fora do catálogo.")
                BotaoPrincipal("Novo tipo") { onAbrir("admin/tipo/0") }
            }
            items(estado.tipos, key = { it.id }) { tipo ->
                CartaoAcao(tipo.nome, if (tipo.ativo) tipo.descricao else "Desativado. ${tipo.descricao}") { onAbrir("admin/tipo/${tipo.id}") }
            }
        }
    }
}

@Composable
fun TelaFormTipo(vm: MobilisViewModel, tipoId: Int, onOk: () -> Unit, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val atual = estado.tipos.find { it.id == tipoId }
    var nome by rememberSaveable { mutableStateOf(atual?.nome ?: "") }
    var descricao by rememberSaveable { mutableStateOf(atual?.descricao ?: "") }
    var orientacao by rememberSaveable { mutableStateOf(atual?.orientacao ?: "") }
    TelaPadrao(if (tipoId == 0) "Novo tipo" else "Editar tipo", onVoltar) { padding ->
        ColunaTela(padding) {
            Campo(nome, { nome = it }, "Nome")
            Campo(descricao, { descricao = it }, "Para quem serve", linhas = 2)
            Campo(orientacao, { orientacao = it }, "Orientação de uso", linhas = 2)
            BotaoPrincipal("Salvar") { vm.salvarTipo(tipoId, nome, descricao, orientacao, onOk) }
            if (atual?.ativo == true) BotaoSecundario("Desativar tipo") { vm.desativarTipo(tipoId) }
            if (atual?.ativo == false) Aviso("Este tipo está desativado e não entra em cadastro novo nem na busca.")
        }
    }
}

@Composable
fun TelaAuditoria(vm: MobilisViewModel, comVoltar: Boolean, onVoltar: () -> Unit, onAba: (String) -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    val usuario = estado.usuarios.find { it.id == pessoa.usuarioId }
    var texto by rememberSaveable { mutableStateOf("") }
    var acao by rememberSaveable { mutableStateOf("") }
    val permitido = pessoa.perfil == Perfil.ADMIN || pessoa.papel == PapelInstituicao.GESTOR
    val conteudo: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit = { padding ->
        if (usuario == null || !permitido) {
            ColunaTela(padding) { EstadoVazio("Acesso restrito", "Solicitante e operador não consultam auditoria.") }
        } else {
            val registros = runCatching { Consultas.auditoria(estado, usuario, texto, acao) }.getOrElse { emptyList() }
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Campo(texto, { texto = it }, "Buscar por pessoa, ação ou texto")
                    Campo(acao, { acao = it }, "Filtrar pela ação")
                }
                if (registros.isEmpty()) item { EstadoVazio("Nenhum registro", "Ajuste o filtro ou aguarde uma nova ação.") }
                items(registros, key = { it.id }) { registro ->
                    PainelCard("${registro.acao} · ${registro.entidade}") {
                        LinhaDado("Quem", registro.usuarioNome)
                        LinhaDado("Quando", Datas.exibirEpoch(registro.data))
                        LinhaDado("Antes", registro.antes.ifBlank { "—" })
                        LinhaDado("Depois", registro.depois.ifBlank { "—" })
                    }
                }
            }
        }
    }
    if (comVoltar) {
        TelaPadrao("Auditoria", onVoltar, conteudo = conteudo)
    } else {
        Casca("Auditoria", "auditoria", pessoa, onAba, conteudo)
    }
}
