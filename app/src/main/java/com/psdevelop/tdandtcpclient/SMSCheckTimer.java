package com.psdevelop.tdandtcpclient;

import android.os.Bundle;
import android.os.Message;

/**
 * Created by ADMIN on 05.01.2015.
 */
public class SMSCheckTimer extends Thread {
    private RouteService ownerSrv;

    public SMSCheckTimer(RouteService own)   {
        this.ownerSrv = own;
        this.start();
    }

    public void checkWaitingSMS()   {
        Message msg = new Message();
        msg.obj = this.ownerSrv;
        msg.arg1 = RouteService.CHECK_WAITING_SMS;
        this.ownerSrv.handle.sendMessage(msg);
    }

    public void run() {
        while (true) {

            try {
                sleep(10000);
                checkWaitingSMS();
            } catch (Exception e) {
                //showMyMsg(
                //        "\nОшибка таймера!" + e.getMessage());
            }

        }

    }

}
