package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.ui.view_models.LoginViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: LoginViewModel,
    navController: NavHostController
) {
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isPasswordTooShort = newPassword.isNotEmpty() && newPassword.length < 6
    var step by remember { mutableIntStateOf(1) } // 1: Request Code, 2: Reset Password

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (step == 1) "Forgot Password" else "Reset Password",
                style = MaterialTheme.typography.headlineMedium
            )

            if (step == 1) {
                Text("Enter your email address to receive a 6-digit reset code.")

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.requestReset(email)
                        step = 2
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.contains("@")
                ) {
                    Text("Send Reset Code")
                }
            } else {
                Text("Enter the code sent to your email and your new password.")

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("6-Digit Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    isError = isPasswordTooShort,
                    supportingText = {
                        if (isPasswordTooShort) {
                            Text(
                                "Password must be at least 6 characters",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, if (passwordVisible) "Hide" else "Show")
                        }
                    },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.resetPassword(email, code, newPassword)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.length == 6 && newPassword.length >= 6
                ) {
                    Text("Update Password")
                }
            }

            TextButton(onClick = { navController.popBackStack() }) {
                Text("Back to Login")
            }
        }
    }
}