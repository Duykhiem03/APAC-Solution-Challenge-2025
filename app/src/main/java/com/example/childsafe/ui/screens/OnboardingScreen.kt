package com.example.childsafe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.childsafe.R
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.theme.ChildSafeTheme

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TriangleMascot()

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Chào bạn,\nsẵn sàng khám phá chưa?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "bắt đầu hành trình an toàn\nhãy để ChildSafe giúp bạn di chuyển đầy tự tin!",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp),
                border = BorderStroke(1.dp, Color.Black),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700)
                )
            ) {
                Text(
                    text = "BẮT ĐẦU NGAY!",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TriangleMascot() {
    Box(
        contentAlignment = Alignment.Center
    ) {
        // Simplified representation - you'd use a custom drawing or image resource
        Image(
            painter = painterResource(R.drawable.triangle_mascot),
            contentDescription = "Mascot",
            modifier = Modifier.size(300.dp)
        )
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    ChildSafeTheme {
        OnboardingScreen(onGetStarted = {})
    }
}

