package com.github.ghmxr.ftpshare.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

public class NetworkEnvironmentUtil {
    /**
     * 获取WiFi网络的IPv4地址
     *
     * @return 可能为null
     */
    public static @Nullable
    String getWifiIp(@NonNull Context context) {
        return getIpv4AddressForTransport(context, NetworkCapabilities.TRANSPORT_WIFI);
    }

    /**
     * 获取本地网络环境所有可用的IPv4地址
     *
     * @return 类似"192.168.1.101"的IP地址集
     */
    public static ArrayList<String> getLocalIpv4Addresses() {
        final ArrayList<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            while ((networkInterface = networkInterfaces.nextElement()) != null) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                try {
                    InetAddress inetAddress;
                    while ((inetAddress = inetAddresses.nextElement()) != null) {
                        if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                            String ip = inetAddress.getHostAddress();
                            if (!result.contains(ip)) result.add(ip);
                        }
                    }
                } catch (Exception e) {
                    //
                }
            }
        } catch (Exception e) {
            //
        }
        return result;
    }

    public static boolean isWifiConnected(@NonNull Context context) {
        return hasTransportOnActiveNetwork(context, NetworkCapabilities.TRANSPORT_WIFI);
    }

    static boolean isCellularNetworkConnected(@NonNull Context context) {
        return hasTransportOnActiveNetwork(context, NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    private static boolean hasTransportOnActiveNetwork(@NonNull Context context, int transport) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) return false;
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null && capabilities.hasTransport(transport);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static @Nullable
    String getIpv4AddressForTransport(@NonNull Context context, int transport) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) return null;
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return null;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities == null || !capabilities.hasTransport(transport)) return null;
            LinkProperties linkProperties = connectivityManager.getLinkProperties(activeNetwork);
            if (linkProperties == null) return null;
            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                InetAddress address = linkAddress.getAddress();
                if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                    return address.getHostAddress();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
