import 'package:shared_preferences/shared_preferences.dart';
import 'dart:async';

class PrefHelper {
  static SharedPreferences _prefInstance;

  static Future<SharedPreferences> initPrefs() async {
    return await SharedPreferences.getInstance();
  }

  static Future<bool> getBool(String key) async {
    if (_prefInstance == null) {
      _prefInstance = await initPrefs();
    }

    return _prefInstance.getBool(key);
  }

  static Future<bool> setBool(String key, bool value) async {
    if (_prefInstance == null) {
      _prefInstance = await initPrefs();
    }
    return _prefInstance.setBool(key, value);
  }

  static Future<bool> getBoolWithDefault(String key, bool defValue) async {
    var val = await getBool(key);
    if (val != null) return val;

    setBool(key, defValue);
    return defValue;
  }

  static Future<String> getString(String key) async {
    if (_prefInstance == null) {
      _prefInstance = await initPrefs();
    }
    return _prefInstance.getString(key);
  }

  static Future<bool> setString(String key, String value) async {
    if (_prefInstance == null) {
      _prefInstance = await initPrefs();
    }
    return _prefInstance.setString(key, value);
  }

  static Future<String> getStringWithDefault(String key, String defValue) async {
    var val = await getString(key);
    if (val != null) return val;

    setString(key, defValue);
    return defValue;
  }

  static Future<List<String>> getStringList(String key) async {
    if (_prefInstance == null) {
      _prefInstance = await initPrefs();
    }
    return _prefInstance.getStringList(key);
  }

  static Future<bool> setStringList(String key, List<String> value) async {
    if (_prefInstance == null) {
      _prefInstance = await initPrefs();
    }
    return _prefInstance.setStringList(key, value);
  }

  static Future<List<String>> getStringListWithDefault(String key, List<String> defValue) async {
    var val = await getStringList(key);
    if (val != null) return val;

    setStringList(key, defValue);
    return defValue;
  }
}
