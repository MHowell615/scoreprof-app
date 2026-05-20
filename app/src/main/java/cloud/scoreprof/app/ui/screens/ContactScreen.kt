package cloud.scoreprof.app.ui.screens

import android.widget.Toast.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    var categoryResId by remember { mutableStateOf(R.string.idea) }
    var subject by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf(
        R.string.idea,
        R.string.bug,
        R.string.question,
        R.string.other
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    text = stringResource(id = R.string.contact),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f) // Ensures title takes up available space
                )
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
            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = stringResource(id = categoryResId),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { selectionResId ->
                        DropdownMenuItem(
                            text = { Text(stringResource(id = selectionResId)) },
                            onClick = { categoryResId = selectionResId; expanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text(stringResource(id = R.string.subject)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = details,
                onValueChange = {
                    if (it.length <= 250) details = it
                },
                label = { Text(stringResource(id = R.string.details)) },
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
                    val categoryString = navController.context.getString(categoryResId)

                    setupViewModel.sendFeedback(categoryString, subject, details)

                    android.widget.Toast.makeText(
                        navController.context,
                        navController.context.getString(R.string.feedback_toast),
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = subject.isNotBlank() && details.isNotBlank()
            ) {
                Text(stringResource(id = R.string.feedback_button))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center) {
                AdBanner(
                    modifier = Modifier.fillMaxWidth(),
                    isMediumRectangle = true
                )
            }
        }
    }
}
