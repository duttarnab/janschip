package com.example.jans_chip.android

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.jans_chip.service.AuthService
import com.example.jans_chip.service.DiscoveryService
import com.example.jans_chip.service.LogoutService
import com.example.jans_chip.storage.AndroidSettingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
sealed class Screen {
    object Discovery : Screen()
    object Login : Screen()
    data class UserInfo(val info: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = AndroidSettingsProvider(applicationContext).getSettings()
        val discoveryService = DiscoveryService(settings)
        val authService = AuthService(settings)
        val logoutService = LogoutService(settings)



        setContent {
            MaterialTheme {
                val context = LocalContext.current
                var discoveryUrl by remember { mutableStateOf("") }
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Discovery) }

                // Load persistent config state
                LaunchedEffect(Unit) {
                    if (discoveryService.isConfigStored()) {
                        currentScreen = Screen.Login
                    }
                }

                when (currentScreen) {
                    is Screen.Discovery -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            TextField(
                                value = discoveryUrl,
                                onValueChange = { discoveryUrl = it },
                                label = { Text("Enter OpenId Provider URL") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val result = discoveryService.fetchConfigAndRegisterClient(discoveryUrl)
                                    if (result.success) {
                                        currentScreen = Screen.Login
                                        Toast.makeText(context, "Config saved", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to fetch config", Toast.LENGTH_SHORT).show()
                                        result.errorMessage?.let { Log.e("Error in fetchConfigAndRegisterClient: ", it) }
                                    }
                                }
                            }) {
                                Text("Submit")
                            }
                        }
                    }
                    is Screen.Login -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Login", color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = username,
                                onValueChange = {username = it},
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = password,
                                onValueChange = {password = it},
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                Toast.makeText(context, "Login attempted", Toast.LENGTH_SHORT).show()
                                CoroutineScope(Dispatchers.Main).launch {
                                    val result = authService.authenticate(username, password)
                                    if (result.success) {
                                        currentScreen = Screen.UserInfo(result.message ?: "")
                                    } else {
                                        Toast.makeText(context, result.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Text("Login")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                discoveryService.clearConfig()
                                discoveryUrl = ""
                                currentScreen = Screen.Discovery
                                Toast.makeText(context, "Config reset", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Reset Config")
                            }
                        }
                    }
                    is Screen.UserInfo -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("User Info", color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text((currentScreen as Screen.UserInfo).info, color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    logoutService.logout()
                                    currentScreen = Screen.Login
                                }
                                Toast.makeText(context, "Session cleared", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Logout")
                            }
                        }
                    }
                }
            }
        }
    }
}
