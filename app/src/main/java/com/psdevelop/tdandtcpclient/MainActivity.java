package com.psdevelop.tdandtcpclient;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import android.os.Handler;


public class MainActivity extends ActionBarActivity {

    public final static int SHOW_MESSAGE_TOAST = 1;
    public final static int SHOW_SMS_INFO = 2;
    public final static int SHOW_INCALL_INFO = 3;
    public final static int SHOW_OUTCALL_INFO = 4;
    public static final int TDC_PERMISSIONS_REQUEST_READ_CONTACTS = 200;
    public static final int PERMISSION_APP_CODE = 201;
    public static Handler handle;
    static TextView tvInCallInfo;
    static TextView tvSMSSendInfo;
    static TextView tvInitCallInfo;

    public void requestPermissions(String[] PERMISSIONS) {
        ActivityCompat.requestPermissions(this, PERMISSIONS,
                MainActivity.TDC_PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    public void configurePermissionsRequest(boolean denyLocation, boolean denyStorage) {
        if (denyLocation && denyStorage) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.SEND_SMS
            });
        }
        else if (denyLocation) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_PHONE_STATE
            });
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.SEND_SMS
            });
        }
    }

    public void checkPermissions() {
        // Here, thisActivity is the current activity
        final boolean denyLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED,
                denyStorage = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED;
        if (denyLocation || denyStorage) {
            // Should we show an explanation?
            if (    (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE) ||
                    !denyLocation) &&
                    (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.SEND_SMS) ||
                            !denyStorage)  ) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("Внимание")
                        .setMessage("Вами не разрешены доступы к статусу телефона и/или отправке СМС, некоторые функции приложения не будут работать, пока вы не разрешите их!")
                        // кнопка "Yes", при нажатии на которую приложение закроется
                        .setPositiveButton("Ок",
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int whichButton)
                                    {
                                        configurePermissionsRequest(denyLocation, denyStorage);
                                    }
                                })
                        .show();

            } else {

                // No explanation needed, we can request the permission.
                configurePermissionsRequest(denyLocation, denyStorage);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MainActivity.TDC_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (    (grantResults.length == 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        ||
                        (grantResults.length == 2
                                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                                && grantResults[1] == PackageManager.PERMISSION_GRANTED) ) {

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    builder.setTitle("ПРЕДУПРЕЖДЕНИЕ")
                            .setMessage("Вами не разрешены доступы к статусу телефона и/или отправке СМС, ряд функций не будет работать! ")
                            // кнопка "Yes", при нажатии на которую приложение закроется
                            .setPositiveButton("Ок",
                                    new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int whichButton)
                                        {
                                            openApplicationSettings();
                                        }
                                    })
                            .show();
                }
                return;
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void openApplicationSettings() {
        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(appSettingsIntent, MainActivity.PERMISSION_APP_CODE);
        finish();
    }

    public static String systemTimeStamp()	{
        int mHour, mMinute;
        int mYear;
        int mMonth;
        int mDay;

        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        return mDay+"-"+mMonth+"-"+mYear+" "+mHour+":"+mMinute;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        try {
            Intent i = new Intent(getBaseContext(), RouteService.class);
            startService(i);
        } catch(Exception ex)	{
            this.showMyMsg("Ошибка активации основной службы шлюза!");
        }

        handle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.arg1 == MainActivity.SHOW_MESSAGE_TOAST) {
                    showToast(msg.getData().
                            getString("msg_text"));
                }
                else if (msg.arg1 == MainActivity.SHOW_SMS_INFO) {
                    tvSMSSendInfo.setText(systemTimeStamp()+": "+msg.getData().
                            getString("msg_text"));
                }
                else if (msg.arg1 == MainActivity.SHOW_INCALL_INFO) {
                    tvInCallInfo.setText(systemTimeStamp()+": "+msg.getData().
                            getString("msg_text"));
                }
                else if (msg.arg1 == MainActivity.SHOW_OUTCALL_INFO) {
                    tvInitCallInfo.setText(systemTimeStamp()+": "+msg.getData().
                            getString("msg_text"));
                }
            }
        };

        this.registerReceiver(new BroadcastReceiver(){
            @Override
                public void onReceive(Context context, Intent intent)
                {
                    int type=intent.getIntExtra(RouteService.TYPE, -1);
                    switch (type)
                    {
                        case RouteService.ID_ACTION_SHOW_OUTSMS_INFO:
                            try {
                                Message msg = new Message();
                                msg.arg1 = MainActivity.SHOW_SMS_INFO;
                                Bundle bnd = new Bundle();
                                bnd.putString("msg_text", intent.getStringExtra(RouteService.MSG_TEXT));
                                msg.setData(bnd);
                                handle.sendMessage(msg);

                            } catch(Exception ex)	{
                                showMyMsg("Ошибка tvSMSSendInfo.setText: "+ex);
                            }
                            //showToast(intent.getStringExtra(RouteService.MSG_TEXT));
                            break;
                        case RouteService.ID_ACTION_SHOW_INCALL_INFO:
                            try {
                                Message msg = new Message();
                                msg.arg1 = MainActivity.SHOW_INCALL_INFO;
                                Bundle bnd = new Bundle();
                                bnd.putString("msg_text", intent.getStringExtra(RouteService.MSG_TEXT));
                                msg.setData(bnd);
                                handle.sendMessage(msg);

                            } catch(Exception ex)	{
                                showMyMsg("Ошибка tvInCallInfo.setText: "+ex);
                            }
                            // выполнение полученного намерения
                            //context.startService(new Intent(context, PlayService.class));
                            break;
                        case RouteService.ID_ACTION_SHOW_OUTCALL_INFO:
                            try {
                                Message msg = new Message();
                                msg.arg1 = MainActivity.SHOW_OUTCALL_INFO;
                                Bundle bnd = new Bundle();
                                bnd.putString("msg_text", intent.getStringExtra(RouteService.MSG_TEXT));
                                msg.setData(bnd);
                                handle.sendMessage(msg);

                            } catch(Exception ex)	{
                                showMyMsg("Ошибка tvOutCallInfo.setText: "+ex);
                            }
                            // выполнение полученного намерения
                            //context.startService(new Intent(context, PlayService.class));
                            break;
                    }
                }
            }
        , new IntentFilter(RouteService.INFO_ACTION));

    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Intent i = new Intent(getBaseContext(), RouteService.class);
            startService(i);
        } catch(Exception ex)	{
            this.showMyMsg("Ошибка активации основной службы шлюза!");
        }
        checkPermissions();
    }

    public void showMyMsg(String message)   {
        try {
            Toast alertMessage = Toast.makeText(getApplicationContext(),
                    "СООБЩЕНИЕ: "
                            +message, Toast.LENGTH_LONG);
            alertMessage.show();
        } catch(Exception ex)   {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void showToast(String message)   {
        Toast.makeText(getApplicationContext(),
                message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            try	{
                Intent settingsActivity = new Intent(getBaseContext(),
                    SettingsActivity.class);
                startActivity(settingsActivity);
            } catch (Exception e) {
                //e.printStackTrace();
                Toast toastErrorStartActivitySMS = Toast.
                        makeText(getApplicationContext(),
                                "Ошибка вывода настроек! Текст сообщения: "
                                        +e.getMessage()+".", Toast.LENGTH_LONG);
                toastErrorStartActivitySMS.show();
            }
            return true;
        }
        else if(id == R.id.test_db_conn) {
            //new RetrieveFeedTask().execute("");
            new AsyncTask() {
                public void showMessageRequest(String msg_text)	{
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = MainActivity.SHOW_MESSAGE_TOAST;
                    Bundle bnd = new Bundle();
                    bnd.putString("msg_text", msg_text);
                    msg.setData(bnd);
                    handle.sendMessage(msg);
                }

                @Override
                protected Object doInBackground(Object... params) {
                    // TODO Auto-generated method stub
                    try {
                        Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
                        //Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
                        Connection con = null;

                        try {
                            con = DriverManager.getConnection(RouteService.MSSQL_DB,
                                    RouteService.MSSQL_LOGIN, RouteService.MSSQL_PASS);
                            //encrypt=true; trustServerCertificate=false;
                            if (con != null) {
                                java.sql.DatabaseMetaData dm = null;
                                dm = con.getMetaData();
                                showMessageRequest("Соединение успешно! Database Product Name:"+dm.getDatabaseProductName()+
                                    ". Database Version: "+dm.getDatabaseProductVersion()+
                                    ".");
                                try {
                                    //Statement statement = con.createStatement();
                                    //if (statement.execute("EXEC InsertOrderWithParams '','9777264648" +
                                    //        "', -1,0,0,0,-1010,0,0,'',-1,-1")) {
                                    //}
                                } catch (Exception e) {
                                    showMessageRequest("Ошибка тестового запроса! Текст сообщения: "
                                            +e.getMessage());
                                }
                            }   else
                                showMessageRequest("Ошибка соединения!");
                        } catch (Exception e) {
                            showMessageRequest("Ошибка работы с БД! Текст сообщения: "
                                            +e.getMessage());
                        } finally   {
                            try {
                                if (con != null) con.close();
                            } catch (SQLException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }

                    }   catch (Exception e) {
                        showMessageRequest(
                                "Ошибка извлечения класса драйвера! Текст сообщения: "
                                        +e.getMessage()+".");
                    }
                    return null;
                }

                //protected void onPostExecute(Object obj) {
                    // TODO: check this.exception
                    // TODO: do something with the feed
                //}

            }.execute();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            tvInCallInfo = (TextView)rootView.findViewById(R.id.tvInCallInfo);
            tvSMSSendInfo = (TextView)rootView.findViewById(R.id.tvSMSSendInfo);
            tvInitCallInfo = (TextView)rootView.findViewById(R.id.tvInitCallInfo);
            return rootView;
        }
    }

}
