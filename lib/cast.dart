import 'dart:async';

import 'package:cast/events.dart';
import 'package:flutter/services.dart';

class Cast {
  static Cast _instance;
  factory Cast() => _instance ??= new Cast._();
  static const MethodChannel _channel = const MethodChannel('didisoft.cast');
  var connectedController = new StreamController<ConnectedEvent>();
  Stream<ConnectedEvent> get onChange => connectedController.stream;

  Cast._() {
    _channel.setMethodCallHandler(_callHandler);
  }

  Future initChromecast(appId) async {
    var message = await _channel.invokeMethod('init', {'appId': appId});
    return message;
  }

  Future<Map> getRoutes() async {
    var routes = await _channel.invokeMethod('getRoutes');
    return routes;
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

  Future<String> dispose() async {
    var result = await _channel.invokeMethod('dispose');
    return result;
  }

  Future<Null> _callHandler(MethodCall call) async {
    switch (call.method) {
      case 'castListAdd':
        var test = call.arguments as Map;
        test.forEach((k, v) => print('$k: $v'));
        break;
      case 'castListRemove':
        final dynamic routeInfo = call.arguments['routeInfo'];
        print(routeInfo);
        break;
      case 'castConnected':
        connectedController.add(new ConnectedEvent('connected!'));
        break;
    }
  }
}
