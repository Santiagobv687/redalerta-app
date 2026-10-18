package com.redalerta.client.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.redalerta.client.R;
import com.redalerta.client.model.alerta.*;

import java.util.List;

public class VerificacionAdapter extends RecyclerView.Adapter<VerificacionAdapter.ViewHolder> {

    public interface OnItemClickListener { void onClick(Alerta alerta); }

    private final List<Alerta> alertas;
    private final OnItemClickListener listener;

    public VerificacionAdapter(List<Alerta> alertas, OnItemClickListener listener) {
        this.alertas = alertas;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_verificacion_alerta, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alerta a = alertas.get(position);

        holder.tvId.setText(a.getId() != null ? a.getId() : "—");
        holder.tvTitulo.setText((a.getTitulo() != null ? a.getTitulo() : "—")
                + " — " + (a.getBarrio() != null ? a.getBarrio() : ""));
        holder.tvDescripcion.setText(a.getDescripcion() != null ? a.getDescripcion() : "");

        String fecha = a.getFechaActualizacion() != null ? a.getFechaActualizacion() : a.getFechaCreacion();
        String direccion = a.getDireccion() != null ? a.getDireccion() : "";
        holder.tvFooter.setText(direccion + " · " + (fecha != null ? fecha : ""));

        // ⬇️ Prioridad con protección null
        String prioridad = a.getPrioridad() != null ? a.getPrioridad() : PrioridadAlerta.BAJA;
        holder.tvPrioridad.setText(prioridad);

        int colorPrio = prioridad.equalsIgnoreCase(PrioridadAlerta.ALTA) ? R.color.ra_prio_alta :
                prioridad.equalsIgnoreCase(PrioridadAlerta.MEDIA) ? R.color.ra_prio_media : R.color.ra_prio_baja;

        holder.tvPrioridad.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorPrio));
        holder.dotPrioridad.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), colorPrio));

        // ⬇️ Estado con protección null
        String estado = a.getEstado() != null ? a.getEstado() : "PENDIENTE";

        if (estado.equals("RECHAZADA")) {
            holder.tvEstado.setText("Rechazada");
            holder.tvEstado.setBackgroundResource(R.drawable.bg_status_revision); // usa rojo
            holder.tvEstado.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.ra_red));
        } else if (estado.equals("VERIFICADA")) {
            holder.tvEstado.setText("Verificada");
            holder.tvEstado.setBackgroundResource(R.drawable.bg_status_verificada);
            holder.tvEstado.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.ra_status_verificada_text));
        } else {
            holder.tvEstado.setText("Pendiente");
            holder.tvEstado.setBackgroundResource(R.drawable.bg_status_pendiente);
            holder.tvEstado.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.ra_status_pendiente_text));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(a));
    }

    @Override public int getItemCount() { return alertas.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvPrioridad, tvEstado, tvTitulo, tvDescripcion, tvFooter;
        View dotPrioridad;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tvId);
            tvPrioridad = itemView.findViewById(R.id.tvPrioridad);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFooter = itemView.findViewById(R.id.tvFooter);
            dotPrioridad = itemView.findViewById(R.id.dotPrioridad);
        }
    }
}