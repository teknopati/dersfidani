package com.dersfidan.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Gün veya kart tamamlanınca alkış sesiyle birlikte gösterilen sade kutlama kartı. */
@Composable
fun GunTamamlandiKutlama(goster: Boolean, bonusPuan: Int, onKapat: () -> Unit) {
    AnimatedVisibility(visible = goster, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible = goster, enter = scaleIn(initialScale = .7f) + fadeIn()) {
                Column(
                    Modifier.padding(32.dp).clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👏", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Tebrikler!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Yeni bir hedefi tamamladın ve ödül kazandın.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text("+$bonusPuan bonus puan 🌟", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onKapat) { Text("Harika!") }
                }
            }
        }
    }
}
