package com.dersfidan.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.dersfidan.app.R

@Composable
private fun DynamicFarmImage(names: List<String>, description: String?, modifier: Modifier, scale: ContentScale) {
    val context = LocalContext.current
    val id = remember(names) {
        names.firstNotNullOfOrNull { name ->
            context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 }
        } ?: 0
    }
    Image(painterResource(id.takeIf { it != 0 } ?: R.drawable.garden_background_green), description, modifier, contentScale = scale)
}

@Composable internal fun AnimalPortrait(esyaId: String, variant: Int, modifier: Modifier = Modifier, age: Int = 20) {
    val v = variant.coerceIn(1, 5)
    val yasOlcegi = (.72f + age.coerceIn(1, 20) / 20f * .28f)
    Box(modifier) {
        DynamicFarmImage(
            listOf("animal_hd_${esyaId}_v$v", "animal_hd_${esyaId}_v1", "animal_${esyaId}_v$v"),
            esyaId,
            Modifier.fillMaxSize().graphicsLayer {
                // Aynı türün sonraki üyeleri kartta kolayca ayırt edilsin.
                scaleX = (if (v % 2 == 0) -.97f else .97f) * yasOlcegi
                scaleY = (when (v) { 3 -> .93f; 5 -> 1.02f; else -> .97f }) * yasOlcegi
                rotationZ = when (v) { 2 -> -2.5f; 4 -> 2.5f; else -> 0f }
            },
            ContentScale.Fit
        )
    }
}

@Composable internal fun HabitatPortrait(esyaId: String, modifier: Modifier = Modifier) =
    DynamicFarmImage(listOf("habitat_hd_$esyaId", "building_$esyaId", "farm_habitat_gallery"), "Habitat", modifier, ContentScale.Fit)

@Composable internal fun BuildingPortrait(esyaId: String, modifier: Modifier = Modifier) =
    DynamicFarmImage(listOf("building_hd_$esyaId", "building_$esyaId"), esyaId, modifier.softFarmEdges(), ContentScale.Fit)

@Composable internal fun TreePortrait(turId: Int, modifier: Modifier = Modifier, stage: Int = 5) {
    Box(modifier) {
        if (stage <= 2) {
            Canvas(Modifier.fillMaxSize().padding(18.dp)) {
                val oran = when (stage) { 0 -> .18f; 1 -> .36f; else -> .58f }
                val taban = size.height * .84f
                val govdeUst = taban - size.height * (.10f + oran * .34f)
                drawOval(Color(0xFF81502E), topLeft = Offset(size.width * .30f, taban - size.height * .035f), size = Size(size.width * .40f, size.height * .07f))
                if (stage == 0) {
                    drawOval(Color(0xFFB9823E), topLeft = Offset(size.width * .46f, taban - size.height * .07f), size = Size(size.width * .08f, size.height * .045f))
                } else {
                    drawLine(Color(0xFF6B442A), Offset(size.width / 2f, taban), Offset(size.width / 2f, govdeUst), size.width * .025f, StrokeCap.Round)
                    val yaprakBoyu = size.width * (.12f + oran * .09f)
                    drawOval(Color(0xFF63B83E), Offset(size.width / 2f - yaprakBoyu, govdeUst), Size(yaprakBoyu, yaprakBoyu * .62f))
                    drawOval(Color(0xFF83CF4E), Offset(size.width / 2f, govdeUst - yaprakBoyu * .12f), Size(yaprakBoyu, yaprakBoyu * .62f))
                    if (stage == 2) {
                        drawOval(Color(0xFF4A9C36), Offset(size.width / 2f - yaprakBoyu * 1.35f, govdeUst + yaprakBoyu * .42f), Size(yaprakBoyu, yaprakBoyu * .62f))
                        drawOval(Color(0xFF72C447), Offset(size.width / 2f + yaprakBoyu * .35f, govdeUst + yaprakBoyu * .35f), Size(yaprakBoyu, yaprakBoyu * .62f))
                    }
                }
            }
        } else {
            val olcek = when (stage) { 3 -> .62f; 4 -> .80f; else -> 1f }
            DynamicFarmImage(
                listOf("tree_${Math.floorMod(turId, 100).toString().padStart(3, '0')}"),
                "Ağaç",
                Modifier.fillMaxSize().graphicsLayer { scaleX = olcek; scaleY = olcek }.softFarmEdges(),
                ContentScale.Fit
            )
        }
    }
}

@Composable internal fun FarmerPortrait(tip: Int, modifier: Modifier = Modifier, waving: Boolean = false) {
    val index = tip.coerceIn(0, 4) + 1
    val pose = if (waving) "wave" else "idle"
    DynamicFarmImage(
        listOf("farmer_hd_${index}_$pose", "farmer_hd_${index}_idle", "farmer_$index"),
        "Çiftçi",
        modifier.softFarmEdges(),
        ContentScale.Fit
    )
}

/** Eski kare illüstrasyonların sınırını yeni çiftlik sahnesine karıştırır. */
private fun Modifier.softFarmEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Color.White, .46f to Color.White, .80f to Color.Transparent, 1f to Color.Transparent),
                center = center,
                radius = size.maxDimension * .58f
            ),
            blendMode = BlendMode.DstIn
        )
    }
