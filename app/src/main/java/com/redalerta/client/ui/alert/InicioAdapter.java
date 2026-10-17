package com.redalerta.client.ui.alert;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.redalerta.client.R;
import com.redalerta.client.model.Alerta;
import java.util.List;

public class InicioAdapter extends RecyclerView.Adapter<InicioAdapter.ViewHolder> {

    private final List<Alerta> alertas;

    public InicioAdapter(List<Alerta> alertas) {
        this.alertas = alertas;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inicio_alerta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alerta alerta = alertas.get(position);

        // Avatar (primera letra del nombre)
        String nombre = alerta.isEsAnonima() ? "Anónimo" : alerta.getUsuarioNombre();
        if (nombre == null || nombre.isEmpty()) nombre = "A";
        holder.tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        holder.tvNombre.setText(nombre);

        // Fecha y demás datos
        holder.tvFecha.setText(alerta.getFechaActualizacion() != null
                ? alerta.getFechaActualizacion() : alerta.getFechaCreacion());
        holder.tvTitulo.setText(alerta.getTitulo());
        holder.tvDescripcion.setText(alerta.getDescripcion());
        holder.tvBarrio.setText(alerta.getBarrio());
        holder.tvDireccion.setText(alerta.getDireccion());
        holder.tvCodigo.setText(alerta.getId()); // Usamos el ID como código ALR

        configurarEstado(holder.tvEstado, alerta.getEstado());
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
            case "EN_ATENCION":
            case "DIRECCIONADA":
                tvEstado.setText("En atención");
                tvEstado.setBackgroundResource(R.drawable.bg_status_atencion);
                tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_atencion_text));
                break;
            case "VERIFICADA":
                tvEstado.setText("Verificada");
                tvEstado.setBackgroundResource(R.drawable.bg_status_verificada);
                tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_verificada_text));
                break;
            case "EN_REVISION":
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
        TextView tvAvatar, tvNombre, tvFecha, tvEstado, tvTitulo, tvDescripcion, tvBarrio, tvDireccion, tvCodigo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvBarrio = itemView.findViewById(R.id.tvBarrio);
            tvDireccion = itemView.findViewById(R.id.tvDireccion);
            tvCodigo = itemView.findViewById(R.id.tvCodigo);
        }
    }
}