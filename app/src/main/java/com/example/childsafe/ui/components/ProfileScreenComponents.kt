// ProfileScreenComponents.kt
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


data class StudentProfile(
    var name: String = "",
    var school: String = "",
    var birthdate: String = "",
    var year: String = ""
)

data class ParentProfile(
    var name: String = "",
    var birthdate: String = "",
    var childName: String = ""
)

@Composable
fun StudentIdCard(profile: StudentProfile, cardColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor) // light yellow background
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "THẺ HỌC SINH",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            HorizontalDivider(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .height(1.dp)
                    .fillMaxWidth(0.6f)
                    .align(Alignment.CenterHorizontally),
                color = Color.DarkGray,
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, Color.Gray, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Photo",
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info columns
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("TÊN", fontSize = 12.sp)
                        Text(profile.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("TRƯỜNG", fontSize = 12.sp)
                        Text(profile.school, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Column {
                        Text("NGÀY SINH", fontSize = 12.sp)
                        Text(profile.birthdate, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("NĂM HỌC", fontSize = 12.sp)
                        Text(profile.year, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

}

@Composable
fun ColorSelector(
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        BorderStroke(
                            width = if (color == selectedColor) 2.dp else 0.dp,
                            color = Color.Black
                        ),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun StudentProfileFormFields(
    profile: StudentProfile,
    onProfileChange: (StudentProfile) -> Unit
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Tên",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = profile.name,
            onValueChange = { onProfileChange(profile.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Tên") }
        )

        Text(
            text = "Ngày sinh",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = profile.birthdate,
            onValueChange = { onProfileChange(profile.copy(birthdate = it)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Ngày sinh") }
        )

        Text(
            text = "Trường",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = profile.school,
            onValueChange = { onProfileChange(profile.copy(school = it)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Trường") }
        )

        Text(
            text = "Niên học",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = profile.year,
            onValueChange = { onProfileChange(profile.copy(year = it)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Năm học") }
        )
    }
}

@Composable
fun ParentProfileFormFields(
    profile: ParentProfile,
    onProfileChange: (ParentProfile) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Tên",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = profile.name,
            onValueChange = { onProfileChange(profile.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Tên") }
        )

        Text(
            text = "Ngày sinh",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = profile.birthdate,
            onValueChange = { onProfileChange(profile.copy(birthdate = it)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Ngày sinh") }
        )

        Text(
            text = "Con của bạn tên gì?",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = profile.childName,
            onValueChange = { onProfileChange(profile.copy(childName = it)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Tên con của bạn") }
        )
    }
}

@Composable
fun CharacterMonitorWithRoad(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Road background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 55.dp)
                .height(60.dp)
                .background(Color(0xFFE0E0E0))
                .align(Alignment.Center)
        ) {
            // Dotted line in the middle of the road
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(20) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(4.dp)
                            .background(Color.White)
                    )
                }
            }
        }

        // Character
        Image(
            painter = painterResource(id = R.drawable.walking_character),
            contentDescription = "Step Monitor Character",
            modifier = Modifier
                .size(100.dp)
                .offset(y = (-8).dp) // Slight offset to place character properly on the road
        )
    }
}