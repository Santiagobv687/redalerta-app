package com.redalerta.client.ui.alert;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentMiPerfilBinding;
import com.redalerta.client.model.usuario.DocumentoRequest;
import com.redalerta.client.model.usuario.UbicacionRequest;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;
import com.redalerta.client.ui.auth.LoginActivity;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.viewmodel.PerfilViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MiPerfilFragment extends Fragment {

    private FragmentMiPerfilBinding binding;
    private PerfilViewModel viewModel;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMiPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(PerfilViewModel.class);

        setupClickListeners();
        observeViewModel();

        viewModel.cargarPerfil();
    }

    private void setupClickListeners() {
        // Opción 1: Editar datos personales
        binding.btnOpcionEditarPerfil.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new EditarPerfilFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Opción 2: Cambiar contraseña
        binding.btnOpcionCambiarPassword.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new CambiarPasswordFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Opción 3: Cerrar sesión
        binding.btnOpcionCerrarSesion.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Estás seguro de que deseas salir de tu cuenta RedAlerta en este dispositivo?")
                    .setPositiveButton("Cerrar sesión", (dialog, which) -> {
                        // Notificar al backend POST /api/v1/auth/logout
                        ApiClient.getApiService(requireContext()).logout().enqueue(new Callback<ApiResponse<Void>>() {
                            @Override
                            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {}
                            @Override
                            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {}
                        });

                        sessionManager.cerrarSesion();
                        Intent intent = new Intent(requireActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void observeViewModel() {
        viewModel.getUsuario().observe(getViewLifecycleOwner(), u -> {
            if (u != null) {
                // Nombre y Email
                String nombre = u.getNombre() != null ? u.getNombre() : "Usuario Ciudadano";
                binding.tvNombreCompleto.setText(nombre);
                binding.tvAvatarInitials.setText(obtenerIniciales(nombre));

                String email = u.getCorreo() != null ? u.getCorreo() : "usuario@redalerta.com";
                binding.tvEmailContacto.setText(email);

                String tel = u.getTelefono() != null && !u.getTelefono().isEmpty() ? u.getTelefono() : "Sin teléfono registrado";
                binding.tvTelefonoContacto.setText("Teléfono: " + tel);

                // Documento de Identidad (Solo lectura)
                DocumentoRequest doc = u.getDocumento();
                if (doc != null && doc.getNumero() != null) {
                    String tipo = doc.getTipo() != null ? doc.getTipo() : "C.C.";
                    binding.tvDocumentoIdentidad.setText(tipo + " " + doc.getNumero());
                } else {
                    binding.tvDocumentoIdentidad.setText("Documento Registrado en Sistema");
                }

                // Ubicación Registrada (Solo lectura)
                UbicacionRequest ub = u.getUbicacion();
                if (ub != null) {
                    String depto = ub.getDepartamento() != null ? ub.getDepartamento() : "Quindío";
                    String mun = ub.getMunicipio() != null ? ub.getMunicipio() : "Armenia";
                    String dir = ub.getDireccionResidencia() != null ? ub.getDireccionResidencia() : "";
                    binding.tvUbicacionRegistrada.setText(!dir.isEmpty() ? depto + ", " + mun + " • " + dir : depto + ", " + mun);
                } else {
                    binding.tvUbicacionRegistrada.setText("Quindío, Armenia");
                }
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String obtenerIniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "U";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length >= 2) {
            return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
        }
        return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
