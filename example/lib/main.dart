import 'package:cast_example/bloc/video_screen_bloc.dart';
import 'package:cast_example/provider/video_screen_provider.dart';
import 'package:cast_example/screen/video_screen.dart';
import 'package:flutter/material.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addObserver(this);
  }

  Widget videoScreenProvider() {
    return VideoScreenProvider(videoScreenBloc: VideoScreenBloc(), child: VideoScreen());
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      initialRoute: '/',
      routes: {
        // When we navigate to the "/" route, build the FirstScreen Widget
        '/': (context) => videoScreenProvider(),
      },
      title: 'Cast example plugin',
      theme: ThemeData(),
    );
  }
}
