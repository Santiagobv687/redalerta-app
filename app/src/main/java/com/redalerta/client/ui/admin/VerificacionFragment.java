package com.redalerta.client.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentVerificacionBinding;
import com.redalerta.client.model.Alerta;
import com.redalerta.client.viewmodel.AlertaViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VerificacionFragment extends Fragment {

    private FragmentVerificacionBinding binding;
    private AlertaViewModel viewModel;
    private VerificacionAdapter adapter;
    private List<Alerta> listaCompleta = new ArrayList<>();
    private String filtroActual = "TODAS";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVerificacionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AlertaViewModel.class);

        binding.rvVerificacion.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VerificacionAdapter(new ArrayList<>(), alerta -> {
            VerificacionBottomSheet bottomSheet = VerificacionBottomSheet.newInstance(alerta);
            bottomSheet.show(getParentFragmentManager(), "VerificacionBottomSheet");
        });
        binding.rvVerificacion.setAdapter(adapter);

        // Observar lista
        viewModel.getTodasLasAlertas().observe(getViewLifecycleOwner(), alertas -> {
            this.listaCompleta = alertas != null ? alertas : new ArrayList<>();
            actualizarContadores();
            aplicarFiltro(filtroActual);
        });

        // Configurar filtros
        binding.filterTodos.setOnClickListener(v -> aplicarFiltro("TODAS"));
        binding.filterPendientes.setOnClickListener(v -> aplicarFiltro("PENDIENTES"));
        binding.filterVerificadas.setOnClickListener(v -> aplicarFiltro("VERIFICADAS"));
        binding.filterCerradas.setOnClickListener(v -> aplicarFiltro("CERRADAS"));

        // Configurar recarga al volver del bottom sheet
        getParentFragmentManager().setFragmentResultListener("verificacion_result", getViewLifecycleOwner(), (key, bundle) -> {
            viewModel.cargarTodasLasAlertas(); // Recargar datos
        });

        viewModel.cargarTodasLasAlertas();
    }

    private void aplicarFiltro(String filtro) {
        this.filtroActual = filtro;

        // Usar ContextCompat.getColor para evitar la deprecación
        int colorSelected = ContextCompat.getColor(requireContext(), R.color.ra_white);
        int colorUnselected = ContextCompat.getColor(requireContext(), R.color.ra_filter_text_unselected);

        // Actualizar UI de los botones de filtro
        binding.filterTodos.setBackgroundResource(filtro.equals("TODAS") ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected);
        binding.filterTodos.setTextColor(filtro.equals("TODAS") ? colorSelected : colorUnselected);

        binding.filterPendientes.setBackgroundResource(filtro.equals("PENDIENTES") ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected);
        binding.filterPendientes.setTextColor(filtro.equals("PENDIENTES") ? colorSelected : colorUnselected);

        binding.filterVerificadas.setBackgroundResource(filtro.equals("VERIFICADAS") ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected);
        binding.filterVerificadas.setTextColor(filtro.equals("VERIFICADAS") ? colorSelected : colorUnselected);

        binding.filterCerradas.setBackgroundResource(filtro.equals("CERRADAS") ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected);
        binding.filterCerradas.setTextColor(filtro.equals("CERRADAS") ? colorSelected : colorUnselected);

        // Filtrar la lista
        List<Alerta> filtradas;
        if (filtro.equals("TODAS")) {
            filtradas = listaCompleta;
        } else if (filtro.equals("PENDIENTES")) {
            filtradas = listaCompleta.stream()
                    .filter(a -> "PENDIENTE".equals(a.getEstado()) || "REGISTRADA".equals(a.getEstado()) || "EN_REVISION".equals(a.getEstado()))
                    .collect(Collectors.toList());
        } else if (filtro.equals("VERIFICADAS")) {
            filtradas = listaCompleta.stream()
                    .filter(a -> "VERIFICADA".equals(a.getEstado()))
                    .collect(Collectors.toList());
        } else { // CERRADAS
            filtradas = listaCompleta.stream()
                    .filter(a -> "RECHAZADA".equals(a.getEstado()) || "CERRADA".equals(a.getEstado()))
                    .collect(Collectors.toList());
        }

        adapter = new VerificacionAdapter(filtradas, alerta -> {
            VerificacionBottomSheet bottomSheet = VerificacionBottomSheet.newInstance(alerta);
            bottomSheet.show(getParentFragmentManager(), "VerificacionBottomSheet");
        });
        binding.rvVerificacion.setAdapter(adapter);
    }

    private void actualizarContadores() {
        int pendientes = 0, verificadas = 0, cerradas = 0;
        for (Alerta a : listaCompleta) {
            if ("VERIFICADA".equals(a.getEstado())) verificadas++;
            else if ("RECHAZADA".equals(a.getEstado()) || "CERRADA".equals(a.getEstado())) cerradas++;
            else pendientes++;
        }
        binding.tvCountPendientes.setText(String.valueOf(pendientes));
        binding.tvCountVerificadas.setText(String.valueOf(verificadas));
        binding.tvCountCerradas.setText(String.valueOf(cerradas));
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}