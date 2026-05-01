package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import cloud.scoreprof.app.ui.view_models.LoginViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID
import cloud.scoreprof.app.BuildConfig
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner
import android.util.Patterns

@Composable
fun LoginScreen(
    onLoginSuccess: (UUID, String) -> Unit,
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val email by viewModel.email
    val password by viewModel.password
    var passwordInput by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading
    var passwordVisible by remember { mutableStateOf(false) }
    val isPasswordTooShort = password.isNotEmpty() && password.length < 6
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    var hasAttemptedLogin by remember { mutableStateOf(false) } // Optional: only show error after click

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is LoginViewModel.UiEvent.LoginSuccess -> {
                    println("ScoreProfDebug: Login Success! Navigating with ID: ${event.userId}")
                    onLoginSuccess(event.userId, event.email)
                }
                is LoginViewModel.UiEvent.Error -> {
                    // Show Snackbar or Toast
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    //Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.scoreprof_launcher_round),
                            contentDescription = "ScoreProf Logo",
                            modifier = Modifier
                                .height(100.dp) // Adjusted height for a cleaner look
                                .fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally//,
            //verticalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(R.string.scoreprof_login), style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { newValue ->
                    viewModel.onEmailChange(newValue)
                    if (isEmailValid) hasAttemptedLogin = false
                },
                label = { Text(stringResource(R.string.email)) },
                isError = hasAttemptedLogin && !isEmailValid,
                supportingText = {
                    if (hasAttemptedLogin && !isEmailValid) {
                        Text(
                            text = stringResource(R.string.email_validation_text),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = passwordInput,
                label = { Text(stringResource(R.string.password)) },
                isError = isPasswordTooShort,
                supportingText = {
                    if (isPasswordTooShort) {
                        Text(
                            "Password must be at least 6 characters",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                onValueChange = { passwordInput = it },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    // Toggle button to hide or display password
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, if (passwordVisible) "Hide" else "Show")
                    }
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.login_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        hasAttemptedLogin = true // Trigger validation display
                        if (isEmailValid && !isPasswordTooShort && email.isNotEmpty()) {
                            viewModel.onPasswordChange(passwordInput)
                            viewModel.login()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.login))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    // Navigate to a new ForgotPasswordScreen
                    navController.navigate("forgot_password")
                }
                //modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(id = R.string.forgot_password),
                    color = MaterialTheme.colorScheme.primary
                )
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


