package br.com.fiap.mobilis.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.mobilis.data.ArquivosLocais
import br.com.fiap.mobilis.domain.Consultas
import br.com.fiap.mobilis.domain.Datas
import br.com.fiap.mobilis.domain.Geo
import br.com.fiap.mobilis.domain.Modalidade
import br.com.fiap.mobilis.domain.OpcaoFila
import br.com.fiap.mobilis.domain.Pedido
import br.com.fiap.mobilis.domain.ResultadoBusca
import br.com.fiap.mobilis.domain.StatusPedido
import br.com.fiap.mobilis.domain.Textos
import br.com.fiap.mobilis.domain.Validacao
import br.com.fiap.mobilis.ui.componentes.Aceite
import br.com.fiap.mobilis.ui.componentes.Aviso
import br.com.fiap.mobilis.ui.componentes.BotaoPrincipal
import br.com.fiap.mobilis.ui.componentes.BotaoSecundario
import br.com.fiap.mobilis.ui.componentes.Campo
import br.com.fiap.mobilis.ui.componentes.CartaoAcao
import br.com.fiap.mobilis.ui.componentes.Casca
import br.com.fiap.mobilis.ui.componentes.ChipsUnico
import br.com.fiap.mobilis.ui.componentes.ColunaTela
import br.com.fiap.mobilis.ui.componentes.EstadoVazio
import br.com.fiap.mobilis.ui.componentes.Etiqueta
import br.com.fiap.mobilis.ui.componentes.LinhaDado
import br.com.fiap.mobilis.ui.componentes.MiniaturaEquipamento
import br.com.fiap.mobilis.ui.componentes.PainelCard
import br.com.fiap.mobilis.ui.componentes.TelaPadrao
import br.com.fiap.mobilis.ui.componentes.TituloSecao
import br.com.fiap.mobilis.viewmodel.MobilisViewModel

@Composable
fun TelaInicioSolicitante(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    val meus = estado.pedidos.filter { it.solicitanteId == pessoa.usuarioId }
    Casca("Mobilis", "home", pessoa, onAba) { padding ->
        ColunaTela(padding) {
            Text("Olá, ${pessoa.nome}", style = MaterialTheme.typography.headlineSmall)
            Text("Você pode buscar um equipamento perto de você, entrar na fila ou acompanhar um pedido. A retirada é na instituição.")
            meus.filter { it.status == StatusPedido.RETIRADO || it.status == StatusPedido.APROVADO }.forEach { pedido ->
                val tipo = Consultas.nomeTipo(estado, pedido.tipoId)
                val fotos = estado.equipamentos.find { it.id == pedido.equipamentoId }?.fotos.orEmpty()
                val devolver = devolverAgora(pedido)
                CartaoAcao(
                    "$tipo · ${pedido.status.rotulo}",
                    if (devolver) {
                        "A data passou. Leve o equipamento de volta à instituição."
                    } else if (pedido.modalidade == Modalidade.EMPRESTIMO && pedido.prazoDevolucao != null) {
                        "Devolução em ${Datas.exibir(pedido.prazoDevolucao)}"
                    } else {
                        "Retirada até ${Datas.exibir(pedido.prazoRetirada)}"
                    },
                    selo = if (devolver) "Devolva o equipamento" else null,
                    miniatura = { MiniaturaEquipamento(tipo, fotos) }
                ) { onAbrir("pedido/${pedido.id}") }
            }
            if (meus.isEmpty()) EstadoVazio("Nenhum pedido ainda", "Comece pela busca. O app mostra só instituições já aprovadas.")
            BotaoPrincipal("Buscar equipamento") { onAba("busca") }
            val indicacoes = estado.indicacoes.filter {
                it.status == "PENDENTE" && it.pacienteCpf == estado.usuarios.find { u -> u.id == pessoa.usuarioId }?.documento
            }
            if (indicacoes.isNotEmpty()) {
                Aviso("Há ${indicacoes.size} indicação de profissional esperando a sua confirmação. Abra Perfil e depois Indicação.")
            }
        }
    }
}

@Composable
fun TelaBusca(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    val usuario = estado.usuarios.find { it.id == pessoa.usuarioId }
    var tipoId by rememberSaveable { mutableIntStateOf(-1) }
    var modalidade by rememberSaveable { mutableStateOf("todas") }
    var raio by rememberSaveable { mutableStateOf(10) }
    var cep by rememberSaveable { mutableStateOf(usuario?.cep.orEmpty()) }
    var mensagem by rememberSaveable { mutableStateOf<String?>(null) }
    var resultados by remember { mutableStateOf(listOf<Int>()) }
    var filas by remember { mutableStateOf(listOf<String>()) }
    var buscando by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(tipoId, modalidade, raio, cep) {
        if (Validacao.cep(cep) != null) return@LaunchedEffect
        val ponto = Geo.coordenadaDoCep(cep)
        buscando = true
        val filtro = when (modalidade) {
            "emprestimo" -> Modalidade.EMPRESTIMO
            "doacao" -> Modalidade.DOACAO
            else -> null
        }
        val resposta = vm.pesquisar(tipoId.takeIf { it > 0 }, filtro, raio.toDouble(), ponto.first, ponto.second)
        buscando = false
        resposta.onSuccess { pesquisa ->
            resultados = pesquisa.itens.map { it.equipamento.id }
            filas = pesquisa.filas.map { "${it.instituicao.id}:${it.tipo.id}" }
            mensagem = when {
                pesquisa.itens.isEmpty() && pesquisa.filas.isNotEmpty() -> "Nada disponível neste raio. Você pode entrar na fila."
                pesquisa.itens.isEmpty() && pesquisa.existemForaDoRaio -> "Não há item neste raio. Aumente a distância."
                pesquisa.itens.isEmpty() -> "Nenhum equipamento disponível com esses filtros."
                else -> "Busca concluída. Resultados ordenados por distância."
            }
        }.onFailure { mensagem = it.message }
    }

    val pontoBusca = if (Validacao.cep(cep) == null) Geo.coordenadaDoCep(cep) else null
    val itens = resultados.mapNotNull { id ->
        val equipamento = estado.equipamentos.find { it.id == id } ?: return@mapNotNull null
        val instituicao = estado.instituicoes.find { it.id == equipamento.instituicaoId } ?: return@mapNotNull null
        val tipo = estado.tipos.find { it.id == equipamento.tipoId } ?: return@mapNotNull null
        val distancia = if (pontoBusca == null) 0.0 else Geo.distanciaKm(pontoBusca.first, pontoBusca.second, instituicao.latitude, instituicao.longitude)
        ResultadoBusca(equipamento, instituicao, tipo, distancia, Consultas.politicaEfetiva(estado, equipamento))
    }
    val opcoesFila = filas.mapNotNull { chave ->
        val partes = chave.split(":")
        val instituicao = estado.instituicoes.find { it.id == partes.getOrNull(0)?.toIntOrNull() } ?: return@mapNotNull null
        val tipo = estado.tipos.find { it.id == partes.getOrNull(1)?.toIntOrNull() } ?: return@mapNotNull null
        OpcaoFila(instituicao, tipo, 0.0, estado.filas.any { it.solicitanteId == pessoa.usuarioId && it.instituicaoId == instituicao.id && it.tipoId == tipo.id })
    }

    Casca("Buscar", "busca", pessoa, onAba) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Instituições aprovadas, ordenadas pela distância. A entrega em casa não faz parte do Mobilis.")
                TituloSecao("Tipo")
                ChipsUnico(listOf(-1) + estado.tipos.filter { it.ativo }.map { it.id }, tipoId, { id ->
                    if (id < 0) "Todos" else estado.tipos.find { it.id == id }?.nome ?: ""
                }) { tipoId = it }
                TituloSecao("Modalidade")
                ChipsUnico(listOf("todas", "emprestimo", "doacao"), modalidade, {
                    when (it) { "emprestimo" -> "Empréstimo"; "doacao" -> "Doação"; else -> "Todos" }
                }) { modalidade = it }
                TituloSecao("Raio")
                ChipsUnico(listOf(5, 10, 25, 50), raio, { "$it km" }) { raio = it }
                Campo(cep, { cep = it }, "CEP", teclado = KeyboardType.Number)
                if (buscando) Text("Buscando...")
                if (mensagem != null) Text(mensagem!!)
            }
            items(itens, key = { it.equipamento.id }) { item ->
                CartaoAcao(
                    "${item.tipo.nome} · ${Geo.formatarKm(item.distanciaKm)}",
                    "${item.instituicao.razaoSocial}. ${item.politica.modalidades.joinToString { it.rotulo }}. ${item.equipamento.conservacao.rotulo}.",
                    miniatura = { MiniaturaEquipamento(item.tipo.nome, item.equipamento.fotos) }
                ) { onAbrir("equipamento/${item.equipamento.id}") }
            }
            items(opcoesFila, key = { "${it.instituicao.id}-${it.tipo.id}" }) { opcao ->
                CartaoAcao(
                    if (opcao.jaNaFila) "Você já está na fila de ${opcao.tipo.nome}" else "Entrar na fila de ${opcao.tipo.nome}",
                    opcao.instituicao.razaoSocial
                ) {
                    if (!opcao.jaNaFila) vm.entrarFila(opcao.instituicao.id, opcao.tipo.id)
                }
            }
        }
    }
}

@Composable
fun TelaDetalheEquipamentoPublico(vm: MobilisViewModel, equipamentoId: Int, onPedir: () -> Unit, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val equipamento = estado.equipamentos.find { it.id == equipamentoId }
    if (equipamento == null) {
        TelaPadrao("Equipamento", onVoltar) { EstadoVazio("Item não encontrado", "Volte e busque de novo.") }
        return
    }
    val instituicao = estado.instituicoes.find { it.id == equipamento.instituicaoId }
    val tipo = estado.tipos.find { it.id == equipamento.tipoId }
    val politica = Consultas.politicaEfetiva(estado, equipamento)
    val nomeTipo = tipo?.nome ?: "Equipamento"
    TelaPadrao(nomeTipo, onVoltar) { padding ->
        ColunaTela(padding) {
            MiniaturaEquipamento(nomeTipo, equipamento.fotos, larga = true)
            Etiqueta(equipamento.status.rotulo)
            LinhaDado("Estado de conservação", equipamento.conservacao.rotulo)
            LinhaDado("Modalidades", politica.modalidades.joinToString { it.rotulo })
            if (politica.itemPrevalece) Aviso("A política deste item prevalece sobre a do tipo.")
            LinhaDado("Para quem serve", tipo?.descricao ?: "")
            LinhaDado("Orientação", tipo?.orientacao ?: "")
            if (equipamento.observacoes.isNotBlank()) LinhaDado("Observações", equipamento.observacoes)
            LinhaDado("Instituição", instituicao?.razaoSocial ?: "")
            LinhaDado("Endereço", instituicao?.endereco ?: "")
            LinhaDado("Horário de retirada", instituicao?.horarioRetirada ?: "")
            Text("A retirada e a devolução acontecem neste endereço. Não há entrega em domicílio.")
            if (equipamento.status.name == "DISPONIVEL") BotaoPrincipal("Solicitar", onClick = onPedir)
        }
    }
}

@Composable
fun TelaNovoPedido(vm: MobilisViewModel, equipamentoId: Int, onOk: () -> Unit, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val equipamento = estado.equipamentos.find { it.id == equipamentoId }
    if (equipamento == null) {
        TelaPadrao("Solicitar", onVoltar) { Text("Equipamento não encontrado.", modifier = Modifier.padding(16.dp)) }
        return
    }
    val politica = Consultas.politicaEfetiva(estado, equipamento)
    var modalidade by rememberSaveable { mutableStateOf(politica.modalidades.first().name) }
    var motivo by rememberSaveable { mutableStateOf("") }
    var anexoNome by rememberSaveable { mutableStateOf<String?>(null) }
    var anexoCaminho by rememberSaveable { mutableStateOf<String?>(null) }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }
    val processando by vm.processando.collectAsStateWithLifecycle()
    val contextoFoto = LocalContext.current
    val escolher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.copiarAnexo(uri)?.let { (nome, caminho) ->
            anexoNome = nome
            anexoCaminho = caminho
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && anexoCaminho != null) anexoNome = "foto-comprovante.jpg"
    }
    val escolhida = Modalidade.entries.find { it.name == modalidade } ?: politica.modalidades.first()
    TelaPadrao("Solicitar reserva", onVoltar) { padding ->
        ColunaTela(padding) {
            Text("Prazo para retirada, se a instituição aprovar: ${Datas.exibir(Datas.maisDias(3))}.")
            TituloSecao("Modalidade permitida")
            ChipsUnico(politica.modalidades, escolhida, { it.rotulo }) { modalidade = it.name }
            Campo(motivo, { motivo = it }, "Motivo", linhas = 3)
            Text(anexoNome ?: "Nenhum comprovante anexado.")
            BotaoSecundario("Anexar arquivo") { escolher.launch(arrayOf("*/*")) }
            BotaoSecundario("Tirar foto do comprovante") {
                val (arquivo, uri) = ArquivosLocais.novoArquivoFoto(contextoFoto)
                anexoCaminho = arquivo.absolutePath
                anexoNome = "foto-comprovante.jpg"
                camera.launch(uri)
            }
            if (erro != null) Text(erro!!, color = MaterialTheme.colorScheme.error)
            BotaoPrincipal("Enviar pedido", !processando) {
                if (motivo.trim().length < 10) {
                    erro = "Descreva o motivo com pelo menos 10 caracteres."
                } else if (escolhida == Modalidade.DOACAO && politica.exigeComprovanteDoacao && anexoCaminho.isNullOrBlank()) {
                    erro = "Anexe o comprovante para a doação."
                } else {
                    erro = null
                    vm.criarPedido(equipamentoId, escolhida, motivo, anexoNome, anexoCaminho, onOk)
                }
            }
        }
    }
}

@Composable
fun TelaPedidosSolicitante(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val pessoa = sessao ?: return
    val pedidos = estado.pedidos.filter { it.solicitanteId == pessoa.usuarioId }.sortedByDescending { it.criadoEm }
    Casca("Meus pedidos", "pedidos", pessoa, onAba) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (pedidos.isEmpty()) item { EstadoVazio("Nenhum pedido", "Quando você solicitar um equipamento, ele aparece aqui com o status.") }
            items(pedidos, key = { it.id }) { pedido ->
                val tipo = Consultas.nomeTipo(estado, pedido.tipoId)
                val fotos = estado.equipamentos.find { it.id == pedido.equipamentoId }?.fotos.orEmpty()
                val devolver = devolverAgora(pedido)
                CartaoAcao(
                    "$tipo · ${pedido.status.rotulo}",
                    "${pedido.modalidade.rotulo}. Aberto em ${Datas.exibirEpoch(pedido.criadoEm)}.",
                    selo = if (devolver) "Devolva o equipamento" else null,
                    miniatura = { MiniaturaEquipamento(tipo, fotos) }
                ) { onAbrir("pedido/${pedido.id}") }
            }
        }
    }
}

@Composable
fun TelaDetalhePedido(vm: MobilisViewModel, pedidoId: Int, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val pedido = estado.pedidos.find { it.id == pedidoId }
    val contexto = LocalContext.current
    if (pedido == null || sessao == null) {
        TelaPadrao("Pedido", onVoltar) { Text("Pedido não encontrado.", modifier = Modifier.padding(16.dp)) }
        return
    }
    val equipamento = estado.equipamentos.find { it.id == pedido.equipamentoId }
    val politica = equipamento?.let { Consultas.politicaEfetiva(estado, it) }
    var termo by rememberSaveable { mutableStateOf(false) }
    var termoDoacao by rememberSaveable { mutableStateOf(false) }
    var notaAtendimento by rememberSaveable { mutableIntStateOf(0) }
    var notaEquipamento by rememberSaveable { mutableIntStateOf(0) }
    var comentario by rememberSaveable { mutableStateOf("") }
    var conversaoTexto by rememberSaveable { mutableStateOf("") }
    var conversaoCaminho by rememberSaveable { mutableStateOf<String?>(null) }
    var conversaoNome by rememberSaveable { mutableStateOf<String?>(null) }
    val arquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.copiarAnexo(uri)?.let { (nome, caminho) ->
            conversaoNome = nome
            conversaoCaminho = caminho
            vm.anexar(pedido.id, nome, caminho)
        }
    }
    val podeCancelar = pedido.status == StatusPedido.AGUARDANDO_ANALISE || pedido.status == StatusPedido.APROVADO
    val emprestimoAtivo = pedido.status == StatusPedido.RETIRADO && pedido.modalidade == Modalidade.EMPRESTIMO
    val vencido = Datas.vencida(pedido.prazoDevolucao)
    val nomeTipo = Consultas.nomeTipo(estado, pedido.tipoId)
    TelaPadrao("Pedido", onVoltar) { padding ->
        ColunaTela(padding) {
            MiniaturaEquipamento(nomeTipo, equipamento?.fotos.orEmpty(), larga = true)
            if (devolverAgora(pedido)) Etiqueta("Devolva o equipamento", alerta = true)
            Etiqueta(pedido.status.rotulo)
            LinhaDado("Equipamento", nomeTipo)
            LinhaDado("Modalidade", pedido.modalidade.rotulo)
            LinhaDado("Motivo", pedido.justificativa)
            if (pedido.validadoPorIndicacao) Aviso("Este pedido já veio validado por uma indicação de saúde.")
            LinhaDado("Prazo de retirada", Datas.exibir(pedido.prazoRetirada))
            if (pedido.modalidade == Modalidade.EMPRESTIMO) LinhaDado("Devolução", Datas.exibir(pedido.prazoDevolucao))
            if (!pedido.motivoRecusa.isNullOrBlank()) Aviso("Recusa: ${pedido.motivoRecusa}")
            if (pedido.propostaStatus != null) {
                Aviso("Proposta de ${pedido.propostaModalidade?.rotulo}: ${pedido.propostaStatus.rotulo}. ${pedido.propostaMotivo ?: ""}")
            }
            if (pedido.conversaoStatus != null) {
                Aviso("Conversão em doação: ${pedido.conversaoStatus.rotulo}. ${pedido.conversaoMotivo ?: ""}")
            }
            if (!pedido.comprovanteCaminho.isNullOrBlank()) {
                BotaoSecundario("Abrir comprovante") { ArquivosLocais.abrir(contexto, pedido.comprovanteCaminho) }
            } else if (pedido.modalidade == Modalidade.DOACAO) {
                BotaoSecundario("Anexar comprovante") { arquivo.launch(arrayOf("*/*")) }
            }
            if (pedido.status == StatusPedido.APROVADO && !pedido.termoAceito) {
                TituloSecao("Termo para retirada")
                Text(Textos.termoCessao(pedido.modalidade, sessao?.instituicaoNome ?: "instituição", Consultas.nomeTipo(estado, pedido.tipoId), Datas.exibir(pedido.prazoDevolucao ?: Datas.maisDias(politica?.prazoDias ?: 30))))
                Aceite(termo, { termo = it }, "Li e aceito o termo")
                BotaoPrincipal("Aceitar termo", termo) { vm.aceitarTermo(pedido.id) }
            }
            if (podeCancelar) BotaoSecundario("Cancelar pedido") { vm.cancelar(pedido.id) }
            if (podeCancelar && politica != null) {
                val outras = politica.modalidades.filter { it != pedido.modalidade }
                if (outras.isNotEmpty() && pedido.propostaStatus != br.com.fiap.mobilis.domain.StatusProposta.AGUARDANDO) {
                    TituloSecao("Propor outra modalidade")
                    outras.forEach { opcao ->
                        BotaoSecundario("Propor ${opcao.rotulo}") { vm.propor(pedido.id, opcao) }
                    }
                }
            }
            if (pedido.prorrogacaoStatus != null) Aviso("Prorrogação: ${pedido.prorrogacaoStatus.rotulo}.")
            if (pedido.atrasoGrave) Aviso("Este empréstimo está em atraso grave. Novos pedidos ficam bloqueados até a devolução.")
            if (emprestimoAtivo && vencido) {
                Aviso("A data de devolução passou. Leve o equipamento de volta à instituição. A prorrogação só pode ser pedida enquanto o prazo ainda está vigente.")
            }
            if (emprestimoAtivo && !vencido && !pedido.prorrogado && pedido.prorrogacaoStatus != br.com.fiap.mobilis.domain.StatusProposta.AGUARDANDO) {
                PainelCard("Prorrogar o prazo") {
                    Text("Este empréstimo ainda está no prazo. O pedido vale por mais 15 dias e a instituição analisa em Pedidos, no filtro Prorrogações.")
                    BotaoSecundario("Pedir prorrogação de 15 dias") { vm.prorrogar(pedido.id) }
                }
            }
            if (emprestimoAtivo && !vencido && pedido.conversaoStatus != br.com.fiap.mobilis.domain.StatusConversao.AGUARDANDO && pedido.conversaoStatus != br.com.fiap.mobilis.domain.StatusConversao.PERMITIDA) {
                TituloSecao("Converter em doação")
                Campo(conversaoTexto, { conversaoTexto = it }, "Por que a doação faz sentido?", linhas = 3)
                BotaoSecundario("Anexar comprovante da conversão") { arquivo.launch(arrayOf("*/*")) }
                Text(conversaoNome ?: "Comprovante ainda não anexado.")
                BotaoPrincipal("Pedir conversão") { vm.pedirConversao(pedido.id, conversaoTexto, conversaoNome, conversaoCaminho) }
            }
            if (pedido.conversaoStatus == br.com.fiap.mobilis.domain.StatusConversao.PERMITIDA && !pedido.termoDoacaoAceito) {
                Text(Textos.termoCessao(Modalidade.DOACAO, "instituição", Consultas.nomeTipo(estado, pedido.tipoId), null))
                Aceite(termoDoacao, { termoDoacao = it }, "Aceito o termo de doação")
                BotaoPrincipal("Aceitar doação", termoDoacao) { vm.aceitarDoacao(pedido.id) }
            }
            if ((emprestimoAtivo || pedido.status == StatusPedido.DOADO) && !pedido.naoPrecisaMais) {
                BotaoSecundario("Não preciso mais") { vm.naoPreciso(pedido.id) }
            }
            if ((pedido.status == StatusPedido.DEVOLVIDO || pedido.status == StatusPedido.DOADO) && !pedido.avaliado) {
                TituloSecao("Avaliar")
                Text("Nota do atendimento")
                ChipsUnico((1..5).toList(), notaAtendimento.takeIf { it > 0 }, { "$it" }) { notaAtendimento = it }
                Text("Nota do equipamento")
                ChipsUnico((1..5).toList(), notaEquipamento.takeIf { it > 0 }, { "$it" }) { notaEquipamento = it }
                Campo(comentario, { comentario = it }, "Comentário (opcional)", linhas = 2)
                BotaoPrincipal("Enviar avaliação") { vm.avaliar(pedido.id, notaAtendimento, notaEquipamento, comentario) }
            }
        }
    }
}

@Composable
fun TelaFila(vm: MobilisViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val filas = estado.filas.filter { it.solicitanteId == sessao?.usuarioId && it.status == "AGUARDANDO" }
    TelaPadrao("Fila de espera", onVoltar) { padding ->
        ColunaTela(padding) {
            if (filas.isEmpty()) EstadoVazio("Você não está em nenhuma fila", "A fila aparece quando a busca não encontra item disponível.")
            filas.forEach { fila ->
                val instituicao = estado.instituicoes.find { it.id == fila.instituicaoId }?.razaoSocial ?: "Instituição"
                CartaoAcao(Consultas.nomeTipo(estado, fila.tipoId), instituicao) {}
                BotaoSecundario("Sair desta fila") { vm.sairFila(fila.id) }
            }
        }
    }
}

@Composable
fun TelaNotificacoes(vm: MobilisViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val usuario = estado.usuarios.find { it.id == sessao?.usuarioId }
    var push by rememberSaveable { mutableStateOf(usuario?.notificarPush ?: true) }
    var email by rememberSaveable { mutableStateOf(usuario?.notificarEmail ?: true) }
    TelaPadrao("Notificações", onVoltar) { padding ->
        ColunaTela(padding) {
            val avisos = estado.notificacoes.filter { it.usuarioId == sessao?.usuarioId }.sortedByDescending { it.criadaEm }
            TituloSecao("Caixa de avisos")
            if (avisos.isEmpty()) EstadoVazio("Nenhum aviso ainda", "Decisões, fila e prazos aparecem aqui.")
            avisos.forEach { aviso ->
                PainelCard {
                    if (!aviso.lida) Etiqueta("Nova")
                    Text(aviso.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(aviso.texto)
                    Text(Datas.exibirEpoch(aviso.criadaEm), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!aviso.lida) BotaoSecundario("Marcar como lida") { vm.marcarAviso(aviso.id) }
                }
            }
            TituloSecao("Canais")
            Aceite(push, { push = it }, "Receber lembretes de prazo")
            Aceite(email, { email = it }, "Manter o canal de e-mail marcado")
            BotaoPrincipal("Salvar preferências") { vm.notificacoes(push, email) }
        }
    }
}

@Composable
fun TelaIndicacao(vm: MobilisViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val documento = estado.usuarios.find { it.id == sessao?.usuarioId }?.documento
    var unidade by rememberSaveable { mutableStateOf("") }
    var nome by rememberSaveable { mutableStateOf("") }
    var cpf by rememberSaveable { mutableStateOf("") }
    var registro by rememberSaveable { mutableStateOf("") }
    var tipoId by rememberSaveable { mutableStateOf(estado.tipos.firstOrNull { it.ativo }?.id ?: 0) }
    val minhas = estado.indicacoes.filter { it.pacienteCpf == documento && it.status == "PENDENTE" }
    TelaPadrao("Indicação de saúde", onVoltar) { padding ->
        ColunaTela(padding) {
            if (minhas.isNotEmpty()) {
                TituloSecao("Para confirmar")
                minhas.forEach { indicacao ->
                    Text("${indicacao.unidade} (${indicacao.registroProfissional.ifBlank { indicacao.profissionalNome }}) indicou ${Consultas.nomeTipo(estado, indicacao.tipoId)} para ${indicacao.pacienteNome}.")
                    BotaoPrincipal("Confirmar e abrir pedido") { vm.confirmarIndicacao(indicacao.id) }
                }
            }
            TituloSecao("Registrar indicação")
            Campo(unidade, { unidade = it }, "Unidade de saúde")
            Campo(nome, { nome = it }, "Nome do paciente")
            Campo(cpf, { cpf = it }, "CPF do paciente", teclado = KeyboardType.Number)
            Campo(registro, { registro = it }, "Registro profissional")
            ChipsUnico(estado.tipos.filter { it.ativo }, estado.tipos.find { it.id == tipoId }, { it.nome }) { tipoId = it.id }
            BotaoPrincipal("Registrar") { vm.indicacao(unidade, nome, cpf, tipoId, registro) {} }
        }
    }
}

@Composable
fun TelaMeusDados(vm: MobilisViewModel, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val usuario = estado.usuarios.find { it.id == sessao?.usuarioId }
    val contexto = LocalContext.current
    var confirmar by rememberSaveable { mutableStateOf(false) }
    val bloqueios = sessao?.let { Consultas.bloqueiosExclusao(estado, it.usuarioId) }.orEmpty()
    TelaPadrao("Meus dados", onVoltar) { padding ->
        ColunaTela(padding) {
            if (usuario == null) {
                EstadoVazio("Sem dados", "Entre novamente.")
                return@ColunaTela
            }
            LinhaDado("Nome", usuario.nome)
            LinhaDado("E-mail", usuario.email)
            LinhaDado("Documento", if (usuario.documento.length == 11) Validacao.formatarCpf(usuario.documento) else Validacao.formatarCnpj(usuario.documento))
            LinhaDado("Telefone", Validacao.formatarTelefone(usuario.telefone))
            LinhaDado("CEP", Validacao.formatarCep(usuario.cep))
            LinhaDado("Versão do termo", usuario.versaoTermo.ifBlank { if (usuario.aceitouTermoGeral) "2026.1" else "Sem aceite" })
            BotaoPrincipal("Exportar meus dados") { ArquivosLocais.compartilhar(contexto, "Meus dados Mobilis", vm.exportarDados()) }
            if (bloqueios.isNotEmpty()) {
                Aviso("A exclusão está bloqueada: ${bloqueios.joinToString(" ")}")
            } else {
                BotaoSecundario("Solicitar exclusão da conta") { confirmar = true }
            }
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Excluir conta") },
            text = { Text("Seus dados pessoais serão removidos. Pedidos já encerrados permanecem sem o seu nome para a instituição.") },
            confirmButton = { TextButton(onClick = { confirmar = false; vm.excluirConta() }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Voltar") } }
        )
    }
}

private fun devolverAgora(pedido: Pedido): Boolean =
    pedido.status == StatusPedido.RETIRADO &&
        pedido.modalidade == Modalidade.EMPRESTIMO &&
        Datas.vencida(pedido.prazoDevolucao)
