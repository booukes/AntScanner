package dev.antworks.antscanner.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.antworks.antscanner.data.AntRepository
import dev.antworks.antscanner.data.db.DocumentEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(private val repository: AntRepository) : ViewModel() {

    // Stan Onboardingu: przerabiamy Flow z repo na StateFlow, które Compose zje na śniadanie
    val hasSeenOnboarding: StateFlow<Boolean> = repository.hasSeenOnboardingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Imię Mamy, gotowe do wyświetlenia na głównym ekranie
    val momName: StateFlow<String?> = repository.momNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Soczysta lista zeskanowanych dokumentów prosto z bazy
    val documents: StateFlow<List<DocumentEntity>> = repository.allDocumentsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Zapis z ekranu powitalnego
    fun saveMomName(name: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                repository.saveMomName(name.trim())
            }
        }
    }

    // Odpalane po pomyślnym skanowaniu i podaniu nazwy pliku
    fun saveScannedDocument(tempUri: Uri, fileName: String) {
        viewModelScope.launch {
            val success = repository.processAndSaveScan(tempUri, fileName)
            if (success) {
                Timber.d("ViewModel ogłasza sukces: plik \$fileName zapisany.")
            } else {
                Timber.e("ViewModel zgłasza wyjebkę. Coś poszło nie tak przy zapisie do Downloads.")
            }
        }
    }
}

// Fabryka jest niezbędna, bo klasyczny ViewModel nie potrafi sam przyjąć Repozytorium w konstruktorze
class MainViewModelFactory(private val repository: AntRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Kurwa no, nieznana klasa ViewModelu!")
    }
}