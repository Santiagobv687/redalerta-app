package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentDetalleAlertaBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.EvidenciaResponse;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.util.TimeUtils;
import com.redalerta.client.viewmodel.AlertaLiderViewModel;

public class DetalleAlertaFragment extends Fragment {

    public static final String ARG_ALERTA = "arg_alerta";

    private FragmentDetalleAlertaBinding binding;
    private Alerta alerta;
    private SessionManager sessionManager;
    private AlertaLiderViewModel viewModel;

    public static DetalleAlertaFragment newInstance(Alerta alerta) {
        DetalleAlertaFragment fragment = new DetalleAlertaFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALERTA, alerta);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDetalleAlertaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(AlertaLiderViewModel.class);

        if (getArguments() != null) {
            alerta = (Alerta) getArguments().getSerializable(ARG_ALERTA);
        }

        binding.toolbarDetalle.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        poblarDatos();
        configurarAccionesPorRol();
        observeViewModel();
    }

    private void poblarDatos() {
        if (alerta == null) return;

        String fecha = alerta.getFechaCreacion() != null ? alerta.getFechaCreacion() : alerta.getFechaActualizacion();
        binding.tvFechaAlerta.setText(TimeUtils.obtenerTiempoTranscurrido(fecha));

        configurarEstadoAccesible(alerta.getEstado());

        binding.tvTituloDetalle.setText(alerta.getTitulo());
        binding.tvDescripcionDetalle.setText(alerta.getDescripcion());

        String mun = alerta.getBarrio() != null ? alerta.getBarrio() : "Armenia";
        String dir = alerta.getDireccion() != null ? alerta.getDireccion() : "";
        binding.tvUbicacionDetalle.setText(!dir.isEmpty() ? mun + " • " + dir : mun);

        String autor = alerta.isEsAnonima() ? "Anónimo" : alerta.getUsuarioNombre();
        if (autor == null || autor.isEmpty()) autor = "Ciudadano";
        binding.tvAutorDetalle.setText(autor);

        // Evidencias
        binding.layoutEvidencias.removeAllViews();
        boolean tieneFotos = false;
        if (alerta.getEvidenciaUrl() != null && !alerta.getEvidenciaUrl().isEmpty()) {
            agregarFoto(alerta.getEvidenciaUrl());
            tieneFotos = true;
        }
        if (alerta.getEvidencias() != null && !alerta.getEvidencias().isEmpty()) {
            for (EvidenciaResponse e : alerta.getEvidencias()) {
                if (e.getUrl() != null && !e.getUrl().equals(alerta.getEvidenciaUrl())) {
                    agregarFoto(e.getUrl());
                    tieneFotos = true;
                }
            }
        }

        if (tieneFotos) {
            binding.tvEvidenciasLabel.setVisibility(View.VISIBLE);
            binding.scrollEvidencias.setVisibility(View.VISIBLE);
        } else {
            binding.tvEvidenciasLabel.setVisibility(View.GONE);
            binding.scrollEvidencias.setVisibility(View.GONE);
        }
    }

    private void agregarFoto(String url) {
        ImageView img = new ImageView(requireContext());
        int size = (int) (120 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd(margin);
        img.setLayoutParams(params);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setClipToOutline(true);
        img.setBackgroundResource(R.drawable.bg_status_revision);

        Glide.with(this).load(url).into(img);
        binding.layoutEvidencias.addView(img);
    }

    private void configurarEstadoAccesible(String estado) {
        String estadoNorm = estado != null ? estado.toUpperCase() : "PENDIENTE_REVISION";
        switch (estadoNorm) {
            case "RESUELTA" -> {
                binding.tvEstadoBadgeDetalle.setText("RESUELTA");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_resuelta);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_status_resuelta_text));
            }
            case "DIRECCIONADA", "EN_GESTION", "EN_ATENCION" -> {
                binding.tvEstadoBadgeDetalle.setText("EN GESTIÓN");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_atencion);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_status_atencion_text));
            }
            case "RECHAZADA" -> {
                binding.tvEstadoBadgeDetalle.setText("RECHAZADA");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_pendiente);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_status_pendiente_text));
            }
            default -> {
                binding.tvEstadoBadgeDetalle.setText("EN REVISIÓN");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_revision);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_status_revision_text));
            }
        }
    }

    private void configurarAccionesPorRol() {
        if (alerta == null) return;
        String rol = sessionManager.getRol();
        String estado = alerta.getEstado() != null ? alerta.getEstado().toUpperCase() : "";

        if ("ROLE_LIDER".equalsIgnoreCase(rol) || "ROLE_ADMIN".equalsIgnoreCase(rol) || "LIDER".equalsIgnoreCase(rol) || "ADMINISTRADOR".equalsIgnoreCase(rol)) {
            binding.layoutAccionesLider.setVisibility(View.VISIBLE);

            if (estado.equals("PENDIENTE_REVISION") || estado.equals("RECHAZADA")) {
                binding.btnEvaluarDireccionar.setVisibility(View.VISIBLE);
                binding.btnModificarDireccionamiento.setVisibility(View.GONE);
                binding.btnCancelarDireccionamiento.setVisibility(View.GONE);

                binding.btnEvaluarDireccionar.setOnClickListener(v -> mostrarBottomSheet(false));
            } else if (estado.equals("DIRECCIONADA") || estado.equals("EN_GESTION")) {
                binding.btnEvaluarDireccionar.setVisibility(View.GONE);
                binding.btnModificarDireccionamiento.setVisibility(View.VISIBLE);
                binding.btnCancelarDireccionamiento.setVisibility(View.VISIBLE);

                binding.btnModificarDireccionamiento.setOnClickListener(v -> mostrarBottomSheet(true));
                binding.btnCancelarDireccionamiento.setOnClickListener(v -> solicitarMotivoCancelacion());
            } else {
                binding.layoutAccionesLider.setVisibility(View.GONE);
            }
        } else {
            binding.layoutAccionesLider.setVisibility(View.GONE);
        }
    }

    private void mostrarBottomSheet(boolean esModificacion) {
        DireccionarAlertaBottomSheet bs = DireccionarAlertaBottomSheet.newInstance(alerta, esModificacion);
        bs.show(getParentFragmentManager(), "BottomSheetTriaje");
    }

    private void solicitarMotivoCancelacion() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Explique por qué cancela el direccionamiento");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar Direccionamiento")
                .setMessage("Esta acción devolverá la alerta a estado PENDIENTE DE REVISIÓN y retirará la asignación a la entidad. Debe ingresar un motivo justificado.")
                .setView(input)
                .setPositiveButton("Confirmar y Cancelar", (dialog, which) -> {
                    String motivo = input.getText().toString().trim();
                    if (motivo.length() < 5) {
                        Toast.makeText(requireContext(), "Motivo muy corto", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        long id = Long.parseLong(alerta.getId());
                        viewModel.cancelarDireccionamiento(id, motivo);
                    } catch (Exception e) {}
                })
                .setNegativeButton("Volver", null)
                .show();
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == AlertaLiderViewModel.UiState.LOADING) {
                // Show loading indicator if needed
            } else if (state == AlertaLiderViewModel.UiState.SUCCESS) {
                // Done
            }
        });

        viewModel.getAlertaDireccionada().observe(getViewLifecycleOwner(), al -> {
            if (al != null) {
                this.alerta = al;
                poblarDatos();
                configurarAccionesPorRol();
                Toast.makeText(requireContext(), "Estado de la alerta actualizado con éxito", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
