package com.redalerta.client.ui.alert;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.redalerta.client.databinding.FragmentModificarAlertaBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.EvidenciaResponse;
import com.redalerta.client.model.alerta.ModificarAlertaRequest;
import com.redalerta.client.model.alerta.UbicacionAlertaDto;
import com.redalerta.client.util.UbicacionCatalogoHelper;
import com.redalerta.client.viewmodel.AlertaViewModel;

import java.util.ArrayList;
import java.util.List;

public class ModificarAlertaFragment extends Fragment {

    public static final String ARG_ALERTA = "arg_alerta";

    private FragmentModificarAlertaBinding binding;
    private AlertaViewModel viewModel;
    private Alerta alertaOriginal;

    private final List<EvidenciasSeleccionadasAdapter.EvidenciaItem> listaEvidencias = new ArrayList<>();
    private EvidenciasSeleccionadasAdapter evidenciasAdapter;
    private ActivityResultLauncher<String> pickerLauncher;

    public static ModificarAlertaFragment newInstance(Alerta alerta) {
        ModificarAlertaFragment fragment = new ModificarAlertaFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALERTA, alerta);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentModificarAlertaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AlertaViewModel.class);

        if (getArguments() != null) {
            alertaOriginal = (Alerta) getArguments().getSerializable(ARG_ALERTA);
        }

        setupToolbar();
        setupUbicacionSelectores();
        setupEvidencias();
        setupTextWatchers();
        observeViewModel();

        precargarDatosOriginales();
    }

    private void setupToolbar() {
        binding.toolbarModificarAlerta.setNavigationOnClickListener(v -> cancelarYVolver());
    }

    private void setupUbicacionSelectores() {
        UbicacionCatalogoHelper.configurarSelectoresEncadenados(
                requireContext(),
                binding.tilDepartamento,
                binding.etDepartamento,
                binding.tilMunicipio,
                binding.etMunicipio,
                this::validarEstadoBotonGuardar
        );
    }

    private void setupEvidencias() {
        evidenciasAdapter = new EvidenciasSeleccionadasAdapter(listaEvidencias, position -> {
            if (position >= 0 && position < listaEvidencias.size()) {
                listaEvidencias.remove(position);
                evidenciasAdapter.notifyDataSetChanged();
                validarEstadoBotonGuardar();
            }
        });

        binding.rvEvidencias.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvEvidencias.setAdapter(evidenciasAdapter);

        pickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                adjuntarYSubirEvidencia(uri);
            }
        });

        binding.btnAgregarFoto.setOnClickListener(v -> {
            if (listaEvidencias.size() >= 5) {
                Toast.makeText(requireContext(), "Máximo 5 evidencias fotográficas permitidas", Toast.LENGTH_SHORT).show();
                return;
            }
            pickerLauncher.launch("image/*");
        });
    }

    private void adjuntarYSubirEvidencia(Uri uri) {
        EvidenciasSeleccionadasAdapter.EvidenciaItem item = new EvidenciasSeleccionadasAdapter.EvidenciaItem(uri);
        item.setSubiendo(true);
        listaEvidencias.add(item);
        int pos = listaEvidencias.size() - 1;
        evidenciasAdapter.notifyItemInserted(pos);
        validarEstadoBotonGuardar();

        binding.tvEstadoSubidaFotos.setText("Subiendo evidencia fotográfica...");

        viewModel.subirEvidencia(uri, new AlertaViewModel.OnEvidenciaSubidaListener() {
            @Override
            public void onExito(String urlSubida) {
                item.setSubiendo(false);
                item.setUrlSubida(urlSubida);
                evidenciasAdapter.notifyItemChanged(pos);
                binding.tvEstadoSubidaFotos.setText("Foto subida exitosamente.");
                validarEstadoBotonGuardar();
            }

            @Override
            public void onError(String mensajeError) {
                item.setSubiendo(false);
                listaEvidencias.remove(item);
                evidenciasAdapter.notifyDataSetChanged();
                binding.tvEstadoSubidaFotos.setText("No fue posible subir la imagen. Intente nuevamente.");
                Toast.makeText(requireContext(), mensajeError, Toast.LENGTH_SHORT).show();
                validarEstadoBotonGuardar();
            }
        });
    }

    private void precargarDatosOriginales() {
        if (alertaOriginal == null) return;

        if (alertaOriginal.getDescripcion() != null) {
            binding.etDescripcion.setText(alertaOriginal.getDescripcion());
        }

        UbicacionAlertaDto ub = alertaOriginal.getUbicacion();
        if (ub != null) {
            if (ub.getDepartamento() != null) binding.etDepartamento.setText(ub.getDepartamento(), false);
            if (ub.getMunicipio() != null) binding.etMunicipio.setText(ub.getMunicipio(), false);
            if (ub.getDireccionDetallada() != null) binding.etDireccionDetallada.setText(ub.getDireccionDetallada());
        } else {
            String dir = alertaOriginal.getDireccion() != null ? alertaOriginal.getDireccion() : "";
            binding.etDireccionDetallada.setText(dir);
        }

        binding.cbAnonimo.setChecked(alertaOriginal.isEsAnonimo());

        // Precargar evidencias existentes
        if (alertaOriginal.getEvidencias() != null && !alertaOriginal.getEvidencias().isEmpty()) {
            for (EvidenciaResponse ev : alertaOriginal.getEvidencias()) {
                if (ev != null && ev.getUrl() != null && !ev.getUrl().isEmpty()) {
                    EvidenciasSeleccionadasAdapter.EvidenciaItem item = new EvidenciasSeleccionadasAdapter.EvidenciaItem(null);
                    item.setUrlSubida(ev.getUrl());
                    item.setSubiendo(false);
                    listaEvidencias.add(item);
                }
            }
            evidenciasAdapter.notifyDataSetChanged();
        } else if (alertaOriginal.getEvidenciaUrl() != null && !alertaOriginal.getEvidenciaUrl().isEmpty()) {
            EvidenciasSeleccionadasAdapter.EvidenciaItem item = new EvidenciasSeleccionadasAdapter.EvidenciaItem(null);
            item.setUrlSubida(alertaOriginal.getEvidenciaUrl());
            item.setSubiendo(false);
            listaEvidencias.add(item);
            evidenciasAdapter.notifyDataSetChanged();
        }

        validarEstadoBotonGuardar();
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarEstadoBotonGuardar();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etDescripcion.addTextChangedListener(watcher);
        binding.etDepartamento.addTextChangedListener(watcher);
        binding.etMunicipio.addTextChangedListener(watcher);
        binding.etDireccionDetallada.addTextChangedListener(watcher);

        binding.cbAnonimo.setOnCheckedChangeListener((btn, checked) -> validarEstadoBotonGuardar());

        binding.btnCancelarModificacion.setOnClickListener(v -> cancelarYVolver());
        binding.btnGuardarModificacion.setOnClickListener(v -> procesarGuardarModificacion());
    }

    private void validarEstadoBotonGuardar() {
        if (binding == null) return;

        String desc = binding.etDescripcion.getText() != null ? binding.etDescripcion.getText().toString().trim() : "";
        String depto = binding.etDepartamento.getText() != null ? binding.etDepartamento.getText().toString().trim() : "";
        String mun = binding.etMunicipio.getText() != null ? binding.etMunicipio.getText().toString().trim() : "";
        String dir = binding.etDireccionDetallada.getText() != null ? binding.etDireccionDetallada.getText().toString().trim() : "";

        boolean descValida = desc.length() >= 10 && desc.length() <= 1000;
        boolean ubValida = !depto.isEmpty() && !mun.isEmpty() && !dir.isEmpty();

        boolean subiendo = false;
        for (EvidenciasSeleccionadasAdapter.EvidenciaItem item : listaEvidencias) {
            if (item.isSubiendo()) {
                subiendo = true;
                break;
            }
        }

        binding.btnGuardarModificacion.setEnabled(descValida && ubValida && !subiendo);
    }

    private void procesarGuardarModificacion() {
        if (alertaOriginal == null || alertaOriginal.getId() == null) return;

        String desc = binding.etDescripcion.getText().toString().trim();
        String depto = binding.etDepartamento.getText().toString().trim();
        String mun = binding.etMunicipio.getText().toString().trim();
        String dir = binding.etDireccionDetallada.getText().toString().trim();

        if (desc.length() < 10) {
            binding.tilDescripcion.setError("La descripción debe tener mínimo 10 caracteres");
            return;
        }

        List<String> evidencias = new ArrayList<>();
        for (EvidenciasSeleccionadasAdapter.EvidenciaItem item : listaEvidencias) {
            if (item.getUrlSubida() != null && !item.getUrlSubida().isEmpty()) {
                evidencias.add(item.getUrlSubida());
            }
        }

        UbicacionAlertaDto ubDto = new UbicacionAlertaDto(depto, mun, dir);
        ModificarAlertaRequest request = new ModificarAlertaRequest(
                desc,
                ubDto,
                evidencias,
                binding.cbAnonimo.isChecked()
        );

        try {
            long id = Long.parseLong(alertaOriginal.getId());
            binding.btnGuardarModificacion.setEnabled(false);
            viewModel.modificarAlerta(id, request);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "ID de alerta no válido", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelarYVolver() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void observeViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            if (cargando != null) {
                binding.btnGuardarModificacion.setEnabled(!cargando);
                binding.btnGuardarModificacion.setText(cargando ? "Guardando cambios..." : "Guardar Cambios");
            }
        });

        viewModel.getAlertaModificada().observe(getViewLifecycleOwner(), alerta -> {
            if (alerta != null) {
                Toast.makeText(requireContext(), "Alerta modificada exitosamente", Toast.LENGTH_SHORT).show();
                cancelarYVolver();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            binding.btnGuardarModificacion.setEnabled(true);
            if (msg != null && !msg.trim().isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
