package dev.antworks.antscanner.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val uriString: String, // Tutaj zapiszemy Uri do pliku w Pobranych jako tekst
    val createdAt: Long = System.currentTimeMillis() // Data skanu do sortowania
)