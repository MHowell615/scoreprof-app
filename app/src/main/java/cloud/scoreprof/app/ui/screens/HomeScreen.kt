package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.NotificationType
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import cloud.scoreprof.app.ui.view_models.NotificationViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.browser.customtabs.CustomTabsIntent
import android.net.Uri
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle

@OptIn(ExperimentalFoundationApi::class) // Required for stickyHeader
@Composable
fun HomeScreen(
    setupViewModel: ListSetupViewModel,
    navController: NavController,
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    passedUserId: String? = null,
    passedEmail: String? = null,
    tokenManager: TokenManager
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val preferredLanguage = configuration.locales[0].language
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val setupState by setupViewModel.setup.collectAsState()
    //val userid = setupState?.userid
    val userid = remember(setupState?.userid, passedUserId) {
        setupState?.userid ?: passedUserId?.let { UUID.fromString(it) }
    }
    val email = remember(setupState?.email, passedEmail) {
        setupState?.email ?: passedEmail ?: ""
    }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userid, setupState) {
        if (userid != null && (setupState == null || setupState?.memberSince == null)) {
            val token = tokenManager.getToken() ?: ""
            if (token.isNotEmpty() && email.isNotEmpty()) {
                setupViewModel.activateUserAccount(email, preferredLanguage)
            }
        }
    }

    LaunchedEffect(email, setupState) {
        if (email.isNotEmpty()) {
            println("TEST: (HomeScreen) Loading Notifications for $email")
            notificationViewModel.loadNotifications(email)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Trigger a silent refresh from the server
                if (userid != null) {
                    setupViewModel.refreshData()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(userid) {
        if (userid != null) {
            setupViewModel.loadInitialDataForUser(userid)
        }
    }

    val competitionsState by setupViewModel.competitions.collectAsState()

    val groupedData = remember(competitionsState) {
        competitionsState
            .filter { it.isSelected } // Only those selected by user
            .groupBy { it.item.sport_type ?: "Other" } // Use the sport_type from your model
            .toSortedMap() // Alphabetical Sport Types (A-Z)
            .mapValues { entry ->
                entry.value.sortedBy { it.item.name } // Alphabetical Competitions (A-Z)
            }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                navController = navController,
                userid = userid,
                onLogoutClick = {
                    scope.launch {
                        setupViewModel.logout {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Your home_text items... (keeping existing logic)
                val textIds = listOf(
                    R.string.home_text_1,
                    R.string.home_text_2,
                    R.string.home_text_3,
                    R.string.home_text_4,
                    R.string.home_text_5
                )
                textIds.forEach { id ->
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(id = id),
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // --- NAVIGATION BUTTONS ---
            item {
                // Reusable navigation button logic for bottom buttons
                val navItems = listOf(
                    R.string.matches to "competitions_screen/$userid",
                    R.string.leagues to "leagues_screen/$userid",
                    R.string.setup to "setup_screen/$userid",
                    R.string.help to "help_screen",
                    R.string.contact to "contact_screen"
                )

                navItems.forEach { (labelId, route) ->
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = button_background,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        onClick = { navController.navigate(route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(id = labelId),
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(vertical = 8.dp)
                ) {
                    AdBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        isMediumRectangle = false
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Make the Privacy Policy clickable
                    Text(
                        text = stringResource(R.string.privacy_policy_title),
                        style = footerStyle.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.clickable {
                            val lang = preferredLanguage
                            val url = if (lang == "fr") "https://www.muntjac-solutions.fr/privacy" else "https://www.muntjac-solutions.com/privacy"
                            val intent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            intent.launchUrl(context, Uri.parse(url))
                        }
                    )
                    Text(
                        text = " | ",
                        style = footerStyle,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    // Make the Legal Notice clickable
                    Text(
                        text = stringResource(R.string.legal_notice_title),
                        style = footerStyle.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.clickable {
                            val lang = preferredLanguage
                            val url = if (lang == "fr") "https://www.muntjac-solutions.fr/legal" else "https://www.muntjac-solutions.com/legal"
                            val intent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            intent.launchUrl(context, Uri.parse(url))
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.copyright),
                        style = footerStyle
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    navController: NavController,
    userid: UUID?,
    onLogoutClick: () -> Unit
) {
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val logoBackgroundColor = Color(0xFF1A0841)

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Color.White)
        )

        // Updated Row to balance the UI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(logoBackgroundColor)
                .padding(horizontal = 12.dp), // Padding for the edges
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // This pushes Logo Left and Icons Right
        ) {
            // 1. LOGO (Now on the Left)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.scoreprof_launcher_round),
                    contentDescription = "ScoreProf Logo",
                    modifier = Modifier.height(48.dp),
                    contentScale = ContentScale.Fit
                )
                //Spacer(modifier = Modifier.width(8.dp))
                /*Text(
                    text = "ScoreProf",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp,
                        color = Color(0xFFFFAD5A)
                    )
                )*/
            }

            // 2. ICONS GROUP (On the Right)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Notifications
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = { showMenu = true }) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(containerColor = Color.Red) {
                                        Text(unreadCount.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.width(280.dp)
                    ) {
                        val notifications by notificationViewModel.notifications.collectAsState()

                        if (notifications.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No new notifications") },
                                onClick = { showMenu = false }
                            )
                        } else {
                            notifications.forEach { notification ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            notification.title?.let {
                                                Text(it,
                                                    fontWeight = FontWeight.Bold)
                                            }
                                            notification.message?.let {
                                                Text(
                                                    it,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (notification.type == NotificationType.LEAGUE_INVITE) {
                                            navController.navigate(
                                                "notifications_screen/${notification.notificationid}"
                                            )
                                        } else if (notification.type == NotificationType.JOIN_REQUEST) {
                                            navController.navigate(
                                                "notifications_screen/${notification.notificationid}"
                                            )
                                        } else if (notification.type == NotificationType.GENERAL) {
                                            navController.navigate(
                                                "notifications_screen/${notification.notificationid}"
                                            )
                                        } else {
                                            navController.navigate("notifications_screen")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Logout Icon (Far Right)
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Add this at the bottom of HomeScreen.kt
private val footerStyle = TextStyle(
    fontSize = 12.sp,
    color = Color.Gray,
    textAlign = TextAlign.Center
)

