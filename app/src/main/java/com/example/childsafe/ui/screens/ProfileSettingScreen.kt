// ProfileSettingScreen.kt
package com.example.childsafe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.theme.ChildSafeTheme

@Composable
fun ProfileSettingScreen(
    onComplete: () -> Unit,
    onSignOut: () -> Unit = {} // Add callback for sign out navigation
) {    var selectedColor by remember { mutableStateOf(Color.LightGray) }
    var selectedButton by remember { mutableStateOf("student") } // Set student as default
    var studentProfile by remember { mutableStateOf(StudentProfile()) }
    var parentProfile by remember { mutableStateOf(ParentProfile()) }
    
    // Local state for showing confirmation dialog
    var showSignOutDialog by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 40.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selectedButton == "student") {
            StudentIdCard(profile = studentProfile, cardColor = selectedColor)
        } else {
            Spacer(modifier = Modifier.height(32.dp))
            CharacterMonitorWithRoad()
            Text(
                text = "Đồng hành cùng con trên mọi chặng đường!",
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Vai trò của bạn",
            fontSize = 14.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    selectedButton = "student"
                },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selectedButton == "student") Color.Yellow else Color.White)
            ) {
                Text("Học sinh", color = Color.Black)
            }


            OutlinedButton(

                onClick = { selectedButton = "parent" },
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selectedButton == "parent") Color.Yellow else Color.White)
            ) {
                Text("Phụ huynh", color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Màu sắc",
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        ColorSelector(
            colors = listOf(Color.LightGray, Color(0xFFFFD700), Color(0xFF87CEEB), Color(0xFFF08080), Color(0xFF90EE90)),
            selectedColor = selectedColor,
            onColorSelected = { selectedColor = it }
        )        
        if (selectedButton == "student") {
            StudentProfileFormFields(profile = studentProfile, onProfileChange = { studentProfile = it })
        } else {
            ParentProfileFormFields(profile = parentProfile, onProfileChange = { parentProfile = it })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onComplete,
            border = BorderStroke(1.dp, Color.Black),
            modifier = Modifier
                .width(140.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD700)
            )
        ) {
            Text(
                text = "Hoàn tất",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
          // Sign Out Button
        OutlinedButton(
            onClick = { showSignOutDialog = true },
            border = BorderStroke(1.dp, Color.Red),
            modifier = Modifier
                .width(140.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Red
            )
        ) {
            Text(
                text = "Đăng xuất",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        
        // Sign Out Confirmation Dialog
        if (showSignOutDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Xác nhận đăng xuất") },
                text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng không?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            onSignOut() // Call the callback to handle navigation
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Đăng xuất", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showSignOutDialog = false }
                    ) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun ProfileSettingScreenPreview() {
    ChildSafeTheme {
        ProfileSettingScreen(onComplete = {})
    }
}