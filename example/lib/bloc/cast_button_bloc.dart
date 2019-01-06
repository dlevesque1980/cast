import 'dart:async';

import 'package:cast/cast.dart';
import 'package:cast/events.dart';
import 'package:cast/route_props.dart';
import 'package:cast_example/helper/prefs_helper.dart';
import 'package:cast_example/receiverConst.dart';

class CastButtonBloc {
  static const String sessionId = "sessionId";
  static const String routeIdPrefName = "routeId";
  static const String itemId = "itemId";
  static CastButtonBloc _instance;

  factory CastButtonBloc() => _instance ??= CastButtonBloc._();

  bool _init = false;
  Cast _cast = new Cast();

  Stream<bool> _connection;
  Stream<RouteChangedEvent> get routeChanged => _cast.routeChanged;
  Stream<bool> get onConnection => _connection;

  var _disconnect = StreamController<bool>();
  Sink<bool> get disconnect => _disconnect.sink;
  var _connect = StreamController<RouteProps>();
  Sink<RouteProps> get connect => _connect.sink;

  RouteChangedEvent initialData() {
    if (!_init) {
      _cast.initChromecast(app_id).then((message) {
        print(message);
      });
      _init = true;
    }
    return RouteChangedEvent(null);
  }

  CastButtonBloc._() {
    _connection = _cast.onConnection.asyncMap(_updateSessionValues).asBroadcastStream();
    _disconnect.stream.listen(_disconnectCast);
    _connect.stream.listen(_selectRoute);
  }

  Future<dynamic> _selectRoute(RouteProps route) async {
    final String routeId = await _cast.selectRoute(route.id);
    await PrefHelper.setString(routeIdPrefName, routeId);
  }

  Future<bool> _disconnectCast(e) async {
    var result = await _cast.unselectRoute();

    print(result);

    return true;
  }

  Future<bool> _updateSessionValues(ConnectedEvent e) async {
    await PrefHelper.setString("sessionId", e.sessionId);
    return e.stateData;
  }

  void dispose() {
    _disconnect.close();
    _disconnect = null;
    _connect.close();
    _connect = null;
    _init = false;
    _cast.dispose().then((message) => print(message));
  }
}
