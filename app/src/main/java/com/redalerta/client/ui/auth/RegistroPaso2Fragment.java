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
import com.redalerta.client.databinding.FragmentRegistroPaso2Binding;
import com.redalerta.client.model.auth.RegistroRequest;
import com.redalerta.client.model.usuario.UbicacionRequest;

public class RegistroPaso2Fragment extends Fragment {

    private FragmentRegistroPaso2Binding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroPaso2Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();

        // Dropdown de Departamentos
        String[] departamentos = {"Quindío", "Risaralda", "Caldas", "Antioquia", "Cundinamarca", "Otro"};
        binding.etDepartamento.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                departamentos));

        // Dropdown de Municipios
        String[] municipios = {"Armenia", "Calarcá", "Circasia", "Salento", "Montenegro", "Quimbaya", "La Tebaida", "Pereira", "Manizales", "Bogotá D.C.", "Otro"};
        binding.etMunicipio.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                municipios));

        // Cargar datos previos si existen
        cargardatosPrevios(req);

        // Configurar limpieza dinámica de errores al digitar
        configurarLimpiezaErrores();

        // Navegación
        binding.btnAnterior.setOnClickListener(v ->
                ((RegistroActivity) requireActivity()).mostrarPaso(1));

        binding.btnSiguiente.setOnClickListener(v -> {
            if (validar()) {
                guardarDatos(req);
                ((RegistroActivity) requireActivity()).mostrarPaso(3);
            }
        });
    }

    private void configurarLimpiezaErrores() {
        limpiarErrorAlDigitar(binding.tilDepartamento, binding.etDepartamento);
        limpiarErrorAlDigitar(binding.tilMunicipio, binding.etMunicipio);
        limpiarErrorAlDigitar(binding.tilBarrio, binding.etBarrio);
        limpiarErrorAlDigitar(binding.tilDireccion, binding.etDireccion);
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

    private void cargardatosPrevios(RegistroRequest req) {
        if (req != null) {
            UbicacionRequest u = req.getUbicacion();
            if (u != null) {
                if (u.getDepartamento() != null) binding.etDepartamento.setText(u.getDepartamento(), false);
                if (u.getMunicipio() != null) binding.etMunicipio.setText(u.getMunicipio(), false);
                if (u.getDireccionResidencia() != null) binding.etDireccion.setText(req.getDireccion() != null ? req.getDireccion() : u.getDireccionResidencia());
            }
            if (req.getBarrio() != null) binding.etBarrio.setText(req.getBarrio());
        }
    }

    private boolean validar() {
        boolean ok = true;
        String departamento = binding.etDepartamento.getText().toString().trim();
        String municipio = binding.etMunicipio.getText().toString().trim();
        String barrio = binding.etBarrio.getText().toString().trim();
        String direccion = binding.etDireccion.getText().toString().trim();

        if (departamento.isEmpty()) { binding.tilDepartamento.setError("Seleccione departamento"); ok = false; } else { binding.tilDepartamento.setError(null); }
        if (municipio.isEmpty()) { binding.tilMunicipio.setError("Seleccione municipio"); ok = false; } else { binding.tilMunicipio.setError(null); }
        if (barrio.isEmpty()) { binding.tilBarrio.setError("Barrio requerido"); ok = false; } else { binding.tilBarrio.setError(null); }
        if (direccion.isEmpty()) { binding.tilDireccion.setError("Dirección requerida"); ok = false; } else { binding.tilDireccion.setError(null); }

        return ok;
    }

    private void guardarDatos(RegistroRequest req) {
        if (req != null) {
            String departamento = binding.etDepartamento.getText().toString().trim();
            String municipio = binding.etMunicipio.getText().toString().trim();
            String barrio = binding.etBarrio.getText().toString().trim();
            String direccion = binding.etDireccion.getText().toString().trim();

            req.setLocalidad(municipio);
            req.setBarrio(barrio);
            req.setDireccion(direccion);
            req.setRol("ROLE_CIUDADANO");

            UbicacionRequest u = req.getUbicacion();
            if (u == null) u = new UbicacionRequest();
            u.setDepartamento(departamento);
            u.setMunicipio(municipio);
            String dirFinal = direccion + (barrio.isEmpty() ? "" : ", " + barrio);
            u.setDireccionResidencia(dirFinal);
            req.setUbicacion(u);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
