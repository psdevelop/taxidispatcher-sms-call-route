package com.psdevelop.tdandtcpclient;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RouteService extends Service {

    public static final String PHONE = "phone";
    public static final String SMS_TEXT = "sms_text";
    public static final String INFO_ACTION = "com.psdevelop.tdandtcpclient.INFO_ACTION";
    public static final String TYPE = "type";
    public static final String MSG_TEXT = "msg_text";
    public static final int ID_ACTION_SHOW_OUTSMS_INFO = 0;
    public static final int ID_ACTION_SHOW_INCALL_INFO = 1;
    public static final int ID_ACTION_SHOW_OUTCALL_INFO = 9;

    public final static int PROCESS_SMS_DATA = 5;
    public final static int RECEIVER_IS_DRIVER = 2;
    public final static int RECEIVER_IS_CLIENT = 3;
    public final static int CHECK_WAITING_SMS = 4;
    public final static int INSERT_DETECT_NUM = 6;
    public final static int PROCESS_CALL_DATA = 7;
    public final static int CHECK_CALLIT_ORDER = 8;
    static String MSSQL_HOST = "192.168.0.50";
    static String MSSQL_DBNAME = "TD5R1";
    static String MSSQL_INSTNAME = "SQLEXPRESS";
    static int MSSQL_PORT = 1433;
    static String MSSQL_DB = "jdbc:jtds:sqlserver://"+MSSQL_HOST+":"+MSSQL_PORT+"/"+MSSQL_DBNAME+";instance="+MSSQL_INSTNAME;
    static String MSSQL_LOGIN = "sa";
    static String MSSQL_PASS= "sadba";
    public static Handler handle;
    public static String DRV_SMS_TEXT="Заказ ***___msg_text";
    public static String START_ORD_CLSMS_TEXT="К вам выехала машина ***___msg_text";
    public static String ONPLACE_CLIENT_SMS_TEMPLATE="Вас ожидает такси ***___msg_text";
    public static String WAIT_CLIENT_SEND_TEMPLATE="(вр. приб. ***___tval мин.)";
    public static String REPORT_CLSMS_TEXT="Ваш заказ составил ***___msg_text";
    SMSCheckTimer smsCheckTimer=null;
    CallItCheckTimer callItCheckTimer=null;

    public static boolean ENABLE_SMS_NOTIFICATIONS=false;
    public static boolean ENABLE_DRIVER_ORDER_SMS=false;
    public static boolean ENABLE_MOVETO_CLIENT_SMS=false;
    public static boolean ENABLE_ONPLACE_CLIENT_SMS=false;
    public static boolean ENABLE_WAIT_CLIENT_SEND=false;
    public static boolean ENABLE_REPORT_CLIENT_SMS=false;
    public static boolean ENABLE_INCALL_DETECTING=false;
    public static boolean ENABLE_SMS_MAILING=false;
    public static boolean ENABLE_AUTO_CALLING=false;
    public static boolean ENABLE_CALLING=false;
    static int CALL_DEVICE_NUM = 0;
    public static String PHONE_CODE = "+7";
    public static String CURRENCY_SHORT = "руб.";
    public static boolean ALT_FIX_DETECTING=false;

    static SharedPreferences prefs=null;
    PowerManager.WakeLock wakeLock;

    public RouteService() {
    }

    public void sendInfoBroadcast(int action_id, String message) {
        Intent intent = new Intent(INFO_ACTION);
        intent.putExtra(TYPE, action_id);
        intent.putExtra(MSG_TEXT, message);
        sendBroadcast(intent);
    }

    public static boolean checkString(String str) {
        try {
            Integer.parseInt(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static int strToIntDef(String str_int, int def) {
        int res = def;

        if (checkString(str_int)) {
            res = Integer.parseInt(str_int);
        }

        return res;
    }

    public void reloadPrefs()    {
        try {
        if(prefs!=null) {
            ENABLE_SMS_NOTIFICATIONS = prefs.getBoolean("ENABLE_SMS_NOTIFICATIONS", false);
            ENABLE_DRIVER_ORDER_SMS = prefs.getBoolean("ENABLE_DRIVER_ORDER_SMS", false);
            ENABLE_MOVETO_CLIENT_SMS = prefs.getBoolean("ENABLE_MOVETO_CLIENT_SMS", false);
            ENABLE_ONPLACE_CLIENT_SMS = prefs.getBoolean("ENABLE_ONPLACE_CLIENT_SMS", false);
            ENABLE_WAIT_CLIENT_SEND = prefs.getBoolean("ENABLE_WAIT_CLIENT_SEND", false);
            ENABLE_REPORT_CLIENT_SMS = prefs.getBoolean("ENABLE_REPORT_CLIENT_SMS", false);
            ENABLE_INCALL_DETECTING = prefs.getBoolean("ENABLE_INCALL_DETECTING", false);
            ALT_FIX_DETECTING = prefs.getBoolean("ALT_FIX_DETECTING", false);
            ENABLE_SMS_MAILING = prefs.getBoolean("ENABLE_SMS_MAILING", false);
            ENABLE_AUTO_CALLING = prefs.getBoolean("ENABLE_AUTO_CALLING", false);
            ENABLE_CALLING = prefs.getBoolean("ENABLE_CALLING", false);
            CALL_DEVICE_NUM = strToIntDef(prefs.getString("CALL_DEVICE_NUM", "0"), 0);
            PHONE_CODE = prefs.getString("PHONE_CODE", "+7");
            CURRENCY_SHORT =  prefs.getString("CURRENCY_SHORT", "руб.");
            DRV_SMS_TEXT = prefs.getString("DRIVER_ORDER_SMS_TEMPLATE", "Заказ ***___msg_text");
            START_ORD_CLSMS_TEXT = prefs.getString("MOVETO_CLIENT_SMS_TEMPLATE", "К вам выехала машина ***___msg_text");
            ONPLACE_CLIENT_SMS_TEMPLATE = prefs.getString("ONPLACE_CLIENT_SMS_TEMPLATE", "Вас ожидает такси ***___msg_text");
            WAIT_CLIENT_SEND_TEMPLATE = prefs.getString("WAIT_CLIENT_SEND_TEMPLATE", "(вр. приб. ***___tval мин.)");
            REPORT_CLSMS_TEXT = prefs.getString("REPORT_CLIENT_SMS_TEMPLATE", "Ваш заказ составил ***___msg_text");
            MSSQL_HOST = prefs.getString("DB_HOST_NAME", "192.168.0.1");
            MSSQL_DBNAME = prefs.getString("DATABASE_NAME", "TD5R1");
            MSSQL_INSTNAME = prefs.getString("DBSRV_INSTANCE_NAME", "SQLEXPRESS");
            MSSQL_PORT = strToIntDef(prefs.getString("DBSERVER_PORT", "1433"),1433);
            MSSQL_LOGIN = prefs.getString("DBSERVER_LOGIN", "sa");
            MSSQL_PASS = prefs.getString("DBSERVER_PASSWORD", "sadba");
            MSSQL_DB = "jdbc:jtds:sqlserver://" + MSSQL_HOST + ":" + MSSQL_PORT + "/" + MSSQL_DBNAME + ";instance=" + MSSQL_INSTNAME;
        }
        } catch (Exception e) {
            //Toast.makeText(getBaseContext(),
            //        "Ошибка извлечения настроек! Текст сообщения: "
            //                +e.getMessage()+".", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate() {

        prefs = PreferenceManager.
                getDefaultSharedPreferences(this);
        reloadPrefs();

        handle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.arg1 == MainActivity.SHOW_MESSAGE_TOAST) {
                    showToast(msg.getData().
                            getString("msg_text"));
                } else if (msg.arg1 == PROCESS_SMS_DATA) {
                    showToast("Sending SMS Signal! phone="+msg.getData().
                            getString(PHONE));
                    startMessageServiceIntent(getBaseContext(),msg.getData().
                            getString(SMS_TEXT),msg.getData().
                            getString(PHONE));
                } else if (msg.arg1 == CHECK_WAITING_SMS) {
                    checkWaitingSMS();
                } else if (msg.arg1 == CHECK_CALLIT_ORDER) {
                    checkCallIt();
                } else if (msg.arg1 == INSERT_DETECT_NUM) {
                    insertDetectNumberIntoDB(msg.getData().
                            getString(PHONE));
                }
                else if (msg.arg1 == PROCESS_CALL_DATA) {
                    try	{
                        Intent dialIntent = new Intent(Intent.ACTION_CALL,
                                Uri.parse("tel:" + (msg.getData().
                                        getString(PHONE).length() > 10 ? "" : PHONE_CODE) + msg.getData().
                                        getString(PHONE)));
                        dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(dialIntent);
                    } catch(Exception cex)	{
                        showToast(
                                "Ошибка набора номера!"+cex.getMessage());
                    }
                }
            }
        };

        PowerManager powerManager = (PowerManager)
                getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock
                (PowerManager.FULL_WAKE_LOCK//PARTIAL_WAKE_LOCK
                        , "No sleep");
        wakeLock.acquire();

        this.registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                //try {
                //	Thread.sleep(5000);
                //} catch (InterruptedException e) {
                // TODO Auto-generated catch block
                //	e.printStackTrace();
                //}
                //SMSSendService.smsWait=false;
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:

                        try	{
                            //Toast.makeText(getBaseContext(), "SMS отправлена " +
                                            //SMSSendService.LAST_SENT_RESULT_PHONE + "(" +
                                            //SMSSendService.LAST_SENT_RESULT_TEXT + ")",
                                    //Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "Ошибка обработки события завершения отправки! Текст сообщения: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        try	{
                            //insertLog("Отказ отпр. "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "Отказ",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "Ошибка обработки события отказа отправки! Текст сообщения: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        try	{
                            //insertLog("Нет услуги "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "Нет услуги SMS",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "Ошибка обработки события Нет услуги SMS! Текст сообщения: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        try	{
                            //insertLog("Null PDU "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "Null PDU",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "Ошибка обработки события Null PDU! Текст сообщения: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        try	{
                            //insertLog("Нет связи "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "Нет связи",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "Ошибка обработки события Нет связи! Текст сообщения: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                }
            }
        }, new IntentFilter(SMSSender.INTENT_MESSAGE_SENT));

        //---when the SMS has been delivered---
        this.registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "Доставлено",
                                Toast.LENGTH_LONG).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        try	{
                            //insertLog("Не доставлено "+SMSSendService.LAST_SENT_RESULT_PHONE+
                            //        "("+SMSSendService.LAST_SENT_RESULT_TEXT+")");
                            Toast.makeText(getBaseContext(), "Не доставлено",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(),
                                    "Ошибка обработки события Не доставлено! Текст сообщения: "
                                            +e.getMessage()+".", Toast.LENGTH_LONG).show();
                        }
                        break;
                }
            }
        }, new IntentFilter(SMSSender.INTENT_MESSAGE_DELIVERED));

        smsCheckTimer = new SMSCheckTimer(this);
        callItCheckTimer = new CallItCheckTimer(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //wakeLock.release();
        return super.onUnbind(intent);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        showToast("Перезагружаем настройки!");
        reloadPrefs();
        showNotification("Упр. шлюз", "Запущена основная служба шлюза!");
        sendInfoBroadcast( ID_ACTION_SHOW_OUTSMS_INFO, "Запущена основная служба шлюза!");
        if(ENABLE_INCALL_DETECTING)
            sendInfoBroadcast( ID_ACTION_SHOW_INCALL_INFO, "Ожидание входящих вызовов...");
        else
            sendInfoBroadcast( ID_ACTION_SHOW_INCALL_INFO, "Определение входящих вызовов запрещено!");
        if(ENABLE_CALLING)
            sendInfoBroadcast( ID_ACTION_SHOW_OUTCALL_INFO, "Ожидание набора номера...");
        else
            sendInfoBroadcast( ID_ACTION_SHOW_OUTCALL_INFO, "Набор номеров запрещен!");
        //startMessageServiceIntent(getBaseContext(),"ля ля ля, привет","+79183120588");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        //unregisterReceiver(messageSent);
        //unregisterReceiver(messageDelivered );
        super.onDestroy();
    }

    public void showToast(String message)   {
        Toast.makeText(getBaseContext(),
                message, Toast.LENGTH_LONG).show();
    }

    public void showNotification(String title, String msg_txt)    {
        // prepare intent which is triggered if the
        // notification is selected
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(msg_txt);
        int NOTIFICATION_ID = 12345;

        Intent targetIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(NOTIFICATION_ID, builder.build());
        /*Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification n  = new Notification.Builder(this)
                .setContentTitle("New mail from " + "test@gmail.com")
                .setContentText("Subject")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_launcher, "Call", pIntent)
                .addAction(R.drawable.ic_launcher, "More", pIntent)
                .addAction(R.drawable.ic_launcher, "And more", pIntent).build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, n);*/
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startMessageServiceIntent(Context context, String message, String receiver) {
        Intent i = new Intent(context, SMSSender.class);
        sendInfoBroadcast( ID_ACTION_SHOW_OUTSMS_INFO, "SMS Sending: PHONE="+receiver+",TEXT="+message);
        i.putExtra(SMSSender.EXTRA_MESSAGE, message);
        i.putExtra(SMSSender.EXTRA_RECEIVERS, new String[] { receiver });
        startService(i);
    }

    public void performDial(String phone){
        if(phone!=null){
            try {
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone)));
            } catch (Exception e) {
                //e.printStackTrace();
                Toast toastErrorStartCallActivity = Toast.makeText(this,
                        "Ошибка осуществления голосового вызова! Текст сообщения: "
                                +e.getMessage()+".", Toast.LENGTH_LONG);
                toastErrorStartCallActivity.show();
            }
        }
    }

    public void checkCallIt()   {
        if (ENABLE_CALLING) {
            new AsyncTask() {
                public void showMessageRequest(String msg_text) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = MainActivity.SHOW_MESSAGE_TOAST;
                    Bundle bnd = new Bundle();
                    bnd.putString("msg_text", msg_text);
                    msg.setData(bnd);
                    handle.sendMessage(msg);
                }

                public void callPhoneNumRequest(String phone) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = PROCESS_CALL_DATA;
                    Bundle bnd = new Bundle();
                    bnd.putString(PHONE, phone);
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
                            con = DriverManager.getConnection(MSSQL_DB, MSSQL_LOGIN, MSSQL_PASS);
                            //encrypt=true; trustServerCertificate=false;
                            if (con != null) {

                                Statement statement = con.createStatement();
                                String queryString = "select * from CallItOrders";
                                ResultSet rs = statement.executeQuery(queryString);

                                while (rs.next()) {
                                    int orderId = rs.getInt("BOLD_ID");
                                    String client_phone = rs.getString("Telefon_klienta");
                                    String order_adres = rs.getString("Adres_vyzova_vvodim");
                                    int dev_num = rs.getInt("dev_num");

                                    if (client_phone.length() == 10&&(
                                            ((CALL_DEVICE_NUM>0)&&(dev_num==CALL_DEVICE_NUM))
                                            ||(CALL_DEVICE_NUM<=0)
                                            )
                                            ) {
                                            //showMessageRequest("RESET DRIVER_SMS_SEND_STATE=2 phone=" +
                                            //        phone + " text=" + sms_text);
                                            if (statement.execute("update Zakaz set call_it=0 where BOLD_ID=" + orderId)) {

                                            }
                                        callPhoneNumRequest(client_phone);
                                    }

                                    //}

                                }

                            } else
                                showMessageRequest("Ошибка соединения!");
                        } catch (Exception e) {
                            showMessageRequest("Ошибка работы с БД! Текст сообщения: "
                                    + e.getMessage());
                        } finally {
                            try {
                                if (con != null) con.close();
                            } catch (SQLException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        showMessageRequest(
                                "Ошибка извлечения класса драйвера! Текст сообщения: "
                                        + e.getMessage() + ".");
                    }
                    return null;
                }

                //protected void onPostExecute(Object obj) {
                // TODO: check this.exception
                // TODO: do something with the feed
                //}

            }.execute();
        }   else    {
            //sendInfoBroadcast( ID_ACTION_SHOW_OUTSMS_INFO, "SMS отправка запрещена!");
        }
    }

    public void checkWaitingSMS()   {
        if (ENABLE_SMS_NOTIFICATIONS&&(ENABLE_DRIVER_ORDER_SMS||
                ENABLE_MOVETO_CLIENT_SMS||ENABLE_REPORT_CLIENT_SMS
        ||ENABLE_ONPLACE_CLIENT_SMS)) {
            new AsyncTask() {
                public void showMessageRequest(String msg_text) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = MainActivity.SHOW_MESSAGE_TOAST;
                    Bundle bnd = new Bundle();
                    bnd.putString("msg_text", msg_text);
                    msg.setData(bnd);
                    handle.sendMessage(msg);
                }

                public void sendSMSRequest(String sms_text, String phone) {
                    //this.showMyMsg("sock show timer");
                    Message msg = new Message();
                    //msg.obj = this.mainActiv;
                    msg.arg1 = PROCESS_SMS_DATA;
                    Bundle bnd = new Bundle();
                    bnd.putString(SMS_TEXT, sms_text);
                    bnd.putString(PHONE, phone);
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
                            con = DriverManager.getConnection(MSSQL_DB, MSSQL_LOGIN, MSSQL_PASS);
                            //encrypt=true; trustServerCertificate=false;
                            if (con != null) {

                                Statement statement = con.createStatement();
                                String queryString = "select * from SMSSendOrders";
                                ResultSet rs = statement.executeQuery(queryString);

                                while (rs.next()) {
                                    String phone = rs.getString("SMS_SEND_DRNUM");
                                    int RECEIVER_TYPE = -1;
                                    String sms_text = DRV_SMS_TEXT;
                                    int orderId = rs.getInt("BOLD_ID");
                                    int waitTime = rs.getInt("WAITING");
                                    double order_summ = rs.getDouble("Uslovn_stoim");
                                    String client_phone = rs.getString("Telefon_klienta");
                                    String order_adres = rs.getString("Adres_vyzova_vvodim");
                                    String CLIENT_ORDER_INFO = rs.getString("CLIENT_ORDER_INFO");

                                        if ((rs.getInt("DRIVER_SMS_SEND_STATE") == 1)&&ENABLE_DRIVER_ORDER_SMS&&
                                                (DRV_SMS_TEXT.length()>5)) {
                                            if (phone.length() >= 5) {
                                                RECEIVER_TYPE = RECEIVER_IS_DRIVER;
                                                showMessageRequest("RESET DRIVER_SMS_SEND_STATE=2 phone=" +
                                                        phone + " text=" + sms_text);
                                                if (statement.execute("update Zakaz set DRIVER_SMS_SEND_STATE=2 where BOLD_ID=" + orderId)) {

                                                }
                                                sendSMSRequest(sms_text.replace("***___msg_text",
                                                        (phone.length() > 10 ? "" : PHONE_CODE)+client_phone + ":" + order_adres), (phone.length() >= 10 ? "" : PHONE_CODE) + phone);
                                            } else
                                                showMessageRequest("Длина телефона для отправки СМС меньше 5-ти");
                                        } else if ((rs.getInt("CLIENT_SMS_SEND_STATE") == 1)&&ENABLE_MOVETO_CLIENT_SMS&&
                                                (START_ORD_CLSMS_TEXT.length()>5)) {
                                            phone = rs.getString("Telefon_klienta");
                                            if (phone.length() >= 5) {
                                                RECEIVER_TYPE = RECEIVER_IS_CLIENT;
                                                sms_text = START_ORD_CLSMS_TEXT;
                                                if (statement.execute("update Zakaz set CLIENT_SMS_SEND_STATE=2 where BOLD_ID=" + orderId)) {
                                                    showMessageRequest("RESET CLIENT_SMS_SEND_STATE=2");
                                                }
                                                sendSMSRequest(sms_text.replace("***___msg_text",
                                                        CLIENT_ORDER_INFO) + (ENABLE_WAIT_CLIENT_SEND ?
                                                        (waitTime > 0 ? WAIT_CLIENT_SEND_TEMPLATE.length() > 5 ? (
                                                                WAIT_CLIENT_SEND_TEMPLATE.replace("***___tval", waitTime + "")
                                                        ) : "" : "") : ""), (phone.length() > 10 ? "" : PHONE_CODE) + phone);
                                            } else
                                                showMessageRequest("Длина телефона для отправки СМС меньше 5-ти");
                                        } else if ((rs.getInt("CLIENT_SMS_SEND_STATE") == 3)&&ENABLE_REPORT_CLIENT_SMS&&
                                                (REPORT_CLSMS_TEXT.length()>5)) {
                                            phone = rs.getString("Telefon_klienta");
                                            if (phone.length() >= 5) {
                                                RECEIVER_TYPE = RECEIVER_IS_CLIENT;
                                                sms_text = REPORT_CLSMS_TEXT;
                                                if (statement.execute("update Zakaz set CLIENT_SMS_SEND_STATE=2 where BOLD_ID=" + orderId)) {
                                                    showMessageRequest("RESET CLIENT_SMS_SEND_STATE=2");
                                                }
                                                sendSMSRequest(sms_text.replace("***___msg_text",
                                                        ((int) order_summ + " " + CURRENCY_SHORT)), (phone.length() > 10 ? "" : PHONE_CODE) + phone);
                                            } else
                                                showMessageRequest("Длина телефона для отправки СМС меньше 5-ти");
                                        } else if ((rs.getInt("CLIENT_SMS_SEND_STATE") == 4)&&ENABLE_ONPLACE_CLIENT_SMS&&
                                                (ONPLACE_CLIENT_SMS_TEMPLATE.length()>5)) {
                                            phone = rs.getString("Telefon_klienta");
                                            if (phone.length() >= 5) {
                                                RECEIVER_TYPE = RECEIVER_IS_CLIENT;
                                                sms_text = ONPLACE_CLIENT_SMS_TEMPLATE;
                                                if (statement.execute("update Zakaz set CLIENT_SMS_SEND_STATE=2 where BOLD_ID=" + orderId)) {
                                                    showMessageRequest("RESET CLIENT_SMS_SEND_STATE=4");
                                                }
                                                sendSMSRequest(sms_text.replace("***___msg_text",
                                                        CLIENT_ORDER_INFO), (phone.length() > 10 ? "" : PHONE_CODE) + phone);
                                            } else
                                                showMessageRequest("Длина телефона для отправки СМС меньше 5-ти");
                                        }

                                }

                            } else
                                showMessageRequest("Ошибка соединения!");
                        } catch (Exception e) {
                            showMessageRequest("Ошибка работы с БД! Текст сообщения: "
                                    + e.getMessage());
                        } finally {
                            try {
                                if (con != null) con.close();
                            } catch (SQLException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        showMessageRequest(
                                "Ошибка извлечения класса драйвера! Текст сообщения: "
                                        + e.getMessage() + ".");
                    }
                    return null;
                }

                //protected void onPostExecute(Object obj) {
                // TODO: check this.exception
                // TODO: do something with the feed
                //}

            }.execute();
        }   else    {
            sendInfoBroadcast( ID_ACTION_SHOW_OUTSMS_INFO, "SMS отправка запрещена!");
        }
    }

    public void insertDetectNumberIntoDB(String phone_number)   {
        final String detect_num=phone_number;
        //showToast("["+phone_number+"]");
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
                        con = DriverManager.getConnection(MSSQL_DB, MSSQL_LOGIN, MSSQL_PASS);
                        //encrypt=true; trustServerCertificate=false;
                        if (con != null) {

                            Statement statement = con.createStatement();
                            //String queryString = "select * from SMSSendOrders";
                            if ((detect_num.length()==12) || (detect_num.length()==11) || true) {

                                if(ALT_FIX_DETECTING && false)   {
                                    if (detect_num.length() == 12) {
                                        if (statement.execute("EXEC InsertOrderWithParamsAlt '','" + detect_num.substring(2) +
                                                "', -1,0,0," + "0,-1010," + "0,0," + "'',-1,-1")) {
                                        }
                                    } else if (detect_num.length() == 11) {
                                        if (statement.execute("EXEC InsertOrderWithParamsAlt '','" + detect_num.substring(1) +
                                                "', -1,0,0," + "0,-1010," + "0,0," + "'',-1,-1")) {
                                        }
                                    }
                                }
                                else {
                                    if (detect_num.length() >= 12) {
                                        showMessageRequest("Request to add detected number 12 len" + detect_num);
                                        if (statement.execute("EXEC InsertOrderWithParams '','" +
                                                detect_num.replace(PHONE_CODE,"") +
                                                "', -1,0,0,0,-1010,0,0,'',-1,-1")) {
                                        }
                                    } else if (detect_num.length() == 11) {
                                        showMessageRequest("Request to add detected number 11 len" + detect_num);
                                        if (statement.execute("EXEC InsertOrderWithParams '','" + detect_num.substring(1) +
                                                "', -1,0,0,0,-1010,0,0,'',-1,-1")) {
                                        }
                                    }
                                    else    {
                                        showMessageRequest("Request to add detected number other len" + detect_num);
                                        if (statement.execute("EXEC InsertOrderWithParams '','" + detect_num +
                                                "', -1,0,0,0,-1010,0,0,'',-1,-1")) {
                                        }
                                    }
                                }

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

    static String phoneNumber = "";

    public static class CallReceiver extends BroadcastReceiver {

        public void showMessageRequest(String msg_text)	{
            Message msg = new Message();
            //msg.obj = this.mainActiv;
            msg.arg1 = MainActivity.SHOW_MESSAGE_TOAST;
            Bundle bnd = new Bundle();
            bnd.putString("msg_text", msg_text);
            msg.setData(bnd);
            handle.sendMessage(msg);
        }

        public void insertDetectNumRequest(String detect_num)	{
            Message msg = new Message();
            //msg.obj = this.mainActiv;
            msg.arg1 = INSERT_DETECT_NUM;
            Bundle bnd = new Bundle();
            bnd.putString(PHONE, detect_num);
            msg.setData(bnd);
            handle.sendMessage(msg);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(ENABLE_INCALL_DETECTING) {
                if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                    //получаем исходящий номер
                    phoneNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
                } else if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                    String phone_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        //телефон звонит, получаем входящий номер
                        phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        showMessageRequest("Входящий вызов с номера: " + phoneNumber);
                        insertDetectNumRequest(phoneNumber);
                    } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                        //телефон находится в режиме звонка (набор номера / разговор)
                    } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        //телефон находится в ждущем режиме. Это событие наступает по окончанию разговора, когда мы уже знаем номер и факт звонка
                    }
                }
            }   else    {
                //showMessageRequest("Определение номеров запрещено!");
            }
        }
    }

}
