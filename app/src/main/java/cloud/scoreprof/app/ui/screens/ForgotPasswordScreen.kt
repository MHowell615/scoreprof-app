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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
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
                text = if (step == 1) stringResource(R.string.forgot_password) else stringResource(R.string.reset_password),
                style = MaterialTheme.typography.headlineMedium
            )

            if (step == 1) {
                Text(stringResource(R.string.forgot_password_text1))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
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
                    Text(stringResource(R.string.send_reset_code))
                }
            } else {
                Text(stringResource(R.string.reset_password_text1))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.six_digit_code)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    isError = isPasswordTooShort,
                    supportingText = {
                        if (isPasswordTooShort) {
                            Text(
                                stringResource(R.string.short_password_msg),
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
                    Text(stringResource(R.string.update_password))
                }
            }

            TextButton(onClick = { navController.popBackStack() }) {
                Text(stringResource(R.string.back_to_login))
            }
        }
    }
}