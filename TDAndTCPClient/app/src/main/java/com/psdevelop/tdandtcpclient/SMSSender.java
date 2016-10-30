package com.psdevelop.tdandtcpclient;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SMSSender extends IntentService {
    public static final String INTENT_MESSAGE_SENT = "message.sent";
    public static final String INTENT_MESSAGE_DELIVERED = "message.delivered";

    public static final String EXTRA_MESSAGE = "extra.message";
    public static final String EXTRA_RECEIVERS = "extra.receivers";

    public SMSSender() {
        super("SMSSender");
    }

    private final String TAG = "SendSMS";


    private static class IDGenerator {

        private static final AtomicInteger counter = new AtomicInteger();

        public static int nextValue() {
            return counter.getAndIncrement();
        }
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

        // build notification
        // the addAction re-use the same intent to keep the example short
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

    private void sendSMS(String message, String[] receivers) {

        SmsManager sm = SmsManager.getDefault();

        ArrayList<String> parts = sm.divideMessage(message);

        PendingIntent sentPI = null;
        PendingIntent deliveredPI = null;

        Intent sentIntent = new Intent(INTENT_MESSAGE_SENT);

        int sentID = IDGenerator.nextValue();
        sentPI = PendingIntent.getBroadcast(SMSSender.this, sentID, sentIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Intent deliveryIntent = new Intent(INTENT_MESSAGE_DELIVERED);

        int deliveredID = IDGenerator.nextValue();
        deliveredPI = PendingIntent.getBroadcast(SMSSender.this, deliveredID,
                deliveryIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Log.i(TAG, "sending SMS: parts: " + parts.size() + " message: "
                + message);

        if (parts.size() > 1) {
            ArrayList<PendingIntent> sentIntents = null;
            ArrayList<PendingIntent> deliveredIntents = null;

            sentIntents = new ArrayList<PendingIntent>();
            deliveredIntents = new ArrayList<PendingIntent>();

            for (int i = 0; i < parts.size(); i++) {
                sentIntents.add(sentPI);
                deliveredIntents.add(deliveredPI);
            }

            for (String receiver : receivers) {
                try {
                    sm.sendMultipartTextMessage(receiver, null, parts,
                            sentIntents, deliveredIntents);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "illegal receiver: " + receiver);
                }

            }
        } else {
            for (String receiver : receivers) {
                try {
                    sm.sendTextMessage(receiver, null, parts.get(0), sentPI,
                            deliveredPI);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "illegal receiver: " + receiver);
                }
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String[] receivers = intent.getStringArrayExtra(EXTRA_RECEIVERS);
        showNotification("Отправка TD SMS...",message);
        sendSMS(message, receivers);

    }

}
