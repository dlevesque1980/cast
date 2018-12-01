import 'package:cast_example/bloc/cast_button_bloc.dart';
import 'package:flutter/widgets.dart';

class CastProvider extends InheritedWidget {
  final CastButtonBloc castBloc;
  CastProvider({Key key, CastButtonBloc castBloc, Widget child})
      : this.castBloc = castBloc ?? CastButtonBloc(),
        super(child: child, key: key);

  @override
  bool updateShouldNotify(InheritedWidget oldWidget) => true;

  static CastButtonBloc of(BuildContext context) => (context.inheritFromWidgetOfExactType(CastProvider) as CastProvider).castBloc;
}
