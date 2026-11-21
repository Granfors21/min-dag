package se.robgra.mindag

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object C {
    val bg = Color(0xFFF6F3EE)
    val card = Color(0xFFFFFFFF)
    val text = Color(0xFF3D4A52)
    val soft = Color(0xFF7C8A92)
    val sage = Color(0xFF8FB8A8)
    val sageDark = Color(0xFF5E8F7D)
    val sageLight = Color(0xFFDCE8E3)
    val blue = Color(0xFFA9C4D9)
    val blueLight = Color(0xFFE3EDF4)
    val wedge = Color(0xFFE3A08A)
    val wedgeLight = Color(0xFFF6DDD3)
    val sand = Color(0xFFEFE7DA)
    val night = Color(0xFF1F2740)
    val nightCard = Color(0xFF2B3454)
    val nightText = Color(0xFFD9DEEA)
    val moon = Color(0xFFF2E6B8)
}

@Composable
fun MinDagTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.sageDark, onPrimary = Color.White, secondary = C.blue,
            background = C.bg, surface = C.card, onSurface = C.text, onBackground = C.text,
            primaryContainer = C.sageLight, surfaceVariant = C.sand,
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 20.sp),
            bodyMedium = TextStyle(fontSize = 18.sp),
            titleLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
            labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        ),
        content = content,
    )
}
