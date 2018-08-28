package com.sensorsdata.analytics.android.sdk;

import android.view.View;
import android.view.accessibility.AccessibilityEvent;

/*public*/ class SensorsDataAccessibilityDelegate extends View.AccessibilityDelegate {
    private View.AccessibilityDelegate mRealDelegate;

    SensorsDataAccessibilityDelegate(View.AccessibilityDelegate realDelegate) {
        this.mRealDelegate = realDelegate;
    }

    @Override
    public void sendAccessibilityEvent(View host, int eventType) {
        if (mRealDelegate != null) {
            mRealDelegate.sendAccessibilityEvent(host, eventType);
        }

        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            SensorsDataPrivate.trackViewOnClick(host);
        }
    }
}
