package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cloud.scoreprof.app.ui.model.OptionConfig

@Composable
fun MenuScreen(title: String, options: List<OptionConfig>, navController: NavController) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        options.forEach { option ->
            Button(
                onClick = {
                    navController.navigate(option.route)
                }
            ) {
                Text(option.name ?: "")
            }
        }
        TextButton(onClick = { navController.popBackStack() }) { Text("Back") }
    }
}
