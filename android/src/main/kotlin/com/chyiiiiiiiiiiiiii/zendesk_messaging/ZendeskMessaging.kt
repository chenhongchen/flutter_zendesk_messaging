import android.content.Intent
import com.chyiiiiiiiiiiiiii.zendesk_messaging.ZendeskMessagingPlugin
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zendesk.android.Zendesk
import zendesk.android.ZendeskUser
import zendesk.android.events.ZendeskEvent
import zendesk.android.events.ZendeskEventListener
import zendesk.messaging.android.DefaultMessagingFactory


class ZendeskMessaging(private val plugin: ZendeskMessagingPlugin, private val channel: MethodChannel) {
    companion object {
        const val tag = "[ZendeskMessaging]"

        // Method channel callback keys
        const val initializeSuccess: String = "initialize_success"
        const val initializeFailure: String = "initialize_failure"
        const val loginSuccess: String = "login_success"
        const val loginFailure: String = "login_failure"
        const val logoutSuccess: String = "logout_success"
        const val logoutFailure: String = "logout_failure"

        // add start
        const val unreadMessageCountChanged: String = "unread_message_count_changed"
        const val authenticationFailed: String = "authentication_failed"
        const val unknownEvent: String = "unknown_event"
        // add end
    }

    // add start
    // To create and use the event listener:
    private val zendeskEventListener: ZendeskEventListener = ZendeskEventListener { zendeskEvent ->
        when (zendeskEvent) {
            is ZendeskEvent.UnreadMessageCountChanged -> {
                val unreadCount = getUnreadMessageCount()
                channel.invokeMethod(unreadMessageCountChanged, mapOf("unreadCount" to unreadCount))
            }

            is ZendeskEvent.AuthenticationFailed -> {
                channel.invokeMethod(authenticationFailed, null)
            }

            is ZendeskEvent.FieldValidationFailed -> {
                channel.invokeMethod(unknownEvent, null)
            }

            else -> {
                channel.invokeMethod(unknownEvent, null)
            }
        }
    }
    // add end


    fun initialize(channelKey: String) {
        println("$tag - Channel Key - $channelKey")
        Zendesk.initialize(
                plugin.activity!!,
                channelKey,
                successCallback = { value ->
                    plugin.isInitialized = true;
                    println("$tag - initialize success - $value")
                    channel.invokeMethod(initializeSuccess, null)
                    // add start
                    Zendesk.instance.addEventListener(zendeskEventListener)
                    // add end
                },
                failureCallback = { error ->
                    plugin.isInitialized = false
                    println("$tag - initialize failure - $error")
                    channel.invokeMethod(initializeFailure, mapOf("error" to error.message))
                },
                messagingFactory = DefaultMessagingFactory()
        )
    }

    fun invalidate() {
        Zendesk.invalidate()
        plugin.isInitialized = false;
        // add start
        Zendesk.instance.removeEventListener(zendeskEventListener)
        // add end
        println("$tag - invalidated")
    }

    fun show() {
        Zendesk.instance.messaging.showMessaging(plugin.activity!!, Intent.FLAG_ACTIVITY_NEW_TASK)
        println("$tag - show")
    }

    fun getUnreadMessageCount(): Int {
        return try {
            Zendesk.instance.messaging.getUnreadMessageCount()
        } catch (error: Throwable) {
            0
        }
    }

    fun setConversationTags(tags: List<String>) {
        Zendesk.instance.messaging.setConversationTags(tags)
    }

    fun clearConversationTags() {
        Zendesk.instance.messaging.clearConversationTags()
    }

    fun loginUser(jwt: String) {
        Zendesk.instance.loginUser(
                jwt,
                { value: ZendeskUser? ->
                    plugin.isLoggedIn = true;
                    value?.let {
                        channel.invokeMethod(loginSuccess, mapOf("id" to it.id, "externalId" to it.externalId))
                    } ?: run {
                        channel.invokeMethod(loginSuccess, mapOf("id" to null, "externalId" to null))
                    }
                },
                { error: Throwable? ->
                    println("$tag - Login failure : ${error?.message}")
                    println(error)
                    channel.invokeMethod(loginFailure, mapOf("error" to error?.message))
                })
    }

    fun logoutUser() {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                Zendesk.instance.logoutUser(successCallback = {
                    plugin.isLoggedIn = false;
                    channel.invokeMethod(logoutSuccess, null)
                }, failureCallback = {
                    channel.invokeMethod(logoutFailure, null)
                });
            } catch (error: Throwable) {
                println("$tag - Logout failure : ${error.message}")
                channel.invokeMethod(logoutFailure, mapOf("error" to error.message))
            }
        }
    }

    fun setConversationFields(fields: Map<String, String>) {
        Zendesk.instance.messaging.setConversationFields(fields)
    }

    fun clearConversationFields() {
        Zendesk.instance.messaging.clearConversationFields()
    }
}
