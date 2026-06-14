package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LoggedAction
import com.example.data.TrackedDay
import com.example.ui.theme.*

/**
 * CarbonDashboard defines the core multi-tab interface of EcoPrint AI.
 * It coordinates calculations, habit registration lists, historical analytics trends,
 * and custom prompt configurations with Gemini.
 *
 * @param viewModel Central carbon dashboard View Model managing database, flows, and calculations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CarbonDashboard(viewModel: CarbonViewModel): Unit {
    val state by viewModel.uiState.collectAsState()
    val isInsightLoading by viewModel.isLoadingInsight.collectAsState()
    val aiInsightText by viewModel.aiInsight.collectAsState()
    val historyList by viewModel.historicalDays.collectAsState()
    val loggedActions by viewModel.loggedActions.collectAsState()

    val userName by viewModel.userName.collectAsState()
    val dailyBudget by viewModel.dailyBudget.collectAsState()
    val isToday by viewModel.isToday.collectAsState()
    val trackingStreak by viewModel.trackingStreak.collectAsState()

    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else PolishTextPrimary
    val textSecondary = if (isDark) Color(0xFFC4C9C1) else PolishTextSecondary
    val textDarkGreen = if (isDark) Color(0xFFD3E8D0) else PolishTextDarkGreen
    val accentGreen = if (isDark) Color(0xFF5EDD9E) else PolishAccentGreen
    val lightGreenBg = if (isDark) Color(0xFF1E3320) else PolishLightGreen
    val borderColor = if (isDark) Color(0xFF2E332E) else PolishBorder

    var activeTab by remember { mutableStateOf("calculator") } // "calculator", "habits", "ai", "history"

    // Dialog trackers
    var showCustomHabitDialog by remember { mutableStateOf(false) }
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var showStreakDialog by remember { mutableStateOf(false) }
    var activeInfoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val presetActions = listOf(
        Triple("Took a bicycle instead of car", 3.0f, "transport"),
        Triple("Ate a fully plant-based meal", 1.2f, "diet"),
        Triple("Shorted shower (5 min max)", 0.45f, "energy"),
        Triple("Composted organic kitchen waste", 0.6f, "waste"),
        Triple("Hung laundry inline (no dryer)", 0.85f, "energy"),
        Triple("Turned off unused AC/Power", 0.3f, "energy")
    )

    // Lazy list scrolling state
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LeafIcon(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 6.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "EcoPrint AI",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        
        // Single central page-wide LazyColumn. No nested scroll views means 100% smooth, native phone-scrollable.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            
            // --- 1. Client Details Head (Modularized to avoid unnecessary headers recomposition) ---
            item {
                UserProfileHeader(
                    userName = userName,
                    trackingStreak = trackingStreak,
                    accentGreen = accentGreen,
                    textPrimary = textPrimary,
                    isDark = isDark,
                    onProfileClick = { showProfileEditDialog = true },
                    onStreakClick = { showStreakDialog = true }
                )
            }

            // --- 2. Date Navigation Selector ---
            item {
                DateSelector(
                    dateString = state.selectedDate,
                    onPreviousClick = { viewModel.changeDateByDays(-1) },
                    onNextClick = { viewModel.changeDateByDays(1) },
                    isNextEnabled = !isToday
                )
            }

            // --- 3. Circular Core Core Counter Gauge ---
            item {
                CarbonFootprintGaugeCard(
                    baseline = state.totalFootprint,
                    offset = state.totalOffset,
                    net = state.netFootprint,
                    dailyGoal = dailyBudget
                )
            }

            // --- 4. Navigation Tab Bar ---
            item {
                TabNavigationRow(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it }
                )
            }

            // --- 5. Dynamic Tab Sub-panel Injector (Mapped inside LazyColumn) ---
            when (activeTab) {
                
                "calculator" -> {
                    // Title for Context
                    item {
                        Column {
                            Text(
                                text = "Personal Calculator Inputs",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tap any category header to expand or collapse details segment dynamically.",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    if (!state.hasSavedRecord) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E2820) else Color(0xFFE8F5E9)),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isDark) Color(0xFF2C4A34) else PolishMediumGreen),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "✍️ INPUT REQUIRED FOR TODAY",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF5EDD9E) else PolishTextDarkGreen,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = "You haven't logged any entries for ${state.selectedDate} yet. All habits are initialized to 0.0 footprint. Adjust inputs below to calculate emissions, or copy yesterday's values instantly!",
                                        fontSize = 12.sp,
                                        color = textPrimary,
                                        lineHeight = 16.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = { viewModel.copyYesterdayInputs() },
                                            colors = ButtonDefaults.buttonColors(containerColor = PolishMediumGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Copy yesterday icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Carry forward yesterday",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        OutlinedButton(
                                            onClick = { 
                                                // Trigger an instant save with empty/zero inputs to initialize in database
                                                viewModel.saveCurrentDay()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF436B4E) else PolishMediumGreen),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishMediumGreen),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Start from 0.0",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF81C784) else PolishTextDarkGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- SECTION 1: TRANSPORT (Modularized to isolate slider changes) ---
                    item {
                        TransportSectionCard(
                            carKm = state.carKm,
                            transitKm = state.transitKm,
                            flightHoursYearly = state.flightHoursYearly,
                            accentGreen = accentGreen,
                            textPrimary = textPrimary,
                            onCarChange = { viewModel.updateCarKm(it) },
                            onTransitChange = { viewModel.updateTransitKm(it) },
                            onFlightChange = { viewModel.updateFlightHours(it) },
                            onSave = { viewModel.saveCurrentDay() },
                            onInfoDialog = { activeInfoDialog = it }
                        )
                    }

                    // --- SECTION 2: UTILITY & POWER (Modularized to isolate inputs) ---
                    item {
                        UtilitySectionCard(
                            electricityKwh = state.electricityKwh,
                            heatingLevel = state.heatingLevel,
                            accentGreen = accentGreen,
                            textPrimary = textPrimary,
                            textDarkGreen = textDarkGreen,
                            textSecondary = textSecondary,
                            lightGreenBg = lightGreenBg,
                            borderColor = borderColor,
                            onElectricityChange = { viewModel.updateElectricityKwh(it) },
                            onHeatingChange = { viewModel.updateHeatingLevel(it) },
                            onSave = { viewModel.saveCurrentDay() },
                            onInfoDialog = { activeInfoDialog = it }
                        )
                    }

                    // --- SECTION 3: DIET (Modularized to isolate dietary pickers) ---
                    item {
                        NutritionSectionCard(
                            dietPreference = state.dietPreference,
                            accentGreen = accentGreen,
                            textPrimary = textPrimary,
                            textDarkGreen = textDarkGreen,
                            textSecondary = textSecondary,
                            lightGreenBg = lightGreenBg,
                            borderColor = borderColor,
                            onDietChange = { viewModel.updateDietPreference(it) },
                            onInfoDialog = { activeInfoDialog = it }
                        )
                    }

                    // --- SECTION 4: WASTE & RECYCLING (Modularized to isolate waste pickers) ---
                    item {
                        WasteSectionCard(
                            trashBags = state.trashBags,
                            recycledChecked = state.recycledChecked,
                            accentGreen = accentGreen,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark,
                            onTrashBagsChange = { viewModel.updateTrashBags(it) },
                            onRecycledChange = { viewModel.updateRecycled(it) },
                            onInfoDialog = { activeInfoDialog = it }
                        )
                    }
                }

                "habits" -> {
                    item {
                        HabitsTabHeader()
                    }

                    // Preset action chips grid (First row)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetActions.take(3).forEach { (name, savings, cat) ->
                                PresetHabitChip(
                                    name = name,
                                    savings = savings,
                                    onClick = { viewModel.logHabit(name, savings, cat) }
                                )
                            }
                        }
                    }

                    // Preset action chips grid (Second row)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetActions.drop(3).forEach { (name, savings, cat) ->
                                PresetHabitChip(
                                    name = name,
                                    savings = savings,
                                    onClick = { viewModel.logHabit(name, savings, cat) }
                                )
                            }
                        }
                    }

                    // Habit list Section Title with Custom Customizer Button
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Green Log",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { showCustomHabitDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("add_custom_habit_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom Action", fontSize = 11.sp)
                            }
                        }
                    }

                    if (loggedActions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Your log is clear today.\nClick preset actions above to record carbon savings!",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(loggedActions) { action ->
                            LoggedHabitRow(
                                action = action,
                                onDelete = { viewModel.deleteHabit(action.id) }
                            )
                        }
                    }
                }

                "ai" -> {
                    item {
                        AiTabHeader(
                            aiInsightText = aiInsightText,
                            isInsightLoading = isInsightLoading,
                            onGenerateClick = { viewModel.generateAIInsights() }
                        )
                    }
                }

                "history" -> {
                    item {
                        HistoryTabHeader(historyList = historyList)
                    }

                    if (historyList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No historical logs available yet.\nUse your calculator to store daily measurements!",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        items(historyList) { day ->
                            HistoricalDayRow(day)
                        }
                    }
                }
            }
        }
    }

    // Custom Habit Add Dialog
    if (showCustomHabitDialog) {
        CustomHabitAddDialog(
            onDismiss = { showCustomHabitDialog = false },
            onConfirm = { name, co2Savings, category ->
                viewModel.logHabit(name, co2Savings, category)
                showCustomHabitDialog = false
            }
        )
    }

    // Custom Profile Edit Dialog
    if (showProfileEditDialog) {
        UserProfileEditDialog(
            currentName = userName,
            currentBudget = dailyBudget,
            onDismiss = { showProfileEditDialog = false },
            onConfirm = { name, budget ->
                viewModel.updateUserProfile(name, budget)
                showProfileEditDialog = false
            }
        )
    }

    activeInfoDialog?.let { (title, explanation) ->
        AlertDialog(
            onDismissRequest = { activeInfoDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = accentGreen, modifier = Modifier.size(24.dp))
                    Text(text = title, fontWeight = FontWeight.Bold, color = textPrimary)
                }
            },
            text = {
                Text(
                    text = explanation,
                    fontSize = 14.sp,
                    color = textPrimary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { activeInfoDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PolishMediumGreen)
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showStreakDialog) {
        AlertDialog(
            onDismissRequest = { showStreakDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🌱 Eco-Streak & Badges", fontWeight = FontWeight.Bold, color = textPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Current Tracking Streak: $trackingStreak ${if (trackingStreak > 0) "days in a row!" else "days. Start today!"}",
                        fontWeight = FontWeight.Bold,
                        color = accentGreen,
                        fontSize = 15.sp
                    )

                    Text(
                        text = "Consistency builds climate awareness. Log entries each day to unlock premium status badges and secure your consecutive tracking profile.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = borderColor)

                    Text(
                        text = "Milestones & Achievements",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textPrimary
                    )

                    // Milestone list
                    val milestones = listOf(
                        Triple("🌱", "Eco Seeder (1 Day)", "Start your sustainable tracking journey."),
                        Triple("🌿", "Green Sentinel (3 Days)", "Develop standard habits and log 3 entries."),
                        Triple("🌳", "Forest Guardian (5 Days)", "Broaden your footprint awareness to 5 consecutive days."),
                        Triple("👑", "Carbon Hero (7 Days)", "Establish a flawless weekly record of climate actions!")
                    )

                    milestones.forEachIndexed { index, (emoji, title, desc) ->
                        val target = when(index) {
                            0 -> 1
                            1 -> 3
                            2 -> 5
                            3 -> 7
                            else -> 1
                        }
                        val isUnlocked = trackingStreak >= target

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isUnlocked) (if (isDark) Color(0xFF1E2820) else Color(0xFFE8F5E9)) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isUnlocked) PolishMediumGreen else (if (isDark) Color(0xFF2C332E) else Color(0xFFEEEEEE)),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isUnlocked) textDarkGreen else textSecondary
                                )
                                Text(
                                    text = desc,
                                    fontSize = 10.sp,
                                    color = textSecondary,
                                    lineHeight = 12.sp
                                )
                            }
                            if (isUnlocked) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Unlocked",
                                    tint = if (isDark) Color(0xFF81C784) else PolishMediumGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStreakDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PolishMediumGreen)
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ==== Date Navigation Selector ====
/**
 * DateSelector presents date traversal utilities supporting previous/next navigation controls.
 */
@Composable
internal fun DateSelector(
    dateString: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    isNextEnabled: Boolean
): Unit {
    val isDark = isSystemInDarkTheme()
    val dateTextColor = if (isDark) Color(0xFF81C784) else MaterialTheme.colorScheme.primary
    val subTextColor = if (isDark) Color(0xFFCFD5CD) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("date_selector"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("prev_date_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Day",
                    tint = dateTextColor
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateString,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = dateTextColor
                )
                Text(
                    text = "Emissions Record Profile",
                    fontSize = 11.sp,
                    color = subTextColor
                )
            }

            IconButton(
                onClick = onNextClick,
                enabled = isNextEnabled,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("next_date_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Day",
                    tint = if (isNextEnabled) dateTextColor else dateTextColor.copy(alpha = 0.38f)
                )
            }
        }
    }
}

// ==== High-Fidelity Custom Canvas Carbon Dial and Statistics Gauge ====
/**
 * CarbonFootprintGaugeCard displays calculated stats alongside a radial canvas dial gauge representation.
 */
@Composable
internal fun CarbonFootprintGaugeCard(
    baseline: Float,
    offset: Float,
    net: Float,
    dailyGoal: Float
): Unit {
    val maxDialValue = 40f
    val progressFraction = (net / maxDialValue).coerceIn(0f, 1f)

    val dialColor = when {
        net <= 10f -> PolishMediumGreen
        net <= 22f -> Color(0xFFEF6C00) // Moderate Orange
        else -> PolishDangerRed
    }

    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) SoftDarkCard else PolishLightGreen
    val labelColor = if (isDark) Color(0xFFA5BFA7) else PolishBodyTextSage
    val valueColor = if (isDark) Color.White else PolishTextDarkGreen
    val indicatorBg = if (isDark) Color(0xFF222923) else Color.White.copy(alpha = 0.5f)
    val traceBg = if (isDark) Color(0xFF222923) else Color.White.copy(alpha = 0.4f)
    val borderStroke = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333A34)) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(32.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(32.dp),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Text(
                text = "Today's Footprint".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = labelColor,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = String.format("%.1f", net),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = valueColor,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "kg CO₂eq",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = labelColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val levelText = when {
                    net <= 10f -> "Low Impact"
                    net <= 22f -> "Moderate"
                    else -> "High Load"
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = indicatorBg,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(dialColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = levelText,
                            color = valueColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(92.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8.dp.toPx()
                        drawArc(
                            color = traceBg,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = dialColor,
                            startAngle = 135f,
                            sweepAngle = progressFraction * 270f,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "NET IMPACT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = labelColor
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF81C784) else PolishTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Total Footprint: ${String.format("%.1f", baseline)} kg",
                            fontSize = 12.sp,
                            color = valueColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF66BB6A) else PolishAccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Saved Green: -${String.format("%.1f", offset)} kg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFF66BB6A) else PolishAccentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val percentUsed = ((net / dailyGoal) * 100).coerceAtLeast(0f)
            val percentString = if (percentUsed > 100f) "Over limit!" else "${String.format("%.0f", percentUsed)}% used"
            val progressRatio = (net / dailyGoal).coerceIn(0f, 1f)
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Goal Threshold: ${String.format("%.1f", dailyGoal)} kg",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor
                    )
                    Text(
                        text = percentString,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (percentUsed > 100f) PolishDangerRed else (if (isDark) Color(0xFF66BB6A) else PolishAccentGreen)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(traceBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressRatio)
                            .clip(CircleShape)
                            .background(if (percentUsed > 100f) PolishDangerRed else (if (isDark) Color(0xFF66BB6A) else PolishMediumGreen))
                    )
                }
            }
        }
    }
}

// ==== Tab Bar ====
/**
 * TabNavigationRow represents the central navigational tab option card in the main view.
 */
@Composable
internal fun TabNavigationRow(
    activeTab: String,
    onTabSelected: (String) -> Unit
): Unit {
    val tabs = listOf(
        Triple("calculator", "Track", Icons.Default.Edit),
        Triple("habits", "Eco-Habits", Icons.Default.CheckCircle),
        Triple("ai", "AI Insights", Icons.Default.Info),
        Triple("history", "Analytics", Icons.AutoMirrored.Filled.List)
    )

    val isDark = isSystemInDarkTheme()
    val navContainerColor = if (isDark) SoftDarkCard else PolishActiveNavGray
    val activePillBg = if (isDark) PolishMediumGreen else PolishLightGreen
    val activeTextTint = if (isDark) Color.White else PolishTextDarkGreen
    val inactiveTextTint = if (isDark) Color(0xFFC4C9C1) else PolishTextSecondary
    val navBorderColor = if (isDark) Color(0xFF2E332E) else PolishBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag("tab_navigation_bar")
            .border(1.dp, navBorderColor, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = navContainerColor),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { (tabId, label, icon) ->
                val isSelected = activeTab == tabId
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tabId) }
                        .testTag("tab_$tabId")
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .background(activePillBg, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = activeTextTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = inactiveTextTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) activeTextTint else inactiveTextTint
                    )
                }
            }
        }
    }
}

// ==== High-Fidelity Minimizable / Expandable Card Component ====
/**
 * CarbonCalculatorSectionCard handles visual collapsible grouping for specific carbon emission sectors.
 */
@Composable
internal fun CarbonCalculatorSectionCard(
    title: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
    summaryText: String,
    content: @Composable ColumnScope.() -> Unit
): Unit {
    val isDark = isSystemInDarkTheme()
    val sectionCardBorder = if (isDark) Color(0xFF2E332E) else PolishBorder
    val sectionTitleColor = if (isDark) Color.White else PolishTextPrimary
    val sectionSummaryColor = if (isDark) Color(0xFF81C784) else PolishAccentGreen
    val arrowTint = if (isDark) Color(0xFFC4C9C1) else PolishTextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                sectionCardBorder,
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Clickable header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onHeaderClick)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = sectionTitleColor
                        )
                        if (!isExpanded) {
                            Text(
                                text = summaryText,
                                fontSize = 11.sp,
                                color = sectionSummaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = arrowTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        color = PolishBorder.copy(alpha = 0.6f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    content()
                }
            }
        }
    }
}

// ==== Preset Chip ====
/**
 * PresetHabitChip is a reusable row scope component for selecting prefabricated green habits easily.
 */
@Composable
internal fun RowScope.PresetHabitChip(
    name: String,
    savings: Float,
    onClick: () -> Unit
): Unit {
    val iconEmoji = when {
        name.contains("bicycle", ignoreCase = true) || name.contains("cycle", ignoreCase = true) -> "🚲"
        name.contains("meal", ignoreCase = true) || name.contains("eat", ignoreCase = true) || name.contains("diet", ignoreCase = true) -> "🥗"
        name.contains("shower", ignoreCase = true) || name.contains("water", ignoreCase = true) -> "🛀"
        name.contains("kitchen", ignoreCase = true) || name.contains("compost", ignoreCase = true) -> "🥑"
        name.contains("laundry", ignoreCase = true) || name.contains("dryer", ignoreCase = true) -> "👕"
        name.contains("power", ignoreCase = true) || name.contains("ac", ignoreCase = true) -> "🔌"
        else -> "🌱"
    }

    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) SoftDarkCard else PolishSoftGrayGreen
    val strokeColor = if (isDark) Color(0xFF2E332E) else PolishBorder
    val textCol = if (isDark) Color.White else PolishTextPrimary
    val greenCol = if (isDark) Color(0xFF81C784) else PolishAccentGreen

    Surface(
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, strokeColor)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = iconEmoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                val simpleName = name.substringBefore(" (").take(14) + "..."
                Text(
                    text = simpleName,
                    fontSize = 9.sp,
                    color = textCol,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "-${savings} kg",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = greenCol
            )
        }
    }
}

// ==== Logged Habit Row ====
/**
 * LoggedHabitRow renders a single item inside the registered carbon offset habits daily history.
 */
@Composable
internal fun LoggedHabitRow(
    action: LoggedAction,
    onDelete: () -> Unit
): Unit {
    val iconEmoji = when {
        action.actionName.contains("bicycle", ignoreCase = true) || action.actionName.contains("cycle", ignoreCase = true) -> "🚲"
        action.actionName.contains("meal", ignoreCase = true) || action.actionName.contains("eat", ignoreCase = true) -> "🥗"
        action.actionName.contains("shower", ignoreCase = true) -> "🛀"
        action.actionName.contains("compost", ignoreCase = true) -> "🥑"
        action.actionName.contains("laundry", ignoreCase = true) -> "👕"
        action.actionName.contains("power", ignoreCase = true) -> "🔌"
        else -> "🌱"
    }

    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) SoftDarkCard else PolishSoftGrayGreen
    val strokeColor = if (isDark) Color(0xFF2E332E) else PolishBorder
    val boxBg = if (isDark) Color(0xFF222923) else Color.White
    val textCol = if (isDark) Color.White else PolishTextPrimary
    val greenCol = if (isDark) Color(0xFF81C784) else PolishAccentGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("logged_habit_${action.id}"),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, strokeColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(boxBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconEmoji, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = action.actionName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textCol
                    )
                    Text(
                        text = "Offset: -${action.co2Saved} kg CO₂e",
                        fontSize = 11.sp,
                        color = greenCol,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_habit_${action.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = PolishDangerRed.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ==== Simple Custom Graphical Chart Card ====
/**
 * CanvasChartCard displays a visual timeline graph tracking calculations of the past 7 days.
 */
@Composable
internal fun CanvasChartCard(days: List<TrackedDay>): Unit {
    val displayedDays = days.take(7).reversed() // past 7 log entries
    val maxCo2 = (displayedDays.maxOfOrNull { it.totalCo2 } ?: 20f).coerceAtLeast(15f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(134.dp)
            .border(
                1.dp,
                PolishBorder,
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val spaceBetween = size.width / (displayedDays.size.coerceAtLeast(1) + 1)
                val chartHeight = size.height - 20f

                // World Target Line
                val targetY = chartHeight - ((12f / maxCo2) * chartHeight)
                drawLine(
                    color = Color.Red.copy(alpha = 0.4f),
                    start = Offset(0f, targetY),
                    end = Offset(size.width, targetY),
                    strokeWidth = 2f
                )

                // Bars
                displayedDays.forEachIndexed { index, day ->
                    val x = (index + 1) * spaceBetween
                    val barHeightFraction = (day.totalCo2 / maxCo2).coerceAtLeast(0.05f)
                    val barHeight = barHeightFraction * chartHeight
                    val y = chartHeight - barHeight

                    val barColor = if (day.totalCo2 <= 12f) PolishMediumGreen else PolishDangerRed

                    drawRect(
                        color = barColor,
                        topLeft = Offset(x - 12f, y),
                        size = Size(24f, barHeight)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("📅 Logged entries past days", fontSize = 9.sp, color = PolishTextSecondary)
                Text("🔴 Global Target limit (12kg)", fontSize = 9.sp, color = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

// ==== Historical Records Row ====
/**
 * HistoricalDayRow presents a historical snapshot of calculated emissions for analytics summaries.
 */
@Composable
internal fun HistoricalDayRow(day: TrackedDay): Unit {
    val ratingColor = if (day.totalCo2 <= 12f) PolishMediumGreen else PolishDangerRed
    val isDark = isSystemInDarkTheme()
    val rowBg = if (isDark) SoftDarkCard else PolishSoftGrayGreen
    val rowBorderColor = if (isDark) Color(0xFF2E332E) else PolishBorder
    val primaryText = if (isDark) Color.White else PolishTextPrimary
    val secondaryText = if (isDark) Color(0xFFC4C9C1) else PolishTextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${day.date}"),
        colors = CardDefaults.cardColors(containerColor = rowBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, rowBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = day.date,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = primaryText
                )
                Text(
                    text = "Transport: ${String.format("%.1f", day.transportCo2)} | Energy: ${String.format("%.1f", day.utilityCo2)} kg",
                    fontSize = 11.sp,
                    color = secondaryText
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ratingColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${String.format("%.1f", day.totalCo2)} kg",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = ratingColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ==== Animated/Inline Leaf Icon Graphics ====
/**
 * LeafIcon represents a beautiful organic vector leaf canvas graphic.
 */
@Composable
public fun LeafIcon(modifier: Modifier = Modifier, color: Color = PolishMediumGreen): Unit {
    Canvas(modifier = modifier) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.8f)
            quadraticTo(
                size.width * 0.15f, size.height * 0.35f,
                size.width * 0.5f, size.height * 0.15f
            )
            quadraticTo(
                size.width * 0.85f, size.height * 0.15f,
                size.width * 0.8f, size.height * 0.5f
            )
            quadraticTo(
                size.width * 0.75f, size.height * 0.8f,
                size.width * 0.2f, size.height * 0.8f
            )
            moveTo(size.width * 0.2f, size.height * 0.8f)
            lineTo(size.width * 0.65f, size.height * 0.35f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// ==== Custom Habit Dialog ====
/**
 * CustomHabitAddDialog displays an overlay alert modal enabling registration/logging of bespoke carbon reduction actions.
 */
@Composable
internal fun CustomHabitAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Float, String) -> Unit
): Unit {
    var name by remember { mutableStateOf("") }
    var savingsString by remember { mutableStateOf("1.5") }
    var selectedCategory by remember { mutableStateOf("transport") }

    val isDark = isSystemInDarkTheme()
    val defaultTextCol = if (isDark) Color.White else PolishTextPrimary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Custom Eco Action", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Action Name") },
                    placeholder = { Text("e.g. Unplugged chargers all day") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_habit_name_input")
                )

                OutlinedTextField(
                    value = savingsString,
                    onValueChange = { savingsString = it },
                    label = { Text("Estimated savings (kg CO₂)") },
                    placeholder = { Text("e.g. 1.5") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_habit_savings_input")
                )

                Text(text = "Category", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = defaultTextCol)
                val categories = listOf("transport", "diet", "energy", "waste", "other")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCategory = cat },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                             ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cat.replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(6.dp),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else defaultTextCol
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val savings = savingsString.toFloatOrNull() ?: 1.0f
                    if (name.isNotBlank()) {
                        onConfirm(name, savings, selectedCategory)
                    }
                },
                modifier = Modifier.testTag("confirm_custom_habit_button")
            ) {
                Text("Add to Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==== High-Fidelity Custom User Profile Customization Dialog ====
/**
 * UserProfileEditDialog displays an overlay alert modal enabling modification/customization of user profile name and target daily carbon budget.
 */
@Composable
internal fun UserProfileEditDialog(
    currentName: String,
    currentBudget: Float,
    onDismiss: () -> Unit,
    onConfirm: (String, Float) -> Unit
): Unit {
    var nameInput by remember { mutableStateOf(currentName) }
    var budgetInput by remember { mutableStateOf(currentBudget.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Eco Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your Name") },
                    placeholder = { Text("e.g. Roshan Varma") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Daily Target Budget (kg CO₂)") },
                    placeholder = { Text("e.g. 12.0") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_budget_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val budget = budgetInput.toFloatOrNull() ?: 12.0f
                    if (nameInput.isNotBlank()) {
                        onConfirm(nameInput, budget)
                    }
                },
                modifier = Modifier.testTag("confirm_profile_button")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==== Modularized Helper Composables ====

@Composable
internal fun UserProfileHeader(
    userName: String,
    trackingStreak: Int,
    accentGreen: Color,
    textPrimary: Color,
    isDark: Boolean,
    onProfileClick: () -> Unit,
    onStreakClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onProfileClick() }
            .testTag("user_profile_header")
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val avatarInitials = remember(userName) {
            val parts = userName.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                (parts[0].take(1) + parts[1].take(1)).uppercase()
            } else if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                parts[0].take(2).uppercase()
            } else {
                "RV"
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (isDark) Color(0xFF2E4E30) else Color(0xFFD1E8D1), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarInitials,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF111F11),
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WELCOME BACK (TAP TO EDIT PROFILE)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentGreen,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (trackingStreak > 0) MaterialTheme.colorScheme.primaryContainer else if (isDark) Color(0xFF2C352E) else Color(0xFFE2EBE3),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onStreakClick() }
            ) {
                Text(
                    text = if (trackingStreak > 0) "🔥 $trackingStreak Day Streak" else "🌱 0 Day Streak",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = if (trackingStreak > 0) MaterialTheme.colorScheme.onPrimaryContainer else if (isDark) Color(0xFFA2CBA5) else PolishTextDarkGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun TransportSectionCard(
    carKm: Float,
    transitKm: Float,
    flightHoursYearly: Float,
    accentGreen: Color,
    textPrimary: Color,
    onCarChange: (Float) -> Unit,
    onTransitChange: (Float) -> Unit,
    onFlightChange: (Float) -> Unit,
    onSave: () -> Unit,
    onInfoDialog: (Pair<String, String>) -> Unit
) {
    var transportExpanded by remember { mutableStateOf(true) }

    CarbonCalculatorSectionCard(
        title = "1. Transport Footprint (0.2kg CO₂/km)",
        leadingIcon = Icons.Default.PlayArrow,
        accentColor = PolishMediumGreen,
        isExpanded = transportExpanded,
        onHeaderClick = { transportExpanded = !transportExpanded },
        summaryText = "Car: ${carKm.toInt()} km • Transit: ${transitKm.toInt()} km • Flights: ${flightHoursYearly.toInt()} hrs"
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Personal Car Distance: ${String.format("%.0f", carKm)} km/day",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            IconButton(
                onClick = {
                    onInfoDialog("Personal Car Distance" to "Commuting by car has a high carbon impact. On average, standard engines release 0.20 kg of CO₂ per kilometer driven. Transitioning to electric cars or carpooling helps save emission offsets.")
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Car distance explanation",
                    tint = accentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Slider(
            value = carKm,
            onValueChange = onCarChange,
            onValueChangeFinished = onSave,
            valueRange = 0f..150f,
            colors = SliderDefaults.colors(
                thumbColor = PolishMediumGreen,
                activeTrackColor = PolishMediumGreen
            ),
            modifier = Modifier.testTag("car_slider")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Public Transit: ${String.format("%.0f", transitKm)} km/day",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            IconButton(
                onClick = {
                    onInfoDialog("Public Transit" to "Trains, buses, and subways are highly efficient. They emit only around 0.08 kg of CO₂ per passenger kilometer, sharing the transit footprint effectively.")
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Public Transit explanation",
                    tint = accentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Slider(
            value = transitKm,
            onValueChange = onTransitChange,
            onValueChangeFinished = onSave,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = PolishMediumGreen,
                activeTrackColor = PolishMediumGreen
            ),
            modifier = Modifier.testTag("transit_slider")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Yearly Flight Hours: ${String.format("%.0f", flightHoursYearly)} hours",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            IconButton(
                onClick = {
                    onInfoDialog("Yearly Flight Hours" to "Aviation emissions are extremely dense. Flying contributes approximately 250 kg of CO₂ per hour in the air. Offsets or fewer flights make a major climate difference.")
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Flight hours explanation",
                    tint = accentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Slider(
            value = flightHoursYearly,
            onValueChange = onFlightChange,
            onValueChangeFinished = onSave,
            valueRange = 0f..80f,
            colors = SliderDefaults.colors(
                thumbColor = PolishMediumGreen,
                activeTrackColor = PolishMediumGreen
            )
        )
    }
}

@Composable
internal fun UtilitySectionCard(
    electricityKwh: Float,
    heatingLevel: String,
    accentGreen: Color,
    textPrimary: Color,
    textDarkGreen: Color,
    textSecondary: Color,
    lightGreenBg: Color,
    borderColor: Color,
    onElectricityChange: (Float) -> Unit,
    onHeatingChange: (String) -> Unit,
    onSave: () -> Unit,
    onInfoDialog: (Pair<String, String>) -> Unit
) {
    var utilitiesExpanded by remember { mutableStateOf(true) }

    CarbonCalculatorSectionCard(
        title = "2. Utility & Power (0.45kg CO₂/kW)",
        leadingIcon = Icons.Default.Star,
        accentColor = PolishMediumGreen,
        isExpanded = utilitiesExpanded,
        onHeaderClick = { utilitiesExpanded = !utilitiesExpanded },
        summaryText = "Elec: ${electricityKwh.toInt()} kWh • Heat Fuel: $heatingLevel"
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Electricity Used: ${String.format("%.0f", electricityKwh)} kWh/day",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            IconButton(
                onClick = {
                    onInfoDialog("Electricity Power Consumption" to "Electrical grids rely on fossil fuels in many areas. Every kWh consumed averages 0.45 kg of CO₂ emissions. Energy-saving appliances and solar power help minimize this footprint.")
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Electricity explanation",
                    tint = accentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Slider(
            value = electricityKwh,
            onValueChange = onElectricityChange,
            onValueChangeFinished = onSave,
            valueRange = 0f..50f,
            colors = SliderDefaults.colors(
                thumbColor = PolishMediumGreen,
                activeTrackColor = PolishMediumGreen
            ),
            modifier = Modifier.testTag("electricity_slider")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Home Heating Fuel Intensity: $heatingLevel",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            IconButton(
                onClick = {
                    onInfoDialog("Home Heating Fuel" to "Heating fuel types differ heavily in carbon intensity. 'None' represents electric/heat pumps, whereas 'Low', 'Medium', or 'High' denote natural gas/heating oil furnace systems.")
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Heating fuel explanation",
                    tint = accentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val levels = listOf("None", "Low", "Medium", "High")
            levels.forEach { level ->
                val isSelected = heatingLevel == level
                OutlinedButton(
                    onClick = { onHeatingChange(level) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) lightGreenBg else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PolishMediumGreen else borderColor
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = level,
                        fontSize = 11.sp,
                        color = if (isSelected) textDarkGreen else textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
internal fun NutritionSectionCard(
    dietPreference: String,
    accentGreen: Color,
    textPrimary: Color,
    textDarkGreen: Color,
    textSecondary: Color,
    lightGreenBg: Color,
    borderColor: Color,
    onDietChange: (String) -> Unit,
    onInfoDialog: (Pair<String, String>) -> Unit
) {
    var nutritionExpanded by remember { mutableStateOf(false) }

    CarbonCalculatorSectionCard(
        title = "3. Nutrition & Food Style",
        leadingIcon = Icons.Default.Favorite,
        accentColor = PolishMediumGreen,
        isExpanded = nutritionExpanded,
        onHeaderClick = { nutritionExpanded = !nutritionExpanded },
        summaryText = "Preference: $dietPreference"
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Chosen Preference: $dietPreference",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            IconButton(
                onClick = {
                    onInfoDialog("Diet Footprint Preferences" to "Diets differ widely in resource demand. Vegan/plant-based diets emit only ~1.2 kg CO₂/day. Vegetarian diets average ~1.7 kg CO₂/day. Balanced diets emit ~2.5 kg CO₂/day, whereas Meat-Heavy diets exceed ~4.2 kg CO₂/day due to intensive livestock emissions.")
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Diet explanation",
                    tint = accentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val diets = listOf("Vegan", "Vegetarian", "Balanced", "Meat Heavy")
            diets.forEach { diet ->
                val isSelected = dietPreference == diet
                val color = if (isSelected) textDarkGreen else textSecondary
                OutlinedButton(
                    onClick = { onDietChange(diet) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) lightGreenBg else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PolishMediumGreen else borderColor
                    )
                ) {
                    Text(
                        text = diet,
                        fontSize = 9.sp,
                        color = color,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
internal fun WasteSectionCard(
    trashBags: Float,
    recycledChecked: Boolean,
    accentGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    onTrashBagsChange: (Float) -> Unit,
    onRecycledChange: (Boolean) -> Unit,
    onInfoDialog: (Pair<String, String>) -> Unit
) {
    var wasteExpanded by remember { mutableStateOf(false) }

    CarbonCalculatorSectionCard(
        title = "4. Waste & Recycle (1.5kg CO₂/bag)",
        leadingIcon = Icons.Default.Warning,
        accentColor = PolishMediumGreen,
        isExpanded = wasteExpanded,
        onHeaderClick = { wasteExpanded = !wasteExpanded },
        summaryText = "Trash bags: ${String.format("%.1f", trashBags)} bags • Recycled: ${if (recycledChecked) "Yes" else "No"}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Daily Trash Generation",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(
                        onClick = {
                            onInfoDialog("Daily Trash Impact" to "Unsorted municipal solid waste decomposes in landfills to release powerful greenhouse gases including methane. Each standard trash bag (~30L) is calculated at approximately 1.5 kg of lifecycle CO₂.")
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Trash explanation",
                            tint = accentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${String.format("%.1f", trashBags)} trash bags",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onTrashBagsChange((trashBags - 0.5f).coerceAtLeast(0f)) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Decrease", tint = if (isDark) Color(0xFFE57373) else PolishDangerRed.copy(alpha = 0.8f))
                }
                IconButton(
                    onClick = { onTrashBagsChange(trashBags + 0.5f) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = if (isDark) Color(0xFF81C784) else PolishMediumGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(
                checked = recycledChecked,
                onCheckedChange = { onRecycledChange(it) },
                modifier = Modifier.testTag("recycle_checkbox")
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Recycle Paper, Plastic, Glass (-50% waste CO₂)",
                fontSize = 12.sp,
                color = textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false)
            )
            IconButton(
                onClick = {
                    onInfoDialog("Recycle Savings Offset" to "Sorting and sending paper, plastics, glass, and tins to recycling loops significantly bypasses mining extraction carbon demands, reducing waste's carbon impact by 50%!")
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Recycling explanation",
                    tint = accentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
internal fun HabitsTabHeader() {
    Column {
        Text(
            text = "Log Daily Saving Actions",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Select actions you took today to reduce your baseline emissions:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

@Composable
internal fun AiTabHeader(
    aiInsightText: String,
    isInsightLoading: Boolean,
    onGenerateClick: () -> Unit
) {
    Column {
        Text(
            text = "Gemini Advisor Insights",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Receive custom eco analyses, comparison values, and actionable sustainability rules directly from Gemini Flash based on your daily emissions data.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        Button(
            onClick = onGenerateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ai_insights_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Analyze with EcoPrint AI", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun HistoryTabHeader(historyList: List<TrackedDay>) {
    Column {
        Text(
            text = "Impact Analytics History",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Track your historical performance relative to the global daily average threshold of 12.0 kg CO₂.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

