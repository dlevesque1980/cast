package didisoft.cast

import android.app.Activity
import android.app.Presentation
import android.content.ContentValues.TAG
import android.os.Build
import android.support.v7.media.MediaRouteSelector
import android.support.v7.media.MediaRouter
import android.util.Log
import android.widget.Toast
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.CastMediaControlIntent
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import android.media.MediaRouter.ROUTE_TYPE_LIVE_VIDEO
import java.util.stream.Collectors
import android.support.v7.media.MediaControlIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.media.MediaItemStatus




class CastPlugin(private val activity: Activity, private val channel: MethodChannel) : MethodCallHandler {
  private val mRouteInfo = ArrayList<MediaRouter.RouteInfo>()
  private var mSelectedDevice: CastDevice? = null
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
        call.method.equals("dispose") -> disposeChromecast(result)
        call.method.equals("getRoutes") -> getRoutes(result)
        else -> result.notImplemented()
    }
  }

  private fun initChromecast(result: Result, app_id: String) {
      val context = activity.applicationContext
      _mediaRouter = MediaRouter.getInstance(context)
      val mediaRouteSelector = MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(app_id)).build()
      _mediaRouter.addCallback(mediaRouteSelector, _mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
      result.success("chromecast initalized!")
  }

    private fun selectRoute(result: Result, castId: String) {
        val item = _mediaRouter.routes.firstOrNull { it.id == castId }
        if (item != null) {
            _mediaRouter.selectRoute(item)

            if (Build.VERSION.SDK_INT >= 17) {
                val selectedRoute = _mediaRouter.selectedRoute
                //val castDevice = CastDevice.getFromBundle(selectedRoute.extras)
                val intent = Intent(MediaControlIntent.ACTION_PLAY)
                intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                intent.setDataAndType(Uri.parse("https://www.w3schools.com/html/mov_bbb.mp4"), "video/mp4")
                selectedRoute.supp
                if (selectedRoute.supportsControlRequest(intent)) {
                    val callback = object : MediaRouter.ControlRequestCallback() {
                        override fun onResult(data: Bundle) {
                            // The request succeeded.
                            // Playback may be controlled using the returned session and item id.
                            val sessionId = data.getString(MediaControlIntent.EXTRA_SESSION_ID)
                            val itemId = data.getString(MediaControlIntent.EXTRA_ITEM_ID)
                            val status = MediaItemStatus.fromBundle(data.getBundle(MediaControlIntent.EXTRA_ITEM_STATUS))
                            // ...
                        }

                        override fun onError(message: String?, data: Bundle?) {
                            // An error occurred!
                        }
                    }
                    selectedRoute.sendControlRequest(intent, callback)
//                    if (display != null) {
//                        val pres = Presentation(activity.applicationContext, display)
//                        pres.show()
//                    }
                }
            }

            result.success("route selected")
            return
        }
        result.error("Invalid","invalid cast id","")
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
        Log.d(TAG, "onRouteAdded: info=" + info!!)
        val arguments = HashMap<String, MediaRouter.RouteInfo?>()
        arguments["routeInfo"] = info
        channel.invokeMethod("castListAdd", arguments)
    }

    override fun onRouteRemoved(router: MediaRouter?, info: MediaRouter.RouteInfo?) {
        Log.d(TAG, "onRouteRemoved: info=" + info!!)
        val arguments = HashMap<String, MediaRouter.RouteInfo?>()
        arguments["routeInfo"] = info
        // Remove route from list of routes
        channel.invokeMethod("castListRemove", arguments)
    }


    override fun onRouteSelected(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
        Log.d(TAG, "onRouteSelected: info=$route")

        this@CastPlugin.mSelectedDevice = CastDevice.getFromBundle(activity.intent.extras)

        // Just display a message for now; In a real app this would be the
        // hook to connect to the selected device and launch the receiver
        // app
        Toast.makeText(activity.applicationContext, "connection", Toast.LENGTH_LONG).show()
    }

    override fun onRouteUnselected(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
        Log.d(TAG, "onRouteUnselected: info=$route")
        this@CastPlugin.mSelectedDevice = null
    }
  }

}
