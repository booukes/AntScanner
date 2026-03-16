package dev.antworks.antscanner.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.antworks.antscanner.data.db.DocumentEntity
import dev.antworks.antscanner.scanner.AntScannerClient
import java.util.Calendar

// Wymagane dla zaawansowanych animacji i nowych TopBarów w Material 3
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val momName by viewModel.momName.collectAsState()
    val documents by viewModel.documents.collectAsState()

    // Stany dla skanera i dialogu nazwy
    var showNameDialog by remember { mutableStateOf(false) }
    var scannedUri by remember { mutableStateOf<Uri?>(null) }
    var fileNameInput by remember { mutableStateOf("") }

    // Konfiguracja chowanego TopBara przy scrollowaniu
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // Launcher skanera
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                scannedUri = pdf.uri
                showNameDialog = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Luksusowy duży TopBar, który się chowa
            // Zamieniliśmy LargeTopAppBar na zwykły TopAppBar, żeby ubić ten martwy margines na górze
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = getGreetingPrefix(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = momName ?: "Mamo",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            // FAB z animacją pulsu, żeby Mama wiedziała, gdzie kliknąć
            val infiniteTransition = rememberInfiniteTransition(label = "puls")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "skalowanie"
            )

            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val scannerClient = AntScannerClient().getClient()
                    scannerClient.getStartScanIntent(context as Activity)
                        .addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(
                                androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                            )
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Błąd startu skanera: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                // Extra duże zaokrąglenie rogów
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.scale(scale).padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DocumentScanner, // Zmieniłem ikonę na bardziej pasującą do skanowania
                    contentDescription = "Skanuj nowy dokument",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (documents.isEmpty()) {
                // --- LUKSUSOWY PUSTY STAN ---
                EmptyStateView()
            } else {
                // --- LISTA Z ANIMACJĄ WEJŚCIA ---
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // itemsIndexed jest potrzebny do animacji opóźnienia zależnego od pozycji
                    itemsIndexed(
                        items = documents,
                        key = { _, doc -> doc.id } // Klucz dla płynnych zmian na liście
                    ) { index, doc ->
                        // Animacja wjazdu i fade-in
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(600, delayMillis = index * 80)) +
                                    slideInVertically(tween(600, delayMillis = index * 80)) { 50 },
                            modifier = Modifier.animateItem()
                        ) {
                            DocumentItem(doc)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG NAZWY (też lekki tuning) ---
    if (showNameDialog && scannedUri != null) {
        AlertDialog(
            onDismissRequest = { /* Brak powrotu - pancerna zasada */ },
            shape = RoundedCornerShape(28.dp), // Luksusowe extra duże rogi
            title = { Text("Skan gotowy do zapisu", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Nadaj swojemu dokumentowi nazwę.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = fileNameInput,
                        onValueChange = { fileNameInput = it },
                        label = { Text("Nazwa dokumentu") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileNameInput.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.saveScannedDocument(scannedUri!!, fileNameInput.trim())
                            showNameDialog = false
                            scannedUri = null
                            fileNameInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = fileNameInput.isNotBlank()
                ) {
                    Text("Zapisz", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    scannedUri = null
                    fileNameInput = ""
                }) {
                    Text("Anuluj", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

// --- LUKSUSOWY WIDOK ELEMENTU LISTY ---
@Composable
fun DocumentItem(doc: DocumentEntity) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Karta z Tonal Elevation (zamiast cienia) i dużymi rogami 28dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            // TonalElevation sprawia, że kolor surfaceVariant jest lekko przyciemniony w luksusowy sposób
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        // Subtelna granica, żeby wyglądało premium
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- MINIATURA Z PANCERNYMI ROGAMI ---
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(doc.uriString)
                    .crossfade(300)
                    .build(),
                contentDescription = "Podgląd dokumentu",
                modifier = Modifier
                    .size(65.dp, 85.dp) // Lekko większa miniatura
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))
            // -------------------------------------

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Format: PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Zapisano bezpiecznie w Downloads",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Przycisk Share z haptyką
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    shareDocument(context, doc.uriString, doc.name)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Udostępnij",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// --- LUKSUSOWY PUSTY STAN Z GRADIENTEM I IKONĄ ---
@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gradientowa ikona
        Icon(
            imageVector = Icons.Filled.DocumentScanner,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 24.dp),
            // Używamy koloru primary, ale z alfą (przezroczystością) dla elegancji
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        Text(
            text = "Twój osobisty cyfrowy archiwizator",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Nie masz jeszcze żadnych skanów.\nKliknij przycisk poniżej, aby zapisać dokument.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Funkcja pomocnicza do share
fun shareDocument(context: android.content.Context, uriString: String, fileName: String) {
    val uri = Uri.parse(uriString)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Udostępnij skan Mamo"))
}

// Tylko prefiks powitania
fun getGreetingPrefix(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Dzień dobry,"
        in 12..17 -> "Witaj,"
        in 18..22 -> "Dobry wieczór,"
        else -> "Spokojnej nocy,"
    }
}