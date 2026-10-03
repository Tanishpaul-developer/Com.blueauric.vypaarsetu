package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val initialX: Float, // 0.0 to 1.0 (relative screen width)
    val initialY: Float, // -0.1 to 0.4 (relative screen height)
    val vx: Float,       // horizontal velocity
    val vy: Float,       // vertical velocity
    val gravity: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float,
    val isCircle: Boolean,
    val wobbleSpeed: Float
)

private val ConfettiColors = listOf(
    Color(0xFF10B981), // Emerald Green
    Color(0xFFF59E0B), // Warm Gold
    Color(0xFF1A56DB), // Trust Blue
    Color(0xFFEC4899), // Pink
    Color(0xFF8B5CF6), // Purple
    Color(0xFF06B6D4), // Cyan
    Color(0xFFF43F5E), // Coral
    Color(0xFFEAB308)  // Vibrant Yellow
)

@Composable
fun ConfettiExplosionOverlay(
    active: Boolean,
    durationMillis: Long = 3200L,
    particleCount: Int = 75,
    onFinished: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = active,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val particles = remember(active) {
            if (!active) emptyList() else List(particleCount) {
                val rand = Random(it * 31 + System.currentTimeMillis())
                ConfettiParticle(
                    initialX = rand.nextFloat(),
                    initialY = rand.nextFloat() * 0.35f - 0.05f,
                    vx = (rand.nextFloat() - 0.5f) * 600f,
                    vy = -rand.nextFloat() * 450f - 150f, // initial upward pop
                    gravity = 750f + rand.nextFloat() * 300f,
                    size = 10f + rand.nextFloat() * 16f,
                    color = ConfettiColors[rand.nextInt(ConfettiColors.size)],
                    rotationSpeed = (rand.nextFloat() - 0.5f) * 720f,
                    isCircle = rand.nextBoolean(),
                    wobbleSpeed = 3f + rand.nextFloat() * 6f
                )
            }
        }

        var startTime by remember(active) { mutableLongStateOf(0L) }
        var elapsedTime by remember(active) { mutableLongStateOf(0L) }

        LaunchedEffect(active) {
            if (active) {
                val start = withFrameMillis { it }
                startTime = start
                while (true) {
                    val frameTime = withFrameMillis { it }
                    val currentElapsed = frameTime - start
                    elapsedTime = currentElapsed
                    if (currentElapsed >= durationMillis) {
                        onFinished()
                        break
                    }
                }
            }
        }

        if (active && particles.isNotEmpty()) {
            val progress = (elapsedTime.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
            val alpha = if (progress > 0.8f) (1f - progress) / 0.2f else 1f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val timeSec = elapsedTime / 1000f

                particles.forEach { p ->
                    val x = (p.initialX * canvasWidth) + (p.vx * timeSec) + (sin(timeSec * p.wobbleSpeed) * 35f)
                    val y = (p.initialY * canvasHeight) + (p.vy * timeSec) + (0.5f * p.gravity * timeSec * timeSec)
                    val rotation = p.rotationSpeed * timeSec

                    if (y in -50f..(canvasHeight + 50f) && x in -50f..(canvasWidth + 50f)) {
                        rotate(degrees = rotation, pivot = Offset(x, y)) {
                            val colorWithAlpha = p.color.copy(alpha = alpha.coerceIn(0f, 1f))
                            if (p.isCircle) {
                                drawCircle(
                                    color = colorWithAlpha,
                                    radius = p.size / 2f,
                                    center = Offset(x, y)
                                )
                            } else {
                                drawRect(
                                    color = colorWithAlpha,
                                    topLeft = Offset(x - p.size / 2f, y - p.size / 3f),
                                    size = Size(p.size, p.size * 0.65f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
