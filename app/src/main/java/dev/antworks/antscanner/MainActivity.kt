package dev.antworks.antscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.antworks.antscanner.ui.MainScreen
import dev.antworks.antscanner.ui.MainViewModel
import dev.antworks.antscanner.ui.MainViewModelFactory
import dev.antworks.antscanner.ui.WelcomeScreen
import dev.antworks.antscanner.ui.theme.AntScannerTheme

class MainActivity : ComponentActivity() {

    // Klasyczne wstrzyknięcie: wyciągamy repozytorium z naszej AntScannerApp
    // i karmimy nim fabrykę, żeby wypluła nam gotowy do akcji ViewModel.
    private val viewModel: MainViewModel by viewModels {
        val app = application as AntScannerApp
        MainViewModelFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Twój theme (domyślnie wygenerowany przez Android Studio, ma wsparcie dla Dynamic Colors)
            AntScannerTheme {
                // Luksusowy fundament. Surface od razu łapie odpowiednie kolory z Material 3.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AntScannerRouter(viewModel)
                }
            }
        }
    }
}

/**
 * Główny zwrotniczy aplikacji.
 * Reaguje na zmiany stanu w ułamku sekundy.
 */
@Composable
fun AntScannerRouter(viewModel: MainViewModel) {
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

    if (hasSeenOnboarding) {
        // Wjeżdża gotowe mięso
        MainScreen(viewModel)
    } else {
        WelcomeScreen(
            onNameSaved = { wpisaneImie ->
                viewModel.saveMomName(wpisaneImie)
            }
        )
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.headlineMedium)
    }
}