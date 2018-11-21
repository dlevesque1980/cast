import 'dart:async';
import 'dart:convert';

import 'package:cast/events.dart';
import 'package:cast/route_props.dart';
import 'package:flutter/services.dart';

class Cast {
  static Cast _instance;
  factory Cast() => _instance ??= Cast._();
  static const MethodChannel _channel = const MethodChannel('didisoft.cast');
  var connectedController = StreamController<ConnectedEvent>();
  Stream<ConnectedEvent> get onConnection => connectedController.stream;
  var routeChangedController = StreamController<RouteChangedEvent>();
  Stream<RouteChangedEvent> get routeChanged => routeChangedController.stream;
  Map<String, RouteProps> _routes;
  var mediaPlayingController = StreamController<MediaPlayingEvent>();
  Stream<MediaPlayingEvent> get onMediaPlaying => mediaPlayingController.stream;
  var _sessionStatusController = StreamController<SessionStatusEvent>();
  Stream<SessionStatusEvent> get onSessionStatusChanged => _sessionStatusController.stream;
  var _itemStatusController = StreamController<ItemStatusEvent>();
  Stream<ItemStatusEvent> get onItemStatusChanged => _itemStatusController.stream;

  Cast._() {
    _channel.setMethodCallHandler(_callHandler);
    _routes = Map<String, RouteProps>();
  }

  Future initChromecast(appId) async {
    var message = await _channel.invokeMethod('init', {'appId': appId});
    _setRoutes();
    return message;
  }

  Future getPlayerStatus(itemId) async {
    var message = await _channel.invokeMethod('getPlayerStatus', {'itemId': itemId});
    _setRoutes();
    return message;
  }

  Future _setRoutes() async {
    Map routes = await _channel.invokeMethod('getRoutes');
    _putRoutes(routes);
    routeChangedController.add(RouteChangedEvent(_routes));
  }

  void _putRoutes(Map<dynamic, dynamic> routes) {
    routes.forEach((k, v) {
      _routes.putIfAbsent(k, () => RouteProps.fromJson(jsonDecode(v)));
    });
  }

  void _removeRoutes(Map<dynamic, dynamic> routes) {
    routes.forEach((k, v) {
      _routes.remove(k);
    });
  }

  Future<String> selectRoute(String castId) async {
    var result = await _channel.invokeMethod('select', {'castId': castId});
    return result;
  }

  Future<String> unselectRoute() async {
    var result = await _channel.invokeMethod('unselect');
    return result;
  }

  Future<String> play(String url, String mimeType) async {
    var result = await _channel.invokeMethod('play', {'url': url, 'mimeType': mimeType});
    return result;
  }

  Future<String> pause() async {
    var result = await _channel.invokeMethod('pause');
    return result;
  }

  Future<String> resume() async {
    var result = await _channel.invokeMethod('resume');
    return result;
  }

  Future<String> dispose() async {
    var result = await _channel.invokeMethod('dispose');
    return result;
  }

  Future<Null> _callHandler(MethodCall call) async {
    switch (call.method) {
      case 'castListAdd':
        var routes = call.arguments as Map;
        _putRoutes(routes);
        routeChangedController.add(RouteChangedEvent(_routes));
        break;
      case 'castListRemove':
        var routes = call.arguments as Map;
        _removeRoutes(routes);
        routeChangedController.add(RouteChangedEvent(_routes));
        break;
      case 'castConnected':
        var mapped = call.arguments as Map;
        var sessionId = mapped["sessionId"] as String;
        connectedController.add(new ConnectedEvent(true, sessionId));
        break;
      case 'castDisconnected':
        connectedController.add(new ConnectedEvent(false, null));
        break;
      case 'castMediaPlaying':
        var itemId = call.arguments as String;
        mediaPlayingController.add(new MediaPlayingEvent(true, itemId, PlayingState.Playing));
        break;
      case 'castMediaPaused':
        mediaPlayingController.add(new MediaPlayingEvent(false, null, PlayingState.Paused));
        break;
      case 'castMediaResumed':
        mediaPlayingController.add(new MediaPlayingEvent(true, null, PlayingState.Resumed));
        break;
      case 'castItemChanged':
        var mapped = call.arguments as Map;
        var sessionId = mapped["sessionId"] as String;
        var sessionState = int.tryParse(mapped["sessionState"]);
        var timeStamps = mapped["mediaSessionTimeStamps"] as String;
        var itemId = mapped["itemId"] as String;
        var itemStatus = int.tryParse(mapped["itemStatus"]);
        var itemDuration = int.tryParse(mapped["itemDuration"]);
        var itemPosition = int.tryParse(mapped["itemPosition"]);
        _itemStatusController.add(ItemStatusEvent(sessionId, sessionState, timeStamps, itemId, itemStatus, itemDuration, itemPosition));
        break;
      /*case 'castSessionChanged':
        var mapped = call.arguments as Map;
        var sessionId = mapped["sessionId"];
        print(call.arguments);
        break;*/
      case 'castSessionStatusChanged':
        var mapped = call.arguments as Map;
        var sessionId = mapped["sessionId"] as String;
        var mediaSessionState = int.tryParse(mapped["mediaSessionState"]);
        var mediaTimeStamps = mapped["mediaSessionTimeStamps"] as String;
        _sessionStatusController.add(SessionStatusEvent(sessionId, mediaSessionState, mediaTimeStamps));
        break;
    }
  }
}
