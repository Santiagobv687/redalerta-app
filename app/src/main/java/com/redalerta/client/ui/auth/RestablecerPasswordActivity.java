package com.redalerta.client.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.redalerta.client.R;
import com.redalerta.client.databinding.ActivityRestablecerPasswordBinding;
import com.redalerta.client.viewmodel.AuthViewModel;

public class RestablecerPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_RESET_TOKEN = "extra_reset_token";
    public static final String EXTRA_CORREO      = "extra_correo_restablecer";

    private ActivityRestablecerPasswordBinding binding;
    private AuthViewModel viewModel;
    private String resetToken = "";
    private String correoUsuario = "";
    private boolean exitoMostrado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRestablecerPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        resetToken = getIntent().getStringExtra(EXTRA_RESET_TOKEN);
        if (resetToken == null) resetToken = "";

        correoUsuario = getIntent().getStringExtra(EXTRA_CORREO);
        if (correoUsuario == null) correoUsuario = "";

        // Configurar limpieza dinámica de errores al digitar
        limpiarErrorAlDigitar(binding.tilNuevaPassword, binding.etNuevaPassword);
        limpiarErrorAlDigitar(binding.tilConfirmarPassword, binding.etConfirmarPassword);

        // Botón superior para cancelar y volver al inicio de sesión
        binding.tvCancelarRestablecer.setOnClickListener(v -> irALogin());

        // Acción primaria: Actualizar contraseña
        binding.btnActualizarPassword.setOnClickListener(v -> intentarRestablecerPassword());

        // Botón "Iniciar sesión" en la pantalla de feedback de éxito
        binding.btnIrLoginExito.setOnClickListener(v -> irALogin());

        // Interceptar botón atrás cuando ya se mostró el éxito para llevar directo al login
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (exitoMostrado) {
                    irALogin();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        observarViewModel();
    }

    private void limpiarErrorAlDigitar(TextInputLayout til, EditText et) {
        if (til == null || et == null) return;
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                limpiarErrorCampo(til);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void limpiarErrorCampo(TextInputLayout til) {
        if (til != null) {
            til.setError(null);
            til.setErrorEnabled(false);
        }
    }

    private void intentarRestablecerPassword() {
        limpiarErrorCampo(binding.tilNuevaPassword);
        limpiarErrorCampo(binding.tilConfirmarPassword);

        String nuevaPass = binding.etNuevaPassword.getText() != null
                ? binding.etNuevaPassword.getText().toString()
                : "";
        String confirmarPass = binding.etConfirmarPassword.getText() != null
                ? binding.etConfirmarPassword.getText().toString()
                : "";

        if (nuevaPass.isEmpty()) {
            binding.tilNuevaPassword.setError(getString(R.string.rec_paso3_error_vacia));
            binding.etNuevaPassword.requestFocus();
            return;
        }

        if (nuevaPass.length() < 6) {
            binding.tilNuevaPassword.setError(getString(R.string.rec_paso3_error_corta));
            binding.etNuevaPassword.requestFocus();
            return;
        }

        if (confirmarPass.isEmpty()) {
            binding.tilConfirmarPassword.setError(getString(R.string.rec_paso3_error_confirmar_vacia));
            binding.etConfirmarPassword.requestFocus();
            return;
        }

        if (!nuevaPass.equals(confirmarPass)) {
            binding.tilConfirmarPassword.setError(getString(R.string.rec_paso3_error_no_coinciden));
            binding.etConfirmarPassword.requestFocus();
            return;
        }

        ocultarTeclado(binding.btnActualizarPassword);
        viewModel.restablecerPassword(resetToken, nuevaPass);
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(this, cargando -> {
            boolean enCarga = Boolean.TRUE.equals(cargando);
            binding.progressBar.setVisibility(enCarga ? View.VISIBLE : View.GONE);
            binding.btnActualizarPassword.setEnabled(!enCarga);
            binding.etNuevaPassword.setEnabled(!enCarga);
            binding.etConfirmarPassword.setEnabled(!enCarga);
            binding.tvCancelarRestablecer.setEnabled(!enCarga);
        });

        viewModel.getError().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                binding.tilNuevaPassword.setError(msg);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getPasswordRestablecida().observe(this, exito -> {
            if (Boolean.TRUE.equals(exito)) {
                mostrarPantallaExito();
            }
        });
    }

    private void mostrarPantallaExito() {
        exitoMostrado = true;
        binding.tvCancelarRestablecer.setVisibility(View.GONE);
        binding.layoutFormularioRestablecer.setVisibility(View.GONE);
        binding.layoutExitoRestablecer.setVisibility(View.VISIBLE);

        if (!correoUsuario.isEmpty()) {
            binding.tvCorreoActualizado.setText(correoUsuario);
            binding.tvCorreoActualizado.setVisibility(View.VISIBLE);
        } else {
            binding.tvCorreoActualizado.setVisibility(View.GONE);
        }
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        if (!correoUsuario.isEmpty()) {
            intent.putExtra(RecuperarPasswordActivity.EXTRA_CORREO_INICIAL, correoUsuario);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void ocultarTeclado(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
