package com.snapyr.sdk.inapp;

import com.snapyr.sdk.http.ConnectionFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

public class AckActionRequest  {
    public static final String AckInappActionUrl = "/v1/actions";
    static void execute(String token) throws IOException {
        String builtUrl = AckInappActionUrl + "?actionToken="+token;
        HttpURLConnection conn = ConnectionFactory.getInstance().engineRequest(builtUrl, "POST");
    }
}
