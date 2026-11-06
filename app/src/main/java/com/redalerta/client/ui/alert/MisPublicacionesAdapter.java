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
import com.redalerta.client.databinding.ItemMiPublicacionBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.EvidenciaResponse;
import com.redalerta.client.util.TimeUtils;

import java.util.List;

public class MisPublicacionesAdapter extends RecyclerView.Adapter<MisPublicacionesAdapter.ViewHolder> {

    public interface OnAccionesAlertaListener {
        void onAlertaClick(Alerta alerta);
        void onModificarClick(Alerta alerta);
        void onEliminarClick(Alerta alerta);
    }

    private final List<Alerta> alertas;
    private final OnAccionesAlertaListener listener;

    public MisPublicacionesAdapter(List<Alerta> alertas, OnAccionesAlertaListener listener) {
        this.alertas = alertas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMiPublicacionBinding binding = ItemMiPublicacionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alerta alerta = alertas.get(position);
        holder.bind(alerta, listener);
    }

    @Override
    public int getItemCount() {
        return alertas != null ? alertas.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemMiPublicacionBinding binding;

        public ViewHolder(@NonNull ItemMiPublicacionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Alerta alerta, OnAccionesAlertaListener listener) {
            Context context = itemView.getContext();

            // 1. Fecha legible
            String fecha = alerta.getFechaCreacion() != null ? alerta.getFechaCreacion() : alerta.getFechaActualizacion();
            binding.tvFechaCreacion.setText(TimeUtils.obtenerTiempoTranscurrido(fecha));

            // 2. Estado accesible (WCAG 2.1 AA)
            configurarEstadoAccesible(alerta.getEstado());

            // 3. Título y Descripción
            binding.tvTitulo.setText(alerta.getTitulo());
            binding.tvDescripcion.setText(alerta.getDescripcion());

            // 4. Ubicación (Municipio y Dirección)
            String mun = alerta.getBarrio() != null ? alerta.getBarrio() : "Armenia";
            String dir = alerta.getDireccion() != null ? alerta.getDireccion() : "";
            binding.tvUbicacion.setText(!dir.isEmpty() ? mun + " • " + dir : mun);

            // 5. Miniatura de evidencia cuadrada (64x64 dp)
            String urlEvidencia = alerta.getEvidenciaUrl();
            List<EvidenciaResponse> evidencias = alerta.getEvidencias();

            if (!esUrlImagenValida(urlEvidencia) && evidencias != null && !evidencias.isEmpty()) {
                if (evidencias.get(0) != null) {
                    urlEvidencia = evidencias.get(0).getUrl();
                }
            }

            if (esUrlImagenValida(urlEvidencia)) {
                binding.cardMiniaturaContainer.setVisibility(View.VISIBLE);

                Glide.with(context)
                        .load(urlEvidencia.trim())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_photo_library)
                        .error(R.drawable.ic_photo_library)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                binding.cardMiniaturaContainer.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                          Target<Drawable> target, DataSource dataSource,
                                                          boolean isFirstResource) {
                                binding.cardMiniaturaContainer.setVisibility(View.VISIBLE);
                                return false;
                            }
                        })
                        .into(binding.imgMiniaturaEvidencia);
            } else {
                binding.cardMiniaturaContainer.setVisibility(View.GONE);
            }

            // 6. Pie: Código de la alerta
            binding.tvCodigoAlerta.setText(alerta.getId() != null ? "ALR-" + alerta.getId() : "ALR-000");

            // 7. Visibilidad de Botones Modificar / Eliminar (Solo cuando está en PENDIENTE DE REVISIÓN)
            String estadoNorm = alerta.getEstado() != null ? alerta.getEstado().toUpperCase() : "PENDIENTE_REVISION";
            boolean esPendiente = estadoNorm.contains("PENDIENTE") || estadoNorm.contains("REVISION");
            if (esPendiente) {
                binding.layoutAccionesModificar.setVisibility(View.VISIBLE);
                binding.btnModificarAlerta.setOnClickListener(v -> {
                    if (listener != null) listener.onModificarClick(alerta);
                });
                binding.btnEliminarAlerta.setOnClickListener(v -> {
                    if (listener != null) listener.onEliminarClick(alerta);
                });
            } else {
                binding.layoutAccionesModificar.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAlertaClick(alerta);
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

        private void configurarEstadoAccesible(String estado) {
            Context ctx = itemView.getContext();
            String estadoNormalizado = estado != null ? estado.toUpperCase() : "PENDIENTE_REVISION";

            switch (estadoNormalizado) {
                case "DIRECCIONADA" -> {
                    binding.tvEstadoBadge.setText("DIRECCIONADA");
                    binding.tvEstadoBadge.setBackgroundResource(R.drawable.bg_status_verificada);
                    binding.tvEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_verificada_text));
                    binding.tvEstadoBadge.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(ctx, R.drawable.ic_check_success), null, null, null);
                }
                case "EN_GESTION", "EN_ATENCION" -> {
                    binding.tvEstadoBadge.setText("EN GESTIÓN");
                    binding.tvEstadoBadge.setBackgroundResource(R.drawable.bg_status_atencion);
                    binding.tvEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_atencion_text));
                    binding.tvEstadoBadge.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(ctx, R.drawable.ic_refresh), null, null, null);
                }
                case "RESUELTA" -> {
                    binding.tvEstadoBadge.setText("RESUELTA");
                    binding.tvEstadoBadge.setBackgroundResource(R.drawable.bg_status_resuelta);
                    binding.tvEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_resuelta_text));
                    binding.tvEstadoBadge.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(ctx, R.drawable.ic_check_success), null, null, null);
                }
                case "RECHAZADA" -> {
                    binding.tvEstadoBadge.setText("RECHAZADA");
                    binding.tvEstadoBadge.setBackgroundResource(R.drawable.bg_status_pendiente);
                    binding.tvEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_pendiente_text));
                    binding.tvEstadoBadge.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(ctx, R.drawable.ic_warning), null, null, null);
                }
                default -> {
                    binding.tvEstadoBadge.setText("EN REVISIÓN");
                    binding.tvEstadoBadge.setBackgroundResource(R.drawable.bg_status_revision);
                    binding.tvEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_revision_text));
                    binding.tvEstadoBadge.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(ctx, R.drawable.ic_schedule), null, null, null);
                }
            }
        }
    }
}
