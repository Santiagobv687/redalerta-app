package com.redalerta.client.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.redalerta.client.databinding.FragmentRegistroPaso3Binding;
import com.redalerta.client.model.CaracterizacionRequest;
import com.redalerta.client.model.RegistroRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RegistroPaso3Fragment extends Fragment {

    private FragmentRegistroPaso3Binding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroPaso3Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // 1. Configurar componentes de Caracterización Social
        configurarCaracterizacionUI();

        // 2. Cargar datos previos si existen
        cargardatosPrevios();

        // 3. Configurar limpieza dinámica de errores al digitar
        configurarLimpiezaErrores();

        // 4. Navegación
        binding.btnAnterior.setOnClickListener(v ->
                ((RegistroActivity) requireActivity()).mostrarPaso(2));

        binding.btnSiguiente.setOnClickListener(v -> {
            if (validarYGuardar()) {
                ((RegistroActivity) requireActivity()).mostrarPaso(4);
            }
        });
    }

    private void configurarLimpiezaErrores() {
        limpiarErrorAlDigitar(binding.tilFechaNacimiento, binding.etFechaNacimiento);
        limpiarErrorAlDigitar(binding.tilGenero, binding.etGenero);
        limpiarErrorAlDigitar(binding.tilIntegrantes, binding.etIntegrantes);
    }

    private void limpiarErrorAlDigitar(TextInputLayout til, EditText et) {
        if (til == null || et == null) return;
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                til.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void configurarCaracterizacionUI() {
        // DatePicker para fecha de nacimiento
        binding.etFechaNacimiento.setOnClickListener(v -> mostrarDatePicker());
        binding.tilFechaNacimiento.setEndIconOnClickListener(v -> mostrarDatePicker());

        // Dropdown de Género
        String[] generos = {"MASCULINO", "FEMENINO", "OTRO"};
        binding.etGenero.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                generos));

        // Listener 1: Mostrar "Gestación o Lactancia" SOLO cuando género sea "FEMENINO"
        binding.etGenero.setOnItemClickListener((parent, view, position, id) -> evaluarVisibilidadGestacion());
        binding.etGenero.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                evaluarVisibilidadGestacion();
            }
        });

        // Listener 2: Mostrar cantidad de menores SOLO si se activa el switch de menores
        binding.swMenores.setOnCheckedChangeListener((btn, isChecked) -> {
            binding.tilCantidadMenores.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                binding.etCantidadMenores.setText("1");
            }
        });

        // Listener 3: Mostrar especificación de discapacidad SOLO si se activa el switch
        binding.swDiscapacidad.setOnCheckedChangeListener((btn, isChecked) -> {
            binding.tilEspecificacionDiscapacidad.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                binding.etEspecificacionDiscapacidad.setText("");
            }
        });

        // Listener 4: Mostrar especificación de grupo étnico SOLO si se activa el switch
        binding.swEtnico.setOnCheckedChangeListener((btn, isChecked) -> {
            binding.tilEspecificacionEtnico.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                binding.etEspecificacionEtnico.setText("");
            }
        });
    }

    private void evaluarVisibilidadGestacion() {
        String genero = binding.etGenero.getText().toString().trim();
        boolean esFemenino = "FEMENINO".equalsIgnoreCase(genero);
        binding.swGestacion.setVisibility(esFemenino ? View.VISIBLE : View.GONE);
        if (!esFemenino) {
            binding.swGestacion.setChecked(false);
        }
    }

    private void mostrarDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccione fecha de nacimiento")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String fechaFormatted = sdf.format(new Date(selection));
            binding.etFechaNacimiento.setText(fechaFormatted);
            binding.tilFechaNacimiento.setError(null);
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void cargardatosPrevios() {
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();
        if (req != null && req.getCaracterizacion() != null) {
            CaracterizacionRequest car = req.getCaracterizacion();
            if (car.getFechaNacimiento() != null) binding.etFechaNacimiento.setText(car.getFechaNacimiento());
            if (car.getGenero() != null) {
                binding.etGenero.setText(car.getGenero(), false);
                evaluarVisibilidadGestacion();
            }

            if (car.getNumeroIntegrantesFamilia() != null) {
                binding.etIntegrantes.setText(String.valueOf(car.getNumeroIntegrantesFamilia()));
            }

            if (Boolean.TRUE.equals(car.getEnGestacionLactancia()) && "FEMENINO".equalsIgnoreCase(car.getGenero())) {
                binding.swGestacion.setChecked(true);
            }

            if (Boolean.TRUE.equals(car.getTieneMenoresDeEdad())) {
                binding.swMenores.setChecked(true);
                binding.tilCantidadMenores.setVisibility(View.VISIBLE);
                if (car.getCantidadMenores() != null) {
                    binding.etCantidadMenores.setText(String.valueOf(car.getCantidadMenores()));
                }
            }

            if (Boolean.TRUE.equals(car.getTieneMiembroDiscapacidad())) {
                binding.swDiscapacidad.setChecked(true);
                binding.tilEspecificacionDiscapacidad.setVisibility(View.VISIBLE);
                if (car.getEspecificacionDiscapacidad() != null) {
                    binding.etEspecificacionDiscapacidad.setText(car.getEspecificacionDiscapacidad());
                }
            }

            if (Boolean.TRUE.equals(car.getPerteneceGrupoEtnico())) {
                binding.swEtnico.setChecked(true);
                binding.tilEspecificacionEtnico.setVisibility(View.VISIBLE);
                if (car.getEspecificacionGrupoEtnico() != null) {
                    binding.etEspecificacionEtnico.setText(car.getEspecificacionGrupoEtnico());
                }
            }
        }
    }

    private boolean validarYGuardar() {
        String fechaNacimiento = binding.etFechaNacimiento.getText().toString().trim();
        String genero = binding.etGenero.getText().toString().trim();
        String numIntegrantesStr = binding.etIntegrantes.getText().toString().trim();

        // Validaciones de Caracterización (Obligatorios en OpenAPI)
        if (fechaNacimiento.isEmpty()) {
            binding.tilFechaNacimiento.setError("Seleccione fecha de nacimiento");
            return false;
        }
        binding.tilFechaNacimiento.setError(null);

        if (genero.isEmpty()) {
            binding.tilGenero.setError("Seleccione género");
            return false;
        }
        binding.tilGenero.setError(null);

        int numIntegrantes = 1;
        if (!numIntegrantesStr.isEmpty()) {
            try {
                numIntegrantes = Integer.parseInt(numIntegrantesStr);
            } catch (NumberFormatException ignored) {}
        }

        // Ensamblar objeto Caracterización
        CaracterizacionRequest car = new CaracterizacionRequest();
        car.setFechaNacimiento(fechaNacimiento);
        car.setGenero(genero);
        car.setNumeroIntegrantesFamilia(numIntegrantes);

        boolean esFemenino = "FEMENINO".equalsIgnoreCase(genero);
        car.setEnGestacionLactancia(esFemenino && binding.swGestacion.isChecked());

        boolean tieneMenores = binding.swMenores.isChecked();
        car.setTieneMenoresDeEdad(tieneMenores);
        if (tieneMenores) {
            String cant = binding.etCantidadMenores.getText().toString().trim();
            car.setCantidadMenores(cant.isEmpty() ? 1 : Integer.parseInt(cant));
        } else {
            car.setCantidadMenores(0);
        }

        boolean tieneDiscapacidad = binding.swDiscapacidad.isChecked();
        car.setTieneMiembroDiscapacidad(tieneDiscapacidad);
        if (tieneDiscapacidad) {
            car.setEspecificacionDiscapacidad(binding.etEspecificacionDiscapacidad.getText().toString().trim());
        } else {
            car.setEspecificacionDiscapacidad(null);
        }

        boolean perteneceEtnico = binding.swEtnico.isChecked();
        car.setPerteneceGrupoEtnico(perteneceEtnico);
        if (perteneceEtnico) {
            car.setEspecificacionGrupoEtnico(binding.etEspecificacionEtnico.getText().toString().trim());
        } else {
            car.setEspecificacionGrupoEtnico(null);
        }

        // Guardar en objeto compartido
        RegistroRequest req = ((RegistroActivity) requireActivity()).getRegistroRequest();
        req.setCaracterizacion(car);
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
