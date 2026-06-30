package cloud.scoreprof.app

import android.os.Build
import android.content.Intent
import cloud.scoreprof.app.data.getAppContext

import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat

class AndroidPlatform : Platform {
    private val context = getAppContext()
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override val version: Int = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode
        }
    } catch (e: Exception) { 14 }
    override val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    override val language: String get() = Locale.getDefault().language
    
    override fun setLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    override fun showSystemNotification(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "scoreprof_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ScoreProf Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "League invitations and match updates"
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setColor(0xFF1A0841.toInt()) // Brand purple
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun getResBitmap(context: Context, resId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, resId) ?: return null
        if (drawable is BitmapDrawable) return drawable.bitmap
        
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun shareText(text: String) {
        val context = getAppContext()
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, null).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getStringComparator(): Comparator<String> {
    val collator = java.text.Collator.getInstance()
    collator.strength = java.text.Collator.SECONDARY
    return Comparator { s1, s2 -> collator.compare(s1, s2) }
}
