package com.klyx.data.preferences

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import com.klyx.api.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File

@Single
class FontManager(private val context: Context) {

    private var cachedUri: String? = null
    private var cachedFontFamily: FontFamily? = null

    /**
     * Suspending function to load the font on the IO dispatcher.
     * It caches the result in memory to prevent repeated disk access.
     */
    suspend fun getFontFamily(uriString: String?): FontFamily {
        if (uriString.isNullOrBlank()) {
            return JetBrainsMonoFontFamily
        }

        if (uriString == cachedUri && cachedFontFamily != null) {
            return cachedFontFamily!!
        }

        return withContext(Dispatchers.IO) {
            try {
                val fontFile = File(context.cacheDir, "current_custom_font.ttf")

                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                    fontFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val typeface = Typeface.createFromFile(fontFile)
                val newFontFamily = FontFamily(typeface)

                cachedUri = uriString
                cachedFontFamily = newFontFamily

                newFontFamily
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to default if anything goes wrong (e.g., file deleted, permission revoked)
                JetBrainsMonoFontFamily
            }
        }
    }

    fun clearCache() {
        cachedUri = null
        cachedFontFamily = null
    }
}
