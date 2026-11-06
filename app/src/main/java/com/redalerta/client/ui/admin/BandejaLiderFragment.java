package com.redalerta.client.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentBandejaLiderBinding;
import com.redalerta.client.ui.alert.DetalleAlertaFragment;
import com.redalerta.client.viewmodel.AlertaLiderViewModel;

import java.util.ArrayList;

public class BandejaLiderFragment extends Fragment {

    private FragmentBandejaLiderBinding binding;
    private AlertaLiderViewModel viewModel;
    private BandejaLiderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBandejaLiderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AlertaLiderViewModel.class);

        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();

        viewModel.cargarBandejaDireccionadas(0, 50);
    }

    private void setupRecyclerView() {
        adapter = new BandejaLiderAdapter(new ArrayList<>(), alerta -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, DetalleAlertaFragment.newInstance(alerta))
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvBandeja.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBandeja.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshBandeja.setColorSchemeResources(R.color.ra_red);
        binding.swipeRefreshBandeja.setOnRefreshListener(() -> viewModel.cargarBandejaDireccionadas(0, 50));
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == AlertaLiderViewModel.UiState.LOADING && !binding.swipeRefreshBandeja.isRefreshing()) {
                binding.layoutLoading.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.rvBandeja.setVisibility(View.GONE);
            } else if (state == AlertaLiderViewModel.UiState.SUCCESS || state == AlertaLiderViewModel.UiState.ERROR) {
                binding.layoutLoading.setVisibility(View.GONE);
                binding.swipeRefreshBandeja.setRefreshing(false);
            }
        });

        viewModel.getBandejaDireccionadas().observe(getViewLifecycleOwner(), alertas -> {
            if (alertas == null || alertas.isEmpty()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.rvBandeja.setVisibility(View.GONE);
            } else {
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.rvBandeja.setVisibility(View.VISIBLE);
                adapter = new BandejaLiderAdapter(alertas, alerta -> {
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, DetalleAlertaFragment.newInstance(alerta))
                            .addToBackStack(null)
                            .commit();
                });
                binding.rvBandeja.setAdapter(adapter);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
