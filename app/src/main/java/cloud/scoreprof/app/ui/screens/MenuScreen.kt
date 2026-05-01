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
import cloud.scoreprof.app.OptionConfig

@Composable
fun MenuScreen(title: String, options: List<OptionConfig>, navController: NavController) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        options.forEach { option ->
            Button(
                onClick = {
                    // --- THE KEY CHANGE IS HERE ---
                    // Check the screenType defined in your JSON for this option's destination.
                    // This requires you to find the corresponding PageConfig.
                    // For now, let's assume the 'route' directly tells us the destination type.

                    // A more robust solution involves looking up the route in your navigationConfig,
                    // but for now, let's make a simple adjustment.

                    // If the route itself indicates the final destination, just navigate.
                    // The 'route' in your OptionConfig should be the *final* destination.
                    navController.navigate(option.route)
                }
            ) {
                Text(option.name ?: "")
            }
        }
        TextButton(onClick = { navController.popBackStack() }) { Text("Back") }
    }
}


