package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.redalerta.client.R;
import com.redalerta.client.databinding.BottomSheetDireccionarAlertaBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.OrganizacionResponse;
import com.redalerta.client.viewmodel.AlertaLiderViewModel;

import java.util.ArrayList;
import java.util.List;

public class DireccionarAlertaBottomSheet extends BottomSheetDialogFragment {

    public static final String ARG_ALERTA = "arg_alerta";
    public static final String ARG_IS_MODIFICACION = "arg_is_modificacion";

    private BottomSheetDireccionarAlertaBinding binding;
    private AlertaLiderViewModel viewModel;
    private Alerta alerta;
    private boolean isModificacion;
    private List<OrganizacionResponse> listaOrganizaciones = new ArrayList<>();
    private Long organizacionIdSeleccionada = null;

    public static DireccionarAlertaBottomSheet newInstance(Alerta alerta, boolean isModificacion) {
        DireccionarAlertaBottomSheet fragment = new DireccionarAlertaBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALERTA, alerta);
        args.putBoolean(ARG_IS_MODIFICACION, isModificacion);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetDireccionarAlertaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AlertaLiderViewModel.class);

        if (getArguments() != null) {
            alerta = (Alerta) getArguments().getSerializable(ARG_ALERTA);
            isModificacion = getArguments().getBoolean(ARG_IS_MODIFICACION);
        }

        setupCategorias();
        setupTextWatchers();
        observeViewModel();

        if (isModificacion) {
            precargarDatosOriginales();
        }

        viewModel.cargarOrganizacionesElegibles();
    }

    private void setupCategorias() {
        String[] categorias = {
                "DESASTRE NATURAL", "INCENDIO", "SEGURIDAD CIUDADANA",
                "INFRAESTRUCTURA Y SERVICIOS", "SALUD Y EMERGENCIA MEDICA", "AMBIENTAL", "OTRO"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categorias);
        binding.etCategoria.setAdapter(adapter);

        if (isModificacion && alerta != null && alerta.getCategoria() != null) {
            binding.etCategoria.setText(alerta.getCategoria().replace("_", " ").toUpperCase(), false);
        }
    }

    private void precargarDatosOriginales() {
        if (alerta == null) return;

        // 1. Categoría
        if (alerta.getCategoria() != null) {
            String catNorm = alerta.getCategoria().replace("_", " ").toUpperCase();
            binding.etCategoria.setText(catNorm, false);
        }

        // 2. Prioridad
        if (alerta.getPrioridad() != null) {
            String prio = alerta.getPrioridad().toUpperCase();
            if (prio.contains("ALTA")) {
                binding.rgPrioridad.check(R.id.rbAlta);
            } else if (prio.contains("BAJA")) {
                binding.rgPrioridad.check(R.id.rbBaja);
            } else {
                binding.rgPrioridad.check(R.id.rbMedia);
            }
        }

        // 3. Observación Técnica
        if (alerta.getObservacionTecnica() != null) {
            binding.etObservacion.setText(alerta.getObservacionTecnica());
        }

        // 4. Intentar seleccionar organización previa
        seleccionarOrganizacionPrevia();
    }

    private void seleccionarOrganizacionPrevia() {
        if (alerta == null || listaOrganizaciones == null || listaOrganizaciones.isEmpty()) return;

        Long targetId = alerta.getOrganizacionId();
        String targetNombre = alerta.getOrganizacionNombre();

        for (OrganizacionResponse o : listaOrganizaciones) {
            boolean coincideId = targetId != null && targetId.equals(o.getId());
            boolean coincideNombre = targetNombre != null && o.getNombre() != null && o.getNombre().equalsIgnoreCase(targetNombre.trim());

            if (coincideId || coincideNombre) {
                organizacionIdSeleccionada = o.getId();
                binding.etOrganizacion.setText(o.getNombre(), false);
                break;
            }
        }
        validarFormulario();
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarFormulario();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etCategoria.addTextChangedListener(watcher);
        binding.etOrganizacion.addTextChangedListener(watcher);
        binding.etObservacion.addTextChangedListener(watcher);
        binding.rgPrioridad.setOnCheckedChangeListener((group, checkedId) -> validarFormulario());

        binding.etOrganizacion.setOnItemClickListener((parent, view, position, id) -> {
            if (listaOrganizaciones != null && position < listaOrganizaciones.size()) {
                organizacionIdSeleccionada = listaOrganizaciones.get(position).getId();
                validarFormulario();
            }
        });

        binding.btnEjecutarDireccionamiento.setOnClickListener(v -> procesarDireccionamiento());
    }

    private void validarFormulario() {
        if (binding == null) return;
        String cat = binding.etCategoria.getText().toString().trim();
        String org = binding.etOrganizacion.getText().toString().trim();
        String obs = binding.etObservacion.getText().toString().trim();
        int prio = binding.rgPrioridad.getCheckedRadioButtonId();

        boolean valido = !cat.isEmpty() && !org.isEmpty() && organizacionIdSeleccionada != null
                && prio != -1 && obs.length() >= 10;

        binding.btnEjecutarDireccionamiento.setEnabled(valido);
    }

    private void procesarDireccionamiento() {
        if (alerta == null || alerta.getId() == null) return;

        String catRaw = binding.etCategoria.getText().toString().trim();
        String catFormateada = catRaw.replace(" ", "_").toUpperCase();

        int idPrio = binding.rgPrioridad.getCheckedRadioButtonId();
        String prioridad = idPrio == R.id.rbAlta ? "ALTA" : idPrio == R.id.rbMedia ? "MEDIA" : "BAJA";

        String observacion = binding.etObservacion.getText().toString().trim();

        try {
            long alertaId = Long.parseLong(alerta.getId());
            if (isModificacion) {
                viewModel.modificarDireccionamiento(alertaId, catFormateada, prioridad, organizacionIdSeleccionada, observacion);
            } else {
                viewModel.direccionarAlerta(alertaId, catFormateada, prioridad, organizacionIdSeleccionada, observacion);
            }
            dismiss();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error al procesar ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeViewModel() {
        viewModel.getOrganizacionesElegibles().observe(getViewLifecycleOwner(), orgs -> {
            if (orgs != null) {
                listaOrganizaciones = orgs;
                List<String> nombres = new ArrayList<>();
                for (OrganizacionResponse o : orgs) {
                    nombres.add(o.getNombre());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres);
                binding.etOrganizacion.setAdapter(adapter);

                if (isModificacion) {
                    seleccionarOrganizacionPrevia();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
