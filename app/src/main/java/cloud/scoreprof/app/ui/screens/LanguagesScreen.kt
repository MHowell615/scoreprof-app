package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.utils.SelectableRowWithCheckboxes
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel

@Composable
fun LanguagesScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        setupViewModel.loadLanguages()
    }

    val languages by setupViewModel.languages.collectAsState()

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
                    //setupViewModel.saveChanges()
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = stringResource(id = R.string.preferred_language),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f) // Ensures title takes up available space
                )
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.language_screen_text),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(languages.sortedBy { it.item.languageName }) { language -> // Changed 'languages' to 'availableLanguages'
                // The 'SelectableRowWithCheckboxes' seems to be using the wrong ViewModel logic
                // based on the composable name. I'll correct this too.
                SelectableRowWithCheckboxes(
                    item = language, // Pass the whole language object
                    name = language.item.languageName, // Get the name directly from the language object
                    isSelected = language.isSelected,
                    onCheckedChange = {
                        setupViewModel.onLanguageChanged(language.item)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


