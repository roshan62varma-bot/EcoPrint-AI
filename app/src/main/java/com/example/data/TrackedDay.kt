package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_days")
data class TrackedDay(
    @PrimaryKey val date: String, // format: YYYY-MM-DD
    val transportCo2: Float,      // calculated from inputs (kg CO2)
    val utilityCo2: Float,        // calculated from inputs (kg CO2)
    val dietCo2: Float,           // calculated from inputs (kg CO2)
    val wasteCo2: Float,          // calculated from inputs (kg CO2)
    val totalCo2: Float,          // pre-calculated sum of above (kg CO2)
    
    // Explicit saved states for lossless restoration
    val carKm: Float = 10f,
    val flightHours: Float = 5f,
    val transitKm: Float = 0f,
    val elecKwh: Float = 8f,
    val heatingLevel: String = "Medium",
    val dietChoice: String = "Balanced",
    val trashBags: Float = 1f,
    val recycled: Boolean = false
)
