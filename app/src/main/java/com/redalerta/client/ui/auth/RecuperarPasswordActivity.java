package com.redalerta.client.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.redalerta.client.R;
import com.redalerta.client.databinding.ActivityRecuperarPasswordBinding;
import com.redalerta.client.util.Validaciones;
import com.redalerta.client.viewmodel.AuthViewModel;

public class RecuperarPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_CORREO_INICIAL = "extra_correo_inicial";

    private ActivityRecuperarPasswordBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecuperarPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Si el usuario ya había escrito su correo en el Login, pre-cargarlo
        String correoInicial = getIntent().getStringExtra(EXTRA_CORREO_INICIAL);
        if (correoInicial != null && !correoInicial.trim().isEmpty()) {
            binding.etCorreoRecuperacion.setText(correoInicial.trim());
        }

        // Botón superior para regresar al login
        binding.tvVolverLogin.setOnClickListener(v -> finish());

        // Limpieza dinámica de error al escribir
        binding.etCorreoRecuperacion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                limpiarErrorCampo(binding.tilCorreoRecuperacion);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Acción primaria: Solicitar código OTP de 6 dígitos
        binding.btnEnviarCodigo.setOnClickListener(v -> intentarSolicitarCodigo());

        observarViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cuando el usuario regresa desde "Editar correo", poner el foco al final del texto para facilitar la corrección
        if (binding.etCorreoRecuperacion.getText() != null && binding.etCorreoRecuperacion.getText().length() > 0) {
            binding.etCorreoRecuperacion.requestFocus();
            binding.etCorreoRecuperacion.setSelection(binding.etCorreoRecuperacion.getText().length());
        }
    }

    private void intentarSolicitarCodigo() {
        limpiarErrorCampo(binding.tilCorreoRecuperacion);

        String correo = binding.etCorreoRecuperacion.getText() != null
                ? binding.etCorreoRecuperacion.getText().toString().trim()
                : "";

        if (!Validaciones.noEstaVacio(correo)) {
            binding.tilCorreoRecuperacion.setError(getString(R.string.rec_error_correo_vacio));
            return;
        }
        if (!Validaciones.esCorreoValido(correo)) {
            binding.tilCorreoRecuperacion.setError(getString(R.string.rec_error_correo_invalido));
            return;
        }

        viewModel.solicitarRecuperacion(correo);
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(this, cargando -> {
            binding.progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
            binding.btnEnviarCodigo.setEnabled(!cargando);
            binding.etCorreoRecuperacion.setEnabled(!cargando);
        });

        viewModel.getError().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                binding.tilCorreoRecuperacion.setError(msg);
            }
        });

        viewModel.getRecuperacionSolicitada().observe(this, mensajeExito -> {
            if (mensajeExito != null && !mensajeExito.isEmpty()) {
                viewModel.limpiarEstadoRecuperacionSolicitada();
                String correo = binding.etCorreoRecuperacion.getText() != null
                        ? binding.etCorreoRecuperacion.getText().toString().trim()
                        : "";
                Intent intent = new Intent(this, VerificarCodigoActivity.class);
                intent.putExtra(VerificarCodigoActivity.EXTRA_CORREO, correo);
                startActivity(intent);
            }
        });
    }

    private void limpiarErrorCampo(TextInputLayout til) {
        if (til != null) {
            til.setError(null);
            til.setErrorEnabled(false);
        }
    }
}
