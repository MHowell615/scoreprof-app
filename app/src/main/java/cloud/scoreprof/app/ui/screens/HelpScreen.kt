package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.domain.model.QuestionAnswer
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.view_models.ListHelpViewModel
import androidx.compose.ui.platform.LocalContext

@Composable
fun HelpScreen(
    navController: NavHostController,
    helpViewModel: ListHelpViewModel,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val faqList = remember {
        (1..12).map { i ->
            val questionResId = context.resources.getIdentifier(
                "help_question$i", "string", context.packageName
            )
            val answerResId = context.resources.getIdentifier(
                "help_answer$i", "string", context.packageName
            )

            QuestionAnswer(
                question = context.getString(questionResId),
                answer = context.getString(answerResId)
            )
        }
    }

    /*val faqList = listOf(
        QuestionAnswer(
            question = stringResource(R.string.help_question1),
            answer = stringResource(R.string.help_answer1)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question2),
            answer = stringResource(R.string.help_answer2)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question3),
            answer = stringResource(R.string.help_answer3)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question4),
            answer = stringResource(R.string.help_answer4)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question5),
            answer = stringResource(R.string.help_answer5)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question6),
            answer = stringResource(R.string.help_answer6)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question7),
            answer = stringResource(R.string.help_answer7)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question8),
            answer = stringResource(R.string.help_answer8)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question9),
            answer = stringResource(R.string.help_answer9)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question10),
            answer = stringResource(R.string.help_answer10)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question11),
            answer = stringResource(R.string.help_answer11)
        ),
        QuestionAnswer(
            question = stringResource(R.string.help_question12),
            answer = stringResource(R.string.help_answer12)
        )
    )*/

    var expandedIndex by remember { mutableIntStateOf(-1) }

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
                    text = stringResource(id = R.string.help),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f) // Ensures title takes up available space
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 8.dp),
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

                        // Use AnimatedVisibility for a smooth slide-down effect
                        androidx.compose.animation.AnimatedVisibility(visible = isOpen) {
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
        }
    }
}
