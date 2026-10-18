package com.redalerta.client.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.redalerta.client.databinding.ActivityLoginBinding;
import com.redalerta.client.ui.MainActivity;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.util.Validaciones;
import com.redalerta.client.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);

        // Si ya hay sesión activa, ir directo al home
        if (session.estaLogueado()) {
            irAHomeSegunRol(session.getRol());
            return;
        }

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        observarViewModel();

        // ══════════════════════════════════════════════════
        // Desmantelamiento dinámico de errores en TextInputLayout
        // ══════════════════════════════════════════════════
        configurarLimpiezaErrores();

        binding.btnLogin.setOnClickListener(v -> intentarLogin());

        binding.btnIrRegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));
    }

    private void configurarLimpiezaErrores() {
        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilCorreo.setError(null);
                binding.tilContrasena.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etCorreo.addTextChangedListener(tw);
        binding.etContrasena.addTextChangedListener(tw);
    }

    private void intentarLogin() {
        binding.tilCorreo.setError(null);
        binding.tilContrasena.setError(null);

        String correo = binding.etCorreo.getText().toString().trim();
        String contrasena = binding.etContrasena.getText().toString();

        if (!Validaciones.noEstaVacio(correo)) {
            binding.tilCorreo.setError("Ingresa tu correo electrónico");
            return;
        }
        if (!Validaciones.esCorreoValido(correo)) {
            binding.tilCorreo.setError("Correo electrónico inválido");
            return;
        }
        if (!Validaciones.noEstaVacio(contrasena)) {
            binding.tilContrasena.setError("Ingresa tu contraseña");
            return;
        }
        viewModel.login(correo, contrasena);
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(this, cargando -> {
            binding.progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!cargando);
        });

        viewModel.getError().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                // Marca ambos campos en rojo por seguridad, pero muestra el texto una sola vez debajo de la contraseña
                binding.tilCorreo.setError(" ");
                binding.tilContrasena.setError(msg);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getLoginExitoso().observe(this, resp -> {
            if (resp != null && resp.getToken() != null) {
                session.guardarSesion(
                        resp.getToken(),
                        resp.getUsuario().getId(),
                        resp.getUsuario().getRol(),
                        resp.getUsuario().getNombre()
                );
                irAHomeSegunRol(resp.getUsuario().getRol());
            }
        });
    }

    private void irAHomeSegunRol(String rol) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
