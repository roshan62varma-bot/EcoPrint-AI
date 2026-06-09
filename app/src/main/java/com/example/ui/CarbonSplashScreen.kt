package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CarbonSplashScreen(onTimeout: () -> Unit) {
    // 1. Initialized delay trigger
    LaunchedEffect(key1 = Unit) {
        delay(2200) // Beautiful 2.2s intro visual timeout
        onTimeout()
    }

    // 2. Continuous rotating and scaling values for orbital animation
    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnimations")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseEffect"
    )

    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowGlow"
    )

    // Delayed text entry visibility simulation
    var startTextReveal by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = Unit) {
        delay(150)
        startTextReveal = true
    }

    val textAlpha by animateFloatAsState(
        targetValue = if (startTextReveal) 1f else 0f,
        animationSpec = tween(1000, easing = EaseInOutQuad),
        label = "TextFade"
    )

    val textScale by animateFloatAsState(
        targetValue = if (startTextReveal) 1.0f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "TextScale"
    )

    // Dark majestic botanical space theme
    val darkGreenGrad = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF040A06), // Midnight charcoal green
            Color(0xFF0B1910), // Rich forest shade
            Color(0xFF040A06)  // Background deep base
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkGreenGrad),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            
            // Core Animated Visual Leaf and Halo Canvas
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scalePulse),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val halfW = size.width / 2f
                    val halfH = size.height / 2f
                    val innerRadius = 52.dp.toPx()

                    // Glowing outer ring backing
                    drawCircle(
                        color = Color(0xFF5EDD9E).copy(alpha = 0.08f * wavePulse),
                        radius = innerRadius + 24.dp.toPx()
                    )

                    // Secondary tracking ring backing
                    drawCircle(
                        color = Color(0xFF81C784).copy(alpha = 0.12f),
                        radius = innerRadius + 8.dp.toPx(),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Spinning primary mint aura dash circle
                    drawArc(
                        color = Color(0xFF5EDD9E),
                        startAngle = rotationAngle,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(halfW - innerRadius, halfH - innerRadius),
                        size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                    
                    // Symmetrical shadow arc tracking the spin
                    drawArc(
                        color = Color(0xFF2E7D32).copy(alpha = 0.4f),
                        startAngle = rotationAngle + 180f,
                        sweepAngle = 80f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(halfW - innerRadius, halfH - innerRadius),
                        size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw organic stylized twin seedling leaf inside
                    val leafScaleWidth = 0.7f
                    val leafScaleHeight = 0.7f
                    val leafCenterOffset = 4.dp.toPx()

                    // Left leaf path with emerald paint
                    val leftLeafPath = Path().apply {
                        moveTo(halfW, halfH + 20.dp.toPx())
                        cubicTo(
                            halfW - (32.dp.toPx() * leafScaleWidth), halfH + (12.dp.toPx() * leafScaleHeight),
                            halfW - (44.dp.toPx() * leafScaleWidth), halfH - (18.dp.toPx() * leafScaleHeight),
                            halfW - leafCenterOffset, halfH - (32.dp.toPx() * leafScaleHeight)
                        )
                        cubicTo(
                            halfW, halfH - (14.dp.toPx() * leafScaleHeight),
                            halfW, halfH + (10.dp.toPx() * leafScaleHeight),
                            halfW, halfH + 20.dp.toPx()
                        )
                        close()
                    }
                    drawPath(path = leftLeafPath, color = Color(0xFF1B5E20))

                    // Right leaf path with glowing light green pastel paint
                    val rightLeafPath = Path().apply {
                        moveTo(halfW, halfH + 20.dp.toPx())
                        cubicTo(
                            halfW + (32.dp.toPx() * leafScaleWidth), halfH + (12.dp.toPx() * leafScaleHeight),
                            halfW + (44.dp.toPx() * leafScaleWidth), halfH - (18.dp.toPx() * leafScaleHeight),
                            halfW + leafCenterOffset, halfH - (32.dp.toPx() * leafScaleHeight)
                        )
                        cubicTo(
                            halfW, halfH - (14.dp.toPx() * leafScaleHeight),
                            halfW, halfH + (10.dp.toPx() * leafScaleHeight),
                            halfW, halfH + 20.dp.toPx()
                        )
                        close()
                    }
                    drawPath(path = rightLeafPath, color = Color(0xFF81C784))

                    // Central stem highlight drawing
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = androidx.compose.ui.geometry.Offset(halfW, halfH + 22.dp.toPx()),
                        end = androidx.compose.ui.geometry.Offset(halfW, halfH - 12.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Animated textual headers column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha)
                    .scale(textScale)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "V E R D A N T",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF5EDD9E),
                        letterSpacing = 5.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Personalized Carbon Footprint Tracker".uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA5BFA7),
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Smooth organic custom greeting block
                Box(
                    modifier = Modifier
                        .background(Color(0xFF132218), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "🌱 \"Every small habit shapes our tomorrow.\"",
                        fontSize = 11.sp,
                        color = Color(0xFF5EDD9E),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(72.dp))

                // Bottom Loading Spinner Track Indicator
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color(0xFF81C784).copy(alpha = 0.2f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color(0xFF5EDD9E),
                            startAngle = rotationAngle * 1.5f,
                            sweepAngle = 100f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Initializing eco dashboard...",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF81C784).copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
