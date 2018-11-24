import Flutter
import UIKit
import GoogleCast

public class SwiftCastPlugin: NSObject, FlutterPlugin {
    private var gckCastOptions: GCKCastOptions?;
    private var discoveryManager: GCKDiscoveryManager?;
    
    override init(){
        gckCastOptions = nil
        discoveryManager = nil
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "didisoft.cast", binaryMessenger: registrar.messenger())
        let instance = SwiftCastPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
            case "getRoutes":
                getRoutes(result: result)
                break;
            case "init":
                let arguments = call.arguments as? NSDictionary
                let appId: String = arguments!["appId"] as! String;
                initChromecast(result: result, appId: appId);
                break;
            case "getPlayerStatus":
                temp(result:result)
                break;
            case "select":
                temp(result:result)
                break;
            case "unselect":
                temp(result:result)
                break;
            case "play":
                temp(result:result)
                break;
            case "pause":
                temp(result:result)
                break;
            case "resume":
                temp(result:result)
                break;
            case "dispose":
                temp(result:result)
                break;
            default:
                result(FlutterMethodNotImplemented)
                break
        }
    }

    private func initChromecast(result: @escaping FlutterResult, appId: String) {
        let discoveryCriteria = GCKDiscoveryCriteria(applicationID: appId)
        gckCastOptions = GCKCastOptions(discoveryCriteria: discoveryCriteria)
        GCKCastContext.setSharedInstanceWith(gckCastOptions!)
        GCKLogger.sharedInstance().delegate = self
        
        self.discoveryManager = GCKCastContext.sharedInstance().discoveryManager
        self.discoveryManager!.add(self)
        self.discoveryManager!.passiveScan = false
        self.discoveryManager!.startDiscovery()
    }
    

    
    private func temp(result: @escaping FlutterResult) {
        result("temporary")
    }
    
    private func getRoutes(result: @escaping FlutterResult) {
        result(["didicast": "{\"connectionState\": 1, \"id\": \"452\"}","toto": "{\"connectionState\": 1, \"id\": \"452\"}"])
    }
    
}

extension SwiftCastPlugin: GCKLoggerDelegate {
    
    func logMessage(_ message: String, at level: GCKLoggerLevel, fromFunction function: String, location: String) {
        print("Message from Chromecast = \(message)")
    }
}

extension SwiftCastPlugin: GCKDiscoveryManagerListener {
    
    private func didStartDiscoveryForDeviceCategory(deviceCategory: String) {
        print("GCKDiscoveryManagerListener: \(deviceCategory)")
        
        print("FOUND: \(self.discoveryManager!.hasDiscoveredDevices)")
    }
}

