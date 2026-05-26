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

@OptIn(ExperimentalFoundationApi::class)
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
    val userid = remember(setupState?.userid, passedUserId) {
        setupState?.userid ?: passedUserId?.let { UUID.fromString(it) }
    }
    val email = remember(setupState?.email, passedEmail) {
        setupState?.email ?: passedEmail ?: ""
    }

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
            notificationViewModel.loadNotifications(email)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (setupState?.is_ads_removed == false) {
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
            }

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // Condensed Welcome Message using string resources
            val resIds = listOf(
                R.string.home_text_1,
                R.string.home_text_2,
                R.string.home_text_3,
                R.string.home_text_4,
                R.string.home_text_5
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                // THIS controls the gap BETWEEN sentences (e.g., 12.dp)
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                resIds.forEach { resId ->
                    Text(
                        text = stringResource(id = resId),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 14.sp,
                            // LineHeight controls spacing WITHIN a wrapped sentence
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.privacy_policy_title),
                    style = footerStyle.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable {
                        val url = if (preferredLanguage == "fr") "https://www.muntjac-solutions.fr/privacy" else "https://www.muntjac-solutions.com/privacy"
                        val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                        intent.launchUrl(context, Uri.parse(url))
                    }
                )
                Text(text = " | ", style = footerStyle, modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = stringResource(R.string.legal_notice_title),
                    style = footerStyle.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable {
                        val url = if (preferredLanguage == "fr") "https://www.muntjac-solutions.fr/legal" else "https://www.muntjac-solutions.com/legal"
                        val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                        intent.launchUrl(context, Uri.parse(url))
                    }
                )
            }
            Text(
                text = stringResource(R.string.copyright),
                style = footerStyle,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(logoBackgroundColor)
            .statusBarsPadding() // Correct way to handle status bar in Android 15
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.scoreprof_launcher_round),
                contentDescription = "ScoreProf Logo",
                modifier = Modifier.height(48.dp),
                contentScale = ContentScale.Fit
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        DropdownMenuItem(text = { Text("No new notifications") }, onClick = { showMenu = false })
                    } else {
                        notifications.forEach { notification ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        notification.title?.let { Text(it, fontWeight = FontWeight.Bold) }
                                        notification.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("notifications_screen/${notification.notificationid}")
                                }
                            )
                        }
                    }
                }
            }

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

private val footerStyle = TextStyle(
    fontSize = 12.sp,
    color = Color.Gray,
    textAlign = TextAlign.Center
)
