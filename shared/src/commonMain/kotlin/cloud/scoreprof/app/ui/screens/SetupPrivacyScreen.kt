package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavHostController
import scoreprof_resources.Res
import scoreprof_resources.privacy_settings
import scoreprof_resources.remove_ads_btn
import scoreprof_resources.remove_ads_desc
import scoreprof_resources.receive_emails
import scoreprof_resources.receive_notifications
import scoreprof_resources.receive_notifications_desc
import scoreprof_resources.allow_analytics
import scoreprof_resources.allow_analytics_desc
import scoreprof_resources.legal_docs_title
import scoreprof_resources.privacy_policy_title
import scoreprof_resources.legal_notice_title
import scoreprof_resources.request_deletion_title
import scoreprof_resources.deletion_desc
import scoreprof_resources.account_mgmt_title
import scoreprof_resources.google_privacy_policy_label
import scoreprof_resources.premium_title
import scoreprof_resources.restore_purchases_btn
import scoreprof_resources.terms_of_use_label
import scoreprof_resources.manage_subscription_btn
import scoreprof_resources.premium_active_msg
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun SetupPrivacyScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val setupState by setupViewModel.setup.collectAsState()
    val uiState by setupViewModel.uiState.collectAsState()
    val isLoading by setupViewModel.isLoading.collectAsState()
    val formattedPrice by setupViewModel.formattedPrice.collectAsState()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        setupViewModel.refreshBillingStatus()
        setupViewModel.messageEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ListSetupViewModel.HomeUiState.Error) {
            snackbarHostState.showSnackbar((uiState as ListSetupViewModel.HomeUiState.Error).message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
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
                    text = stringResource(Res.string.privacy_settings),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = modifier
                .padding(contentPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (setupState?.is_ads_removed == false) {
                item {
                    Text(
                        text = stringResource(Res.string.premium_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ElevatedButton(
                        onClick = {
                            setupViewModel.purchasePremium()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (formattedPrice != null) {
                                    stringResource(Res.string.remove_ads_btn)
                                        .replace("0.99 € / Month", "$formattedPrice / Month")
                                } else {
                                    stringResource(Res.string.remove_ads_btn)
                                }
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            setupViewModel.restorePurchases()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text(
                            text = stringResource(Res.string.restore_purchases_btn),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Text(
                        text = stringResource(Res.string.remove_ads_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(Res.string.premium_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            val url = if (setupViewModel.platform.name.contains("Android", ignoreCase = true)) {
                                "https://play.google.com/store/account/subscriptions?package=cloud.scoreprof.app"
                            } else {
                                "https://apps.apple.com/account/subscriptions"
                            }
                            uriHandler.openUri(url)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.manage_subscription_btn))
                    }
                    
                    Text(
                        text = stringResource(Res.string.premium_active_msg),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.receive_emails),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = setupState?.receive_email ?: true,
                            onCheckedChange = { 
                                setupViewModel.onPrivacySettingsChanged(
                                    receiveEmail = it,
                                    receiveNotifications = setupState?.receive_notifications ?: true
                                )
                            }
                        )
                    }
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.receive_notifications),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = setupState?.receive_notifications ?: true,
                            onCheckedChange = {
                                setupViewModel.onPrivacySettingsChanged(
                                    receiveEmail = setupState?.receive_email ?: true,
                                    receiveNotifications = it
                                )
                            }
                        )
                    }
                    Text(
                        text = stringResource(Res.string.receive_notifications_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.allow_analytics),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = true,
                            onCheckedChange = { /* To be implemented */ }
                        )
                    }
                    Text(
                        text = stringResource(Res.string.allow_analytics_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(Res.string.legal_docs_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { uriHandler.openUri("https://www.apple.com/legal/internet-services/itunes/dev/stdeula/") },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.terms_of_use_label), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                TextButton(
                    onClick = { uriHandler.openUri("https://www.muntjac-solutions.com/privacy") },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.privacy_policy_title), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                TextButton(
                    onClick = { uriHandler.openUri("https://business.safety.google/privacy/") },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.google_privacy_policy_label), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                TextButton(
                    onClick = { uriHandler.openUri("https://www.muntjac-solutions.com/legal") },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.legal_notice_title), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(Res.string.account_mgmt_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                var showDeleteDialog by remember { mutableStateOf(false) }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Account") },
                        text = { Text("Are you sure you want to permanently delete your account and all associated data? This action cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    setupViewModel.deleteAccount {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.request_deletion_title),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(Res.string.deletion_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
