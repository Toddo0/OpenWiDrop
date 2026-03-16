package com.toddo.openwidrop;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

public class NetworkUtils {

    public static String getLocalIp(Context context) {

        WifiManager wm = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        int ip = wm.getConnectionInfo().getIpAddress();

        return Formatter.formatIpAddress(ip);
    }
}