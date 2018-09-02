package com.dam.grabadoradellamadas;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Javi on 19/05/2017.
 */

// Adaptador para la lista de telefonos
public class AdaptadorListaTelefonos extends ArrayAdapter<Telefono>
{
    private final List<Telefono> mTelefonos;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private int mResource;

    public AdaptadorListaTelefonos (Context context, int resourceId, ArrayList<Telefono> telefonos)
    {
        super(context, resourceId);

        mContext = context;
        mTelefonos = telefonos;
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
            holder.nombre = (TextView)convertView.findViewById(R.id.telefonoNombre);
            holder.numero = (TextView)convertView.findViewById(R.id.telefonoNumero);
            holder.borrar = (Button)convertView.findViewById(R.id.telefonoBorrar);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }
        final Telefono telefono = getItem(position);
        if (telefono != null)
        {
            holder.numero.setText(telefono.getNumero());
            holder.nombre.setText(telefono.getNombre());
            holder.borrar.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            final String num = telefono.getNumero();
                            final String nom = telefono.getNombre();
                            final String n = (nom != null && !nom.equals("")) ? nom : num;
                            builder.setMessage("¿Eliminar " + n + "?");
                            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    if (mContext.deleteFile(num))
                                        Toast.makeText(getContext(), "Eliminado " + n, Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_LONG).show();
                                }
                            });
                            builder.setNegativeButton(R.string.no, null);
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
            );
        }
        return convertView;
    }

    @Override
    public int getCount()
    {
        return mTelefonos.size();
    }

    @Override
    public Telefono getItem(int position)
    {
        return mTelefonos.get(position);
    }

    // Holder para aumentar la eficiencia al no tener que hacer continuamente
    // findViewById.
    public class ViewHolder
    {
        TextView nombre;
        TextView numero;
        Button borrar;
    }
}
