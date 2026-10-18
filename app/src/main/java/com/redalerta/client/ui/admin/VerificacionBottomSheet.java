package com.redalerta.client.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.redalerta.client.R;
import com.redalerta.client.databinding.BottomSheetVerificacionBinding;
import com.redalerta.client.model.alerta.*;
import com.redalerta.client.viewmodel.AlertaViewModel;

public class VerificacionBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetVerificacionBinding binding;
    private Alerta alertaActual;
    private AlertaViewModel viewModel;

    public static VerificacionBottomSheet newInstance(Alerta alerta) {
        VerificacionBottomSheet fragment = new VerificacionBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("alerta", alerta); // Asegúrate de que Alerta implemente Serializable o usa Gson/Parcelable
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetVerificacionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(AlertaViewModel.class);

        if (getArguments() != null) {
            alertaActual = (Alerta) getArguments().getSerializable("alerta");
        }

        if (alertaActual != null) {
            binding.bsId.setText(alertaActual.getId());
            binding.bsTitulo.setText(alertaActual.getTitulo());
            binding.bsDescripcion.setText(alertaActual.getDescripcion());
            binding.bsUbicacion.setText(alertaActual.getDireccion() + ", " + alertaActual.getBarrio());
            binding.bsReportante.setText(alertaActual.getUsuarioNombre());
            binding.bsFecha.setText(alertaActual.getFechaActualizacion());

            binding.bsPrioridad.setText(alertaActual.getPrioridad());
            int colorPrio = alertaActual.getPrioridad().equalsIgnoreCase(PrioridadAlerta.ALTA) ? R.color.ra_prio_alta :
                    alertaActual.getPrioridad().equalsIgnoreCase(PrioridadAlerta.MEDIA) ? R.color.ra_prio_media : R.color.ra_prio_baja;
            binding.bsPrioridad.setTextColor(ContextCompat.getColor(requireContext(), colorPrio));
            binding.bsDotPrioridad.setBackgroundColor(ContextCompat.getColor(requireContext(), colorPrio));

            binding.bsEstado.setText(alertaActual.getEstado());
        }

        // Dropdown de estados
        String[] opciones = {"Verificada", "Rechazada"};
        binding.dropdownEstado.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, opciones));

        binding.btnCerrar.setOnClickListener(v -> dismiss());

        binding.btnAceptar.setOnClickListener(v -> {
            String seleccion = binding.dropdownEstado.getText().toString();
            if (seleccion.isEmpty()) {
                Toast.makeText(requireContext(), "Seleccione un estado", Toast.LENGTH_SHORT).show();
                return;
            }

            String nuevoEstado = seleccion.equals("Verificada") ? "VERIFICADA" : "RECHAZADA";
            viewModel.actualizarEstadoAlerta(alertaActual, nuevoEstado);

            // Notificar al fragmento para que recargue
            getParentFragmentManager().setFragmentResult("verificacion_result", new Bundle());
            dismiss();
        });
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}