package com.redalerta.client.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentRegistroPaso3Binding;
import com.redalerta.client.model.RegistroRequest;
import com.redalerta.client.util.Validaciones;
import com.redalerta.client.viewmodel.AuthViewModel;

public class RegistroPaso3Fragment extends Fragment {

    private FragmentRegistroPaso3Binding binding;
    private AuthViewModel viewModel;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroPaso3Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // 1. Pintar el resumen con los datos acumulados
        pintarResumen();

        // 2. Checkbox con link rojo a Términos y condiciones
        configurarCheckboxTerminos();

        // 3. Navegación
        binding.btnAnterior.setOnClickListener(v ->
                ((RegistroActivity) requireActivity()).mostrarPaso(2));

        binding.btnCrear.setOnClickListener(v -> intentarCrearCuenta());

        // 4. Observar respuesta del backend
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            binding.progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
            binding.btnCrear.setEnabled(!cargando);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });

        viewModel.getRegistroExitoso().observe(getViewLifecycleOwner(), exito -> {
            if (Boolean.TRUE.equals(exito)) {
                RegistroRequest req =
                        ((RegistroActivity) requireActivity()).getRegistroRequest();

                Intent intent = new Intent(requireContext(), RegistroExitosoActivity.class);
                intent.putExtra(RegistroExitosoActivity.EXTRA_NOMBRE,   req.getNombre());
                intent.putExtra(RegistroExitosoActivity.EXTRA_APELLIDO, req.getApellido());
                intent.putExtra(RegistroExitosoActivity.EXTRA_CORREO,   req.getCorreo());
                startActivity(intent);
                requireActivity().finish();
            }
        });
    }

    private void pintarResumen() {
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();
        String nombre = safe(req.getNombre());
        String apellido = safe(req.getApellido());
        String correo = safe(req.getCorreo());
        String localidad = safe(req.getLocalidad());
        String barrio = safe(req.getBarrio());
        String direccion = safe(req.getDireccion());
        String rol = rolLegible(req.getRol());

        String resumen =
                nombre + " " + apellido + "\n" +
                        correo + "\n" +
                        localidad + " — " + barrio + "\n" +
                        direccion + "\n" +
                        "Rol: " + rol;

        binding.tvResumen.setText(resumen);
    }

    private void configurarCheckboxTerminos() {
        String texto = getString(R.string.reg_terminos);
        SpannableString ss = new SpannableString(texto);

        String link = "Términos y condiciones";
        int start = texto.indexOf(link);
        int end = start + link.length();

        if (start >= 0) {
            int colorRojo = ContextCompat.getColor(requireContext(), R.color.ra_red);
            ss.setSpan(new ForegroundColorSpan(colorRojo), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) {
                    Toast.makeText(requireContext(),
                            "Abriendo Términos y condiciones…", Toast.LENGTH_SHORT).show();
                    // TODO: abrir WebView o DialogFragment con el contenido legal
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        binding.cbTerminos.setText(ss);
        binding.cbTerminos.setMovementMethod(LinkMovementMethod.getInstance());
        binding.cbTerminos.setHighlightColor(Color.TRANSPARENT);
    }

    private void intentarCrearCuenta() {
        String contrasena = binding.etContrasena.getText().toString();
        String confirmar = binding.etConfirmar.getText().toString();

        if (contrasena.length() < 6) {
            binding.tilContrasena.setError("Mínimo 6 caracteres");
            return;
        }
        binding.tilContrasena.setError(null);

        if (!contrasena.equals(confirmar)) {
            binding.tilConfirmar.setError("Las contraseñas no coinciden");
            return;
        }
        binding.tilConfirmar.setError(null);

        if (!binding.cbTerminos.isChecked()) {
            Toast.makeText(requireContext(),
                    "Debe aceptar los términos y condiciones", Toast.LENGTH_SHORT).show();
            return;
        }

        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();
        req.setContrasena(contrasena);

        // Enviar al backend
        viewModel.registrar(req);
    }

    private String safe(String v) { return v != null ? v : "—"; }

    private String rolLegible(String rol) {
        if (rol == null) return "—";
        switch (rol) {
            case "USUARIO":      return "Ciudadano";
            case "LIDER":        return "Operador";
            case "ORGANIZACION": return "Supervisor";
            default:             return rol;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}