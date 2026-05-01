package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import cloud.scoreprof.app.ui.view_models.NotificationViewModel

@Composable
fun NotificationScreen(
    navController: NavController,
    setupViewModel: ListSetupViewModel,
    notificationViewModel: NotificationViewModel,
    notificationId: Int
) {
    // You can fetch the specific notification details from the ViewModel
    // For now, let's show the invitation explanation:
    val setupState by setupViewModel.setup.collectAsState()    // This local variable will hold the actual object or null
    val userid = setupState?.userid

    LaunchedEffect(notificationId) {
        notificationViewModel.loadNotificationById(notificationId)
    }
    val notification by notificationViewModel.currentNotification.collectAsState()

    // 2. Auto-mark as read once the notification is loaded
    LaunchedEffect(notification) {
        notification?.let {
            if (!it.isread) {
                notificationViewModel.markAsRead(it.email, it.notificationid)
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Standard height for a top app bar
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f) // Ensures title takes up available space
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val currentNotification = notification
            if (currentNotification != null) {
                item {
                    Text("You've been invited!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "You have a pending invitation from ${currentNotification.ownername} to join the league: ${currentNotification.leagueid}. " +
                            "To participate, you need to confirm your acceptance in the Setup Leagues section by checking the checkbox next to that league.",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
                item {
                    Button(onClick = {
                        // Mark as read on the server (we'll add this to the VM)

                        // Go to the leagues setup
                        navController.navigate("setup_leagues_screen/$userid")
                    }) {
                        Text("Go to Setup Leagues")
                    }
                }
            } else {
                item {
                    CircularProgressIndicator() // Wait for DB fetch
                }
            }
        }
    }
}