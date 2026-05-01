package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cloud.scoreprof.app.data.ScoreProfDatabase

@Composable
fun MenuItemScreen(
    id: String,
    type: String,
    screenType: String,
    competition: String,
    db: ScoreProfDatabase,
    navController: NavController
) {
    // This screen is now only for displaying generic menu items.
    // The "matches" routing logic has been removed.
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("Menu Item Details", style = MaterialTheme.typography.labelLarge)
        Text("ID: $id", style = MaterialTheme.typography.displayMedium)
        Text("Competition: $competition", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { navController.popBackStack() }) { Text("Back") }
    }
}
