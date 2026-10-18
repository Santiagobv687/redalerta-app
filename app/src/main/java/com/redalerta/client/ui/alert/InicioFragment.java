package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.redalerta.client.databinding.FragmentInicioBinding;
import com.redalerta.client.model.alerta.*;
import com.redalerta.client.viewmodel.AlertaViewModel;
import java.util.ArrayList;

public class InicioFragment extends Fragment {

    private FragmentInicioBinding binding;
    private AlertaViewModel viewModel;
    private InicioAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentInicioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AlertaViewModel.class);

        adapter = new InicioAdapter(new ArrayList<>());
        binding.rvInicio.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvInicio.setAdapter(adapter);

        viewModel.getTodasLasAlertas().observe(getViewLifecycleOwner(), alertas -> {
            if (alertas != null) {
                adapter = new InicioAdapter(alertas);
                binding.rvInicio.setAdapter(adapter);
            }
        });

        viewModel.cargarTodasLasAlertas();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}