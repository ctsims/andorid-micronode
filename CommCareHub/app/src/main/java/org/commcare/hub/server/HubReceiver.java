package org.commcare.hub.server;

import android.content.Context;
import android.util.Base64;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.mirror.SyncableUser;
import org.commcare.hub.util.StreamsUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by ctsims on 12/14/2015.
 */
public class HubReceiver extends NanoHTTPD {

    Context context;

    public HubReceiver() {
        super(8080);
        context = HubApplication._();
    }

    @Override
    public Response serve(IHTTPSession session) {

        try {
            return route(session);
        } catch(AuthRequiredException are) {
            Response authRequest = new Response(Response.Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT, "Authorization Required");
            authRequest.addHeader("WWW-Authenticate", "Basic Realm=\"CommCareMicroNode\"");
            return authRequest;
        }
    }

    private String basicResponse(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, String> parms = session.getParms();
        if (parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }

        return msg;
    }

    private Response route(IHTTPSession session) throws AuthRequiredException{
        String uri = session.getUri();

        if(uri == null ) {
            uri = "";
        }
        if(uri.startsWith("/restore")) {
            SyncableUser user = checkAuth(session);
            String filePath = user.getSyncRecord();
            if(filePath != null) {
                try {
                    return responseWrap(StreamsUtil.loadToString(context.openFileInput(filePath)));
                }catch(IOException ie) {
                    return responseWrap("ERROR: " + ie.getMessage());
                }
            }
        } else if(uri.startsWith("/keys")) {
            SyncableUser user = checkAuth(session);
            String keyFilePath = user.getKeyRecord();
            if(keyFilePath != null) {
                try {
                    return responseWrap(StreamsUtil.loadToString(context.openFileInput(keyFilePath)));
                }catch(IOException ie) {
                    return responseWrap("ERROR: " + ie.getMessage());
                }
            }
        } else if(uri.startsWith("/receiver")) {
            checkAuth(session);
            return responseWrap("<html><body><h1>receiver</h1></body></html>");
        } else if(ManifestApi.handles(uri)) {
            try {
                return responseWrap(ManifestApi.dispatch(uri));
            } catch (IOException e ){
                return responseWrap("ERROR: " + e.getMessage());
            }
        }else if(AppDownloadApi.handles(uri)) {
            try {
                return responseWrap(AppDownloadApi.dispatch(uri));
            } catch (IOException e ){
                return responseWrap("ERROR: " + e.getMessage());
            }
        }
        return responseWrap(basicResponse(session));
    }

    private Response responseWrap(String s) {
        return new Response(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT,s);
    }

    final static String[] possibleHeaderNames = new String[] {"authorization","basic-credentials", "Authorization"};

    private SyncableUser checkAuth(IHTTPSession session)  throws AuthRequiredException{
        String basicCreds = getBasicCredsString(session);
        if(basicCreds == null) {
            throw new AuthRequiredException();
        }
        else {
            SQLiteDatabase database = HubApplication._().getDatabaseHandle();

            String authString = basicCreds.substring("Basic ".length());

            authString = new String(Base64.decode(authString, Base64.DEFAULT));
            String[] components = authString.split(":");

            String username = components[0];

            Cursor c = database.query("SyncableUser", null, null, null, null, null, null);
            SyncableUser toReturn = null;
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                SyncableUser u = SyncableUser.fromDb(c);

                String testUsername = u.getUsername();

                if(testUsername.equals(username) && comparePasswordHashes(u.getPasswordHash(),components[1])) {
                    toReturn = u;
                }
            }
            c.close();
            //database.close();
            if(toReturn != null) {
                return toReturn;
            }
        }
        throw new AuthRequiredException();
    }

    public boolean comparePasswordHashes(String passwordHash, String passwordAttempt) {
        try {
            if (passwordHash.contains("$")) {
                String alg = "sha1";
                String salt = passwordHash.split("\\$")[1];
                String check = passwordHash.split("\\$")[2];
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                BigInteger number = new BigInteger(1, md.digest((salt + passwordAttempt).getBytes()));
                String hashed = number.toString(16);

                while (hashed.length() < check.length()) {
                    hashed = "0" + hashed;
                }
                if (passwordHash.equals(alg + "$" + salt + "$" + hashed)) {
                    return true;
                }
                return false;
            }
            return false;
        }catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String getBasicCredsString(IHTTPSession session) {
        for(String possibleString : possibleHeaderNames) {
            if(session.getHeaders().containsKey(possibleString)) {
                return session.getHeaders().get(possibleString);
            }
        }
        return null;
    }
}
