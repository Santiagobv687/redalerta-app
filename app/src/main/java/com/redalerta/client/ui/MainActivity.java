package com.redalerta.client.ui;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.badge.BadgeDrawable;
import com.redalerta.client.R;
import com.redalerta.client.databinding.ActivityMainBinding;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.ui.admin.BandejaLiderFragment;
import com.redalerta.client.ui.alert.InicioFragment;
import com.redalerta.client.ui.alert.MiPerfilFragment;
import com.redalerta.client.ui.alert.MisPublicacionesFragment;
import com.redalerta.client.ui.auth.LoginActivity;
import com.redalerta.client.ui.notificacion.NotificacionesFragment;
import com.redalerta.client.ui.org.BandejaOrganizacionFragment;
import com.redalerta.client.util.CerrarSesionDialog;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.viewmodel.NotificacionViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager session;
    private NotificacionViewModel notificacionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        notificacionViewModel = new ViewModelProvider(this).get(NotificacionViewModel.class);

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
        // Indicador de Notificaciones No Leídas
        // ══════════════════════════════════════════════════
        observarConteoNotificacionesNoLeidas();

        // ══════════════════════════════════════════════════
        // Escuchador de Selección de la Barra Inferior
        // ══════════════════════════════════════════════════
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            // Refrescar de forma discreta el conteo al cambiar de pestaña
            notificacionViewModel.cargarConteoNoLeidas();

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
            } else if (id == R.id.nav_notificaciones) {
                mostrarFragmentInternal(new NotificacionesFragment());
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
        // Manejo del botón "atrás" físico y visibilidad del BottomNav
        // ══════════════════════════════════════════════════
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean esPantallaRaiz = getSupportFragmentManager().getBackStackEntryCount() == 0;
            setBottomNavigationVisible(esPantallaRaiz);
            if (esPantallaRaiz) {
                notificacionViewModel.cargarConteoNoLeidas();
            }
        });

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

    @Override
    protected void onResume() {
        super.onResume();
        if (notificacionViewModel != null) {
            notificacionViewModel.cargarConteoNoLeidas();
        }
    }

    private void observarConteoNotificacionesNoLeidas() {
        notificacionViewModel.getConteoNoLeidas().observe(this, conteo -> {
            if (binding == null) return;
            long total = conteo != null ? conteo : 0L;
            if (total <= 0) {
                binding.bottomNavigation.removeBadge(R.id.nav_notificaciones);
            } else {
                BadgeDrawable badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_notificaciones);
                badge.setVisible(true);
                badge.setMaxCharacterCount(3); // Muestra "99+" si supera 99
                badge.setNumber((int) Math.min(total, Integer.MAX_VALUE));
                badge.setBackgroundColor(ContextCompat.getColor(this, R.color.ra_red));
                badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.ra_white));
                badge.setHorizontalOffset(4);
                badge.setVerticalOffset(4);
            }
        });
    }

    public void setBottomNavigationVisible(boolean visible) {
        if (binding != null && binding.bottomNavigation != null) {
            binding.bottomNavigation.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    public void cerrarSesionConConfirmacion() {
        CerrarSesionDialog.mostrar(this, this::ejecutarLogoutBackendYSalir);
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
