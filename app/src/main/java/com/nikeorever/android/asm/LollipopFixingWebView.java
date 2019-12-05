package com.nikeorever.android.asm;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;

public class LollipopFixingWebView extends WebView {

    public LollipopFixingWebView(Context context) {
        super(getFixedContext(context));
    }

    public LollipopFixingWebView(Context context, AttributeSet attrs) {
        super(getFixedContext(context), attrs);
    }

    public LollipopFixingWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getFixedContext(context), attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LollipopFixingWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(getFixedContext(context), attrs, defStyleAttr, defStyleRes);
    }

    public static Context getFixedContext(Context context) {
        Log.i("LollipopFixingWebView", "WebView has been fixed by LollipopWebViewFixingPlugin");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) // Android Lollipop 5.0 & 5.1
            return context.createConfigurationContext(new Configuration());
        return context;
    }
}
