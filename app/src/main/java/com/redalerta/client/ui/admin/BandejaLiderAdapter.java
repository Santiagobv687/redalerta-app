package com.redalerta.client.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.redalerta.client.databinding.ItemAlertaGestionBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.util.TimeUtils;

import java.util.List;

public class BandejaLiderAdapter extends RecyclerView.Adapter<BandejaLiderAdapter.ViewHolder> {

    public interface OnAlertaClickListener {
        void onAlertaClick(Alerta alerta);
    }

    private final List<Alerta> alertas;
    private final OnAlertaClickListener listener;

    public BandejaLiderAdapter(List<Alerta> alertas, OnAlertaClickListener listener) {
        this.alertas = alertas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlertaGestionBinding binding = ItemAlertaGestionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(alertas.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return alertas != null ? alertas.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemAlertaGestionBinding binding;

        public ViewHolder(@NonNull ItemAlertaGestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Alerta alerta, OnAlertaClickListener listener) {
            String fecha = alerta.getFechaCreacion() != null ? alerta.getFechaCreacion() : alerta.getFechaActualizacion();
            binding.tvFecha.setText(TimeUtils.obtenerTiempoTranscurrido(fecha));

            binding.tvDescripcion.setText(alerta.getDescripcion());
            
            String org = alerta.getOrganizacionNombre() != null ? alerta.getOrganizacionNombre() : "Sin asignar";
            binding.tvOrganizacion.setText("Asignado a: " + org);

            String prio = alerta.getPrioridad() != null ? alerta.getPrioridad() : "NO DEFINIDA";
            binding.tvPrioridad.setText("Prioridad: " + prio);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAlertaClick(alerta);
            });
        }
    }
}
