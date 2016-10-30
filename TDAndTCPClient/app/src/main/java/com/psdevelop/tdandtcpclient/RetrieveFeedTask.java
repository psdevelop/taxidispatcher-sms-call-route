package com.psdevelop.tdandtcpclient;

import android.os.AsyncTask;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by ADMIN on 30.12.2014.
 */
class RetrieveFeedTask extends AsyncTask<String, Void, String> {

    private Exception exception;

    protected String doInBackground(String... urls) {
        return "";
    }

    protected void onPostExecute(String result) {
        // TODO: check this.exception
        // TODO: do something with the feed
    }
}
