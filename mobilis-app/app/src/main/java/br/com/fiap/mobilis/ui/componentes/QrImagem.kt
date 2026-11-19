package br.com.fiap.mobilis.ui.componentes

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun ImagemQr(conteudo: String, descricao: String) {
    val imagem = remember(conteudo) { gerarQr(conteudo) }
    Image(bitmap = imagem, contentDescription = descricao, modifier = Modifier.size(220.dp))
}

private fun gerarQr(texto: String, tamanho: Int = 512) = runCatching {
    val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamanho, tamanho)
    val bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.RGB_565)
    for (x in 0 until tamanho) {
        for (y in 0 until tamanho) {
            bitmap.setPixel(x, y, if (matriz.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    bitmap.asImageBitmap()
}.getOrElse {
    Bitmap.createBitmap(32, 32, Bitmap.Config.RGB_565).asImageBitmap()
}
