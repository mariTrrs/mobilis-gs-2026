package br.com.fiap.mobilis.data

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File

object ArquivosLocais {
    private const val CINCO_MB = 5L * 1024 * 1024

    fun copiar(context: Context, uri: Uri, prefixo: String, somenteImagem: Boolean = false): Pair<String, String>? {
        val nomeOriginal = consultarNome(context, uri) ?: "$prefixo-${System.currentTimeMillis()}"
        val destino = File(context.filesDir, "anexos/${System.currentTimeMillis()}-$nomeOriginal")
        destino.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            destino.outputStream().use { saida -> entrada.copyTo(saida) }
        } ?: return null
        val mime = context.contentResolver.getType(uri).orEmpty()
        val nome = nomeOriginal.lowercase()
        val imagem = mime == "image/jpeg" || mime == "image/png" || nome.endsWith(".jpg") || nome.endsWith(".jpeg") || nome.endsWith(".png")
        val pdf = mime == "application/pdf" || nome.endsWith(".pdf")
        val aceito = if (somenteImagem) imagem else imagem || pdf
        if (!aceito || destino.length() > CINCO_MB) {
            destino.delete()
            return null
        }
        return nomeOriginal to destino.absolutePath
    }

    fun novoArquivoFoto(context: Context): Pair<File, Uri> {
        val pasta = File(context.cacheDir, "fotos").apply { mkdirs() }
        val arquivo = File(pasta, "foto-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
        return arquivo to uri
    }

    fun etiqueta(context: Context, codigo: String, titulo: String) {
        val documento = PdfDocument()
        val pagina = documento.startPage(PdfDocument.PageInfo.Builder(300, 420, 1).create())
        val canvas = pagina.canvas
        val tinta = Paint().apply { color = Color.BLACK; textSize = 16f }
        canvas.drawText(titulo, 24f, 40f, tinta)
        canvas.drawText(codigo, 24f, 68f, tinta)
        val qr = qrBitmap("MOBILIS:$codigo", 220)
        if (qr != null) canvas.drawBitmap(qr, 40f, 96f, null)
        documento.finishPage(pagina)
        val arquivo = File(context.cacheDir, "etiqueta-$codigo.pdf")
        arquivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        compartilharArquivo(context, arquivo, "application/pdf", "Etiqueta $codigo")
    }

    data class SecaoRelatorio(val titulo: String, val linhas: List<String>)

    fun relatorioPdf(context: Context, instituicao: String, geradoEm: String, filtro: String, secoes: List<SecaoRelatorio>) {
        val documento = PdfDocument()
        val largura = 595
        val altura = 842
        val teal = Color.parseColor("#0F6E66")
        val tintaEscura = Color.parseColor("#1A1C1C")
        val tintaSuave = Color.parseColor("#3F4947")
        val fundo = Color.parseColor("#F4F7F6")
        val logo = BitmapFactory.decodeResource(context.resources, br.com.fiap.mobilis.R.drawable.ic_logo_mobilis)
        val faixa = Paint().apply { color = teal }
        val tituloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtituloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 11f }
        val secaoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = teal
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val corpo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tintaEscura; textSize = 11f }
        val apoio = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tintaSuave; textSize = 10f }
        val linhaPaint = Paint().apply { color = Color.parseColor("#B7EBE4"); strokeWidth = 1.5f }
        var numero = 1
        var pagina = documento.startPage(PdfDocument.PageInfo.Builder(largura, altura, numero).create())
        var canvas = pagina.canvas

        fun rodape() {
            canvas.drawLine(32f, 800f, 563f, 800f, linhaPaint)
            canvas.drawText("Mobilis · Banco solidário de equipamentos", 32f, 820f, apoio)
            val paginaTexto = "Página $numero"
            canvas.drawText(paginaTexto, 563f - apoio.measureText(paginaTexto), 820f, apoio)
        }

        fun faixa(compacta: Boolean): Float {
            val alto = if (compacta) 56f else 92f
            canvas.drawRect(0f, 0f, largura.toFloat(), alto, faixa)
            if (logo != null) canvas.drawBitmap(logo, null, Rect(24, if (compacta) 10 else 22, 64, if (compacta) 50 else 62), null)
            canvas.drawText("Mobilis", 76f, if (compacta) 34f else 46f, tituloPaint)
            if (!compacta) canvas.drawText("Banco solidário de equipamentos de apoio", 76f, 68f, subtituloPaint)
            return alto + 28f
        }

        fun novaPagina() {
            rodape()
            documento.finishPage(pagina)
            numero += 1
            pagina = documento.startPage(PdfDocument.PageInfo.Builder(largura, altura, numero).create())
            canvas = pagina.canvas
        }

        var y = faixa(false)
        canvas.drawText(instituicao, 32f, y, secaoPaint)
        y += 18f
        canvas.drawText("Gerado em $geradoEm", 32f, y, apoio)
        y += 16f
        canvas.drawText("Filtro: $filtro", 32f, y, apoio)
        y += 22f
        secoes.forEach { secao ->
            if (y > 740f) {
                novaPagina()
                y = faixa(true)
            }
            canvas.drawRoundRect(32f, y - 14f, 563f, y + 8f, 8f, 8f, Paint().apply { color = fundo })
            canvas.drawText(secao.titulo, 44f, y, secaoPaint)
            y += 22f
            val linhas = secao.linhas.ifEmpty { listOf("Nenhum registro nesta seleção.") }
            linhas.forEach { linha ->
                quebrar(linha, corpo, 500f).forEach { trecho ->
                    if (y > 780f) {
                        novaPagina()
                        y = faixa(true)
                    }
                    canvas.drawText(trecho, 44f, y, corpo)
                    y += 16f
                }
            }
            y += 14f
        }
        rodape()
        documento.finishPage(pagina)
        val arquivo = File(context.cacheDir, "relatorio-mobilis.pdf")
        arquivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        logo?.recycle()
        compartilharArquivo(context, arquivo, "application/pdf", "Relatório Mobilis")
    }

    private fun quebrar(texto: String, paint: Paint, maximo: Float): List<String> {
        val palavras = texto.split(" ")
        val linhas = mutableListOf<String>()
        var atual = ""
        palavras.forEach { palavra ->
            val candidato = if (atual.isEmpty()) palavra else "$atual $palavra"
            if (paint.measureText(candidato) > maximo && atual.isNotEmpty()) {
                linhas += atual
                atual = palavra
            } else {
                atual = candidato
            }
        }
        if (atual.isNotEmpty()) linhas += atual
        return linhas.ifEmpty { listOf("") }
    }

    fun compartilhar(context: Context, titulo: String, texto: String) {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        context.startActivity(Intent.createChooser(envio, titulo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun abrir(context: Context, caminho: String) {
        val arquivo = File(caminho)
        if (!arquivo.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
        val ver = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(ver, "Abrir comprovante").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun rota(context: Context, latitude: Double, longitude: Double, nome: String) {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(nome)})")
        val mapa = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(mapa)
        } catch (_: Exception) {
            compartilhar(context, nome, "Retirada em $nome. Coordenadas: $latitude, $longitude")
        }
    }

    private fun compartilharArquivo(context: Context, arquivo: File, mime: String, titulo: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(envio, titulo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun qrBitmap(texto: String, tamanho: Int): android.graphics.Bitmap? = runCatching {
        val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamanho, tamanho)
        val bitmap = android.graphics.Bitmap.createBitmap(tamanho, tamanho, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until tamanho) {
            for (y in 0 until tamanho) {
                bitmap.setPixel(x, y, if (matriz[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    }.getOrNull()

    private fun consultarNome(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val indice = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (indice >= 0 && cursor.moveToFirst()) return cursor.getString(indice)
        }
        return null
    }
}
