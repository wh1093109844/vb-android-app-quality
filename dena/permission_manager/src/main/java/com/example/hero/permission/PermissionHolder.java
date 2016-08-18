package com.example.hero.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;

/**
 * Created by hero on 2016/8/9.
 */

public final class PermissionHolder {

    private static final String TAG = PermissionHolder.class.getSimpleName();

    private static List<RequestPermissionListener> list = new ArrayList<>();

    private PermissionHolder() { };

    /**
     * 请求权限
     * @param activity
     * @param requestCode
     * @param subscriber
     * @param permission
     */
    public static void requestPermission(final Activity activity, final int requestCode, final Subscriber subscriber, final String permission) {
        ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
        list.add(new RequestPermissionListener() {
            @Override
            public void onCompile(int requestCode1, @NonNull String[] permissions, @NonNull int[] grantResults) {
                if (requestCode1 == requestCode) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(permission)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                if (subscriber != null) {
                                    subscriber.onCompleted();
                                }
                            } else {
                                if (subscriber != null) {
                                    subscriber.onError(new Exception("拒绝" + permission));
                                }
                            }
                            list.remove(this);
                        }
                    }
                }
            }
        });
    }

    /**
     * 获取IMEI
     * @param activity
     * @return
     */
    public static Observable<String> getIMEI(final Activity activity) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                if (checkPermission(activity, Manifest.permission.READ_PHONE_STATE)) {
                    TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
                    subscriber.onNext(tm.getDeviceId());
                    subscriber.onCompleted();
                } else {
                    Observable.create(new Observable.OnSubscribe<String>() {
                        @Override
                        public void call(final Subscriber<? super String> subscriber) {
                            requestPermission(activity, 100, subscriber, Manifest.permission.READ_PHONE_STATE);
                        }
                    }).subscribe(new Observer<String>() {
                        @Override
                        public void onCompleted() {
                            TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
                            subscriber.onNext(tm.getDeviceId());
                            subscriber.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            subscriber.onError(e);
                        }

                        @Override
                        public void onNext(String s) {

                        }
                    });
                }
            }
        });
    }

    /**
     * 接收请求权限返回的结果
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "requestCode:" + requestCode);
        Log.i(TAG, "permissions:" + permissions);
        for (RequestPermissionListener listener : list) {
            listener.onCompile(requestCode, permissions, grantResults);
        }
    }

    /**
     * 检查是否拥有某权限
     * @param context
     * @param permission
     * @return
     */
    public static boolean checkPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求权限监听
     */
    public interface RequestPermissionListener {
        public void onCompile(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);
    }
}
