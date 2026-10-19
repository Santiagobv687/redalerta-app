package com.redalerta.client.ui.alert;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.redalerta.client.R;
import com.redalerta.client.databinding.ItemAlertaBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.EvidenciaResponse;
import com.redalerta.client.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class AlertaAdapter extends RecyclerView.Adapter<AlertaAdapter.AlertaViewHolder> {

    public interface OnAlertaClickListener {
        void onAlertaClick(Alerta alerta);
    }

    private final List<Alerta> listaAlertas;
    private final OnAlertaClickListener clickListener;

    public AlertaAdapter(List<Alerta> listaAlertas, OnAlertaClickListener clickListener) {
        this.listaAlertas = listaAlertas != null ? listaAlertas : new ArrayList<>();
        this.clickListener = clickListener;
    }

    public void actualizarLista(List<Alerta> nuevasAlertas) {
        this.listaAlertas.clear();
        if (nuevasAlertas != null) {
            this.listaAlertas.addAll(nuevasAlertas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlertaBinding binding = ItemAlertaBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AlertaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertaViewHolder holder, int position) {
        Alerta alerta = listaAlertas.get(position);
        holder.bind(alerta, clickListener);
    }

    @Override
    public int getItemCount() {
        return listaAlertas != null ? listaAlertas.size() : 0;
    }

    public static class AlertaViewHolder extends RecyclerView.ViewHolder {

        private final ItemAlertaBinding binding;

        public AlertaViewHolder(@NonNull ItemAlertaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Alerta alerta, OnAlertaClickListener listener) {
            Context context = itemView.getContext();

            // 1. Categoría (Chip superior)
            String categoria = alerta.getCategoria() != null ? alerta.getCategoria().toUpperCase() : "GENERAL";
            binding.chipCategoria.setText(formatearNombreCategoria(categoria));

            // 2. Indicador de Estado Accesible (Texto + Badge accesible)
            configurarEstadoAccesible(alerta.getEstado());

            // 3. Tiempo Transcurrido Legible
            String fechaIso = alerta.getFechaActualizacion() != null ? alerta.getFechaActualizacion() : alerta.getFechaCreacion();
            binding.tvTiempoTranscurrido.setText(TimeUtils.obtenerTiempoTranscurrido(fechaIso));

            // 4. Título y Extracto de Descripción
            binding.tvTitulo.setText(alerta.getTitulo());
            binding.tvDescripcionExcerpt.setText(alerta.getDescripcion());

            // 5. Ubicación del Evento (Municipio y Dirección)
            String municipio = alerta.getBarrio() != null ? alerta.getBarrio() : "Armenia";
            String direccion = alerta.getDireccion() != null ? alerta.getDireccion() : "";
            if (!direccion.isEmpty()) {
                binding.tvUbicacion.setText(municipio + " • " + direccion);
            } else {
                binding.tvUbicacion.setText(municipio);
            }

            // 6. Evidencia Fotográfica (Se procesa cuando la alerta cuenta con URL de evidencia válida)
            String urlEvidencia = alerta.getEvidenciaUrl();
            List<EvidenciaResponse> evidencias = alerta.getEvidencias();
            int totalEvidencias = alerta.getTotalEvidencias();

            if (!esUrlImagenValida(urlEvidencia) && evidencias != null && !evidencias.isEmpty()) {
                if (evidencias.get(0) != null) {
                    urlEvidencia = evidencias.get(0).getUrl();
                }
            }

            if (esUrlImagenValida(urlEvidencia)) {
                binding.layoutEvidenciaContainer.setVisibility(View.VISIBLE);

                int cantidadTotalEvidencias = 0;
                if (evidencias != null && !evidencias.isEmpty()) {
                    cantidadTotalEvidencias = evidencias.size();
                }
                if (totalEvidencias > cantidadTotalEvidencias) {
                    cantidadTotalEvidencias = totalEvidencias;
                }

                int extrasCount = Math.max(0, cantidadTotalEvidencias - 1);

                if (extrasCount > 0) {
                    binding.tvBadgeFotosExtra.setVisibility(View.VISIBLE);
                    binding.tvBadgeFotosExtra.setText("+" + extrasCount);
                } else {
                    binding.tvBadgeFotosExtra.setVisibility(View.GONE);
                }

                Glide.with(context)
                        .load(urlEvidencia.trim())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(600, 338)
                        .fitCenter()
                        .placeholder(R.drawable.ic_photo_library)
                        .error(R.drawable.ic_photo_library)
                        .into(binding.imgEvidenciaPreview);
            } else {
                binding.layoutEvidenciaContainer.setVisibility(View.GONE);
            }

            // 7. Autor y Código de Seguimiento
            String autor = alerta.isEsAnonima() ? "Anónimo" : alerta.getUsuarioNombre();
            if (autor == null || autor.isEmpty()) autor = "Ciudadano";
            binding.tvAutor.setText("Reportado por: " + autor);

            String codigo = alerta.getId() != null ? "ALR-" + alerta.getId() : "ALR-000";
            binding.tvCodigoAlerta.setText(codigo);

            // Click listener en la tarjeta
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlertaClick(alerta);
                }
            });
        }

        private boolean esUrlImagenValida(String url) {
            if (url == null) return false;
            String urlClean = url.trim().toLowerCase();
            if (urlClean.isEmpty() || urlClean.equals("null") || urlClean.equals("none")
                    || urlClean.equals("n/a") || urlClean.equals("{}") || urlClean.equals("[]")) {
                return false;
            }
            return urlClean.startsWith("http://") || urlClean.startsWith("https://")
                    || urlClean.startsWith("data:image/") || urlClean.startsWith("content://")
                    || urlClean.startsWith("file://");
        }

        private String formatearNombreCategoria(String cat) {
            return switch (cat) {
                case "DESASTRE_NATURAL" -> "DESASTRE NATURAL";
                case "SEGURIDAD_CIUDADANA" -> "SEGURIDAD CIUDADANA";
                case "EMERGENCIA_MEDICA" -> "EMERGENCIA MÉDICA";
                case "ROBO" -> "ROBO / HURTO";
                default -> cat.replace("_", " ");
            };
        }

        private void configurarEstadoAccesible(String estado) {
            Context ctx = itemView.getContext();
            String estadoNormalizado = estado != null ? estado.toUpperCase() : "PENDIENTE_REVISION";

            switch (estadoNormalizado) {
                case "RESUELTA", "CERRADA" -> {
                    binding.tvStatusBadge.setText("RESUELTA");
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_resuelta);
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_resuelta_text));
                }
                case "EN_GESTION", "EN_ATENCION" -> {
                    binding.tvStatusBadge.setText("EN GESTIÓN");
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_atencion);
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_atencion_text));
                }
                case "DIRECCIONADA", "VERIFICADA" -> {
                    binding.tvStatusBadge.setText("DIRECCIONADA");
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_verificada);
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_verificada_text));
                }
                default -> {
                    binding.tvStatusBadge.setText("PENDIENTE REVISIÓN");
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_revision);
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_revision_text));
                }
            }
        }
    }
}
