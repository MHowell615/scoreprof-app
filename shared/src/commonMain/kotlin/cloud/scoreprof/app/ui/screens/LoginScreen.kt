package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import scoreprof_resources.Res
import scoreprof_resources.scoreprof_login
import scoreprof_resources.login
import scoreprof_resources.sign_up
import scoreprof_resources.confirm_password
import scoreprof_resources.password_mismatch
import scoreprof_resources.signup_text
import scoreprof_resources.forgot_password
import scoreprof_resources.email
import scoreprof_resources.password
import scoreprof_resources.scoreprof_launcher_round
import scoreprof_resources.continue_as_guest
import scoreprof_resources.age_certification
import scoreprof_resources.age_limited
import cloud.scoreprof.app.ui.view_models.LoginViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit,
    navController: NavController,
    viewModel: LoginViewModel = koinViewModel()
) {
    val email by viewModel.email
    val password by viewModel.password
    var passwordInput by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading
    var passwordVisible by remember { mutableStateOf(false) }
    val isPasswordTooShort = password.isNotEmpty() && password.length < 6
    
    // Simple regex for email validation in KMP
    val isEmailValid = remember(email) {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        email.trim().matches(emailRegex)
    }
    
    var hasAttemptedLogin by remember { mutableStateOf(false) }
    val isLoginMode by viewModel.isLoginMode.collectAsState()
    var confirmPasswordInput by remember { mutableStateOf("") }
    val passwordsMatch = passwordInput == confirmPasswordInput || isLoginMode
    var isAdultChecked by remember { mutableStateOf(false) }

    var showFields by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is LoginViewModel.UiEvent.LoginSuccess -> {
                    onLoginSuccess(event.userId, event.email)
                }
                is LoginViewModel.UiEvent.RequestResetSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is LoginViewModel.UiEvent.ResetPasswordSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is LoginViewModel.UiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Spacer(modifier = Modifier.statusBarsPadding())
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Using placeholder if logo not yet available in Res
                 Image(
                    painter = painterResource(Res.drawable.scoreprof_launcher_round),
                    contentDescription = "ScoreProf Logo",
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth(),
                     contentScale = ContentScale.Fit
                 )
                //Text("ScoreProf Logo Placeholder", style = MaterialTheme.typography.headlineLarge)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!showFields) {
                Text(
                    text = stringResource(Res.string.scoreprof_login),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        viewModel.setLoginMode(true)
                        showFields = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.login))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.setLoginMode(false)
                        showFields = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(Res.string.sign_up))
                }

                Spacer(modifier = Modifier.height(32.dp))

                TextButton(
                    onClick = {
                        onLoginSuccess("guest", "guest@scoreprof.cloud")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.continue_as_guest))
                }
            }
            else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showFields = false }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = if (isLoginMode) stringResource(Res.string.login) else stringResource(Res.string.sign_up),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text(stringResource(Res.string.email)) },
                    isError = hasAttemptedLogin && !isEmailValid,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = passwordInput,
                    label = { Text(stringResource(Res.string.password)) },
                    isError = isPasswordTooShort,
                    onValueChange = { passwordInput = it },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isLoginMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text(stringResource(Res.string.confirm_password)) },
                        isError = hasAttemptedLogin && (!passwordsMatch || !isAdultChecked),
                        supportingText = {
                            if (hasAttemptedLogin && !passwordsMatch) {
                                Text(stringResource(Res.string.password_mismatch), color = MaterialTheme.colorScheme.error)
                            } else if (hasAttemptedLogin && !isAdultChecked) {
                                Text(stringResource(Res.string.age_limited), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAdultChecked,
                            onCheckedChange = { isAdultChecked = it }
                        )
                        Text(
                            text = stringResource(Res.string.age_certification),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasAttemptedLogin && !isAdultChecked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            hasAttemptedLogin = true
                            if (isEmailValid && !isPasswordTooShort && email.isNotEmpty() && passwordsMatch) {
                                viewModel.onPasswordChange(passwordInput.trim())
                                viewModel.login()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoginMode) stringResource(Res.string.login) else stringResource(Res.string.sign_up))
                    }

                    if (isLoginMode) {
                        TextButton(onClick = {
                            navController.navigate("forgot_password")
                        }) {
                            Text(
                                text = stringResource(Res.string.forgot_password),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!isLoginMode) {
                    Text(
                        text = stringResource(Res.string.signup_text),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1.5f))
        }
    }
}