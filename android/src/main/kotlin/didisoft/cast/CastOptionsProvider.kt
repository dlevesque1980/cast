package didisoft.cast

import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

import android.content.Context


class CastOptionsProvider : OptionsProvider {

    companion object {
        public var AppId: String = ""
    }

    override fun getCastOptions(context: Context): CastOptions {
        Log.d(TAG, "AppId = " + AppId)
        return CastOptions.Builder()
                .setReceiverApplicationId(CastOptionsProvider.AppId)
                .setEnableReconnectionService(false)
                .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
