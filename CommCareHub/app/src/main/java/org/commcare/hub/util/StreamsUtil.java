package org.commcare.hub.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ctsims on 12/14/2015.
 */
public class StreamsUtil {
    /**
     * Write is to os and close both
     */
    public static void writeFromInputToOutput(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        long counter = 0;

        try {
            int count = is.read(buffer);
            while(count != -1) {
                counter += count;
                os.write(buffer, 0, count);
                count = is.read(buffer);
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                os.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public static String loadToString(InputStream input) throws IOException{
        InputStream is = new BufferedInputStream(input);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamsUtil.writeFromInputToOutput(is, baos);
        return new String(baos.toByteArray());
    }


}
