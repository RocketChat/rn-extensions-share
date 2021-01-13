package chat.rocket.rnshareextension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Parcelable
import com.facebook.react.bridge.*
import java.io.File

class ShareModule(reactContext: ReactApplicationContext?) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "ReactNativeShareExtension"

    @ReactMethod
    fun close() = currentActivity?.finish()

    @ReactMethod
    fun data(promise: Promise) = promise.resolve(processIntent(currentActivity))

    private fun processIntent(activity: Activity?): WritableArray {

        val items = Arguments.createArray()
        val currentActivity = activity ?: return items

        val intent = activity.intent

        val result = when {
            intent.action == ACTION_SEND && intent.isTypeOf("text/plain") -> actionSendText(intent)
            intent.action == ACTION_SEND && intent.isTypeOf("image/") -> actionSendImage(intent, currentActivity)
            intent.action == ACTION_SEND_MULTIPLE && intent.isTypeOf("image/") -> actionSendMultiple(intent, currentActivity)
            else -> emptyList()
        }

        result.map {
            items.pushMap(it)
        }

        return items
    }

    private fun actionSendMultiple(intent: Intent, activity: Activity): List<WritableMap> {

        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) as? List<Uri>
                ?: emptyList()

        return uris.mapNotNull {
            createImageFilePathArgumentsMap(it, activity)
        }
    }

    private fun actionSendImage(intent: Intent, activity: Activity): List<WritableMap> {

        val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                ?: return emptyList()

        return createImageFilePathArgumentsMap(uri, activity)?.let { listOf(it) } ?: emptyList()
    }

    private fun actionSendText(intent: Intent): List<WritableMap> {

        return intent.getStringExtra(Intent.EXTRA_TEXT)?.let { listOf(it.createMap("text")) }
                ?: return emptyList()
    }

    private fun createImageFilePathArgumentsMap(uri: Uri, activity: Activity): WritableMap? {

        return runCatching { createPrivateCopy(activity, uri) }
                .getOrDefault(null)
                ?.createMap("media")
    }

    private fun storeImage(cacheDir: File, bitmap: Bitmap): String {

        return File(cacheDir, "share-${System.currentTimeMillis()}.jpg").apply {
            writeBitmap(bitmap, Bitmap.CompressFormat.JPEG, 85)
        }.absolutePath
    }

    private fun createPrivateCopy(context: Context, uri: Uri): String? {

        return context.contentResolver.openInputStream(uri).use {
            val bitmap = BitmapFactory.decodeStream(it)
            storeImage(context.cacheDir, bitmap)
        }
    }
}

private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}

private fun String.createMap(type: String): WritableMap {

    return Arguments.createMap().apply {
        putString("value", this@createMap)
        putString("type", type)
    }
}

private fun Intent.isTypeOf(typePrefix: String): Boolean {
    return type?.startsWith(typePrefix) == true
}