package br.com.fiap.mobilis.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.com.fiap.mobilis.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fiap.mobilis.ui.TelaAguardando
import br.com.fiap.mobilis.ui.TelaAnalisePedido
import br.com.fiap.mobilis.ui.TelaAtrasos
import br.com.fiap.mobilis.ui.TelaAuditoria
import br.com.fiap.mobilis.ui.TelaAvaliacoes
import br.com.fiap.mobilis.ui.TelaBusca
import br.com.fiap.mobilis.ui.TelaCadastroInstituicao
import br.com.fiap.mobilis.ui.TelaCadastroSolicitante
import br.com.fiap.mobilis.ui.TelaCatalogoAdmin
import br.com.fiap.mobilis.ui.TelaConfirmarCodigo
import br.com.fiap.mobilis.ui.TelaPerfil
import br.com.fiap.mobilis.ui.TelaConvite
import br.com.fiap.mobilis.ui.TelaDetalheEquipamentoPublico
import br.com.fiap.mobilis.ui.TelaDetalheEstoque
import br.com.fiap.mobilis.ui.TelaDetalhePedido
import br.com.fiap.mobilis.ui.TelaEquipe
import br.com.fiap.mobilis.ui.TelaEscolhaCadastro
import br.com.fiap.mobilis.ui.TelaEstoque
import br.com.fiap.mobilis.ui.TelaFila
import br.com.fiap.mobilis.ui.TelaFormEquipamento
import br.com.fiap.mobilis.ui.TelaFormTipo
import br.com.fiap.mobilis.ui.TelaIndicacao
import br.com.fiap.mobilis.ui.TelaInicioAdmin
import br.com.fiap.mobilis.ui.TelaInicioInstituicao
import br.com.fiap.mobilis.ui.TelaInicioSolicitante
import br.com.fiap.mobilis.ui.TelaInstituicaoAdmin
import br.com.fiap.mobilis.ui.TelaInstituicoes
import br.com.fiap.mobilis.ui.TelaLogin
import br.com.fiap.mobilis.ui.TelaMais
import br.com.fiap.mobilis.ui.TelaMeusDados
import br.com.fiap.mobilis.ui.TelaNotificacoes
import br.com.fiap.mobilis.ui.TelaNovaSenha
import br.com.fiap.mobilis.ui.TelaNovoPedido
import br.com.fiap.mobilis.ui.TelaOperacaoQr
import br.com.fiap.mobilis.ui.TelaPedidosInstituicao
import br.com.fiap.mobilis.ui.TelaPedidosSolicitante
import br.com.fiap.mobilis.ui.TelaPoliticas
import br.com.fiap.mobilis.ui.TelaRecuperarSenha
import br.com.fiap.mobilis.ui.TelaTermos
import br.com.fiap.mobilis.domain.Perfil
import br.com.fiap.mobilis.viewmodel.MobilisViewModel

@Composable
fun AppNavigation(vm: MobilisViewModel) {
    val nav = rememberNavController()
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val pronto by vm.pronto.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    val snack = androidx.compose.runtime.remember { SnackbarHostState() }
    val atual by nav.currentBackStackEntryAsState()
    val rota = atual?.destination?.route
    val gate = when {
        sessao == null -> "off"
        sessao?.aguardandoInstituicao == true -> "wait"
        else -> "in"
    }

    LaunchedEffect(mensagem) {
        val texto = mensagem ?: return@LaunchedEffect
        snack.showSnackbar(texto)
        vm.consumirMensagem()
    }
    LaunchedEffect(gate, rota, pronto) {
        if (!pronto || rota == null) return@LaunchedEffect
        val publica = rota in Rotas.publicas
        when (gate) {
            "off" -> if (!publica) nav.limpar(Rotas.LOGIN)
            "wait" -> if (rota != Rotas.AGUARDANDO) nav.limpar(Rotas.AGUARDANDO)
            "in" -> if (publica || rota == Rotas.AGUARDANDO) nav.limpar(Rotas.HOME)
        }
    }

    val voltar: () -> Unit = { nav.popBackStack() }
    val aba: (String) -> Unit = { destino ->
        nav.navigate(destino) {
            popUpTo(Rotas.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!pronto) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_mobilis),
                    contentDescription = "Logo do Mobilis",
                    modifier = Modifier.size(72.dp)
                )
                Text("Mobilis", color = MaterialTheme.colorScheme.primary)
            }
            return@Box
        }
        NavHost(navController = nav, startDestination = Rotas.LOGIN) {
            composable(Rotas.LOGIN) { TelaLogin(vm, { nav.navigate(Rotas.CADASTRO) }, { nav.navigate(Rotas.RECUPERAR) }, { nav.navigate(Rotas.CONVITE) }) }
            composable(Rotas.CADASTRO) { TelaEscolhaCadastro({ nav.navigate(Rotas.CADASTRO_PESSOA) }, { nav.navigate(Rotas.CADASTRO_INSTITUICAO) }, voltar) }
            composable(Rotas.CADASTRO_PESSOA) { TelaCadastroSolicitante(vm, { nav.navigate(Rotas.TERMOS) }, voltar) }
            composable(Rotas.CADASTRO_INSTITUICAO) { TelaCadastroInstituicao(vm, { nav.navigate(Rotas.TERMOS) }, voltar) }
            composable(Rotas.TERMOS) { TelaTermos(vm, { nav.navigate(Rotas.CONFIRMAR) }, voltar) }
            composable(Rotas.CONFIRMAR) { TelaConfirmarCodigo(vm, { nav.limpar(Rotas.LOGIN) }, voltar) }
            composable(Rotas.RECUPERAR) { TelaRecuperarSenha(vm, { nav.navigate(Rotas.NOVA_SENHA) }, voltar) }
            composable(Rotas.NOVA_SENHA) { TelaNovaSenha(vm, { nav.limpar(Rotas.LOGIN) }, voltar) }
            composable(Rotas.CONVITE) { TelaConvite(vm, { nav.limpar(Rotas.LOGIN) }, voltar) }
            composable(Rotas.AGUARDANDO) { TelaAguardando(vm) }
            composable(Rotas.HOME) {
                when (sessao?.perfil) {
                    Perfil.ADMIN -> TelaInicioAdmin(vm, aba)
                    Perfil.INSTITUICAO -> TelaInicioInstituicao(vm, aba) { nav.navigate(it) }
                    else -> TelaInicioSolicitante(vm, aba) { nav.navigate(it) }
                }
            }
            composable(Rotas.BUSCA) { TelaBusca(vm, aba) { nav.navigate(it) } }
            composable(Rotas.PEDIDOS) {
                if (sessao?.perfil == Perfil.INSTITUICAO) TelaPedidosInstituicao(vm, aba) { nav.navigate(it) }
                else TelaPedidosSolicitante(vm, aba) { nav.navigate(it) }
            }
            composable(Rotas.PERFIL) { TelaPerfil(vm, aba) { nav.navigate(it) } }
            composable(Rotas.MAIS) { TelaMais(vm, aba) { nav.navigate(it) } }
            composable(Rotas.ESTOQUE) { TelaEstoque(vm, aba) { nav.navigate(it) } }
            composable(Rotas.INSTITUICOES) { TelaInstituicoes(vm, aba) { nav.navigate(it) } }
            composable(Rotas.CATALOGO) { TelaCatalogoAdmin(vm, aba) { nav.navigate(it) } }
            composable(Rotas.AUDITORIA) { TelaAuditoria(vm, comVoltar = sessao?.perfil != Perfil.ADMIN, onVoltar = voltar, onAba = aba) }
            composable(Rotas.FILA) { TelaFila(vm, voltar) }
            composable(Rotas.NOTIFICACOES) { TelaNotificacoes(vm, voltar) }
            composable(Rotas.INDICACAO) { TelaIndicacao(vm, voltar) }
            composable(Rotas.MEUS_DADOS) { TelaMeusDados(vm, voltar) }
            composable(Rotas.POLITICAS) { TelaPoliticas(vm, voltar) }
            composable(Rotas.RETIRADA) { TelaOperacaoQr(vm, devolucao = false, voltar) }
            composable(Rotas.DEVOLUCAO) { TelaOperacaoQr(vm, devolucao = true, voltar) }
            composable(Rotas.ATRASOS) { TelaAtrasos(vm, voltar) }
            composable(Rotas.EQUIPE) { TelaEquipe(vm, voltar) }
            composable(Rotas.AVALIACOES) { TelaAvaliacoes(vm, voltar) }
            composable(Rotas.EQUIPAMENTO, args("id")) { entrada ->
                val id = entrada.arguments?.getString("id")?.toIntOrNull() ?: 0
                TelaDetalheEquipamentoPublico(vm, id, { nav.navigate(Rotas.novoPedido(id)) }, voltar)
            }
            composable(Rotas.NOVO_PEDIDO, args("id")) { entrada ->
                val id = entrada.arguments?.getString("id")?.toIntOrNull() ?: 0
                TelaNovoPedido(vm, id, voltar, voltar)
            }
            composable(Rotas.PEDIDO, args("id")) { entrada ->
                TelaDetalhePedido(vm, entrada.arguments?.getString("id")?.toIntOrNull() ?: 0, voltar)
            }
            composable(Rotas.FORM_EQUIPAMENTO, args("id")) { entrada ->
                val id = entrada.arguments?.getString("id")?.toIntOrNull() ?: 0
                TelaFormEquipamento(vm, id, {
                    if (id == 0) {
                        val novo = vm.estado.value.equipamentos.maxOfOrNull { it.id } ?: 0
                        nav.popBackStack()
                        nav.navigate(Rotas.detalheEstoque(novo))
                    } else {
                        voltar()
                    }
                }, voltar)
            }
            composable(Rotas.DETALHE_ESTOQUE, args("id")) { entrada ->
                val id = entrada.arguments?.getString("id")?.toIntOrNull() ?: 0
                TelaDetalheEstoque(vm, id, { nav.navigate(Rotas.formEquipamento(id)) }, voltar)
            }
            composable(Rotas.ANALISE, args("id")) { entrada ->
                TelaAnalisePedido(vm, entrada.arguments?.getString("id")?.toIntOrNull() ?: 0, voltar)
            }
            composable(Rotas.ADMIN_INSTITUICAO, args("id")) { entrada ->
                TelaInstituicaoAdmin(vm, entrada.arguments?.getString("id")?.toIntOrNull() ?: 0, voltar)
            }
            composable(Rotas.FORM_TIPO, args("id")) { entrada ->
                TelaFormTipo(vm, entrada.arguments?.getString("id")?.toIntOrNull() ?: 0, voltar, voltar)
            }
        }
        SnackbarHost(
            hostState = snack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (rota in Rotas.comBarra && gate == "in") 88.dp else 16.dp)
        )
    }
}

private fun args(nome: String) = listOf(navArgument(nome) { type = NavType.StringType })

private fun NavHostController.limpar(destino: String) {
    navigate(destino) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
