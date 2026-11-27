package com.example.explorelanka.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.explorelanka.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    var createAccount by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Explore Lanka", style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Your personal travel companion", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(30.dp))
        Text(if (createAccount) "Create an account" else "Welcome back",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        if (createAccount) {
            OutlinedTextField(name, { name = it }, label = { Text("Full name") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(email, { email = it }, label = { Text("Email") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        viewModel.notice?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = {
                if (createAccount) viewModel.register(name, email, password)
                else viewModel.signIn(email, password)
            },
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewModel.busy) CircularProgressIndicator(
                modifier = Modifier.size(20.dp), strokeWidth = 2.dp
            ) else Text(if (createAccount) "Create account" else "Sign in")
        }
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = { createAccount = !createAccount; viewModel.clearNotice() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (createAccount) "Already have an account? Sign in" else "New here? Create account")
        }
        if (!createAccount) {
            TextButton(onClick = { viewModel.resetPassword(email) },
                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Forgot password?")
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Sign in to keep your trips separate and synchronize them across devices.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
