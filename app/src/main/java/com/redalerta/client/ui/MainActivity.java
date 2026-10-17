package com.redalerta.client.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Configurar el Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.app_name, R.string.app_name);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navigationView.setNavigationItemSelectedListener(this);

        // Por defecto, mostrar Registro de Alertas
        if (savedInstanceState == null) {
            mostrarFragment(new RegistroAlertaFragment());
            binding.navigationView.setCheckedItem(R.id.nav_registro_alerta);
        }

        // Botón Salir
        binding.btnSalir.setOnClickListener(v -> {
            session.cerrarSesion();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

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

    private void mostrarFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}