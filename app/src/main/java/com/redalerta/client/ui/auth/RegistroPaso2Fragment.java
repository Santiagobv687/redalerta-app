package com.redalerta.client.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentRegistroPaso2Binding;
import com.redalerta.client.model.auth.RegistroRequest;
import com.redalerta.client.model.usuario.UbicacionRequest;
import com.redalerta.client.util.UbicacionCatalogoHelper;

public class RegistroPaso2Fragment extends Fragment {

    private FragmentRegistroPaso2Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroPaso2Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();

        // Configurar selectores encadenados dinámicos y feedback visual
        UbicacionCatalogoHelper.configurarSelectoresEncadenados(
                requireContext(),
                binding.tilDepartamento,
                binding.etDepartamento,
                binding.tilMunicipio,
                binding.etMunicipio,
                binding.progressUbicacion,
                null
        );

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
                limpiarErrorCampo(til);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void limpiarErrorCampo(TextInputLayout til) {
        if (til != null) {
            til.setError(null);
            til.setErrorEnabled(false);
        }
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

        if (departamento.isEmpty()) {
            binding.tilDepartamento.setError(getString(R.string.reg_error_depto_requerido));
            ok = false;
        } else {
            limpiarErrorCampo(binding.tilDepartamento);
        }

        if (municipio.isEmpty()) {
            binding.tilMunicipio.setError(getString(R.string.reg_error_municipio_requerido));
            ok = false;
        } else {
            limpiarErrorCampo(binding.tilMunicipio);
        }

        if (barrio.isEmpty()) {
            binding.tilBarrio.setError("Barrio requerido");
            ok = false;
        } else {
            limpiarErrorCampo(binding.tilBarrio);
        }

        if (direccion.isEmpty()) {
            binding.tilDireccion.setError("Dirección requerida");
            ok = false;
        } else {
            limpiarErrorCampo(binding.tilDireccion);
        }

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
