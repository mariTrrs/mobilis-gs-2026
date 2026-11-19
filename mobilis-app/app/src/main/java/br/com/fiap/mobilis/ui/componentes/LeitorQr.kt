package br.com.fiap.mobilis.ui.componentes

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors

@Composable
fun CameraQr(onCodigo: (String) -> Unit) {
    val contexto = LocalContext.current
    var permitido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitido = it }
    if (!permitido) {
        BotaoSecundario("Permitir câmera para ler o QR Code") { pedir.launch(Manifest.permission.CAMERA) }
        return
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
    Box(Modifier.fillMaxWidth().height(240.dp)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val futuro = ProcessCameraProvider.getInstance(ctx)
                futuro.addListener({
                    val provedor = futuro.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val analise = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analise.setAnalyzer(executor) { imagem ->
                        val codigo = ler(imagem)
                        imagem.close()
                        if (codigo != null) onCodigo(codigo)
                    }
                    val dono = ctx as? LifecycleOwner ?: return@addListener
                    runCatching {
                        provedor.unbindAll()
                        provedor.bindToLifecycle(dono, CameraSelector.DEFAULT_BACK_CAMERA, preview, analise)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxWidth().height(240.dp)
        )
    }
}

private fun ler(imagem: androidx.camera.core.ImageProxy): String? = runCatching {
    val plano = imagem.planes.first()
    val buffer = plano.buffer
    val dados = ByteArray(buffer.remaining())
    buffer.get(dados)
    val largura = imagem.width
    val altura = imagem.height
    val fonte = PlanarYUVLuminanceSource(dados, largura, altura, 0, 0, largura, altura, false)
    QRCodeReader().decode(BinaryBitmap(HybridBinarizer(fonte))).text
}.getOrNull()
