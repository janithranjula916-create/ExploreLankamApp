package com.example.explorelanka.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.explorelanka.data.DestinationData
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.remote.DestinationCloudListener
import com.example.explorelanka.data.remote.TripCloudSync
import com.example.explorelanka.ui.screens.*
import com.example.explorelanka.viewmodel.AuthViewModel
import com.example.explorelanka.ui.theme.LankaPalette
import com.example.explorelanka.ui.theme.ExploreLankaColors
import com.example.explorelanka.ui.theme.ExploreLankaTypography

private object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val TRIPS = "trips"
    const val PROFILE = "profile"
    const val DETAILS = "destination/{id}"
    const val MAP = "map"
    const val MAP_FOCUS = "map/{id}"
    const val JOURNAL = "journal"
    const val CAMERA = "journal/camera?placeId={placeId}"
    const val SCANNER = "qr-scanner"
    const val SMART_PLAN = "smart-planner"
    fun camera(placeId: String = "") = "journal/camera?placeId=${Uri.encode(placeId)}"
    fun mapFor(id: String) = "map/$id"
    fun destination(id: String): String = "destination/$id"
}

private data class BottomTab(
    val route: String, val title: String,
    val icon: ImageVector, val selectedIcon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    BottomTab(Routes.EXPLORE, "Explore", Icons.Outlined.Explore, Icons.Filled.Explore),
    BottomTab(Routes.SMART_PLAN, "Plan", Icons.Outlined.Event, Icons.Filled.Event),
    BottomTab(Routes.TRIPS, "My Trips", Icons.Outlined.Event, Icons.Filled.Event),
    BottomTab(Routes.PROFILE, "Profile", Icons.Outlined.Person, Icons.Filled.Person)
)

@Composable
fun ExploreLankaApp() {
    val auth: AuthViewModel = viewModel()
    val userId = auth.user?.uid

    MaterialTheme(
        colorScheme = ExploreLankaColors,
        typography = ExploreLankaTypography
    ) {
        if (userId == null) {
            AuthScreen(auth)
        } else {
            // A keyed subtree and account-scoped DAO queries prevent old account
            // content being shown after an account switch on the same device.
            androidx.compose.runtime.key(userId) {
                SignedInExploreLanka(userId, auth)
            }
        }
    }
}

@Composable
private fun SignedInExploreLanka(userId: String, auth: AuthViewModel) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val database = remember(context) { ExploreLankaDatabase.getInstance(context) }
    DisposableEffect(userId) {
        val stopSync = TripCloudSync(database).start(coroutineScope, userId)
        val stopPlaces = DestinationCloudListener.start()
        onDispose {
            stopSync()
            stopPlaces.remove()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: Routes.HOME
    val isTabRoute = bottomTabs.any { it.route == route }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTabRoute) {
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 17.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = LankaPalette.Forest,
                    shadowElevation = 5.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = tab.route == route
                            Column(
                                modifier = Modifier.weight(1f)
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .padding(vertical = 9.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (selected) LankaPalette.Accent
                                           else LankaPalette.White.copy(alpha = 0.70f),
                                    modifier = Modifier.size(23.dp)
                                )
                                Text(
                                    text = if (tab.route == Routes.TRIPS) "Trips" else tab.title,
                                    color = if (selected) LankaPalette.Accent
                                            else LankaPalette.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onExploreClick = { navController.navigate(Routes.EXPLORE) },
                    onDestinationClick = { navController.navigate(Routes.destination(it)) },
                    onMapClick = { navController.navigate(Routes.MAP) },
                    onJournalClick = { navController.navigate(Routes.JOURNAL) },
                    onScanClick = { navController.navigate(Routes.SCANNER) },
                    onPlannerClick = { navController.navigate(Routes.SMART_PLAN) }
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onDestinationClick = { navController.navigate(Routes.destination(it)) },
                    onMapClick = { navController.navigate(Routes.MAP) },
                    onJournalClick = { navController.navigate(Routes.JOURNAL) },
                    onScanClick = { navController.navigate(Routes.SCANNER) }
                )
            }
            composable(Routes.SMART_PLAN) {
                SmartTripPlannerScreen(
                    userId = userId,
                    onBackClick = { navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    } },
                    onMapDestinationClick = { id -> navController.navigate(Routes.mapFor(id)) }
                )
            }
            composable(Routes.TRIPS) { TripsScreen(userId = userId) }
            composable(Routes.PROFILE) { ProfileScreen(auth, userId) }
            composable(Routes.DETAILS) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                DestinationDetailScreen(
                    destination = DestinationData.findById(id),
                    onBackClick = { navController.popBackStack() },
                    onMapClick = { placeId -> navController.navigate(Routes.mapFor(placeId)) },
                    onTakePhotoClick = { placeId -> navController.navigate(Routes.camera(placeId)) }
                )
            }
            composable(Routes.JOURNAL) {
                JournalScreen(
                    userId = userId,
                    onBackClick = { navController.popBackStack() },
                    onCaptureClick = { navController.navigate(Routes.camera()) }
                )
            }
            composable(
                route = Routes.CAMERA,
                arguments = listOf(navArgument("placeId") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { entry ->
                PhotoCaptureScreen(
                    userId = userId,
                    initialDestinationId = entry.arguments?.getString("placeId").orEmpty(),
                    onBackClick = { navController.popBackStack() },
                    onSaved = {
                        navController.navigate(Routes.JOURNAL) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.SCANNER) {
                QrScannerScreen(
                    userId = userId,
                    onBackClick = { navController.popBackStack() },
                    onDestinationClick = { id ->
                        navController.navigate(Routes.destination(id)) {
                            popUpTo(Routes.SCANNER) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.MAP) {
                ExploreMapScreen(
                    onBackClick = { navController.popBackStack() },
                    onDestinationClick = { navController.navigate(Routes.destination(it)) }
                )
            }
            composable(Routes.MAP_FOCUS) { entry ->
                ExploreMapScreen(
                    focusPlaceId = entry.arguments?.getString("id"),
                    onBackClick = { navController.popBackStack() },
                    onDestinationClick = { navController.navigate(Routes.destination(it)) }
                )
            }
        }
    }
}
