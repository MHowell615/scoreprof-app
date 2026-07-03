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
import scoreprof_resources.Res
import scoreprof_resources.forgot_password
import scoreprof_resources.forgot_password_text1
import scoreprof_resources.email
import scoreprof_resources.new_password
import scoreprof_resources.reset_password_text1
import scoreprof_resources.six_digit_code
import scoreprof_resources.short_password_msg
import scoreprof_resources.send_reset_code
import scoreprof_resources.reset_password
import scoreprof_resources.update_password
import scoreprof_resources.back_to_login
import cloud.scoreprof.app.ui.view_models.LoginViewModel
import org.jetbrains.compose.resources.stringResource

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
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (step == 1) stringResource(Res.string.forgot_password) else stringResource(Res.string.reset_password),
                style = MaterialTheme.typography.headlineMedium
            )

            if (step == 1) {
                Text(stringResource(Res.string.forgot_password_text1))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(Res.string.email)) },
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
                    Text(stringResource(Res.string.send_reset_code))
                }
            } else {
                Text(stringResource(Res.string.reset_password_text1))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(Res.string.six_digit_code)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(Res.string.new_password)) },
                    isError = isPasswordTooShort,
                    supportingText = {
                        if (isPasswordTooShort) {
                            Text(
                                stringResource(Res.string.short_password_msg),
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
                    Text(stringResource(Res.string.update_password))
                }
            }

            TextButton(onClick = { navController.popBackStack() }) {
                Text(stringResource(Res.string.back_to_login))
            }
        }
    }
}
