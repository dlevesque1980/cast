import 'dart:async';

import 'package:flutter/services.dart';

class Cast {
  static const MethodChannel _channel = const MethodChannel('didisoft.cast');

  static Future initChromecast(appId) async {
    var message = await _channel.invokeMethod('init', {'appId': appId});
    return message;
  }

  static Future<Map> getRoutes() async {
    var routes = await _channel.invokeMethod('getRoutes');
    return routes;
  }

  static Future<String> selectRoute(String castId) async {
    var result = await _channel.invokeMethod('select', {'castId': castId});
    return result;
  }

  Cast._() {
    _channel.setMethodCallHandler(_callHandler);
  }

  dispose() {
    //await _channel.invokeMethod('dispose');
  }

  Future<Null> _callHandler(MethodCall call) async {
    switch (call.method) {
      case 'castListAdd':
        final dynamic routeInfo = call.arguments['routeInfo'];
        print(routeInfo);
        break;
      case 'castListRemove':
        final dynamic routeInfo = call.arguments['routeInfo'];
        print(routeInfo);
        break;
    }
  }
}
