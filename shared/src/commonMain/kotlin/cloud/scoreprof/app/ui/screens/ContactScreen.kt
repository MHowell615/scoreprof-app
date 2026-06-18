package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.Res
import cloud.scoreprof.app.idea
import cloud.scoreprof.app.bug
import cloud.scoreprof.app.question
import cloud.scoreprof.app.other
import cloud.scoreprof.app.feedback_button
import cloud.scoreprof.app.feedback_toast
import cloud.scoreprof.app.category
import cloud.scoreprof.app.subject
import cloud.scoreprof.app.details
import cloud.scoreprof.app.contact
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val setupState by setupViewModel.setup.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var categoryRes by remember { mutableStateOf(Res.string.idea) }
    var subject by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val categories = listOf(
        Res.string.idea,
        Res.string.bug,
        Res.string.question,
        Res.string.other
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
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
                        text = stringResource(Res.string.contact),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = stringResource(categoryRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { selectionRes ->
                        DropdownMenuItem(
                            text = { Text(stringResource(selectionRes)) },
                            onClick = { categoryRes = selectionRes; expanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text(stringResource(Res.string.subject)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = details,
                onValueChange = {
                    if (it.length <= 250) details = it
                },
                label = { Text(stringResource(Res.string.details)) },
                supportingText = {
                    Text(
                        text = "${details.length} / 250",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                minLines = 5,
                isError = details.length >= 250
            )

            Button(
                onClick = {
                    scope.launch {
                        val categoryString = getString(categoryRes)
                        setupViewModel.sendFeedback(categoryString, subject, details)
                        snackbarHostState.showSnackbar(getString(Res.string.feedback_toast))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = subject.isNotBlank() && details.isNotBlank()
            ) {
                Text(stringResource(Res.string.feedback_button))
            }

            if (setupState?.is_ads_removed == false) {
                Spacer(modifier = Modifier.height(16.dp))
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
