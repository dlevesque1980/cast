import 'package:flutter/material.dart';
import 'dart:async';
import 'package:cast_example/receiverConst.dart';
import 'package:cast/cast.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Map<dynamic, dynamic> _chromecastDevices = Map<dynamic, dynamic>();

  @override
  void initState() {
    super.initState();
    initChromecast();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  initChromecast() {
    Cast.initChromecast(app_id).then((message) {
      print(message);
    });
  }

  Future<dynamic> getRoutes() async {
    var mapRoutes = await Cast.getRoutes();
    setState(() {
      _chromecastDevices = mapRoutes;
    });
  }

  Future<dynamic> selectRoute(String route) async {
    final String castId = _chromecastDevices[route];
    final String response = await Cast.selectRoute(castId);
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
            Center(child: RaisedButton(onPressed: () => getRoutes(), child: Text("Chromecast"))),
            Text(""),
            SizedBox(height: 200.0, child: buildListView()),
          ],
        ),
      ),
    );
  }
}
