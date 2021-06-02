import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';

import 'location_details.dart';

class LocationUpdatePlugin {
  late Function onLogDetails;
  late MethodChannel channel;
  bool serviceStarted = false;

  LocationUpdatePlugin(this.onLogDetails) {
    if (Platform.isAndroid) {
      channel = MethodChannel(channelName);
      channel.setMethodCallHandler(this._onMethodCall);
    }
  }

  Future<bool> _onMethodCall(MethodCall call) async {
    switch (call.method) {
      case logDetails:
        List<String> locationList =
            (jsonDecode(call.arguments) as List<dynamic>).cast<String>();
       // print("Size ${locationList.length}");
        onLogDetails(locationList);
        break;
    }
    throw MissingPluginException(
        '${call.method} was invoked but has no handler');
  }

  void startLocationUpdates() {
    if (Platform.isAndroid) channel.invokeMethod("startLocationUpdates");
    serviceStarted = true;
  }

  void stopLocationUpdates() {
    if (Platform.isAndroid) channel.invokeMethod("stopLocationUpdates");
    serviceStarted = false;
  }
}
