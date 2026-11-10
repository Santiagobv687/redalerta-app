package com.redalerta.client.ui.notificacion;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redalerta.client.R;
import com.redalerta.client.databinding.ItemNotificacionBinding;
import com.redalerta.client.model.notificacion.NotificacionResponse;
import com.redalerta.client.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder> {

    public interface OnNotificacionClickListener {
        void onNotificacionClick(@NonNull NotificacionResponse notificacion);
    }

    private final List<NotificacionResponse> items = new ArrayList<>();
    private final OnNotificacionClickListener listener;

    public NotificacionAdapter(List<NotificacionResponse> initialItems, OnNotificacionClickListener listener) {
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
        this.listener = listener;
    }

    public void actualizarLista(List<NotificacionResponse> nuevas) {
        items.clear();
        if (nuevas != null) {
            items.addAll(nuevas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificacionBinding binding = ItemNotificacionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NotificacionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacionViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public NotificacionResponse getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    static class NotificacionViewHolder extends RecyclerView.ViewHolder {

        private final ItemNotificacionBinding binding;

        NotificacionViewHolder(ItemNotificacionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NotificacionResponse item, OnNotificacionClickListener listener) {
            Context ctx = binding.getRoot().getContext();

            // Título y tiempo relativo
            String titulo = item.getTitulo() != null && !item.getTitulo().trim().isEmpty()
                    ? item.getTitulo().trim()
                    : "Actualización de Alerta";
            binding.tvTituloNotificacion.setText(titulo);
            binding.tvFechaNotificacion.setText(TimeUtils.obtenerTiempoTranscurrido(item.getFechaCreacion()));

            // Mensaje corto
            String mensaje = item.getMensaje() != null ? item.getMensaje().trim() : "";
            binding.tvMensajeNotificacion.setText(mensaje);

            // Diferenciación visual  entre leída y no leída
            boolean noLeida = !item.isLeida();
            binding.viewPuntoNoLeida.setVisibility(noLeida ? View.VISIBLE : View.GONE);

            // 1. Tipografía: Negrita destacada para no leídas
            binding.tvTituloNotificacion.setTypeface(null, noLeida ? Typeface.BOLD : Typeface.NORMAL);
            binding.tvTituloNotificacion.setTextColor(ContextCompat.getColor(
                    ctx, noLeida ? R.color.ra_light_text : R.color.ra_light_text_secondary));
            binding.tvMensajeNotificacion.setTextColor(ContextCompat.getColor(
                    ctx, noLeida ? R.color.ra_light_text : R.color.ra_text_muted));

            // 2. Fondo, borde, elevación y opacidad
            float densidad = ctx.getResources().getDisplayMetrics().density;
            binding.cardNotificacion.setStrokeColor(ContextCompat.getColor(
                    ctx, noLeida ? R.color.ra_summary_border : R.color.ra_light_border));
            binding.cardNotificacion.setCardBackgroundColor(ContextCompat.getColor(
                    ctx, noLeida ? R.color.ra_light_card : R.color.ra_footer_bg));
            binding.cardNotificacion.setCardElevation(noLeida ? 1.5f * densidad : 0f);
            binding.cardNotificacion.setAlpha(noLeida ? 1.0f : 0.72f);

            // 3. Accesibilidad TalkBack explícita para no vidente
            String estadoLecturaAccesible = noLeida ? "No leída. " : "Leída. ";
            binding.cardNotificacion.setContentDescription(
                    estadoLecturaAccesible + titulo + ". " + mensaje);

            // Estado resultante
            configurarBadgeEstado(ctx, item.getNuevoEstado());

            // Responsable
            String responsable = item.getResponsableNombre();
            if (responsable != null && !responsable.trim().isEmpty()) {
                binding.tvResponsableNotif.setVisibility(View.VISIBLE);
                binding.tvResponsableNotif.setText("• " + responsable.trim());
            } else {
                binding.tvResponsableNotif.setVisibility(View.GONE);
            }

            // Motivo / observación opcional
            String motivo = item.getMotivoAccion();
            if (motivo != null && !motivo.trim().isEmpty()) {
                binding.tvMotivoNotif.setVisibility(View.VISIBLE);
                binding.tvMotivoNotif.setText("\"" + motivo.trim() + "\"");
            } else {
                binding.tvMotivoNotif.setVisibility(View.GONE);
            }

            binding.cardNotificacion.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificacionClick(item);
                }
            });
        }

        private void configurarBadgeEstado(Context ctx, String estadoRaw) {
            String estado = estadoRaw != null ? estadoRaw.toUpperCase() : "";
            switch (estado) {
                case "DIRECCIONADA" -> {
                    binding.tvEstadoBadgeNotif.setVisibility(View.VISIBLE);
                    binding.tvEstadoBadgeNotif.setText("DIRECCIONADA");
                    binding.tvEstadoBadgeNotif.setBackgroundResource(R.drawable.bg_status_verificada);
                    binding.tvEstadoBadgeNotif.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_verificada_text));
                    binding.ivIconoEstadoNotif.setImageResource(R.drawable.ic_notifications);
                    binding.ivIconoEstadoNotif.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.ra_status_verificada_text)));
                }
                case "EN_GESTION", "EN_ATENCION" -> {
                    binding.tvEstadoBadgeNotif.setVisibility(View.VISIBLE);
                    binding.tvEstadoBadgeNotif.setText("EN GESTIÓN");
                    binding.tvEstadoBadgeNotif.setBackgroundResource(R.drawable.bg_status_atencion);
                    binding.tvEstadoBadgeNotif.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_atencion_text));
                    binding.ivIconoEstadoNotif.setImageResource(R.drawable.ic_schedule);
                    binding.ivIconoEstadoNotif.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.ra_status_atencion_text)));
                }
                case "RESUELTA" -> {
                    binding.tvEstadoBadgeNotif.setVisibility(View.VISIBLE);
                    binding.tvEstadoBadgeNotif.setText("RESUELTA");
                    binding.tvEstadoBadgeNotif.setBackgroundResource(R.drawable.bg_status_resuelta);
                    binding.tvEstadoBadgeNotif.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_resuelta_text));
                    binding.ivIconoEstadoNotif.setImageResource(R.drawable.ic_check_success);
                    binding.ivIconoEstadoNotif.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.ra_status_resuelta_text)));
                }
                case "RECHAZADA" -> {
                    binding.tvEstadoBadgeNotif.setVisibility(View.VISIBLE);
                    binding.tvEstadoBadgeNotif.setText("RECHAZADA");
                    binding.tvEstadoBadgeNotif.setBackgroundResource(R.drawable.bg_status_pendiente);
                    binding.tvEstadoBadgeNotif.setTextColor(ContextCompat.getColor(ctx, R.color.ra_red_dark));
                    binding.ivIconoEstadoNotif.setImageResource(R.drawable.ic_warning);
                    binding.ivIconoEstadoNotif.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.ra_red)));
                }
                case "PENDIENTE_REVISION" -> {
                    binding.tvEstadoBadgeNotif.setVisibility(View.VISIBLE);
                    binding.tvEstadoBadgeNotif.setText("EN REVISIÓN");
                    binding.tvEstadoBadgeNotif.setBackgroundResource(R.drawable.bg_status_revision);
                    binding.tvEstadoBadgeNotif.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_revision_text));
                    binding.ivIconoEstadoNotif.setImageResource(R.drawable.ic_notifications);
                    binding.ivIconoEstadoNotif.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.ra_status_revision_text)));
                }
                default -> {
                    binding.tvEstadoBadgeNotif.setVisibility(View.GONE);
                    binding.ivIconoEstadoNotif.setImageResource(R.drawable.ic_notifications);
                    binding.ivIconoEstadoNotif.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.ra_red)));
                }
            }
        }
    }
}
