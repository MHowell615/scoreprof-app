package cloud.scoreprof.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cloud.scoreprof.app.*
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.view_models.ListHelpViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel


import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun HelpScreen(
    navController: NavHostController,
    helpViewModel: ListHelpViewModel,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val setupState by setupViewModel.setup.collectAsState()

    val faqList = remember {
        (1..13).map { i ->
            when (i) {
                1 -> Res.string.help_question1 to Res.string.help_answer1
                2 -> Res.string.help_question2 to Res.string.help_answer2
                3 -> Res.string.help_question3 to Res.string.help_answer3
                4 -> Res.string.help_question4 to Res.string.help_answer4
                5 -> Res.string.help_question5 to Res.string.help_answer5
                6 -> Res.string.help_question6 to Res.string.help_answer6
                7 -> Res.string.help_question7 to Res.string.help_answer7
                8 -> Res.string.help_question8 to Res.string.help_answer8
                9 -> Res.string.help_question9 to Res.string.help_answer9
                10 -> Res.string.help_question10 to Res.string.help_answer10
                11 -> Res.string.help_question11 to Res.string.help_answer11
                12 -> Res.string.help_question12 to Res.string.help_answer12
                13 -> Res.string.help_question13 to Res.string.help_answer13
                else -> Res.string.help to Res.string.help // Fallback
            }
        }
    }

    var expandedIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Modern edge-to-edge handling
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = stringResource(Res.string.help),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(faqList) { index, item ->
                val isOpen = expandedIndex == index
                val question = stringResource(item.first)
                val answer = stringResource(item.second)

                Surface(
                    onClick = { expandedIndex = if (isOpen) -1 else index },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOpen) dropdown_background else button_background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = question,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = isOpen) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = answer,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Ad Banner at the bottom of the help list
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    AdBanner(
                        modifier = Modifier.fillMaxWidth(),
                        isMediumRectangle = true,
                        showAds = setupState?.is_ads_removed == false
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
