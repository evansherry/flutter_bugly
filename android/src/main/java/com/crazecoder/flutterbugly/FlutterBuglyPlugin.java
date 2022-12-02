package com.crazecoder.flutterbugly;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.crazecoder.flutterbugly.bean.BuglyInitResultInfo;
import com.crazecoder.flutterbugly.utils.JsonUtil;
import com.crazecoder.flutterbugly.utils.MapUtil;
import com.tencent.bugly.crashreport.CrashReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.BuildConfig;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterBuglyPlugin
 */
public class FlutterBuglyPlugin implements FlutterPlugin, MethodCallHandler {
    private Result result;
    private boolean isResultSubmitted = false;
    private MethodChannel channel;
    private Context applicationContext;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        FlutterBuglyPlugin plugin = new FlutterBuglyPlugin();
        plugin.setupChannel(registrar.messenger(), registrar.context());
    }

    @Override
    public void onMethodCall(final MethodCall call, @NonNull final Result result) {
        isResultSubmitted = false;
        this.result = result;
        switch (call.method) {
            case "initBugly":
                if (call.hasArgument("appId")) {
                    String appId = call.argument("appId").toString();
                    CrashReport.initCrashReport(applicationContext, appId, BuildConfig.DEBUG);
                    if (call.hasArgument("channel")) {
                        String channel = call.argument("channel");
                        if (!TextUtils.isEmpty(channel))
                            CrashReport.setAppChannel(applicationContext, channel);
                    }
                    result(getResultBean(true, appId, "Bugly 初始化成功"));
                } else {
                    result(getResultBean(false, null, "Bugly appId不能为空"));
                }
                break;
            case "setUserId":
                if (call.hasArgument("userId")) {
                    String userId = call.argument("userId");
                    CrashReport.setUserId(applicationContext, userId);
                }
                result(null);
                break;
            case "setUserTag":
                if (call.hasArgument("userTag")) {
                    Integer userTag = call.argument("userTag");
                    if (userTag != null)
                        CrashReport.setUserSceneTag(applicationContext, userTag);
                }
                result(null);
                break;
            case "putUserData":
                if (call.hasArgument("key") && call.hasArgument("value")) {
                    String userDataKey = call.argument("key");
                    String userDataValue = call.argument("value");
                    CrashReport.putUserData(applicationContext, userDataKey, userDataValue);
                }
                result(null);
                break;
            case "postCatchedException":
                postException(call);
                result(null);
                break;
            default:
                result.notImplemented();
                isResultSubmitted = true;
                break;
        }

    }

    private void postException(MethodCall call) {
        String message = "";
        String detail = null;
        if (call.hasArgument("crash_message")) {
            message = call.argument("crash_message");
        }
        if (call.hasArgument("crash_detail")) {
            detail = call.argument("crash_detail");
        }
        if (TextUtils.isEmpty(detail)) return;
        CrashReport.postException(8, "Flutter Exception", message, detail, null);

    }

    private void result(Object object) {
        if (result != null && !isResultSubmitted) {
            if (object == null) {
                result.success(null);
            } else {
                result.success(JsonUtil.toJson(MapUtil.deepToMap(object)));
            }
            isResultSubmitted = true;
        }
    }

    private BuglyInitResultInfo getResultBean(boolean isSuccess, String appId, String msg) {
        BuglyInitResultInfo bean = new BuglyInitResultInfo();
        bean.setSuccess(isSuccess);
        bean.setAppId(appId);
        bean.setMessage(msg);
        return bean;
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        setupChannel(binding.getBinaryMessenger(), binding.getApplicationContext());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
    }

    private void setupChannel(BinaryMessenger messenger, Context context) {
        channel = new MethodChannel(messenger, "crazecoder/flutter_bugly");
        channel.setMethodCallHandler(this);
        applicationContext = context;
    }

    private List<Class<? extends Activity>> forNameActivity(List<String> acts) {
        if (acts == null || acts.isEmpty()) return null;
        final List<Class<? extends Activity>> classList = new ArrayList<>();
        for (String act : acts) {
            try {
                classList.add((Class<? extends Activity>) Class.forName(act));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return classList;
    }
}