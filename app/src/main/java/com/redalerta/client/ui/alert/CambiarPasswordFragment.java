package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.redalerta.client.databinding.FragmentCambiarPasswordBinding;
import com.redalerta.client.viewmodel.PerfilViewModel;

public class CambiarPasswordFragment extends Fragment {

    private FragmentCambiarPasswordBinding binding;
    private PerfilViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCambiarPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PerfilViewModel.class);
        viewModel.limpiarEstadoExito();

        setupToolbar();
        setupButtons();
        setupLiveValidation();
        observeViewModel();

        validarEstadoBotonGuardar();
    }

    private void setupToolbar() {
        binding.toolbarCambiarPassword.setNavigationOnClickListener(v -> cancelarYVolver());
    }

    private void setupButtons() {
        binding.btnCancelarPassword.setOnClickListener(v -> cancelarYVolver());
        binding.btnGuardarPassword.setOnClickListener(v -> procesarCambiarPassword());
        binding.btnGuardarPassword.setEnabled(false);
    }

    private void setupLiveValidation() {
        TextWatcher passwordWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                limpiarErrores();
                validarEstadoBotonGuardar();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etPasswordActual.addTextChangedListener(passwordWatcher);
        binding.etPasswordNuevo.addTextChangedListener(passwordWatcher);
        binding.etConfirmarPassword.addTextChangedListener(passwordWatcher);
    }

    private void validarEstadoBotonGuardar() {
        if (binding == null) return;

        String passActual = binding.etPasswordActual.getText() != null ? binding.etPasswordActual.getText().toString().trim() : "";
        String passNuevo = binding.etPasswordNuevo.getText() != null ? binding.etPasswordNuevo.getText().toString().trim() : "";
        String passConfirmar = binding.etConfirmarPassword.getText() != null ? binding.etConfirmarPassword.getText().toString().trim() : "";

        boolean esValido = !passActual.isEmpty() && passNuevo.length() >= 8 && passConfirmar.equals(passNuevo);
        binding.btnGuardarPassword.setEnabled(esValido);
    }

    private void procesarCambiarPassword() {
        limpiarErrores();

        String passActual = binding.etPasswordActual.getText() != null ? binding.etPasswordActual.getText().toString().trim() : "";
        String passNuevo = binding.etPasswordNuevo.getText() != null ? binding.etPasswordNuevo.getText().toString().trim() : "";
        String passConfirmar = binding.etConfirmarPassword.getText() != null ? binding.etConfirmarPassword.getText().toString().trim() : "";

        boolean esValido = true;

        if (passActual.isEmpty()) {
            binding.tilPasswordActual.setError("Ingresa tu contraseña actual");
            esValido = false;
        }

        if (passNuevo.isEmpty()) {
            binding.tilPasswordNuevo.setError("Ingresa la nueva contraseña");
            esValido = false;
        } else if (passNuevo.length() < 8) {
            binding.tilPasswordNuevo.setError("La nueva contraseña debe tener mínimo 8 caracteres");
            esValido = false;
        }

        if (passConfirmar.isEmpty()) {
            binding.tilConfirmarPassword.setError("Confirma la nueva contraseña");
            esValido = false;
        } else if (!passConfirmar.equals(passNuevo)) {
            binding.tilConfirmarPassword.setError("Las contraseñas no coinciden. Verifícalas.");
            esValido = false;
        }

        if (!esValido) return;

        // Deshabilitar botón y enviar a la API
        binding.btnGuardarPassword.setEnabled(false);
        viewModel.cambiarPassword(passActual, passNuevo);
    }

    private void limpiarErrores() {
        binding.tilPasswordActual.setError(null);
        binding.tilPasswordNuevo.setError(null);
        binding.tilConfirmarPassword.setError(null);
        binding.tvPasswordErrorBanner.setVisibility(View.GONE);
    }

    private void cancelarYVolver() {
        binding.etPasswordActual.setText("");
        binding.etPasswordNuevo.setText("");
        binding.etConfirmarPassword.setText("");
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void observeViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            if (cargando != null) {
                if (cargando) {
                    binding.btnGuardarPassword.setEnabled(false);
                } else {
                    validarEstadoBotonGuardar();
                }
            }
        });

        viewModel.getPasswordActualizadoExito().observe(getViewLifecycleOwner(), exito -> {
            if (exito != null && exito) {
                viewModel.limpiarEstadoExito();
                Toast.makeText(requireContext(), "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show();
                cancelarYVolver();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            validarEstadoBotonGuardar();
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                binding.tvPasswordErrorBanner.setVisibility(View.VISIBLE);
                binding.tvPasswordErrorBanner.setText(errorMsg);
                binding.tilPasswordActual.setError("Verifica los datos ingresados");
            } else {
                binding.tvPasswordErrorBanner.setVisibility(View.GONE);
                binding.tilPasswordActual.setError(null);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
