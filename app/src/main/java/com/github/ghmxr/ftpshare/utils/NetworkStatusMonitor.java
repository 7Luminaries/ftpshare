package com.github.ghmxr.ftpshare.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.HashSet;

public class NetworkStatusMonitor {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final HashSet<NetworkStatusCallback> callbacks = new HashSet<>();
    private static boolean wifiConnected = false;
    private static boolean ethernetConnected = false;
    private static boolean cellularConnected = false;
    private static boolean apEnabled = false;
    private static final BroadcastReceiver apReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equalsIgnoreCase(intent.getAction())) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                if (state == 11) {//AP关闭
                    updateApState(false);
                }
                if (state == 13) {//AP打开
                    updateApState(true);
                }
            }
        }
    };
    private static ConnectivityManager connectivityManager;
    private static ConnectivityManager.NetworkCallback networkCallback;
    private static boolean initialized = false;

    public static void init(@NonNull Context context) {
        if (initialized) return;
        Context appContext = context.getApplicationContext();
        connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        registerReceiverCompat(appContext, apReceiver, new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"));
        registerNetworkCallback();
        refreshActiveNetworkState();
        initialized = true;
    }

    public static void addNetworkStatusCallback(@NonNull NetworkStatusCallback callback) {
        synchronized (callbacks) {
            callbacks.add(callback);
        }
    }

    public static void removeNetworkStatusCallback(@NonNull NetworkStatusCallback callback) {
        synchronized (callbacks) {
            callbacks.remove(callback);
        }
    }

    private static void sendToCallbacks(final boolean isConnected, final NetworkType networkType) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    for (NetworkStatusCallback callback : callbacks) {
                        callback.onNetworkConnected(networkType);
                    }
                } else {
                    for (NetworkStatusCallback callback : callbacks) {
                        callback.onNetworkDisconnected(networkType);
                    }
                }
            }
        });
    }

    private static void registerNetworkCallback() {
        if (connectivityManager == null || networkCallback != null) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                refreshActiveNetworkState();
            }

            @Override
            public void onLost(@NonNull Network network) {
                refreshActiveNetworkState();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                refreshActiveNetworkState();
            }
        };
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void refreshActiveNetworkState() {
        sendStatusChangedToCallbacks();
        boolean wifi = false;
        boolean ethernet = false;
        boolean cellular = false;
        try {
            if (connectivityManager != null) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                NetworkCapabilities capabilities = activeNetwork == null ? null : connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities != null) {
                    wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    ethernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
                    cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateTransportState(NetworkType.WIFI, wifi);
        updateTransportState(NetworkType.ETHERNET, ethernet);
        updateTransportState(NetworkType.CELLULAR, cellular);
    }

    private static void updateApState(boolean enabled) {
        if (apEnabled == enabled) {
            return;
        }
        apEnabled = enabled;
        sendStatusChangedToCallbacks();
        sendToCallbacks(enabled, NetworkType.AP);
    }

    private static void updateTransportState(@NonNull NetworkType networkType, boolean connected) {
        boolean changed;
        switch (networkType) {
            case WIFI:
                changed = wifiConnected != connected;
                wifiConnected = connected;
                break;
            case ETHERNET:
                changed = ethernetConnected != connected;
                ethernetConnected = connected;
                break;
            case CELLULAR:
                changed = cellularConnected != connected;
                cellularConnected = connected;
                break;
            default:
                return;
        }
        if (changed) {
            sendToCallbacks(connected, networkType);
        }
    }

    private static void sendStatusChangedToCallbacks() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (NetworkStatusCallback callback : callbacks) {
                    callback.onNetworkStatusRefreshed();
                }
            }
        });
    }

    private static void registerReceiverCompat(@NonNull Context context, @NonNull BroadcastReceiver receiver, @NonNull IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
    }

    public enum NetworkType {
        WIFI, AP, ETHERNET, CELLULAR
    }

    public interface NetworkStatusCallback {
        /**
         * 当网络状态发生变化时回调
         */
        void onNetworkStatusRefreshed();

        /**
         * 网络连接回调，在主线程
         *
         * @param networkType 参考{@link NetworkType}
         */
        void onNetworkConnected(NetworkType networkType);

        /**
         * 网络断开回调，在主线程
         *
         * @param networkType 参考{@link NetworkType}
         */
        void onNetworkDisconnected(NetworkType networkType);
    }
}
