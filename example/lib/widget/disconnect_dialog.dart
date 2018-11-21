import 'package:cast_example/bloc/cast_button_bloc.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class DisconnectDialog extends StatelessWidget {
  const DisconnectDialog({
    Key key,
    @required this.castButtonBloc,
  }) : super(key: key);

  final CastButtonBloc castButtonBloc;

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
        title: Text("Devices"),
        content: StreamBuilder(
          initialData: true,
          stream: castButtonBloc.onConnection,
          builder: (BuildContext context, AsyncSnapshot<bool> snap) => RaisedButton(
              onPressed: (snap.data)
                  ? () {
                      castButtonBloc.disconnect.add(true);
                      Navigator.pop(context);
                    }
                  : null,
              child: Text("Disconnect")),
        ));
  }
}
