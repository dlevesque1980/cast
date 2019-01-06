import 'dart:async';

import 'package:async/async.dart';
import 'package:cast/cast.dart';
import 'package:cast/events.dart';
import 'package:cast_example/bloc/video.dart';
import 'package:cast_example/helper/prefs_helper.dart';

class VideoScreenBloc {
  Cast _cast = new Cast();
  List<Video> _videoState = [
    Video("Sintel", "https://media.w3.org/2010/05/sintel/trailer.mp4", "https://media.w3.org/2010/05/sintel/poster.png", "video/mp4", false, false, ""),
    Video("Bunny", "https://player.mediaspip.net/IMG/webm/big_buck_bunny_1080p_surround-encoded.webm?1382666749",
        "https://peach.blender.org/wp-content/uploads/title_anouncement.jpg?x11217", "video/webm", false, false, "")
  ];
  static VideoScreenBloc _instance;
  factory VideoScreenBloc() => _instance ??= VideoScreenBloc._();
  var _actionQueue = StreamController<Video>();
  Sink<Video> get actionQueue => _actionQueue.sink;
  Stream<List<Video>> videos;
  var _playState = StreamController<MediaPlayingEvent>();
  var _test = StreamController<String>();
  Sink<String> get test => _test.sink;

  VideoScreenBloc._() {
    var v1 = _actionQueue.stream.asyncMap(_actionVideo).asBroadcastStream();
    var v2 = _cast.onMediaPlaying.asyncMap(mediaUpdate).asBroadcastStream();
    var v3 = _cast.onItemStatusChanged.asyncMap(itemStatusChanged).asBroadcastStream();
    videos = StreamGroup.merge([v1, v2, v3]);

    _test.stream.listen(testCall);

    _cast.onSessionStatusChanged.listen(sessionStatusChanged);
  }

  Future<String> testCall(String something) async {
    var sessionId = await PrefHelper.getString("sessionId");
    var routeId = await PrefHelper.getString("routeId");
    var message = await _cast.testCall(sessionId, routeId);
    return message;
  }

  Future<List<Video>> itemStatusChanged(ItemStatusEvent event) async {
    if (event.itemStatus == 4) {
      var video = _videoState.firstWhere((v) => v.itemId == event.itemId);
      video.isPlaying = false;
      video.playActive = false;
    }

    return _videoState;
  }

  Future<void> sessionStatusChanged(SessionStatusEvent event) async {
    print("SessionStatusChanged from bloc: $event");
  }

  // TODO: need something more solid
  Future<List<Video>> mediaUpdate(MediaPlayingEvent event) async {
    var video = _videoState.firstWhere((v) => v.playActive);
    if (event.itemId != null) {
      video.itemId = event.itemId;
    }

    video.isPlaying = event.isPlaying;

    return _videoState;
/*
    _cast.onMediaPlaying.listen((e) async {
      var prefs = await _prefs;
      prefs.setString(itemId, e.itemId);
      _itemId = e.itemId;
    });
*/
  }

  Future<List<Video>> _actionVideo(Video video) async {
    if (!video.isPlaying && !video.playActive) {
      var message = await _cast.play(video.url, video.mimeType);
      video.playActive = true;
      print(message);
    } else {
      video.isPlaying ? _cast.pause() : _cast.resume();
    }

    return _videoState;
  }

  List<Video> initialData() {
    return _videoState;
  }

  void dispose() {
    _actionQueue.close();
    _playState.close();
    _test.close();
  }
}
