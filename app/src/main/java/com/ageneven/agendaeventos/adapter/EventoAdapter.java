package com.ageneven.agendaeventos.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import com.ageneven.agendaeventos.R;
import com.ageneven.agendaeventos.model.Evento;
import com.ageneven.agendaeventos.util.ImagenUtil;

/** Convierte cada Evento de la lista en una tarjeta visible. */
public class EventoAdapter extends RecyclerView.Adapter<EventoAdapter.EventoViewHolder> {

    /** Avisa a la Activity cuando el usuario toca una tarjeta. */
    public interface OnEventoClickListener {
        void onEventoClick(Evento evento);
    }

    private final List<Evento> eventos = new ArrayList<>();
    private final OnEventoClickListener listener;

    public EventoAdapter(OnEventoClickListener listener) {
        this.listener = listener;
    }

    /** Reemplaza los datos y redibuja la lista. */
    public void actualizarLista(List<Evento> nuevos) {
        eventos.clear();
        if (nuevos != null) {
            eventos.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    /** 1) Crea una tarjeta vacía a partir de item_evento.xml */
    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_evento, parent, false);
        return new EventoViewHolder(vista);
    }

    /** 2) Rellena una tarjeta con los datos del evento en esa posición */
    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        Evento evento = eventos.get(position);
        Context ctx = holder.itemView.getContext();

        holder.txtTitulo.setText(evento.getTitulo());
        holder.txtFechaHora.setText(ctx.getString(R.string.fecha_hora, evento.getFecha(), evento.getHora()));
        holder.txtTipo.setText(evento.getTipo());

        // Color según el tipo (etiqueta y cuadrito)
        int color = ContextCompat.getColor(ctx, colorPorTipo(evento.getTipo()));
        holder.txtTipo.setTextColor(color);
        holder.cardMiniatura.setCardBackgroundColor(color);

        // Foto del usuario o imagen predeterminada
        boolean fotoMostrada = evento.tieneFoto() && mostrarFoto(holder.imgMiniatura, evento.getFotoUri());
        if (fotoMostrada) {
            holder.txtMiniatura.setVisibility(View.GONE);
        } else {
            holder.imgMiniatura.setImageResource(ImagenUtil.drawablePorTipo(evento.getTipo()));
            holder.txtMiniatura.setVisibility(View.GONE);
        }

        // Al tocar la tarjeta, avisa a la Activity
        holder.itemView.setOnClickListener(v -> listener.onEventoClick(evento));
    }

    /** 3) Cuántas tarjetas hay */
    @Override
    public int getItemCount() {
        return eventos.size();
    }

    /** Devuelve el color que corresponde a cada tipo */
    private int colorPorTipo(String tipo) {
        if (Evento.TIPO_CUMPLEANOS.equals(tipo)) return R.color.tipo_cumpleanos;
        if (Evento.TIPO_EVENTO.equals(tipo)) return R.color.tipo_evento;
        return R.color.tipo_otro;
    }

    /** Intenta cargar la foto; si falla (permiso o archivo borrado) devuelve false */
    private boolean mostrarFoto(ImageView imagen, String uri) {
        try {
            imagen.setImageURI(Uri.parse(uri));
            return imagen.getDrawable() != null;
        } catch (SecurityException e) {
            return false;
        }
    }

    /** Guarda las referencias a las vistas de una tarjeta para no buscarlas cada vez */
    public static class EventoViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardMiniatura;
        final ImageView imgMiniatura;
        final TextView txtMiniatura, txtTitulo, txtFechaHora, txtTipo;

        EventoViewHolder(@NonNull View vista) {
            super(vista);
            cardMiniatura = vista.findViewById(R.id.cardMiniatura);
            imgMiniatura = vista.findViewById(R.id.imgMiniatura);
            txtMiniatura = vista.findViewById(R.id.txtMiniatura);
            txtTitulo = vista.findViewById(R.id.txtTitulo);
            txtFechaHora = vista.findViewById(R.id.txtFechaHora);
            txtTipo = vista.findViewById(R.id.txtTipo);
        }
    }
}
