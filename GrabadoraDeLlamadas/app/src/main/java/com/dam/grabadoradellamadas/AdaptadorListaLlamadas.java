package com.dam.grabadoradellamadas;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

/**
 * Created by Javi on 19/05/2017.
 */

// Adaptador para la lista de llamadas
public class AdaptadorListaLlamadas extends ArrayAdapter<Llamada>
{
    private final List<Llamada> mLlamadas;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private int mResource;

    public AdaptadorListaLlamadas (Context context, int resourceId, ArrayList<Llamada> llamadas)
    {
        super(context, resourceId);

        mContext = context;
        mLlamadas = llamadas;
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;
        if (convertView == null)
        {
            convertView = mInflater.inflate(mResource, null);
            holder = new ViewHolder();
            holder.nombre = (TextView)convertView.findViewById(R.id.llamadaNombre);
            holder.numero = (TextView)convertView.findViewById(R.id.llamadaNumero);
            holder.fecha = (TextView)convertView.findViewById(R.id.llamadaFecha);
            holder.sentido = (ImageView)convertView.findViewById(R.id.llamadaDireccion);
            holder.duracion = (TextView)convertView.findViewById(R.id.llamadaDuracion);
            holder.archivo = (TextView)convertView.findViewById(R.id.llamadaArchivo);

            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }
        final Llamada llamada = getItem(position);
        if (llamada != null)
        {
            holder.numero.setText(llamada.getNumero());
            holder.nombre.setText(llamada.getNombre());
            holder.fecha.setText(llamada.getFecha());
            if (llamada.getSentido().equals(Llamada.INCOMING))
                holder.sentido.setImageResource(R.drawable.icon_incoming);
            else if (llamada.getSentido().equals(Llamada.OUTGOING))
                holder.sentido.setImageResource(R.drawable.icon_outgoing);
            else
                holder.sentido.setImageResource(R.drawable.icon_llamada);
            holder.duracion.setText(secondsToString(llamada.getDuracion()));
            holder.archivo.setText(llamada.getArchivo());
        }
        return convertView;
    }

    public static String secondsToString(int seconds)
    {
        int horas = (int) Math.ceil(seconds/(60*60));
        seconds -= horas*60*60;
        int minutos = (int) Math.ceil(seconds/60);
        seconds -= minutos*60;

        String duracion = "";
        if (horas > 0)
            duracion += horas+"h ";
        if (minutos > 0 || horas > 0)
            duracion += minutos+"m ";
        duracion += seconds+"s";

        return duracion;
    }

    @Override
    public int getCount()
    {
        return mLlamadas.size();
    }

    @Override
    public Llamada getItem(int position)
    {
        return mLlamadas.get(position);
    }

    // Holder para aumentar la eficiencia al no tener que hacer continuamente
    // findViewById.
    public class ViewHolder {
        TextView nombre;
        TextView numero;
        TextView fecha;
        ImageView sentido;
        TextView duracion;
        TextView archivo;
    }

}
