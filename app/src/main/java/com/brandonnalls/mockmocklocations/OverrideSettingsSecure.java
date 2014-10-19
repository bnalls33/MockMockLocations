package com.brandonnalls.mockmocklocations;

import android.content.ContentResolver;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class OverrideSettingsSecure implements IXposedHookLoadPackage {
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getString", ContentResolver.class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String requested = (String) param.args[1];
                if (requested.equals("mock_location")) {
                    param.setResult("0");
                }
            }
        });
    }
}