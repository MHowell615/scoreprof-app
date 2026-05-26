package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.sharp.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner

@Composable
fun SetupScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val setupState by setupViewModel.setup.collectAsState()
    val currentSetup = setupState
    val userid = setupState?.userid
    var newPassword by remember { mutableStateOf("") }
    val uiState by setupViewModel.uiState.collectAsState()
    val isLoading by setupViewModel.isLoading.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val isPasswordTooShort = newPassword.isNotEmpty() && newPassword.length < 6

    DisposableEffect(Unit) {
        onDispose {
            if (!setupViewModel.isLoading.value) {
                setupViewModel.saveSetupScreenChanges()
            }
        }
    }

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
                IconButton(
                    enabled = !isLoading,
                    onClick = {
                        setupViewModel.saveSetupScreenChanges()
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.setup),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                )
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 4.dp)
        ) {
            item {
                OutlinedTextField(
                    value = currentSetup?.email ?: "",
                    label = { Text(stringResource(id = R.string.email)) },
                    onValueChange = { newEmail ->
                        setupViewModel.onSetupDetailChanged(
                            email = newEmail,
                            name = currentSetup?.name ?: ""
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPassword,
                    label = { Text(stringResource(id = R.string.password)) },
                    isError = isPasswordTooShort,
                    supportingText = {
                        if (isPasswordTooShort) {
                            Text(
                                "Password must be at least 6 characters",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    onValueChange = { newPassword = it },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (newPassword.length >= 6) {
                                IconButton(
                                    onClick = {
                                        setupViewModel.changePassword(newPassword)
                                        newPassword = ""
                                    }) {
                                    Icon(
                                        imageVector = Icons.Sharp.SaveAlt,
                                        contentDescription = "Save Password",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide" else "Show")
                            }
                        }
                    },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                when (val state = uiState) {
                    is ListSetupViewModel.HomeUiState.Error -> {
                        Text(
                            text = state.message,
                            color = if (state.message.contains("success", ignoreCase = true))
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentSetup?.name ?: "",
                    label = { Text(stringResource(id = R.string.username)) },
                    onValueChange = { newName ->
                        setupViewModel.onSetupDetailChanged(
                            email = currentSetup?.email ?: "",
                            name = newName
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = stringResource(id = R.string.setup_text_1),
                    style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                )
            }
            item {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = stringResource(id = R.string.setup_text_2),
                    style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = button_background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { navController.navigate("setup_competitions_screen/$userid") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(id = R.string.competitions),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward"
                    )
                }
            }

            item {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = button_background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        navController.navigate("setup_leagues_screen/$userid")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(id = R.string.leagues),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward"
                    )
                }
            }
            item {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = button_background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        navController.navigate("setup_privacy_screen/$userid")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(id = R.string.privacy),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward"
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box (
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    AdBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        isMediumRectangle = true,
                        showAds = setupState?.is_ads_removed == false
                    )
                }
            }
        }
    }
}
