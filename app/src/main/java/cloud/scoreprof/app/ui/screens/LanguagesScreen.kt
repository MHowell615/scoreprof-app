package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.utils.SelectableRowWithCheckboxes
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel

@Composable
fun LanguagesScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val setupState by setupViewModel.setup.collectAsState()
    val languages by setupViewModel.languages.collectAsState()

    LaunchedEffect(Unit) {
        setupViewModel.loadLanguages()
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
                IconButton(onClick = {
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
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.language_screen_text),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )
            }

            items(languages.sortedBy { it.item.languageName }) { language ->
                SelectableRowWithCheckboxes(
                    item = language,
                    name = language.item.languageName,
                    isSelected = language.isSelected,
                    onCheckedChange = {
                        setupViewModel.onLanguageChanged(language.item)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Ad Banner for free users
            if (setupState?.is_ads_removed == false) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        AdBanner(
                            modifier = Modifier.fillMaxWidth(),
                            isMediumRectangle = true,
                            showAds = true
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
