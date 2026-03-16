package dev.antworks.antscanner.ui
import android.content.Intent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.antworks.antscanner.data.db.DocumentEntity
import dev.antworks.antscanner.scanner.AntScannerClient
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val momName by viewModel.momName.collectAsState()
    val documents by viewModel.documents.collectAsState()

    // Stan do obsługi wpisywania nazwy pliku po udanym skanie
    var showNameDialog by remember { mutableStateOf(false) }
    var scannedUri by remember { mutableStateOf<Uri?>(null) }
    var fileNameInput by remember { mutableStateOf("") }

    // Rejestrujemy launcher, który odpali intencję skanera i odbierze wynik
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                // Mamy PDF! Zapisujemy URI w stanie i pokazujemy dialog z nazwą
                scannedUri = pdf.uri
                showNameDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getGreetingMessage(momName ?: "Mamo")) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val scannerClient = AntScannerClient().getClient()
                    scannerClient.getStartScanIntent(context as Activity)
                        .addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(
                                androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                            )
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Skaner spierdolił się przy starcie: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Skanuj nowy dokument")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (documents.isEmpty()) {
                // Pusty stan
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Nie masz jeszcze żadnych skanów.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Kliknij + aby dodać pierwszy dokument.")
                }
            } else {
                // Lista zeskanowanych dokumentów
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documents) { doc ->
                        DocumentItem(doc)
                    }
                }
            }
        }
    }

    // Modalny dialog/bottom sheet zmuszający do podania nazwy pliku
    // (To wklej wewnątrz MainScreen, zamiast starego AlertDialog)
    val haptic = LocalHapticFeedback.current

    // Modalny dialog/bottom sheet zmuszający do podania nazwy pliku
    if (showNameDialog && scannedUri != null) {
        AlertDialog(
            onDismissRequest = { /* Brak powrotu - trzeba podać nazwę! */ },
            title = { Text("Zapisz skan") },
            text = {
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    label = { Text("Nazwa dokumentu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileNameInput.isNotBlank()) {
                            // Wymuszamy solidną wibrację potwierdzającą sukces
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            viewModel.saveScannedDocument(scannedUri!!, fileNameInput.trim())

                            // Czyścimy stan
                            showNameDialog = false
                            scannedUri = null
                            fileNameInput = ""
                        }
                    },
                    enabled = fileNameInput.isNotBlank()
                ) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    scannedUri = null
                    fileNameInput = ""
                }) {
                    Text("Anuluj")
                }
            }
        )
    }
} // Tu kończy się funkcja MainScreen

// --- PONIŻEJ WKLEJ ZAKTUALIZOWANE FUNKCJE POMOCNICZE ---

@Composable
fun DocumentItem(doc: DocumentEntity) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = doc.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Zapisano w: Pobrane",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Przycisk Share Sheet odpala natywne menu Androida
            IconButton(
                onClick = { shareDocument(context, doc.uriString, doc.name) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Udostępnij",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Funkcja ogarniająca bezpieczne udostępnianie PDF-a
fun shareDocument(context: android.content.Context, uriString: String, fileName: String) {
    val uri = Uri.parse(uriString)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        // Nadajemy tymczasowe uprawnienia aplikacji, która odbierze plik (np. Gmail)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Udostępnij skan Mamo"))
}

fun getGreetingMessage(name: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Dzień dobry, $name!"
        in 12..17 -> "Witaj, $name!"
        in 18..22 -> "Dobry wieczór, $name!"
        else -> "Spokojnej nocy, $name!"
    }
}