package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentMisPublicacionesBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.viewmodel.AlertaViewModel;

import java.util.ArrayList;
import java.util.List;

public class MisPublicacionesFragment extends Fragment {

    private FragmentMisPublicacionesBinding binding;
    private AlertaViewModel viewModel;
    private MisPublicacionesAdapter adapter;
    private List<Alerta> listaCompletaMisAlertas = new ArrayList<>();
    private String estadoFiltroActual = null;

    private final Handler fabHandler = new Handler(Looper.getMainLooper());
    private final Runnable expandFabRunnable = () -> {
        if (binding != null && binding.fabReportar != null && !binding.fabReportar.isExtended()) {
            binding.fabReportar.extend();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMisPublicacionesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AlertaViewModel.class);

        setupRecyclerView();
        setupSwipeRefresh();
        setupFabScrollBehavior();
        setupEstadoFilterChips();
        setupErrorRetry();
        observeViewModel();

        // Consulta del endpoint GET /api/v1/alertas/mis-alertas
        viewModel.cargarMisAlertas();
    }

    private void setupRecyclerView() {
        adapter = new MisPublicacionesAdapter(new ArrayList<>(), alerta -> {
            Toast.makeText(requireContext(),
                    "Detalle / Seguimiento de Alerta: ALR-" + alerta.getId(),
                    Toast.LENGTH_SHORT).show();
        });

        binding.rvMisPublicaciones.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMisPublicaciones.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshMisPublicaciones.setColorSchemeResources(R.color.ra_red);
        binding.swipeRefreshMisPublicaciones.setOnRefreshListener(() -> viewModel.cargarMisAlertas());
    }

    private void setupFabScrollBehavior() {
        binding.rvMisPublicaciones.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    fabHandler.removeCallbacks(expandFabRunnable);
                    fabHandler.postDelayed(expandFabRunnable, 500);
                } else {
                    fabHandler.removeCallbacks(expandFabRunnable);
                    if (binding.fabReportar.isExtended()) {
                        binding.fabReportar.shrink();
                    }
                }
            }
        });

        View.OnClickListener abrirReporteListener = v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new RegistroAlertaFragment())
                    .addToBackStack(null)
                    .commit();
        };

        binding.fabReportar.setOnClickListener(abrirReporteListener);
        binding.btnCrearPrimerReporte.setOnClickListener(abrirReporteListener);
    }

    private void setupEstadoFilterChips() {
        binding.chipGroupMisPublicaciones.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipMisRevision)) {
                estadoFiltroActual = "PENDIENTE_REVISION";
            } else if (checkedIds.contains(R.id.chipMisGestion)) {
                estadoFiltroActual = "EN_GESTION";
            } else if (checkedIds.contains(R.id.chipMisResueltas)) {
                estadoFiltroActual = "RESUELTA";
            } else {
                estadoFiltroActual = null;
            }
            aplicarFiltroEnMemoria();
        });
    }

    private void aplicarFiltroEnMemoria() {
        if (listaCompletaMisAlertas == null || listaCompletaMisAlertas.isEmpty()) {
            mostrarEstadoVacio();
            return;
        }

        List<Alerta> filtradas = new ArrayList<>();
        if (estadoFiltroActual == null) {
            filtradas.addAll(listaCompletaMisAlertas);
        } else {
            for (Alerta a : listaCompletaMisAlertas) {
                if (a.getEstado() != null && (a.getEstado().equalsIgnoreCase(estadoFiltroActual)
                        || (estadoFiltroActual.equalsIgnoreCase("EN_GESTION") && a.getEstado().equalsIgnoreCase("DIRECCIONADA")))) {
                    filtradas.add(a);
                }
            }
        }

        if (filtradas.isEmpty()) {
            mostrarEstadoVacio();
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.layoutError.setVisibility(View.GONE);
            binding.swipeRefreshMisPublicaciones.setVisibility(View.VISIBLE);

            adapter = new MisPublicacionesAdapter(filtradas, alerta -> {
                Toast.makeText(requireContext(),
                        "Detalle / Seguimiento: ALR-" + alerta.getId(),
                        Toast.LENGTH_SHORT).show();
            });
            binding.rvMisPublicaciones.setAdapter(adapter);
        }
    }

    private void mostrarEstadoVacio() {
        binding.layoutEmpty.setVisibility(View.VISIBLE);
        binding.swipeRefreshMisPublicaciones.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.GONE);
    }

    private void setupErrorRetry() {
        View.OnClickListener actualizarListener = v -> viewModel.cargarMisAlertas();
        binding.btnReintentar.setOnClickListener(actualizarListener);
        binding.btnActualizarEmpty.setOnClickListener(actualizarListener);
    }

    private void observeViewModel() {
        // Observar estado de carga (Garantiza ocultar el loader al recibir respuesta)
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            if (cargando != null) {
                if (cargando && !binding.swipeRefreshMisPublicaciones.isRefreshing()) {
                    binding.layoutLoading.setVisibility(View.VISIBLE);
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.layoutError.setVisibility(View.GONE);
                } else if (!cargando) {
                    binding.layoutLoading.setVisibility(View.GONE);
                    binding.swipeRefreshMisPublicaciones.setRefreshing(false);
                }
            }
        });

        // Observar errores
        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                binding.layoutError.setVisibility(View.VISIBLE);
                binding.tvErrorMensaje.setText(errorMsg);
                binding.swipeRefreshMisPublicaciones.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                binding.layoutError.setVisibility(View.GONE);
            }
        });

        // Observar lista de mis alertas
        viewModel.getMisAlertas().observe(getViewLifecycleOwner(), alertas -> {
            listaCompletaMisAlertas = alertas != null ? alertas : new ArrayList<>();
            aplicarFiltroEnMemoria();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fabHandler.removeCallbacks(expandFabRunnable);
        binding = null;
    }
}
