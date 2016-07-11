package org.commcare.hub.server;

import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.services.HubService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by ctsims on 12/14/2015.
 */
public class ServerService implements HubService {

    public static HubReceiver server = new HubReceiver();

    @Override
    public Status getServiceStatus() {
        if(server.isAlive()) {
            return Status.RUNNING;
        } else {
            return Status.STOPPED;
        }
    }

    @Override
    public void startService() {
        if(!server.isAlive()) {
            try {
                server.start();
                ServicesMonitor.reportMessage("Server now running at: " + getIpAddress() + ":8080");
            } catch (IOException e) {
                e.printStackTrace();
                //TODO: report
            }
        }

    }

    @Override
    public void stopService() {
        if(server.isAlive()) {
            server.stop();
        }
    }

    @Override
    public void restartService() {
        stopService();
        startService();
    }

    public static String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && (inetAddress.getAddress().length == 4)) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
            return "unknown";
        } catch (SocketException e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    public static String getApiRoot() throws IOException{
        String ipAddress = ServerService.getIpAddress();

        if(ipAddress == null) {
            throw new IOException("No valid IP listed for API!");
        }
        return "http://" + ipAddress + ":8080";
    }

}
