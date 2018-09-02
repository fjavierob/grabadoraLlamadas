package com.dam.grabadoradellamadas;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Clase para operar con la base de datos.
public class ManejadorBD extends SQLiteOpenHelper
{

    private static final int VERSION_BASEDATOS = 1;
    private static final String NOMBRE_BASEDATOS = "grabadorallamadas.db";
    public static final String TABLA_LLAMADAS = "llamadas_grabadas";
    public static final String TABLA_CONFIGURADOS ="telefonos_configurados";
    public static final String TABLA_EXCLUIDOS = "telefonos_excluidos";
    private static final String CREAR_TABLA_LLAMADAS = "CREATE TABLE IF NOT EXISTS "+TABLA_LLAMADAS+
            " (nombre TEXT, numero TEXT, sentido TEXT, duracion INTEGER, fecha TEXT, archivo TEXT PRIMARY KEY)";
    private static final String CREAR_TABLA_CONFIGURADOS ="CREATE TABLE IF NOT EXISTS "+TABLA_CONFIGURADOS+
            " (nombre TEXT, numero TEXT PRIMARY KEY)";
    private static final String CREAR_TABLA_EXCLUIDOS = "CREATE TABLE IF NOT EXISTS "+TABLA_EXCLUIDOS+
            " (nombre TEXT, numero TEXT PRIMARY KEY)";

    public ManejadorBD(Context context) {
        super(context, NOMBRE_BASEDATOS, null, VERSION_BASEDATOS);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREAR_TABLA_LLAMADAS);
        db.execSQL(CREAR_TABLA_CONFIGURADOS);
        db.execSQL(CREAR_TABLA_EXCLUIDOS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_LLAMADAS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_CONFIGURADOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_EXCLUIDOS);
        onCreate(db);
    }

    // Método para insertar un nuevo teléfono en la tabla indicada
    public boolean insertarTelefono(String tabla, String nombre, String numero) {
        long salida=0;
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            ContentValues valores = new ContentValues();
            if(numero!=null && !numero.equals("") && (tabla.equals(TABLA_EXCLUIDOS) || tabla.equals(TABLA_CONFIGURADOS))) {
                if (nombre != null && !nombre.equals(""))
                    valores.put("nombre", nombre);
                valores.put("numero", numero);
                salida = db.insert(tabla, null, valores);
            }
        }
        db.close();
        return(salida>0);
    }

    // Método para borrar un teléfono de la tabla indicada
    public boolean  borrarTelefono(String tabla, String numero) {
        SQLiteDatabase db = getWritableDatabase();
        long salida=0;
        if (db != null) {
            if(numero!=null && !numero.equals("") && (tabla.equals(TABLA_EXCLUIDOS) || tabla.equals(TABLA_CONFIGURADOS)))
                salida=db.delete(tabla, "numero='" + numero+"'", null);
        }
        db.close();
        return(salida>0);
    }

    // Método para obtener un teléfono, a partir del número, de la
    // tabla indicada.
    public Telefono getTelefono(String tabla, String numero) {
        Telefono telefono = null;
        if(numero!=null && !numero.equals("") && (tabla.equals(TABLA_EXCLUIDOS) || tabla.equals(TABLA_CONFIGURADOS))) {
            SQLiteDatabase db = getReadableDatabase();
            String[] columnas = {"nombre", "numero"};
            Cursor c = db.query(tabla, columnas, "numero='" + numero+"'", null, null, null, null, null);
            if (c.moveToFirst())
                telefono = new Telefono(c.getString(0), c.getString(1));
            db.close();
            c.close();
        }
        return telefono;
    }

    // Método para obtener todos los teléfonos de la tabla indicada.
    public ArrayList<Telefono> getTelefonos(String tabla)
    {
        ArrayList<Telefono> telefonos = new ArrayList<Telefono>();
        if(tabla.equals(TABLA_EXCLUIDOS) || tabla.equals(TABLA_CONFIGURADOS))
        {
            SQLiteDatabase db = getReadableDatabase();
            String[] columnas = {"nombre", "numero"};
            Cursor c = db.query(tabla, columnas, null, null, null, null, null, null);
            if(c.moveToFirst()) {
                do {
                    Telefono telefono = new Telefono(c.getString(0), c.getString(1));
                    telefonos.add(telefono);
                } while (c.moveToNext());
            }
            db.close();
            c.close();
        }
        return telefonos;
    }

    // Método para insertar una nueva llamada grabada.
    public boolean insertarLlamada(String nombre, String numero, String sentido, int duracion, String fecha, String archivo)
    {
        long salida=0;
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            ContentValues valores = new ContentValues();
            if(numero!=null && !numero.equals(""))
            {
                valores.put("numero", numero);
                if (nombre != null && !nombre.equals(""))
                    valores.put("nombre", nombre);
                valores.put("sentido", sentido);
                valores.put("duracion", duracion);
                valores.put("fecha", fecha);
                valores.put("archivo", archivo);
                salida = db.insert(TABLA_LLAMADAS, null, valores);
            }
        }
        db.close();
        return(salida>0);
    }

    // Método para obtener todas las llamadas grabadas de la tabla de llamadas
    // grabadas.
    public ArrayList<Llamada> getLlamadas()
    {
        ArrayList<Llamada> llamadas = new ArrayList<Llamada>();

        SQLiteDatabase db = getReadableDatabase();
        String[] columnas = {"nombre", "numero", "sentido", "duracion", "fecha", "archivo"};
        Cursor c = db.query(TABLA_LLAMADAS, columnas, null, null, null, null, "archivo DESC");
        if (c.moveToFirst()) {
            do {
                Llamada llamada = new Llamada(c.getString(0), c.getString(1), c.getString(2), c.getInt(3), c.getString(4), c.getString(5));
                llamadas.add(llamada);
            } while (c.moveToNext());
        }
        db.close();
        c.close();

        return llamadas;
    }

    // Método para borrar una llamada de la tabla de llamadas grabadas.
    public boolean borrarLlamada(String archivo)
    {
        SQLiteDatabase db = getWritableDatabase();
        long salida=0;
        salida=db.delete(TABLA_LLAMADAS, "archivo='" + archivo+"'", null);
        db.close();
        return(salida>0);
    }

    // Método para comprobar si un número se encuentra en la tabla
    // de teléfonos excluidos. Devuelve verdadero en caso afirmativo
    // y falso en caso contario.
    public boolean numeroExcluido(String numero){
        return checkNumeroEnDB(TABLA_EXCLUIDOS, numero);
    }

    // Método para comprobar si un número se encuentra en la tabla
    // de teléfonos configurados. Devuelve verdadero en caso afirmativo
    // y falso en caso contario.
    public boolean numeroConfigurado(String numero){
        return checkNumeroEnDB(TABLA_CONFIGURADOS, numero);
    }

    // Método para comprobar si un número se encuentra en la tabla
    // indicada. Devuelve verdadero en caso afirmativo
    // y falso en caso contario.
    private boolean checkNumeroEnDB(String tabla, String numero)
    {
        boolean ret = false;
        if(numero!=null && !numero.equals(""))
        {
            if (numero.substring(0,1).equals("+") && numero.length() > 3)
                numero = numero.substring(3);
            numero = numero.replaceAll("[()\\s-]+", "");
            SQLiteDatabase db = getReadableDatabase();
            String[] columnas = {"numero"};
            Cursor c = db.query(tabla, columnas, "numero='" + numero+"'", null, null, null, null, null);
            if (c.moveToFirst())
                ret = true;
            db.close();
            c.close();
        }
        return ret;
    }

}

