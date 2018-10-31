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
  Map<dynamic, dynamic> _chromecastDevices = Map<dynamic, dynamic>();
  bool _readyToPlay = false;
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

    _cast.onChange.listen((e) {
      print(e.eventData);
      setState(() {
        _readyToPlay = true;
      });
      _cast.play("https://media.w3.org/2010/05/sintel/trailer.mp4", "video/mp4").then((message) => print(message));
    });
  }

  Future<dynamic> getRoutes() async {
    var mapRoutes = await _cast.getRoutes();
    setState(() {
      _chromecastDevices = mapRoutes;
    });
  }

  Future<dynamic> disconnect() async {
    var result = await _cast.unselectRoute();
    setState(() {
      _readyToPlay = false;
    });
    print(result);
  }

  Future<dynamic> selectRoute(String route) async {
    final String castId = _chromecastDevices[route];
    final String response = await _cast.selectRoute(castId);
    print(response);
  }

  Future<dynamic> play(String video) async {
    final String response = await _cast.play(video, "video/mp4");
    print(response);
  }

  Widget buildListView() {
    if (_chromecastDevices.isEmpty) return Text("Waiting for detection...");
    var listView = ListView(
      children: _chromecastDevices.keys
          .map((route) => GestureDetector(
                onTap: () => selectRoute(route),
                child: ListTile(
                  title: Text(route),
                  leading: Icon(Icons.cast),
                ),
              ))
          .toList(),
    );
    return listView;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: <Widget>[
            Row(
              children: <Widget>[
                Padding(padding: EdgeInsets.only(left: 8.0, right: 8.0), child: RaisedButton(onPressed: () => getRoutes(), child: Text("Chromecast"))),
                Expanded(child: Text("")),
                Padding(
                    padding: EdgeInsets.only(left: 8.0, right: 8.0),
                    child: RaisedButton(onPressed: (_readyToPlay) ? () => disconnect() : null, child: Text("Disconnect")))
              ],
            ),
            Text(""),
            SizedBox(height: 200.0, child: buildListView()),
          ],
        ),
      ),
    );
  }
}
