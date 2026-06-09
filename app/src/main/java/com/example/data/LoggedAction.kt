package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logged_actions")
data class LoggedAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,          // format: YYYY-MM-DD
    val actionName: String,    // e.g. "Ate a vegan meal"
    val co2Saved: Float,       // CO2 offset in kg, e.g. 1.5f
    val category: String       // "transport", "diet", "energy", "waste", "other"
)
