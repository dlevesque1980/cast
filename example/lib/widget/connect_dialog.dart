import 'package:cast/route_props.dart';
import 'package:cast_example/bloc/cast_button_bloc.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class ConnectDialog extends StatelessWidget {
  const ConnectDialog({
    Key key,
    @required this.routes,
    @required this.castButtonBloc,
  }) : super(key: key);

  final Map<String, RouteProps> routes;
  final CastButtonBloc castButtonBloc;

  @override
  Widget build(BuildContext context) {
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
                            castButtonBloc.connect.add(routes[route]);
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
}
