package didisoft.cast

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.media.*
import android.support.v7.media.MediaRouter.UNSELECT_REASON_STOPPED
import android.support.v7.media.RemotePlaybackClient.SessionActionCallback
import android.support.v7.media.MediaItemMetadata
import android.util.Log
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.*

internal var TAG = "FlutterCast"

class CastPlugin(private val activity: Activity, private val channel: MethodChannel) : MethodCallHandler {
    private var _playbackClient: RemotePlaybackClient? = null
    private var _mediaRouter: MediaRouter = MediaRouter.getInstance(activity.applicationContext)
    private val _mediaRouterCallback = DidisoftMediaRouterCallback()
    private var _mediaRouteSelector: MediaRouteSelector? = null



    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            val channel = MethodChannel(registrar.messenger(), "didisoft.cast")
            val activity = registrar.activity()
            channel.setMethodCallHandler(CastPlugin(activity, channel))
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        when {
            call.method.equals("init") -> {
                val appId: String = call.argument("appId")!!
                initChromecast(result, appId)
            }
            call.method.equals("select") -> {
                val castId: String = call.argument("castId")!!
                selectRoute(result, castId)
            }

            call.method.equals("getPlayerStatus") -> {
                val itemId: String = call.argument("itemId")!!
                getPlayerStatus(itemId, result)
            }

            call.method.equals("unselect") -> unSelectRoute(result)

            call.method.equals("play") -> {
                val url: String = call.argument("url")!!
                val mimeType: String = call.argument("mimeType")!!
                val metadata: Map<String, String>? = call.argument("metadata")
                val position: Long = call.argument<Long>("position")!!
                play(result, url, mimeType, metadata, position)
            }
            call.method.equals("pause") -> pause(result)
            call.method.equals("resume") -> resume(result)
            call.method.equals("dispose") -> disposeChromecast(result)
            call.method.equals("getRoutes") -> getRoutes(result)
            else -> result.notImplemented()
        }
    }

    private fun initChromecast(result: Result, app_id: String) {

        if (_mediaRouteSelector == null) {
            _mediaRouteSelector = MediaRouteSelector.Builder()
                    .addControlCategory(CastMediaControlIntent.categoryForCast(app_id))
                    .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .build()
        } else {
            _mediaRouter.removeCallback(_mediaRouterCallback)
        }

        _mediaRouter.addCallback(_mediaRouteSelector!!, _mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)

        result.success("chromecast initalized!")

    }

    private fun getRoutes(result: Result) {
        Log.d(TAG, "getRoutes")
        val routes = _mediaRouter.routes
        val isGreaterThanZero: (Int, MediaRouter.RouteInfo?) -> Boolean = { i, _ -> i > 0 }
        val newRoutes = routes.filterIndexed(isGreaterThanZero)

        val mapped = mutableMapOf<String, String>()

        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val jsonAdapter = moshi.adapter(RouteProps::class.java)

        newRoutes.associateByTo(mapped, { it.name }, { jsonAdapter.toJson(RouteProps(it.connectionState, it.id)) })

        result.success(mapped)
    }

    private fun selectRoute(result: Result, castId: String) {
        Log.d(TAG, "selectRoute: $castId")
        val item = _mediaRouter.routes.firstOrNull { it.id == castId }
        if (item != null) {
            _mediaRouter.selectRoute(item)

            result.success("route selected")
            return
        }

        result.error("Invalid", "invalid cast id", "")
    }

    private fun unSelectRoute(result: Result) {
        Log.d(TAG, "unSelectRoute")

        _playbackClient?.endSession(activity.intent.extras, object : SessionActionCallback() {
            override fun onResult(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?) {
                Log.d(TAG, "Unselected OnResult= $sessionId")
                super.onResult(data, sessionId, sessionStatus)
                _mediaRouter.unselect(UNSELECT_REASON_STOPPED)
            }

            override fun onError(error: String?, code: Int, data: Bundle?) {
                Log.d(TAG, "Unselected OnError= $error, $code, $data")
                super.onError(error, code, data)
            }
        })
        result.success("route unselected")
    }

    private fun getPlayerStatus(itemId: String, result: Result) {

        if (_playbackClient?.hasSession()!!) {
            _playbackClient?.getStatus(itemId, activity.intent.extras, object : RemotePlaybackClient.ItemActionCallback() {
                override fun onResult(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?, itemId: String?, itemStatus: MediaItemStatus?) {
                    Log.d(TAG, "getStatus onResult: $sessionId, $sessionStatus, $itemId, $itemStatus")
                    super.onResult(data, sessionId, sessionStatus, itemId, itemStatus)
                }

                override fun onError(error: String?, code: Int, data: Bundle?) {
                    Log.d(TAG, "getStatus onError: $error, $code, $data")
                    super.onError(error, code, data)
                }
            })
        }

        result.success("control request success")
    }

    private fun play(result: Result, url: String, mimeType: String, metadata: Map<String, String>?, position: Long) {
        val bundle = Bundle()
        metadata?.forEach { k, v -> bundle.putString(k, v) }
        _playbackClient?.play(Uri.parse(url), mimeType, bundle, position, null, object : RemotePlaybackClient.ItemActionCallback() {
            override fun onResult(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?, itemId: String?, itemStatus: MediaItemStatus?) {
                Log.d(TAG, "ItemPlayback OnResult= $data, $sessionId, $sessionStatus")
                channel.invokeMethod("castMediaPlaying", itemId)
                super.onResult(data, sessionId, sessionStatus, itemId, itemStatus)
            }

            override fun onError(error: String?, code: Int, data: Bundle?) {
                Log.d(TAG, "ItemPlayback OnError= $error")
                super.onError(error, code, data)
            }
        })

        result.success("Play started")
    }

    private fun pause(result: Result) {

        _playbackClient?.pause(activity.intent.extras, object : RemotePlaybackClient.SessionActionCallback() {
            override fun onResult(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?) {
                Log.d(TAG, "sessionPlayback pause OnResult= $data, $sessionId, $sessionStatus")
                channel.invokeMethod("castMediaPaused", null)
                super.onResult(data, sessionId, sessionStatus)
            }

            override fun onError(error: String?, code: Int, data: Bundle?) {
                Log.d(TAG, "sessionPlayback pause OnError= $error")
                super.onError(error, code, data)
            }
        })

        result.success("Pause started")
    }

    private fun resume(result: Result) {

        _playbackClient?.resume(activity.intent.extras, object : RemotePlaybackClient.SessionActionCallback() {
            override fun onResult(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?) {
                Log.d(TAG, "sessionPlayback pause OnResult= $data, $sessionId, $sessionStatus")
                channel.invokeMethod("castMediaResumed", null)
                super.onResult(data, sessionId, sessionStatus)
            }

            override fun onError(error: String?, code: Int, data: Bundle?) {
                Log.d(TAG, "sessionPlayback pause OnError= $error")
                super.onError(error, code, data)
            }
        })

        result.success("Resume started")
    }

    private fun disposeChromecast(result: Result) {
        _mediaRouter.removeCallback(_mediaRouterCallback)
        result.success("callback removed")
    }

    private fun getRouteArgument(route: MediaRouter.RouteInfo): HashMap<String, String> {
        val arguments = HashMap<String, String>()
        val name: String = route.name
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val jsonAdapter = moshi.adapter(RouteProps::class.java)

        arguments[name] = jsonAdapter.toJson(RouteProps(route.connectionState, route.id))
        return arguments
    }

    private inner class DidisoftMediaRouterCallback : MediaRouter.Callback() {

        override fun onRouteAdded(router: MediaRouter?, info: MediaRouter.RouteInfo?) {
            Log.d(TAG, "onRouteAdded: info=$info!!")
            val arguments = getRouteArgument(info!!)
            channel.invokeMethod("castListAdd", arguments)
        }

        override fun onRouteRemoved(router: MediaRouter?, info: MediaRouter.RouteInfo?) {
            Log.d(TAG, "onRouteRemoved: info=$info!!")
            val arguments = getRouteArgument(info!!)
            channel.invokeMethod("castListRemove", arguments)
        }

        override fun onRouteSelected(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
            Log.d(TAG, "onRouteSelected: info=$route")

            _playbackClient = RemotePlaybackClient(activity, route)

            _playbackClient?.setStatusCallback(object: RemotePlaybackClient.StatusCallback() {
                override fun onSessionChanged(sessionId: String?) {
                    Log.d(TAG, "StatusCallback onSessionChanged: $sessionId")
                    channel.invokeMethod("castSessionChanged", sessionId)
                    super.onSessionChanged(sessionId)
                }

                override fun onSessionStatusChanged(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?) {
                    Log.d(TAG, "StatusCallback onSessionStatusChanged: $data, $sessionId, $sessionStatus")
                    val arguments = HashMap<String, String>()
                    arguments["sessionId"] = sessionId ?: ""
                    arguments["mediaSessionState"] = sessionStatus?.sessionState.toString()
                    arguments["mediaSessionTimeStamps"] = sessionStatus?.timestamp.toString()
                    channel.invokeMethod("castSessionStatusChanged", arguments)
                    super.onSessionStatusChanged(data, sessionId, sessionStatus)
                }

                override fun onItemStatusChanged(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?, itemId: String?, itemStatus: MediaItemStatus?) {
                    Log.d(TAG, "StatusCallback onItemStatusChanged: $data, $sessionId, $sessionStatus, $itemId, $itemStatus")
                    val arguments = HashMap<String, String>()
                    arguments["sessionId"] = sessionId ?: ""
                    arguments["sessionState"] = sessionStatus?.sessionState.toString()
                    arguments["mediaSessionTimeStamps"] = sessionStatus?.timestamp.toString()
                    arguments["itemId"] = itemId ?: ""
                    arguments["itemStatus"] =  itemStatus?.playbackState.toString()
                    arguments["itemDuration"] =  itemStatus?.contentDuration.toString()
                    arguments["itemPosition"] =  itemStatus?.contentPosition.toString()


                    channel.invokeMethod("castItemChanged", arguments)
                    super.onItemStatusChanged(data, sessionId, sessionStatus, itemId, itemStatus)
                }
            })


            val sessionActionCallback = object : SessionActionCallback() {
                override fun onResult(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?) {
                    super.onResult(data, sessionId, sessionStatus)
                    Log.d(TAG, "SessionActionCallback OnResult= $data,$sessionId, $sessionStatus")
                    val arguments = HashMap<String, String>()
                    arguments["sessionId"] = sessionId ?: ""
                    channel.invokeMethod("castConnected", arguments)
                }

                override fun onError(error: String?, code: Int, data: Bundle?) {
                    Log.d(TAG, "OnError= $error, $code, $data")
                    super.onError(error, code, data)
                }
            }

            activity.intent.putExtra(CastMediaControlIntent.EXTRA_CAST_RELAUNCH_APPLICATION, false)
            activity.intent.putExtra(CastMediaControlIntent.EXTRA_CAST_STOP_APPLICATION_WHEN_SESSION_ENDS, false)
            activity.intent.putExtra(CastMediaControlIntent.EXTRA_DEBUG_LOGGING_ENABLED, true)
            activity.intent.putExtra(MediaControlIntent.EXTRA_ITEM_STATUS, true)
            activity.intent.putExtra(MediaControlIntent.EXTRA_SESSION_STATUS, true)
            _playbackClient?.startSession(this@CastPlugin.activity.intent.extras, sessionActionCallback)



        }

        override fun onRouteUnselected(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
            Log.d(TAG, "onRouteUnselected: info=$route")
            channel.invokeMethod("castDisconnected", null)
        }
    }

    private inner class StatusListener: RemoteMediaClient.ProgressListener {
        override fun onProgressUpdated(p0: Long, p1: Long) {
            Log.d(TAG, "OnProgressUpdated: $p0, $p1")
        }
    }
}
