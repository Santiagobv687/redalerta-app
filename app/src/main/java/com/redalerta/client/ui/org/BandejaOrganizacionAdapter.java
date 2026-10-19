package com.redalerta.client.ui.org;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redalerta.client.R;
import com.redalerta.client.databinding.ItemAlertaOrganizacionBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.util.TimeUtils;

import java.util.List;

public class BandejaOrganizacionAdapter extends RecyclerView.Adapter<BandejaOrganizacionAdapter.ViewHolder> {

    public interface OnAlertaClickListener {
        void onAlertaClick(Alerta alerta);
    }

    private final List<Alerta> alertas;
    private final OnAlertaClickListener listener;

    public BandejaOrganizacionAdapter(List<Alerta> alertas, OnAlertaClickListener listener) {
        this.alertas = alertas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlertaOrganizacionBinding binding = ItemAlertaOrganizacionBinding.inflate(
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

        private final ItemAlertaOrganizacionBinding binding;

        public ViewHolder(@NonNull ItemAlertaOrganizacionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Alerta alerta, OnAlertaClickListener listener) {
            Context context = itemView.getContext();

            // 1. Remitente (Líder)
            String lider = alerta.getLiderNombre() != null ? alerta.getLiderNombre() : "Líder Comunitario";
            binding.tvRemitenteLider.setText("Remitido por: " + lider);

            // 2. Fecha y Hora
            String fecha = alerta.getFechaCreacion() != null ? alerta.getFechaCreacion() : alerta.getFechaActualizacion();
            binding.tvFecha.setText(TimeUtils.obtenerTiempoTranscurrido(fecha));

            // 3. Categoría
            String cat = alerta.getCategoria() != null ? alerta.getCategoria().replace("_", " ").toUpperCase() : "GENERAL";
            binding.tvCategoriaChip.setText(cat);

            // 4. Chip de Prioridad Sobrio (ALTA: Carmesí, MEDIA: Ámbar, BAJA: Verde Bosque)
            configurarChipPrioridad(context, alerta.getPrioridad());

            // 5. Descripción sintetizada
            binding.tvDescripcion.setText(alerta.getDescripcion());

            // 6. Ubicación (Municipio y Dirección)
            String mun = alerta.getBarrio() != null ? alerta.getBarrio() : "Armenia";
            String dir = alerta.getDireccion() != null ? alerta.getDireccion() : "";
            binding.tvUbicacion.setText(!dir.isEmpty() ? mun + " • " + dir : mun);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAlertaClick(alerta);
            });
        }

        private void configurarChipPrioridad(Context context, String prioridad) {
            String prioNorm = prioridad != null ? prioridad.toUpperCase() : "MEDIA";
            int color;

            switch (prioNorm) {
                case "ALTA" -> {
                    color = ContextCompat.getColor(context, R.color.ra_red);
                    binding.tvPrioridadChip.setText("PRIORIDAD ALTA");
                }
                case "BAJA" -> {
                    color = Color.parseColor("#2E7D32"); // Verde Bosque
                    binding.tvPrioridadChip.setText("PRIORIDAD BAJA");
                }
                default -> {
                    color = Color.parseColor("#FB8C00"); // Ámbar / Naranja
                    binding.tvPrioridadChip.setText("PRIORIDAD MEDIA");
                }
            }

            binding.tvPrioridadChip.setTextColor(color);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(16f);
            shape.setStroke(3, color);
            shape.setColor(Color.parseColor("#10" + String.format("%06X", (0xFFFFFF & color)))); // Fondo tonal muy suave
            binding.tvPrioridadChip.setBackground(shape);
        }
    }
}
