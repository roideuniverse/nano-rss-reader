package com.example.xyzreader.remote;

import android.util.Log;

import com.squareup.okhttp.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

public class RemoteEndpointUtil
{
    private static final String TAG = "RemoteEndpointUtil";

    private RemoteEndpointUtil()
    {
    }

    public static JSONArray fetchJsonArray()
    {
        String itemsJson = null;
        try
        {
            itemsJson = fetchPlainText(Config.BASE_URL);
        }
        catch(IOException e)
        {
            Log.e(TAG, "Error fetching items JSON", e);
            return null;
        }

        // Parse JSON
        try
        {
            JSONTokener tokener = new JSONTokener(itemsJson);
            Object val = tokener.nextValue();
            if(! (val instanceof JSONArray))
            {
                throw new JSONException("Expected JSONArray");
            }
            return (JSONArray) val;
        }
        catch(JSONException e)
        {
            Log.e(TAG, "Error parsing items JSON", e);
        }

        return null;
    }

    static String fetchPlainText(URL url) throws IOException
    {
        String data = new String(fetch(url), "UTF-8");
        Log.d(TAG, "Fetch Complete");
        return data;
    }

    static byte[] fetch(URL url) throws IOException
    {
        InputStream in = null;

        try
        {
            OkHttpClient client = new OkHttpClient();
            SSLContext sslContext;
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, null);
            } catch (GeneralSecurityException e) {
                throw new AssertionError(); // The system has no TLS. Just give up.
            }
            client.setSslSocketFactory(sslContext.getSocketFactory());

            Log.d(TAG, "StartingFetch::" + url);
            HttpURLConnection conn = client.open(url);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while((bytesRead = in.read(buffer)) > 0)
            {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();

        }
        finally
        {
            if(in != null)
            {
                in.close();
            }
        }
    }
}
