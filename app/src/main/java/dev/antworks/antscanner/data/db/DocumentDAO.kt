package dev.antworks.antscanner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    // Pobieramy wszystko posortowane od najnowszego.
    // Używamy Flow, więc lista w UI zaktualizuje się sama po nowym skanie.
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    // Zapisujemy nowy skan.
    @Insert
    suspend fun insertDocument(document: DocumentEntity)

    // UWAGA: Celowo nie ma tu metody @Delete. Aplikacja jest Mama-Proof.
    // Raz zeskanowane, zostaje w bazie.
}