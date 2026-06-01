package cloud.scoreprof.app.ui.screens

import android.R.attr.onClick
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
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner
import android.util.Patterns
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
    var hasAttemptedLogin by remember { mutableStateOf(false) }
    val isLoginMode by viewModel.isLoginMode.collectAsState()
    var confirmPasswordInput by remember { mutableStateOf("") }
    val passwordsMatch = passwordInput == confirmPasswordInput || isLoginMode



    val buttonText = if (isLoginMode)
        stringResource(R.string.login)
    else
        stringResource(R.string.sign_up)

    val toggleText = if (isLoginMode)
        "Don't have an account? Sign Up"
    else
        "Already have an account? Login"
    var showFields by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is LoginViewModel.UiEvent.LoginSuccess -> {
                    onLoginSuccess(event.userId, event.email)
                }
                is LoginViewModel.UiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                    viewModel.logUiError(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Only handle the status bar height here
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
                Image(
                    painter = painterResource(id = R.drawable.scoreprof_launcher_round),
                    contentDescription = "ScoreProf Logo",
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // STEP 1: Selection Mode
            if (!showFields) {
                Text(
                    text = stringResource(R.string.scoreprof_login),
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
                    Text(stringResource(R.string.login))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.setLoginMode(false)
                        showFields = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.sign_up))
                }
            }
            // STEP 2: Input Mode
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
                        text = if (isLoginMode) stringResource(R.string.login) else stringResource(R.string.sign_up),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text(stringResource(R.string.email)) },
                    isError = hasAttemptedLogin && !isEmailValid,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = passwordInput,
                    label = { Text(stringResource(R.string.password)) },
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

                // NEW: Confirm Password Field (Only for Sign Up)
                if (!isLoginMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        isError = hasAttemptedLogin && !passwordsMatch,
                        supportingText = {
                            if (hasAttemptedLogin && !passwordsMatch) {
                                Text(stringResource(R.string.password_mismatch), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            hasAttemptedLogin = true
                            if (isEmailValid && !isPasswordTooShort && email.isNotEmpty() && passwordsMatch) {
                                viewModel.onPasswordChange(passwordInput)
                                viewModel.login()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoginMode) stringResource(R.string.login) else stringResource(R.string.sign_up))
                    }

                    if (isLoginMode) {
                        TextButton(onClick = {
                            navController.navigate("forgot_password")
                        }) {
                            Text(
                                text = stringResource(R.string.forgot_password),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!isLoginMode) {
                    Text(
                        text = stringResource(R.string.signup_text),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1.5f))
        }
    }
}