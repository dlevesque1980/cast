import 'package:cast/route_props.dart';

class ConnectedEvent {
  bool stateData;
  String sessionId;
  ConnectedEvent(this.stateData, this.sessionId);
}

class RouteChangedEvent {
  Map<String, RouteProps> routes;
  RouteChangedEvent(this.routes);
}

enum PlayingState { Playing, Paused, Resumed }

class MediaPlayingEvent {
  String itemId;
  bool isPlaying;
  PlayingState state;
  MediaPlayingEvent(this.isPlaying, this.itemId, this.state);
}

class SessionStatusEvent {
  String sessionId;
  int mediaSessionState;
  String mediaTimeStamps;
  SessionStatusEvent(this.sessionId, this.mediaSessionState, this.mediaTimeStamps);
}

class ItemStatusEvent {
  String sessionId;
  int sessionState;
  String timeStamps;
  String itemId;
  int itemStatus;
  int itemDuration;
  int itemPosition;
  ItemStatusEvent(this.sessionId, this.sessionState, this.timeStamps, this.itemId, this.itemStatus, this.itemDuration, this.itemPosition);
}
