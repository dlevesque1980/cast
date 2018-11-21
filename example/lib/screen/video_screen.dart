import 'package:cast_example/bloc/video.dart';
import 'package:cast_example/bloc/video_screen_bloc.dart';
import 'package:cast_example/provider/video_screen_provider.dart';
import 'package:cast_example/widget/cast_button.dart';
import 'package:flutter/material.dart';

class VideoScreen extends StatefulWidget {
  @override
  _VideoScreenState createState() => new _VideoScreenState();
}

class _VideoScreenState extends State<VideoScreen> {
  VideoScreenBloc _bloc;
  static const String itemId = "itemId";

  @override
  void initState() {
    super.initState();
  }

  @override
  void didChangeDependencies() {
    if (_bloc == null) {
      _bloc = VideoScreenProvider.of(context);
    }
    super.didChangeDependencies();
  }

  @override
  void dispose() {
    _bloc.dispose();
    super.dispose();
  }

  List<Stack> buildStack(BuildContext context, AsyncSnapshot<List<Video>> snapshot) {
    if (snapshot.hasData) {
      return snapshot.data
          .map((video) => Stack(children: <Widget>[
                ListTile(
                  leading: Container(child: Image.network(video.poster), width: 100.0, height: 100.0),
                  isThreeLine: false,
                  title: Text(video.title),
                  trailing: IconButton(
                      icon: Icon(video.isPlaying ? Icons.pause : Icons.play_arrow),
                      onPressed: () {
                        _bloc.actionQueue.add(video);
                      }),
                ),
              ]))
          .toList();
    }
    return List<Stack>();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(title: const Text('Cast example app'), actions: <Widget>[
          // action button
          CastButton(),
        ]

            // action button
            ),
        body: StreamBuilder(
          initialData: _bloc.initialData(),
          stream: _bloc.videos,
          builder: (BuildContext context, AsyncSnapshot<List<Video>> snapshot) =>
              ListView(padding: const EdgeInsets.only(top: 16.0, bottom: 16.0), children: buildStack(context, snapshot)),
        ));
  }
}
