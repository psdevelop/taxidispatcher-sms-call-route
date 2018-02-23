package com.psdevelop.tdandtcpclient;

import android.os.Message;

/**
 * Created by ADMIN on 18.06.2015.
 */
public class CallItCheckTimer extends Thread {
    private RouteService ownerSrv;

    public CallItCheckTimer(RouteService own)   {
        this.ownerSrv = own;
        this.start();
    }

    public void checkWaitingSMS()   {
        Message msg = new Message();
        msg.obj = this.ownerSrv;
        msg.arg1 = RouteService.CHECK_CALLIT_ORDER;
        this.ownerSrv.handle.sendMessage(msg);
    }

    public void run() {
        while (true) {

            try {
                sleep(4000);
                checkWaitingSMS();
            } catch (Exception e) {
                //showMyMsg(
                //        "\nОшибка таймера!" + e.getMessage());
            }

        }

    }


}
