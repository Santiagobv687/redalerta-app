package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.redalerta.client.databinding.FragmentEditarPerfilBinding;
import com.redalerta.client.model.usuario.UbicacionRequest;
import com.redalerta.client.model.usuario.Usuario;
import com.redalerta.client.util.UbicacionCatalogoHelper;
import com.redalerta.client.viewmodel.PerfilViewModel;

public class EditarPerfilFragment extends Fragment {

    private FragmentEditarPerfilBinding binding;
    private PerfilViewModel viewModel;

    // Valores originales precargados para verificar cambios reales
    private String originalNombre = "";
    private String originalApellido = "";
    private String originalEmail = "";
    private String originalTelefono = "";
    private String originalDepartamento = "";
    private String originalMunicipio = "";
    private String originalDireccion = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditarPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PerfilViewModel.class);
        viewModel.limpiarEstadoExito();

        setupToolbar();
        setupButtons();
        setupUbicacionSelectores();
        setupTextWatchers();
        observeViewModel();

        precargarDatosActuales();
    }

    private void setupToolbar() {
        binding.toolbarEditarPerfil.setNavigationOnClickListener(v -> cancelarYVolver());
    }

    private void setupButtons() {
        binding.btnCancelarDatos.setOnClickListener(v -> cancelarYVolver());
        binding.btnGuardarDatos.setOnClickListener(v -> procesarGuardarDatos());
        binding.btnGuardarDatos.setEnabled(false);
    }

    private void setupUbicacionSelectores() {
        UbicacionCatalogoHelper.configurarSelectoresEncadenados(
                requireContext(),
                binding.tilDepartamento,
                binding.etDepartamento,
                binding.tilMunicipio,
                binding.etMunicipio,
                this::validarEstadoBotonGuardar
        );
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarEstadoBotonGuardar();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etNombre.addTextChangedListener(watcher);
        binding.etApellido.addTextChangedListener(watcher);
        binding.etEmail.addTextChangedListener(watcher);
        binding.etTelefono.addTextChangedListener(watcher);
        binding.etDepartamento.addTextChangedListener(watcher);
        binding.etMunicipio.addTextChangedListener(watcher);
        binding.etDireccionResidencia.addTextChangedListener(watcher);
    }

    private void precargarDatosActuales() {
        Usuario u = viewModel.getUsuario().getValue();
        if (u != null) {
            String nombreCompleto = u.getNombre() != null ? u.getNombre().trim() : "";
            if (!nombreCompleto.isEmpty()) {
                String[] partes = nombreCompleto.split("\\s+");
                if (partes.length == 1) {
                    originalNombre = partes[0];
                    originalApellido = "";
                } else {
                    originalNombre = partes[0];
                    StringBuilder apellidoSb = new StringBuilder();
                    for (int i = 1; i < partes.length; i++) {
                        if (i > 1) apellidoSb.append(" ");
                        apellidoSb.append(partes[i]);
                    }
                    originalApellido = apellidoSb.toString();
                }
            }

            originalEmail = u.getEmail() != null ? u.getEmail().trim() : "";
            originalTelefono = u.getTelefono() != null ? u.getTelefono().trim() : "";

            UbicacionRequest ub = u.getUbicacion();
            if (ub != null) {
                originalDepartamento = ub.getDepartamento() != null ? ub.getDepartamento().trim() : "Quindío";
                originalMunicipio = ub.getMunicipio() != null ? ub.getMunicipio().trim() : "Armenia";
                originalDireccion = ub.getDireccionResidencia() != null ? ub.getDireccionResidencia().trim() : "";
            } else {
                originalDepartamento = "Quindío";
                originalMunicipio = "Armenia";
                originalDireccion = "";
            }

            binding.etNombre.setText(originalNombre);
            binding.etApellido.setText(originalApellido);
            binding.etEmail.setText(originalEmail);
            binding.etTelefono.setText(originalTelefono);
            binding.etDepartamento.setText(originalDepartamento, false);
            binding.etMunicipio.setText(originalMunicipio, false);
            binding.etDireccionResidencia.setText(originalDireccion);

            // Tras precargar, verificar estado del botón
            validarEstadoBotonGuardar();
        }
    }

    private void validarEstadoBotonGuardar() {
        if (binding == null) return;

        String nombre = binding.etNombre.getText() != null ? binding.etNombre.getText().toString().trim() : "";
        String apellido = binding.etApellido.getText() != null ? binding.etApellido.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String telefono = binding.etTelefono.getText() != null ? binding.etTelefono.getText().toString().trim() : "";

        String departamento = binding.etDepartamento.getText() != null ? binding.etDepartamento.getText().toString().trim() : "";
        String municipio = binding.etMunicipio.getText() != null ? binding.etMunicipio.getText().toString().trim() : "";
        String direccion = binding.etDireccionResidencia.getText() != null ? binding.etDireccionResidencia.getText().toString().trim() : "";

        // Ningún campo debe quedar vacío
        boolean todosCamposRellenos = !nombre.isEmpty()
                && !apellido.isEmpty()
                && !email.isEmpty()
                && !telefono.isEmpty()
                && !departamento.isEmpty()
                && !municipio.isEmpty()
                && !direccion.isEmpty();

        boolean emailValido = Patterns.EMAIL_ADDRESS.matcher(email).matches();

        boolean huboModificacion = !nombre.equals(originalNombre)
                || !apellido.equals(originalApellido)
                || !email.equals(originalEmail)
                || !telefono.equals(originalTelefono)
                || !departamento.equals(originalDepartamento)
                || !municipio.equals(originalMunicipio)
                || !direccion.equals(originalDireccion);

        binding.btnGuardarDatos.setEnabled(todosCamposRellenos && emailValido && huboModificacion);
    }

    private void procesarGuardarDatos() {
        limpiarErrores();

        String nombre = binding.etNombre.getText() != null ? binding.etNombre.getText().toString().trim() : "";
        String apellido = binding.etApellido.getText() != null ? binding.etApellido.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String telefono = binding.etTelefono.getText() != null ? binding.etTelefono.getText().toString().trim() : "";

        String departamento = binding.etDepartamento.getText() != null ? binding.etDepartamento.getText().toString().trim() : "";
        String municipio = binding.etMunicipio.getText() != null ? binding.etMunicipio.getText().toString().trim() : "";
        String direccionResidencia = binding.etDireccionResidencia.getText() != null ? binding.etDireccionResidencia.getText().toString().trim() : "";

        boolean esValido = true;

        if (nombre.isEmpty()) {
            binding.tilNombre.setError("Ingresa tu nombre");
            esValido = false;
        }

        if (apellido.isEmpty()) {
            binding.tilApellido.setError("Ingresa tu apellido");
            esValido = false;
        }

        String nombreCompletoFinal = (nombre + " " + apellido).trim();
        if (nombreCompletoFinal.length() > 100) {
            binding.tilNombre.setError("El nombre y apellido combinados no pueden exceder 100 caracteres");
            esValido = false;
        }

        if (email.isEmpty()) {
            binding.tilEmail.setError("Ingresa tu correo electrónico");
            esValido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Ingresa un correo electrónico válido (ejemplo@dominio.com)");
            esValido = false;
        }

        if (telefono.isEmpty()) {
            binding.tilTelefono.setError("Ingresa tu teléfono de contacto");
            esValido = false;
        } else if (telefono.length() > 20) {
            binding.tilTelefono.setError("El teléfono no puede exceder 20 dígitos");
            esValido = false;
        }

        if (departamento.isEmpty()) {
            binding.tilDepartamento.setError("Selecciona tu departamento de residencia");
            esValido = false;
        }

        if (municipio.isEmpty()) {
            binding.tilMunicipio.setError("Selecciona tu municipio de residencia");
            esValido = false;
        }

        if (direccionResidencia.isEmpty()) {
            binding.tilDireccionResidencia.setError("Ingresa tu dirección detallada de residencia");
            esValido = false;
        }

        if (!esValido) {
            validarEstadoBotonGuardar();
            return;
        }

        UbicacionRequest ubRequest = new UbicacionRequest();
        ubRequest.setDepartamento(departamento);
        ubRequest.setMunicipio(municipio);
        ubRequest.setDireccionResidencia(direccionResidencia);

        // Deshabilitar botón y enviar a la API
        binding.btnGuardarDatos.setEnabled(false);
        viewModel.actualizarDatosPersonales(nombreCompletoFinal, email, telefono, ubRequest);
    }

    private void limpiarErrores() {
        binding.tilNombre.setError(null);
        binding.tilApellido.setError(null);
        binding.tilEmail.setError(null);
        binding.tilTelefono.setError(null);
        binding.tilDepartamento.setError(null);
        binding.tilMunicipio.setError(null);
        binding.tilDireccionResidencia.setError(null);
    }

    private void cancelarYVolver() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void observeViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            if (cargando != null) {
                if (cargando) {
                    binding.btnGuardarDatos.setEnabled(false);
                } else {
                    validarEstadoBotonGuardar();
                }
            }
        });

        viewModel.getPerfilActualizadoExito().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null && !mensaje.isEmpty()) {
                viewModel.limpiarEstadoExito();
                View mainView = requireActivity().findViewById(android.R.id.content);
                if (mainView != null) {
                    Snackbar.make(mainView, "Perfil actualizado exitosamente", Snackbar.LENGTH_SHORT).show();
                }
                cancelarYVolver();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            validarEstadoBotonGuardar();
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                if (errorMsg.toLowerCase().contains("correo") || errorMsg.toLowerCase().contains("email")) {
                    binding.tilEmail.setError(errorMsg);
                }
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
