import 'package:cast/route_props.dart';

class ConnectedEvent {
  String eventData;
  ConnectedEvent(this.eventData);
}

class RouteChangedEvent {
  Map<String, RouteProps> routes;
  RouteChangedEvent(this.routes);
}
