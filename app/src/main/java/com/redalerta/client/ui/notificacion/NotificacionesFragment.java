package com.redalerta.client.ui.notificacion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentNotificacionesBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.notificacion.NotificacionResponse;
import com.redalerta.client.ui.alert.DetalleAlertaFragment;
import com.redalerta.client.viewmodel.NotificacionViewModel;

import java.util.ArrayList;
import java.util.Locale;

public class NotificacionesFragment extends Fragment {

    private FragmentNotificacionesBinding binding;
    private NotificacionViewModel viewModel;
    private NotificacionAdapter adapter;
    private LinearLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificacionesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Usamos el ViewModel compartido a nivel de Activity para mantener sincronizado el badge del BottomNavigationView
        viewModel = new ViewModelProvider(requireActivity()).get(NotificacionViewModel.class);

        setupRecyclerView();
        setupSwipeRefresh();
        setupFilterChips();
        setupRetryButtons();
        observeViewModel();

        viewModel.cargarNotificaciones(true);
    }

    private void setupRecyclerView() {
        adapter = new NotificacionAdapter(new ArrayList<>(), this::mostrarDetalleNotificacion);
        layoutManager = new LinearLayoutManager(requireContext());
        binding.rvNotificaciones.setLayoutManager(layoutManager);
        binding.rvNotificaciones.setAdapter(adapter);

        setupItemTouchHelper();

        // Paginación al llegar al final de la lista
        binding.rvNotificaciones.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || layoutManager == null) return;

                int totalItems = layoutManager.getItemCount();
                int lastVisible = layoutManager.findLastVisibleItemPosition();

                if (totalItems > 0 && lastVisible >= totalItems - 3) {
                    viewModel.cargarSiguientePagina();
                }
            }
        });
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            private final Paint paintFondo = new Paint();
            private final RectF rectFondo = new RectF();

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || adapter == null) return;

                NotificacionResponse item = adapter.getItem(position);
                if (item == null || item.getId() == null) return;

                ejecutarEliminacionConDeshacer(position, item);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState,
                                    boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                float cornerRadius = 14f * getResources().getDisplayMetrics().density;

                if (dX != 0) {
                    paintFondo.setColor(ContextCompat.getColor(requireContext(), R.color.ra_red));

                    if (dX > 0) { // Deslizamiento a la derecha
                        rectFondo.set(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom());
                    } else { // Deslizamiento a la izquierda
                        rectFondo.set(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    }
                    c.drawRoundRect(rectFondo, cornerRadius, cornerRadius, paintFondo);

                    Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
                    if (icon != null) {
                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();

                        if (dX > 0) {
                            int iconLeft = itemView.getLeft() + iconMargin;
                            int iconRight = iconLeft + icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        } else {
                            int iconRight = itemView.getRight() - iconMargin;
                            int iconLeft = iconRight - icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        }
                        icon.setTint(ContextCompat.getColor(requireContext(), R.color.ra_white));
                        icon.draw(c);
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(binding.rvNotificaciones);
    }

    private void ejecutarEliminacionConDeshacer(int posicion, @NonNull NotificacionResponse item) {
        long notifId = item.getId();
        int posicionReal = viewModel.removerNotificacionLocalmente(notifId);
        int posFinal = posicionReal != -1 ? posicionReal : posicion;

        final boolean[] deshecho = {false};

        Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                R.string.notif_eliminada_exito,
                Snackbar.LENGTH_LONG
        );
        snackbar.setAction(R.string.notif_accion_deshacer, v -> {
            deshecho[0] = true;
            viewModel.restaurarNotificacionLocalmente(posFinal, item);
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (!deshecho[0]) {
                    viewModel.eliminarNotificacionEnBackend(notifId);
                }
            }
        });
        snackbar.show();
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshNotificaciones.setColorSchemeResources(R.color.ra_red);
        binding.swipeRefreshNotificaciones.setOnRefreshListener(() -> viewModel.cargarNotificaciones(true));
    }

    private void setupFilterChips() {
        Boolean soloNoLeidas = viewModel.getSoloNoLeidasFiltro().getValue();
        if (Boolean.TRUE.equals(soloNoLeidas)) {
            binding.chipNotifNoLeidas.setChecked(true);
        } else {
            binding.chipNotifTodas.setChecked(true);
        }

        binding.chipGroupFiltroNotificaciones.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean filtrarNoLeidas = checkedIds.contains(R.id.chipNotifNoLeidas);
            viewModel.cambiarFiltroSoloNoLeidas(filtrarNoLeidas);
        });
    }

    private void setupRetryButtons() {
        View.OnClickListener reload = v -> viewModel.cargarNotificaciones(true);
        binding.btnReintentarNotif.setOnClickListener(reload);
        binding.btnActualizarEmptyNotif.setOnClickListener(reload);

        // Acción "Marcar todas como leídas"
        binding.btnMarcarTodasLeidas.setOnClickListener(v -> ejecutarMarcarTodasComoLeidas());
    }

    private void ejecutarMarcarTodasComoLeidas() {
        Long sinLeer = viewModel.getConteoNoLeidas().getValue();
        if (sinLeer == null || sinLeer <= 0) return;

        binding.btnMarcarTodasLeidas.setEnabled(false);
        viewModel.marcarTodasComoLeidas(new NotificacionViewModel.OnOperacionSimpleCallback() {
            @Override
            public void onExito() {
                if (binding == null || !isAdded()) return;
                binding.btnMarcarTodasLeidas.setEnabled(false);
                binding.btnMarcarTodasLeidas.setVisibility(View.GONE);
                com.google.android.material.snackbar.Snackbar.make(
                        binding.getRoot(),
                        R.string.notif_todas_marcadas_exito,
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onError(@NonNull String mensajeError) {
                if (binding == null || !isAdded()) return;
                binding.btnMarcarTodasLeidas.setEnabled(true);
                com.redalerta.client.util.AlertaMensajesHelper.mostrarError(binding.getRoot(), mensajeError);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (binding == null || state == null) return;

            if (state == NotificacionViewModel.UiState.LOADING && !binding.swipeRefreshNotificaciones.isRefreshing()) {
                binding.layoutLoadingNotif.setVisibility(View.VISIBLE);
                binding.layoutEmptyNotif.setVisibility(View.GONE);
                binding.layoutErrorNotif.setVisibility(View.GONE);
            } else if (state == NotificacionViewModel.UiState.SUCCESS) {
                binding.layoutLoadingNotif.setVisibility(View.GONE);
                binding.swipeRefreshNotificaciones.setRefreshing(false);
                binding.layoutEmptyNotif.setVisibility(View.GONE);
                binding.layoutErrorNotif.setVisibility(View.GONE);
                binding.swipeRefreshNotificaciones.setVisibility(View.VISIBLE);
            } else if (state == NotificacionViewModel.UiState.EMPTY) {
                binding.layoutLoadingNotif.setVisibility(View.GONE);
                binding.swipeRefreshNotificaciones.setRefreshing(false);
                binding.swipeRefreshNotificaciones.setVisibility(View.GONE);
                binding.layoutErrorNotif.setVisibility(View.GONE);
                binding.layoutEmptyNotif.setVisibility(View.VISIBLE);

                boolean soloNoLeidas = Boolean.TRUE.equals(viewModel.getSoloNoLeidasFiltro().getValue());
                binding.tvEmptyTituloNotif.setText(
                        soloNoLeidas ? "No tienes notificaciones sin leer" : "No tienes notificaciones"
                );
                binding.tvEmptySubtituloNotif.setText(
                        soloNoLeidas
                                ? "Estás al día con todas las novedades de tus alertas comunitarias."
                                : "Aquí aparecerán las actualizaciones de estado y seguimiento de tus alertas comunitarias."
                );
            } else if (state == NotificacionViewModel.UiState.ERROR) {
                binding.layoutLoadingNotif.setVisibility(View.GONE);
                binding.swipeRefreshNotificaciones.setRefreshing(false);
                binding.swipeRefreshNotificaciones.setVisibility(View.GONE);
                binding.layoutEmptyNotif.setVisibility(View.GONE);
                binding.layoutErrorNotif.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getNotificaciones().observe(getViewLifecycleOwner(), lista -> {
            if (adapter != null) {
                adapter.actualizarLista(lista);
            }
        });

        viewModel.getConteoNoLeidas().observe(getViewLifecycleOwner(), conteo -> {
            if (binding == null) return;
            long total = conteo != null ? conteo : 0L;
            if (total > 0) {
                binding.tvResumenNoLeidas.setVisibility(View.VISIBLE);
                binding.tvResumenNoLeidas.setText(String.format(Locale.getDefault(), "%d sin leer", total));
                binding.btnMarcarTodasLeidas.setVisibility(View.VISIBLE);
                binding.btnMarcarTodasLeidas.setEnabled(true);
            } else {
                binding.tvResumenNoLeidas.setVisibility(View.GONE);
                binding.btnMarcarTodasLeidas.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (binding != null && err != null && !err.trim().isEmpty()) {
                binding.tvErrorMensajeNotif.setText(err);
            }
        });
    }

    private void mostrarDetalleNotificacion(@NonNull NotificacionResponse notificacion) {
        if (!isAdded()) return;

        DetalleNotificacionBottomSheet bottomSheet = DetalleNotificacionBottomSheet.newInstance(notificacion);
        bottomSheet.setOnVerAlertaListener(this::abrirAlertaAsociada);
        bottomSheet.setOnEliminarNotificacionListener(this::eliminarDesdeDetalle);
        bottomSheet.show(getChildFragmentManager(), DetalleNotificacionBottomSheet.TAG);
    }

    private void eliminarDesdeDetalle(@NonNull NotificacionResponse item) {
        if (item.getId() == null) return;
        ejecutarEliminacionConDeshacer(0, item);
    }

    private void abrirAlertaAsociada(@NonNull NotificacionResponse notificacion) {
        if (notificacion.getAlertaId() == null) return;

        Alerta alertaMinima = new Alerta();
        alertaMinima.setId(String.valueOf(notificacion.getAlertaId()));
        alertaMinima.setTitulo(notificacion.getTitulo());
        alertaMinima.setDescripcion(notificacion.getMensaje());
        alertaMinima.setEstado(notificacion.getNuevoEstado());

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, DetalleAlertaFragment.newInstance(alertaMinima))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
