package com.example.childsafe.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun PhoneAuthScreen(
    viewModel: PhoneAuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity
    
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Show error messages in snackbar
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = (authState as AuthState.Error).message
                    )
                }
            }
            is AuthState.Success -> {
                onAuthSuccess()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ChildSafe",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Đăng nhập với số điện thoại",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Để bảo vệ tài khoản của bạn, chúng tôi sẽ xác minh danh tính của bạn bằng tin nhắn SMS",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                when (authState) {
                    is AuthState.Idle, is AuthState.Error -> {
                        // Phone number input step
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Số điện thoại (với mã quốc gia)") },
                            placeholder = { Text("+84 xxx xxx xxx") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { viewModel.verifyPhoneNumber(phoneNumber, activity) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = phoneNumber.length >= 8 // Basic validation
                        ) {
                            Text("Gửi mã xác nhận")
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Lưu ý:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "• Hãy nhập số điện thoại với mã quốc gia (ví dụ: +84 cho Việt Nam)\n• Bạn sẽ nhận được mã xác nhận qua SMS\n• Có thể mất vài phút để nhận được tin nhắn",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    is AuthState.CodeSent -> {
                        // Verification code input step
                        Text(
                            text = "Nhập mã xác nhận đã gửi đến",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                        
                        Text(
                            text = phoneNumber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { 
                                // Only allow 6 digits
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    verificationCode = it 
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Mã xác nhận (6 chữ số)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { viewModel.submitVerificationCode(verificationCode) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = verificationCode.length == 6
                        ) {
                            Text("Xác nhận và đăng nhập")
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Không nhận được mã?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "• Kiểm tra tin nhắn trong hộp thư SMS\n• Đảm bảo số điện thoại nhập đúng\n• Có thể mất vài phút để tin nhắn đến",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                TextButton(
                                    onClick = { 
                                        verificationCode = "" 
                                        viewModel.resendVerificationCode(phoneNumber, activity) 
                                    }
                                ) {
                                    Text("Gửi lại mã")
                                }
                            }
                        }
                    }
                    
                    is AuthState.Loading -> {
                        CircularProgressIndicator()
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Processing...",
                            fontSize = 14.sp
                        )
                    }
                    
                    else -> { /* Success is handled by LaunchedEffect */ }
                }
            }
        }
    }
}
