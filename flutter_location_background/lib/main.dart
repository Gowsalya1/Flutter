import 'package:flutter/material.dart';
import 'package:flutter_location_background/location_plugin.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Location Update',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  List<String> _locationList = [];
  late LocationUpdatePlugin _locationUpdatePlugin;
  var _isLocationUpdates = false;
  var _isViewLog = false;

  @override
  void initState() {
    _locationUpdatePlugin = LocationUpdatePlugin((logList) {
      setState(() {
        this._locationList = logList;
        if (_locationList.isEmpty || logList == null) {
          _locationList = [];
          _isLocationUpdates = false;
        } else
          _isLocationUpdates = true;
      });
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: <Widget>[
          Container(
            height: 100.0,
          ),
          ElevatedButton(
              onPressed: () {
                setState(() {
                  if (_isLocationUpdates) {
                    _locationUpdatePlugin.stopLocationUpdates();
                    _isLocationUpdates = false;
                    _locationList = [];
                  } else {
                    _locationUpdatePlugin.startLocationUpdates();
                    _isLocationUpdates = true;
                  }
                });
              },
              style: ButtonStyle(
                backgroundColor: MaterialStateProperty.all(Colors.blue),
              ),
              child: Text(
                _isLocationUpdates
                    ? "Stop Location Update"
                    : "Start Location Update",
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w500,
                  fontSize: 16,
                ),
              )),
          SizedBox(
            height: 20.0,
          ),
          ElevatedButton(
              onPressed: () {
                setState(() {
                  _isViewLog = !_isViewLog;
                });
              },
              style: ButtonStyle(
                backgroundColor: MaterialStateProperty.all(Colors.blue),
              ),
              child: Text(
                _isViewLog ? "Hide Logs" : "View Logs",
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w500,
                  fontSize: 16,
                ),
              )),
          Visibility(
              visible: _isViewLog,
              child: _locationList.length != 0
                  ? Expanded(
                      child: ListView.builder(
                      padding: EdgeInsets.only(top: 10.0, bottom: 10.0),
                      itemBuilder: (context, position) {
                        return Container(
                          alignment: Alignment.center,
                          padding: EdgeInsets.only(top: 16.0, left: 20.0),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            crossAxisAlignment: CrossAxisAlignment.center,
                            children: [
                              Padding(
                                padding: const EdgeInsets.only(right: 20.0, left: 20.0),
                                child: Text("•   ",
                                    style: TextStyle(
                                        color: Colors.black,
                                        fontSize: 30.0,
                                        fontWeight: FontWeight.bold)),
                              ),
                              Expanded(
                                flex : 1,
                                child: Text(
                                  _locationList[position],
                                  textAlign: TextAlign.start,
                                  style: TextStyle(
                                    color: Colors.black,
                                    fontSize: 15.0,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                      itemCount: _locationList.length,
                    ))
                  : Container(
                      alignment: Alignment.center,
                      margin: EdgeInsets.only(
                        top: 30.0,
                      ),
                      child: Text(
                        "No logs available",
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Colors.black,
                          fontSize: 13,
                        ),
                      )))
        ],
      ),
    );
  }
}
