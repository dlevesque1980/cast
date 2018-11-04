class RouteProps {
  String id;
  int connectionState;

  RouteProps.fromJson(Map json) {
    this.id = json["id"];
    this.connectionState = json["connectionState"];
  }
}
