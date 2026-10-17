package com.redalerta.client.ui.auth;

import android.content.Intent;
import android.os.Bundle;
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
/*
        binding.btnLogin.setOnClickListener(v -> intentarLogin());
        binding.tvIrARegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));
        binding.tvRecuperar.setOnClickListener(v ->
                startActivity(new Intent(this, RecuperacionActivity.class)));

 */
    }

    private void intentarLogin() {
        String correo = binding.etCorreo.getText().toString().trim();
        String contrasena = binding.etContrasena.getText().toString();

        if (!Validaciones.esCorreoValido(correo)) {
            binding.etCorreo.setError("Correo inválido");
            return;
        }
        if (!Validaciones.noEstaVacio(contrasena)) {
            binding.etContrasena.setError("Ingresa tu contraseña");
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
            if (msg != null) {
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
    /*
    private void irAHomeSegunRol(String rol) {
        // Los tres roles móviles van a la lista de alertas.
        // La UI de la lista adapta acciones según el rol (lectura, verificación, atención).
        // startActivity(new Intent(this, ListaAlertasActivity.class));
        finish();
    }

     */
}