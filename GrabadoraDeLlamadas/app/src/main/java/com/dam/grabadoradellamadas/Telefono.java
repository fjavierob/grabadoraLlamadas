package com.dam.grabadoradellamadas;

/**
 * Created by Javi on 19/05/2017.
 */
// Clase para almacenar toda la información relativa a un teléfono (que se configura o excluye).
public class Telefono
{

    private String nombre;
    private String numero;

    public Telefono(String nombre, String numero) {
        this.nombre = nombre;
        this.numero = numero;
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

}
