package org.commcare.hub.nsd;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;

import org.commcare.hub.services.HubService;

/**
 * Created by ctsims on 12/14/2015.
 */
public class NsdHubService implements HubService {

    Context context;
    Status status = Status.WAITING;

    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String SERVICE_NAME = "commcare_micronode";

    public NsdHubService(Context context) {
        this.context = context;
    }

    @Override
    public Status getServiceStatus() {
        return status;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startService() {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(8080);

        NsdManager mNsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, new NsdManager.RegistrationListener() {

                    @Override
                    public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                        // Save the service name.  Android may have changed it in order to
                        // resolve a conflict, so update the name you initially requested
                        // with the name Android actually used.
                        //mServiceName = NsdServiceInfo.getServiceName();
                        NsdHubService.this.status = Status.RUNNING;
                    }

                    @Override
                    public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        // Registration failed!  Put debugging code here to determine why.
                    }

                    @Override
                    public void onServiceUnregistered(NsdServiceInfo arg0) {
                        // Service has been unregistered.  This only happens when you call
                        // NsdManager.unregisterService() and pass in this listener.
                    }

                    @Override
                    public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        // Unregistration failed.  Put debugging code here to determine why.
                    }
                });
    }

    @Override
    public void stopService() {

    }

    @Override
    public void restartService() {

    }
}
