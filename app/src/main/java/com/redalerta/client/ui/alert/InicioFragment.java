package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentInicioFeedBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.viewmodel.AlertaViewModel;

import java.util.ArrayList;
import java.util.List;

public class InicioFragment extends Fragment {

    private FragmentInicioFeedBinding binding;
    private AlertaViewModel viewModel;
    private AlertaAdapter adapter;

    private final Handler fabHandler = new Handler(Looper.getMainLooper());
    private final Runnable expandFabRunnable = () -> {
        if (binding != null && binding.fabReportarAlerta != null && !binding.fabReportarAlerta.isExtended()) {
            binding.fabReportarAlerta.extend();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentInicioFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AlertaViewModel.class);

        setupRecyclerView();
        setupSwipeRefresh();
        setupFabScrollBehavior();
        setupTerritorioFilter();
        setupEstadoFilter();
        setupErrorRetry();
        observeViewModel();

        // Cargar alertas iniciales
        viewModel.cargarTodasLasAlertas();
    }

    private void setupRecyclerView() {
        adapter = new AlertaAdapter(new ArrayList<>(), alerta -> {
            // Acción al tocar una tarjeta de alerta
            Toast.makeText(requireContext(),
                    "Alerta: " + alerta.getTitulo() + "\nUbicación: " + alerta.getDireccion(),
                    Toast.LENGTH_SHORT).show();
        });

        binding.rvFeedAlertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFeedAlertas.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.ra_red);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.cargarTodasLasAlertas());
    }

    private void setupFabScrollBehavior() {
        // Se contrae al desplazarse la lista y espera 500ms (medio segundo) quieto
        // antes de volver a desplegar la etiqueta "Reportar Alerta".
        binding.rvFeedAlertas.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    fabHandler.removeCallbacks(expandFabRunnable);
                    fabHandler.postDelayed(expandFabRunnable, 500);
                } else {
                    fabHandler.removeCallbacks(expandFabRunnable);
                    if (binding.fabReportarAlerta.isExtended()) {
                        binding.fabReportarAlerta.shrink();
                    }
                }
            }
        });

        binding.fabReportarAlerta.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new RegistroAlertaFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupTerritorioFilter() {
        binding.btnFiltroTerritorio.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), binding.btnFiltroTerritorio);
            popup.setForceShowIcon(true);

            String filtroActual = viewModel.getMunicipioFiltro().getValue();
            if (filtroActual == null) filtroActual = "Armenia";

            MenuItem m1 = popup.getMenu().add(0, 1, 0, "Armenia");
            if (filtroActual.equalsIgnoreCase("Armenia")) {
                m1.setIcon(R.drawable.ic_check);
            }

            MenuItem m2 = popup.getMenu().add(0, 2, 1, "Calarcá");
            if (filtroActual.equalsIgnoreCase("Calarcá")) {
                m2.setIcon(R.drawable.ic_check);
            }

            MenuItem m3 = popup.getMenu().add(0, 3, 2, "Montenegro");
            if (filtroActual.equalsIgnoreCase("Montenegro")) {
                m3.setIcon(R.drawable.ic_check);
            }

            MenuItem m4 = popup.getMenu().add(0, 4, 3, "Todo el departamento");
            if (filtroActual.equalsIgnoreCase("TODO")) {
                m4.setIcon(R.drawable.ic_check);
            }

            popup.setOnMenuItemClickListener(item -> {
                CharSequence title = item.getTitle();
                if (title != null) {
                    String seleccion = title.toString().trim();
                    if (seleccion.equalsIgnoreCase("Armenia")) {
                        binding.tvMunicipio.setText(getString(R.string.filtro_municipio_puntual, "Armenia"));
                        binding.btnFiltroTerritorio.setText("Armenia");
                        viewModel.setMunicipioFiltro("Armenia");
                    } else if (seleccion.equalsIgnoreCase("Calarcá")) {
                        binding.tvMunicipio.setText(getString(R.string.filtro_municipio_puntual, "Calarcá"));
                        binding.btnFiltroTerritorio.setText("Calarcá");
                        viewModel.setMunicipioFiltro("Calarcá");
                    } else if (seleccion.equalsIgnoreCase("Montenegro")) {
                        binding.tvMunicipio.setText(getString(R.string.filtro_municipio_puntual, "Montenegro"));
                        binding.btnFiltroTerritorio.setText("Montenegro");
                        viewModel.setMunicipioFiltro("Montenegro");
                    } else {
                        binding.tvMunicipio.setText(getString(R.string.filtro_todos_municipios));
                        binding.btnFiltroTerritorio.setText("Todo el Dpto");
                        viewModel.setMunicipioFiltro("TODO");
                    }
                }
                return true;
            });
            popup.show();
        });
    }

    private void setupEstadoFilter() {
        binding.chipGroupEstado.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipEstadoRevision)) {
                viewModel.setEstadoFiltro("PENDIENTE_REVISION");
            } else if (checkedIds.contains(R.id.chipEstadoGestion)) {
                viewModel.setEstadoFiltro("EN_GESTION");
            } else if (checkedIds.contains(R.id.chipEstadoDireccionada)) {
                viewModel.setEstadoFiltro("DIRECCIONADA");
            } else if (checkedIds.contains(R.id.chipEstadoResuelta)) {
                viewModel.setEstadoFiltro("RESUELTA");
            } else {
                viewModel.setEstadoFiltro(null);
            }
        });
    }

    private void setupErrorRetry() {
        binding.btnReintentar.setOnClickListener(v -> viewModel.cargarTodasLasAlertas());
    }

    private void observeViewModel() {
        // Observar estado de carga
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            if (cargando != null) {
                if (cargando && !binding.swipeRefresh.isRefreshing()) {
                    binding.layoutLoading.setVisibility(View.VISIBLE);
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.layoutError.setVisibility(View.GONE);
                } else if (!cargando) {
                    binding.layoutLoading.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                }
            }
        });

        // Observar errores
        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                binding.layoutError.setVisibility(View.VISIBLE);
                binding.tvErrorMensaje.setText(errorMsg);
                binding.swipeRefresh.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                binding.layoutError.setVisibility(View.GONE);
            }
        });

        // Observar lista de alertas
        viewModel.getTodasLasAlertas().observe(getViewLifecycleOwner(), alertas -> {
            if (alertas == null || alertas.isEmpty()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.swipeRefresh.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.GONE);
            } else {
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.GONE);
                binding.swipeRefresh.setVisibility(View.VISIBLE);

                adapter = new AlertaAdapter(alertas, alerta -> {
                    Toast.makeText(requireContext(),
                            "Alerta: " + alerta.getTitulo() + "\nUbicación: " + alerta.getDireccion(),
                            Toast.LENGTH_SHORT).show();
                });
                binding.rvFeedAlertas.setAdapter(adapter);
            }
        });

        // Observar municipio de filtro
        viewModel.getMunicipioFiltro().observe(getViewLifecycleOwner(), mun -> {
            if (mun != null) {
                if (mun.equalsIgnoreCase("TODO")) {
                    binding.tvMunicipio.setText(getString(R.string.filtro_todos_municipios));
                    binding.btnFiltroTerritorio.setText("Todo el Dpto");
                } else {
                    binding.tvMunicipio.setText(getString(R.string.filtro_municipio_puntual, mun));
                    binding.btnFiltroTerritorio.setText(mun);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fabHandler.removeCallbacks(expandFabRunnable);
        binding = null;
    }
}
