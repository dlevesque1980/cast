import 'dart:ui';

import 'package:cast/events.dart';
import 'package:cast/route_props.dart';
import 'package:cast_example/video.dart';
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

  List<Video> _videos = [
    Video("Sintel", "https://media.w3.org/2010/05/sintel/trailer.mp4", "https://media.w3.org/2010/05/sintel/poster.png", "video/mp4", false, false),
    Video("Bunny", "https://player.mediaspip.net/IMG/webm/big_buck_bunny_1080p_surround-encoded.webm?1382666749",
        "https://peach.blender.org/wp-content/uploads/title_anouncement.jpg?x11217", "video/webm", false, false)
  ];

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
      print("connected state: " + e.stateData.toString());
      setState(() {
        _isConnected = e.stateData;
      });
    });
  }

  Future<dynamic> disconnect() async {
    var result = await _cast.unselectRoute();
    for (var video in _videos) {
      video.isPlaying = false;
      video.playActive = false;
    }
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
          body: ListView(
            padding: const EdgeInsets.only(top: 16.0, bottom: 16.0),
            children: buildStack(),
          )),
    );
  }

  List<Widget> buildStack() {
    return _videos
        .map((video) => Stack(children: <Widget>[
              ListTile(
                leading: Container(child: Image.network(video.poster), width: 100.0, height: 100.0),
                isThreeLine: false,
                title: Text(video.title),
                trailing: IconButton(
                    icon: Icon(video.isPlaying ? Icons.pause : Icons.play_arrow),
                    onPressed: () {
                      if (!video.playActive && !video.isPlaying) {
                        _cast.play(video.url, video.mimeType).then((message) => print(message));
                        video.playActive = true;
                      } else {
                        video.isPlaying ? _cast.pause() : _cast.resume();
                      }

                      setState(() {
                        video.isPlaying = !video.isPlaying;
                      });
                    }),
              ),
            ]))
        .toList();
  }
}
