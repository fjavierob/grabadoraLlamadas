package com.dam.grabadoradellamadas;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.dam.grabadoradellamadas.MainActivity.CARPETA_LLAMADAS;

/**
 * Created by FPiriz on 19/5/17.
 *
 */

public class ServicioGrabacion extends Service  implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener
{
    static final String PAUSE="pause";
    static final String RESUME="resume";
    private Cronometro cronometro;
    private boolean recording;
    private boolean isAllowed;
    private ManejadorBD mbd;
    private MediaRecorder recorder;
    private String number;
    private String nombreFichero;
    private String sentido;
    private Date fechaLlamada;
    private NotificationCompat.Builder builder;
    private NotificationManager nMN;


    @Override
    public void onCreate()
    {
        Log.d("GrabadoraDeLlamadas","onCreate()");
        this.recording=false;
        this.mbd=new ManejadorBD(getBaseContext());
        this.builder=new NotificationCompat.Builder(this);
        this.nMN =(NotificationManager)getSystemService(NOTIFICATION_SERVICE);

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("GrabadoraDeLlamadas","onBind");

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i("GrabadoraDeLlamadas","onStartCommand("+intent+" , "+flags+" , "+startId+")");

        String action=intent.getStringExtra("action");
        String numero="";

        if(action.equals("accion_boton"))
        {
            Log.d("GrabadoraDeLlamadas",intent.getStringExtra("accion"));
            if(intent.getStringExtra("accion").equals(ServicioGrabacion.PAUSE))
            {
                this.onPause();
            }
            else if(intent.getStringExtra("accion").equals(ServicioGrabacion.RESUME))
            {
                this.onResume();
            }
        }
        else if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

            numero = intent.getStringExtra("telefono");
            this.sentido=Llamada.OUTGOING;

            if (numero.substring(0,1).equals("+"))
                numero = numero.substring(3);

            if(allowed(numero,this.sentido))
            {
                this.number=numero;
                if(this.record())
                {
                    showNotificationOngoing(this.number);
                }
            }
        }
        else {
            String state = intent.getStringExtra("state");
            numero = intent.getStringExtra("telefono");

            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                Log.d("GrabadoraDeLlamadas", "Ringing with number:" + numero);
                this.sentido=Llamada.INCOMING;

                if (numero.substring(0,1).equals("+"))
                    numero = numero.substring(3);

                isAllowed=allowed(numero,this.sentido);
                this.number=numero;
            }
            if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                Log.d("GrabadoraDeLlamadas", "OFFHOOK with number:" + numero);
                if (isAllowed&&this.record())
                {
                    showNotificationOngoing(this.number);
                }
            }
            if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                Log.d("GrabadoraDeLlamadas", "Telephone idle");
                stopRecording();
            }
        }


        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Log.i("GrabadoraDeLlamadas","onDestroy()");
        cancelNotification();
    }

    private boolean allowed(String number,String sentido)
    {
        Log.i("GrabadoraDeLlamadas","allowed("+number+", "+sentido+" )");
        boolean allow=false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        allow=prefs.getBoolean("habilitado",false);

        if(allow)
        {
            Log.v("GrabadoraDeLlamadas","Servicio habilitado");
            allow = !mbd.numeroExcluido(number);

            if(allow)
            {
                Log.v("GrabadoraDeLlamadas","Numero "+number+" no excluido");
                allow=false;

                if(sentido.equals(Llamada.INCOMING)&&prefs.getBoolean("pref_entrantes",false))
                {
                    Log.v("GrabadoraDeLlamadas","Llamada entrante y grabar entrantes");
                    allow=true;
                }
                else if(sentido.equals(Llamada.OUTGOING)&&prefs.getBoolean("pref_salientes",false))
                {
                    Log.v("GrabadoraDeLlamadas","Llamada saliente y grabar saliente");
                    allow=true;
                }

                if(allow)
                {
                    int grabar = Integer.parseInt(prefs.getString("pref_llamadas_a_grabar","2"));
                    switch (grabar)
                    {
                        case 1:
                            Log.v("GrabadoraDeLlamadas","Grabarlas todas");
                            break;
                        case 2:
                            Log.v("GrabadoraDeLlamadas","Grabar solo de los numeros configurados");
                            allow=mbd.numeroConfigurado(number);
                            break;
                    }

                }

            }

        }
        Log.v("GrabadoraDeLlamadas","allowed("+number+") = "+allow);


        return allow;

    }

    public void showNotificationOngoing(String number)
    {
        Log.i("GrabadoraDeLlamadas","showNotificationOngoing("+number+")");

        Intent intent1=new Intent(getApplicationContext(), ServicioGrabacion.class);
        intent1.putExtra("accion",ServicioGrabacion.PAUSE);
        intent1.putExtra("action","accion_boton");
        PendingIntent pIntent1 = PendingIntent.getService(this, (int) System.currentTimeMillis(), intent1, 0);

        Notification n  = builder
                .setContentTitle("Grabando")
                .setContentText("Llamada con "+number)
                .setSmallIcon(R.drawable.icon_grabando) //set your icon
                .addAction(R.drawable.icon_pause, "Parar grabacion", pIntent1)
                //.setColor(Color.RED)
                .setLights(Color.RED, 100, 100)
                .setOngoing(true)
                //.setStyle(new NotificationCompat.BigTextStyle(NorProv))
                .build();
        startForeground(10648,n);

    }

    private void showNotificationError(String mensaje)
    {
        Log.i("GrabadoraDeLlamadas","showNotificationError("+mensaje+")");

        Notification n  = new NotificationCompat.Builder(getBaseContext())
                .setContentTitle("Grabadora de llamadas")
                .setContentText("Ha ocurrido un error")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setSmallIcon(R.drawable.icon_error) //set your icon
                .setLights(Color.RED, 300, 1000)
                //.setStyle(new NotificationCompat.BigTextStyle(NorProv))
                .build();

        this.nMN.notify(10649,n);
    }

    public void updateNotification(int duracion)
    {
        int secs = duracion;
        int mins = secs / 60;
        secs = secs % 60;
        String textoDuracion="" + String.format(Locale.ENGLISH,"%02d", mins) + ":"
                + String.format(Locale.ENGLISH,"%02d", secs);


        builder.setContentText("Duracion "+textoDuracion)
                .setContentText("Llamada "+number+" - Duracion "+textoDuracion);

        this.nMN.notify(10648, builder.build());

    }

    public void cancelNotification()
    {
        Log.i("GrabadoraDeLlamadas","cancelNotification()");

        this.cronometro.interrupt();
        this.nMN.cancel(10648);
    }

    public boolean isRecording()
    {
        Log.i("GrabadoraDeLlamadas","isRecording()");
        return recording;
    }

    @Nullable
    private File crearFichero(String prefix, String suffix)
    {
        Log.i("GrabadoraDeLlamadas","crearFichero("+prefix+" , "+suffix+")");

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            File sdCard = Environment.getExternalStorageDirectory();

            File dir = new File(sdCard.getAbsolutePath() + CARPETA_LLAMADAS);
            Log.d("GrabadoraDeLlamadas", "Ruta del fichero: " + sdCard.getAbsolutePath() + CARPETA_LLAMADAS + "/" + prefix + suffix);
            File destino = new File(sdCard.getAbsolutePath() + CARPETA_LLAMADAS + "/" + prefix + suffix);
            this.nombreFichero=prefix+suffix;
            Log.d("GrabadoraDeLlamadas", "Destino = " + destino);

            return destino;
        }
        else
        {
            Log.w("GrabadoraDeLlamadas","La aplicacion no tiene permisos para crear ficheros");
            return null;
        }
    }

    public boolean record()
    {
        Log.i("GrabadoraDeLlamadas","record()");
        boolean devolver=false;
        if(!isRecording()) {
            Log.v("GrabadoraDeLlamadas","Beginning to record");

            String nombre="";
            SimpleDateFormat formato=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_", Locale.ENGLISH);
            this.fechaLlamada=new Date();
            nombre=formato.format(this.fechaLlamada)+this.number+"";

            String suffix=".amr";

            File fichero=null;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            int source=Integer.parseInt(prefs.getString("pref_audio_a_grabar","1"));

            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED)
            {
                Log.d("GrabadoraDeLlamadas","Con permiso para grabar");
                recorder = new MediaRecorder();

                switch(source)
                {
                    case 1: // all
                        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
                        break;

                    case 2: // speaker
                        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_DOWNLINK);
                        break;

                    case 3: // mic
                        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_UPLINK);
                        break;
                }

                recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                fichero=crearFichero(nombre,suffix);

                if(fichero!=null)
                {
                    recorder.setOutputFile(fichero.getAbsolutePath());

                    try {
                        recorder.prepare();
                    } catch (IOException e) {
                        Log.w("GrabadoraDeLlamadas", "Error al preparar la grabadora de la llamada", e);
                    }

                    recorder.setOnInfoListener(this);
                    recorder.setOnErrorListener(this);

                    this.cronometro = new Cronometro(10648,this);
                    cronometro.start();

                    recorder.start();

                    recording = true;
                    devolver=true;
                }
                else
                {
                    showNotificationError("Necesario permiso de almacenamiento");
                }

            }
            else
            {
                Log.w("GrabadoraDeLlamadas","Necesario permiso de grabacion de audio");
                showNotificationError("Necesario permiso de grabacion de audio");
            }
        }

        return devolver;

    }

    private void onPause()
    {
        Log.i("GrabadoraDeLlamadas","onPause()");
        Intent intent1=new Intent(getApplicationContext(), ServicioGrabacion.class);
        intent1.putExtra("accion",ServicioGrabacion.RESUME);
        intent1.putExtra("action","accion_boton");
        PendingIntent pIntent1 = PendingIntent.getService(this, (int) System.currentTimeMillis(), intent1, 0);

        this.cronometro.onPause();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder.pause();
            Log.v("GrabadoraDeLlamadas","Tu version de SDK N o superior (API) 24 ");

        }

        Notification n  = new NotificationCompat.Builder(getBaseContext())
                .setContentTitle("Pausada grabacion")
                .setContentText("Llamada con "+number)
                .setSmallIcon(R.drawable.icon_pause) //set your icon
                .addAction(R.drawable.icon_resume, "Continuar grabando", pIntent1)
                .setLights(Color.RED, 500, 1000)
                .setOngoing(true)
                .build();
        this.nMN.notify(10648,n);

    }

    private void onResume()
    {
        Log.i("GrabadoraDeLlamadas","onResume()");
        this.cronometro.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder.resume();
            Log.v("GrabadoraDeLlamadas","Tu version de SDK N o superior (API) 24 ");
        }

        Notification n  = builder
                .build();
        this.nMN.notify(10648,n);


    }

    public void stopRecording()
    {
        Log.i("GrabadoraDeLlamadas","stopRecording()");
        int duracion=0;
        SimpleDateFormat formatoSalida=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ENGLISH);
        String fecha;

        if(isRecording())
        {
            fecha=formatoSalida.format(this.fechaLlamada);
            Log.v("GrabadoraDeLlamadas","End of recording");
            recording=false;
            recorder.stop();
            duracion=cronometro.getDuracion();
            String nombre="";

            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) {

                nombre = getContactName(this.getBaseContext(), this.number);
            }
            else
            {
                showNotificationError("Necesario permiso para acceder a los contactos ");
            }

            mbd.insertarLlamada(nombre, this.number,
                    this.sentido, duracion, fecha, this.nombreFichero);
            recorder.release();
            recorder = null;
            Log.v("GrabadoraDeLlamadas","Interruption successful");
            stopSelf();
        }
    }

    private String getContactName(Context context, String phoneNumber) {
        Log.i("GrabadoraDeLlamadas","getContactName(context, "+phoneNumber+" )");
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if(cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        Log.v("GrabadoraDeLlamadas","Nombre = "+contactName);
        return contactName;
    }

    // MediaRecorder.OnInfoListener
    public void onInfo(MediaRecorder mr, int what, int extra)
    {
        Log.i("GrabadoraDeLlamadas", "onInfo(MediaRecorder , what: " + what + " , extra: " + extra+")");
        recording = false;
    }

    // MediaRecorder.OnErrorListener
    public void onError(MediaRecorder mr, int what, int extra)
    {
        Log.i("GrabadoraDeLlamadas", "onError(MediaRecorder , what: " + what + " , extra: " + extra+")");
        recording = false;
        mr.release();
    }
}
