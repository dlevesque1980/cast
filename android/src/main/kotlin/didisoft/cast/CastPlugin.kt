package didisoft.cast

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.media.*
import android.support.v7.media.MediaRouter.UNSELECT_REASON_STOPPED
import android.support.v7.media.RemotePlaybackClient.SessionActionCallback
import android.util.Log
import com.google.android.gms.cast.CastMediaControlIntent
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

internal var TAG = "FlutterCast"

class CastPlugin(private val activity: Activity, private val channel: MethodChannel) : MethodCallHandler {
    private var _playbackClient: RemotePlaybackClient? = null
    private var _mediaRouter: MediaRouter = MediaRouter.getInstance(activity.applicationContext)
    private val _mediaRouterCallback = DidisoftMediaRouterCallback()
    private var _mediaRouteSelector: MediaRouteSelector? = null
    private var _multicastLock: WifiManager.MulticastLock? = null
    private var _jmdns: JmDNS? = null

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
                CastOptionsProvider.AppId = appId
                CastOptionsProvider.activity = activity
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

            call.method.equals("testCall") -> {
                val sessionId: String = call.argument("sessionId")!!
                val routeId: String = call.argument("routeId")!!
                testCall(sessionId, routeId, result)
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

    private fun initChromecast(result: Result, app_id: String) = runBlocking<Unit> {
        if (_mediaRouteSelector == null) {
            _mediaRouteSelector = MediaRouteSelector.Builder()
                    .addControlCategory(CastMediaControlIntent.categoryForCast(app_id))
                    .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    //.addSelector(_castContext?.mergedSelector!!)
                    .build()
        } else {
            _mediaRouter.removeCallback(_mediaRouterCallback)
        }

        launch(Dispatchers.Default) { // will get dispatched to DefaultDispatcher
            try {

                Log.i(TAG, "Starting Mutlicast Lock...");
                val wifi = activity.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager

                _multicastLock = wifi?.createMulticastLock(activity.javaClass.name)
                _multicastLock?.setReferenceCounted(true)
                _multicastLock?.acquire()
                Log.i(TAG, "Starting ZeroConf probe....")

                val addr = getLocalIpAddress()
                val hostName = addr?.hostName
                _jmdns = JmDNS.create(addr, hostName)
                _jmdns?.addServiceListener("_googlecast._tcp.local.", SampleListerner())
            } catch (e: IOException) {
                Log.d(TAG, e.message)
            }
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

        newRoutes.associateByTo(mapped, { it.name }, { jsonAdapter.toJson(RouteProps(it.connectionState, it.id, null)) })

        result.success(mapped)
    }

    private fun selectRoute(result: Result, castId: String) {
        Log.d(TAG, "selectRoute: $castId")
        val item = _mediaRouter.routes.firstOrNull { it.id == castId }
        //val item = _mediaRouter.routes.firstOrNull { it.id.contains(castId) }
        if (item != null) {
            item.select()
            result.success(item.id)
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

    private fun getLocalIpAddress(): InetAddress? {
        val wifiManager = activity.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager!!.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        var address: InetAddress? = null
        try {
            address = InetAddress.getByName(String.format(Locale.ENGLISH,
                    "%d.%d.%d.%d", ipAddress and 0xff, ipAddress shr 8 and 0xff, ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff))
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }

        return address
    }

    private fun testCall(sessionId: String, routeId: String, result: Result) = runBlocking<Unit> {
        // val route = _mediaRouter.routes.firstOrNull { it.id == routeId }


        result.success("control request success")
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

        if (metadata != null) {
            for ((key, value) in metadata) {
                bundle.putString(key, value)
            }
        }

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
        _jmdns?.unregisterAllServices()
        result.success("callback removed")
    }

    private fun getRouteArgument(route: MediaRouter.RouteInfo): HashMap<String, String> {
        val arguments = HashMap<String, String>()
        val name: String = route.name
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val jsonAdapter = moshi.adapter(RouteProps::class.java)

        arguments[name] = jsonAdapter.toJson(RouteProps(route.connectionState, route.id, null))
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

    private inner class SampleListerner : ServiceListener {
        override fun serviceAdded(event: ServiceEvent?) {
            Log.d(TAG, "serviceAdded: $event")

        }

        override fun serviceRemoved(event: ServiceEvent?) {
            Log.d(TAG, "serviceRemoved: $event")
        }

        override fun serviceResolved(event: ServiceEvent?) {
            Log.d(TAG, "serviceResolved: $event")
            val arguments = HashMap<String, String>()
            val fn = event?.info?.getPropertyString("fn")
            val id = event?.info?.getPropertyString("id")
            val address = event?.info?.hostAddresses?.first()
            val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
            val jsonAdapter = moshi.adapter(RouteProps::class.java)

            if (!fn.isNullOrEmpty()) {
                arguments[fn] = jsonAdapter.toJson(RouteProps(0, id.orEmpty(),address))
                channel.invokeMethod("castListAdd", arguments)
            }

        }

    }

}
