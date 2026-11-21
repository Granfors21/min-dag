package se.robgra.mindag

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Egna foton till steg: beskärs till kvadrat, skalas till 512 px och sparas i appens privata lagring. */
object Photos {
    const val SIZE = 512
    fun dir(ctx: Context) = File(ctx.filesDir, "steg").apply { mkdirs() }
    fun file(ctx: Context, name: String) = File(dir(ctx), name)
    fun cameraTemp(ctx: Context) = File(ctx.cacheDir, "kamera").apply { mkdirs() }.let { File(it, "foto.jpg") }

    fun importUri(ctx: Context, uri: Uri): String? {
        return try {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val rot = try { ExifInterface(bytes.inputStream()).rotationDegrees } catch (_: Exception) { 0 }
            importBytes(ctx, bytes, rot)
        } catch (_: Exception) { null }
    }

    fun importBytes(ctx: Context, bytes: ByteArray, rotation: Int = 0): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
        val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        return save(ctx, src, rotation)
    }

    fun save(ctx: Context, src: Bitmap, rotation: Int = 0): String {
        val side = min(src.width, src.height)
        val m = Matrix().apply {
            val sc = SIZE.toFloat() / side
            postScale(sc, sc)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        val sq = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side, m, true)
        val name = "steg-" + UUID.randomUUID().toString().take(12) + ".jpg"
        file(ctx, name).outputStream().use { sq.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return name
    }

    fun load(ctx: Context, name: String, maxPx: Int = SIZE): Bitmap? {
        val f = file(ctx, name)
        if (!f.exists()) return null
        return BitmapFactory.decodeFile(f.path)?.let { b ->
            if (max(b.width, b.height) <= maxPx) b else Bitmap.createScaledBitmap(b, maxPx, maxPx, true)
        }
    }

    /** Ta bort foton som inget steg använder längre. */
    fun cleanup(ctx: Context, s: Settings) {
        val used = s.routines.flatMap { r -> r.steps.mapNotNull { it.image } }.toSet()
        dir(ctx).listFiles()?.forEach { if (it.name !in used) it.delete() }
    }
}
