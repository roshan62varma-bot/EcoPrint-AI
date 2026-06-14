package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TrackedDay stores calculated footprint details and individual slider parameter states for a single day.
 * This ensures lossless state restoration during dates navigation in the dashboard.
 *
 * @property date Single unique database key string, formatted as "yyyy-MM-dd".
 * @property transportCo2 Total computed transport emission load in kg.
 * @property utilityCo2 Total computed home power/utility emission load in kg.
 * @property dietCo2 Total computed food/dietary emission load in kg.
 * @property wasteCo2 Total computed waste/refuse emission load in kg.
 * @property totalCo2 Overarching base carbon emissions sum calculated for this day.
 * @property carKm Traveled personal car kilometers.
 * @property flightHours Scheduled flight hours.
 * @property transitKm Traveled public transit kilometers.
 * @property elecKwh Electrical power consumed in kWh.
 * @property heatingLevel Home heating fuel selection level.
 * @property dietChoice Configured dietary style choice.
 * @property trashBags Disposed household refuse bags.
 * @property recycled Flag marking active sorted recycling habits.
 */
@Entity(tableName = "tracked_days")
public data class TrackedDay(
    @PrimaryKey
    public val date: String,
    public val transportCo2: Float,
    public val utilityCo2: Float,
    public val dietCo2: Float,
    public val wasteCo2: Float,
    public val totalCo2: Float,
    public val carKm: Float = 10f,
    public val flightHours: Float = 5f,
    public val transitKm: Float = 0f,
    public val elecKwh: Float = 8f,
    public val heatingLevel: String = "Medium",
    public val dietChoice: String = "Balanced",
    public val trashBags: Float = 1f,
    public val recycled: Boolean = false
)

