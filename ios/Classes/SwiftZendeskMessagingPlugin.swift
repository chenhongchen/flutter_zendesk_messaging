import Flutter
import UIKit

public class SwiftZendeskMessagingPlugin: NSObject, FlutterPlugin {
    let TAG = "[SwiftZendeskMessagingPlugin]"
    private var channel: FlutterMethodChannel
    var isInitialized = false
    var isLoggedIn = false
    
    init(channel: FlutterMethodChannel) {
        self.channel = channel
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "zendesk_messaging", binaryMessenger: registrar.messenger())
        let instance = SwiftZendeskMessagingPlugin(channel: channel)
        registrar.addMethodCallDelegate(instance, channel: channel)
        registrar.addApplicationDelegate(instance)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        DispatchQueue.main.async {
            self.processMethodCall(call, result: result)
        }
    }

    private func processMethodCall(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let method = call.method
        let arguments = call.arguments as? Dictionary<String, Any>
        let zendeskMessaging = ZendeskMessaging(flutterPlugin: self, channel: channel)
        
        switch(method){
            case "initialize":
                if (isInitialized) {
                    print("\(TAG) - Messaging is already initialize!\n")
                    return
                }
                let channelKey: String = (arguments?["channelKey"] ?? "") as! String
                zendeskMessaging.initialize(channelKey: channelKey)
                break;
            case "show":
                if (!isInitialized) {
                    print("\(TAG) - Messaging needs to be initialized first.\n")
                }
                zendeskMessaging.show(rootViewController: UIApplication.shared.delegate?.window??.rootViewController)
                break
            case "loginUser":
                if (!isInitialized) {
                    print("\(TAG) - Messaging needs to be initialized first.\n")
                }
                let jwt: String = arguments?["jwt"] as! String
                zendeskMessaging.loginUser(jwt: jwt)
                break
            case "logoutUser":
                if (!isInitialized) {
                    print("\(TAG) - Messaging needs to be initialized first.\n")
                }
                zendeskMessaging.logoutUser()
                break
            case "getUnreadMessageCount":
                if (!isInitialized) {
                    print("\(TAG) - Messaging needs to be initialized first.\n")
                }
                result(handleMessageCount())
                return
            
            case "isInitialized":
                result(handleInitializedStatus())
                return
            case "isLoggedIn":
                result(handleLoggedInStatus())
                return
            
            case "setConversationTags":
                if (!isInitialized) {
                    print("\(TAG) - Messaging needs to be initialized first.\n")
                }
                let tags: [String] = arguments?["tags"] as! [String]
                zendeskMessaging.setConversationTags(tags:tags)
                break
            case "clearConversationTags":
                if (!isInitialized) {
                    print("\(TAG) - Messaging needs to be initialized first.\n")
                }
                zendeskMessaging.clearConversationTags()
                break
            case "setConversationFields":
                if (!isInitialized) {
                    print("\(TAG) - Messaging needs to be initialized first.\n")
                }
                let fields: [String: String] = arguments?["fields"] as! [String: String]
                zendeskMessaging.setConversationFields(fields:fields)
                break
            case "clearConversationFields":
                if (!isInitialized) {
                    print("\(TAG) - Messaging needs to be initialized first.\n")
                }
                zendeskMessaging.clearConversationFields()
                break
            case "invalidate":
                if (!isInitialized) {
                    print("\(TAG) - Messaging is already on an invalid state\n")
                    return
                }
                zendeskMessaging.invalidate()
                break
            default:
                result(FlutterMethodNotImplemented)
                return;
        }
        
        if (arguments != nil) {
            result(arguments)
        } else {
            result(0)
        }
    }

    private func handleMessageCount() ->Int{
         let zendeskMessaging = ZendeskMessaging(flutterPlugin: self, channel: channel)

        return zendeskMessaging.getUnreadMessageCount()
    }
    private func handleInitializedStatus() ->Bool{
        return isInitialized
    }
    private func handleLoggedInStatus() ->Bool{
        return isLoggedIn
    }
}
