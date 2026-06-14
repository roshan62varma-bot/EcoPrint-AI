package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LoggedAction represents a single eco-friendly activity recorded by the user.
 * It tracks carbon offset values accomplished per action under a specific category on a given date.
 *
 * @property id Auto-generated unique primary key database index.
 * @property date Calendar date the action was performed, formatted as "yyyy-MM-dd".
 * @property actionName Conversational name or label describing the habit action.
 * @property co2Saved Estimated weight of CO2 offset or saved in kilograms (kg).
 * @property category The functional classification (e.g., "transport", "diet", "energy", "waste").
 */
@Entity(tableName = "logged_actions")
public data class LoggedAction(
    @PrimaryKey(autoGenerate = true)
    public val id: Long = 0,
    public val date: String,
    public val actionName: String,
    public val co2Saved: Float,
    public val category: String
)

