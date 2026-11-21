package se.robgra.mindag

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Urtavla med siffror 1–12. Tårtbiten börjar vid minutvisarens läge nu och slutar där minutvisaren
 * står när tiden är slut, så den krymper när visaren rör sig in i den (som en Time Timer).
 * Längre än 60 minuter visas som en hel tårta.
 */
@Composable
fun WedgeClock(
    now: Long,
    endAt: Long?,
    modifier: Modifier = Modifier,
    wedgeColor: Color = C.wedge,
    face: Color = Color.White,
    ink: Color = C.text,
) {
    Box(modifier.aspectRatio(1f)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = min(size.width, size.height) / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(face, r, c)
            drawCircle(ink.copy(alpha = 0.25f), r, c, style = Stroke(width = r * 0.025f))

            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val minuteF = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
            val hourF = (cal.get(Calendar.HOUR) + minuteF / 60f)

            if (endAt != null && endAt > now) {
                val remainMin = ((endAt - now) / 60_000f).coerceAtMost(60f)
                val startAngle = minuteF * 6f - 90f
                val sweep = remainMin * 6f
                val wr = r * 0.80f
                drawArc(wedgeColor, startAngle, sweep, useCenter = true,
                    topLeft = Offset(c.x - wr, c.y - wr), size = Size(wr * 2, wr * 2))
            }
            // Minutstreck
            for (i in 0 until 60) {
                val a = Math.toRadians((i * 6 - 90).toDouble())
                val outer = r * 0.95f
                val inner = if (i % 5 == 0) r * 0.86f else r * 0.91f
                drawLine(ink.copy(alpha = if (i % 5 == 0) 0.7f else 0.3f),
                    Offset(c.x + cos(a).toFloat() * inner, c.y + sin(a).toFloat() * inner),
                    Offset(c.x + cos(a).toFloat() * outer, c.y + sin(a).toFloat() * outer),
                    strokeWidth = if (i % 5 == 0) r * 0.02f else r * 0.01f)
            }
            // Siffror
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true; color = ink.toArgb(); textSize = r * 0.17f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            for (n in 1..12) {
                val a = Math.toRadians((n * 30 - 90).toDouble())
                val tr = r * 0.70f
                val x = c.x + cos(a).toFloat() * tr
                val y = c.y + sin(a).toFloat() * tr - (paint.descent() + paint.ascent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(n.toString(), x, y, paint)
            }
            // Visare
            fun hand(angleDeg: Float, len: Float, w: Float, col: Color) {
                val a = Math.toRadians((angleDeg - 90).toDouble())
                drawLine(col, c, Offset(c.x + cos(a).toFloat() * len, c.y + sin(a).toFloat() * len), strokeWidth = w, cap = StrokeCap.Round)
            }
            hand(hourF * 30f, r * 0.45f, r * 0.06f, ink)
            hand(minuteF * 6f, r * 0.74f, r * 0.04f, ink)
            drawCircle(ink, r * 0.06f, c)
        }
    }
}

