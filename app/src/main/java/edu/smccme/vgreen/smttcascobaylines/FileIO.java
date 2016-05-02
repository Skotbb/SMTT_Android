package edu.smccme.vgreen.smttcascobaylines;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

//import com.google.transit.realtime.GtfsRealtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by vgreen on 3/21/16.  Modified from Murach's Android Programming, chapter 11
 */
public class FileIO {
    private Context m_context;

    public FileIO(Context context) {
        m_context = context;
    }

    // try to download the file.  Throw an exception if it's not possible to connect.
    public String downloadFile(String url) throws ConnectException {
        String fileContents = "";

        // get NetworkInfo object
        ConnectivityManager cm = (ConnectivityManager) m_context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        if (ni != null && ni.isConnected()) {

            try {
                // form the URL
                URL downloadURL = new URL(url);

                // input stream
                InputStream in = downloadURL.openStream();

                StringBuffer buf = new StringBuffer();

                // read input into buffer until done.
                byte[] bytes = new byte[1024];
                int bytesRead = in.read(bytes);
                while (bytesRead != -1) {
                    buf.append(new String(bytes, 0, bytesRead));
                    bytesRead = in.read(bytes);
                }
                in.close();
                // here you go!
                fileContents = buf.toString();
            }
            catch (IOException e) {
                Log.e(FileIO.class.toString(), e.toString());
            }
        }
        else {
            throw new ConnectException("no connectivity");
        }

        return fileContents;
    }

    // vgreen: the following code works but we may not need it (because the API
    // can return JSON instead, for the vehicle updates)
/*    public GtfsRealtime.FeedMessage readRealtimeFeed(String urlString) throws ConnectException {
        GtfsRealtime.FeedMessage msg = null;

        try {
            URL url = new URL(urlString);

            try {
                msg = GtfsRealtime.FeedMessage.parseFrom(url.openStream());
            } catch (IOException e) {
                Log.d(FileIO.class.toString(), e.getMessage());
            }
        }catch (MalformedURLException e) {
            Log.d(FileIO.class.toString(), e.getMessage());
        }

        return msg;

    }
    */


}
