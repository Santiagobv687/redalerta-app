package com.redalerta.client.ui.notificacion;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.redalerta.client.R;
import com.redalerta.client.databinding.BottomSheetDetalleNotificacionBinding;
import com.redalerta.client.model.notificacion.NotificacionResponse;
import com.redalerta.client.util.TimeUtils;
import com.redalerta.client.viewmodel.NotificacionViewModel;

/**
 * BottomSheet ergonómico para visualizar el detalle explicativo de una notificación
 * (definido en NotificacionResponseDto).
 * Consume GET /api/v1/notificaciones/{id} y sincroniza el marcado de lectura.
 */
public class DetalleNotificacionBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "DetalleNotificacionBottomSheet";
    private static final String ARG_NOTIFICACION = "arg_notificacion";

    public interface OnVerAlertaListener {
        void onVerAlerta(@NonNull NotificacionResponse notificacion);
    }

    public interface OnEliminarNotificacionListener {
        void onEliminar(@NonNull NotificacionResponse notificacion);
    }

    private BottomSheetDetalleNotificacionBinding binding;
    private NotificacionResponse notificacion;
    private OnVerAlertaListener verAlertaListener;
    private OnEliminarNotificacionListener eliminarListener;
    private NotificacionViewModel viewModel;

    public static DetalleNotificacionBottomSheet newInstance(@NonNull NotificacionResponse notificacion) {
        DetalleNotificacionBottomSheet fragment = new DetalleNotificacionBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_NOTIFICACION, notificacion);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnVerAlertaListener(OnVerAlertaListener listener) {
        this.verAlertaListener = listener;
    }

    public void setOnEliminarNotificacionListener(OnEliminarNotificacionListener listener) {
        this.eliminarListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificacion = getArguments().getSerializable(ARG_NOTIFICACION, NotificacionResponse.class);
            } else {
                notificacion = (NotificacionResponse) getArguments().getSerializable(ARG_NOTIFICACION);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetDetalleNotificacionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(NotificacionViewModel.class);

        if (notificacion == null) {
            dismiss();
            return;
        }

        // Renderizado inicial con los datos en memoria
        popularDatosNotificacion(notificacion);

        // Marcado como leída inmediato al abrir
        if (notificacion.getId() != null) {
            viewModel.marcarComoLeida(notificacion.getId());
            notificacion.setLeida(true);
        }

        // Botones de acción
        binding.btnCerrarDetalleNotif.setOnClickListener(v -> dismiss());
        binding.btnEntendidoNotif.setOnClickListener(v -> dismiss());

        binding.btnEliminarDetalleNotif.setOnClickListener(v -> {
            dismiss();
            if (eliminarListener != null && notificacion != null) {
                eliminarListener.onEliminar(notificacion);
            }
        });

        if (notificacion.getAlertaId() != null) {
            binding.btnVerAlertaNotif.setVisibility(View.VISIBLE);
            binding.btnVerAlertaNotif.setOnClickListener(v -> {
                dismiss();
                if (verAlertaListener != null) {
                    verAlertaListener.onVerAlerta(notificacion);
                }
            });
        } else {
            binding.btnVerAlertaNotif.setVisibility(View.GONE);
        }

        // Consultar el detalle exhaustivo en el servidor (GET /notificaciones/{id})
        if (notificacion.getId() != null) {
            consultarDetalleServidor(notificacion.getId());
        }
    }

    private void consultarDetalleServidor(long id) {
        if (binding == null) return;
        binding.progressDetalleNotif.setVisibility(View.VISIBLE);

        viewModel.obtenerDetalleNotificacion(id, new NotificacionViewModel.OnDetalleNotificacionCallback() {
            @Override
            public void onExito(@NonNull NotificacionResponse detalleActualizado) {
                if (binding == null || !isAdded()) return;
                binding.progressDetalleNotif.setVisibility(View.GONE);
                notificacion = detalleActualizado;
                popularDatosNotificacion(detalleActualizado);
            }

            @Override
            public void onError(@NonNull String mensajeError) {
                if (binding == null || !isAdded()) return;
                binding.progressDetalleNotif.setVisibility(View.GONE);
                // Se conservan los datos previos ya mostrados de forma segura
            }
        });
    }

    private void popularDatosNotificacion(@NonNull NotificacionResponse item) {
        if (binding == null) return;
        Context ctx = requireContext();

        // 1. Título y fecha
        String titulo = item.getTitulo() != null && !item.getTitulo().trim().isEmpty()
                ? item.getTitulo().trim()
                : "Actualización de Alerta";
        binding.tvTituloDetalleNotif.setText(titulo);
        binding.tvFechaDetalleNotif.setText(TimeUtils.obtenerTiempoTranscurrido(item.getFechaCreacion()));

        // 2. Mensaje descriptivo
        binding.tvMensajeDetalleNotif.setText(item.getMensaje() != null ? item.getMensaje().trim() : "");

        // 3. Estado resultante
        configurarBadgeEstado(ctx, item.getNuevoEstado());

        // 4. Responsable de la acción
        String nombreResp = item.getResponsableNombre();
        String rolResp = mapearRolAmigable(item.getResponsableRol());
        if (nombreResp != null && !nombreResp.trim().isEmpty()) {
            binding.layoutResponsableDetalle.setVisibility(View.VISIBLE);
            String texto = rolResp != null && !rolResp.isEmpty()
                    ? getString(R.string.notif_detalle_responsable_formato, nombreResp.trim(), rolResp)
                    : nombreResp.trim();
            binding.tvResponsableDetalle.setText(texto);
        } else {
            binding.layoutResponsableDetalle.setVisibility(View.GONE);
        }

        // 5. Motivo / observación explicativa
        String motivo = item.getMotivoAccion();
        if (motivo != null && !motivo.trim().isEmpty()) {
            binding.layoutMotivoDetalle.setVisibility(View.VISIBLE);
            binding.tvMotivoDetalle.setText(motivo.trim());
        } else {
            binding.layoutMotivoDetalle.setVisibility(View.GONE);
        }
    }

    private void configurarBadgeEstado(Context ctx, String estadoRaw) {
        String estado = estadoRaw != null ? estadoRaw.toUpperCase() : "";
        switch (estado) {
            case "DIRECCIONADA" -> {
                binding.tvEstadoBadgeDetalle.setText("DIRECCIONADA");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_verificada);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_verificada_text));
                binding.ivIconoDetalleNotif.setImageResource(R.drawable.ic_notifications);
                binding.ivIconoDetalleNotif.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.ra_status_verificada_text)));
            }
            case "EN_GESTION", "EN_ATENCION" -> {
                binding.tvEstadoBadgeDetalle.setText("EN GESTIÓN");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_atencion);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_atencion_text));
                binding.ivIconoDetalleNotif.setImageResource(R.drawable.ic_schedule);
                binding.ivIconoDetalleNotif.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.ra_status_atencion_text)));
            }
            case "RESUELTA" -> {
                binding.tvEstadoBadgeDetalle.setText("RESUELTA");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_resuelta);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_resuelta_text));
                binding.ivIconoDetalleNotif.setImageResource(R.drawable.ic_check_success);
                binding.ivIconoDetalleNotif.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.ra_status_resuelta_text)));
            }
            case "RECHAZADA" -> {
                binding.tvEstadoBadgeDetalle.setText("RECHAZADA");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_pendiente);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(ctx, R.color.ra_red_dark));
                binding.ivIconoDetalleNotif.setImageResource(R.drawable.ic_warning);
                binding.ivIconoDetalleNotif.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.ra_red)));
            }
            case "PENDIENTE_REVISION" -> {
                binding.tvEstadoBadgeDetalle.setText("EN REVISIÓN");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_revision);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(ctx, R.color.ra_status_revision_text));
                binding.ivIconoDetalleNotif.setImageResource(R.drawable.ic_notifications);
                binding.ivIconoDetalleNotif.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.ra_status_revision_text)));
            }
            default -> {
                binding.tvEstadoBadgeDetalle.setVisibility(View.GONE);
                binding.ivIconoDetalleNotif.setImageResource(R.drawable.ic_notifications);
                binding.ivIconoDetalleNotif.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.ra_red)));
            }
        }
    }

    private String mapearRolAmigable(String rolRaw) {
        if (rolRaw == null) return null;
        return switch (rolRaw.toUpperCase()) {
            case "ROLE_LIDER", "LIDER" -> "Líder Comunitario";
            case "ROLE_ORGANIZACION", "ORGANIZACION" -> "Entidad / Comité Asignado";
            case "ROLE_ADMIN", "ADMIN", "ADMINISTRADOR" -> "Administrador";
            case "ROLE_CIUDADANO", "CIUDADANO" -> "Ciudadano";
            default -> rolRaw;
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
