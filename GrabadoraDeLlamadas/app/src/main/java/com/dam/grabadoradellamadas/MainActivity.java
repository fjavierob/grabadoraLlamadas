package com.dam.grabadoradellamadas;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import static com.dam.grabadoradellamadas.R.string.no;

// Actividad main que muestra la lista de llamadas grabadas y un menú
// con un par de botones: borrar llamadas seleccionadas y preferencias.

public class MainActivity extends AppCompatActivity
{

    private final static int MY_PERMISSIONS_REQUEST = 0;
    private final static int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private final static int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 2;
    private final static int MY_PERMISSIONS_REQUEST_PROCESS_OUTGOING_CALLS = 3;
    private final static int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 4;
    private final static int MY_PERMISSIONS_REQUEST_BORRAR_LLAMADA = 5;
    private final static int MY_PERMISSIONS_REQUEST_BORRAR_SELECCIONADAS = 6;

    public final static String CARPETA_LLAMADAS = "/grabadorallamadas";

    private ListView listaLlamadas;
    private ManejadorBD MBD;
    private Menu menu;

    // Variable para almacenar las llamadas que se seleccionan y
    // que, posteriormente, pueden borrarse.
    private ArrayList<View> llamadasSeleccionadas;
    // Variable para almacenar el nombre del archivo de una llamada que
    // se va a borrar.
    private String aBorrarArchivoLlamada;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(getString(R.string.logMA), "onResume # ");
        aBorrarArchivoLlamada = null;
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        llamadasSeleccionadas = new ArrayList<>();

        MBD = new ManejadorBD(getApplicationContext());

            /* Crear ficheros de prueba */
        // permissionWriteES();
        // Crear si tenemos permisos o cuando los tengamos.

        // Pedir permisos necesarios para el servicio
        permissions();

        // crear la lista
        updateLista();
        // onClick en elementos de la lista
        listaLlamadas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                // Seleccionar si el usuario está seleccionando llamadas para
                // posteriormente borrarlas.
                if (!llamadasSeleccionadas.isEmpty())
                {
                    seleccionarLlamada(view);
                    return;
                }

                // Si no, mostrar menú popup con opciones de eliminar y reproducir.
                final View llamadaView = view;
                TextView archivoTv = (TextView) view.findViewById(R.id.llamadaArchivo);
                final String archivo = archivoTv.getText().toString();

                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.getMenuInflater().inflate(R.menu.menu_llamada, popup.getMenu());
                // onClick para los botones del menú
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId())
                        {
                            // Botón eliminar
                            case R.id.menuLlamadaEliminar:
                                new AlertDialog.Builder(MainActivity.this)
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setTitle("Eliminar llamadas")
                                        .setMessage("¿Desea eliminar la llamada?")
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                aBorrarArchivoLlamada = archivo;
                                                if (permisoBorrar(MY_PERMISSIONS_REQUEST_BORRAR_LLAMADA))
                                                    borrarLlamadaSeleccionada();
                                            }
                                        })
                                        .setNegativeButton(no, null)
                                        .show();
                                return true;
                            // Botón reproducir
                            case R.id.menuLlamadaReproducir:
                                File sdCard = Environment.getExternalStorageDirectory();
                                File dir = new File (sdCard.getAbsolutePath() + CARPETA_LLAMADAS);
                                File llamada = new File(dir, archivo);
                                if(llamada.isFile())
                                {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(llamada), "audio/*");
                                    startActivity(intent);
                                }
                                else Toast.makeText(getApplicationContext(), "Llamada no encontrada", Toast.LENGTH_LONG).show();
                                return true;
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });
        // onLongClick en elementos de la lista: seleccionar el elemento
        listaLlamadas.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                           long id) {
                seleccionarLlamada(view);
                return true;
            }
        });

    }

    @Override
    protected void onResume()
    {
        Log.d(getString(R.string.logMA), "onResume # ");
        super.onResume();
        updateLista();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.menu = menu;
        Log.d(getString(R.string.logMA), "onCreateOptionsMenu # ");
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // Menú superior con título y un par de botones.
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Log.d(getString(R.string.logMA), "onOptionsItemSelected # ");
        switch (item.getItemId())
        {
            // Botón de opciones
            case R.id.botonOpciones:
                Intent intent = new Intent(this, OpcionesActivity.class);
                startActivity(intent);
                return true;
            // Botón para borrar las llamadas seleccionadas.
            case R.id.botonBorrarLlamadas:
                boolean ret = true;
                if (llamadasSeleccionadas.isEmpty())
                {
                    Toast.makeText(getBaseContext(), "Ninguna llamada seleccionada", Toast.LENGTH_LONG).show();
                    return false;
                }
                final MenuItem delete = item;
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Eliminar llamadas")
                        .setMessage("¿Desea eliminar las llamadas?")
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (permisoBorrar(MY_PERMISSIONS_REQUEST_BORRAR_SELECCIONADAS))
                                    borrarLlamadasSeleccionadas();
                            }
                        })
                        .setNegativeButton(no, null)
                        .show();
                return ret;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Método para obtener las llamadas de la base de datos y crear la lista
    // a partir de estas.
    protected void updateLista()
    {
        ArrayList<Llamada> datos = MBD.getLlamadas();
        listaLlamadas = (ListView) findViewById(R.id.listaLlamadas);
        listaLlamadas.setAdapter(new AdaptadorListaLlamadas(this, R.layout.llamada, datos));
    }

    @Override
    // Método para manejar el clic en el botón de retroceso.
    // Si el usuario está seleccionando llamadas: se deseleccionan.
    // Si no: se sale de la actividad.
    public void onBackPressed(){
        if (llamadasSeleccionadas.isEmpty())
            finish();
        else
        {
            for (int i = 0; i < llamadasSeleccionadas.size(); i++)
            {
                View llamadaView = llamadasSeleccionadas.get(i);
                TextView s = (TextView) llamadaView.findViewById(R.id.llamadaSelected);
                s.setText("false");
                llamadaView.setBackgroundColor(Color.rgb(0xf6, 0xf6, 0xf6));
            }
            llamadasSeleccionadas.clear();
            MenuItem delete = menu.findItem(R.id.botonBorrarLlamadas);
            delete.setVisible(false);
        }

    }

    // Método para marcar una llamada (elemento de la lista) como seleccionado:
    // Se cambia su color de fondo y se guarda en el ArrayList de llamadas
    // seleccionadas.
    private void seleccionarLlamada(View view)
    {
        TextView s = (TextView) view.findViewById(R.id.llamadaSelected);
        if (s.getText().toString().equals("true"))
        {
            s.setText("false");
            view.setBackgroundColor(Color.rgb(0xf6,0xf6,0xf6));
            llamadasSeleccionadas.remove(view);
            if (llamadasSeleccionadas.isEmpty())
            {
                MenuItem delete = menu.findItem(R.id.botonBorrarLlamadas);
                delete.setVisible(false);
            }
        }
        else
        {
            s.setText("true");
            view.setBackgroundColor(Color.rgb(0xff,0x52,0x52));
            llamadasSeleccionadas.add(view);
            MenuItem delete = menu.findItem(R.id.botonBorrarLlamadas);
            delete.setVisible(true);
        }
    }

    // Método para pedir los permisos necesarios para la aplicación.
    // Comprueba qué permisos posee ya, y los que no posee los pide.
    private void permissions()
    {

        int numPermisos = 0;
        String[] permisos;
        boolean pWES = false;
        boolean pRPS = false;
        boolean pRA = false;
        boolean pPOC = false;
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            numPermisos++;
            pWES = true;
        }
        else {/* crearFicheros(); */}

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED)
        {
            numPermisos++;
            pRPS = true;
        }
        else { /* Hacer algo con permiso*/ }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
        {
            numPermisos++;
            pRA = true;
        }
        else { /* Hacer algo con permiso */ }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.PROCESS_OUTGOING_CALLS)
                != PackageManager.PERMISSION_GRANTED)
        {
            numPermisos++;
            pPOC = true;
        }
        else { /* Hacer algo con permiso */ }

        if (numPermisos > 0) {
            permisos = new String[numPermisos];
            int i = 0;
            if (pWES) {
                permisos[i] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                i++;
            }
            if (pRA) {
                permisos[i] = Manifest.permission.RECORD_AUDIO;
                i++;
            }
            if (pRPS) {
                permisos[i] = Manifest.permission.READ_PHONE_STATE;
                i++;
            }
            if (pPOC) {
                permisos[i] = Manifest.permission.PROCESS_OUTGOING_CALLS;
                i++;
            }
            ActivityCompat.requestPermissions(MainActivity.this, permisos, MY_PERMISSIONS_REQUEST);
        }
    }

    // Método para el permiso de escritura en el almacenamiento externo.
    private void permissionWriteES()
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Mostrar explicación al usuario ?
            }
            else
            {
                // No explicación, pedimos el permiso
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

            }
        }
        else {
            //crearFicheros();
        }
    }

    // Método para el permiso de lectura del estado del teléfono.
    private void permissionReadPhoneState()
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_PHONE_STATE)) {

                // Mostrar explicación al usuario ?
            }
            else
            {
                // No explicación, pedimos el permiso
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
        }
        else {
            // Hacer algo si tengo permiso
        }
    }

    // Método para el permiso de grabación de audio.
    private void permissionRecordAudio()
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO)) {

                // Mostrar explicación al usuario ?
            }
            else
            {
                // No explicación, pedimos el permiso
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

            }
        }
        else {
            // Hacer algo si tengo permiso
        }
    }

    // Método para el permiso de procesar llamadas salientes.
    private void permissionProcessOutgoingCalls()
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.PROCESS_OUTGOING_CALLS)
                != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.PROCESS_OUTGOING_CALLS)) {

                // Mostrar explicación al usuario ?
            }
            else
            {
                // No explicación, pedimos el permiso
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.PROCESS_OUTGOING_CALLS}, MY_PERMISSIONS_REQUEST_PROCESS_OUTGOING_CALLS);
            }
        }
        else {
            // Hacer algo si tengo permiso
        }
    }

    @Override
    // Método que recibe el resultado de la petición de permisos.
    // Aquí introducimos el código que se debía ejecutar si necesitabamos
    // permiso para ejecutarlo.
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST:
                for (int i = 0; i < permissions.length; i++)
                {
                    switch (permissions[i])
                    {
                        case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                            //crearFicheros();
                            break;
                        case Manifest.permission.PROCESS_OUTGOING_CALLS:
                            // Hacer algo con el permiso
                            break;
                        case Manifest.permission.READ_PHONE_STATE:
                            // Hacer algo con el permiso
                            break;
                        case Manifest.permission.RECORD_AUDIO:
                            // Hacer algo con el permiso
                            break;
                    }
                }
                return;
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //crearFicheros();
                }
                else
                {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            case MY_PERMISSIONS_REQUEST_PROCESS_OUTGOING_CALLS:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // Hacer algo si tengo permiso
                }
                else
                {
                    // Hacer algo si no me dan permiso
                }
                return;
            case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // Hacer algo si tengo permiso
                }
                else
                {
                    // Hacer algo si no me dan permiso
                }
                return;
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // Hacer algo si tengo permiso
                }
                else
                {
                    // Hacer algo si no me dan permiso
                }
                return;
            case MY_PERMISSIONS_REQUEST_BORRAR_LLAMADA:
                borrarLlamadaSeleccionada();
                return;
            case MY_PERMISSIONS_REQUEST_BORRAR_SELECCIONADAS:
                borrarLlamadasSeleccionadas();
                return;
        }
    }

    // Método para comprobar si se puede borrar una o varias llamadas, es
    // decir, si se dispone del permiso de escritura en el almacenamiento
    // externo. Devuelve verdadero en caso afirmativo, y en caso contrario,
    // pide el permiso (el borrado se hará entonces en onRequestPermissionsResult)
    // y devuelve falso.
    private boolean permisoBorrar(int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            }
            else
            {
                // No tengo permiso escritura. Lo pido y el borrado se hará
                // en onRequestPermissionsResult
                 ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            }
            return false;
        }
        return true;
    }

    // Método para borrar la llamada indicada en la variable
    // aBorrarArchivoLlamada. Útil para que se pueda llamar desde
    // el método onRequestPermissionsResult sin ningún parámetro.
    private void borrarLlamadaSeleccionada()
    {
        if (aBorrarArchivoLlamada != null)
        {
            borrarLlamada(aBorrarArchivoLlamada);
            aBorrarArchivoLlamada = null;
            updateLista();
        }

    }

    // Método para borrar las llamadas seleccionadas (indicadas en
    // la variable llamadasSeleccionadas. Útil para que se pueda llamar
    // desde el método onRequestPermissionsResult sin ningún parámetro.
    private void borrarLlamadasSeleccionadas()
    {
        for (int i = 0; i < llamadasSeleccionadas.size(); i++)
        {
            View llamadaView = llamadasSeleccionadas.get(i);
            TextView archivoTv = (TextView) llamadaView.findViewById(R.id.llamadaArchivo);
            borrarLlamada(archivoTv.getText().toString());
        }
        llamadasSeleccionadas.clear();
        MenuItem delete = menu.findItem(R.id.botonBorrarLlamadas);
        delete.setVisible(false);
        updateLista();
    }

    // Método para borrar una llamada a partir de su archivo, que se
    // recibe como parámetro.
    private void borrarLlamada(String archivo)
    {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + CARPETA_LLAMADAS);
        File llamada = new File(dir, archivo);
        llamada.delete();
        MBD.borrarLlamada(archivo);
    }

    // Crear un fichero llamada1.mp3 de pruebas a partir de un fichero raw.
    private void crearFicheros()
    {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + CARPETA_LLAMADAS);
        File llamada1 = new File (sdCard.getAbsolutePath() + CARPETA_LLAMADAS+"/llamada1.mp3");
        if(!dir.isDirectory())
            dir.mkdirs();

        if(!llamada1.isFile())
        {
            InputStream ins = getResources().openRawResource(R.raw.waltz1);
            int size;
            try {
                size = ins.available();
                byte[] buffer = new byte[size];
                ins.read(buffer);
                ins.close();
                FileOutputStream fos = new FileOutputStream(new File(dir, "llamada1.mp3"));
                fos.write(buffer);
                fos.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }
}
