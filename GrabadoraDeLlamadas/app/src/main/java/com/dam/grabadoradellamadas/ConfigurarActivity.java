package com.dam.grabadoradellamadas;

//import android.content.Intent;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

// Actividad en la que se muestran los números configurados o excluidos
// y que permite añadir números a estas categorías.

/*  La actividad al crearse a partir de un intent recibe un parámetro,
 *  "tipo", que indica si la actividad va a ser para configurar
 *  o para excluir números (comportamiento idéntico, sólo varían un par
 *  de cadenas de texto y la tabla de la base de datos en la que inserta,
 *  borra o extrae la información).
 */

public class ConfigurarActivity extends AppCompatActivity
{

    private ListView lista;

    static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;

    // tipos: CONFIGURAR ó EXCLUIR
    static final boolean CONFIGURAR = true;
    static final boolean EXCLUIR = false;

    private static final int PICK_CONTACT = 1;

    private boolean tipo;

    // tabla de la bd con la que va a operar.
    private String tabla;

    private ManejadorBD MBD;

    private View dialogView;

    FloatingActionButton botonAniadir;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        MBD = new ManejadorBD(getApplicationContext());

        super.onCreate(savedInstanceState);

        tipo = CONFIGURAR;
        tabla = ManejadorBD.TABLA_CONFIGURADOS;
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            tipo = extras.getBoolean("tipo");

        setContentView(R.layout.activity_configurar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.pref_configurar_title);

        if (!tipo) {
            toolbar.setTitle(R.string.pref_excluir_title);
            tabla = ManejadorBD.TABLA_EXCLUIDOS;
        }

        botonAniadir = (FloatingActionButton) findViewById(R.id.configurarAniadir);
        botonAniadir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              AlertDialog dialog = crearDialogoInsertar(tipo);
                dialog.show();
                /*
                Snackbar.make(view, tipo ? "Configurar" : "Excluir", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                */
            }
        });

        // Crear la lista
        updateLista();
    }

    // Método para obtener los números de la base de datos y crear la lista
    // a partir de estos.
    protected void updateLista()
    {
        ArrayList<Telefono> datos = MBD.getTelefonos(tabla);
        lista = (ListView) findViewById(R.id.listaTelefonos);
        lista.setAdapter(new AdaptadorListaTelefonos(this, R.layout.telefono, datos));
    }

    @Override
    public boolean deleteFile(String name)
    {
        boolean ret = MBD.borrarTelefono(tabla, name);
        updateLista();
        return ret;
    }

    // Crear el diálogo para insertar un nuevo número
    // ver res/layout/dialog_insertar_telefono.xml
    private AlertDialog crearDialogoInsertar(boolean tipo)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        dialogView = inflater.inflate(R.layout.dialog_insertar_telefono, null);
        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();

        Button buscarEnContactos = (Button) dialogView.findViewById(R.id.botonBuscarEnContactos);
        Button insertarTelefono = (Button) dialogView.findViewById(R.id.botonInsertarTelefono);

        final EditText nombreEt = (EditText) dialogView.findViewById(R.id.inputNombre);
        final EditText numeroEt = (EditText) dialogView.findViewById(R.id.inputNumero);

        String t = MBD.TABLA_CONFIGURADOS;

        if (!tipo) // Si tipo == EXCLUIR (por defecto, CONFIGURAR)
        {
            TextView info = (TextView) dialogView.findViewById(R.id.dialog_info_text);
            info.setText("Excluir nuevo teléfono");
            insertarTelefono.setText("Excluir teléfono");
            t = MBD.TABLA_EXCLUIDOS;
        }

        final String tabla = t;

        buscarEnContactos.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                            permissionReadContacts();
                    }
                }
        );

        insertarTelefono.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Comprobar correcto formato
                        String nombre = nombreEt.getText().toString();
                        String numero = numeroEt.getText().toString();
                        if (numeroTelefonoValido(numero))
                        {
                            numero = numero.replaceAll("[()\\s-]+", "");
                            if (numero.substring(0,1).equals("+"))
                                numero = numero.substring(3);
                            //if (numero)
                            if (MBD.insertarTelefono(tabla, nombre, numero))
                            {
                                updateLista();
                            }
                            else
                                Toast.makeText(getBaseContext(), "Error al insertar", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                        else {
                            Toast.makeText(getBaseContext(), "Número inválido", Toast.LENGTH_LONG).show();
                            //dialog.dismiss();
                        }
                    }
                }

        );
        return dialog;
    }

    // Método para determinar si un número de teléfono es válido.
    private static final boolean numeroTelefonoValido(String telefono)
    {
        if (telefono == null || TextUtils.isEmpty(telefono)) {
            return false;
        } else {
            return android.util.Patterns.PHONE.matcher(telefono).matches();
        }
    }

    // Método para recibir un resultado de una actividad que se haya lanzado.
    // Útil para recibir un contacto de la agenda.
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // check whether the result is ok
        if (resultCode == RESULT_OK) {
            // Check for the request code, we might be usign multiple startActivityForReslut
            switch (requestCode) {
                case PICK_CONTACT:
                    recibidoContacto(data);
                    break;
            }
        } else {
            Toast.makeText(getBaseContext(), "Error al elegir contacto", Toast.LENGTH_LONG).show();
        }
    }

    // Método para procesar un contacto recibido de la agenda.
    // Se extrae nombre y número y se colocan en los EditText del
    // diálogo.
    private void recibidoContacto(Intent data)
    {
        EditText nombreEt = (EditText) dialogView.findViewById(R.id.inputNombre);
        EditText numeroEt = (EditText) dialogView.findViewById(R.id.inputNumero);
        Cursor cursor = null;
        try {
            String numero = null ;
            String nombre = null;
            // getData() method will have the Content Uri of the selected contact
            Uri uri = data.getData();
            //Query the content uri
            cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            // column index of the phone number
            int  phoneIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            // column index of the contact name
            int  nameIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            numero = cursor.getString(phoneIndex);
            nombre = cursor.getString(nameIndex);
            // Set the value to the textviews
            nombreEt.setText(nombre);
            numeroEt.setText(numero);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para el permiso de leer contactos.
    private void permissionReadContacts()
    {
        if (ContextCompat.checkSelfPermission(ConfigurarActivity.this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Mostrar explicación al usuario ?
            if (ActivityCompat.shouldShowRequestPermissionRationale(ConfigurarActivity.this,
                    Manifest.permission.READ_CONTACTS)) {

             }
            else
            {
                // No explicación, pedimos el permiso
                ActivityCompat.requestPermissions(ConfigurarActivity.this,
                        new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }
        else
            buscarContacto();
    }

    @Override
    // Método que recibe el resultado de la petición de permisos.
    // Aquí introducimos el código que se debía ejecutar si necesitabamos
    // permiso para ejecutarlo.
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    buscarContacto();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Necesita proporcionar permiso", Toast.LENGTH_LONG);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    // Método para lanzar un intent para obtener un contacto de la agenda.
    private void buscarContacto()
    {
        Intent i=new Intent(Intent.ACTION_PICK);
        i.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(i, PICK_CONTACT);
    }

}
