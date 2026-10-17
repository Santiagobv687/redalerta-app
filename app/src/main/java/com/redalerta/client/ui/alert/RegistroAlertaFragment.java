package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentRegistroAlertaBinding;
import com.redalerta.client.util.SessionManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegistroAlertaFragment extends Fragment {

    private FragmentRegistroAlertaBinding binding;
    private String tipoSeleccionado = null;
    private SessionManager session;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroAlertaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());

        // 1. Mostrar fecha y nombre del reportante
        actualizarReportadoPor();

        // 2. Listeners de las tarjetas de emergencia
        configurarTarjetasEmergencia();

        // 3. Listener del checkbox anónimo (cambia el texto de "Reportado por")
        binding.cbAnonimo.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                binding.tvReportadoPor.setText("Reportado por: Anónimo — " + obtenerFechaActual());
            } else {
                actualizarReportadoPor();
            }
        });

        // 4. Botón registrar
        binding.btnRegistrarAlerta.setOnClickListener(v -> validarYRegistrar());
    }

    private void actualizarReportadoPor() {
        String nombre = session.getNombre() != null ? session.getNombre() : "Usuario";
        String fecha = obtenerFechaActual();
        binding.tvReportadoPor.setText("Reportado por: " + nombre + " — " + fecha);
    }

    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, h:mm:ss a", new Locale("es", "CO"));
        return sdf.format(new Date());
    }

    private void configurarTarjetasEmergencia() {
        binding.cardIncendio.setOnClickListener(v -> seleccionarTipo(binding.cardIncendio, "Incendio"));
        binding.cardInundacion.setOnClickListener(v -> seleccionarTipo(binding.cardInundacion, "Inundación"));
        binding.cardAccidente.setOnClickListener(v -> seleccionarTipo(binding.cardAccidente, "Accidente"));
        binding.cardRobo.setOnClickListener(v -> seleccionarTipo(binding.cardRobo, "Robo / Hurto"));
        binding.cardMedica.setOnClickListener(v -> seleccionarTipo(binding.cardMedica, "Emergencia Médica"));
        binding.cardDesastre.setOnClickListener(v -> seleccionarTipo(binding.cardDesastre, "Desastre Natural"));
        binding.cardOtro.setOnClickListener(v -> seleccionarTipo(binding.cardOtro, "Otro"));
    }

    private void seleccionarTipo(LinearLayout cardSeleccionada, String tipo) {
        // Desmarcar todas
        LinearLayout[] todas = {
                binding.cardIncendio, binding.cardInundacion, binding.cardAccidente,
                binding.cardRobo, binding.cardMedica, binding.cardDesastre, binding.cardOtro
        };
        for (LinearLayout c : todas) {
            c.setSelected(false);
        }
        // Marcar la seleccionada
        cardSeleccionada.setSelected(true);
        tipoSeleccionado = tipo;
    }

    private void validarYRegistrar() {
        if (tipoSeleccionado == null) {
            Toast.makeText(requireContext(), "Seleccione el tipo de emergencia", Toast.LENGTH_SHORT).show();
            return;
        }

        String descripcion = binding.etDescripcion.getText().toString().trim();
        if (descripcion.isEmpty() || descripcion.length() > 500) {
            binding.tilDescripcion.setError("Descripción requerida (máx. 500 caracteres)");
            return;
        }
        binding.tilDescripcion.setError(null);

        String direccion = binding.etDireccion.getText().toString().trim();
        if (direccion.isEmpty()) {
            binding.etDireccion.setError("Requerido");
            return;
        }

        String barrio = binding.etBarrio.getText().toString().trim();
        if (barrio.isEmpty()) {
            binding.etBarrio.setError("Requerido");
            return;
        }

        int idPrioridad = binding.rgPrioridad.getCheckedRadioButtonId();
        if (idPrioridad == -1) {
            Toast.makeText(requireContext(), "Seleccione un nivel de prioridad", Toast.LENGTH_SHORT).show();
            return;
        }

        String prioridad = idPrioridad == R.id.rbAlta ? "Alta" :
                idPrioridad == R.id.rbMedia ? "Media" : "Baja";

        boolean heridos = binding.cbHeridos.isChecked();
        boolean anonimo = binding.cbAnonimo.isChecked();
        String evidencia = binding.etEvidencia.getText().toString().trim();

        // TODO: Enviar al backend
        // TODO: Enviar al backend real (POST /api/alertas)

// Navegar a la pantalla de éxito
        AlertaRegistradaFragment exitosoFragment = new AlertaRegistradaFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AlertaRegistradaFragment.ARG_TIPO, tipoSeleccionado);
        bundle.putString(AlertaRegistradaFragment.ARG_PRIORIDAD, prioridad);
        bundle.putString(AlertaRegistradaFragment.ARG_UBICACION, direccion + ", " + barrio);
        exitosoFragment.setArguments(bundle);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, exitosoFragment)
                .addToBackStack(null) // Permite volver atrás
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}