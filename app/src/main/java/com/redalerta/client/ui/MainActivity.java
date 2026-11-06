package com.redalerta.client.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.redalerta.client.R;
import com.redalerta.client.databinding.ActivityMainBinding;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.ui.admin.BandejaLiderFragment;
import com.redalerta.client.ui.alert.InicioFragment;
import com.redalerta.client.ui.alert.MiPerfilFragment;
import com.redalerta.client.ui.alert.MisPublicacionesFragment;
import com.redalerta.client.ui.auth.LoginActivity;
import com.redalerta.client.ui.org.BandejaOrganizacionFragment;
import com.redalerta.client.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);

        // ══════════════════════════════════════════════════
        // Configurar Toolbar Superior
        // ══════════════════════════════════════════════════
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // ══════════════════════════════════════════════════
        // Evaluación Reactiva del Rol de Usuario
        // ══════════════════════════════════════════════════
        String rol = session.getRol();
        boolean esOrganizacion = "ROLE_ORGANIZACION".equalsIgnoreCase(rol) || "ORGANIZACION".equalsIgnoreCase(rol);
        boolean esLider = "ROLE_LIDER".equalsIgnoreCase(rol) || "LIDER".equalsIgnoreCase(rol) || "ROLE_ADMIN".equalsIgnoreCase(rol) || "ADMINISTRADOR".equalsIgnoreCase(rol);

        // Inflar el menú inferior diferenciado según el rol
        binding.bottomNavigation.getMenu().clear();
        if (esOrganizacion) {
            binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_organizacion);
        } else if (esLider) {
            binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_lider);
        } else {
            binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_ciudadano);
        }

        // ══════════════════════════════════════════════════
        // Escuchador de Selección de la Barra Inferior (M3)
        // ══════════════════════════════════════════════════
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                mostrarFragmentInternal(new InicioFragment());
                return true;
            } else if (id == R.id.nav_mis_alertas) {
                mostrarFragmentInternal(new MisPublicacionesFragment());
                return true;
            } else if (id == R.id.nav_gestion_lider) {
                mostrarFragmentInternal(new BandejaLiderFragment());
                return true;
            } else if (id == R.id.nav_bandeja_org) {
                mostrarFragmentInternal(new BandejaOrganizacionFragment());
                return true;
            } else if (id == R.id.nav_perfil) {
                mostrarFragmentInternal(new MiPerfilFragment());
                return true;
            }
            return false;
        });

        // ══════════════════════════════════════════════════
        // Carga del Fragmento Inicial Reactivo al Rol
        // ══════════════════════════════════════════════════
        if (savedInstanceState == null) {
            if (esOrganizacion) {
                mostrarFragmentInternal(new BandejaOrganizacionFragment());
                binding.bottomNavigation.setSelectedItemId(R.id.nav_bandeja_org);
            } else {
                mostrarFragmentInternal(new InicioFragment());
                binding.bottomNavigation.setSelectedItemId(R.id.nav_inicio);
            }
        }

        // ══════════════════════════════════════════════════
        // Botón Superior "Cerrar sesión" con confirmación
        // ══════════════════════════════════════════════════
        binding.btnSalir.setOnClickListener(v -> confirmarYCerrarSesion());

        // ══════════════════════════════════════════════════
        // Manejo del botón "atrás" físico
        // ══════════════════════════════════════════════════
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                            getSupportFragmentManager().popBackStack();
                        } else {
                            finish();
                        }
                    }
                });
    }

    private void confirmarYCerrarSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas salir de tu cuenta RedAlerta en este dispositivo?")
                .setPositiveButton("Cerrar sesión", (dialog, which) -> ejecutarLogoutBackendYSalir())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarLogoutBackendYSalir() {
        // Notificar al backend POST /api/v1/auth/logout
        ApiClient.getApiService(this).logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {}
            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {}
        });

        // Limpiar sesión local y redirigir a Login
        session.cerrarSesion();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void mostrarFragmentInternal(Fragment fragment) {
        getSupportFragmentManager().popBackStack(
                null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
