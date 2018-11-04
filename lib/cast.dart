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

  Cast._() {
    _channel.setMethodCallHandler(_callHandler);
    _routes = Map<String, RouteProps>();
  }

  Future initChromecast(appId) async {
    var message = await _channel.invokeMethod('init', {'appId': appId});
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
        connectedController.add(new ConnectedEvent('connected!'));
        break;
    }
  }
}
