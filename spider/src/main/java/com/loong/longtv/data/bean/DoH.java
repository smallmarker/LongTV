package com.loong.longtv.data.bean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * DoH (DNS over HTTPS) configuration shim — used by OkHttp.java in the catvod net layer.
 */
public class DoH {
    private final String url;
    private final List<String> ips;

    public DoH(String url, List<String> ips) {
        this.url = url != null ? url : "";
        this.ips = ips != null ? ips : new ArrayList<>();
    }

    public String getUrl() {
        return url;
    }

    /** Returns bootstrap DNS hosts as InetAddress list, may be empty. */
    public List<InetAddress> getHosts() {
        List<InetAddress> result = new ArrayList<>();
        for (String ip : ips) {
            try {
                result.add(InetAddress.getByName(ip));
            } catch (UnknownHostException ignored) {
            }
        }
        return result;
    }
}
