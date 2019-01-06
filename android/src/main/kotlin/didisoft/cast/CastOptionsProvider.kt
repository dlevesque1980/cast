package didisoft.cast

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
        return CastOptions.Builder()
                .setReceiverApplicationId(CastOptionsProvider.AppId)
                //.setStopReceiverApplicationWhenEndingSession(true)
                //.setEnableReconnectionService(false)
                .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }

}
