import 'package:cast/events.dart';
import 'package:cast/route_props.dart';
import 'package:flutter/material.dart';
import 'dart:async';
import 'package:cast_example/receiverConst.dart';
import 'package:cast/cast.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  AppLifecycleState _lastLifecycleState;
  bool _isConnected = false;
  Cast _cast = new Cast();

  @override
  void initState() {
    super.initState();
    initChromecast();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused) {
      _cast.dispose().then((message) => print(message));
    } else if (state == AppLifecycleState.resumed) {
      initChromecast();
    }

    if (state != _lastLifecycleState) {
      setState(() {
        _lastLifecycleState = state;
      });
    }
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  initChromecast() {
    _cast.initChromecast(app_id).then((message) {
      print(message);
    });

    _cast.onConnection.listen((e) {
      print(e.eventData);
      setState(() {
        _isConnected = true;
      });
    });
  }

  Future<dynamic> disconnect() async {
    var result = await _cast.unselectRoute();
    setState(() {
      _isConnected = false;
    });
    print(result);
  }

  Future<dynamic> selectRoute(RouteProps route) async {
    final String castId = route.id;
    final String response = await _cast.selectRoute(castId);
    print(response);
  }

  Future<dynamic> play(String video) async {
    final String response = await _cast.play(video, "video/mp4");
    print(response);
  }

  Widget getConnectDialog(BuildContext context, Map<dynamic, RouteProps> routes) {
    return AlertDialog(
      title: Text('Cast to'),
      content: Container(
        width: MediaQuery.of(context).size.width * 0.9,
        height: 20.0 + 20.0 * routes.length,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Expanded(
              child: ListView(
                children: routes.keys
                    .map((route) => GestureDetector(
                          onTap: () {
                            selectRoute(routes[route]);
                            Navigator.pop(context);
                          },
                          child: ListTile(
                            title: Text(route),
                            leading: Icon(Icons.tv),
                          ),
                        ))
                    .toList(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget getDisconnectDialog(BuildContext context) {
    return AlertDialog(
        title: new Text("Devices"),
        content: RaisedButton(
            onPressed: (_isConnected)
                ? () {
                    disconnect();
                    Navigator.pop(context);
                  }
                : null,
            child: Text("Disconnect")));
  }

  Widget getIconButton(BuildContext context, AsyncSnapshot<RouteChangedEvent> snapshot) {
    if (snapshot.hasData) {
      return IconButton(
          icon: Icon(_isConnected ? Icons.cast_connected : Icons.cast),
          onPressed: () async {
            if (!_isConnected) {
              showDialog(context: context, builder: (_) => getConnectDialog(context, snapshot.data.routes));
              return;
            }

            showDialog(context: context, builder: (_) => getDisconnectDialog(context));
          });
    }

    return Container();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(title: const Text('Cast example app'), actions: <Widget>[
            // action button
            StreamBuilder(stream: _cast.routeChanged, builder: (BuildContext context, AsyncSnapshot<RouteChangedEvent> snapshot) => getIconButton(context, snapshot))
          ]

              // action button
              ),
          body: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Center(
                child: FlatButton(
                    child: Text("Play sintel"),
                    onPressed: () => _cast.play("https://media.w3.org/2010/05/sintel/trailer.mp4", "video/mp4").then((message) => print(message)))),
          )),
    );
  }
}
