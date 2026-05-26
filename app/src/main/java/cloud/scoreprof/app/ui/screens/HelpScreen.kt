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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.domain.model.QuestionAnswer
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.view_models.ListHelpViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel

@Composable
fun HelpScreen(
    navController: NavHostController,
    helpViewModel: ListHelpViewModel,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val setupState by setupViewModel.setup.collectAsState()

    val faqList = (1..12).map { i ->
        val (questionRes, answerRes) = when (i) {
            1 -> R.string.help_question1 to R.string.help_answer1
            2 -> R.string.help_question2 to R.string.help_answer2
            3 -> R.string.help_question3 to R.string.help_answer3
            4 -> R.string.help_question4 to R.string.help_answer4
            5 -> R.string.help_question5 to R.string.help_answer5
            6 -> R.string.help_question6 to R.string.help_answer6
            7 -> R.string.help_question7 to R.string.help_answer7
            8 -> R.string.help_question8 to R.string.help_answer8
            9 -> R.string.help_question9 to R.string.help_answer9
            10 -> R.string.help_question10 to R.string.help_answer10
            11 -> R.string.help_question11 to R.string.help_answer11
            12 -> R.string.help_question12 to R.string.help_answer12
            else -> R.string.help to R.string.help // Fallback
        }

        QuestionAnswer(
            question = stringResource(id = questionRes),
            answer = stringResource(id = answerRes)
        )
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
                    text = stringResource(id = R.string.help),
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

                Surface(
                    onClick = { expandedIndex = if (isOpen) -1 else index },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOpen) dropdown_background else button_background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.question,
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
                                    text = item.answer,
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
