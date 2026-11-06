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
import com.redalerta.client.databinding.FragmentRegistroAlertaBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.CrearAlertaRequest;
import com.redalerta.client.model.alerta.UbicacionAlertaDto;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.util.UbicacionCatalogoHelper;
import com.redalerta.client.viewmodel.AlertaViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegistroAlertaFragment extends Fragment {

    private FragmentRegistroAlertaBinding binding;
    private SessionManager session;
    private AlertaViewModel viewModel;

    // Gestión de evidencias fotográficas seleccionadas
    private final List<EvidenciasSeleccionadasAdapter.EvidenciaItem> listaEvidencias = new ArrayList<>();
    private EvidenciasSeleccionadasAdapter evidenciasAdapter;
    private ActivityResultLauncher<String> pickerLauncher;

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
        super.onViewCreated(view, savedInstanceState);

        session = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(AlertaViewModel.class);

        setupToolbar();
        setupUbicacionSelectores();
        setupEvidencias();
        setupTextWatchers();
        observarViewModel();

        actualizarReportadoPor();
        validarEstadoBotonPublicar();
    }

    private void setupToolbar() {
        binding.toolbarReportarAlerta.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
    }

    private void setupUbicacionSelectores() {
        UbicacionCatalogoHelper.configurarSelectoresEncadenados(
                requireContext(),
                binding.tilDepartamento,
                binding.etDepartamento,
                binding.tilMunicipio,
                binding.etMunicipio,
                this::validarEstadoBotonPublicar
        );

        // Precargar departamento y municipio predeterminados si no tienen texto
        if (binding.etDepartamento.getText().toString().isEmpty()) {
            binding.etDepartamento.setText("Quindío", false);
        }
        if (binding.etMunicipio.getText().toString().isEmpty()) {
            binding.etMunicipio.setText("Armenia", false);
        }
    }

    private void setupEvidencias() {
        evidenciasAdapter = new EvidenciasSeleccionadasAdapter(listaEvidencias, position -> {
            if (position >= 0 && position < listaEvidencias.size()) {
                listaEvidencias.remove(position);
                evidenciasAdapter.notifyDataSetChanged();
                validarEstadoBotonPublicar();
            }
        });

        binding.rvEvidencias.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvEvidencias.setAdapter(evidenciasAdapter);

        // Launcher de galería de imágenes
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
        validarEstadoBotonPublicar();

        binding.tvEstadoSubidaFotos.setText("Subiendo evidencia fotográfica...");

        viewModel.subirEvidencia(uri, new AlertaViewModel.OnEvidenciaSubidaListener() {
            @Override
            public void onExito(String urlSubida) {
                item.setSubiendo(false);
                item.setUrlSubida(urlSubida);
                evidenciasAdapter.notifyItemChanged(pos);
                binding.tvEstadoSubidaFotos.setText("Foto subida exitosamente.");
                validarEstadoBotonPublicar();
            }

            @Override
            public void onError(String mensajeError) {
                item.setSubiendo(false);
                listaEvidencias.remove(item);
                evidenciasAdapter.notifyDataSetChanged();
                binding.tvEstadoSubidaFotos.setText("No fue posible subir la imagen. Intente nuevamente.");
                Toast.makeText(requireContext(), mensajeError, Toast.LENGTH_SHORT).show();
                validarEstadoBotonPublicar();
            }
        });
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarEstadoBotonPublicar();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etDescripcion.addTextChangedListener(watcher);
        binding.etDireccionDetallada.addTextChangedListener(watcher);
        binding.cbAnonimo.setOnCheckedChangeListener((btn, checked) -> {
            actualizarReportadoPor();
            validarEstadoBotonPublicar();
        });

        binding.btnRegistrarAlerta.setOnClickListener(v -> procesarPublicarAlerta());
    }

    private void actualizarReportadoPor() {
        boolean esAnonimo = binding.cbAnonimo.isChecked();
        String fecha = new SimpleDateFormat("dd/MM/yyyy, h:mm a", Locale.getDefault()).format(new Date());
        if (esAnonimo) {
            binding.tvReportadoPor.setText("Reportado por: Anónimo — " + fecha);
        } else {
            String nombre = session.getNombre() != null ? session.getNombre() : "Usuario Ciudadano";
            binding.tvReportadoPor.setText("Reportado por: " + nombre + " — " + fecha);
        }
    }

    private void validarEstadoBotonPublicar() {
        if (binding == null) return;

        String desc = binding.etDescripcion.getText() != null ? binding.etDescripcion.getText().toString().trim() : "";
        String depto = binding.etDepartamento.getText() != null ? binding.etDepartamento.getText().toString().trim() : "";
        String mun = binding.etMunicipio.getText() != null ? binding.etMunicipio.getText().toString().trim() : "";
        String dir = binding.etDireccionDetallada.getText() != null ? binding.etDireccionDetallada.getText().toString().trim() : "";

        boolean descValida = desc.length() >= 10 && desc.length() <= 1000;
        boolean ubicacionValida = !depto.isEmpty() && !mun.isEmpty() && !dir.isEmpty();

        boolean subiendoFoto = false;
        for (EvidenciasSeleccionadasAdapter.EvidenciaItem item : listaEvidencias) {
            if (item.isSubiendo()) {
                subiendoFoto = true;
                break;
            }
        }

        binding.btnRegistrarAlerta.setEnabled(descValida && ubicacionValida && !subiendoFoto);
    }

    private void procesarPublicarAlerta() {
        String desc = binding.etDescripcion.getText().toString().trim();
        String depto = binding.etDepartamento.getText().toString().trim();
        String mun = binding.etMunicipio.getText().toString().trim();
        String dir = binding.etDireccionDetallada.getText().toString().trim();

        if (desc.length() < 10) {
            binding.tilDescripcion.setError("La descripción debe tener mínimo 10 caracteres");
            return;
        }
        binding.tilDescripcion.setError(null);

        if (depto.isEmpty()) {
            binding.tilDepartamento.setError("Seleccione departamento");
            return;
        }

        if (mun.isEmpty()) {
            binding.tilMunicipio.setError("Seleccione municipio");
            return;
        }

        if (dir.isEmpty()) {
            binding.tilDireccionDetallada.setError("Ingrese la dirección detallada o punto de referencia");
            return;
        }

        // Recopilar URLs de evidencias subidas
        List<String> evidenciasUrls = new ArrayList<>();
        for (EvidenciasSeleccionadasAdapter.EvidenciaItem item : listaEvidencias) {
            if (item.getUrlSubida() != null && !item.getUrlSubida().isEmpty()) {
                evidenciasUrls.add(item.getUrlSubida());
            }
        }

        // Construir DTO de ubicación conforme a OpenAPI
        UbicacionAlertaDto ubicacionDto = new UbicacionAlertaDto(depto, mun, dir);

        CrearAlertaRequest request = new CrearAlertaRequest();
        request.setDescripcion(desc);
        request.setUbicacion(ubicacionDto);
        request.setEvidenciasUrls(evidenciasUrls);
        request.setEsAnonimo(binding.cbAnonimo.isChecked());

        // Deshabilitar botón y procesar envío
        binding.btnRegistrarAlerta.setEnabled(false);
        viewModel.crearAlerta(request);
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            if (cargando != null) {
                binding.btnRegistrarAlerta.setEnabled(!cargando);
                binding.btnRegistrarAlerta.setText(cargando ? "Publicando alerta..." : "Publicar Alerta");
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            binding.btnRegistrarAlerta.setEnabled(true);
            if (msg != null && !msg.trim().isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getAlertaCreada().observe(getViewLifecycleOwner(), alerta -> {
            if (alerta != null) {
                Toast.makeText(requireContext(), "Alerta publicada exitosamente", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
