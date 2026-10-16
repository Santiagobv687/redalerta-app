package com.redalerta.client.ui.auth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.redalerta.client.R;
import com.redalerta.client.databinding.ActivityRegistroBinding;

public class RegistroActivity extends AppCompatActivity {

    private ActivityRegistroBinding binding;
    private int pasoActual = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvVolverLogin.setOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            mostrarPaso(1);
        }
    }

    public void mostrarPaso(int paso) {
        pasoActual = paso;
        Fragment fragment;
        switch (paso) {
            case 2:  fragment = new RegistroPaso2Fragment();  break;
            case 3:  fragment = new RegistroPaso3Fragment();  break;
            default: fragment = new RegistroPaso1Fragment();  break;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        actualizarIndicador(paso);
    }

    private void actualizarIndicador(int paso) {
        // Paso 1
        aplicarEstadoCirculo(binding.stepCircle1, paso, 1);
        aplicarEstadoLinea(binding.stepLine1, paso, 1);
        // Paso 2
        aplicarEstadoCirculo(binding.stepCircle2, paso, 2);
        aplicarEstadoLinea(binding.stepLine2, paso, 2);
        // Paso 3
        aplicarEstadoCirculo(binding.stepCircle3, paso, 3);
    }

    private void aplicarEstadoCirculo(android.widget.TextView circle, int pasoActual, int numero) {
        if (pasoActual > numero) {
            // Completado
            circle.setBackgroundResource(R.drawable.bg_step_completed);
            circle.setText("✓");
            circle.setTextColor(getColor(R.color.ra_white));
        } else if (pasoActual == numero) {
            // Activo
            circle.setBackgroundResource(R.drawable.bg_step_active);
            circle.setText(String.valueOf(numero));
            circle.setTextColor(getColor(R.color.ra_white));
        } else {
            // Inactivo
            circle.setBackgroundResource(R.drawable.bg_step_inactive);
            circle.setText(String.valueOf(numero));
            circle.setTextColor(getColor(R.color.ra_step_inactive_text));
        }
    }

    private void aplicarEstadoLinea(android.view.View line, int pasoActual, int numero) {
        int color = (pasoActual > numero)
                ? getColor(R.color.ra_red)
                : getColor(R.color.ra_step_inactive_bg);
        line.setBackgroundColor(color);
    }

    private final com.redalerta.client.model.RegistroRequest registroRequest =
            new com.redalerta.client.model.RegistroRequest();

    public com.redalerta.client.model.RegistroRequest getRegistroRequest() {
        return registroRequest;
    }
}