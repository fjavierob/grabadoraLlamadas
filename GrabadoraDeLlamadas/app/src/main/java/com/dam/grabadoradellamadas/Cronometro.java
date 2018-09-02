package com.dam.grabadoradellamadas;


import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.Semaphore;

/**
 * Created by FPiriz on 20/5/17.
 */

public class Cronometro extends Thread {

    private int duracion;
    private boolean seguir;
    private int id;
    private ServicioGrabacion servicio;

    private final Semaphore continuar = new Semaphore(1, true);



    Cronometro(int id,ServicioGrabacion servicio) {
        Log.i("GrabadoraDeLlamadas","Cronometro.Cronometro("+id+", servicio)");

        this.duracion=0;
        this.id=0;
        this.seguir=true;
        this.servicio=servicio;
    }

    public void run() {
        Log.i("GrabadoraDeLlamadas","Cronometro.run()");
        while (seguir)
        {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                Log.d("GrabadoraDeLlamadas","Exception Cronometro.run()",e);
            }
            try {
                continuar.acquire();
            } catch (InterruptedException e) {
                Log.d("GrabadoraDeLlamadas","Exception Cronometro.run().acquire()",e);
            }
            if(seguir) {
                this.servicio.updateNotification(++duracion);
                Log.v("GrabadoraDeLlamadas", "Duracion = " + duracion + " segundos");
            }
            continuar.release();

        }
        super.interrupt();

    }

    @Override
    public void interrupt() {
        Log.i("GrabadoraDeLlamadas","Cronometro.interrupt()");
        this.seguir=false;
    }

    public int getDuracion()
    {
        return this.duracion;
    }

    public void onResume()
    {
        continuar.release();
    }

    public void onPause()
    {
        try {
            continuar.acquire();
        } catch (InterruptedException e) {
            Log.d("GrabadoraDeLlamadas","Exception Cronometro.onPause().acquire()",e);
        }
    }
}
