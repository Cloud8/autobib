package org.seaview.opus;

import java.lang.StringBuilder;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.net.MalformedURLException;
import java.util.Base64;
import java.util.Properties;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** ✪ (c) reserved.
    @license http://www.apache.org/licenses/LICENSE-2.0
    @title REST client 
    @date 2015-10-01
*/
public class REST {

    private static final String USER_AGENT = "Seaview/1.1";

    public static String post(String url, Properties header, String data)
    {
        String result = null;
        try {
            URL curl = new URL (url);
            HttpURLConnection conn = (HttpURLConnection) curl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            for(String key : header.stringPropertyNames()) {
                conn.setRequestProperty(key, header.getProperty(key));
            }
            try( DataOutputStream wr =
                 new DataOutputStream(conn.getOutputStream())) {
                 wr.write(data.getBytes("UTF-8"));
            }
            InputStream is = (InputStream)conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            result = sb.toString();
        } catch(UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
        } catch(IOException e) {
            log(e);
        } finally {
            return result;
        }
    }

    public static String post(String url, Properties header, String data, 
                              String user, String pass) {
        String result = null;
        try {
            byte[] postData = data.getBytes("UTF-8");
            URL curl = new URL (url);
            HttpURLConnection conn = (HttpURLConnection) curl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            byte[] bytes = new String(user+":"+pass).getBytes();
            String perm = new String(Base64.getEncoder().encode(bytes));
            conn.setRequestProperty("Authorization", "Basic " + perm);
            for(String key : header.stringPropertyNames()) {
                conn.setRequestProperty(key, header.getProperty(key));
                //System.out.println(key + " => " + header.getProperty(key));
            }
            //conn.setRequestProperty("Content-Type", "application/xml");
            //conn.setRequestProperty("charset", "utf-8");
            try( DataOutputStream wr =
                 new DataOutputStream(conn.getOutputStream())) {
                 wr.write(data.getBytes("UTF-8"));
            }
            InputStream is = (InputStream)conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            result = sb.toString();
        } catch(UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
        } catch(IOException e) {
            log(e);
        } finally {
            return result;
        }
    }

    public static String post(String url, ByteArrayOutputStream baos, 
        String format, String user, String pass) {
        Properties prop = new Properties();
        prop.setProperty("Content-Type", format);
        return post(url, prop, baos, user, pass);
    }

    public static String post(String url, Properties header, 
        ByteArrayOutputStream baos, String user, String pass) {
        String result = null;
        try {
            URL curl = new URL (url);
            HttpURLConnection conn = (HttpURLConnection) curl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            byte[] bytes = new String(user+":"+pass).getBytes();
            String perm = new String(Base64.getEncoder().encode(bytes));
            conn.setRequestProperty("Authorization", "Basic " + perm);
            for(String key : header.stringPropertyNames()) {
                conn.setRequestProperty(key, header.getProperty(key));
                //System.out.println(key + " => " + header.getProperty(key));
            }
            try( DataOutputStream wr =
                 new DataOutputStream(conn.getOutputStream())) {
                baos.writeTo(wr);
                wr.flush();
                wr.close();
            }
            InputStream is = (InputStream)conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            result = sb.toString();
        } catch(UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
        } catch(IOException e) {
            log(e);
        } finally {
            return result;
        }
    }

    public static String post(String url, String data, String user, String pass)
    {
        StringBuilder sb = new StringBuilder();
        try {
            byte[] postData = data.getBytes("UTF-8");
            URL curl = new URL (url);
            HttpURLConnection conn = (HttpURLConnection) curl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            byte[] bytes = new String(user+":"+pass).getBytes();
            String perm = new String(Base64.getEncoder().encode(bytes));
            conn.setRequestProperty("Authorization", "Basic " + perm);
            conn.setRequestProperty("Content-Type", "application/xml");
            conn.setRequestProperty("charset", "utf-8");
            try( DataOutputStream wr =
                 new DataOutputStream(conn.getOutputStream())) {
                 wr.write( postData );
            }
            InputStream is = (InputStream)conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
        } catch(UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
        } catch(IOException e) {
            log(e);
        }
        return sb.toString();
    }

    public static String get(String url) {
        return get(url, null, null);
    }

    public static String get(String url, String user, String pass) {
        return get(url, "application/xml", user, pass);
    }

    public static String get(String url, String type, String user, String pass)
    {
        StringBuilder sb = new StringBuilder();
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setRequestProperty("Accept-Charset", "utf-8");
            ((HttpURLConnection)conn).setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", type);
            if (user!=null && pass!=null) {
                byte[] bytes = new String(user+":"+pass).getBytes();
                String perm = new String(Base64.getEncoder().encode(bytes));
                conn.setRequestProperty("Authorization", "Basic " + perm);
            }
            InputStream is = conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
        } catch(UnsupportedEncodingException e) { log(e); }
          catch(IOException e) { log(e); }
          finally {
            return sb.toString();
        }
    }

    public static String delete(String url, String user, String pass) {
        StringBuilder sb = new StringBuilder();
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setRequestProperty("Accept-Charset", "utf-8");
            ((HttpURLConnection)conn).setRequestMethod("DELETE");
            if (user!=null && pass!=null) {
                byte[] bytes = new String(user+":"+pass).getBytes();
                String perm = new String(Base64.getEncoder().encode(bytes));
                conn.setRequestProperty("Authorization", "Basic " + perm);
            }
            InputStream is = conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
        } catch(UnsupportedEncodingException e) { log(e); }
          catch(IOException e) { log(e); }
          finally {
              return sb.toString();
        }
    }

    private String getHeader(URLConnection conn) {
        StringBuilder sb = new StringBuilder();
        Map response = conn.getHeaderFields();
        for (Iterator it = response.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            List values = (List) response.get(key);
            sb.append(key + ": ");
            for (int i = 0; i < values.size(); i++) {
                Object o = values.get(i);
                sb.append(o + ", ");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** returns (may be redirected) URL */
    public static String head(String url) {
        StringBuilder sb = new StringBuilder();
        try {
            URLConnection conn = new URL(url).openConnection();
            ((HttpURLConnection)conn).setRequestMethod("HEAD");
            conn.connect();
            InputStream is = conn.getInputStream();
			sb.append(conn.getURL());
            is.close();
        } catch(UnsupportedEncodingException e) { log(e); }
          catch(IOException e) { log(e); }
          finally {
              return sb.toString();
        }
    }

    public static int head(String url, String user, String pass) {
        int status = 0;
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setRequestProperty("Accept-Charset", "utf-8");
            ((HttpURLConnection)conn).setRequestMethod("HEAD");
            if (user!=null && pass!=null) {
                byte[] bytes = new String(user+":"+pass).getBytes();
                String perm = new String(Base64.getEncoder().encode(bytes));
                conn.setRequestProperty("Authorization", "Basic " + perm);
            }
            status = ((HttpURLConnection)conn).getResponseCode();
        } catch(UnsupportedEncodingException e) { log(e); }
          catch(IOException e) { log(e); }
          finally {
            return status;
        }
    }

    // curl -i -X PUT "http://localhost:8080/fcrepo/rest/node/to/create"
    public static String put(String url, String user, String pass) {
        StringBuilder sb = new StringBuilder();
        try {
            URLConnection conn = new URL(url).openConnection();
            ((HttpURLConnection)conn).setRequestMethod("PUT");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            if (user!=null && pass!=null) {
                byte[] bytes = new String(user+":"+pass).getBytes();
                String perm = new String(Base64.getEncoder().encode(bytes));
                conn.setRequestProperty("Authorization", "Basic " + perm);
            }
            InputStream is = conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
        } catch(UnsupportedEncodingException e) { log(e); }
          catch(IOException e) { log(e); }
          finally {
            return sb.toString();
        }
    }

    public static String put(String url, ByteArrayOutputStream baos, 
        String format, String user, String pass) {
        StringBuilder sb = new StringBuilder();
        try {
            byte[] bytes = baos.toByteArray();
            if (format.equals("video/mp4"))
                log("put " + url + " " + format + " " + bytes.length);
            URLConnection conn = new URL(url).openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            ((HttpURLConnection)conn).setRequestMethod("PUT");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept-Charset", "utf-8");
            conn.setRequestProperty("Content-Type", format);
            conn.setRequestProperty("Content-length", "" + bytes.length);
            ((HttpURLConnection)conn).setChunkedStreamingMode( 0 );
            if (user!=null && pass!=null) {
                byte[] pbytes = new String(user+":"+pass).getBytes();
                String perm = new String(Base64.getEncoder().encode(pbytes));
                conn.setRequestProperty("Authorization", "Basic " + perm);
            }
            try( DataOutputStream wr =
                 new DataOutputStream(conn.getOutputStream())) {
                 wr.write( bytes );
            }
            InputStream is = conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
        } catch(UnsupportedEncodingException e) { log(e); }
          catch(IOException e) { log(e); }
          finally {
            return sb.toString();
        }
    }

    private static Logger logger = Logger.getLogger(REST.class.getName());

    private static void log(String msg) {
        logger.info(msg);
    }

    private static void log(Exception e) {
        log(e.toString());
        // e.printStackTrace();
        try {
             throw(e);
        } catch(Exception ex) {}
    }

}

