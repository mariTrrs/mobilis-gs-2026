package br.com.fiap.mobilis

import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.mobilis.navigation.AppNavigation
import br.com.fiap.mobilis.ui.theme.MobilisTheme
import br.com.fiap.mobilis.viewmodel.MobilisViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O emulador anuncia tela de 10 bits e depois falha ao criar essa superfície.
        // Sem este formato a janela fica desenhada, porém invisível (tela preta).
        window.setFormat(PixelFormat.RGBA_8888)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }
        enableEdgeToEdge()
        setContent {
            MobilisTheme {
                val viewModel: MobilisViewModel = viewModel(factory = MobilisViewModel.factory(application))
                AppNavigation(viewModel)
            }
        }
    }
}
