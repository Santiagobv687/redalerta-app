package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redalerta.client.databinding.FragmentMisPublicacionesBinding;
import com.redalerta.client.model.Alerta;
import com.redalerta.client.viewmodel.AlertaViewModel;

import java.util.ArrayList;
import java.util.List;

public class MisPublicacionesFragment extends Fragment implements MisPublicacionesAdapter.OnItemActionListener {

    private FragmentMisPublicacionesBinding binding;
    private AlertaViewModel viewModel;
    private MisPublicacionesAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMisPublicacionesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AlertaViewModel.class);

        // Configurar RecyclerView
        adapter = new MisPublicacionesAdapter(new ArrayList<>(), this);
        binding.rvMisPublicaciones.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMisPublicaciones.setAdapter(adapter);

        // Observar lista de alertas
        viewModel.getMisAlertas().observe(getViewLifecycleOwner(), alertas -> {
            if (alertas != null) {
                adapter = new MisPublicacionesAdapter(alertas, this);
                binding.rvMisPublicaciones.setAdapter(adapter);
            }
        });

        // Pedir datos al ViewModel
        viewModel.cargarMisAlertas();
    }

    @Override
    public void onEditar(Alerta alerta) {
        Toast.makeText(requireContext(), "Editar: " + alerta.getTitulo(), Toast.LENGTH_SHORT).show();
        // TODO: Abrir pantalla de edición
    }

    @Override
    public void onEliminar(Alerta alerta) {
        // Aquí deberías mostrar un AlertDialog de confirmación antes de eliminar
        Toast.makeText(requireContext(), "Eliminar: " + alerta.getTitulo(), Toast.LENGTH_SHORT).show();
        viewModel.eliminarAlerta(alerta);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}