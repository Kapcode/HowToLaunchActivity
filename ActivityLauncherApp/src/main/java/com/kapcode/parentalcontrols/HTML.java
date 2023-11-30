package com.kapcode.parentalcontrols;

import java.io.InputStream;
import java.net.URL;

public class HTML {
    public static String downloadHTML(String surl,long timeout) throws InterruptedException {
        final String[] content = new String[1];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(surl);
                    InputStream is = url.openStream();
                    int ptr = 0;
                    StringBuffer buffer = new StringBuffer();
                    while ((ptr = is.read()) != -1) {
                        buffer.append((char)ptr);
                    }
                    content[0]=buffer.toString();
                }catch ( Exception ex ) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();
        t.join(timeout);
        return content[0];
    }
}
