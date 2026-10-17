package com.redalerta.client.ui.alert;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentAlertaRegistradaBinding;

public class AlertaRegistradaFragment extends Fragment {

    private FragmentAlertaRegistradaBinding binding;

    public static final String ARG_TIPO = "arg_tipo";
    public static final String ARG_PRIORIDAD = "arg_prioridad";
    public static final String ARG_UBICACION = "arg_ubicacion";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertaRegistradaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Recibir datos del Bundle
        if (getArguments() != null) {
            String tipo = getArguments().getString(ARG_TIPO, "—");
            String prioridad = getArguments().getString(ARG_PRIORIDAD, "—");
            String ubicacion = getArguments().getString(ARG_UBICACION, "—");

            binding.tvResumenTipo.setText(tipo);
            binding.tvResumenPrioridad.setText(prioridad);
            binding.tvResumenUbicacion.setText(ubicacion);
        }

        // Botón para volver a registrar otra alerta
        binding.btnRegistrarOtra.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new RegistroAlertaFragment())
                    .commit();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}