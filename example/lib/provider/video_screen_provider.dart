import 'package:cast_example/bloc/video_screen_bloc.dart';
import 'package:flutter/widgets.dart';

class VideoScreenProvider extends InheritedWidget {
  final VideoScreenBloc videoScreenBloc;
  VideoScreenProvider({Key key, VideoScreenBloc videoScreenBloc, Widget child})
      : this.videoScreenBloc = videoScreenBloc ?? VideoScreenBloc(),
        super(child: child, key: key);

  @override
  bool updateShouldNotify(InheritedWidget oldWidget) => true;

  static VideoScreenBloc of(BuildContext context) => (context.inheritFromWidgetOfExactType(VideoScreenProvider) as VideoScreenProvider).videoScreenBloc;
}
