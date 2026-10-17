package com.redalerta.client.ui.alert;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redalerta.client.R;
import com.redalerta.client.model.Alerta;

import java.util.List;

public class MisPublicacionesAdapter extends RecyclerView.Adapter<MisPublicacionesAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEditar(Alerta alerta);
        void onEliminar(Alerta alerta);
    }

    private final List<Alerta> alertas;
    private final OnItemActionListener listener;

    public MisPublicacionesAdapter(List<Alerta> alertas, OnItemActionListener listener) {
        this.alertas = alertas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mi_publicacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alerta alerta = alertas.get(position);

        holder.tvFechaActualizacion.setText(
                alerta.getFechaActualizacion() != null
                        ? alerta.getFechaActualizacion()
                        : alerta.getFechaCreacion()
        );

        holder.tvTitulo.setText(alerta.getTitulo());
        holder.tvDescripcion.setText(alerta.getDescripcion());
        holder.tvUbicacion.setText(alerta.getDireccion() + ", " + alerta.getBarrio());
        holder.tvAutor.setText(alerta.isEsAnonima() ? "Anónimo" : "Por " + alerta.getUsuarioNombre());

        // Configurar estado (color y texto)
        configurarEstado(holder.tvEstado, alerta.getEstado());

        // Menú de 3 puntos
        holder.btnOpciones.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 1, R.string.mis_pub_editar);
            popup.getMenu().add(0, 2, 2, R.string.mis_pub_eliminar);

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    listener.onEditar(alerta);
                    return true;
                } else if (item.getItemId() == 2) {
                    listener.onEliminar(alerta);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void configurarEstado(TextView tvEstado, String estado) {
        Context ctx = tvEstado.getContext();
        if (estado == null) estado = "PENDIENTE";

        switch (estado.toUpperCase()) {
            case "RESUELTA":
            case "CERRADA":
                tvEstado.setText("Resuelta");
                tvEstado.setBackgroundResource(R.drawable.bg_status_resuelta);
                tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_resuelta_text));
                break;
            case "EN_REVISION":
            case "VERIFICADA":
                tvEstado.setText("En revisión");
                tvEstado.setBackgroundResource(R.drawable.bg_status_revision);
                tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_revision_text));
                break;
            default:
                tvEstado.setText("Pendiente");
                tvEstado.setBackgroundResource(R.drawable.bg_status_pendiente);
                tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_pendiente_text));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return alertas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFechaActualizacion, tvEstado, tvTitulo, tvDescripcion, tvUbicacion, tvAutor;
        ImageView btnOpciones;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFechaActualizacion = itemView.findViewById(R.id.tvFechaActualizacion);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
            tvAutor = itemView.findViewById(R.id.tvAutor);
            btnOpciones = itemView.findViewById(R.id.btnOpciones);
        }
    }
}