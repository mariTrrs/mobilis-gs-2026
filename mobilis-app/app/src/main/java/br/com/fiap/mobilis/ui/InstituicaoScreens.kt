package br.com.fiap.mobilis.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.mobilis.data.ArquivosLocais
import br.com.fiap.mobilis.domain.Conservacao
import br.com.fiap.mobilis.domain.Consultas
import br.com.fiap.mobilis.domain.Datas
import br.com.fiap.mobilis.domain.Modalidade
import br.com.fiap.mobilis.domain.PapelInstituicao
import br.com.fiap.mobilis.domain.Perfil
import br.com.fiap.mobilis.domain.StatusConversao
import br.com.fiap.mobilis.domain.StatusEquipamento
import br.com.fiap.mobilis.domain.StatusPedido
import br.com.fiap.mobilis.domain.StatusProposta
import br.com.fiap.mobilis.domain.TipoManutencao
import br.com.fiap.mobilis.domain.Validacao
import br.com.fiap.mobilis.ui.componentes.Aviso
import br.com.fiap.mobilis.ui.componentes.BotaoPrincipal
import br.com.fiap.mobilis.ui.componentes.BotaoSecundario
import br.com.fiap.mobilis.ui.componentes.Campo
import br.com.fiap.mobilis.ui.componentes.CartaoAcao
import br.com.fiap.mobilis.ui.componentes.Casca
import br.com.fiap.mobilis.ui.componentes.ChipsMulti
import br.com.fiap.mobilis.ui.componentes.ChipsUnico
import br.com.fiap.mobilis.ui.componentes.ColunaTela
import br.com.fiap.mobilis.ui.componentes.EstadoVazio
import br.com.fiap.mobilis.ui.componentes.Etiqueta
import br.com.fiap.mobilis.ui.componentes.GraficoBarras
import br.com.fiap.mobilis.ui.componentes.ImagemQr
import br.com.fiap.mobilis.ui.componentes.LinhaDado
import br.com.fiap.mobilis.ui.componentes.MiniaturaEquipamento
import br.com.fiap.mobilis.ui.componentes.PainelCard
import br.com.fiap.mobilis.ui.componentes.TelaPadrao
import br.com.fiap.mobilis.ui.componentes.TituloSecao
import br.com.fiap.mobilis.viewmodel.MobilisViewModel
import coil.compose.AsyncImage
import java.io.File

@Composable
fun TelaInicioInstituicao(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    val instituicaoId = pessoa.instituicaoId ?: return
    if (pessoa.papel == PapelInstituicao.OPERADOR) {
        Casca("Mobilis", "home", pessoa, onAba) { padding ->
            ColunaTela(padding) {
                Text("Olá, ${pessoa.nome}", style = MaterialTheme.typography.headlineSmall)
                Text("Retirada e devolução são confirmadas com o QR Code do equipamento, no ponto da instituição.")
                CartaoAcao("Ler QR da retirada", "Confirma a entrega depois do termo aceito") { onAbrir("retirada") }
                CartaoAcao("Ler QR da devolução", "Registra o estado e envia para higienização") { onAbrir("devolucao") }
                CartaoAcao("Ver atrasos", "Empréstimos que passaram da data") { onAbrir("atrasos") }
            }
        }
        return
    }
    var tipoFiltro by rememberSaveable { mutableIntStateOf(-1) }
    var modalidadeFiltro by rememberSaveable { mutableStateOf("todas") }
    val tipoSelecionado = tipoFiltro.takeIf { it > 0 }
    val modalidadeSelecionada = when (modalidadeFiltro) {
        "emprestimo" -> Modalidade.EMPRESTIMO
        "doacao" -> Modalidade.DOACAO
        else -> null
    }
    val painel = Consultas.painel(estado, instituicaoId, tipoSelecionado, modalidadeSelecionada)
    val contexto = LocalContext.current
    val limite = estado.instituicoes.find { it.id == instituicaoId }?.limiteOciosidadeDias ?: 30
    val naFila = estado.filas.count {
        it.instituicaoId == instituicaoId && it.status == "AGUARDANDO" && (tipoSelecionado == null || it.tipoId == tipoSelecionado)
    }
    val noEstoque = estado.equipamentos.count {
        it.instituicaoId == instituicaoId && it.status != StatusEquipamento.BAIXADO && (tipoSelecionado == null || it.tipoId == tipoSelecionado)
    }
    val filtroTexto = buildString {
        append(if (tipoSelecionado == null) "Todos os tipos" else Consultas.nomeTipo(estado, tipoSelecionado))
        append(" · ")
        append(
            when (modalidadeSelecionada) {
                Modalidade.EMPRESTIMO -> "Empréstimo"
                Modalidade.DOACAO -> "Doação"
                null -> "Todas as modalidades"
            }
        )
    }
    Casca("Painel", "home", pessoa, onAba) { padding ->
        ColunaTela(padding, espaco = 20.dp) {
            Text(pessoa.instituicaoNome ?: "Instituição", style = MaterialTheme.typography.headlineSmall)
            MetricasPainel(
                "No estoque" to noEstoque.toString(),
                "Na fila" to naFila.toString(),
                "Atrasos" to painel.atrasos.size.toString(),
                "Parados" to painel.ociosos.size.toString()
            )
            TituloSecao("Filtros")
            ChipsUnico(listOf(-1) + estado.tipos.map { it.id }, tipoFiltro, { id -> if (id < 0) "Todos" else Consultas.nomeTipo(estado, id) }) { tipoFiltro = it }
            ChipsUnico(listOf("todas", "emprestimo", "doacao"), modalidadeFiltro, {
                when (it) { "emprestimo" -> "Empréstimo"; "doacao" -> "Doação"; else -> "Todos" }
            }) { modalidadeFiltro = it }
            GraficoBarras("Por situação", painel.porStatus)
            GraficoBarras("Por tipo", painel.porTipo)
            GraficoBarras("Por modalidade", painel.porModalidade)
            SecaoPainel(
                "Perto do vencimento",
                "Nenhum empréstimo vence nos próximos 7 dias.",
                painel.proximos.map { "${Consultas.nomeTipo(estado, it.tipoId)} · ${Datas.exibir(it.prazoDevolucao)}" }
            )
            SecaoPainel(
                "Atrasos",
                "Não há atraso.",
                painel.atrasos.map { "${Consultas.nomePessoa(estado, it.solicitanteId)} · venceu em ${Datas.exibir(it.prazoDevolucao)}" }
            )
            SecaoPainel(
                "Parados há $limite dias ou mais",
                "Nenhum alerta de ociosidade.",
                painel.ociosos.map { "${it.codigo} · ${Consultas.nomeTipo(estado, it.tipoId)} · ${Datas.diasDesde(it.disponivelDesde)} dias" }
            )
            SecaoPainel(
                "Previsão de demanda",
                "Ainda não há fila nem estoque para comparar.",
                painel.previsoes.map { previsao ->
                    if (previsao.suficiente) "${previsao.tipo}: fila ${previsao.fila}, disponíveis ${previsao.disponiveis}. ${previsao.criterio}" else "${previsao.tipo}: ${previsao.criterio}"
                }
            )
            SecaoPainel(
                "Não preciso mais",
                "Ninguém avisou que encerrou a necessidade.",
                painel.encerramentos.map { "${Consultas.nomeTipo(estado, it.tipoId)} · ${it.status.rotulo}" }
            )
            BotaoPrincipal("Gerar relatório em PDF") {
                ArquivosLocais.relatorioPdf(
                    contexto,
                    pessoa.instituicaoNome ?: "Instituição",
                    Datas.exibirEpoch(System.currentTimeMillis()),
                    filtroTexto,
                    listOf(
                        ArquivosLocais.SecaoRelatorio("Equipamentos por situação", painel.porStatus.map { "${it.rotulo}: ${it.quantidade}" }),
                        ArquivosLocais.SecaoRelatorio("Equipamentos por tipo", painel.porTipo.map { "${it.rotulo}: ${it.quantidade}" }),
                        ArquivosLocais.SecaoRelatorio("Pedidos por modalidade", painel.porModalidade.map { "${it.rotulo}: ${it.quantidade}" }),
                        ArquivosLocais.SecaoRelatorio("Perto do vencimento", painel.proximos.map { "${Consultas.nomeTipo(estado, it.tipoId)} · devolver até ${Datas.exibir(it.prazoDevolucao)}" }),
                        ArquivosLocais.SecaoRelatorio("Atrasos", painel.atrasos.map { "${Consultas.nomePessoa(estado, it.solicitanteId)} · venceu em ${Datas.exibir(it.prazoDevolucao)}" }),
                        ArquivosLocais.SecaoRelatorio("Parados há $limite dias ou mais", painel.ociosos.map { "${it.codigo} · ${Consultas.nomeTipo(estado, it.tipoId)} · ${Datas.diasDesde(it.disponivelDesde)} dias" }),
                        ArquivosLocais.SecaoRelatorio("Previsão de demanda", painel.previsoes.map { "${it.tipo}: fila ${it.fila}, disponíveis ${it.disponiveis}. ${it.criterio}" }),
                        ArquivosLocais.SecaoRelatorio("Não preciso mais", painel.encerramentos.map { "${Consultas.nomeTipo(estado, it.tipoId)} · ${it.status.rotulo}" })
                    )
                )
            }
        }
    }
}

@Composable
private fun MetricasPainel(vararg itens: Pair<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itens.toList().chunked(2).forEach { linha ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linha.forEach { (rotulo, valor) ->
                    Card(
                        Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(valor, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            Text(rotulo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecaoPainel(titulo: String, vazio: String, linhas: List<String>) {
    PainelCard(titulo) {
        if (linhas.isEmpty()) {
            Text(vazio, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            linhas.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
fun TelaEstoque(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    val itens = estado.equipamentos.filter { it.instituicaoId == pessoa.instituicaoId }
    Casca("Estoque", "estoque", pessoa, onAba) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { BotaoPrincipal("Cadastrar equipamento") { onAbrir("estoque/form/0") } }
            if (itens.isEmpty()) item { EstadoVazio("Estoque vazio", "Cadastre o primeiro equipamento da instituição.") }
            items(itens, key = { it.id }) { item ->
                val tipo = Consultas.nomeTipo(estado, item.tipoId)
                CartaoAcao(
                    "${item.codigo} · $tipo",
                    "${item.status.rotulo}. ${item.conservacao.rotulo}.",
                    miniatura = { MiniaturaEquipamento(tipo, item.fotos) }
                ) { onAbrir("estoque/${item.id}") }
            }
        }
    }
}

@Composable
fun TelaFormEquipamento(vm: MobilisViewModel, equipamentoId: Int, onOk: () -> Unit, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val atual = estado.equipamentos.find { it.id == equipamentoId }
    val tipos = estado.tipos.filter { it.ativo || it.id == atual?.tipoId }
    var tipoId by rememberSaveable { mutableIntStateOf(atual?.tipoId ?: tipos.firstOrNull()?.id ?: 0) }
    var conservacao by rememberSaveable { mutableStateOf(atual?.conservacao?.name ?: Conservacao.BOM.name) }
    var observacoes by rememberSaveable { mutableStateOf(atual?.observacoes ?: "") }
    var modalidades by rememberSaveable { mutableStateOf(atual?.modalidades?.map { it.name }?.toSet() ?: setOf(Modalidade.EMPRESTIMO.name)) }
    var fotos by rememberSaveable { mutableStateOf(atual?.fotos ?: emptyList()) }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }
    val contexto = LocalContext.current
    var fotoAtual by rememberSaveable { mutableStateOf<String?>(null) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (fotos.size >= 3) {
            erro = "É possível anexar no máximo 3 fotos."
            return@rememberLauncherForActivityResult
        }
        vm.copiarAnexo(uri, somenteImagem = true)?.let { fotos = fotos + it.second } ?: run { erro = "Use uma foto JPG ou PNG de até 5 MB." }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val caminho = fotoAtual
        if (!ok || caminho == null) return@rememberLauncherForActivityResult
        val arquivo = File(caminho)
        if (!arquivo.exists() || arquivo.length() > 5L * 1024 * 1024) {
            arquivo.delete()
            erro = "A foto precisa ser JPG de até 5 MB."
            return@rememberLauncherForActivityResult
        }
        fotos = fotos + caminho
    }
    TelaPadrao(if (equipamentoId == 0) "Novo equipamento" else "Editar equipamento", onVoltar) { padding ->
        ColunaTela(padding) {
            TituloSecao("Tipo")
            ChipsUnico(tipos, tipos.find { it.id == tipoId }, { it.nome }) { tipoId = it.id }
            TituloSecao("Conservação")
            ChipsUnico(Conservacao.entries, Conservacao.entries.find { it.name == conservacao }, { it.rotulo }) { conservacao = it.name }
            Campo(observacoes, { observacoes = it }, "Observações", linhas = 3)
            TituloSecao("Modalidades")
            ChipsMulti(Modalidade.entries, Modalidade.entries.filter { it.name in modalidades }.toSet(), { it.rotulo }) { opcao ->
                modalidades = if (opcao.name in modalidades) modalidades - opcao.name else modalidades + opcao.name
            }
            if (atual != null && Consultas.politicaEfetiva(estado, atual).itemPrevalece) {
                Aviso("A política deste item prevalece sobre a do tipo.")
            }
            TituloSecao("Fotos (${fotos.size} de 3)")
            fotos.forEachIndexed { indice, caminho ->
                AsyncImage(File(caminho), "Foto ${indice + 1} do equipamento", Modifier.fillMaxWidth().height(140.dp))
                BotaoSecundario("Remover foto ${indice + 1}") { fotos = fotos.filterNot { it == caminho } }
            }
            BotaoSecundario("Galeria") {
                if (fotos.size >= 3) erro = "É possível anexar no máximo 3 fotos."
                else galeria.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            BotaoSecundario("Câmera") {
                if (fotos.size >= 3) erro = "É possível anexar no máximo 3 fotos."
                else {
                    val (arquivo, uri) = ArquivosLocais.novoArquivoFoto(contexto)
                    fotoAtual = arquivo.absolutePath
                    camera.launch(uri)
                }
            }
            if (erro != null) Text(erro!!, color = MaterialTheme.colorScheme.error)
            BotaoPrincipal("Salvar") {
                val escolhidas = Modalidade.entries.filter { it.name in modalidades }
                if (escolhidas.isEmpty()) erro = "Selecione ao menos uma modalidade."
                else vm.salvarEquipamento(equipamentoId, tipoId, Conservacao.valueOf(conservacao), observacoes, escolhidas, fotos, onOk)
            }
        }
    }
}

@Composable
fun TelaDetalheEstoque(vm: MobilisViewModel, equipamentoId: Int, onEditar: () -> Unit, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val equipamento = estado.equipamentos.find { it.id == equipamentoId }
    var motivoBaixa by rememberSaveable { mutableStateOf("") }
    var tipoManutencao by rememberSaveable { mutableStateOf(TipoManutencao.HIGIENIZACAO.name) }
    var data by rememberSaveable { mutableStateOf(Datas.exibir(Datas.hoje())) }
    var responsavel by rememberSaveable { mutableStateOf("") }
    var notas by rememberSaveable { mutableStateOf("") }
    if (equipamento == null) {
        TelaPadrao("Equipamento", onVoltar) { Text("Não encontrado.", modifier = Modifier.padding(16.dp)) }
        return
    }
    val politica = Consultas.politicaEfetiva(estado, equipamento)
    val podeBaixar = equipamento.status == StatusEquipamento.DISPONIVEL || equipamento.status == StatusEquipamento.HIGIENIZACAO
    val contexto = LocalContext.current
    TelaPadrao(equipamento.codigo, onVoltar) { padding ->
        ColunaTela(padding) {
            PainelCard("Status e identificação") {
                MiniaturaEquipamento(Consultas.nomeTipo(estado, equipamento.tipoId), equipamento.fotos, larga = true)
                Etiqueta(equipamento.status.rotulo)
                LinhaDado("Tipo", Consultas.nomeTipo(estado, equipamento.tipoId))
                LinhaDado("Conservação", equipamento.conservacao.rotulo)
                LinhaDado("Modalidades", equipamento.modalidades.joinToString { it.rotulo })
                if (politica.itemPrevalece) Aviso("A política deste item prevalece sobre a do tipo.")
            }
            if (equipamento.observacoes.isNotBlank()) {
                PainelCard("Especificações e observações") {
                    LinhaDado("Observações", equipamento.observacoes)
                }
            }
            PainelCard("Etiqueta e QR Code") {
                ImagemQr("MOBILIS:${equipamento.codigo}", "QR Code do equipamento ${equipamento.codigo}")
                Text("Código impresso: ${equipamento.codigo}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                BotaoSecundario("Gerar etiqueta em PDF") { ArquivosLocais.etiqueta(contexto, equipamento.codigo, Consultas.nomeTipo(estado, equipamento.tipoId)) }
                if (equipamento.status != StatusEquipamento.DOADO && equipamento.status != StatusEquipamento.BAIXADO) {
                    BotaoSecundario("Editar", onClick = onEditar)
                }
                if (equipamento.status == StatusEquipamento.DISPONIVEL) {
                    BotaoSecundario("Enviar para higienização") { vm.enviarHigienizacao(equipamento.id) }
                }
            }
            PainelCard("Higienização ou reparo") {
                ChipsUnico(TipoManutencao.entries, TipoManutencao.entries.find { it.name == tipoManutencao }, { it.rotulo }) { tipoManutencao = it.name }
                Campo(data, { data = it }, "Data (dd/mm/aaaa)")
                Campo(responsavel, { responsavel = it }, "Responsável")
                Campo(notas, { notas = it }, "Observações", linhas = 2)
                BotaoPrincipal("Registrar") { vm.manutencao(equipamento.id, TipoManutencao.valueOf(tipoManutencao), data, responsavel, notas) }
            }
            if (podeBaixar) {
                PainelCard("Baixa") {
                    Campo(motivoBaixa, { motivoBaixa = it }, "Motivo da baixa")
                    BotaoSecundario("Dar baixa") { vm.baixar(equipamento.id, motivoBaixa) {} }
                }
            } else if (equipamento.status == StatusEquipamento.EMPRESTADO || equipamento.status == StatusEquipamento.DEVOLUCAO_PENDENTE) {
                Aviso("A baixa fica indisponível enquanto o item está emprestado.")
            }
            PainelCard("Histórico") {
                val eventos = (
                    estado.movimentacoes.filter { it.equipamentoId == equipamento.id }.map { it.em to "${Datas.exibirEpoch(it.em)} · ${it.descricao} (${it.de} → ${it.para})" } +
                        estado.manutencoes.filter { it.equipamentoId == equipamento.id }.map { it.registradoEm to "${it.data} · ${it.tipo.rotulo} por ${it.responsavel}. ${it.observacoes}" }
                    ).sortedByDescending { it.first }
                if (eventos.isEmpty()) Text("Ainda não há movimentação.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                eventos.forEach { Text(it.second) }
            }
        }
    }
}

@Composable
fun TelaPoliticas(vm: MobilisViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val instituicaoId = sessao?.instituicaoId
    if (sessao?.papel != PapelInstituicao.GESTOR) {
        TelaPadrao("Políticas", onVoltar) { Text("Apenas o gestor configura políticas.", modifier = Modifier.padding(16.dp)) }
        return
    }
    TelaPadrao("Políticas", onVoltar) { padding ->
        ColunaTela(padding) {
            Text("Escolha empréstimo, doação ou os dois. Sem modalidade, a política não é salva. A política de um item prevalece sobre a do tipo.")
            estado.tipos.filter { it.ativo }.forEach { tipo ->
                val atual = estado.politicasTipo.find { it.instituicaoId == instituicaoId && it.tipoId == tipo.id }
                PoliticaEditor(
                    titulo = tipo.nome,
                    modalidadesIniciais = atual?.modalidades ?: listOf(Modalidade.EMPRESTIMO, Modalidade.DOACAO),
                    prazoInicial = atual?.prazoDias ?: 30,
                    comprovanteInicial = atual?.exigeComprovanteDoacao ?: true,
                    aviso = null,
                    onSalvar = { modalidades, prazo, comprovante -> vm.salvarPoliticaTipo(tipo.id, modalidades, prazo, comprovante) }
                )
            }
            TituloSecao("Por equipamento")
            estado.equipamentos.filter { it.instituicaoId == instituicaoId && it.status != StatusEquipamento.BAIXADO }.forEach { equipamento ->
                val atual = estado.politicasItem.find { it.equipamentoId == equipamento.id }
                PoliticaEditor(
                    titulo = "${equipamento.codigo} · ${Consultas.nomeTipo(estado, equipamento.tipoId)}",
                    modalidadesIniciais = atual?.modalidades ?: equipamento.modalidades,
                    prazoInicial = atual?.prazoDias ?: 30,
                    comprovanteInicial = atual?.exigeComprovanteDoacao ?: true,
                    aviso = if (atual != null) "Esta política do item prevalece sobre a do tipo." else "Sem política própria, vale a do tipo.",
                    onSalvar = { modalidades, prazo, comprovante -> vm.salvarPoliticaItem(equipamento.id, modalidades, prazo, comprovante) },
                    onRemover = if (atual != null) { { vm.removerPoliticaItem(equipamento.id) } } else null
                )
            }
        }
    }
}

@Composable
private fun PoliticaEditor(
    titulo: String,
    modalidadesIniciais: List<Modalidade>,
    prazoInicial: Int,
    comprovanteInicial: Boolean,
    aviso: String?,
    onSalvar: (List<Modalidade>, Int, Boolean) -> Unit,
    onRemover: (() -> Unit)? = null
) {
    var modalidades by rememberSaveable(titulo) { mutableStateOf(modalidadesIniciais.map { it.name }.toSet()) }
    var prazo by rememberSaveable(titulo) { mutableStateOf(prazoInicial.toString()) }
    var comprovante by rememberSaveable(titulo) { mutableStateOf(comprovanteInicial) }
    PainelCard(titulo) {
        if (aviso != null) Text(aviso, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipsMulti(Modalidade.entries, Modalidade.entries.filter { it.name in modalidades }.toSet(), { it.rotulo }) { opcao ->
            modalidades = if (opcao.name in modalidades) modalidades - opcao.name else modalidades + opcao.name
        }
        Campo(prazo, { prazo = it.filter { c -> c.isDigit() } }, "Prazo padrão do empréstimo (dias)", teclado = KeyboardType.Number)
        ChipsUnico(listOf(true, false), comprovante, { if (it) "Exigir comprovante na doação" else "Doação sem comprovante" }) { comprovante = it }
        BotaoPrincipal("Salvar ${titulo.substringBefore(" ·")}") {
            onSalvar(Modalidade.entries.filter { it.name in modalidades }, prazo.toIntOrNull() ?: 0, comprovante)
        }
        if (onRemover != null) BotaoSecundario("Usar a política do tipo", onClick = onRemover)
    }
}

@Composable
fun TelaPedidosInstituicao(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    var filtro by rememberSaveable { mutableStateOf("todos") }
    val base = estado.pedidos.filter { it.instituicaoId == pessoa.instituicaoId }
    val pedidos = when (filtro) {
        "pendentes" -> base.filter { it.status == StatusPedido.AGUARDANDO_ANALISE }
        "propostas" -> base.filter { it.propostaStatus == StatusProposta.AGUARDANDO }
        "conversoes" -> base.filter { it.conversaoStatus == StatusConversao.AGUARDANDO }
        "prorrogacoes" -> base.filter { it.prorrogacaoStatus == StatusProposta.AGUARDANDO }
        else -> base
    }
    Casca("Pedidos", "pedidos", pessoa, onAba) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ChipsUnico(listOf("todos", "pendentes", "propostas", "conversoes", "prorrogacoes"), filtro, {
                    when (it) {
                        "pendentes" -> "Pendentes"
                        "propostas" -> "Propostas"
                        "conversoes" -> "Conversões"
                        "prorrogacoes" -> "Prorrogações"
                        else -> "Todos"
                    }
                }) { filtro = it }
            }
            if (pedidos.isEmpty()) item { EstadoVazio("Nada nesta lista", "Quando alguém solicitar um item, a análise aparece aqui.") }
            items(pedidos, key = { it.id }) { pedido ->
                val tipo = Consultas.nomeTipo(estado, pedido.tipoId)
                val fotos = estado.equipamentos.find { it.id == pedido.equipamentoId }?.fotos.orEmpty()
                CartaoAcao(
                    "${Consultas.nomePessoa(estado, pedido.solicitanteId)} · $tipo",
                    "${pedido.modalidade.rotulo}. ${pedido.status.rotulo}.",
                    miniatura = { MiniaturaEquipamento(tipo, fotos) }
                ) { onAbrir("analise/${pedido.id}") }
            }
        }
    }
}

@Composable
fun TelaAnalisePedido(vm: MobilisViewModel, pedidoId: Int, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pedido = estado.pedidos.find { it.id == pedidoId }
    val contexto = LocalContext.current
    var motivo by rememberSaveable { mutableStateOf("") }
    var motivoProposta by rememberSaveable { mutableStateOf("") }
    var motivoConversao by rememberSaveable { mutableStateOf("") }
    if (pedido == null) {
        TelaPadrao("Análise", onVoltar) { Text("Pedido não encontrado.", modifier = Modifier.padding(16.dp)) }
        return
    }
    val pessoa = estado.usuarios.find { it.id == pedido.solicitanteId }
    TelaPadrao("Análise", onVoltar) { padding ->
        ColunaTela(padding) {
            PainelCard("Resumo da solicitação") {
                val tipo = Consultas.nomeTipo(estado, pedido.tipoId)
                val fotos = estado.equipamentos.find { it.id == pedido.equipamentoId }?.fotos.orEmpty()
                MiniaturaEquipamento(tipo, fotos, larga = true)
                Etiqueta(pedido.status.rotulo)
                LinhaDado("Solicitante", pessoa?.nome ?: "")
                LinhaDado("Contato", "${pessoa?.email ?: ""} · ${Validacao.formatarTelefone(pessoa?.telefone ?: "")}")
                LinhaDado("Equipamento", "$tipo · ${estado.equipamentos.find { it.id == pedido.equipamentoId }?.codigo ?: ""}")
                LinhaDado("Modalidade", pedido.modalidade.rotulo)
                LinhaDado("Motivo", pedido.justificativa)
                if (pedido.validadoPorIndicacao) Aviso("Pedido validado por indicação de saúde.")
                if (!pedido.comprovanteCaminho.isNullOrBlank()) BotaoSecundario("Abrir comprovante") { ArquivosLocais.abrir(contexto, pedido.comprovanteCaminho) }
                if (!pedido.conversaoComprovanteCaminho.isNullOrBlank()) BotaoSecundario("Abrir comprovante da conversão") { ArquivosLocais.abrir(contexto, pedido.conversaoComprovanteCaminho) }
            }
            if (pedido.status == StatusPedido.AGUARDANDO_ANALISE) {
                PainelCard("Decisão") {
                    BotaoPrincipal("Aprovar") { vm.aprovarPedido(pedido.id) }
                    Campo(motivo, { motivo = it }, "Motivo da recusa")
                    BotaoSecundario("Recusar") { vm.recusarPedido(pedido.id, motivo) }
                }
            }
            if (pedido.propostaStatus == StatusProposta.AGUARDANDO) {
                PainelCard("Proposta: ${pedido.propostaModalidade?.rotulo}") {
                    BotaoPrincipal("Aceitar proposta") { vm.responderProposta(pedido.id, true, "") }
                    Campo(motivoProposta, { motivoProposta = it }, "Motivo se recusar a proposta")
                    BotaoSecundario("Recusar proposta") { vm.responderProposta(pedido.id, false, motivoProposta) }
                }
            }
            if (pedido.prorrogacaoStatus == StatusProposta.AGUARDANDO) {
                PainelCard("Prorrogação de 15 dias") {
                    LinhaDado("Devolução atual", Datas.exibir(pedido.prazoDevolucao))
                    BotaoPrincipal("Aceitar prorrogação") { vm.decidirProrrogacao(pedido.id, true, "") }
                    Campo(motivo, { motivo = it }, "Motivo se recusar a prorrogação")
                    BotaoSecundario("Recusar prorrogação") { vm.decidirProrrogacao(pedido.id, false, motivo) }
                }
            }
            if (pedido.conversaoStatus == StatusConversao.AGUARDANDO) {
                PainelCard("Conversão em doação") {
                    Text(pedido.conversaoJustificativa ?: "")
                    BotaoPrincipal("Permitir e remover a data de devolução") { vm.analisarConversao(pedido.id, true, "") }
                    Campo(motivoConversao, { motivoConversao = it }, "Motivo se negar")
                    BotaoSecundario("Negar e manter a data") { vm.analisarConversao(pedido.id, false, motivoConversao) }
                }
            }
        }
    }
}

@Composable
fun TelaOperacaoQr(vm: MobilisViewModel, devolucao: Boolean, onVoltar: () -> Unit) {
    var codigo by rememberSaveable { mutableStateOf("") }
    var conservacao by rememberSaveable { mutableStateOf(Conservacao.BOM.name) }
    var lido by rememberSaveable { mutableStateOf(false) }
    TelaPadrao(if (devolucao) "Registrar devolução" else "Confirmar retirada", onVoltar) { padding ->
        ColunaTela(padding) {
            Text(if (devolucao) "Leia o QR Code do item que está voltando. O estado informado leva o equipamento para higienização." else "Leia o QR Code depois que a pessoa aceitar o termo no aplicativo.")
            if (!lido) {
                br.com.fiap.mobilis.ui.componentes.CameraQr { valor ->
                    if (!lido) {
                        lido = true
                        codigo = valor.removePrefix("MOBILIS:")
                    }
                }
            }
            Campo(codigo, { codigo = it }, "Ou digite o código da etiqueta")
            if (devolucao) {
                TituloSecao("Estado na devolução")
                ChipsUnico(Conservacao.entries, Conservacao.entries.find { it.name == conservacao }, { it.rotulo }) { conservacao = it.name }
                BotaoPrincipal("Registrar devolução") { vm.devolver(codigo, Conservacao.valueOf(conservacao)) {} }
            } else {
                BotaoPrincipal("Confirmar entrega") { vm.retirada(codigo) {} }
            }
            BotaoSecundario("Ler de novo") { lido = false }
        }
    }
}

@Composable
fun TelaAtrasos(vm: MobilisViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val atrasos = estado.pedidos.filter {
        it.instituicaoId == sessao?.instituicaoId && it.status == StatusPedido.RETIRADO &&
            it.modalidade == Modalidade.EMPRESTIMO && Datas.vencida(it.prazoDevolucao)
    }
    TelaPadrao("Atrasos", onVoltar) { padding ->
        ColunaTela(padding) {
            if (atrasos.isEmpty()) EstadoVazio("Nenhum atraso", "Os empréstimos vencidos da instituição aparecem aqui.")
            atrasos.forEach {
                LinhaDado(Consultas.nomePessoa(estado, it.solicitanteId), "${Consultas.nomeTipo(estado, it.tipoId)} · venceu em ${Datas.exibir(it.prazoDevolucao)}")
            }
        }
    }
}

@Composable
fun TelaEquipe(vm: MobilisViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val codigo by vm.codigoDemonstracao.collectAsStateWithLifecycle()
    val instituicaoId = sessao?.instituicaoId
    if (sessao?.papel != PapelInstituicao.GESTOR) {
        TelaPadrao("Equipe", onVoltar) { Text("Apenas o gestor gerencia a equipe.", modifier = Modifier.padding(16.dp)) }
        return
    }
    var email by rememberSaveable { mutableStateOf("") }
    val equipe = estado.usuarios.filter { it.instituicaoId == instituicaoId && it.perfil == Perfil.INSTITUICAO }
    val gestores = equipe.count { it.papel == PapelInstituicao.GESTOR && it.ativo }
    val convites = estado.convites.filter { it.instituicaoId == instituicaoId && it.status == "PENDENTE" }
    TelaPadrao("Equipe", onVoltar) { padding ->
        ColunaTela(padding) {
            Campo(email, { email = it }, "E-mail do convite", teclado = KeyboardType.Email)
            BotaoPrincipal("Convidar operador") { vm.convidar(email) }
            if (!codigo.isNullOrBlank()) Aviso("Código do convite para demonstração: $codigo. O e-mail automático ainda depende de um serviço externo.")
            TituloSecao("Pessoas")
            equipe.forEach { membro ->
                val unico = membro.papel == PapelInstituicao.GESTOR && gestores <= 1
                LinhaDado(membro.nome, "${membro.papel?.rotulo} · ${if (membro.ativo) "ativo" else "inativo"} · ${membro.email}")
                if (membro.ativo) {
                    val outro = if (membro.papel == PapelInstituicao.GESTOR) PapelInstituicao.OPERADOR else PapelInstituicao.GESTOR
                    BotaoSecundario("Mudar para ${outro.rotulo}") { vm.alterarPapel(membro.id, outro) }
                    if (unico) Text("O único gestor não pode se desativar.")
                    else BotaoSecundario("Desativar") { vm.desativarMembro(membro.id) }
                }
            }
            TituloSecao("Convites pendentes")
            if (convites.isEmpty()) Text("Nenhum convite pendente.")
            convites.forEach { Text("${it.email} · código ${it.codigo}") }
        }
    }
}

@Composable
fun TelaAvaliacoes(vm: MobilisViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val gestor = sessao
    if (gestor?.papel != PapelInstituicao.GESTOR) {
        TelaPadrao("Avaliações", onVoltar) { Text("Apenas o gestor consulta avaliações.", modifier = Modifier.padding(16.dp)) }
        return
    }
    val lista = estado.avaliacoes.filter { it.instituicaoId == gestor.instituicaoId }
    TelaPadrao("Avaliações", onVoltar) { padding ->
        ColunaTela(padding) {
            Text("As avaliações não mostram o nome de quem avaliou.")
            if (lista.isEmpty()) EstadoVazio("Nenhuma avaliação", "Elas aparecem depois da devolução ou da doação.")
            lista.forEach {
                LinhaDado("Atendimento ${it.notaAtendimento} de 5", "Equipamento ${it.notaEquipamento} de 5. ${it.comentario.ifBlank { "Sem comentário." }}")
            }
        }
    }
}

@Composable
fun TelaMais(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    var sair by rememberSaveable { mutableStateOf(false) }
    Casca("Mais", "mais", pessoa, onAba) { padding ->
        ColunaTela(padding) {
            CartaoAcao("Confirmar retirada", "Ler o QR Code na entrega") { onAbrir("retirada") }
            CartaoAcao("Registrar devolução", "Ler o QR Code na volta do item") { onAbrir("devolucao") }
            CartaoAcao("Atrasos", "Empréstimos vencidos") { onAbrir("atrasos") }
            if (pessoa.papel == PapelInstituicao.GESTOR) {
                CartaoAcao("Políticas", "Modalidade, prazo e comprovante") { onAbrir("politicas") }
                CartaoAcao("Equipe", "Convidar, mudar papel ou desativar") { onAbrir("equipe") }
                CartaoAcao("Auditoria", "Quem fez o quê, com o antes e o depois") { onAbrir("auditoria") }
                CartaoAcao("Avaliações", "Notas anônimas do atendimento") { onAbrir("avaliacoes") }
            }
            BotaoSecundario("Sair da conta") { sair = true }
        }
    }
    if (sair) {
        AlertDialog(
            onDismissRequest = { sair = false },
            title = { Text("Sair da conta") },
            text = { Text("O acesso da instituição continua salvo neste aparelho até você entrar de novo.") },
            confirmButton = { TextButton(onClick = vm::sair) { Text("Sair") } },
            dismissButton = { TextButton(onClick = { sair = false }) { Text("Voltar") } }
        )
    }
}
