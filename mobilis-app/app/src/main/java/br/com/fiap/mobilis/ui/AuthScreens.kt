package br.com.fiap.mobilis.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.mobilis.R
import br.com.fiap.mobilis.domain.Textos
import br.com.fiap.mobilis.ui.componentes.Aceite
import br.com.fiap.mobilis.ui.componentes.Aviso
import br.com.fiap.mobilis.ui.componentes.BotaoPrincipal
import br.com.fiap.mobilis.ui.componentes.BotaoSecundario
import br.com.fiap.mobilis.ui.componentes.Campo
import br.com.fiap.mobilis.ui.componentes.ColunaTela
import br.com.fiap.mobilis.ui.componentes.TelaPadrao
import br.com.fiap.mobilis.ui.componentes.TituloSecao
import br.com.fiap.mobilis.viewmodel.MobilisViewModel
import br.com.fiap.mobilis.viewmodel.RascunhoInstituicao
import br.com.fiap.mobilis.viewmodel.RascunhoPessoa

@Composable
fun TelaLogin(vm: MobilisViewModel, onCadastrar: () -> Unit, onRecuperar: () -> Unit, onConvite: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    val processando by vm.processando.collectAsStateWithLifecycle()
    TelaPadrao("Entrar", marca = false) { padding ->
        ColunaTela(padding) {
            Image(
                painter = painterResource(R.drawable.ic_logo_mobilis),
                contentDescription = "Logo do Mobilis",
                modifier = Modifier.size(96.dp)
            )
            Text("Banco solidário de equipamentos de apoio", style = MaterialTheme.typography.headlineSmall)
            Text("Empréstimo ou doação, sem custo. A retirada e a devolução são sempre na instituição.")
            Campo(email, { email = it }, "E-mail", teclado = KeyboardType.Email)
            Campo(senha, { senha = it }, "Senha", senha = true, teclado = KeyboardType.Password)
            BotaoPrincipal("Entrar", !processando) { vm.entrar(email, senha) }
            BotaoSecundario("Criar conta", onClick = onCadastrar)
            BotaoSecundario("Esqueci a senha", onClick = onRecuperar)
            BotaoSecundario("Tenho um convite de operador", onClick = onConvite)
        }
    }
}

@Composable
fun TelaEscolhaCadastro(onPessoa: () -> Unit, onInstituicao: () -> Unit, onVoltar: () -> Unit) {
    TelaPadrao("Criar conta", onVoltar) { padding ->
        ColunaTela(padding) {
            Text("Escolha o tipo de conta. Cada perfil vê só o que precisa.")
            BotaoPrincipal("Sou solicitante ou cuidador", onClick = onPessoa)
            BotaoPrincipal("Represento uma ONG, igreja ou hospital", onClick = onInstituicao)
        }
    }
}

@Composable
fun TelaCadastroSolicitante(vm: MobilisViewModel, onContinuar: () -> Unit, onVoltar: () -> Unit) {
    var nome by rememberSaveable { mutableStateOf("") }
    var cpf by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var telefone by rememberSaveable { mutableStateOf("") }
    var cep by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }
    TelaPadrao("Cadastro de solicitante", onVoltar) { padding ->
        ColunaTela(padding) {
            Text("Use seus dados para pedir um equipamento e acompanhar a retirada.")
            Campo(nome, { nome = it }, "Nome completo")
            Campo(cpf, { cpf = it }, "CPF", teclado = KeyboardType.Number)
            Campo(email, { email = it }, "E-mail", teclado = KeyboardType.Email)
            Campo(telefone, { telefone = it }, "Telefone com DDD", teclado = KeyboardType.Phone)
            Campo(cep, { cep = it }, "CEP", teclado = KeyboardType.Number)
            Campo(senha, { senha = it }, "Senha", senha = true, teclado = KeyboardType.Password)
            if (erro != null) Text(erro!!, color = MaterialTheme.colorScheme.error)
            BotaoPrincipal("Ler os termos") {
                val falha = vm.guardarPessoa(RascunhoPessoa(nome, cpf, email, telefone, cep, senha))
                erro = falha
                if (falha == null) onContinuar()
            }
        }
    }
}

@Composable
fun TelaCadastroInstituicao(vm: MobilisViewModel, onContinuar: () -> Unit, onVoltar: () -> Unit) {
    var razao by rememberSaveable { mutableStateOf("") }
    var cnpj by rememberSaveable { mutableStateOf("") }
    var endereco by rememberSaveable { mutableStateOf("") }
    var cep by rememberSaveable { mutableStateOf("") }
    var horario by rememberSaveable { mutableStateOf("") }
    var responsavel by rememberSaveable { mutableStateOf("") }
    var telefone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }
    TelaPadrao("Cadastro da instituição", onVoltar) { padding ->
        ColunaTela(padding) {
            Text("A conta fica como gestora e só entra no painel depois da aprovação da administração.")
            Campo(razao, { razao = it }, "Razão social")
            Campo(cnpj, { cnpj = it }, "CNPJ", teclado = KeyboardType.Number)
            Campo(endereco, { endereco = it }, "Endereço")
            Campo(cep, { cep = it }, "CEP", teclado = KeyboardType.Number)
            Campo(horario, { horario = it }, "Horário de retirada")
            Campo(responsavel, { responsavel = it }, "Nome do responsável")
            Campo(telefone, { telefone = it }, "Telefone do responsável", teclado = KeyboardType.Phone)
            Campo(email, { email = it }, "E-mail do responsável", teclado = KeyboardType.Email)
            Campo(senha, { senha = it }, "Senha", senha = true, teclado = KeyboardType.Password)
            if (erro != null) Text(erro!!, color = MaterialTheme.colorScheme.error)
            BotaoPrincipal("Ler os termos") {
                val falha = vm.guardarInstituicao(
                    RascunhoInstituicao(razao, cnpj, endereco, cep, horario, responsavel, telefone, email, senha)
                )
                erro = falha
                if (falha == null) onContinuar()
            }
        }
    }
}

@Composable
fun TelaTermos(vm: MobilisViewModel, onContinuar: () -> Unit, onVoltar: () -> Unit) {
    var geral by rememberSaveable { mutableStateOf(false) }
    var saude by rememberSaveable { mutableStateOf(false) }
    val processando by vm.processando.collectAsStateWithLifecycle()
    TelaPadrao("Termos e privacidade", onVoltar) { padding ->
        ColunaTela(padding) {
            TituloSecao("Termo geral")
            Text(Textos.termoGeral)
            Aceite(geral, { geral = it }, "Li e aceito o termo geral")
            TituloSecao("Dados de saúde")
            Text(Textos.termoSaude)
            Aceite(saude, { saude = it }, "Autorizo o uso dos dados de saúde para o empréstimo ou a doação")
            Text("Os dois aceites são obrigatórios. Sem eles o cadastro não é concluído.")
            BotaoPrincipal("Receber código", geral && saude && !processando) { vm.enviarCodigoCadastro(geral, saude, onContinuar) }
        }
    }
}

@Composable
fun TelaConfirmarCodigo(vm: MobilisViewModel, onOk: () -> Unit, onVoltar: () -> Unit) {
    var codigo by rememberSaveable { mutableStateOf("") }
    val demonstracao by vm.codigoDemonstracao.collectAsStateWithLifecycle()
    val processando by vm.processando.collectAsStateWithLifecycle()
    TelaPadrao("Confirmar código", onVoltar) { padding ->
        ColunaTela(padding) {
            Text("Digite o código de 6 dígitos enviado para o e-mail ${vm.emailFluxo}.")
            if (!demonstracao.isNullOrBlank()) {
                Aviso("Nesta versão o e-mail ainda não é enviado por um servidor. Código de demonstração: $demonstracao")
            }
            Campo(codigo, { codigo = it }, "Código", teclado = KeyboardType.Number)
            BotaoPrincipal("Confirmar", !processando) { vm.confirmarCodigo(codigo, onOk) }
        }
    }
}

@Composable
fun TelaRecuperarSenha(vm: MobilisViewModel, onContinuar: () -> Unit, onVoltar: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    val processando by vm.processando.collectAsStateWithLifecycle()
    TelaPadrao("Recuperar senha", onVoltar) { padding ->
        ColunaTela(padding) {
            Text("Informe o e-mail da conta. O código de 6 dígitos aparece na próxima tela para você definir outra senha.")
            Campo(email, { email = it }, "E-mail", teclado = KeyboardType.Email)
            BotaoPrincipal("Gerar código", !processando) { vm.pedirSenha(email, onContinuar) }
        }
    }
}

@Composable
fun TelaNovaSenha(vm: MobilisViewModel, onOk: () -> Unit, onVoltar: () -> Unit) {
    var codigo by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    val demonstracao by vm.codigoDemonstracao.collectAsStateWithLifecycle()
    val processando by vm.processando.collectAsStateWithLifecycle()
    TelaPadrao("Nova senha", onVoltar) { padding ->
        ColunaTela(padding) {
            if (!demonstracao.isNullOrBlank()) Aviso("Código de demonstração: $demonstracao")
            Campo(codigo, { codigo = it }, "Código", teclado = KeyboardType.Number)
            Campo(senha, { senha = it }, "Nova senha", senha = true, teclado = KeyboardType.Password)
            BotaoPrincipal("Salvar senha", !processando) { vm.definirSenha(codigo, senha, onOk) }
        }
    }
}

@Composable
fun TelaConvite(vm: MobilisViewModel, onOk: () -> Unit, onVoltar: () -> Unit) {
    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var codigo by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var geral by rememberSaveable { mutableStateOf(false) }
    var saude by rememberSaveable { mutableStateOf(false) }
    val processando by vm.processando.collectAsStateWithLifecycle()
    TelaPadrao("Aceitar convite", onVoltar) { padding ->
        ColunaTela(padding) {
            Text("O gestor envia o convite. Aqui você cria a senha e passa a operar o estoque da instituição.")
            Campo(nome, { nome = it }, "Seu nome")
            Campo(email, { email = it }, "E-mail do convite", teclado = KeyboardType.Email)
            Campo(codigo, { codigo = it }, "Código do convite", teclado = KeyboardType.Number)
            Campo(senha, { senha = it }, "Senha", senha = true, teclado = KeyboardType.Password)
            Text(Textos.termoGeral, modifier = Modifier.height(120.dp).verticalScroll(rememberScrollState()))
            Aceite(geral, { geral = it }, "Aceito o termo geral")
            Aceite(saude, { saude = it }, "Aceito o uso de dados de saúde")
            BotaoPrincipal("Criar acesso de operador", geral && saude && !processando) {
                vm.aceitarConvite(email, codigo, nome, senha, geral, saude, onOk)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun TelaAguardando(vm: MobilisViewModel) {
    val sessao by vm.sessao.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val instituicao = estado.instituicoes.find { it.id == sessao?.instituicaoId }
    TelaPadrao("Cadastro em análise") { padding ->
        ColunaTela(padding) {
            Text("Olá, ${sessao?.nome ?: ""}.")
            Text("A instituição ${instituicao?.razaoSocial ?: ""} está ${instituicao?.status?.rotulo?.lowercase() ?: "em análise"}. O painel abre quando a administração aprovar.")
            if (!instituicao?.motivoReprovacao.isNullOrBlank()) {
                Aviso("Motivo: ${instituicao?.motivoReprovacao}")
            }
            LinhaSePreciso("CNPJ", instituicao?.cnpj)
            LinhaSePreciso("Endereço", instituicao?.endereco)
            LinhaSePreciso("Horário de retirada", instituicao?.horarioRetirada)
            BotaoSecundario("Sair", onClick = vm::sair)
        }
    }
}

@Composable
private fun LinhaSePreciso(rotulo: String, valor: String?) {
    if (!valor.isNullOrBlank()) {
        br.com.fiap.mobilis.ui.componentes.LinhaDado(rotulo, valor)
    }
}
