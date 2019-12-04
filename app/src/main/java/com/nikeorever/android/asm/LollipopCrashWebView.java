package com.nikeorever.android.asm;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;

public class LollipopCrashWebView extends WebView {
    public LollipopCrashWebView(Context context) {
        super(context);
    }

    public LollipopCrashWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LollipopCrashWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LollipopCrashWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        //com.nikeorever.android.asm.LollipopCrashWebView
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}