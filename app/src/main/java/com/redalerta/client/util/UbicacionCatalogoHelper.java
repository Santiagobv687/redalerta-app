package com.redalerta.client.util;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.redalerta.client.R;
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
                .obtenerCatalogoCompleto()
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
                        Log.e(TAG, "Error al obtener catálogo de ubicaciones (/ubicaciones/catalogo): " + t.getMessage());
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

        fallback.add(new DepartamentoResponse(1, "Quindío", Arrays.asList(
                "Armenia", "Buenavista", "Calarcá", "Circasia", "Córdoba",
                "Filandia", "Génova", "La Tebaida", "Montenegro", "Pijao", "Quimbaya", "Salento"
        )));

        fallback.add(new DepartamentoResponse(2, "Risaralda", Arrays.asList(
                "Pereira", "Dosquebradas", "Santa Rosa de Cabal", "La Virginia",
                "Belén de Umbría", "Marsella", "Santuario"
        )));

        fallback.add(new DepartamentoResponse(3, "Caldas", Arrays.asList(
                "Manizales", "Villamaría", "Chinchiná", "Neira", "Palestina", "Riosucio", "La Dorada"
        )));

        fallback.add(new DepartamentoResponse(4, "Antioquia", Arrays.asList(
                "Medellín", "Bello", "Itagüí", "Envigado", "Rionegro", "Sabaneta", "Apartadó", "Caucasia"
        )));

        fallback.add(new DepartamentoResponse(5, "Cundinamarca", Arrays.asList(
                "Bogotá D.C.", "Soacha", "Chía", "Zipaquirá", "Facatativá", "Fusagasugá", "Girardot"
        )));

        fallback.add(new DepartamentoResponse(6, "Valle del Cauca", Arrays.asList(
                "Cali", "Palmira", "Buenaventura", "Tuluá", "Buga", "Cartago", "Jamundí"
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
        configurarSelectoresEncadenados(context, tilDepto, actvDepto, tilMun, actvMun, null, listener);
    }

    public static void configurarSelectoresEncadenados(
            Context context,
            TextInputLayout tilDepto,
            MaterialAutoCompleteTextView actvDepto,
            TextInputLayout tilMun,
            MaterialAutoCompleteTextView actvMun,
            android.view.View progressIndicator,
            OnUbicacionChangeListener listener) {

        if (context == null || tilDepto == null || actvDepto == null || tilMun == null || actvMun == null) {
            return;
        }

        // 1. Estilizar la ventana emergente de ambos selectores
        estilizarPopupDropdown(context, actvDepto, 32);
        estilizarPopupDropdown(context, actvMun, 0);

        // 2. Estado inicial accesible: municipio deshabilitado con mensaje explicativo
        tilMun.setEnabled(false);
        tilMun.setHelperText("Seleccione primero el departamento");

        if (progressIndicator != null) {
            progressIndicator.setVisibility(android.view.View.VISIBLE);
        }
        tilDepto.setHelperText("Cargando catálogo nacional…");

        cargarCatalogo(context, catalogo -> {
            if (progressIndicator != null) {
                progressIndicator.setVisibility(android.view.View.GONE);
            }
            tilDepto.setHelperText(null);
            tilDepto.setHelperTextEnabled(false);

            List<String> nombresDeptos = new ArrayList<>();
            for (DepartamentoResponse d : catalogo) {
                if (d.getDepartamento() != null && !d.getDepartamento().trim().isEmpty()) {
                    nombresDeptos.add(d.getDepartamento().trim());
                }
            }

            ArrayAdapter<String> adapterDepto = new ArrayAdapter<>(
                    context,
                    R.layout.item_dropdown_ubicacion,
                    nombresDeptos
            );
            actvDepto.setAdapter(adapterDepto);
            estilizarPopupDropdown(context, actvDepto, nombresDeptos.size());

            // Escuchar cambios en la selección del departamento
            actvDepto.setOnItemClickListener((parent, view, position, id) -> {
                String deptoSeleccionado = (String) parent.getItemAtPosition(position);
                actualizarMunicipiosPorDepartamento(context, deptoSeleccionado, tilMun, actvMun, progressIndicator, listener);
            });

            actvDepto.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String deptoActual = actvDepto.getText() != null ? actvDepto.getText().toString().trim() : "";
                    if (!deptoActual.isEmpty()) {
                        actualizarMunicipiosPorDepartamento(context, deptoActual, tilMun, actvMun, progressIndicator, listener);
                    } else {
                        actvMun.setText("", false);
                        tilMun.setEnabled(false);
                        tilMun.setHelperText("Seleccione primero el departamento");
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

            // Si el departamento ya traía texto preexistente
            String deptoExistente = actvDepto.getText() != null ? actvDepto.getText().toString().trim() : "";
            if (!deptoExistente.isEmpty()) {
                actualizarMunicipiosPorDepartamento(context, deptoExistente, tilMun, actvMun, progressIndicator, listener);
            }
        });
    }

    /**
     * Ajusta la apariencia del menú desplegable y calcula su altura dinámicamente:
     * - Si hay 4 o menos opciones, usa WRAP_CONTENT para ajustarse exactamente a la cantidad de ítems sin dejar espacio vacío.
     * - Si hay más de 4 opciones, limita la altura a 210dp para permitir scroll y evitar que el menú se despliegue hacia arriba.
     */
    private static void estilizarPopupDropdown(Context context, MaterialAutoCompleteTextView actv, int itemCount) {
        float density = context.getResources().getDisplayMetrics().density;
        actv.setDropDownBackgroundResource(R.drawable.bg_dropdown_popup);
        actv.setDropDownVerticalOffset((int) (6 * density));
        if (itemCount > 4) {
            actv.setDropDownHeight((int) (210 * density));
        } else {
            actv.setDropDownHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static void actualizarMunicipiosPorDepartamento(
            Context context,
            String deptoNombre,
            TextInputLayout tilMun,
            MaterialAutoCompleteTextView actvMun,
            android.view.View progressIndicator,
            OnUbicacionChangeListener listener) {

        if (deptoNombre == null || deptoNombre.trim().isEmpty()) {
            actvMun.setText("", false);
            tilMun.setEnabled(false);
            tilMun.setHelperText("Seleccione primero el departamento");
            if (listener != null) listener.onUbicacionChanged();
            return;
        }

        List<DepartamentoResponse> catalogo = obtenerCatalogoEnMemoria();
        DepartamentoResponse deptoEncontrado = null;

        for (DepartamentoResponse d : catalogo) {
            if (d.getDepartamento() != null && d.getDepartamento().equalsIgnoreCase(deptoNombre.trim())) {
                deptoEncontrado = d;
                break;
            }
        }

        if (deptoEncontrado != null && deptoEncontrado.getMunicipios() != null && !deptoEncontrado.getMunicipios().isEmpty()) {
            aplicarMunicipiosAdapter(context, deptoEncontrado.getMunicipios(), tilMun, actvMun, listener);
        } else {
            if (progressIndicator != null) progressIndicator.setVisibility(android.view.View.VISIBLE);
            tilMun.setHelperText("Consultando municipios de " + deptoNombre.trim() + "…");

            DepartamentoResponse finalDeptoEncontrado = deptoEncontrado;
            ApiClient.getApiService(context)
                    .listarMunicipiosPorDepartamento(deptoNombre.trim())
                    .enqueue(new Callback<ApiResponse<List<String>>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<List<String>>> call, Response<ApiResponse<List<String>>> response) {
                            if (progressIndicator != null) progressIndicator.setVisibility(android.view.View.GONE);
                            List<String> lista = (response.isSuccessful() && response.body() != null && response.body().getData() != null)
                                    ? response.body().getData()
                                    : new ArrayList<>();

                            if (!lista.isEmpty()) {
                                if (finalDeptoEncontrado != null) {
                                    finalDeptoEncontrado.setMunicipios(lista);
                                } else {
                                    catalogoMemoria.add(new DepartamentoResponse(deptoNombre.trim(), lista));
                                }
                                aplicarMunicipiosAdapter(context, lista, tilMun, actvMun, listener);
                            } else {
                                tilMun.setEnabled(false);
                                tilMun.setHelperText("No se encontraron municipios para este departamento");
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<List<String>>> call, Throwable t) {
                            if (progressIndicator != null) progressIndicator.setVisibility(android.view.View.GONE);
                            Log.e(TAG, "Error al consultar municipios por departamento: " + t.getMessage());
                            tilMun.setEnabled(false);
                            tilMun.setHelperText("No fue posible cargar los municipios");
                        }
                    });
        }
    }

    private static void aplicarMunicipiosAdapter(
            Context context,
            List<String> municipios,
            TextInputLayout tilMun,
            MaterialAutoCompleteTextView actvMun,
            OnUbicacionChangeListener listener) {

        ArrayAdapter<String> adapterMun = new ArrayAdapter<>(
                context,
                R.layout.item_dropdown_ubicacion,
                municipios
        );
        actvMun.setAdapter(adapterMun);
        estilizarPopupDropdown(context, actvMun, municipios.size());
        tilMun.setEnabled(true);
        tilMun.setHelperText(null);
        tilMun.setHelperTextEnabled(false);

        // Si el municipio actual no pertenece al nuevo departamento seleccionado, se limpia
        String munActual = actvMun.getText() != null ? actvMun.getText().toString().trim() : "";
        if (!munActual.isEmpty() && !contieneMunicipioInsensible(municipios, munActual)) {
            actvMun.setText("", false);
        }

        if (listener != null) {
            listener.onUbicacionChanged();
        }
    }

    private static boolean contieneMunicipioInsensible(List<String> municipios, String buscado) {
        for (String m : municipios) {
            if (m.equalsIgnoreCase(buscado.trim()) || normalizarTexto(m).equals(normalizarTexto(buscado))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Busca el nombre canónico del departamento en el catálogo a partir del texto devuelto por Geocoder
     * (ej: "Departamento del Quindío" o "Quindio" -> "Quindío").
     */
    public static String buscarDepartamentoCanonico(String rawDepto) {
        if (rawDepto == null || rawDepto.trim().isEmpty()) {
            return null;
        }
        String limpio = rawDepto.trim()
                .replaceFirst("(?i)^departamento\\s+del\\s+", "")
                .replaceFirst("(?i)^departamento\\s+de\\s+la\\s+", "")
                .replaceFirst("(?i)^departamento\\s+de\\s+", "")
                .trim();

        String normBuscado = normalizarTexto(limpio);
        for (DepartamentoResponse d : obtenerCatalogoEnMemoria()) {
            if (d.getDepartamento() != null) {
                String normCatalogo = normalizarTexto(d.getDepartamento());
                if (normCatalogo.equals(normBuscado) || normBuscado.contains(normCatalogo) || normCatalogo.contains(normBuscado)) {
                    return d.getDepartamento().trim();
                }
            }
        }
        return limpio;
    }

    /**
     * Busca el nombre canónico del municipio dentro del departamento indicado
     * (ej: "Armenia" o "Calarca" -> "Calarcá").
     */
    public static String buscarMunicipioCanonico(String departamento, String rawMunicipio) {
        if (rawMunicipio == null || rawMunicipio.trim().isEmpty()) {
            return null;
        }
        String limpio = rawMunicipio.trim()
                .replaceFirst("(?i)^municipio\\s+de\\s+", "")
                .trim();
        String normMun = normalizarTexto(limpio);

        for (DepartamentoResponse d : obtenerCatalogoEnMemoria()) {
            if (departamento == null
                    || (d.getDepartamento() != null && normalizarTexto(d.getDepartamento()).equals(normalizarTexto(departamento)))) {
                if (d.getMunicipios() != null) {
                    for (String m : d.getMunicipios()) {
                        if (normalizarTexto(m).equals(normMun)) {
                            return m.trim();
                        }
                    }
                }
            }
        }
        return limpio;
    }

    private static String normalizarTexto(String input) {
        if (input == null) return "";
        String normalized = java.text.Normalizer.normalize(input.trim(), java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase( java.util.Locale.ROOT);
    }
}
