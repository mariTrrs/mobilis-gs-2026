package br.com.fiap.mobilis.ui.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import br.com.fiap.mobilis.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.fiap.mobilis.domain.Contagem
import coil.compose.AsyncImage
import java.io.File
import br.com.fiap.mobilis.domain.PapelInstituicao
import br.com.fiap.mobilis.domain.Perfil
import br.com.fiap.mobilis.domain.Sessao

data class Aba(val rota: String, val rotulo: String, val icone: ImageVector)

fun abasDe(sessao: Sessao): List<Aba> = when (sessao.perfil) {
    Perfil.SOLICITANTE -> listOf(
        Aba("home", "Início", Icons.Filled.Home),
        Aba("busca", "Buscar", Icons.Filled.Search),
        Aba("pedidos", "Pedidos", Icons.AutoMirrored.Filled.Assignment),
        Aba("perfil", "Perfil", Icons.Filled.Person)
    )
    Perfil.ADMIN -> listOf(
        Aba("home", "Início", Icons.Filled.Dashboard),
        Aba("instituicoes", "Instituições", Icons.Filled.Apartment),
        Aba("catalogo", "Catálogo", Icons.Filled.Category),
        Aba("auditoria", "Auditoria", Icons.AutoMirrored.Filled.FactCheck),
        Aba("perfil", "Perfil", Icons.Filled.Person)
    )
    Perfil.INSTITUICAO -> if (sessao.papel == PapelInstituicao.GESTOR) {
        listOf(
            Aba("home", "Painel", Icons.Filled.Dashboard),
            Aba("estoque", "Estoque", Icons.Filled.Inventory2),
            Aba("pedidos", "Pedidos", Icons.AutoMirrored.Filled.Assignment),
            Aba("mais", "Mais", Icons.Filled.MoreHoriz),
            Aba("perfil", "Perfil", Icons.Filled.Person)
        )
    } else {
        listOf(
            Aba("home", "Início", Icons.Filled.Home),
            Aba("estoque", "Estoque", Icons.Filled.Inventory2),
            Aba("pedidos", "Pedidos", Icons.AutoMirrored.Filled.Assignment),
            Aba("mais", "Mais", Icons.Filled.MoreHoriz),
            Aba("perfil", "Perfil", Icons.Filled.Person)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPadrao(
    titulo: String,
    onVoltar: (() -> Unit)? = null,
    acoes: @Composable RowScope.() -> Unit = {},
    marca: Boolean = true,
    conteudo: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            if (marca) {
                MarcaBar(titulo, onVoltar, acoes)
            } else {
                TopAppBar(
                    windowInsets = WindowInsets.statusBars,
                    title = { Text(titulo) },
                    navigationIcon = {
                        if (onVoltar != null) {
                            IconButton(onClick = onVoltar) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                            }
                        }
                    },
                    actions = acoes
                )
            }
        },
        content = conteudo
    )
}

@Composable
private fun MarcaBar(
    titulo: String,
    onVoltar: (() -> Unit)?,
    acoes: @Composable RowScope.() -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Box(Modifier.fillMaxWidth().height(64.dp)) {
                if (onVoltar != null) {
                    IconButton(onClick = onVoltar, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo_mobilis),
                        contentDescription = "Logo do Mobilis",
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        "Mobilis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(Modifier.align(Alignment.CenterEnd), content = acoes)
            }
            if (titulo.isNotBlank() && titulo != "Mobilis") {
                Text(
                    titulo,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Casca(
    titulo: String,
    aba: String,
    sessao: Sessao,
    onAba: (String) -> Unit,
    conteudo: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = { MarcaBar(titulo, onVoltar = null, acoes = {}) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                abasDe(sessao).forEach { item ->
                    NavigationBarItem(
                        selected = aba == item.rota,
                        onClick = { onAba(item.rota) },
                        icon = { Icon(item.icone, contentDescription = null) },
                        label = { Text(item.rotulo) },
                        alwaysShowLabel = true
                    )
                }
            }
        },
        content = conteudo
    )
}

@Composable
fun ColunaTela(padding: PaddingValues, espaco: Dp = 16.dp, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(espaco),
        content = conteudo
    )
}

@Composable
fun Campo(
    valor: String,
    onChange: (String) -> Unit,
    rotulo: String,
    erro: String? = null,
    senha: Boolean = false,
    teclado: KeyboardType = KeyboardType.Text,
    linhas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = { if (erro != null) Text(erro) },
        visualTransformation = if (senha) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        minLines = linhas,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BotaoPrincipal(texto: String, habilitado: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    ) { Text(texto) }
}

@Composable
fun BotaoSecundario(texto: String, habilitado: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    ) { Text(texto) }
}

@Composable
fun PainelCard(titulo: String? = null, conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (titulo != null) TituloSecao(titulo)
            conteudo()
        }
    }
}

@Composable
fun TituloSecao(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
fun Etiqueta(texto: String, alerta: Boolean = false) {
    Surface(
        color = if (alerta) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (alerta) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun LinhaDado(rotulo: String, valor: String) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartaoAcao(
    titulo: String,
    descricao: String,
    selo: String? = null,
    miniatura: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (miniatura != null) miniatura()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!selo.isNullOrBlank()) Etiqueta(selo, alerta = true)
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(descricao, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MiniaturaEquipamento(nomeTipo: String, fotos: List<String> = emptyList(), larga: Boolean = false) {
    val salvas = fotos.filter { File(it).exists() }
    val forma = if (larga) {
        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
    } else {
        Modifier.size(72.dp).clip(RoundedCornerShape(14.dp))
    }
    if (salvas.isEmpty()) {
        Image(
            painter = painterResource(desenhoDoTipo(nomeTipo)),
            contentDescription = "Foto ilustrativa de $nomeTipo",
            modifier = forma,
            contentScale = ContentScale.Crop
        )
    } else if (larga) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            salvas.forEachIndexed { indice, caminho ->
                AsyncImage(
                    model = File(caminho),
                    contentDescription = "Foto ${indice + 1} de $nomeTipo",
                    modifier = forma,
                    contentScale = ContentScale.Crop
                )
            }
        }
    } else {
        AsyncImage(
            model = File(salvas.first()),
            contentDescription = "Foto de $nomeTipo",
            modifier = forma,
            contentScale = ContentScale.Crop
        )
    }
}

private fun desenhoDoTipo(nomeTipo: String): Int {
    val nome = nomeTipo.lowercase()
    return when {
        "banho" in nome -> R.drawable.equip_cadeira_banho
        "roda" in nome -> R.drawable.equip_cadeira_rodas
        "andador" in nome -> R.drawable.equip_andador
        "muleta" in nome -> R.drawable.equip_muleta
        "bengala" in nome -> R.drawable.equip_bengala
        "cama" in nome -> R.drawable.equip_cama
        else -> R.drawable.equip_generico
    }
}

@Composable
fun EstadoVazio(titulo: String, descricao: String) {
    PainelCard {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(descricao, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Aceite(marcado: Boolean, onChange: (Boolean) -> Unit, texto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Checkbox) { onChange(!marcado) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = marcado, onCheckedChange = null)
        Text(texto, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipsUnico(
    opcoes: List<T>,
    selecionado: T?,
    rotulo: (T) -> String,
    onSelect: (T) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { opcao ->
            FilterChip(
                selected = opcao == selecionado,
                onClick = { onSelect(opcao) },
                label = { Text(rotulo(opcao)) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipsMulti(
    opcoes: List<T>,
    selecionados: Set<T>,
    rotulo: (T) -> String,
    onToggle: (T) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { opcao ->
            FilterChip(
                selected = opcao in selecionados,
                onClick = { onToggle(opcao) },
                label = { Text(rotulo(opcao)) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            )
        }
    }
}

@Composable
fun GraficoBarras(titulo: String, itens: List<Contagem>) {
    val visiveis = itens.filter { it.quantidade > 0 }
    val total = visiveis.sumOf { it.quantidade }
    var selecionado by remember(visiveis) { mutableStateOf(visiveis.firstOrNull()?.rotulo) }
    val paleta = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
        MaterialTheme.colorScheme.onSurfaceVariant
    )
    PainelCard(titulo) {
        if (visiveis.isEmpty() || total == 0) {
            Text("Ainda não há dados para este gráfico.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@PainelCard
        }
        val atual = visiveis.find { it.rotulo == selecionado } ?: visiveis.first()
        val indiceAtual = visiveis.indexOf(atual)
        Text(
            "${atual.rotulo} · ${atual.quantidade} de $total · ${atual.quantidade * 100 / total}%",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            visiveis.forEachIndexed { indice, item ->
                Box(
                    Modifier
                        .weight(item.quantidade.toFloat())
                        .fillMaxHeight()
                        .background(paleta[indice % paleta.size])
                        .clickable { selecionado = item.rotulo }
                        .semantics { contentDescription = "${item.rotulo}: ${item.quantidade}" }
                )
            }
        }
        visiveis.forEachIndexed { indice, item ->
            val ativo = indice == indiceAtual
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { selecionado = item.rotulo }
                    .background(if (ativo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(paleta[indice % paleta.size]))
                Text(item.rotulo, modifier = Modifier.weight(1f))
                Text("${item.quantidade}", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun Aviso(texto: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(texto, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun AcaoTexto(texto: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(texto, fontWeight = FontWeight.SemiBold)
    }
}
