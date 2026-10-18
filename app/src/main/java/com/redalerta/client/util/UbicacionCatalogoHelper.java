package com.redalerta.client.util;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.redalerta.client.model.common.DepartamentoResponse;
import com.redalerta.client.network.ApiClient;
import com.redalerta.client.network.ApiResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UbicacionCatalogoHelper {

    private static final String TAG = "UbicacionCatalogo";
    private static List<DepartamentoResponse> catalogoMemoria = null;

    public interface OnCatalogoCargadoListener {
        void onCargado(List<DepartamentoResponse> catalogo);
    }

    public interface OnUbicacionChangeListener {
        void onUbicacionChanged();
    }

    public static void cargarCatalogo(Context context, OnCatalogoCargadoListener listener) {
        if (catalogoMemoria != null && !catalogoMemoria.isEmpty()) {
            if (listener != null) listener.onCargado(catalogoMemoria);
            return;
        }

        ApiClient.getApiService(context)
                .obtenerUbicaciones()
                .enqueue(new Callback<ApiResponse<List<DepartamentoResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<DepartamentoResponse>>> call,
                                           Response<ApiResponse<List<DepartamentoResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                            catalogoMemoria = response.body().getData();
                        } else {
                            catalogoMemoria = obtenerCatalogoFallback();
                        }
                        if (listener != null) listener.onCargado(catalogoMemoria);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<DepartamentoResponse>>> call, Throwable t) {
                        Log.e(TAG, "Error al obtener catálogo de ubicaciones: " + t.getMessage());
                        catalogoMemoria = obtenerCatalogoFallback();
                        if (listener != null) listener.onCargado(catalogoMemoria);
                    }
                });
    }

    public static List<DepartamentoResponse> obtenerCatalogoEnMemoria() {
        if (catalogoMemoria == null || catalogoMemoria.isEmpty()) {
            catalogoMemoria = obtenerCatalogoFallback();
        }
        return catalogoMemoria;
    }

    public static List<DepartamentoResponse> obtenerCatalogoFallback() {
        List<DepartamentoResponse> fallback = new ArrayList<>();

        fallback.add(new DepartamentoResponse("Quindío", Arrays.asList(
                "Armenia", "Buenavista", "Calarcá", "Circasia", "Córdoba",
                "Filandia", "Génova", "La Tebaida", "Montenegro", "Pijao", "Quimbaya", "Salento"
        )));

        fallback.add(new DepartamentoResponse("Risaralda", Arrays.asList(
                "Pereira", "Dosquebradas", "Santa Rosa de Cabal", "La Virginia",
                "Belén de Umbría", "Marsella", "Santuario"
        )));

        fallback.add(new DepartamentoResponse("Caldas", Arrays.asList(
                "Manizales", "Villamaría", "Chinchiná", "Neira", "Palestina", "Riosucio", "La Dorada"
        )));

        fallback.add(new DepartamentoResponse("Antioquia", Arrays.asList(
                "Medellín", "Bello", "Itagüí", "Envigado", "Rionegro", "Sabaneta"
        )));

        fallback.add(new DepartamentoResponse("Cundinamarca", Arrays.asList(
                "Bogotá D.C.", "Soacha", "Chía", "Zipaquirá", "Facatativá", "Fusagasugá"
        )));

        return fallback;
    }

    public static void configurarSelectoresEncadenados(
            Context context,
            TextInputLayout tilDepto,
            MaterialAutoCompleteTextView actvDepto,
            TextInputLayout tilMun,
            MaterialAutoCompleteTextView actvMun,
            OnUbicacionChangeListener listener) {

        if (context == null || tilDepto == null || actvDepto == null || tilMun == null || actvMun == null) {
            return;
        }

        // 1. Deshabilitar municipio por defecto hasta seleccionar departamento
        tilMun.setEnabled(false);

        cargarCatalogo(context, catalogo -> {
            List<String> nombresDeptos = new ArrayList<>();
            for (DepartamentoResponse d : catalogo) {
                if (d.getDepartamento() != null) nombresDeptos.add(d.getDepartamento());
            }

            ArrayAdapter<String> adapterDepto = new ArrayAdapter<>(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    nombresDeptos
            );
            actvDepto.setAdapter(adapterDepto);

            // Escuchar cambios en la selección del departamento
            actvDepto.setOnItemClickListener((parent, view, position, id) -> {
                String deptoSeleccionado = (String) parent.getItemAtPosition(position);
                actualizarMunicipiosPorDepartamento(context, deptoSeleccionado, tilMun, actvMun, listener);
            });

            actvDepto.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String deptoActual = actvDepto.getText() != null ? actvDepto.getText().toString().trim() : "";
                    if (!deptoActual.isEmpty()) {
                        actualizarMunicipiosPorDepartamento(context, deptoActual, tilMun, actvMun, listener);
                    } else {
                        actvMun.setText("", false);
                        tilMun.setEnabled(false);
                        if (listener != null) listener.onUbicacionChanged();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            // Escuchar cambios en la selección del municipio
            actvMun.setOnItemClickListener((parent, view, position, id) -> {
                if (listener != null) {
                    listener.onUbicacionChanged();
                }
            });

            actvMun.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (listener != null) {
                        listener.onUbicacionChanged();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            // Si el departamento ya traía texto pre-existente
            String deptoExistente = actvDepto.getText() != null ? actvDepto.getText().toString().trim() : "";
            if (!deptoExistente.isEmpty()) {
                actualizarMunicipiosPorDepartamento(context, deptoExistente, tilMun, actvMun, listener);
            }
        });
    }

    private static void actualizarMunicipiosPorDepartamento(
            Context context,
            String deptoNombre,
            TextInputLayout tilMun,
            MaterialAutoCompleteTextView actvMun,
            OnUbicacionChangeListener listener) {

        List<DepartamentoResponse> catalogo = obtenerCatalogoEnMemoria();
        List<String> municipios = new ArrayList<>();

        for (DepartamentoResponse d : catalogo) {
            if (d.getDepartamento() != null && d.getDepartamento().equalsIgnoreCase(deptoNombre.trim())) {
                if (d.getMunicipios() != null) {
                    municipios.addAll(d.getMunicipios());
                }
                break;
            }
        }

        if (!municipios.isEmpty()) {
            ArrayAdapter<String> adapterMun = new ArrayAdapter<>(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    municipios
            );
            actvMun.setAdapter(adapterMun);
            tilMun.setEnabled(true);

            // Si el municipio actual no pertenece al nuevo departamento seleccionado, se limpia
            String munActual = actvMun.getText() != null ? actvMun.getText().toString().trim() : "";
            if (!munActual.isEmpty() && !contieneMunicipioInsensible(municipios, munActual)) {
                actvMun.setText("", false);
            }
        } else {
            actvMun.setText("", false);
            tilMun.setEnabled(false);
        }

        if (listener != null) {
            listener.onUbicacionChanged();
        }
    }

    private static boolean contieneMunicipioInsensible(List<String> municipios, String buscado) {
        for (String m : municipios) {
            if (m.equalsIgnoreCase(buscado.trim())) return true;
        }
        return false;
    }
}
