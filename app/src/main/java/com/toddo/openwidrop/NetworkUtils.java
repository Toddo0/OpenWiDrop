package com.toddo.openwidrop;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class NetworkUtils {
    public static void logAllNetworkInterfaces() {
        try {
            java.util.List<NetworkInterface> interfaces = java.util.Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : interfaces) {
                // Sadece aktif olan arayüzleri al
                if (!ni.isUp()) continue;

                for (InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                    // Sadece IPv4 ve Loopback olmayan (127.0.0.1 hariç) adresleri yazdır
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        android.util.Log.d("OpenWiDrop_Network",
                                "Arayüz İsmi: " + ni.getName() + " ---> IP: " + addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getLocalIp() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface ni : interfaces) {
                // Kapalı, loopback veya sanal arayüzleri atla
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }

                String name = ni.getName().toLowerCase();

                // Hücresel veri (mobil ağ - rmnet) ve dummy arayüzlerini doğrudan atla
                if (name.contains("rmnet") || name.contains("dummy")) {
                    continue;
                }

                // Cihaz markalarına göre değişebilen Wi-Fi ve Hotspot arayüz isimleri
                boolean isWifiOrHotspot = name.contains("wlan") ||
                        name.contains("ap") ||
                        name.contains("swlan") ||
                        name.contains("softap");

                if (!isWifiOrHotspot) {
                    continue;
                }

                List<InetAddress> addresses = Collections.list(ni.getInetAddresses());
                for (InetAddress addr : addresses) {
                    // Sadece IPv4 ve loopback olmayan adresleri al
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {

                        // isSiteLocalAddress(); 192.168.x.x, 10.x.x.x ve 172.16.x.x gibi
                        // yerel ağ IP bloklarını otomatik olarak tanır.
                        if (addr.isSiteLocalAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "IP bulunamadı";
    }
}