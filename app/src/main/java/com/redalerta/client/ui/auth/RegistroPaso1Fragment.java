package com.redalerta.client.ui.auth;

import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.redalerta.client.databinding.FragmentRegistroPaso1Binding;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        // Spinner de tipos de documento
        String[] tipos = {"CC", "CE", "TI", "Pasaporte"};
        binding.etTipoDoc.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                tipos));

        binding.btnSiguiente.setOnClickListener(v -> {
            if (validar()) {
                ((RegistroActivity) requireActivity()).mostrarPaso(2);
            }
        });
    }

    private boolean validar() {
        boolean ok = true;
        String tipoDoc   = binding.etTipoDoc.getText().toString();
        String numDoc    = binding.etNumDoc.getText().toString().trim();
        String nombre    = binding.etNombre.getText().toString().trim();
        String apellido  = binding.etApellido.getText().toString().trim();
        String correo    = binding.etCorreo.getText().toString().trim();
        String telefono  = binding.etTelefono.getText().toString().trim();

        if (tipoDoc.isEmpty())   { binding.tilTipoDoc.setError("Seleccione"); ok = false; }
        if (numDoc.isEmpty())    { binding.etNumDoc.setError("Requerido"); ok = false; }
        if (nombre.isEmpty())    { binding.etNombre.setError("Requerido"); ok = false; }
        if (apellido.isEmpty())  { binding.etApellido.setError("Requerido"); ok = false; }
        if (!Validaciones.esCorreoValido(correo)) {
            binding.etCorreo.setError("Correo inválido"); ok = false;
        }
        if (telefono.length() < 10) { binding.etTelefono.setError("Inválido"); ok = false; }
        return ok;
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}