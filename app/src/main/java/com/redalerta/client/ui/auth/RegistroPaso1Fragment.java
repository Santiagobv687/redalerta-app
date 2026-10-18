package com.redalerta.client.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputLayout;
import com.redalerta.client.databinding.FragmentRegistroPaso1Binding;
import com.redalerta.client.model.DocumentoRequest;
import com.redalerta.client.model.RegistroRequest;
import com.redalerta.client.util.Validaciones;

public class RegistroPaso1Fragment extends Fragment {

    private FragmentRegistroPaso1Binding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroPaso1Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Dropdown de Tipos de Documento
        String[] tipos = {"Cédula de Ciudadanía (CC)", "Cédula de Extranjería (CE)", "Pasaporte"};
        binding.etTipoDoc.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                tipos));

        // Cargar datos previos si el usuario retrocede
        cargardatosPrevios();

        // Configurar limpieza dinámica de errores al digitar
        configurarLimpiezaErrores();

        binding.btnSiguiente.setOnClickListener(v -> {
            if (validar()) {
                guardarDatos();
                ((RegistroActivity) requireActivity()).mostrarPaso(2);
            }
        });
    }

    private void configurarLimpiezaErrores() {
        limpiarErrorAlDigitar(binding.tilTipoDoc, binding.etTipoDoc);
        limpiarErrorAlDigitar(binding.tilNumDoc, binding.etNumDoc);
        limpiarErrorAlDigitar(binding.tilNombre, binding.etNombre);
        limpiarErrorAlDigitar(binding.tilApellido, binding.etApellido);
        limpiarErrorAlDigitar(binding.tilCorreo, binding.etCorreo);
        limpiarErrorAlDigitar(binding.tilTelefono, binding.etTelefono);
    }

    private void limpiarErrorAlDigitar(TextInputLayout til, EditText et) {
        if (til == null || et == null) return;
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                til.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void cargardatosPrevios() {
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();
        if (req != null) {
            if (req.getNombre() != null) binding.etNombre.setText(req.getNombre());
            if (req.getApellido() != null) binding.etApellido.setText(req.getApellido());
            if (req.getCorreo() != null) binding.etCorreo.setText(req.getCorreo());
            if (req.getTelefono() != null) binding.etTelefono.setText(req.getTelefono());

            DocumentoRequest doc = req.getDocumento();
            if (doc != null) {
                if (doc.getTipo() != null) binding.etTipoDoc.setText(doc.getTipo(), false);
                if (doc.getNumero() != null) binding.etNumDoc.setText(doc.getNumero());
            }
        }
    }

    private boolean validar() {
        boolean ok = true;
        String tipoDoc = binding.etTipoDoc.getText().toString().trim();
        String numDoc = binding.etNumDoc.getText().toString().trim();
        String nombre = binding.etNombre.getText().toString().trim();
        String apellido = binding.etApellido.getText().toString().trim();
        String correo = binding.etCorreo.getText().toString().trim();
        String telefono = binding.etTelefono.getText().toString().trim();

        if (tipoDoc.isEmpty()) { binding.tilTipoDoc.setError("Seleccione tipo de documento"); ok = false; } else { binding.tilTipoDoc.setError(null); }
        if (numDoc.isEmpty()) { binding.tilNumDoc.setError("Número de documento requerido"); ok = false; } else { binding.tilNumDoc.setError(null); }
        if (nombre.isEmpty()) { binding.tilNombre.setError("Nombre requerido (Mín. 3 caracteres)"); ok = false; } else { binding.tilNombre.setError(null); }
        if (apellido.isEmpty()) { binding.tilApellido.setError("Apellido requerido"); ok = false; } else { binding.tilApellido.setError(null); }
        if (!Validaciones.esCorreoValido(correo)) { binding.tilCorreo.setError("Correo electrónico inválido"); ok = false; } else { binding.tilCorreo.setError(null); }
        if (telefono.length() > 20) { binding.tilTelefono.setError("Máximo 20 dígitos"); ok = false; } else { binding.tilTelefono.setError(null); }

        return ok;
    }

    private void guardarDatos() {
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();

        String nombre = binding.etNombre.getText().toString().trim();
        String apellido = binding.etApellido.getText().toString().trim();
        String correo = binding.etCorreo.getText().toString().trim();
        String telefono = binding.etTelefono.getText().toString().trim();

        req.setNombre(nombre + " " + apellido);
        req.setApellido(apellido);
        req.setCorreo(correo);
        req.setTelefono(telefono);

        // Documento
        String tipoDoc = binding.etTipoDoc.getText().toString().trim();
        String numDoc = binding.etNumDoc.getText().toString().trim();
        req.setTipoDocumento(tipoDoc);
        req.setNumeroDocumento(numDoc);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
