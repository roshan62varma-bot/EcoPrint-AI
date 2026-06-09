package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CarbonDatabase
import com.example.data.CarbonRepository
import com.example.data.GeminiService
import com.example.data.LoggedAction
import com.example.data.TrackedDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CarbonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CarbonRepository
    private val prefs = application.getSharedPreferences("eco_print_prefs", android.content.Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString("user_name", "Roshan Varma") ?: "Roshan Varma")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _dailyBudget = MutableStateFlow(prefs.getFloat("daily_budget", 12.0f))
    val dailyBudget: StateFlow<Float> = _dailyBudget.asStateFlow()

    fun updateUserProfile(name: String, budget: Float) {
        _userName.value = name
        _dailyBudget.value = budget
        prefs.edit()
            .putString("user_name", name)
            .putFloat("daily_budget", budget)
            .apply()
    }

    init {
        val database = CarbonDatabase.getDatabase(application)
        repository = CarbonRepository(database.carbonDao())
    }

    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _isToday = MutableStateFlow(true)
    val isToday: StateFlow<Boolean> = _isToday.asStateFlow()

    // Screen input states
    private val _carKm = MutableStateFlow(0f)
    private val _flightHoursYearly = MutableStateFlow(0f)
    private val _transitKm = MutableStateFlow(0f)
    private val _electricityKwh = MutableStateFlow(0f)
    private val _heatingLevel = MutableStateFlow("None") // "None", "Low", "Medium", "High"
    private val _dietPreference = MutableStateFlow("Not Set") // "Not Set", "Vegan", "Vegetarian", "Balanced", "Meat Heavy"
    private val _trashBags = MutableStateFlow(0f)
    private val _recycledChecked = MutableStateFlow(false)

    // AI suggestion states
    private val _aiInsight = MutableStateFlow("")
    val aiInsight: StateFlow<String> = _aiInsight.asStateFlow()

    private val _isLoadingInsight = MutableStateFlow(false)
    val isLoadingInsight: StateFlow<Boolean> = _isLoadingInsight.asStateFlow()

    // Reactively load TrackedDay from DB when date changes
    private val _savedDayState = _selectedDate.flatMapLatest { date ->
        repository.getTrackedDay(date)
    }

    // Reactively load LoggedActions when date changes
    val loggedActions: StateFlow<List<LoggedAction>> = _selectedDate.flatMapLatest { date ->
        repository.getLoggedActionsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Historical record of tracking
    val historicalDays: StateFlow<List<TrackedDay>> = repository.getAllTrackedDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive Consecutive Tracking Streak (Days with tracked entries historically)
    val trackingStreak: StateFlow<Int> = historicalDays.map { days ->
        val sorted = days.distinctBy { it.date }.sortedByDescending { it.date }
        if (sorted.isEmpty()) return@map 0
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var streak = 0
        val cal = Calendar.getInstance()
        cal.time = Date() // Today
        
        var checkingToday = true
        while (true) {
            val formattedDate = sdf.format(cal.time)
            val dayExists = sorted.any { it.date == formattedDate }
            
            if (dayExists) {
                streak++
                cal.add(Calendar.DATE, -1)
                checkingToday = false
            } else {
                if (checkingToday) {
                    cal.add(Calendar.DATE, -1)
                    val formattedYesterday = sdf.format(cal.time)
                    val yesterdayExists = sorted.any { it.date == formattedYesterday }
                    if (yesterdayExists) {
                        streak++
                        cal.add(Calendar.DATE, -1)
                        checkingToday = false
                        continue
                    }
                }
                break
            }
        }
        streak
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Combined UI State
    val uiState: StateFlow<CarbonUiState> = combine(
        _selectedDate,
        _carKm,
        _flightHoursYearly,
        _transitKm,
        _electricityKwh,
        _heatingLevel,
        _dietPreference,
        _trashBags,
        _recycledChecked,
        _savedDayState,
        loggedActions
    ) { flows ->
        val date = flows[0] as String
        val car = flows[1] as Float
        val flight = flows[2] as Float
        val transit = flows[3] as Float
        val elec = flows[4] as Float
        val heating = flows[5] as String
        val diet = flows[6] as String
        val trash = flows[7] as Float
        val recycled = flows[8] as Boolean
        val savedDay = flows[9] as? TrackedDay
        @Suppress("UNCHECKED_CAST")
        val actions = flows[10] as List<LoggedAction>
        
        // Let's compute footprints
        val travelCo2 = computeTransportCo2(car, flight, transit)
        val utilityCo2 = computeUtilityCo2(elec, heating)
        val dietCo2 = computeDietCo2(diet)
        val wasteCo2 = computeWasteCo2(trash, recycled)
        val baseFootprint = travelCo2 + utilityCo2 + dietCo2 + wasteCo2
        
        val offset = actions.sumOf { it.co2Saved.toDouble() }.toFloat()
        val net = (baseFootprint - offset).coerceAtLeast(0f)

        CarbonUiState(
            selectedDate = date,
            carKm = car,
            flightHoursYearly = flight,
            transitKm = transit,
            electricityKwh = elec,
            heatingLevel = heating,
            dietPreference = diet,
            trashBags = trash,
            recycledChecked = recycled,
            transportCo2 = travelCo2,
            utilityCo2 = utilityCo2,
            dietCo2 = dietCo2,
            wasteCo2 = wasteCo2,
            totalFootprint = baseFootprint,
            totalOffset = offset,
            netFootprint = net,
            hasSavedRecord = savedDay != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CarbonUiState(getCurrentDateString())
    )

    init {
        // Automatically load state when inputs change
        viewModelScope.launch {
            _selectedDate.collect { date ->
                _isToday.value = (date == getCurrentDateString())
                loadDayFromDatabase(date)
            }
        }
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private suspend fun loadDayFromDatabase(date: String) {
        // Retrieve only the first item (snapshot) to hydrate screen input fields once and avoid feedback loop
        try {
            val saved = repository.getTrackedDay(date).first()
            if (saved != null) {
                _carKm.value = saved.carKm
                _flightHoursYearly.value = saved.flightHours
                _transitKm.value = saved.transitKm
                _electricityKwh.value = saved.elecKwh
                _heatingLevel.value = saved.heatingLevel
                _dietPreference.value = saved.dietChoice
                _trashBags.value = saved.trashBags
                _recycledChecked.value = saved.recycled
            } else {
                // Reset defaults for a fresh day to zero
                _carKm.value = 0f
                _flightHoursYearly.value = 0f
                _transitKm.value = 0f
                _electricityKwh.value = 0f
                _heatingLevel.value = "None"
                _dietPreference.value = "Not Set"
                _trashBags.value = 0f
                _recycledChecked.value = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var saveJob: kotlinx.coroutines.Job? = null

    private fun saveCurrentDayWithDebounce() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350) // 350ms debounce before writing to Room database
            saveCurrentDayDirectly()
        }
    }

    private suspend fun saveCurrentDayDirectly() {
        val state = uiState.value
        val trackedDay = TrackedDay(
            date = state.selectedDate,
            transportCo2 = state.transportCo2,
            utilityCo2 = state.utilityCo2,
            dietCo2 = state.dietCo2,
            wasteCo2 = state.wasteCo2,
            totalCo2 = state.totalFootprint,
            carKm = state.carKm,
            flightHours = state.flightHoursYearly,
            transitKm = state.transitKm,
            elecKwh = state.electricityKwh,
            heatingLevel = state.heatingLevel,
            dietChoice = state.dietPreference,
            trashBags = state.trashBags,
            recycled = state.recycledChecked
        )
        repository.insertTrackedDay(trackedDay)
    }

    // Update inputs instantly in-memory for lock-solid slider drag frame rate
    fun updateCarKm(km: Float) {
        _carKm.value = km
    }

    fun updateFlightHours(hours: Float) {
        _flightHoursYearly.value = hours
    }

    fun updateTransitKm(km: Float) {
        _transitKm.value = km
    }

    fun updateElectricityKwh(kwh: Float) {
        _electricityKwh.value = kwh
    }

    fun updateHeatingLevel(level: String) {
        _heatingLevel.value = level
        saveCurrentDay()
    }

    fun updateDietPreference(diet: String) {
        _dietPreference.value = diet
        saveCurrentDay()
    }

    fun updateTrashBags(bags: Float) {
        _trashBags.value = bags
        saveCurrentDay()
    }

    fun updateRecycled(checked: Boolean) {
        _recycledChecked.value = checked
        saveCurrentDay()
    }

    fun saveCurrentDay() {
        saveCurrentDayWithDebounce()
    }

    fun copyYesterdayInputs() {
        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = sdf.parse(_selectedDate.value) ?: return@launch
                val cal = Calendar.getInstance()
                cal.time = currentDate
                cal.add(Calendar.DATE, -1)
                val yesterdayStr = sdf.format(cal.time)
                
                val yesterdayData = repository.getTrackedDay(yesterdayStr).first()
                if (yesterdayData != null) {
                    _carKm.value = yesterdayData.carKm
                    _flightHoursYearly.value = yesterdayData.flightHours
                    _transitKm.value = yesterdayData.transitKm
                    _electricityKwh.value = yesterdayData.elecKwh
                    _heatingLevel.value = yesterdayData.heatingLevel
                    _dietPreference.value = yesterdayData.dietChoice
                    _trashBags.value = yesterdayData.trashBags
                    _recycledChecked.value = yesterdayData.recycled
                    
                    // Save to database for today
                    saveCurrentDayDirectly()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changeDateByDays(days: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val date = sdf.parse(_selectedDate.value) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DATE, days)
            
            val todayString = getCurrentDateString()
            val targetString = sdf.format(cal.time)
            
            // Limit to today or past
            if (days < 0 || targetString <= todayString) {
                _selectedDate.value = targetString
                _aiInsight.value = "" // clear insight for new day to encourage reloading
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Log green habits
    fun logHabit(actionName: String, co2Saved: Float, category: String) {
        viewModelScope.launch {
            val action = LoggedAction(
                date = _selectedDate.value,
                actionName = actionName,
                co2Saved = co2Saved,
                category = category
            )
            repository.logAction(action)
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            repository.deleteLoggedAction(id)
        }
    }

    // AI recommendation assistant trigger
    fun generateAIInsights() {
        val currentState = uiState.value
        val actions = loggedActions.value
        _isLoadingInsight.value = true
        _aiInsight.value = ""

        viewModelScope.launch {
            val fieldsMap = mapOf(
                "Transport Emissions" to currentState.transportCo2,
                "Utility Emissions" to currentState.utilityCo2,
                "Diet Emissions" to currentState.dietCo2,
                "Waste Emissions" to currentState.wasteCo2
            )
            val insight = GeminiService.getPersonalizedEcoInsights(
                totalCo2 = currentState.totalFootprint,
                fields = fieldsMap,
                loggedActions = actions
            )
            _aiInsight.value = insight
            _isLoadingInsight.value = false
        }
    }

    // Formulas
    private fun computeTransportCo2(carKm: Float, flightHoursYearly: Float, transitKm: Float): Float {
        return (carKm * 0.20f) + (flightHoursYearly * 250f / 365f) + (transitKm * 0.08f)
    }

    private fun computeUtilityCo2(electricityKwh: Float, heatingLevel: String): Float {
        val heatingOffset = when (heatingLevel) {
            "None" -> 0f
            "Low" -> 1.5f
            "Medium" -> 3.5f
            "High" -> 7.0f
            else -> 3.5f
        }
        return (electricityKwh * 0.45f) + heatingOffset
    }

    private fun computeDietCo2(preference: String): Float {
        return when (preference) {
            "Not Set" -> 0.0f
            "Vegan" -> 0.8f
            "Vegetarian" -> 1.4f
            "Balanced" -> 2.2f
            "Meat Heavy" -> 3.3f
            else -> 0.0f
        }
    }

    private fun computeWasteCo2(trashBags: Float, recycledChecked: Boolean): Float {
        val base = trashBags * 1.5f
        return if (recycledChecked) base * 0.5f else base
    }

    // Reverse conversions for hydrated database models
    private fun Float.toCarKm() = this / 0.20f
    private fun Float.toElecKwh() = this / 0.45f
}

data class CarbonUiState(
    val selectedDate: String,
    val carKm: Float = 0f,
    val flightHoursYearly: Float = 0f,
    val transitKm: Float = 0f,
    val electricityKwh: Float = 0f,
    val heatingLevel: String = "None",
    val dietPreference: String = "Not Set",
    val trashBags: Float = 0f,
    val recycledChecked: Boolean = false,
    
    // Footprint stats (kg CO2)
    val transportCo2: Float = 0f,
    val utilityCo2: Float = 0f,
    val dietCo2: Float = 0f,
    val wasteCo2: Float = 0f,
    
    val totalFootprint: Float = 0f,
    val totalOffset: Float = 0f,
    val netFootprint: Float = 0f,
    val hasSavedRecord: Boolean = false
)

class CarbonViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarbonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarbonViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
