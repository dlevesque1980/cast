import 'package:cast_example/bloc/cast_button_bloc.dart';
import 'package:cast_example/provider/cast_button_provider.dart';
import 'package:cast_example/widget/connect_dialog.dart';
import 'package:cast_example/widget/disconnect_dialog.dart';
import 'package:flutter/material.dart';
import 'package:cast/events.dart';

class CastButton extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return CastProvider(child: InnerButton());
  }
}

class InnerButton extends StatefulWidget {
  @override
  InnerButtonState createState() {
    return new InnerButtonState();
  }
}

class InnerButtonState extends State<InnerButton> with WidgetsBindingObserver {
  AppLifecycleState _lastLifecycleState;
  CastButtonBloc _bloc;

  Widget getIconButton(BuildContext context, AsyncSnapshot<RouteChangedEvent> snapshot) {
    var castBloc = CastProvider.of(context);
    if (snapshot.hasData) {
      return StreamBuilder(
        initialData: false,
        stream: castBloc.onConnection,
        builder: (BuildContext context, AsyncSnapshot<bool> snap) => IconButton(
            icon: Icon(snap.data ? Icons.cast_connected : Icons.cast),
            onPressed: () async {
              if (!snap.data) {
                showDialog(context: context, builder: (_) => ConnectDialog(routes: snapshot.data.routes, castButtonBloc: castBloc));
                return;
              }

              showDialog(context: context, builder: (_) => DisconnectDialog(castButtonBloc: castBloc));
            }),
      );
    }

    return Container();
  }

  @override
  void didChangeDependencies() {
    if (_bloc == null) {
      _bloc = CastProvider.of(context);
    }
    super.didChangeDependencies();
  }

  @override
  void dispose() {
    _bloc.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused) {
      _bloc.dispose();
    } else if (state == AppLifecycleState.resumed) {
      if (state != _lastLifecycleState) {
        setState(() {
          _lastLifecycleState = state;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return StreamBuilder(
        initialData: _bloc.initialData(),
        stream: _bloc.routeChanged,
        builder: (BuildContext context, AsyncSnapshot<RouteChangedEvent> snapshot) => getIconButton(context, snapshot));
  }
}
