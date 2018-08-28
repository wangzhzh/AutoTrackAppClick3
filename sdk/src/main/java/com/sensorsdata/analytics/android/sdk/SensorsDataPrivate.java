package com.sensorsdata.analytics.android.sdk;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Keep;
import android.support.v7.view.menu.ActionMenuItemView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Created by 王灼洲 on 2018/7/22
 */
/*public*/ class SensorsDataPrivate {
    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"
            + ".SSS", Locale.CHINA);

    public static void mergeJSONObject(final JSONObject source, JSONObject dest)
            throws JSONException {
        Iterator<String> superPropertiesIterator = source.keys();
        while (superPropertiesIterator.hasNext()) {
            String key = superPropertiesIterator.next();
            Object value = source.get(key);
            if (value instanceof Date) {
                synchronized (mDateFormat) {
                    dest.put(key, mDateFormat.format((Date) value));
                }
            } else {
                dest.put(key, value);
            }
        }
    }

    public static Map<String, Object> getDeviceInfo(Context context) {
        final Map<String, Object> deviceInfo = new HashMap<>();
        {
            deviceInfo.put("$lib", "Android");
            deviceInfo.put("$lib_version", SensorsDataAPI.SDK_VERSION);
            deviceInfo.put("$os", "Android");
            deviceInfo.put("$os_version",
                    Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            deviceInfo
                    .put("$manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
            if (TextUtils.isEmpty(Build.MODEL)) {
                deviceInfo.put("$model", "UNKNOWN");
            } else {
                deviceInfo.put("$model", Build.MODEL.trim());
            }

            try {
                final PackageManager manager = context.getPackageManager();
                final PackageInfo packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
                deviceInfo.put("$app_version", packageInfo.versionName);

                int labelRes = packageInfo.applicationInfo.labelRes;
                deviceInfo.put("$app_name", context.getResources().getString(labelRes));
            } catch (final Exception e) {
                e.printStackTrace();
            }

            final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            deviceInfo.put("$screen_height", displayMetrics.heightPixels);
            deviceInfo.put("$screen_width", displayMetrics.widthPixels);

            return Collections.unmodifiableMap(deviceInfo);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static ViewGroup getRootViewFromActivity(Activity activity, boolean decorView) {
        if (decorView) {
            return (ViewGroup) activity.getWindow().getDecorView();
        } else {
            return activity.findViewById(android.R.id.content);
        }
    }

    /**
     * 注册 Application.ActivityLifecycleCallbacks
     *
     * @param application Application
     */
    @TargetApi(14)
    public static void registerActivityLifecycleCallbacks(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

            @Override
            public void onActivityCreated(final Activity activity, Bundle bundle) {
                final ViewGroup rootView = getRootViewFromActivity(activity, true);
                onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        delegateViewAccessibilityDelegate(activity, rootView);
                    }
                };
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(final Activity activity) {
                final ViewGroup rootView = getRootViewFromActivity(activity, true);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        delegateViewAccessibilityDelegate(activity, rootView);
                    }
                });
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (Build.VERSION.SDK_INT >= 16) {
                    final ViewGroup rootView = getRootViewFromActivity(activity, true);
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    /**
     * Delegate view OnClickListener
     *
     * @param context Context
     * @param view    View
     */
    @TargetApi(15)
    @SuppressWarnings("all")
    private static void delegateViewAccessibilityDelegate(final Context context, final View view) {
        if (context == null || view == null) {
            return;
        }

        if (view instanceof RatingBar) {
            final RatingBar.OnRatingBarChangeListener onRatingBarChangeListener =
                    ((RatingBar) view).getOnRatingBarChangeListener();
            if (onRatingBarChangeListener != null &&
                    !(onRatingBarChangeListener instanceof WrapperOnRatingBarChangeListener)) {
                ((RatingBar) view).setOnRatingBarChangeListener(
                        new WrapperOnRatingBarChangeListener(onRatingBarChangeListener));
            }
        } else if (view instanceof SeekBar) {
            final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener =
                    getOnSeekBarChangeListener(view);
            if (onSeekBarChangeListener != null &&
                    !(onSeekBarChangeListener instanceof WrapperOnSeekBarChangeListener)) {
                ((SeekBar) view).setOnSeekBarChangeListener(
                        new WrapperOnSeekBarChangeListener(onSeekBarChangeListener));
            }
        } else if (view instanceof Spinner) {
            AdapterView.OnItemSelectedListener onItemSelectedListener =
                    ((Spinner) view).getOnItemSelectedListener();
            if (onItemSelectedListener != null &&
                    !(onItemSelectedListener instanceof WrapperAdapterViewOnItemSelectedListener)) {
                ((Spinner) view).setOnItemSelectedListener(
                        new WrapperAdapterViewOnItemSelectedListener(onItemSelectedListener));
            }
        } else if (view instanceof ExpandableListView) {
            try {
                Class viewClazz = Class.forName("android.widget.ExpandableListView");
                //Child
                Field mOnChildClickListenerField = viewClazz.getDeclaredField("mOnChildClickListener");
                if (!mOnChildClickListenerField.isAccessible()) {
                    mOnChildClickListenerField.setAccessible(true);
                }
                ExpandableListView.OnChildClickListener onChildClickListener =
                        (ExpandableListView.OnChildClickListener) mOnChildClickListenerField.get(view);
                if (onChildClickListener != null &&
                        !(onChildClickListener instanceof WrapperOnChildClickListener)) {
                    ((ExpandableListView) view).setOnChildClickListener(
                            new WrapperOnChildClickListener(onChildClickListener));
                }

                //Group
                Field mOnGroupClickListenerField = viewClazz.getDeclaredField("mOnGroupClickListener");
                if (!mOnGroupClickListenerField.isAccessible()) {
                    mOnGroupClickListenerField.setAccessible(true);
                }
                ExpandableListView.OnGroupClickListener onGroupClickListener =
                        (ExpandableListView.OnGroupClickListener) mOnGroupClickListenerField.get(view);
                if (onGroupClickListener != null &&
                        !(onGroupClickListener instanceof WrapperOnGroupClickListener)) {
                    ((ExpandableListView) view).setOnGroupClickListener(
                            new WrapperOnGroupClickListener(onGroupClickListener));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (view instanceof ListView ||
                view instanceof GridView) {
            AdapterView.OnItemClickListener onItemClickListener =
                    ((AdapterView) view).getOnItemClickListener();
            if (onItemClickListener != null &&
                    !(onItemClickListener instanceof WrapperAdapterViewOnItemClick)) {
                ((AdapterView) view).setOnItemClickListener(
                        new WrapperAdapterViewOnItemClick(onItemClickListener));
            }
        } else {
            View.AccessibilityDelegate delegate = null;
            try {
                Class<?> kClass = view.getClass();
                Method method = kClass.getMethod("getAccessibilityDelegate");
                delegate = (View.AccessibilityDelegate) method.invoke(view);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (delegate == null || !(delegate instanceof SensorsDataAccessibilityDelegate)) {
                view.setAccessibilityDelegate(new SensorsDataAccessibilityDelegate(delegate));
            }
        }

        //如果 view 是 ViewGroup，需要递归遍历子 View 并 hook
        if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    View childView = viewGroup.getChildAt(i);
                    //递归
                    delegateViewAccessibilityDelegate(context, childView);
                }
            }
        }
    }

    @SuppressWarnings("all")
    private static SeekBar.OnSeekBarChangeListener getOnSeekBarChangeListener(View view) {
        try {
            Class viewClazz = Class.forName("android.widget.SeekBar");
            Field mOnCheckedChangeListenerField = viewClazz.getDeclaredField("mOnSeekBarChangeListener");
            if (!mOnCheckedChangeListenerField.isAccessible()) {
                mOnCheckedChangeListenerField.setAccessible(true);
            }
            return (SeekBar.OnSeekBarChangeListener) mOnCheckedChangeListenerField.get(view);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String traverseViewContent(StringBuilder stringBuilder, View root) {
        try {
            if (root == null) {
                return stringBuilder.toString();
            }

            if (root instanceof ViewGroup) {
                final int childCount = ((ViewGroup) root).getChildCount();
                for (int i = 0; i < childCount; ++i) {
                    final View child = ((ViewGroup) root).getChildAt(i);

                    if (child.getVisibility() != View.VISIBLE) {
                        continue;
                    }
                    if (child instanceof ViewGroup) {
                        traverseViewContent(stringBuilder, child);
                    } else {
                        String viewText = getElementContent(child);
                        if (!TextUtils.isEmpty(viewText)) {
                            stringBuilder.append(viewText);
                        }
                    }
                }
            } else {
                stringBuilder.append(getElementContent(root));
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return stringBuilder.toString();
        }
    }

    public static void trackAdapterView(AdapterView<?> adapterView, View view, int position) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_type", adapterView.getClass().getCanonicalName());
            jsonObject.put("$element_id", getViewId(adapterView));
            jsonObject.put("$element_position", String.valueOf(position));
            StringBuilder stringBuilder = new StringBuilder();
            String viewText = traverseViewContent(stringBuilder, view);
            if (!TextUtils.isEmpty(viewText)) {
                jsonObject.put("$element_element", viewText);
            }
            Activity activity = getActivityFromView(adapterView);
            if (activity != null) {
                jsonObject.put("$activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().track("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackAdapterView(AdapterView<?> adapterView, View view, int groupPosition, int childPosition) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_type", adapterView.getClass().getCanonicalName());
            jsonObject.put("$element_id", getViewId(adapterView));
            if (childPosition > -1) {
                jsonObject.put("$element_position", String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));
            } else {
                jsonObject.put("$element_position", String.format(Locale.CHINA, "%d", groupPosition));
            }
            StringBuilder stringBuilder = new StringBuilder();
            String viewText = traverseViewContent(stringBuilder, view);
            if (!TextUtils.isEmpty(viewText)) {
                jsonObject.put("$element_element", viewText);
            }
            Activity activity = getActivityFromView(adapterView);
            if (activity != null) {
                jsonObject.put("$activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().track("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * View 被点击，自动埋点
     *
     * @param view View
     */
    @Keep
    public static void trackViewOnClick(View view) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_type", view.getClass().getCanonicalName());
            jsonObject.put("$element_id", SensorsDataPrivate.getViewId(view));
            jsonObject.put("$element_content", SensorsDataPrivate.getElementContent(view));

            Activity activity = SensorsDataPrivate.getActivityFromView(view);
            if (activity != null) {
                jsonObject.put("$activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().track("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取 Android ID
     *
     * @param mContext Context
     * @return String
     */
    @SuppressLint("HardwareIds")
    public static String getAndroidID(Context mContext) {
        String androidID = "";
        try {
            androidID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return androidID;
    }

    /**
     * 获取 view 的 anroid:id 对应的字符串
     *
     * @param view View
     * @return String
     */
    private static String getViewId(View view) {
        String idString = null;
        try {
            if (view.getId() != View.NO_ID) {
                idString = view.getContext().getResources().getResourceEntryName(view.getId());
            }
        } catch (Exception e) {
            //ignore
        }
        return idString;
    }

    /**
     * 获取 View 上显示的文本
     *
     * @param view View
     * @return String
     */
    private static String getElementContent(View view) {
        if (view == null) {
            return null;
        }

        String text = null;
        if (view instanceof Button) {
            text = ((Button) view).getText().toString();
        } else if (view instanceof ActionMenuItemView) {
            text = ((ActionMenuItemView) view).getText().toString();
        } else if (view instanceof TextView) {
            text = ((TextView) view).getText().toString();
        } else if (view instanceof ImageView) {
            text = view.getContentDescription().toString();
        } else if (view instanceof RadioGroup) {
            try {
                RadioGroup radioGroup = (RadioGroup) view;
                Activity activity = getActivityFromView(view);
                if (activity != null) {
                    int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                    RadioButton radioButton = activity.findViewById(checkedRadioButtonId);
                    if (radioButton != null) {
                        text = radioButton.getText().toString();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (view instanceof RatingBar) {
            text = String.valueOf(((RatingBar) view).getRating());
        } else if (view instanceof SeekBar) {
            text = String.valueOf(((SeekBar) view).getProgress());
        } else if (view instanceof ViewGroup) {
            text = traverseViewContent(new StringBuilder(), view);
        }
        return text;
    }

    /**
     * 获取 View 所属 Activity
     *
     * @param view View
     * @return Activity
     */
    private static Activity getActivityFromView(View view) {
        Activity activity = null;
        if (view == null) {
            return null;
        }

        try {
            Context context = view.getContext();
            if (context != null) {
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (!(context instanceof Activity) && context instanceof ContextWrapper) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activity;
    }

    private static void addIndentBlank(StringBuilder sb, int indent) {
        try {
            for (int i = 0; i < indent; i++) {
                sb.append('\t');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String formatJson(String jsonStr) {
        try {
            if (null == jsonStr || "".equals(jsonStr)) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            char last;
            char current = '\0';
            int indent = 0;
            boolean isInQuotationMarks = false;
            for (int i = 0; i < jsonStr.length(); i++) {
                last = current;
                current = jsonStr.charAt(i);
                switch (current) {
                    case '"':
                        if (last != '\\') {
                            isInQuotationMarks = !isInQuotationMarks;
                        }
                        sb.append(current);
                        break;
                    case '{':
                    case '[':
                        sb.append(current);
                        if (!isInQuotationMarks) {
                            sb.append('\n');
                            indent++;
                            addIndentBlank(sb, indent);
                        }
                        break;
                    case '}':
                    case ']':
                        if (!isInQuotationMarks) {
                            sb.append('\n');
                            indent--;
                            addIndentBlank(sb, indent);
                        }
                        sb.append(current);
                        break;
                    case ',':
                        sb.append(current);
                        if (last != '\\' && !isInQuotationMarks) {
                            sb.append('\n');
                            addIndentBlank(sb, indent);
                        }
                        break;
                    default:
                        sb.append(current);
                }
            }

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
