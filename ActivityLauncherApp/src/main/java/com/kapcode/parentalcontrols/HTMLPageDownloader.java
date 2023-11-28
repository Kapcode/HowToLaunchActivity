package com.kapcode.parentalcontrols;


import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpResponse;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.DefaultHttpClient;

//changed minsdk level from 19 to 21 because of dex issues. for more infor  go here https://developer.android.com/build/multidex
//
//hit limit by importing com.google.firebase.crashlytics.buildtools.reloc.org.apache.http*

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

//Source - user1248568 https://stackoverflow.com/questions/15940114/android-download-html-and-convert-it-to-string
public class HTMLPageDownloader {
    public static interface HTMLPageDownloaderListener {
        public abstract void completionCallBack(String html);
    }
    public HTMLPageDownloaderListener listener;
    public String link;
    public static String downloadHtml (String aLink) {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(aLink);
        String html = "";
        try {
            HttpResponse response = client.execute(request);
            InputStream in;
            in = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in));
            StringBuilder str = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                str.append(line);
            }
            in.close();
            html = str.toString();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return html;


    }




}
