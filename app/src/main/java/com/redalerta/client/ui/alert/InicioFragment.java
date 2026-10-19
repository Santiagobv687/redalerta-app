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
import com.redalerta.client.model.common.DepartamentoResponse;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.util.UbicacionCatalogoHelper;
import com.redalerta.client.viewmodel.AlertaViewModel;

import java.util.ArrayList;
import java.util.List;

public class InicioFragment extends Fragment {

    private FragmentInicioFeedBinding binding;
    private AlertaViewModel viewModel;
    private AlertaAdapter adapter;
    private SessionManager session;

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

        session = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(AlertaViewModel.class);

        String rol = session.getRol();
        boolean esLider = "ROLE_LIDER".equalsIgnoreCase(rol) || "LIDER".equalsIgnoreCase(rol) || "ROLE_ADMIN".equalsIgnoreCase(rol) || "ADMINISTRADOR".equalsIgnoreCase(rol);

        if (esLider) {
            String munLider = session.getMunicipio() != null ? session.getMunicipio() : "Armenia";
            viewModel.setMunicipioFiltro(munLider);
        } else {
            viewModel.setMunicipioFiltro("TODO");
        }

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
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, DetalleAlertaFragment.newInstance(alerta))
                    .addToBackStack(null)
                    .commit();
        });

        binding.rvFeedAlertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFeedAlertas.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.ra_red);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.cargarTodasLasAlertas());
    }

    private void setupFabScrollBehavior() {
        String rol = new com.redalerta.client.util.SessionManager(requireContext()).getRol();
        boolean esLider = "ROLE_LIDER".equalsIgnoreCase(rol) || "LIDER".equalsIgnoreCase(rol) || "ROLE_ADMIN".equalsIgnoreCase(rol) || "ADMINISTRADOR".equalsIgnoreCase(rol);

        if (esLider) {
            binding.fabReportarAlerta.setVisibility(View.GONE);
            return;
        }

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
        String rol = session.getRol();
        boolean esLider = "ROLE_LIDER".equalsIgnoreCase(rol) || "LIDER".equalsIgnoreCase(rol) || "ROLE_ADMIN".equalsIgnoreCase(rol) || "ADMINISTRADOR".equalsIgnoreCase(rol);

        if (esLider) {
            String munLider = session.getMunicipio() != null ? session.getMunicipio() : "Armenia";
            binding.btnFiltroTerritorio.setText(munLider);
            binding.tvMunicipio.setText(getString(R.string.filtro_municipio_puntual, munLider));

            binding.btnFiltroTerritorio.setOnClickListener(v -> {
                Toast.makeText(requireContext(),
                        "Como Líder Comunitario, tu territorio está asignado fijamente en " + munLider,
                        Toast.LENGTH_SHORT).show();
            });
            return;
        }

        binding.btnFiltroTerritorio.setOnClickListener(v -> {
            UbicacionCatalogoHelper.cargarCatalogo(requireContext(), catalogo -> {
                if (binding == null) return;

                PopupMenu popup = new PopupMenu(requireContext(), binding.btnFiltroTerritorio);
                popup.setForceShowIcon(true);

                String filtroActual = viewModel.getMunicipioFiltro().getValue();
                if (filtroActual == null || filtroActual.trim().isEmpty()) {
                    filtroActual = "TODO";
                }

                // Opción 1: Todo el departamento
                MenuItem itemTodo = popup.getMenu().add(0, 0, 0, "Todo el departamento");
                if (filtroActual.equalsIgnoreCase("TODO")) {
                    itemTodo.setIcon(R.drawable.ic_check);
                }

                // Obtener municipios de Quindío (o primer departamento del catálogo)
                List<String> municipios = new ArrayList<>();
                for (DepartamentoResponse d : catalogo) {
                    if (d.getDepartamento() != null && d.getDepartamento().equalsIgnoreCase("Quindío")) {
                        if (d.getMunicipios() != null) municipios.addAll(d.getMunicipios());
                        break;
                    }
                }
                if (municipios.isEmpty() && !catalogo.isEmpty() && catalogo.get(0).getMunicipios() != null) {
                    municipios.addAll(catalogo.get(0).getMunicipios());
                }

                // Agregar todos los municipios del catálogo
                int order = 1;
                for (String munNombre : municipios) {
                    MenuItem mItem = popup.getMenu().add(0, order, order, munNombre);
                    if (filtroActual.equalsIgnoreCase(munNombre.trim())) {
                        mItem.setIcon(R.drawable.ic_check);
                    }
                    order++;
                }

                popup.setOnMenuItemClickListener(item -> {
                    CharSequence title = item.getTitle();
                    if (title != null) {
                        String seleccion = title.toString().trim();
                        if (seleccion.equalsIgnoreCase("Todo el departamento")) {
                            binding.tvMunicipio.setText(getString(R.string.filtro_todos_municipios));
                            binding.btnFiltroTerritorio.setText("Todo el Dpto");
                            viewModel.setMunicipioFiltro("TODO");
                        } else {
                            binding.tvMunicipio.setText(getString(R.string.filtro_municipio_puntual, seleccion));
                            binding.btnFiltroTerritorio.setText(seleccion);
                            viewModel.setMunicipioFiltro(seleccion);
                        }
                    }
                    return true;
                });
                popup.show();
            });
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
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, DetalleAlertaFragment.newInstance(alerta))
                            .addToBackStack(null)
                            .commit();
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
