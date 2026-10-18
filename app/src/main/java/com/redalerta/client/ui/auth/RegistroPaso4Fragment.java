package com.redalerta.client.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputLayout;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentRegistroPaso4Binding;
import com.redalerta.client.model.RegistroRequest;
import com.redalerta.client.viewmodel.AuthViewModel;

public class RegistroPaso4Fragment extends Fragment {

    private FragmentRegistroPaso4Binding binding;
    private AuthViewModel viewModel;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroPaso4Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // 1. Resumen de registro
        pintarResumen();

        // 2. Checkbox con link a Términos
        configurarCheckboxTerminos();

        // 3. Configurar limpieza dinámica de errores al digitar
        configurarLimpiezaErrores();

        // 4. Navegación
        binding.btnAnterior.setOnClickListener(v ->
                ((RegistroActivity) requireActivity()).mostrarPaso(3));

        binding.btnCrear.setOnClickListener(v -> intentarCrearCuenta());

        // 5. Observar respuesta del ViewModel
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            binding.progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
            binding.btnCrear.setEnabled(!cargando);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                binding.tilContrasena.setError(msg);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
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

    private void configurarLimpiezaErrores() {
        limpiarErrorAlDigitar(binding.tilContrasena, binding.etContrasena);
        limpiarErrorAlDigitar(binding.tilConfirmar, binding.etConfirmar);
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

    private void pintarResumen() {
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();
        String nombre = safe(req.getNombre());
        String apellido = safe(req.getApellido());
        String correo = safe(req.getCorreo());
        String localidad = safe(req.getLocalidad());
        String barrio = safe(req.getBarrio());
        String direccion = safe(req.getDireccion());

        String resumen =
                nombre + " " + apellido + "\n" +
                        correo + "\n" +
                        localidad + " — " + barrio + "\n" +
                        direccion;

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

        // Validaciones de Contraseña
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

        // Armar objeto RegistroRequest completo
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();
        req.setContrasena(contrasena);

        // Enviar al backend
        viewModel.registrar(req);
    }

    private String safe(String v) { return v != null ? v : "—"; }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
