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

import com.google.android.material.snackbar.Snackbar;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentInicioFeedBinding;
import com.redalerta.client.model.common.DepartamentoResponse;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.util.UbicacionCatalogoHelper;
import com.redalerta.client.viewmodel.AlertaViewModel;
import com.redalerta.client.viewmodel.FeedUiState;

import java.util.ArrayList;
import java.util.List;

public class InicioFragment extends Fragment {

    private FragmentInicioFeedBinding binding;
    private AlertaViewModel viewModel;
    private AlertaAdapter adapter;
    private SessionManager session;
    private LinearLayoutManager layoutManager;

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
        boolean esLider = "ROLE_LIDER".equalsIgnoreCase(rol)
                || "LIDER".equalsIgnoreCase(rol)
                || "ROLE_ADMIN".equalsIgnoreCase(rol)
                || "ADMINISTRADOR".equalsIgnoreCase(rol);

        if (esLider) {
            String munLider = session.getMunicipio() != null ? session.getMunicipio() : "Armenia";
            viewModel.inicializarMunicipioFiltroPorDefecto(munLider);
        } else if (viewModel.getMunicipioFiltro().getValue() == null) {
            viewModel.inicializarMunicipioFiltroPorDefecto("TODO");
        }

        setupRecyclerView();
        setupSwipeRefresh();
        setupFabScrollBehavior();
        setupTerritorioFilter();
        setupEstadoFilter();
        setupErrorRetry();
        observeViewModel();

        // Cargar el feed respetando la caché en memoria (forzarRecarga = false)
        viewModel.cargarFeed(false);
    }

    private void setupRecyclerView() {
        adapter = new AlertaAdapter(alerta -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, DetalleAlertaFragment.newInstance(alerta))
                    .addToBackStack(null)
                    .commit();
        });

        layoutManager = new LinearLayoutManager(requireContext());
        binding.rvFeedAlertas.setLayoutManager(layoutManager);
        binding.rvFeedAlertas.setHasFixedSize(true);
        binding.rvFeedAlertas.setItemAnimator(null);
        binding.rvFeedAlertas.setAdapter(adapter);

        // Scroll infinito por cursor (Keyset Pagination)
        binding.rvFeedAlertas.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || layoutManager == null) return;

                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();

                if (totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 3) {
                    viewModel.cargarSiguientePaginaFeed();
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.ra_red);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.cargarFeed(true));
    }

    private void setupFabScrollBehavior() {
        String rol = session.getRol();
        boolean esCiudadano = "ROLE_CIUDADANO".equalsIgnoreCase(rol) || "CIUDADANO".equalsIgnoreCase(rol) || rol == null;

        if (!esCiudadano) {
            binding.fabReportarAlerta.setVisibility(View.GONE);
            return;
        }

        binding.fabReportarAlerta.setVisibility(View.VISIBLE);

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
        boolean esLider = "ROLE_LIDER".equalsIgnoreCase(rol)
                || "LIDER".equalsIgnoreCase(rol)
                || "ROLE_ADMIN".equalsIgnoreCase(rol)
                || "ADMINISTRADOR".equalsIgnoreCase(rol);

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
        // Sincronizar el chip seleccionado con el filtro activo en el ViewModel antes de adjuntar el listener
        String estadoActivo = viewModel.getEstadoFiltro().getValue();
        if ("PENDIENTE_REVISION".equals(estadoActivo)) {
            binding.chipGroupEstado.check(R.id.chipEstadoRevision);
        } else if ("EN_GESTION".equals(estadoActivo)) {
            binding.chipGroupEstado.check(R.id.chipEstadoGestion);
        } else if ("DIRECCIONADA".equals(estadoActivo)) {
            binding.chipGroupEstado.check(R.id.chipEstadoDireccionada);
        } else if ("RESUELTA".equals(estadoActivo)) {
            binding.chipGroupEstado.check(R.id.chipEstadoResuelta);
        } else {
            binding.chipGroupEstado.check(R.id.chipEstadoTodos);
        }

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
        binding.btnReintentar.setOnClickListener(v -> viewModel.cargarFeed(true));
    }

    private void observeViewModel() {
        // Observar estado cerrado FeedUiState (Loading con Shimmer / Success con DiffUtil / Empty / Error)
        viewModel.getFeedUiState().observe(getViewLifecycleOwner(), state -> {
            if (binding == null || state == null) return;

            if (state instanceof FeedUiState.Loading) {
                binding.layoutLoading.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.GONE);
                binding.rvFeedAlertas.setVisibility(View.GONE);

                binding.shimmerViewContainer.setVisibility(View.VISIBLE);
                binding.shimmerViewContainer.startShimmer();

            } else if (state instanceof FeedUiState.Success successState) {
                binding.shimmerViewContainer.stopShimmer();
                binding.shimmerViewContainer.setVisibility(View.GONE);
                binding.layoutLoading.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);

                binding.layoutEmpty.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.GONE);
                binding.swipeRefresh.setVisibility(View.VISIBLE);
                binding.rvFeedAlertas.setVisibility(View.VISIBLE);

                adapter.submitList(successState.alertas());

            } else if (state instanceof FeedUiState.Empty) {
                binding.shimmerViewContainer.stopShimmer();
                binding.shimmerViewContainer.setVisibility(View.GONE);
                binding.layoutLoading.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);

                binding.rvFeedAlertas.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);

            } else if (state instanceof FeedUiState.Error errorState) {
                binding.shimmerViewContainer.stopShimmer();
                binding.shimmerViewContainer.setVisibility(View.GONE);
                binding.layoutLoading.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);

                if (adapter != null && adapter.getItemCount() > 0) {
                    // Si ya hay alertas retenidas en memoria, conservarlas en pantalla y mostrar aviso discreto
                    binding.swipeRefresh.setVisibility(View.VISIBLE);
                    binding.rvFeedAlertas.setVisibility(View.VISIBLE);
                    binding.layoutError.setVisibility(View.GONE);
                    Snackbar.make(binding.getRoot(), errorState.mensaje(), Snackbar.LENGTH_LONG).show();
                } else {
                    binding.rvFeedAlertas.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.layoutError.setVisibility(View.VISIBLE);
                    binding.tvErrorMensaje.setText(errorState.mensaje());
                }
            }
        });

        // Observar municipio de filtro
        viewModel.getMunicipioFiltro().observe(getViewLifecycleOwner(), mun -> {
            if (binding != null && mun != null) {
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
    public void onPause() {
        if (binding != null && binding.shimmerViewContainer.isShimmerStarted()) {
            binding.shimmerViewContainer.stopShimmer();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && binding.shimmerViewContainer.getVisibility() == View.VISIBLE) {
            binding.shimmerViewContainer.startShimmer();
        }
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.shimmerViewContainer.stopShimmer();
        }
        super.onDestroyView();
        fabHandler.removeCallbacks(expandFabRunnable);
        binding = null;
    }
}
