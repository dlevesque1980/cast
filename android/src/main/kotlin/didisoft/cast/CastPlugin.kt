package didisoft.cast

import android.app.Activity
import android.content.ContentValues.TAG
import android.net.Uri
import android.os.Bundle
import android.support.v7.media.*
import android.support.v7.media.RemotePlaybackClient.SessionActionCallback
import android.util.Log
import com.google.android.gms.cast.CastMediaControlIntent
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class CastPlugin(private val activity: Activity, private val channel: MethodChannel) : MethodCallHandler {
    private var _playbackClient: RemotePlaybackClient? = null
    private var _mediaRouter: MediaRouter = MediaRouter.getInstance(activity.applicationContext)
    private val _mediaRouterCallback = DidisoftMediaRouterCallback()


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
            val appId: String = call.argument("appId")
            initChromecast(result, appId)
        }
        call.method.equals("select") -> {
            val castId: String = call.argument("castId")
            selectRoute(result, castId)
        }

        call.method.equals("unselect") -> unSelectRoute(result)

        call.method.equals("play") -> {
            val url: String = call.argument("url")
            val mimeType: String = call.argument("mimeType")
            play(result, url, mimeType)
        }
        call.method.equals("dispose") -> disposeChromecast(result)
        call.method.equals("getRoutes") -> getRoutes(result)
        else -> result.notImplemented()
    }
  }

  private fun initChromecast(result: Result, app_id: String) {
      val context = activity.applicationContext
      _mediaRouter = MediaRouter.getInstance(context)
      //val new_app_id = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
      val mediaRouteSelector = MediaRouteSelector.Builder()
                                                    .addControlCategory(CastMediaControlIntent.categoryForCast(app_id))
                                                    .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                                                    .build()
      _mediaRouter.addCallback(mediaRouteSelector, _mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
      result.success("chromecast initalized!")
  }

    private fun selectRoute(result: Result, castId: String) {
        val item = _mediaRouter.routes.firstOrNull { it.id == castId }
        if (item != null) {
            _mediaRouter.selectRoute(item)

            result.success("route selected")
            return
        }
        result.error("Invalid","invalid cast id","")
    }

    private fun unSelectRoute(result: Result) {
        _mediaRouter.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED)

        result.success("route unselected")
    }

    private fun play(result: Result, url: String, mimeType: String) {
        _playbackClient?.play(Uri.parse(url), mimeType, null, 0, null, object: RemotePlaybackClient.ItemActionCallback() {
            override fun onResult(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?, itemId: String?, itemStatus: MediaItemStatus?) {
                Log.d(TAG,"ItemPlayback OnResult= $sessionStatus")
                super.onResult(data, sessionId, sessionStatus, itemId, itemStatus)
            }

            override fun onError(error: String?, code: Int, data: Bundle?) {
                Log.d(TAG,"ItemPlayback OnError= $error")
                super.onError(error, code, data)
            }
        })

        result.success("Play started")
    }

  private fun disposeChromecast(result: Result) {
      _mediaRouter.removeCallback(_mediaRouterCallback)
      result.success("callback removed")
  }

  private fun getRoutes(result: Result){
      val routes = _mediaRouter.routes
      val isGreaterThanZero: (Int, MediaRouter.RouteInfo?) -> Boolean = { i, _ -> i > 0 }
      val newRoutes = routes.filterIndexed(isGreaterThanZero)
      val mapped = newRoutes.associateBy({it.name},{it.id}).toMap()

      result.success(mapped)
  }

  private inner class DidisoftMediaRouterCallback : MediaRouter.Callback() {


    override fun onRouteAdded(router: MediaRouter?, info: MediaRouter.RouteInfo?) {
        Log.d(TAG, "onRouteAdded: info=$info!!")
        val arguments = HashMap<String, String>()
        val name: String = info!!.name
        arguments[name] = info.id
        channel.invokeMethod("castListAdd", arguments)
    }

    override fun onRouteRemoved(router: MediaRouter?, info: MediaRouter.RouteInfo?) {
        Log.d(TAG, "onRouteRemoved: info=$info!!")
        val arguments = HashMap<String, String>()
        val name: String = info!!.name
        arguments[name] = info.id
        channel.invokeMethod("castListRemove", arguments)
    }


    override fun onRouteSelected(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
        Log.d(TAG, "onRouteSelected: info=$route")

        _playbackClient = RemotePlaybackClient(this@CastPlugin.activity, route)
        val sessionActionCallback = object: SessionActionCallback() {
            override fun onResult(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?) {
                super.onResult(data, sessionId, sessionStatus)
                Log.d(TAG,"OnResult= $sessionStatus")
                val arguments = HashMap<String, String>()
                channel.invokeMethod("castConnected",null)


            }

            override fun onError(error: String?, code: Int, data: Bundle?) {
                Log.d(TAG,"OnError= $error")
                super.onError(error, code, data)
            }
        }
        _playbackClient?.startSession(this@CastPlugin.activity.intent.extras, sessionActionCallback)
    }

    override fun onRouteUnselected(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
        Log.d(TAG, "onRouteUnselected: info=$route")
        _playbackClient?.endSession(this@CastPlugin.activity.intent.extras,object: SessionActionCallback() {
            override fun onResult(data: Bundle?, sessionId: String?, sessionStatus: MediaSessionStatus?) {
                Log.d(TAG,"Unselected OnResult= $sessionId")
                super.onResult(data, sessionId, sessionStatus)
            }

            override fun onError(error: String?, code: Int, data: Bundle?) {
                Log.d(TAG,"Unselected OnError= $error")
                super.onError(error, code, data)
            }
        })
    }
  }

}
