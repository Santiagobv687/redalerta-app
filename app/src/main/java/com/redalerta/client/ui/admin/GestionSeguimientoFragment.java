package com.redalerta.client.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.redalerta.client.databinding.FragmentGestionSeguimientoBinding;

public class GestionSeguimientoFragment extends Fragment {
    private FragmentGestionSeguimientoBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGestionSeguimientoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
}