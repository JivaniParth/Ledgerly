package com.parthjivani.hisabtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parthjivani.hisabtracker.ui.HisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HisabViewModel
) {
    val userName by viewModel.userName.collectAsState()
    var editingName by remember { mutableStateOf(userName) }
    var isEditingName by remember { mutableStateOf(false) }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isChangingPassword by remember { mutableStateOf(false) }
    var passwordMessage by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My Profile",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Profile Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Profile Details", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                if (isEditingName) {
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isEditingName = false; editingName = userName }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            viewModel.updateUserName(editingName.trim())
                            isEditingName = false
                        }) {
                            Text("Save")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Display Name", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(userName.ifEmpty { "Not Set" }, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = { isEditingName = true; editingName = userName }) {
                            Text("Edit")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Change Password Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Security", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Change Password", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; passwordError = null; passwordMessage = null },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordError = null; passwordMessage = null },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                if (passwordError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                if (passwordMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = passwordMessage!!, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordError = "Password must be at least 6 characters long."
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            passwordError = "Passwords do not match."
                            return@Button
                        }
                        isChangingPassword = true
                        passwordError = null
                        passwordMessage = null
                        
                        viewModel.updatePassword(newPassword) { success, errorMsg ->
                            isChangingPassword = false
                            if (success) {
                                passwordMessage = "Password successfully updated!"
                                newPassword = ""
                                confirmPassword = ""
                            } else {
                                passwordError = errorMsg ?: "Failed to update password."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChangingPassword
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Update Password")
                    }
                }
            }
        }
    }
}
