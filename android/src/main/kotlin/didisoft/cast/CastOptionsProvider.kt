package didisoft.cast

import android.util.Log
import android.app.Activity
import android.content.Context
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.*
import com.google.android.gms.common.images.WebImage
import java.util.*


class CastOptionsProvider : OptionsProvider {

    companion object {
        public var AppId: String = ""
        public var activity: Activity? = null
    }

    override fun getCastOptions(context: Context): CastOptions {
        Log.d(TAG, "AppId = " + AppId)
        val notificationOptions = NotificationOptions.Builder()
                .setActions(Arrays.asList(MediaIntentReceiver.ACTION_SKIP_NEXT,
                        MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                        MediaIntentReceiver.ACTION_STOP_CASTING), intArrayOf(1, 2))
                .setTargetActivityClassName(activity!!::class.java.name)
                .build()
        val mediaOptions = CastMediaOptions.Builder()
                .setImagePicker(ImagePickerImpl())
                .setNotificationOptions(notificationOptions)
                .setExpandedControllerActivityClassName(activity!!::class.java.name)
                .build()
        return CastOptions.Builder()
                .setReceiverApplicationId(AppId)
                .setResumeSavedSession(false)
                .setCastMediaOptions(mediaOptions)
                .build()

        /*return CastOptions.Builder()
                .setReceiverApplicationId(CastOptionsProvider.AppId)
                .setEnableReconnectionService(false)
                .build()*/
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }

    private class ImagePickerImpl : ImagePicker() {

        override fun onPickImage(p0: MediaMetadata?, p1: ImageHints): WebImage? {
            if (p0 == null || !p0.hasImages()) {
                return null
            }
            val images = p0.getImages()
            return if (images.size == 1) {
                images.get(0)
            } else {
                if (p1.type == ImagePicker.IMAGE_TYPE_MEDIA_ROUTE_CONTROLLER_DIALOG_BACKGROUND) {
                    images.get(0)
                } else {
                    images.get(1)
                }
            }
        }
    }
}
