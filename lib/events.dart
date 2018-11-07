import 'package:cast/route_props.dart';

class ConnectedEvent {
  bool stateData;
  ConnectedEvent(this.stateData);
}

class RouteChangedEvent {
  Map<String, RouteProps> routes;
  RouteChangedEvent(this.routes);
}
