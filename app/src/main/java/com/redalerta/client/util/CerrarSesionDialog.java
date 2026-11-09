package com.redalerta.client.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.redalerta.client.databinding.DialogCerrarSesionBinding;

/**
 * Diálogo de confirmación de cierre de sesión con el estilo visual de RedAlerta.
 * Reutilizado desde la barra superior (MainActivity) y desde "Mi perfil".
 */
public final class CerrarSesionDialog {

    private CerrarSesionDialog() {}

    public static void mostrar(@NonNull Context context, @NonNull Runnable onConfirmar) {
        DialogCerrarSesionBinding b = DialogCerrarSesionBinding.inflate(LayoutInflater.from(context));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(b.getRoot())
                // El fondo y las esquinas los aporta la MaterialCardView del layout
                .setBackground(new ColorDrawable(Color.TRANSPARENT))
                .create();

        b.btnCancelarCerrarSesion.setOnClickListener(v -> dialog.dismiss());
        b.btnConfirmarCerrarSesion.setOnClickListener(v -> {
            // Evitar doble toque mientras se procesa el cierre
            b.btnConfirmarCerrarSesion.setEnabled(false);
            b.btnCancelarCerrarSesion.setEnabled(false);
            dialog.dismiss();
            onConfirmar.run();
        });

        dialog.show();
    }
}
