package com.dam.grabadoradellamadas;

/**
 * Created by Javi on 19/05/2017.
 */

// Clase para almacenar toda la información relativa a una llamada.
public class Llamada
{

    public final static String OUTGOING = "Saliente";
    public final static String INCOMING = "Entrante";

    private String nombre;
    private String numero;
    private String fecha;
    private String sentido;
    private int duracion;
    private String archivo;

    public Llamada(String nombre, String numero, String sentido, int duracion, String fecha, String archivo) {
        this.nombre = nombre;
        this.numero = numero;
        this.fecha = fecha;
        this.sentido = sentido;
        this.duracion = duracion;
        this.archivo = archivo;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNumero() {
        return numero;
    }
    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getFecha() {
        return fecha;
    }
    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getSentido() {
        return sentido;
    }
    public void setSentido(String sentido) {
        this.sentido = sentido;
    }

    public int getDuracion() {
        return duracion;
    }
    public void setDuracion(int duracion) {
        this.duracion = duracion;
    }

    public String getArchivo() {
        return archivo;
    }
    public void setArchivo(String archivo) {
        this.archivo = archivo;
    }

}