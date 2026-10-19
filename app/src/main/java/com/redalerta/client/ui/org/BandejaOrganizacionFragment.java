package com.redalerta.client.ui.org;

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
import com.redalerta.client.databinding.FragmentBandejaOrganizacionBinding;
import com.redalerta.client.ui.alert.DetalleAlertaFragment;
import com.redalerta.client.viewmodel.OrganizacionBandejaViewModel;

import java.util.ArrayList;

public class BandejaOrganizacionFragment extends Fragment {

    private FragmentBandejaOrganizacionBinding binding;
    private OrganizacionBandejaViewModel viewModel;
    private BandejaOrganizacionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBandejaOrganizacionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OrganizacionBandejaViewModel.class);

        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();

        viewModel.cargarBandejaOrganizacion();
    }

    private void setupRecyclerView() {
        adapter = new BandejaOrganizacionAdapter(new ArrayList<>(), alerta -> {
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
        binding.swipeRefreshBandeja.setOnRefreshListener(() -> viewModel.cargarBandejaOrganizacion());
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == OrganizacionBandejaViewModel.UiState.LOADING && !binding.swipeRefreshBandeja.isRefreshing()) {
                binding.layoutLoading.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.rvBandeja.setVisibility(View.GONE);
            } else if (state == OrganizacionBandejaViewModel.UiState.SUCCESS || state == OrganizacionBandejaViewModel.UiState.EMPTY || state == OrganizacionBandejaViewModel.UiState.ERROR) {
                binding.layoutLoading.setVisibility(View.GONE);
                binding.swipeRefreshBandeja.setRefreshing(false);
            }
        });

        viewModel.getBandejaOrganizacion().observe(getViewLifecycleOwner(), alertas -> {
            if (alertas == null || alertas.isEmpty()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.rvBandeja.setVisibility(View.GONE);
            } else {
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.rvBandeja.setVisibility(View.VISIBLE);

                adapter = new BandejaOrganizacionAdapter(alertas, alerta -> {
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
