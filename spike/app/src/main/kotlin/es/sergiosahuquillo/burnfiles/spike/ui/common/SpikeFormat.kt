package es.sergiosahuquillo.burnfiles.spike.ui.common

import android.content.Context
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Sizes, numbers and dates formatted for the active locale (constitution V). */
class SpikeFormat(private val context: Context, private val locale: Locale) {
    private val integer = NumberFormat.getIntegerInstance(locale)
    private val decimal =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
    private val dateTime =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())

    fun bytes(value: Long): String = Formatter.formatShortFileSize(context, value)

    fun count(value: Long): String = integer.format(value)

    fun seconds(millis: Long): String = decimal.format(millis / MILLIS)

    fun decimal(value: Double): String = decimal.format(value)

    fun dateTime(instant: Instant): String = dateTime.format(instant)

    private companion object {
        const val MILLIS = 1000.0
    }
}

@Composable
fun rememberFormat(): SpikeFormat {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(context, locale) { SpikeFormat(context, locale) }
}
