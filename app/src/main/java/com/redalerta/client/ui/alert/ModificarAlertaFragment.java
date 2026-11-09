package com.redalerta.client.ui.alert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentModificarAlertaBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.EvidenciaResponse;
import com.redalerta.client.model.alerta.ModificarAlertaRequest;
import com.redalerta.client.model.alerta.UbicacionAlertaDto;
import com.redalerta.client.ui.MainActivity;
import com.redalerta.client.util.GeocodificacionHelper;
import com.redalerta.client.viewmodel.AlertaViewModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModificarAlertaFragment extends Fragment {

    public static final String ARG_ALERTA = "arg_alerta";

    private static final double DEFAULT_LATITUD = 4.533889;
    private static final double DEFAULT_LONGITUD = -75.681111;
    private static final double DEFAULT_ALTITUD = 1483.0;
    private static final double DEFAULT_ZOOM = 17.0;

    private FragmentModificarAlertaBinding binding;
    private AlertaViewModel viewModel;
    private Alerta alertaOriginal;

    // Información territorial confirmada para UbicacionAlertaDto (OpenAPI)
    private String departamentoDetectado = "Quindío";
    private String municipioDetectado = "Armenia";

    // Estado de coordenadas GPS confirmadas para el payload UbicacionAlertaDto
    private double latitudConfirmada = DEFAULT_LATITUD;
    private double longitudConfirmada = DEFAULT_LONGITUD;
    @Nullable
    private Double altitudConfirmada = DEFAULT_ALTITUD;
    private boolean modoSeleccionMapaActivo = false;

    private LocationManager locationManager;
    private LocationListener activeLocationListener;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private final List<EvidenciasSeleccionadasAdapter.EvidenciaItem> listaEvidencias = new ArrayList<>();
    private EvidenciasSeleccionadasAdapter evidenciasAdapter;
    private ActivityResultLauncher<String> pickerLauncher;

    public static ModificarAlertaFragment newInstance(Alerta alerta) {
        ModificarAlertaFragment fragment = new ModificarAlertaFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALERTA, alerta);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        binding = FragmentModificarAlertaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AlertaViewModel.class);
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        if (getArguments() != null) {
            alertaOriginal = (Alerta) getArguments().getSerializable(ARG_ALERTA);
        }

        setupToolbar();
        setupMapaInteractivo();
        setupPermisosYGps();
        setupEvidencias();
        setupTextWatchers();
        observeViewModel();

        precargarDatosOriginales();
    }

    private void setupToolbar() {
        binding.toolbarModificarAlerta.setNavigationOnClickListener(v -> cancelarYVolver());
    }

    private void actualizarTextoUbicacionDetectada() {
        if (binding == null || !isAdded()) return;
        binding.tvUbicacionDetectada.setText(getString(R.string.alerta_municipio_detectado, municipioDetectado, departamentoDetectado));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMapaInteractivo() {
        binding.mapViewUbicacion.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapViewUbicacion.setMultiTouchControls(true);
        binding.mapViewUbicacion.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        binding.mapViewUbicacion.getController().setZoom(DEFAULT_ZOOM);

        // Cuando está en modo selección, evitar que el NestedScrollView robe los gestos de desplazamiento del mapa
        binding.mapViewUbicacion.setOnTouchListener((v, event) -> {
            if (modoSeleccionMapaActivo) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    binding.nestedScrollModificarAlerta.requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    binding.nestedScrollModificarAlerta.requestDisallowInterceptTouchEvent(false);
                }
                return false;
            }
            return true;
        });

        // Actualizar la vista previa de coordenadas en tiempo real mientras el usuario mueve el mapa
        binding.mapViewUbicacion.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (modoSeleccionMapaActivo && binding != null) {
                    GeoPoint centro = (GeoPoint) binding.mapViewUbicacion.getMapCenter();
                    actualizarTextoCoordenadas(centro.getLatitude(), centro.getLongitude(), null);
                }
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });

        // Botón "Seleccionar en el mapa": activa el modo de corrección manual del puntero
        binding.btnSeleccionarEnMapa.setOnClickListener(v -> activarModoSeleccionMapa(true));

        // Botón "Cancelar": descarta el movimiento del puntero y restaura la coordenada previa
        binding.btnCancelarMapa.setOnClickListener(v -> {
            activarModoSeleccionMapa(false);
            centrarMapaEnCoordenadas(latitudConfirmada, longitudConfirmada);
            actualizarTextoCoordenadas(latitudConfirmada, longitudConfirmada, altitudConfirmada);
        });

        // Botón "Guardar": confirma la nueva posición del puntero y ejecuta geocodificación inversa
        binding.btnGuardarMapa.setOnClickListener(v -> {
            GeoPoint nuevoCentro = (GeoPoint) binding.mapViewUbicacion.getMapCenter();
            latitudConfirmada = redondearCoordenada(nuevoCentro.getLatitude());
            longitudConfirmada = redondearCoordenada(nuevoCentro.getLongitude());
            altitudConfirmada = null;

            activarModoSeleccionMapa(false);
            actualizarTextoCoordenadas(latitudConfirmada, longitudConfirmada, null);
            ejecutarGeocodificacionInversa(latitudConfirmada, longitudConfirmada, null);
        });

        // Botón "Mi GPS": captura la ubicación actual del dispositivo y actualiza el formulario
        binding.btnUsarMiGps.setOnClickListener(v -> {
            if (modoSeleccionMapaActivo) {
                activarModoSeleccionMapa(false);
            }
            solicitarUbicacionGpsActual();
        });
    }

    private void activarModoSeleccionMapa(boolean activar) {
        modoSeleccionMapaActivo = activar;
        if (binding == null) return;

        binding.viewBloqueoMapa.setVisibility(activar ? View.GONE : View.VISIBLE);
        binding.tvBannerModoEdicionMapa.setVisibility(activar ? View.VISIBLE : View.GONE);

        binding.btnSeleccionarEnMapa.setVisibility(activar ? View.GONE : View.VISIBLE);
        binding.layoutBotonesEdicionMapa.setVisibility(activar ? View.VISIBLE : View.GONE);

        float density = getResources().getDisplayMetrics().density;
        binding.ivPunteroMapa.animate()
                .translationY(activar ? (-24f * density) : (-18f * density))
                .setDuration(150)
                .start();

        validarEstadoBotonGuardar();
    }

    private void setupPermisosYGps() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                        obtenerCoordenadasDesdeLocationManager();
                    } else if (binding != null) {
                        binding.tvEstadoGeocoder.setText(
                                "Permiso de GPS no concedido. Puede usar 'Seleccionar en el mapa' para ajustar la ubicación."
                        );
                    }
                }
        );
    }

    private void solicitarUbicacionGpsActual() {
        if (!isAdded()) return;

        boolean fineGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            obtenerCoordenadasDesdeLocationManager();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerCoordenadasDesdeLocationManager() {
        if (locationManager == null || binding == null) return;

        binding.tvEstadoGeocoder.setText("Obteniendo señal GPS del dispositivo…");

        try {
            boolean gpsHabilitado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean redHabilitada = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location ultimaUbicacion = null;
            if (gpsHabilitado) {
                ultimaUbicacion = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (ultimaUbicacion == null && redHabilitada) {
                ultimaUbicacion = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (ultimaUbicacion != null) {
                aplicarUbicacionGpsObtenida(ultimaUbicacion);
            }

            String proveedorActivo = gpsHabilitado
                    ? LocationManager.GPS_PROVIDER
                    : (redHabilitada ? LocationManager.NETWORK_PROVIDER : null);

            if (proveedorActivo != null) {
                detenerActualizacionesUbicacion();
                activeLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        detenerActualizacionesUbicacion();
                        if (!modoSeleccionMapaActivo && isAdded()) {
                            aplicarUbicacionGpsObtenida(location);
                        }
                    }

                    @Override public void onProviderEnabled(@NonNull String provider) {}
                    @Override public void onProviderDisabled(@NonNull String provider) {}
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                };
                locationManager.requestLocationUpdates(proveedorActivo, 0L, 0f, activeLocationListener);
            }
        } catch (Exception e) {
            ejecutarGeocodificacionInversa(latitudConfirmada, longitudConfirmada, altitudConfirmada);
        }
    }

    private void aplicarUbicacionGpsObtenida(@NonNull Location location) {
        latitudConfirmada = redondearCoordenada(location.getLatitude());
        longitudConfirmada = redondearCoordenada(location.getLongitude());
        altitudConfirmada = location.hasAltitude()
                ? Math.round(location.getAltitude() * 10.0) / 10.0
                : DEFAULT_ALTITUD;

        centrarMapaEnCoordenadas(latitudConfirmada, longitudConfirmada);
        actualizarTextoCoordenadas(latitudConfirmada, longitudConfirmada, altitudConfirmada);
        ejecutarGeocodificacionInversa(latitudConfirmada, longitudConfirmada, altitudConfirmada);
    }

    private void centrarMapaEnCoordenadas(double latitud, double longitud) {
        if (binding == null) return;
        GeoPoint punto = new GeoPoint(latitud, longitud);
        binding.mapViewUbicacion.getController().animateTo(punto);
    }

    private void actualizarTextoCoordenadas(double latitud, double longitud, @Nullable Double altitud) {
        if (binding == null) return;
        String textoCoord = String.format(Locale.US, "Lat: %.6f, Lon: %.6f", latitud, longitud);
        if (altitud != null) {
            textoCoord += String.format(Locale.US, " • Alt: %.0f m", altitud);
        }
        binding.tvCoordenadasGps.setText(textoCoord);
    }

    private void ejecutarGeocodificacionInversa(double latitud, double longitud, @Nullable Double altitud) {
        if (!isAdded() || binding == null) return;

        binding.tvEstadoGeocoder.setText("Consultando departamento, municipio y dirección…");
        binding.tvEstadoGeocoder.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_light_text_secondary));

        GeocodificacionHelper.obtenerUbicacionDesdeCoordenadas(
                requireContext(),
                latitud,
                longitud,
                altitud,
                departamentoDetectado,
                municipioDetectado,
                resultado -> {
                    if (!isAdded() || binding == null) return;

                    UbicacionAlertaDto dto = resultado.getUbicacionDto();

                    if (dto.getDepartamento() != null && !dto.getDepartamento().isEmpty()) {
                        departamentoDetectado = dto.getDepartamento();
                    }
                    if (dto.getMunicipio() != null && !dto.getMunicipio().isEmpty()) {
                        municipioDetectado = dto.getMunicipio();
                    }
                    actualizarTextoUbicacionDetectada();

                    if (dto.getDireccionDetallada() != null && !dto.getDireccionDetallada().isEmpty()) {
                        binding.etDireccionDetallada.setText(dto.getDireccionDetallada());
                    }

                    binding.tvEstadoGeocoder.setText(resultado.getMensajeInformativo());
                    int colorTexto = resultado.isExitoGeocoder()
                            ? ContextCompat.getColor(requireContext(), R.color.ra_success_card_text)
                            : ContextCompat.getColor(requireContext(), R.color.ra_status_revision_text);
                    binding.tvEstadoGeocoder.setTextColor(colorTexto);

                    limpiarErrorCampo(binding.tilDireccionDetallada);
                    validarEstadoBotonGuardar();
                }
        );
    }

    private double redondearCoordenada(double valor) {
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }

    private void detenerActualizacionesUbicacion() {
        if (locationManager != null && activeLocationListener != null) {
            try {
                locationManager.removeUpdates(activeLocationListener);
            } catch (Exception ignored) {}
            activeLocationListener = null;
        }
    }

    private void setupEvidencias() {
        evidenciasAdapter = new EvidenciasSeleccionadasAdapter(listaEvidencias, position -> {
            if (position >= 0 && position < listaEvidencias.size()) {
                listaEvidencias.remove(position);
                evidenciasAdapter.notifyDataSetChanged();
                validarEstadoBotonGuardar();
            }
        });

        binding.rvEvidencias.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvEvidencias.setAdapter(evidenciasAdapter);

        pickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                adjuntarYSubirEvidencia(uri);
            }
        });

        binding.btnAgregarFoto.setOnClickListener(v -> {
            if (listaEvidencias.size() >= 5) {
                com.redalerta.client.util.AlertaMensajesHelper.mostrarError(
                        binding.getRoot(),
                        "Máximo 5 evidencias fotográficas permitidas"
                );
                return;
            }
            pickerLauncher.launch("image/*");
        });
    }

    private void adjuntarYSubirEvidencia(Uri uri) {
        EvidenciasSeleccionadasAdapter.EvidenciaItem item = new EvidenciasSeleccionadasAdapter.EvidenciaItem(uri);
        item.setSubiendo(true);
        listaEvidencias.add(item);
        int pos = listaEvidencias.size() - 1;
        evidenciasAdapter.notifyItemInserted(pos);
        validarEstadoBotonGuardar();

        binding.tvEstadoSubidaFotos.setText("Subiendo evidencia fotográfica...");

        viewModel.subirEvidencia(uri, new AlertaViewModel.OnEvidenciaSubidaListener() {
            @Override
            public void onExito(String urlSubida) {
                item.setSubiendo(false);
                item.setUrlSubida(urlSubida);
                evidenciasAdapter.notifyItemChanged(pos);
                binding.tvEstadoSubidaFotos.setText("Foto subida exitosamente.");
                validarEstadoBotonGuardar();
            }

            @Override
            public void onError(String mensajeError) {
                item.setSubiendo(false);
                listaEvidencias.remove(item);
                evidenciasAdapter.notifyDataSetChanged();
                binding.tvEstadoSubidaFotos.setText("No fue posible subir la imagen. Intente nuevamente.");
                com.redalerta.client.util.AlertaMensajesHelper.mostrarError(binding.getRoot(), mensajeError);
                validarEstadoBotonGuardar();
            }
        });
    }

    private void precargarDatosOriginales() {
        if (alertaOriginal == null) return;

        if (alertaOriginal.getDescripcion() != null) {
            binding.etDescripcion.setText(alertaOriginal.getDescripcion());
        }

        UbicacionAlertaDto ub = alertaOriginal.getUbicacion();
        if (ub != null) {
            if (ub.getDepartamento() != null && !ub.getDepartamento().isEmpty()) {
                departamentoDetectado = ub.getDepartamento();
            }
            if (ub.getMunicipio() != null && !ub.getMunicipio().isEmpty()) {
                municipioDetectado = ub.getMunicipio();
            }
            if (ub.getDireccionDetallada() != null) {
                binding.etDireccionDetallada.setText(ub.getDireccionDetallada());
            }

            latitudConfirmada = ub.getLatitud() != null ? ub.getLatitud() : DEFAULT_LATITUD;
            longitudConfirmada = ub.getLongitud() != null ? ub.getLongitud() : DEFAULT_LONGITUD;
            altitudConfirmada = ub.getAltitud();
        } else {
            String dir = alertaOriginal.getDireccion() != null ? alertaOriginal.getDireccion() : "";
            binding.etDireccionDetallada.setText(dir);
            latitudConfirmada = DEFAULT_LATITUD;
            longitudConfirmada = DEFAULT_LONGITUD;
            altitudConfirmada = DEFAULT_ALTITUD;
        }

        actualizarTextoUbicacionDetectada();

        GeoPoint puntoInicial = new GeoPoint(latitudConfirmada, longitudConfirmada);
        binding.mapViewUbicacion.getController().setCenter(puntoInicial);
        actualizarTextoCoordenadas(latitudConfirmada, longitudConfirmada, altitudConfirmada);

        binding.cbAnonimo.setChecked(alertaOriginal.isEsAnonimo());

        // Precargar evidencias existentes
        if (alertaOriginal.getEvidencias() != null && !alertaOriginal.getEvidencias().isEmpty()) {
            for (EvidenciaResponse ev : alertaOriginal.getEvidencias()) {
                if (ev != null && ev.getUrl() != null && !ev.getUrl().isEmpty()) {
                    EvidenciasSeleccionadasAdapter.EvidenciaItem item = new EvidenciasSeleccionadasAdapter.EvidenciaItem(null);
                    item.setUrlSubida(ev.getUrl());
                    item.setSubiendo(false);
                    listaEvidencias.add(item);
                }
            }
            evidenciasAdapter.notifyDataSetChanged();
        } else if (alertaOriginal.getEvidenciaUrl() != null && !alertaOriginal.getEvidenciaUrl().isEmpty()) {
            EvidenciasSeleccionadasAdapter.EvidenciaItem item = new EvidenciasSeleccionadasAdapter.EvidenciaItem(null);
            item.setUrlSubida(alertaOriginal.getEvidenciaUrl());
            item.setSubiendo(false);
            listaEvidencias.add(item);
            evidenciasAdapter.notifyDataSetChanged();
        }

        validarEstadoBotonGuardar();
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                limpiarErrorCampo(binding.tilDescripcion);
                limpiarErrorCampo(binding.tilDireccionDetallada);
                validarEstadoBotonGuardar();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etDescripcion.addTextChangedListener(watcher);
        binding.etDireccionDetallada.addTextChangedListener(watcher);

        // Al hacer tap en la tarjeta de anonimato o en el checkbox
        binding.cardAnonimo.setOnClickListener(v -> binding.cbAnonimo.toggle());
        binding.cbAnonimo.setOnCheckedChangeListener((btn, checked) -> validarEstadoBotonGuardar());

        binding.btnCancelarModificacion.setOnClickListener(v -> cancelarYVolver());
        binding.btnGuardarModificacion.setOnClickListener(v -> procesarGuardarModificacion());
    }

    private void limpiarErrorCampo(com.google.android.material.textfield.TextInputLayout til) {
        if (til != null) {
            til.setError(null);
            til.setErrorEnabled(false);
        }
    }

    private void validarEstadoBotonGuardar() {
        if (binding == null) return;

        String desc = binding.etDescripcion.getText() != null ? binding.etDescripcion.getText().toString().trim() : "";
        boolean descValida = desc.length() >= 10 && desc.length() <= 1000;
        boolean ubValida = departamentoDetectado != null && !departamentoDetectado.isEmpty()
                && municipioDetectado != null && !municipioDetectado.isEmpty();

        boolean subiendo = false;
        for (EvidenciasSeleccionadasAdapter.EvidenciaItem item : listaEvidencias) {
            if (item.isSubiendo()) {
                subiendo = true;
                break;
            }
        }

        binding.btnGuardarModificacion.setEnabled(descValida && ubValida && !subiendo && !modoSeleccionMapaActivo);
    }

    private void procesarGuardarModificacion() {
        if (alertaOriginal == null || alertaOriginal.getId() == null) return;

        String desc = binding.etDescripcion.getText().toString().trim();
        String dir = binding.etDireccionDetallada.getText().toString().trim();

        if (desc.length() < 10) {
            binding.tilDescripcion.setError("La descripción debe tener mínimo 10 caracteres");
            return;
        }
        limpiarErrorCampo(binding.tilDescripcion);

        if (dir.isEmpty()) {
            dir = GeocodificacionHelper.DIRECCION_POR_DEFECTO_GPS;
            binding.etDireccionDetallada.setText(dir);
        }
        limpiarErrorCampo(binding.tilDireccionDetallada);

        List<String> evidencias = new ArrayList<>();
        for (EvidenciasSeleccionadasAdapter.EvidenciaItem item : listaEvidencias) {
            if (item.getUrlSubida() != null && !item.getUrlSubida().isEmpty()) {
                evidencias.add(item.getUrlSubida());
            }
        }

        UbicacionAlertaDto ubDto = new UbicacionAlertaDto(
                departamentoDetectado,
                municipioDetectado,
                dir,
                latitudConfirmada,
                longitudConfirmada,
                altitudConfirmada
        );
        ModificarAlertaRequest request = new ModificarAlertaRequest(
                desc,
                ubDto,
                evidencias,
                binding.cbAnonimo.isChecked()
        );

        try {
            long id = Long.parseLong(alertaOriginal.getId());
            binding.btnGuardarModificacion.setEnabled(false);
            viewModel.modificarAlerta(id, request);
        } catch (NumberFormatException e) {
            com.redalerta.client.util.AlertaMensajesHelper.mostrarError(
                    binding.getRoot(),
                    "ID de alerta no válido"
            );
        }
    }

    private void cancelarYVolver() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void observeViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            if (cargando != null) {
                binding.btnGuardarModificacion.setEnabled(!cargando);
                binding.btnGuardarModificacion.setText(cargando ? "Guardando cambios..." : "Guardar Cambios");
            }
        });

        viewModel.getAlertaModificada().observe(getViewLifecycleOwner(), alerta -> {
            if (alerta != null) {
                com.redalerta.client.util.AlertaMensajesHelper.mostrarExito(
                        requireActivity(),
                        "Alerta modificada exitosamente"
                );
                cancelarYVolver();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            binding.btnGuardarModificacion.setEnabled(true);
            if (msg != null && !msg.trim().isEmpty()) {
                com.redalerta.client.util.AlertaMensajesHelper.mostrarError(binding.getRoot(), msg);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisible(false);
        }
        if (binding != null) {
            binding.mapViewUbicacion.onResume();
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            binding.mapViewUbicacion.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        detenerActualizacionesUbicacion();
        if (binding != null) {
            binding.mapViewUbicacion.onDetach();
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisible(true);
        }
        super.onDestroyView();
        binding = null;
    }
}
