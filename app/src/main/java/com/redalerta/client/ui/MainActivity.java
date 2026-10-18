package com.redalerta.client.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;
import com.redalerta.client.R;
import com.redalerta.client.databinding.ActivityMainBinding;
import com.redalerta.client.ui.admin.GestionSeguimientoFragment;
import com.redalerta.client.ui.admin.VerificacionFragment;
import com.redalerta.client.ui.alert.InicioFragment;
import com.redalerta.client.ui.alert.MiPerfilFragment;
import com.redalerta.client.ui.alert.MisPublicacionesFragment;
import com.redalerta.client.ui.alert.RegistroAlertaFragment;
import com.redalerta.client.ui.auth.LoginActivity;
import com.redalerta.client.util.SessionManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);

        // ══════════════════════════════════════════════════
        // Configurar Toolbar
        // ══════════════════════════════════════════════════
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // ══════════════════════════════════════════════════
        // Configurar Drawer
        // ══════════════════════════════════════════════════
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.app_name, R.string.app_name);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navigationView.setNavigationItemSelectedListener(this);

        // ══════════════════════════════════════════════════
        // Control de visibilidad del menú según el ROL
        // ══════════════════════════════════════════════════
        configurarMenuSegunRol();

        // ══════════════════════════════════════════════════
        // Configurar la Barra de Navegación Inferior Única (M3)
        // ══════════════════════════════════════════════════
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                mostrarFragmentInternal(new InicioFragment());
                binding.navigationView.setCheckedItem(R.id.nav_inicio);
                return true;
            } else if (id == R.id.nav_mis_alertas) {
                mostrarFragmentInternal(new MisPublicacionesFragment());
                binding.navigationView.setCheckedItem(R.id.nav_mis_alertas);
                return true;
            } else if (id == R.id.nav_perfil) {
                mostrarFragmentInternal(new MiPerfilFragment());
                binding.navigationView.setCheckedItem(R.id.nav_mi_perfil);
                return true;
            }
            return false;
        });

        // ══════════════════════════════════════════════════
        // Fragmento por defecto (Feed Principal de Inicio)
        // ══════════════════════════════════════════════════
        if (savedInstanceState == null) {
            mostrarFragment(new InicioFragment());
            binding.navigationView.setCheckedItem(R.id.nav_inicio);
        }

        // ══════════════════════════════════════════════════
        // Botón Salir
        // ══════════════════════════════════════════════════
        binding.btnSalir.setOnClickListener(v -> {
            session.cerrarSesion();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // ══════════════════════════════════════════════════
        // Manejo del botón "atrás" físico
        // ══════════════════════════════════════════════════
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            binding.drawerLayout.closeDrawer(GravityCompat.START);
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    // ══════════════════════════════════════════════════════
    // Mostrar/ocultar opciones del menú según el rol
    // ══════════════════════════════════════════════════════
    private void configurarMenuSegunRol() {
        String rol = session.getRol();
        boolean esAdmin = "ADMINISTRADOR".equalsIgnoreCase(rol);

        // Habilitar/deshabilitar las opciones de admin
        MenuItem itemVerificacion = binding.navigationView.getMenu().findItem(R.id.nav_verificacion);
        MenuItem itemGestion = binding.navigationView.getMenu().findItem(R.id.nav_gestion);

        if (itemVerificacion != null) itemVerificacion.setVisible(esAdmin);
        if (itemGestion != null) itemGestion.setVisible(esAdmin);
    }

    // ══════════════════════════════════════════════════════
    // Navegación del menú lateral
    // ══════════════════════════════════════════════════════
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // --- Opciones de Ciudadano ---
        if (id == R.id.nav_inicio) {
            mostrarFragment(new InicioFragment());
        } else if (id == R.id.nav_registro_alerta) {
            mostrarFragment(new RegistroAlertaFragment());
        } else if (id == R.id.nav_mi_perfil) {
            mostrarFragment(new MiPerfilFragment());
        } else if (id == R.id.nav_mis_alertas) {
            mostrarFragment(new MisPublicacionesFragment());
        }

        // --- Opciones de Administrador ---
        else if (id == R.id.nav_verificacion) {
            mostrarFragment(new VerificacionFragment());
        } else if (id == R.id.nav_gestion) {
            mostrarFragment(new GestionSeguimientoFragment());
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ══════════════════════════════════════════════════════
    // Reemplazar fragmento actual
    // ══════════════════════════════════════════════════════
    private void mostrarFragment(Fragment fragment) {
        if (fragment instanceof InicioFragment) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_inicio);
        } else if (fragment instanceof MisPublicacionesFragment) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_mis_alertas);
        } else if (fragment instanceof MiPerfilFragment) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_perfil);
        } else {
            mostrarFragmentInternal(fragment);
        }
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