package com.redalerta.client.ui.auth;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentRegistroPaso2Binding;
import com.redalerta.client.model.RegistroRequest;

public class RegistroPaso2Fragment extends Fragment {

    private FragmentRegistroPaso2Binding binding;
    private String rolSeleccionado = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroPaso2Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Localidades de ejemplo — en producción vienen del backend
        String[] localidades = {
                "Armenia", "Barrios Unidos", "Laureles", "El Poblado",
                "Centro", "La Esperanza", "Villa Jardín", "Otro"
        };
        binding.etLocalidad.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                localidades));

        // Restaurar datos previos si existen
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();
        if (req.getLocalidad() != null) binding.etLocalidad.setText(req.getLocalidad(), false);
        if (req.getBarrio() != null) binding.etBarrio.setText(req.getBarrio());
        if (req.getDireccion() != null) binding.etDireccion.setText(req.getDireccion());
        if (req.getRol() != null) {
            rolSeleccionado = req.getRol();
            resaltarRol();
        }

        // Listeners de los chips de rol
        binding.btnRolCiudadano.setOnClickListener(v -> seleccionarRol("CIUDADANO"));

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

    private void seleccionarRol(String rol) {
        rolSeleccionado = rol;
        resaltarRol();
    }

    private void resaltarRol() {
        int rojo = ContextCompat.getColor(requireContext(), R.color.ra_red);
        int blanco = Color.WHITE;
        int bordeGris = ContextCompat.getColor(requireContext(), R.color.ra_light_border);
        int textoOscuro = ContextCompat.getColor(requireContext(), R.color.ra_light_text);

        MaterialButton[] botones = {
                binding.btnRolCiudadano
        };
        String[] roles = {"USUARIO", "LIDER", "ORGANIZACION"};

        for (int i = 0; i < botones.length; i++) {
            MaterialButton b = botones[i];
            if (roles[i].equals(rolSeleccionado)) {
                b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(rojo));
                b.setTextColor(blanco);
                b.setStrokeColor(android.content.res.ColorStateList.valueOf(rojo));
            } else {
                b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
                b.setTextColor(textoOscuro);
                b.setStrokeColor(android.content.res.ColorStateList.valueOf(bordeGris));
            }
        }
    }

    private boolean validar() {
        boolean ok = true;
        String localidad = binding.etLocalidad.getText().toString().trim();
        String barrio = binding.etBarrio.getText().toString().trim();
        String direccion = binding.etDireccion.getText().toString().trim();

        if (localidad.isEmpty()) {
            binding.tilLocalidad.setError("Seleccione una localidad");
            ok = false;
        } else {
            binding.tilLocalidad.setError(null);
        }

        if (barrio.isEmpty()) {
            binding.etBarrio.setError("Requerido");
            ok = false;
        } else {
            binding.etBarrio.setError(null);
        }

        if (direccion.isEmpty()) {
            binding.etDireccion.setError("Requerido");
            ok = false;
        } else {
            binding.etDireccion.setError(null);
        }

        if (rolSeleccionado == null) {
            android.widget.Toast.makeText(requireContext(),
                    "Seleccione un rol en el sistema", android.widget.Toast.LENGTH_SHORT).show();
            ok = false;
        }
        return ok;
    }

    private void guardarDatos(RegistroRequest req) {
        req.setLocalidad(binding.etLocalidad.getText().toString().trim());
        req.setBarrio(binding.etBarrio.getText().toString().trim());
        req.setDireccion(binding.etDireccion.getText().toString().trim());
        req.setRol(rolSeleccionado);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}