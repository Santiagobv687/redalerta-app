package com.redalerta.client.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import android.content.res.ColorStateList;
import android.text.InputFilter;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.redalerta.client.R;
import com.redalerta.client.databinding.DialogCancelarDireccionamientoBinding;
import com.redalerta.client.databinding.DialogEliminarAlertaBinding;
import com.redalerta.client.databinding.DialogGestionOrganizacionBinding;
import com.redalerta.client.databinding.DialogTerminosCondicionesBinding;

/**
 * Utilitario centralizado de diálogos institucionales y avisos flotantes estilizados
 * conforme al sistema de diseño de RedAlerta (evitando componentes crudos por defecto de Android).
 */
public final class AlertaMensajesHelper {

    public interface OnMotivoConfirmadoListener {
        void onConfirmar(@NonNull String motivo);
    }

    private AlertaMensajesHelper() {}

    // ══════════════════════════════════════════════════════════════════
    // 1. AVISOS FLOTANTES ESTILIZADOS (Reemplazo de Toast nativo)
    // ══════════════════════════════════════════════════════════════════

    public static void mostrarExito(@Nullable View rootView, @NonNull String mensaje) {
        mostrarBanner(rootView, mensaje, R.color.ra_success_card_text, R.color.ra_white, Snackbar.LENGTH_SHORT);
    }

    public static void mostrarInfo(@Nullable View rootView, @NonNull String mensaje) {
        mostrarBanner(rootView, mensaje, R.color.ra_topbar, R.color.ra_white, Snackbar.LENGTH_SHORT);
    }

    public static void mostrarError(@Nullable View rootView, @NonNull String mensaje) {
        mostrarBanner(rootView, mensaje, R.color.ra_red_dark, R.color.ra_white, Snackbar.LENGTH_LONG);
    }

    public static void mostrarExito(@NonNull Activity activity, @NonNull String mensaje) {
        View root = activity.findViewById(android.R.id.content);
        mostrarExito(root, mensaje);
    }

    public static void mostrarInfo(@NonNull Activity activity, @NonNull String mensaje) {
        View root = activity.findViewById(android.R.id.content);
        mostrarInfo(root, mensaje);
    }

    public static void mostrarError(@NonNull Activity activity, @NonNull String mensaje) {
        View root = activity.findViewById(android.R.id.content);
        mostrarError(root, mensaje);
    }

    private static void mostrarBanner(@Nullable View rootView,
                                      @NonNull String mensaje,
                                      int bgColorRes,
                                      int textColorRes,
                                      int duration) {
        if (rootView == null || rootView.getContext() == null) return;
        Context ctx = rootView.getContext();

        Snackbar snackbar = Snackbar.make(rootView, mensaje, duration);
        View sbView = snackbar.getView();

        float density = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(14f * density);
        shape.setColor(ContextCompat.getColor(ctx, bgColorRes));
        sbView.setBackground(shape);

        TextView tv = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setTextColor(ContextCompat.getColor(ctx, textColorRes));
            tv.setMaxLines(4);
            tv.setTextSize(14f);
        }

        snackbar.show();
    }

    // ══════════════════════════════════════════════════════════════════
    // 2. DIÁLOGO DE ELIMINACIÓN DE ALERTA
    // ══════════════════════════════════════════════════════════════════

    public static void mostrarDialogoEliminarAlerta(@NonNull Context context,
                                                    @NonNull String codigoAlerta,
                                                    @NonNull Runnable onConfirmar) {
        DialogEliminarAlertaBinding b = DialogEliminarAlertaBinding.inflate(LayoutInflater.from(context));
        b.tvMensajeEliminarAlerta.setText(
                context.getString(R.string.eliminar_alerta_dialog_mensaje, codigoAlerta)
        );

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(b.getRoot())
                .setBackground(new ColorDrawable(Color.TRANSPARENT))
                .create();

        b.btnCancelarEliminarAlerta.setOnClickListener(v -> dialog.dismiss());
        b.btnConfirmarEliminarAlerta.setOnClickListener(v -> {
            b.btnConfirmarEliminarAlerta.setEnabled(false);
            b.btnCancelarEliminarAlerta.setEnabled(false);
            dialog.dismiss();
            onConfirmar.run();
        });

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════════
    // 3. DIÁLOGO DE CANCELACIÓN DE DIRECCIONAMIENTO (CON MOTIVO)
    // ══════════════════════════════════════════════════════════════════

    public static void mostrarDialogoCancelarDireccionamiento(@NonNull Context context,
                                                              @NonNull OnMotivoConfirmadoListener listener) {
        DialogCancelarDireccionamientoBinding b = DialogCancelarDireccionamientoBinding.inflate(LayoutInflater.from(context));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(b.getRoot())
                .setBackground(new ColorDrawable(Color.TRANSPARENT))
                .create();

        b.etMotivoCancelacion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilMotivoCancelacion.setError(null);
                b.tilMotivoCancelacion.setErrorEnabled(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.btnVolverCancelacionDir.setOnClickListener(v -> dialog.dismiss());
        b.btnConfirmarCancelacionDir.setOnClickListener(v -> {
            String motivo = b.etMotivoCancelacion.getText() != null
                    ? b.etMotivoCancelacion.getText().toString().trim()
                    : "";
            if (motivo.length() < 5) {
                b.tilMotivoCancelacion.setError(context.getString(R.string.cancelar_dir_dialog_error_motivo));
                return;
            }
            b.btnConfirmarCancelacionDir.setEnabled(false);
            b.btnVolverCancelacionDir.setEnabled(false);
            dialog.dismiss();
            listener.onConfirmar(motivo);
        });

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════════
    // 4. DIÁLOGO DE TÉRMINOS Y CONDICIONES
    // ══════════════════════════════════════════════════════════════════

    public static void mostrarDialogoTerminos(@NonNull Context context,
                                              @Nullable Runnable onAceptar) {
        DialogTerminosCondicionesBinding b = DialogTerminosCondicionesBinding.inflate(LayoutInflater.from(context));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(b.getRoot())
                .setBackground(new ColorDrawable(Color.TRANSPARENT))
                .create();

        b.btnCerrarTerminosDialog.setOnClickListener(v -> dialog.dismiss());
        b.btnAceptarTerminosDialog.setOnClickListener(v -> {
            dialog.dismiss();
            if (onAceptar != null) {
                onAceptar.run();
            }
        });

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════════
    // 5. DIÁLOGOS DE GESTIÓN Y ATENCIÓN COMUNITARIA (ROLE_ORGANIZACION)
    // ══════════════════════════════════════════════════════════════════

    /**
     * 1. Diálogo "Iniciar Gestión" (DIRECCIONADA -> EN_GESTION):
     * - Título: "Iniciar Atención Comunitaria"
     * - Campo de texto (TextInputLayout): "Observación inicial o notas de desplazamiento"
     * - Validación: obligatorio, no vacío, máx 500 caracteres.
     */
    public static void mostrarDialogoIniciarGestion(@NonNull Context context,
                                                    @NonNull OnMotivoConfirmadoListener listener) {
        mostrarDialogoGestionOrganizacion(
                context,
                "Iniciar Atención Comunitaria",
                "Registre la observación inicial o notas de desplazamiento de la brigada (ej. \"Brigada en camino al sector\").",
                "Observación inicial o notas de desplazamiento",
                "Iniciar Gestión",
                R.drawable.ic_schedule,
                R.color.ra_red,
                1,
                500,
                3,
                "Ingrese una observación inicial para iniciar la atención",
                listener
        );
    }

    /**
     * 2. Diálogo "Resolver Alerta" (EN_GESTION -> RESUELTA):
     * - Título: "Informe de Resolución de Alerta"
     * - Campo de texto multilínea (TextInputLayout): "Informe final de actividades y solución aplicada"
     * - Validación: obligatorio, mín 20 y máx 1500 caracteres.
     */
    public static void mostrarDialogoResolverAlerta(@NonNull Context context,
                                                    @NonNull OnMotivoConfirmadoListener listener) {
        mostrarDialogoGestionOrganizacion(
                context,
                "Informe de Resolución de Alerta",
                "Describa detalladamente las actividades ejecutadas en terreno y la solución aplicada (mínimo 20 caracteres).",
                "Informe final de actividades y solución aplicada",
                "Resolver y Cerrar Alerta",
                R.drawable.ic_check_success,
                R.color.ra_success_card_text,
                20,
                1500,
                4,
                "El informe de resolución debe tener al menos 20 caracteres",
                listener
        );
    }

    /**
     * 3. Diálogo "Rechazar Alerta" (Pasa a RECHAZADA):
     * - Título: "Rechazar o Descartar Alerta"
     * - Campo de texto (TextInputLayout): "Motivo del rechazo"
     * - Validación: obligatorio, mín 10 y máx 500 caracteres.
     */
    public static void mostrarDialogoRechazarAlerta(@NonNull Context context,
                                                    @NonNull OnMotivoConfirmadoListener listener) {
        mostrarDialogoGestionOrganizacion(
                context,
                "Rechazar o Descartar Alerta",
                "Indique el motivo justificado por el cual su entidad descarta o rechaza este reporte (mínimo 10 caracteres).",
                "Motivo del rechazo",
                "Confirmar Rechazo",
                R.drawable.ic_warning,
                R.color.ra_red_dark,
                10,
                500,
                3,
                "El motivo del rechazo debe tener al menos 10 caracteres",
                listener
        );
    }

    private static void mostrarDialogoGestionOrganizacion(@NonNull Context context,
                                                          @NonNull String titulo,
                                                          @NonNull String subtitulo,
                                                          @NonNull String hintCampo,
                                                          @NonNull String textoBotonConfirmar,
                                                          int iconoRes,
                                                          int colorAcentoRes,
                                                          int minChars,
                                                          int maxChars,
                                                          int minLines,
                                                          @NonNull String mensajeErrorMin,
                                                          @NonNull OnMotivoConfirmadoListener listener) {
        DialogGestionOrganizacionBinding b = DialogGestionOrganizacionBinding.inflate(LayoutInflater.from(context));

        int colorAcento = ContextCompat.getColor(context, colorAcentoRes);
        b.ivIconoDialogOrg.setImageResource(iconoRes);
        b.ivIconoDialogOrg.setImageTintList(ColorStateList.valueOf(colorAcento));

        b.tvTituloDialogOrg.setText(titulo);
        b.tvSubtituloDialogOrg.setText(subtitulo);

        b.tilCampoDialogOrg.setHint(hintCampo);
        b.tilCampoDialogOrg.setCounterMaxLength(maxChars);
        b.etCampoDialogOrg.setMinLines(minLines);
        b.etCampoDialogOrg.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxChars)});

        b.btnConfirmarDialogOrg.setText(textoBotonConfirmar);
        b.btnConfirmarDialogOrg.setBackgroundTintList(ColorStateList.valueOf(colorAcento));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(b.getRoot())
                .setBackground(new ColorDrawable(Color.TRANSPARENT))
                .create();

        b.etCampoDialogOrg.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilCampoDialogOrg.setError(null);
                b.tilCampoDialogOrg.setErrorEnabled(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.btnCancelarDialogOrg.setOnClickListener(v -> dialog.dismiss());
        b.btnConfirmarDialogOrg.setOnClickListener(v -> {
            String valor = b.etCampoDialogOrg.getText() != null
                    ? b.etCampoDialogOrg.getText().toString().trim()
                    : "";
            if (valor.isEmpty() || valor.length() < minChars) {
                b.tilCampoDialogOrg.setError(mensajeErrorMin);
                return;
            }
            if (valor.length() > maxChars) {
                b.tilCampoDialogOrg.setError("El texto no puede superar los " + maxChars + " caracteres");
                return;
            }
            b.btnConfirmarDialogOrg.setEnabled(false);
            b.btnCancelarDialogOrg.setEnabled(false);
            dialog.dismiss();
            listener.onConfirmar(valor);
        });

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════════
    // 5. VISOR DE EVIDENCIA FOTOGRÁFICA AMPLIADA (Pantalla Completa)
    // ══════════════════════════════════════════════════════════════════

    public static void mostrarVisorImagen(@Nullable Context context, @Nullable Object fuenteImagen) {
        if (context == null || fuenteImagen == null) return;

        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_visualizador_imagen, null);
        dialog.setContentView(dialogView);

        ImageView imgGrande = dialogView.findViewById(R.id.imgFotoGrande);
        View pbCarga = dialogView.findViewById(R.id.pbCargaImagen);
        View btnCerrar = dialogView.findViewById(R.id.btnCerrarVisor);
        View container = dialogView.findViewById(R.id.containerVisorImagen);

        if (pbCarga != null) pbCarga.setVisibility(View.VISIBLE);

        com.bumptech.glide.Glide.with(context)
                .load(fuenteImagen)
                .fitCenter()
                .placeholder(R.drawable.ic_photo_library)
                .error(R.drawable.ic_photo_library)
                .listener(new com.bumptech.glide.request.RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                                Object model,
                                                com.bumptech.glide.request.target.Target<Drawable> target,
                                                boolean isFirstResource) {
                        if (pbCarga != null) pbCarga.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource,
                                                  Object model,
                                                  com.bumptech.glide.request.target.Target<Drawable> target,
                                                  com.bumptech.glide.load.DataSource dataSource,
                                                  boolean isFirstResource) {
                        if (pbCarga != null) pbCarga.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(imgGrande);

        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> dialog.dismiss());
        }

        if (container != null) {
            container.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
