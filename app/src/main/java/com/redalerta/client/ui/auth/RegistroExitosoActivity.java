package com.redalerta.client.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.redalerta.client.databinding.ActivityRegistroExitosoBinding;

public class RegistroExitosoActivity extends AppCompatActivity {

    public static final String EXTRA_NOMBRE   = "extra_nombre";
    public static final String EXTRA_APELLIDO = "extra_apellido";
    public static final String EXTRA_CORREO   = "extra_correo";

    private ActivityRegistroExitosoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroExitosoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Leer datos del Intent
        String nombre   = getIntent().getStringExtra(EXTRA_NOMBRE);
        String apellido = getIntent().getStringExtra(EXTRA_APELLIDO);
        String correo   = getIntent().getStringExtra(EXTRA_CORREO);

        // Mostrar iniciales + correo (ej: "S S — s@g.c")
        String iniciales = construirIniciales(nombre, apellido);
        String resumen = iniciales + " — " + (correo != null ? correo : "");
        binding.tvResumenUsuario.setText(resumen);

        // Botón: volver al login
        binding.btnIrLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Bloquear botón "atrás" para que no vuelva al formulario
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        binding.btnIrLogin.performClick();
                    }
                });
    }

    private String construirIniciales(String nombre, String apellido) {
        String n = (nombre != null && !nombre.isEmpty())
                ? String.valueOf(nombre.charAt(0)).toUpperCase() : "";
        String a = (apellido != null && !apellido.isEmpty())
                ? String.valueOf(apellido.charAt(0)).toUpperCase() : "";
        return (n + " " + a).trim();
    }
}