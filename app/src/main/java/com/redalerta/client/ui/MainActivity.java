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
        // Fragmento por defecto (Registro de Alertas)
        // ══════════════════════════════════════════════════
        if (savedInstanceState == null) {
            mostrarFragment(new RegistroAlertaFragment());
            binding.navigationView.setCheckedItem(R.id.nav_registro_alerta);
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
                            // Deshabilitar este callback y delegar al sistema
                            // (así se cierra la app o se navega al fragmento anterior si existe backstack)
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    // ══════════════════════════════════════════════════════
    // Navegación del menú lateral
    // ══════════════════════════════════════════════════════
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_registro_alerta) {
            mostrarFragment(new RegistroAlertaFragment());
        } else if (id == R.id.nav_inicio) {
            Toast.makeText(this, "Inicio (próximamente)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_mi_perfil) {
            Toast.makeText(this, "Mi perfil (próximamente)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_mis_publicaciones) {
            Toast.makeText(this, "Mis publicaciones (próximamente)", Toast.LENGTH_SHORT).show();
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ══════════════════════════════════════════════════════
    // Reemplazar fragmento actual
    // ══════════════════════════════════════════════════════
    private void mostrarFragment(Fragment fragment) {
        // Limpiar el backstack para que al navegar por el menú
        // no se acumulen fragmentos anteriores
        getSupportFragmentManager().popBackStack(
                null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}