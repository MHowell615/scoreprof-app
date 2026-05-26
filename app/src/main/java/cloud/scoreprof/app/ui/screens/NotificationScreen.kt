package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cloud.scoreprof.app.domain.model.NotificationType
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import cloud.scoreprof.app.ui.view_models.NotificationViewModel

@Composable
fun NotificationScreen(
    navController: NavController,
    setupViewModel: ListSetupViewModel,
    notificationViewModel: NotificationViewModel,
    notificationId: Int
) {
    val setupState by setupViewModel.setup.collectAsState()
    val userid = setupState?.userid

    LaunchedEffect(notificationId) {
        notificationViewModel.loadNotificationById(notificationId)
    }
    val notification by notificationViewModel.currentNotification.collectAsState()

    LaunchedEffect(notification) {
        notification?.let {
            if (!it.isread) {
                notificationViewModel.markAsRead(it.email, it.notificationid)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Modern Edge-to-Edge fix
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.close_btn)
                    )
                }
                Text(
                    text = stringResource(R.string.notifications),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val currentNotification = notification
            if (currentNotification != null) {
                item {
                    val title = when (currentNotification.type) {
                        NotificationType.JOIN_REQUEST -> stringResource(R.string.notif_join_request_title)
                        NotificationType.LEAGUE_INVITE -> stringResource(R.string.notif_invited_title)
                        else -> stringResource(R.string.notification)
                    }
                    Text(text = title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    val message = when (currentNotification.type) {
                        NotificationType.JOIN_REQUEST -> currentNotification.message ?: ""
                        NotificationType.LEAGUE_INVITE -> stringResource(
                            id = R.string.notif_invite_msg_body,
                            currentNotification.ownername ?: "Someone",
                            currentNotification.leagueid ?: ""
                        )
                        else -> currentNotification.message ?: ""
                    }

                    Text(text = message, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    when (currentNotification.type) {
                        NotificationType.JOIN_REQUEST -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(onClick = {
                                    notificationViewModel.acceptJoinRequest(currentNotification)
                                    navController.popBackStack()
                                }) {
                                    Text(stringResource(R.string.accept))
                                }
                                Button(
                                    onClick = {
                                        notificationViewModel.declineJoinRequest(currentNotification)
                                        navController.popBackStack()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(stringResource(R.string.ignore))
                                }
                            }
                        }
                        NotificationType.LEAGUE_INVITE -> {
                            Button(onClick = {
                                navController.navigate("setup_leagues_screen/$userid")
                            }) {
                                Text(stringResource(id = R.string.go_to_setup_leagues_btn))
                            }
                        }
                        else -> {
                            Button(onClick = { navController.popBackStack() }) {
                                Text(stringResource(id = R.string.close_btn))
                            }
                        }
                    }
                }
            } else {
                item {
                    CircularProgressIndicator()
                }
            }

            // Ad Banner for free users
            if (setupState?.is_ads_removed == false) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        AdBanner(
                            modifier = Modifier.fillMaxWidth(),
                            isMediumRectangle = true,
                            showAds = true
                        )
                    }
                }
            }
        }
    }
}
