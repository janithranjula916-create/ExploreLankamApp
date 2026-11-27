package com.example.explorelanka.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.explorelanka.data.location.LocationRepository
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.explorelanka.data.DestinationData
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.local.entity.SmartPlanWithStops
import com.example.explorelanka.data.planner.*
import com.example.explorelanka.data.repository.SmartPlanRepository
import com.example.explorelanka.data.weather.DestinationCoordinates
import com.example.explorelanka.viewmodel.SmartPlannerViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Forest = Color(0xFF10291F)
private val Sage = Color(0xFFDCEADD)
private val Accent = Color(0xFFDBE974)
private val Soft = Color(0xFFF6F7F1)
private val InkMuted = Color(0xFF54695D)
private val DateStyle = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

@Composable
fun SmartTripPlannerScreen(
    userId: String,
    onBackClick: () -> Unit,
    onMapDestinationClick: (String) -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) { ExploreLankaDatabase.getInstance(context.applicationContext) }
    val model: SmartPlannerViewModel = viewModel(
        key = "smart-planner-$userId",
        factory = remember(userId, database) {
            SmartPlannerViewModel.Factory(SmartPlanRepository(database, userId))
        }
    )
    val draft by model.draft.collectAsState()
    val savedPlans by model.savedPlans.collectAsState()
    val message by model.message.collectAsState()
    val busy by model.busy.collectAsState()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val allDestinations = DestinationData.places.toList()
    val mappedPlaces = allDestinations.mapNotNull { destination ->
        DestinationCoordinates.forId(destination.id)?.let { coordinates ->
            PlannerPlace(
                destination.id, destination.name, destination.category,
                coordinates.latitude, coordinates.longitude
            )
        }
    }
    val interestOptions = mappedPlaces.map { it.category }.distinct().sorted()

    var city by rememberSaveable { mutableStateOf("Colombo") }
    var startingLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var startingLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var gpsLoading by remember { mutableStateOf(false) }
    var placesLoading by remember { mutableStateOf(false) }
    val gpsRepository = remember(context) { LocationRepository(context.applicationContext) }

    // The API key comes from your existing Step 6 Maps manifest metadata.
    // No key is embedded in Kotlin source code.
    val placesAvailable = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA
            )
            val key = appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
            if (key.isBlank() || key == "DEFAULT_API_KEY") false else {
                if (!Places.isInitialized()) {
                    Places.initializeWithNewPlacesApiEnabled(context.applicationContext, key)
                }
                true
            }
        }.getOrDefault(false)
    }
    var startDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var duration by rememberSaveable { mutableIntStateOf(4) }
    var budgetText by rememberSaveable { mutableStateOf("100000") }
    var interestsText by rememberSaveable { mutableStateOf("") }
    val interests = interestsText.split(',').filter { it.isNotBlank() }.toSet()
    var cityPickerOpen by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val placeSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data
        if (result.resultCode == PlaceAutocompleteActivity.RESULT_OK && intent != null) {
            val prediction = PlaceAutocomplete.getPredictionFromIntent(intent)
            val sessionToken = PlaceAutocomplete.getSessionTokenFromIntent(intent)
            if (prediction != null && placesAvailable) {
                placesLoading = true
                val request = FetchPlaceRequest.builder(
                    prediction.placeId, listOf(Place.Field.LOCATION)
                ).setSessionToken(sessionToken).build()
                Places.createClient(context).fetchPlace(request)
                    .addOnSuccessListener { response ->
                        placesLoading = false
                        val position = response.place.location
                        when {
                            position == null -> scope.launch { snack.showSnackbar("This place has no coordinates.") }
                            position.latitude !in 5.8..10.1 || position.longitude !in 79.4..82.3 ->
                                scope.launch { snack.showSnackbar("Select a location in Sri Lanka.") }
                            else -> {
                                city = prediction.getPrimaryText(null).toString().take(100)
                                startingLatitude = position.latitude
                                startingLongitude = position.longitude
                                cityPickerOpen = false
                            }
                        }
                    }
                    .addOnFailureListener { error ->
                        placesLoading = false
                        scope.launch { snack.showSnackbar("Google Places error: ${error.localizedMessage ?: "try again"}") }
                    }
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED && intent != null) {
            val status = PlaceAutocomplete.getResultStatusFromIntent(intent)
            scope.launch { snack.showSnackbar("Place search: ${status?.statusMessage ?: "unavailable"}") }
        }
    }

    fun requestGpsFix() {
        if (gpsLoading) return
        scope.launch {
            gpsLoading = true
            try {
                val position = gpsRepository.getCurrentPosition()
                when {
                    position == null -> snack.showSnackbar("Cannot determine location. Try Google Maps in the emulator first.")
                    position.latitude !in 5.8..10.1 || position.longitude !in 79.4..82.3 ->
                        snack.showSnackbar("GPS is outside Sri Lanka. Set an emulator test point in Sri Lanka.")
                    else -> {
                        city = "Current location"
                        startingLatitude = position.latitude
                        startingLongitude = position.longitude
                        cityPickerOpen = false
                    }
                }
            } catch (e: Exception) {
                snack.showSnackbar(e.localizedMessage ?: "Location unavailable. Check device GPS.")
            } finally {
                gpsLoading = false
            }
        }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) requestGpsFix()
        else scope.launch { snack.showSnackbar("Location permission denied. Offline city selection still works.") }
    }

    fun openGps() {
        if (gpsRepository.hasLocationPermission()) requestGpsFix()
        else gpsPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    fun openGoogleSearch() {
        if (!placesAvailable) {
            scope.launch { snack.showSnackbar("Maps API key is not configured for Places.") }
            return
        }
        runCatching {
            PlaceAutocomplete.IntentBuilder()
                .setCountries(listOf("LK"))
                .build(context)
        }.onSuccess { intent ->
            placeSearchLauncher.launch(intent)
        }.onFailure { error ->
            scope.launch { snack.showSnackbar(error.localizedMessage ?: "Cannot start Google Places search.") }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snack.showSnackbar(it)
            model.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(Soft)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 38.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "top") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = Forest)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("EXPLORE LANKA", style = MaterialTheme.typography.labelLarge,
                        color = Forest, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Surface(color = Sage, shape = RoundedCornerShape(50)) {
                        Text("OFFLINE READY", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall, color = Forest,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
            item(key = "hero") {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                        .background(Brush.linearGradient(listOf(Forest, Color(0xFF31614C))))
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Icon(Icons.Outlined.Map, contentDescription = null,
                            tint = Accent, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Every great trip", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("starts with a plan.", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = Accent)
                        Spacer(Modifier.height(10.dp))
                        Text("Make a day-by-day Sri Lanka itinerary with your dates, interests and budget.",
                            color = Color.White.copy(alpha = .85f),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item(key = "input") {
                Surface(color = Color.White, shape = RoundedCornerShape(27.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Your next adventure", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = Forest)
                        Text("STEP 01  /  TRIP PREFERENCES",
                            color = InkMuted, style = MaterialTheme.typography.labelMedium)

                        Text("Starting city", fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = { cityPickerOpen = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Outlined.Place, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(city, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        }
                        Text("Choose from 62 offline towns, search Google Places, or use GPS",
                            style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        if (startingLatitude != null && startingLongitude != null) {
                            Text(
                                "Selected coordinates: %.5f, %.5f".format(
                                    Locale.ENGLISH, startingLatitude, startingLongitude
                                ),
                                style = MaterialTheme.typography.bodySmall, color = Forest
                            )
                        }
                        Text("Travel start date", fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = {
                                val current = runCatching { LocalDate.parse(startDateText) }
                                    .getOrElse { LocalDate.now() }
                                DatePickerDialog(context, { _, year, month, day ->
                                    startDateText = LocalDate.of(year, month + 1, day).toString()
                                }, current.year, current.monthValue - 1, current.dayOfMonth).apply {
                                    datePicker.minDate = System.currentTimeMillis() - 86_400_000L
                                }.show()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(LocalDate.parse(startDateText).format(DateStyle),
                                modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.Edit, contentDescription = null,
                                modifier = Modifier.size(17.dp))
                        }
                        Text("How many days?", fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedIconButton(onClick = { duration = (duration - 1).coerceAtLeast(1) },
                                enabled = duration > 1) {
                                Icon(Icons.Outlined.Remove, "Reduce trip length")
                            }
                            Text("$duration days", modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            OutlinedIconButton(onClick = { duration = (duration + 1).coerceAtMost(10) },
                                enabled = duration < 10) {
                                Icon(Icons.Outlined.Add, "Increase trip length")
                            }
                        }
                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = { budgetText = it.filter(Char::isDigit).take(12) },
                            label = { Text("Total budget in LKR") },
                            leadingIcon = { Icon(Icons.Outlined.Payments, null) },
                            supportingText = { Text("Planning estimate only, not live prices") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Text("What do you enjoy?", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            interestOptions.forEach { category ->
                                FilterChip(
                                    selected = category in interests,
                                    onClick = {
                                        val updated = interests.toMutableSet()
                                        if (category in updated) updated.remove(category) else updated.add(category)
                                        interestsText = updated.sorted().joinToString(",")
                                    },
                                    label = { Text(category) }
                                )
                            }
                        }
                        Text("Leave interests unselected to include every category.",
                            style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        Button(
                            onClick = {
                                val date = runCatching { LocalDate.parse(startDateText) }.getOrNull()
                                val budget = budgetText.toLongOrNull()
                                when {
                                    date == null -> scope.launch { snack.showSnackbar("Choose a valid date.") }
                                    date.isBefore(LocalDate.now()) -> scope.launch { snack.showSnackbar("Select today or a future date.") }
                                    budget == null || budget <= 0 -> scope.launch { snack.showSnackbar("Enter a valid budget in LKR.") }
                                    else -> {
                                        model.generate(
                                            date, duration, city, interests, budget, mappedPlaces,
                                            startingLatitude = startingLatitude,
                                            startingLongitude = startingLongitude
                                        )
                                        scope.launch { listState.animateScrollToItem(3) }
                                    }
                                }
                            },
                            enabled = mappedPlaces.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp),
                            shape = RoundedCornerShape(19.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Forest,
                                contentColor = Color.White)
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, null)
                            Spacer(Modifier.width(9.dp))
                            Text("Generate itinerary", fontWeight = FontWeight.Bold)
                        }
                        if (mappedPlaces.isEmpty()) {
                            Text("No attractions have configured coordinates yet.",
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (draft != null) {
                item(key = "previewHeader") {
                    val plan = draft!!
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("STEP 02  /  YOUR ITINERARY", color = InkMuted,
                            style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = plan.title,
                            onValueChange = model::rename,
                            label = { Text("Trip name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(17.dp)
                        )
                        val estimate = SmartPlannerEngine.totalEstimate(plan.numberOfDays, plan.stops)
                        val underBudget = estimate <= plan.budgetLkr
                        Surface(color = if (underBudget) Sage else Color(0xFFFFF1D6),
                            shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(17.dp)) {
                                Text("Estimated total", style = MaterialTheme.typography.labelMedium,
                                    color = Forest)
                                Text(lkr(estimate), style = MaterialTheme.typography.headlineMedium,
                                    color = Forest, fontWeight = FontWeight.Bold)
                                Text("Your budget: ${lkr(plan.budgetLkr)}", color = InkMuted)
                                Text(if (underBudget) "Within your planned budget" else
                                    "Estimate exceeds budget by ${lkr(estimate - plan.budgetLkr)}",
                                    color = Forest, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(7.dp))
                                Text("Illustrative allowances: LKR 18,000/day for food, stay and travel, plus category-based admissions. Real prices and road travel may differ.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted)
                            }
                        }
                    }
                }
                val plan = draft!!
                items((0 until plan.numberOfDays).toList(), key = { "day-$it" }) { day ->
                    val dayStops = plan.stops.filter { it.dayIndex == day }.sortedBy { it.position }
                    Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Forest, shape = RoundedCornerShape(15.dp)) {
                                    Text("${day + 1}", Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                                        color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Day ${day + 1}", style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold)
                                    Text(plan.startDate.plusDays(day.toLong()).format(DateStyle),
                                        style = MaterialTheme.typography.bodySmall, color = InkMuted)
                                }
                                if (dayStops.isNotEmpty()) {
                                    TextButton(onClick = {
                                        openDayDirections(context, plan, day, mappedPlaces)
                                    }) {
                                        Icon(Icons.Outlined.Directions, null,
                                            modifier = Modifier.size(19.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Route")
                                    }
                                }
                            }
                            if (dayStops.isEmpty()) {
                                Text("Free day — relax or add a destination.", color = InkMuted)
                            } else {
                                dayStops.forEach { stop ->
                                    val destination = allDestinations.firstOrNull { it.id == stop.destinationId }
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (destination != null) {
                                            AsyncImage(
                                                model = destination.imageUrl,
                                                contentDescription = destination.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(67.dp).clip(RoundedCornerShape(13.dp))
                                            )
                                        } else {
                                            Box(Modifier.size(67.dp).clip(RoundedCornerShape(13.dp))
                                                .background(Sage), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Outlined.Place, contentDescription = null)
                                            }
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(destination?.name ?: stop.destinationId,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text("Admission estimate: ${lkr(stop.estimatedAdmissionLkr)}",
                                                style = MaterialTheme.typography.bodySmall, color = InkMuted)
                                            TextButton(onClick = { onMapDestinationClick(stop.destinationId) },
                                                contentPadding = PaddingValues(0.dp)) {
                                                Text("View on Explore map")
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            IconButton(onClick = { model.moveDay(stop.destinationId, -1) },
                                                enabled = day > 0) {
                                                Icon(Icons.Outlined.KeyboardArrowUp, "Move to previous day")
                                            }
                                            IconButton(onClick = { model.moveDay(stop.destinationId, 1) },
                                                enabled = day < plan.numberOfDays - 1) {
                                                Icon(Icons.Outlined.KeyboardArrowDown, "Move to next day")
                                            }
                                            IconButton(onClick = { model.removeStop(stop.destinationId) }) {
                                                Icon(Icons.Outlined.DeleteOutline, "Remove destination",
                                                    tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                    if (stop != dayStops.last()) HorizontalDivider()
                                }
                            }
                            var addMenu by remember(plan.id, day, plan.stops) { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { addMenu = true },
                                    shape = RoundedCornerShape(15.dp)) {
                                    Icon(Icons.Outlined.Add, null)
                                    Spacer(Modifier.width(5.dp))
                                    Text("Add a destination")
                                }
                                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                                    val choices = mappedPlaces.filterNot { place ->
                                        plan.stops.any { it.destinationId == place.id }
                                    }
                                    if (choices.isEmpty()) {
                                        DropdownMenuItem(text = { Text("All destinations included") },
                                            enabled = false, onClick = {})
                                    }
                                    choices.forEach { place ->
                                        DropdownMenuItem(text = { Text(place.name) },
                                            onClick = { model.addStop(place, day); addMenu = false })
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "save") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = model::saveDraft,
                            enabled = !busy && plan.stops.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp),
                            shape = RoundedCornerShape(19.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Forest)
                        ) {
                            Icon(Icons.Outlined.Save, null)
                            Spacer(Modifier.width(9.dp))
                            Text(if (plan.id == null) "Save itinerary offline" else "Save changes",
                                fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = model::clearDraft,
                            modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Close itinerary")
                        }
                    }
                }
            }

            item(key = "savedHeader") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Saved adventures", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${savedPlans.size}", color = InkMuted)
                }
            }
            if (savedPlans.isEmpty()) {
                item(key = "savedEmpty") {
                    Surface(color = Sage, shape = RoundedCornerShape(21.dp)) {
                        Text("No saved plans yet. Generate an itinerary and save it to use offline.",
                            Modifier.fillMaxWidth().padding(18.dp), color = Forest)
                    }
                }
            } else {
                items(savedPlans, key = { "saved-${it.plan.id}" }) { saved ->
                    SavedSmartPlanCard(
                        saved = saved,
                        onOpen = {
                            model.openSaved(saved)
                            scope.launch { listState.animateScrollToItem(3) }
                        },
                        onDelete = { pendingDeleteId = saved.plan.id }
                    )
                }
            }
        }
        SnackbarHost(snack, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (cityPickerOpen) {
        StartingCityPicker(
            selectedCity = city,
            onSelect = { selected ->
                city = selected
                startingLatitude = null
                startingLongitude = null
                cityPickerOpen = false
            },
            onUseGps = { openGps() },
            onSearchGoogle = { openGoogleSearch() },
            gpsLoading = gpsLoading,
            placesLoading = placesLoading,
            placesAvailable = placesAvailable,
            onDismiss = { cityPickerOpen = false }
        )
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete saved itinerary?") },
            text = { Text("This offline plan and its daily stops will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { model.deletePlan(id); pendingDeleteId = null }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

/** Offline town search with optional on-demand GPS and Google Places search. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartingCityPicker(
    selectedCity: String,
    onSelect: (String) -> Unit,
    onUseGps: () -> Unit,
    onSearchGoogle: () -> Unit,
    gpsLoading: Boolean,
    placesLoading: Boolean,
    placesAvailable: Boolean,
    onDismiss: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    val matchingCities = remember(search) {
        SmartPlannerEngine.cities.filter {
            it.name.contains(search.trim(), ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Soft
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Choose your starting city",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Forest
            )
            Text(
                "Use GPS, search places online, or choose an offline town",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onUseGps,
                enabled = !gpsLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (gpsLoading) "Locating..." else "Use my current location")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSearchGoogle,
                enabled = placesAvailable && !placesLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.TravelExplore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (placesLoading) "Loading place..." else "Search any place in Sri Lanka")
            }
            if (!placesAvailable) {
                Text("Online search requires the existing Maps API key and Places API (New).",
                    color = InkMuted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Text("OFFLINE LOCATIONS", color = InkMuted,
                style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search city or town") },
                placeholder = { Text("e.g. Jaffna, Matara, Nuwara Eliya") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${matchingCities.size} locations",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted
            )
            Spacer(Modifier.height(6.dp))
            if (matchingCities.isEmpty()) {
                Text(
                    "No matching town. Try another name.",
                    modifier = Modifier.padding(vertical = 22.dp),
                    color = InkMuted
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp),
                    contentPadding = PaddingValues(bottom = 22.dp)
                ) {
                    items(matchingCities, key = { it.name }) { option ->
                        ListItem(
                            headlineContent = { Text(option.name) },
                            leadingContent = {
                                Icon(Icons.Outlined.Place, contentDescription = null)
                            },
                            trailingContent = {
                                if (option.name == selectedCity) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = Forest
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onSelect(option.name) },
                            colors = ListItemDefaults.colors(containerColor = Soft)
                        )
                        HorizontalDivider(color = Sage)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedSmartPlanCard(
    saved: SmartPlanWithStops,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Sage, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Map, null,
                    Modifier.padding(13.dp), tint = Forest)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(saved.plan.title, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${saved.plan.numberOfDays} days  ·  ${saved.stops.size} places",
                    style = MaterialTheme.typography.bodySmall, color = InkMuted)
                Text("${saved.plan.startDate}  ·  ${lkr(saved.plan.budgetLkr)}",
                    style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
            Column {
                TextButton(onClick = onOpen) { Text("Open") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, "Delete plan",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun lkr(amount: Long): String = "LKR %,d".format(Locale.ENGLISH, amount)

/** External Google Maps directions for one day; itinerary data itself remains offline. */
private fun openDayDirections(
    context: Context,
    draft: PlannerDraft,
    day: Int,
    places: List<PlannerPlace>
) {
    val daily = draft.stops.filter { it.dayIndex == day }
        .sortedBy { it.position }
        .mapNotNull { stop -> places.firstOrNull { it.id == stop.destinationId } }
    if (daily.isEmpty()) return
    val prior = draft.stops.filter { it.dayIndex < day }
        .sortedWith(compareBy({ it.dayIndex }, { it.position }))
        .lastOrNull()?.let { stop -> places.firstOrNull { it.id == stop.destinationId } }
    val city = SmartPlannerEngine.cities.firstOrNull { it.name.equals(draft.startingCity, ignoreCase = true) }
    val origin = if (prior != null) "${prior.latitude},${prior.longitude}"
        else if (draft.startingLatitude != null && draft.startingLongitude != null)
            "${draft.startingLatitude},${draft.startingLongitude}"
        else if (city != null) "${city.latitude},${city.longitude}"
        else {
            Toast.makeText(context, "Starting coordinates unavailable.", Toast.LENGTH_LONG).show()
            return
        }
    val last = daily.last()
    val waypoint = if (daily.size > 1) {
        "&waypoints=${Uri.encode(daily.dropLast(1).joinToString("|") { "${it.latitude},${it.longitude}" })}"
    } else ""
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1" +
            "&origin=${Uri.encode(origin)}" +
            "&destination=${Uri.encode("${last.latitude},${last.longitude}")}" +
            waypoint + "&travelmode=driving"
    )
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure {
            Toast.makeText(context, "No map app or browser can open directions.", Toast.LENGTH_LONG).show()
        }
}
