package br.com.fiap.mobilis.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.mobilis.domain.PapelInstituicao
import br.com.fiap.mobilis.domain.Perfil
import br.com.fiap.mobilis.domain.Validacao
import br.com.fiap.mobilis.ui.componentes.BotaoPrincipal
import br.com.fiap.mobilis.ui.componentes.BotaoSecundario
import br.com.fiap.mobilis.ui.componentes.Campo
import br.com.fiap.mobilis.ui.componentes.CartaoAcao
import br.com.fiap.mobilis.ui.componentes.Casca
import br.com.fiap.mobilis.ui.componentes.ColunaTela
import br.com.fiap.mobilis.ui.componentes.LinhaDado
import br.com.fiap.mobilis.ui.componentes.TituloSecao
import br.com.fiap.mobilis.viewmodel.MobilisViewModel

@Composable
fun TelaPerfil(vm: MobilisViewModel, onAba: (String) -> Unit, onAbrir: (String) -> Unit) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val processando by vm.processando.collectAsStateWithLifecycle()
    val pessoa = sessao
    val usuario = estado.usuarios.find { it.id == pessoa?.usuarioId }
    val instituicao = estado.instituicoes.find { it.id == pessoa?.instituicaoId }
    val gestor = usuario?.papel == PapelInstituicao.GESTOR
    val solicitante = usuario?.perfil == Perfil.SOLICITANTE

    var nome by rememberSaveable { mutableStateOf("") }
    var telefone by rememberSaveable { mutableStateOf("") }
    var cep by rememberSaveable { mutableStateOf("") }
    var endereco by rememberSaveable { mutableStateOf("") }
    var horario by rememberSaveable { mutableStateOf("") }
    var senhaAtual by rememberSaveable { mutableStateOf("") }
    var novaSenha by rememberSaveable { mutableStateOf("") }
    var pronto by rememberSaveable { mutableStateOf(false) }
    var tentou by rememberSaveable { mutableStateOf(false) }
    var tentouSenha by rememberSaveable { mutableStateOf(false) }
    var sair by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(usuario?.id) {
        val atual = usuario ?: return@LaunchedEffect
        if (pronto) return@LaunchedEffect
        nome = atual.nome
        telefone = atual.telefone
        cep = atual.cep
        endereco = if (gestor) instituicao?.endereco.orEmpty() else atual.endereco
        horario = instituicao?.horarioRetirada.orEmpty()
        pronto = true
    }

    if (pessoa == null || usuario == null) return

    val rotuloNome = if (gestor) "Nome do responsável" else "Nome"
    val erroNome = if (tentou) Validacao.nome(nome, if (gestor) "o nome do responsável" else "o nome") else null
    val erroTelefone = if (tentou && (usuario.perfil != Perfil.ADMIN || telefone.isNotBlank())) Validacao.telefone(telefone) else null
    val erroCep = if (tentou && (solicitante || gestor)) Validacao.cep(cep) else null
    val erroEndereco = if (tentou && gestor) Validacao.obrigatorio(endereco, "o endereço") else null
    val erroHorario = if (tentou && gestor) Validacao.obrigatorio(horario, "o horário de retirada") else null
    val erroSenhaAtual = if (tentouSenha && senhaAtual.isBlank()) "Informe a senha atual." else null
    val erroNovaSenha = if (tentouSenha) Validacao.senha(novaSenha) else null
    val documento = when (usuario.perfil) {
        Perfil.SOLICITANTE -> Validacao.formatarCpf(usuario.documento)
        Perfil.INSTITUICAO -> Validacao.formatarCnpj(instituicao?.cnpj ?: usuario.documento)
        Perfil.ADMIN -> ""
    }

    Casca("Perfil", "perfil", pessoa, onAba) { padding ->
        ColunaTela(padding) {
            Text(pessoa.rotulo, style = MaterialTheme.typography.bodyLarge)
            LinhaDado("E-mail", usuario.email)
            if (documento.isNotBlank()) LinhaDado(if (solicitante) "CPF" else "CNPJ", documento)
            Text("O e-mail e o documento identificam a conta e permanecem como foram cadastrados.")

            TituloSecao("Dados da conta")
            Campo(nome, { nome = it }, rotuloNome, erroNome)
            Campo(
                telefone,
                { telefone = it },
                if (usuario.perfil == Perfil.ADMIN) "Telefone com DDD (opcional)" else "Telefone com DDD",
                erroTelefone,
                teclado = KeyboardType.Phone
            )
            if (solicitante || gestor) {
                Campo(cep, { cep = it }, "CEP", erroCep, teclado = KeyboardType.Number)
            }
            if (gestor) {
                Campo(endereco, { endereco = it }, "Endereço da instituição", erroEndereco)
                Campo(horario, { horario = it }, "Horário de retirada", erroHorario)
            }
            BotaoPrincipal("Salvar perfil", !processando) {
                tentou = true
                val falhou = Validacao.nome(nome, if (gestor) "o nome do responsável" else "o nome") != null ||
                    (usuario.perfil != Perfil.ADMIN || telefone.isNotBlank()) && Validacao.telefone(telefone) != null ||
                    (solicitante || gestor) && Validacao.cep(cep) != null ||
                    gestor && (Validacao.obrigatorio(endereco, "o endereço") != null || Validacao.obrigatorio(horario, "o horário de retirada") != null)
                if (!falhou) vm.atualizarPerfil(nome, telefone, cep, endereco, horario)
            }

            TituloSecao("Senha")
            Campo(senhaAtual, { senhaAtual = it }, "Senha atual", erroSenhaAtual, senha = true, teclado = KeyboardType.Password)
            Campo(novaSenha, { novaSenha = it }, "Nova senha", erroNovaSenha, senha = true, teclado = KeyboardType.Password)
            BotaoSecundario("Salvar nova senha", !processando) {
                tentouSenha = true
                if (senhaAtual.isNotBlank() && Validacao.senha(novaSenha) == null) {
                    vm.alterarSenha(senhaAtual, novaSenha) {
                        senhaAtual = ""
                        novaSenha = ""
                        tentouSenha = false
                    }
                }
            }

            CartaoAcao("Avisos", "Decisões, fila, prazos e aprovação da conta") { onAbrir("notificacoes") }
            if (solicitante) {
                TituloSecao("Sua conta")
                CartaoAcao("Meus dados", "Ver, exportar ou pedir exclusão") { onAbrir("meus-dados") }
                CartaoAcao("Notificações", "Escolher push e e-mail") { onAbrir("notificacoes") }
                CartaoAcao("Fila de espera", "Entrar ou sair de uma fila") { onAbrir("fila") }
                CartaoAcao("Indicação de saúde", "Registrar ou confirmar uma indicação") { onAbrir("indicacao") }
            }

            TituloSecao("Sessão")
            Text("Sair encerra esta sessão e volta para a tela de login.")
            BotaoSecundario("Sair da conta") { sair = true }
        }
    }

    if (sair) {
        AlertDialog(
            onDismissRequest = { sair = false },
            title = { Text("Sair da conta") },
            text = { Text("Você volta para a tela de login e pode entrar de novo com o e-mail e a senha.") },
            confirmButton = { TextButton(onClick = vm::sair) { Text("Sair") } },
            dismissButton = { TextButton(onClick = { sair = false }) { Text("Voltar") } }
        )
    }
}
