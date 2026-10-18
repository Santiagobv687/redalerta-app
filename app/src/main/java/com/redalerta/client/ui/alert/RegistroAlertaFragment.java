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
import androidx.lifecycle.ViewModelProvider;

import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentRegistroAlertaBinding;
import com.redalerta.client.model.alerta.*;
import com.redalerta.client.model.auth.*;
import com.redalerta.client.model.usuario.*;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.util.UbicacionCatalogoHelper;
import com.redalerta.client.viewmodel.AlertaViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegistroAlertaFragment extends Fragment {

    private FragmentRegistroAlertaBinding binding;
    private String tipoSeleccionado = null;
    private SessionManager session;
    private AlertaViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroAlertaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(AlertaViewModel.class);

        // 0. Configurar selectores encadenados de ubicación
        UbicacionCatalogoHelper.configurarSelectoresEncadenados(
                requireContext(),
                binding.tilDepartamento,
                binding.etDepartamento,
                binding.tilMunicipio,
                binding.etMunicipio,
                null
        );

        // Precargar "Quindío" y "Armenia" por defecto
        binding.etDepartamento.setText("Quindío", false);
        binding.etMunicipio.setText("Armenia", false);

        // 1. Mostrar fecha y nombre del reportante
        actualizarReportadoPor();

        // 2. Listeners de las tarjetas de emergencia
        configurarTarjetasEmergencia();

        // 3. Listener del checkbox anónimo
        binding.cbAnonimo.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                binding.tvReportadoPor.setText(
                        "Reportado por: Anónimo — " + obtenerFechaActual());
            } else {
                actualizarReportadoPor();
            }
        });

        // 4. Botón registrar
        binding.btnRegistrarAlerta.setOnClickListener(v -> validarYRegistrar());

        // 5. Observers del ViewModel
        observarViewModel();
    }

    // ══════════════════════════════════════════════════════
    // Observers
    // ══════════════════════════════════════════════════════
    private void observarViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            binding.btnRegistrarAlerta.setEnabled(!cargando);
            binding.btnRegistrarAlerta.setText(
                    cargando
                            ? "Enviando..."
                            : getString(R.string.alerta_btn_registrar)
            );
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getAlertaCreada().observe(getViewLifecycleOwner(), alerta -> {
            if (alerta != null) {
                navegarAExito(alerta);
            }
        });
    }

    // ══════════════════════════════════════════════════════
    // Reportado por
    // ══════════════════════════════════════════════════════
    private void actualizarReportadoPor() {
        String nombre = session.getNombre() != null ? session.getNombre() : "Usuario";
        String fecha = obtenerFechaActual();
        binding.tvReportadoPor.setText("Reportado por: " + nombre + " — " + fecha);
    }

    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "dd/MM/yyyy, h:mm:ss a", new Locale("es", "CO"));
        return sdf.format(new Date());
    }

    // ══════════════════════════════════════════════════════
    // Tarjetas de emergencia
    // ══════════════════════════════════════════════════════
    private void configurarTarjetasEmergencia() {
        binding.cardIncendio.setOnClickListener(v ->
                seleccionarTipo(binding.cardIncendio, "Incendio"));
        binding.cardInundacion.setOnClickListener(v ->
                seleccionarTipo(binding.cardInundacion, "Inundación"));
        binding.cardAccidente.setOnClickListener(v ->
                seleccionarTipo(binding.cardAccidente, "Accidente"));
        binding.cardRobo.setOnClickListener(v ->
                seleccionarTipo(binding.cardRobo, "Robo / Hurto"));
        binding.cardMedica.setOnClickListener(v ->
                seleccionarTipo(binding.cardMedica, "Emergencia Médica"));
        binding.cardDesastre.setOnClickListener(v ->
                seleccionarTipo(binding.cardDesastre, "Desastre Natural"));
        binding.cardOtro.setOnClickListener(v ->
                seleccionarTipo(binding.cardOtro, "Otro"));
    }

    private void seleccionarTipo(LinearLayout cardSeleccionada, String tipo) {
        LinearLayout[] todas = {
                binding.cardIncendio, binding.cardInundacion, binding.cardAccidente,
                binding.cardRobo, binding.cardMedica, binding.cardDesastre, binding.cardOtro
        };
        for (LinearLayout c : todas) {
            c.setSelected(false);
        }
        cardSeleccionada.setSelected(true);
        tipoSeleccionado = tipo;
    }

    // ══════════════════════════════════════════════════════
    // Validación y envío
    // ══════════════════════════════════════════════════════
    private void validarYRegistrar() {
        if (tipoSeleccionado == null) {
            Toast.makeText(requireContext(),
                    "Seleccione el tipo de emergencia", Toast.LENGTH_SHORT).show();
            return;
        }

        String descripcion = binding.etDescripcion.getText().toString().trim();
        if (descripcion.isEmpty() || descripcion.length() > 500) {
            binding.tilDescripcion.setError(
                    "Descripción requerida (máx. 500 caracteres)");
            return;
        }
        binding.tilDescripcion.setError(null);

        String departamento = binding.etDepartamento.getText().toString().trim();
        if (departamento.isEmpty()) {
            binding.tilDepartamento.setError("Seleccione departamento");
            return;
        }
        binding.tilDepartamento.setError(null);

        String municipio = binding.etMunicipio.getText().toString().trim();
        if (municipio.isEmpty()) {
            binding.tilMunicipio.setError("Seleccione municipio");
            return;
        }
        binding.tilMunicipio.setError(null);

        String direccion = binding.etDireccion.getText().toString().trim();
        if (direccion.isEmpty()) {
            binding.etDireccion.setError("Requerido");
            return;
        }

        String barrio = binding.etBarrio.getText().toString().trim();

        int idPrioridad = binding.rgPrioridad.getCheckedRadioButtonId();
        if (idPrioridad == -1) {
            Toast.makeText(requireContext(),
                    "Seleccione un nivel de prioridad", Toast.LENGTH_SHORT).show();
            return;
        }

        String prioridad = idPrioridad == R.id.rbAlta ? "Alta" :
                idPrioridad == R.id.rbMedia ? "Media" : "Baja";

        boolean heridos  = binding.cbHeridos.isChecked();
        boolean anonimo  = binding.cbAnonimo.isChecked();
        String evidencia = binding.etEvidencia.getText().toString().trim();

        // ══════════════════════════════════════════════════
        // Construir el request
        // ══════════════════════════════════════════════════
        UbicacionAlertaDto ubAlerta = new UbicacionAlertaDto();
        ubAlerta.setDepartamento(departamento);
        ubAlerta.setMunicipio(municipio);
        String dirDetallada = direccion + (!barrio.isEmpty() ? " - Barrio " + barrio : "");
        ubAlerta.setDireccionDetallada(dirDetallada);

        CrearAlertaRequest req = new CrearAlertaRequest();
        req.setTitulo(tipoSeleccionado);
        req.setDescripcion(descripcion);
        req.setCategoria(mapearCategoria(tipoSeleccionado));
        req.setPrioridad(mapearPrioridad(prioridad));
        req.setUbicacion(ubAlerta);
        req.setDireccion(dirDetallada);
        req.setBarrio(barrio.isEmpty() ? municipio : barrio);
        req.setHayHeridos(heridos);
        req.setEsAnonima(anonimo);
        req.setEvidenciaUrl(evidencia.isEmpty() ? null : evidencia);
        req.setUsuarioId(anonimo ? null : session.getUsuarioId());

        // ══════════════════════════════════════════════════
        // Enviar al ViewModel (mock o backend)
        // ══════════════════════════════════════════════════
        viewModel.crearAlerta(req);
    }

    // ══════════════════════════════════════════════════════
    // Helpers de mapeo
    // ══════════════════════════════════════════════════════
    private String mapearCategoria(String tipo) {
        if (tipo == null) return CategoriaAlerta.OTRO;
        switch (tipo) {
            case "Incendio":          return CategoriaAlerta.INCENDIO;
            case "Inundación":        return CategoriaAlerta.INUNDACION;
            case "Accidente":         return CategoriaAlerta.ACCIDENTE;
            case "Robo / Hurto":      return CategoriaAlerta.ROBO_HURTO;
            case "Emergencia Médica": return CategoriaAlerta.EMERGENCIA_MEDICA;
            case "Desastre Natural":  return CategoriaAlerta.DESASTRE_NATURAL;
            default:                  return CategoriaAlerta.OTRO;
        }
    }

    private String mapearPrioridad(String prioridad) {
        if (prioridad == null) return PrioridadAlerta.BAJA;
        switch (prioridad) {
            case "Alta":  return PrioridadAlerta.ALTA;
            case "Media": return PrioridadAlerta.MEDIA;
            default:      return PrioridadAlerta.BAJA;
        }
    }

    // ══════════════════════════════════════════════════════
    // Navegación al éxito (solo tras confirmación del backend)
    // ══════════════════════════════════════════════════════
    private void navegarAExito(Alerta alerta) {
        AlertaRegistradaFragment exitoso = new AlertaRegistradaFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AlertaRegistradaFragment.ARG_TIPO,
                alerta.getCategoria());
        bundle.putString(AlertaRegistradaFragment.ARG_PRIORIDAD,
                alerta.getPrioridad());
        bundle.putString(AlertaRegistradaFragment.ARG_UBICACION,
                alerta.getDireccion() + ", " + alerta.getBarrio());
        exitoso.setArguments(bundle);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, exitoso)
                // .addToBackStack(null)  ← no lo usamos: el formulario ya se envió
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}