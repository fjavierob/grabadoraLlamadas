package com.dam.grabadoradellamadas;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by FPiriz on 20/5/17.
 *
 */

public class ReceptorTelefono extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("GrabadoraDeLlamadas","ReceptorTelefono.onReceive("+context+" , "+intent+" )");
        Log.v("GrabadoraDeLlamadas","recibido el evento "+intent.getAction());
        Bundle extras = intent.getExtras();
        String numero="";

        Intent intent_service=new Intent(context, ServicioGrabacion.class);


        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

            if (extras != null) {
                numero = extras.getString(Intent.EXTRA_PHONE_NUMBER);
            }
            Log.d("GrabadoraDeLlamadas","llamada saliente al numero "+numero);

            intent_service.putExtra("action",intent.getAction());
            intent_service.putExtra("telefono",numero);
            context.startService(intent_service);

        }
        else
        {

            String phoneNumber="";
            String state="";

            if (extras != null) {
                state = extras.getString(TelephonyManager.EXTRA_STATE);
                phoneNumber= extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            }
            Log.d("GrabadoraDeLlamadas","llamada entrante. Estado: "+state);

            intent_service.putExtra("action",intent.getAction());
            intent_service.putExtra("state",state);
            intent_service.putExtra("telefono",phoneNumber);
            context.startService(intent_service);


        }
    }
}


