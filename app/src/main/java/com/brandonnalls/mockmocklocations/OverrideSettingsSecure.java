package com.brandonnalls.mockmocklocations;

import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class OverrideSettingsSecure implements IXposedHookLoadPackage {
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // before API level 18, hook Settings.Secure.ALLOW_MOCK_LOCATION
        if (Build.VERSION.SDK_INT < 18) {
            findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getString",
                    ContentResolver.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String requested = (String) param.args[1];
                    if (requested.equals(Settings.Secure.ALLOW_MOCK_LOCATION)) {
                        param.setResult("0");
                    }
                }
            });
            return;
        }

        // after API level 18, hook Location.isFromMockProvider
        findAndHookMethod("android.location.Location", lpparam.classLoader, "isFromMockProvider",
                new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}