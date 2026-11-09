package com.redalerta.client.ui.alert;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.redalerta.client.R;
import com.redalerta.client.databinding.FragmentDetalleAlertaBinding;
import com.redalerta.client.model.alerta.Alerta;
import com.redalerta.client.model.alerta.EvidenciaResponse;
import com.redalerta.client.model.alerta.UbicacionAlertaDto;
import com.redalerta.client.util.SessionManager;
import com.redalerta.client.util.TimeUtils;
import com.redalerta.client.viewmodel.AlertaLiderViewModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class DetalleAlertaFragment extends Fragment {

    public static final String ARG_ALERTA = "arg_alerta";

    private static final double DEFAULT_LATITUD = 4.533889;
    private static final double DEFAULT_LONGITUD = -75.681111;
    private static final double DEFAULT_ZOOM = 17.0;

    private FragmentDetalleAlertaBinding binding;
    private Alerta alerta;
    private SessionManager sessionManager;
    private AlertaLiderViewModel viewModel;

    private Marker marcadorIncidente;
    private GeoPoint puntoIncidenteActual = new GeoPoint(DEFAULT_LATITUD, DEFAULT_LONGITUD);

    public static DetalleAlertaFragment newInstance(Alerta alerta) {
        DetalleAlertaFragment fragment = new DetalleAlertaFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALERTA, alerta);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        binding = FragmentDetalleAlertaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(AlertaLiderViewModel.class);

        if (getArguments() != null) {
            alerta = (Alerta) getArguments().getSerializable(ARG_ALERTA);
        }

        binding.toolbarDetalle.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        configurarMapaVisualizacion();
        poblarDatos();
        configurarAccionesPorRol();
        observeViewModel();

        // Consultar el detalle fresco desde el servidor mediante GET /api/v1/alertas/{id}
        if (alerta != null && alerta.getId() != null) {
            try {
                long id = Long.parseLong(alerta.getId());
                viewModel.cargarDetalleAlerta(id);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Configura el mapa en modo solo visualización:
     * - No solicita permisos de ubicación del dispositivo.
     * - El marcador permanece fijo en la coordenada del incidente (no arrastrable).
     * - Permite desplazarse y hacer zoom libremente para consultar puntos de referencia cercanos.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void configurarMapaVisualizacion() {
        binding.mapViewDetalle.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapViewDetalle.setMultiTouchControls(true);
        binding.mapViewDetalle.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        binding.mapViewDetalle.getController().setZoom(DEFAULT_ZOOM);

        // Permitir navegar por el mapa sin que el NestedScrollView intercepte el desplazamiento
        binding.mapViewDetalle.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                binding.nestedScrollDetalleAlerta.requestDisallowInterceptTouchEvent(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                binding.nestedScrollDetalleAlerta.requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        marcadorIncidente = new Marker(binding.mapViewDetalle);
        marcadorIncidente.setDraggable(false);
        marcadorIncidente.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        Drawable iconoPin = obtenerIconoPinEscalado();
        if (iconoPin != null) {
            marcadorIncidente.setIcon(iconoPin);
        }
        // Al tocar el marcador, centrar suavemente sin abrir burbuja flotante
        marcadorIncidente.setOnMarkerClickListener((marker, mapView) -> {
            mapView.getController().animateTo(marker.getPosition());
            return true;
        });
        binding.mapViewDetalle.getOverlays().add(marcadorIncidente);

        binding.btnCentrarMapaDetalle.setOnClickListener(v -> {
            if (binding != null && puntoIncidenteActual != null) {
                binding.mapViewDetalle.getController().setZoom(DEFAULT_ZOOM);
                binding.mapViewDetalle.getController().animateTo(puntoIncidenteActual);
            }
        });
    }

    @Nullable
    private Drawable obtenerIconoPinEscalado() {
        Drawable vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pin);
        if (vectorDrawable == null) return null;

        int sizePx = (int) (40 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, sizePx, sizePx);
        vectorDrawable.draw(canvas);
        return new BitmapDrawable(getResources(), bitmap);
    }

    private void actualizarMapaUbicacionAlerta() {
        if (binding == null || alerta == null) return;

        UbicacionAlertaDto ub = alerta.getUbicacion();
        double lat = (ub != null && ub.getLatitud() != null) ? ub.getLatitud() : DEFAULT_LATITUD;
        double lon = (ub != null && ub.getLongitud() != null) ? ub.getLongitud() : DEFAULT_LONGITUD;
        Double alt = (ub != null) ? ub.getAltitud() : null;

        puntoIncidenteActual = new GeoPoint(lat, lon);
        if (marcadorIncidente != null) {
            marcadorIncidente.setPosition(puntoIncidenteActual);
        }
        binding.mapViewDetalle.getController().setCenter(puntoIncidenteActual);
        binding.mapViewDetalle.invalidate();

        String textoCoord = String.format(Locale.US, "Lat: %.6f, Lon: %.6f", lat, lon);
        if (alt != null) {
            textoCoord += String.format(Locale.US, " • Alt: %.0f m", alt);
        }
        binding.tvCoordenadasDetalle.setText(textoCoord);
    }

    private void poblarDatos() {
        if (alerta == null) return;

        String fecha = alerta.getFechaCreacion() != null ? alerta.getFechaCreacion() : alerta.getFechaActualizacion();
        binding.tvFechaAlerta.setText(TimeUtils.obtenerTiempoTranscurrido(fecha));

        configurarEstadoAccesible(alerta.getEstado());

        binding.tvTituloDetalle.setText(alerta.getTitulo());
        binding.tvDescripcionDetalle.setText(alerta.getDescripcion());

        UbicacionAlertaDto ub = alerta.getUbicacion();
        String depto = (ub != null && ub.getDepartamento() != null && !ub.getDepartamento().isEmpty())
                ? ub.getDepartamento()
                : "";
        String mun = alerta.getBarrio() != null ? alerta.getBarrio() : "Armenia";
        String dir = alerta.getDireccion() != null ? alerta.getDireccion() : "";

        StringBuilder ubicacionTexto = new StringBuilder();
        if (!depto.isEmpty()) {
            ubicacionTexto.append(mun).append(", ").append(depto);
        } else {
            ubicacionTexto.append(mun);
        }
        if (!dir.isEmpty() && !"—".equals(dir)) {
            ubicacionTexto.append(" • ").append(dir);
        }
        binding.tvUbicacionDetalle.setText(ubicacionTexto.toString());

        actualizarMapaUbicacionAlerta();

        String autor = alerta.isEsAnonima() ? "Anónimo" : alerta.getUsuarioNombre();
        if (autor == null || autor.isEmpty()) autor = "Ciudadano";
        binding.tvAutorDetalle.setText(autor);

        // Evidencias
        binding.layoutEvidencias.removeAllViews();
        boolean tieneFotos = false;
        if (alerta.getEvidenciaUrl() != null && !alerta.getEvidenciaUrl().isEmpty()) {
            agregarFoto(alerta.getEvidenciaUrl());
            tieneFotos = true;
        }
        if (alerta.getEvidencias() != null && !alerta.getEvidencias().isEmpty()) {
            for (EvidenciaResponse e : alerta.getEvidencias()) {
                if (e.getUrl() != null && !e.getUrl().equals(alerta.getEvidenciaUrl())) {
                    agregarFoto(e.getUrl());
                    tieneFotos = true;
                }
            }
        }

        if (tieneFotos) {
            binding.tvEvidenciasLabel.setVisibility(View.VISIBLE);
            binding.scrollEvidencias.setVisibility(View.VISIBLE);
        } else {
            binding.tvEvidenciasLabel.setVisibility(View.GONE);
            binding.scrollEvidencias.setVisibility(View.GONE);
        }

        // Evaluación del Líder Comunitario y Seguimiento Operativo (Si existe)
        String lider = alerta.getLiderNombre() != null ? alerta.getLiderNombre() : null;
        String obsTech = alerta.getObservacionTecnica() != null ? alerta.getObservacionTecnica() : null;
        String orgNombre = alerta.getOrganizacionNombre() != null ? alerta.getOrganizacionNombre() : null;
        String obsInicial = alerta.getObservacionInicial() != null ? alerta.getObservacionInicial() : null;
        String informeRes = alerta.getInformeResolucion() != null ? alerta.getInformeResolucion() : null;
        String motivoRechazo = alerta.getMotivoRechazo() != null ? alerta.getMotivoRechazo() : null;

        if (lider != null || obsTech != null || orgNombre != null || obsInicial != null || informeRes != null || motivoRechazo != null) {
            binding.cardEvaluacionLider.setVisibility(View.VISIBLE);
            if (lider != null) {
                binding.tvLiderRemitente.setText("Remitido por: " + lider);
            } else {
                binding.tvLiderRemitente.setText("Evaluado por Líder Comunitario");
            }

            String cat = alerta.getCategoria() != null ? alerta.getCategoria().replace("_", " ").toUpperCase() : "GENERAL";
            String prio = alerta.getPrioridad() != null ? alerta.getPrioridad().toUpperCase() : "NO DEFINIDA";
            String catPrioText = "Categoría: " + cat + " • Prioridad: " + prio;
            if (orgNombre != null) {
                catPrioText += "\nAsignado a: " + orgNombre;
            }
            binding.tvLiderCategoriaPrioridad.setText(catPrioText);

            StringBuilder notasDetalle = new StringBuilder();
            if (obsTech != null && !obsTech.trim().isEmpty()) {
                notasDetalle.append("Observación técnica: ").append(obsTech.trim());
            }
            if (obsInicial != null && !obsInicial.trim().isEmpty()) {
                if (notasDetalle.length() > 0) notasDetalle.append("\n\n");
                notasDetalle.append("Nota de inicio de gestión: ").append(obsInicial.trim());
            }
            if (informeRes != null && !informeRes.trim().isEmpty()) {
                if (notasDetalle.length() > 0) notasDetalle.append("\n\n");
                notasDetalle.append("Informe de resolución: ").append(informeRes.trim());
            }
            if (motivoRechazo != null && !motivoRechazo.trim().isEmpty()) {
                if (notasDetalle.length() > 0) notasDetalle.append("\n\n");
                notasDetalle.append("Motivo de rechazo: ").append(motivoRechazo.trim());
            }

            if (notasDetalle.length() > 0) {
                binding.tvLiderObservacionTecnica.setVisibility(View.VISIBLE);
                binding.tvLiderObservacionTecnica.setText(notasDetalle.toString());
            } else {
                binding.tvLiderObservacionTecnica.setVisibility(View.GONE);
            }
        } else {
            binding.cardEvaluacionLider.setVisibility(View.GONE);
        }
    }

    private void agregarFoto(String url) {
        ImageView img = new ImageView(requireContext());
        int size = (int) (120 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd(margin);
        img.setLayoutParams(params);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setClipToOutline(true);
        img.setBackgroundResource(R.drawable.bg_status_revision);

        Glide.with(this).load(url).into(img);

        // Al presionar la miniatura, se muestra la foto en grande a pantalla completa
        img.setOnClickListener(v -> mostrarImagenEnGrande(url));

        binding.layoutEvidencias.addView(img);
    }

    private void mostrarImagenEnGrande(String urlImagen) {
        if (urlImagen == null || urlImagen.trim().isEmpty()) return;

        android.app.Dialog dialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_visualizador_imagen, null);
        dialog.setContentView(dialogView);

        ImageView imgGrande = dialogView.findViewById(R.id.imgFotoGrande);
        View pbCarga = dialogView.findViewById(R.id.pbCargaImagen);
        View btnCerrar = dialogView.findViewById(R.id.btnCerrarVisor);
        View container = dialogView.findViewById(R.id.containerVisorImagen);

        if (pbCarga != null) pbCarga.setVisibility(View.VISIBLE);

        Glide.with(requireContext())
                .load(urlImagen.trim())
                .fitCenter()
                .placeholder(R.drawable.ic_photo_library)
                .error(R.drawable.ic_photo_library)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        if (pbCarga != null) pbCarga.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                  Target<Drawable> target, DataSource dataSource,
                                                  boolean isFirstResource) {
                        if (pbCarga != null) pbCarga.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(imgGrande);

        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> dialog.dismiss());
        }

        if (container != null) {
            container.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void configurarEstadoAccesible(String estado) {
        String estadoNorm = estado != null ? estado.toUpperCase() : "PENDIENTE_REVISION";
        switch (estadoNorm) {
            case "DIRECCIONADA" -> {
                binding.tvEstadoBadgeDetalle.setText("DIRECCIONADA");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_verificada);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_status_verificada_text));
            }
            case "EN_GESTION", "EN_ATENCION" -> {
                binding.tvEstadoBadgeDetalle.setText("EN GESTIÓN");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_atencion);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_status_atencion_text));
            }
            case "RESUELTA" -> {
                binding.tvEstadoBadgeDetalle.setText("RESUELTA");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_resuelta);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_status_resuelta_text));
            }
            case "RECHAZADA" -> {
                binding.tvEstadoBadgeDetalle.setText("RECHAZADA");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_pendiente);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_status_pendiente_text));
            }
            default -> {
                binding.tvEstadoBadgeDetalle.setText("EN REVISIÓN");
                binding.tvEstadoBadgeDetalle.setBackgroundResource(R.drawable.bg_status_revision);
                binding.tvEstadoBadgeDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.ra_status_revision_text));
            }
        }
    }

    private void configurarAccionesPorRol() {
        if (alerta == null || binding == null) return;
        String rol = sessionManager.getRol();
        String estado = alerta.getEstado() != null ? alerta.getEstado().toUpperCase() : "";

        boolean esLider = "ROLE_LIDER".equalsIgnoreCase(rol) || "ROLE_ADMIN".equalsIgnoreCase(rol) || "LIDER".equalsIgnoreCase(rol) || "ADMINISTRADOR".equalsIgnoreCase(rol);
        boolean esOrganizacion = "ROLE_ORGANIZACION".equalsIgnoreCase(rol) || "ORGANIZACION".equalsIgnoreCase(rol);

        if (esLider) {
            binding.layoutBannerOrganizacion.setVisibility(View.GONE);
            binding.btnIniciarGestion.setVisibility(View.GONE);
            binding.btnResolverAlerta.setVisibility(View.GONE);
            binding.btnRechazarAlerta.setVisibility(View.GONE);

            binding.layoutAccionesLider.setVisibility(View.VISIBLE);

            if (estado.equals("PENDIENTE_REVISION") || estado.equals("RECHAZADA")) {
                binding.btnEvaluarDireccionar.setVisibility(View.VISIBLE);
                binding.btnModificarDireccionamiento.setVisibility(View.GONE);
                binding.btnCancelarDireccionamiento.setVisibility(View.GONE);

                binding.btnEvaluarDireccionar.setOnClickListener(v -> mostrarBottomSheet(false));
            } else if (estado.equals("DIRECCIONADA") || estado.equals("EN_GESTION")) {
                binding.btnEvaluarDireccionar.setVisibility(View.GONE);
                binding.btnModificarDireccionamiento.setVisibility(View.VISIBLE);
                binding.btnCancelarDireccionamiento.setVisibility(View.VISIBLE);

                binding.btnModificarDireccionamiento.setOnClickListener(v -> mostrarBottomSheet(true));
                binding.btnCancelarDireccionamiento.setOnClickListener(v -> solicitarMotivoCancelacion());
            } else {
                binding.layoutAccionesLider.setVisibility(View.GONE);
            }
        } else if (esOrganizacion) {
            binding.layoutAccionesLider.setVisibility(View.GONE);

            if (estado.equals("DIRECCIONADA")) {
                binding.layoutBannerOrganizacion.setVisibility(View.VISIBLE);
                binding.tvEstadoOperativoOrganizacion.setText("Caso asignado a su entidad para atención operativa");

                binding.btnIniciarGestion.setVisibility(View.VISIBLE);
                binding.btnResolverAlerta.setVisibility(View.GONE);
                binding.btnRechazarAlerta.setVisibility(View.VISIBLE);

                binding.btnIniciarGestion.setOnClickListener(v -> solicitarIniciarGestion());
                binding.btnRechazarAlerta.setOnClickListener(v -> solicitarRechazarAlerta());
            } else if (estado.equals("EN_GESTION") || estado.equals("EN_ATENCION")) {
                binding.layoutBannerOrganizacion.setVisibility(View.VISIBLE);
                binding.tvEstadoOperativoOrganizacion.setText("Atención comunitaria en terreno en curso");

                binding.btnIniciarGestion.setVisibility(View.GONE);
                binding.btnResolverAlerta.setVisibility(View.VISIBLE);
                binding.btnRechazarAlerta.setVisibility(View.VISIBLE);

                binding.btnResolverAlerta.setOnClickListener(v -> solicitarResolverAlerta());
                binding.btnRechazarAlerta.setOnClickListener(v -> solicitarRechazarAlerta());
            } else {
                // RESUELTA, RECHAZADA u otro estado finalizado: ocultar todos los botones de acción
                binding.layoutBannerOrganizacion.setVisibility(View.GONE);
                binding.btnIniciarGestion.setVisibility(View.GONE);
                binding.btnResolverAlerta.setVisibility(View.GONE);
                binding.btnRechazarAlerta.setVisibility(View.GONE);
            }
        } else {
            // Para otros roles (como CIUDADANO común): ocultar todos los botones de acción
            binding.layoutAccionesLider.setVisibility(View.GONE);
            binding.layoutBannerOrganizacion.setVisibility(View.GONE);
            binding.btnIniciarGestion.setVisibility(View.GONE);
            binding.btnResolverAlerta.setVisibility(View.GONE);
            binding.btnRechazarAlerta.setVisibility(View.GONE);
        }
    }

    private void mostrarBottomSheet(boolean esModificacion) {
        DireccionarAlertaBottomSheet bs = DireccionarAlertaBottomSheet.newInstance(alerta, esModificacion);
        bs.show(getParentFragmentManager(), "BottomSheetTriaje");
    }

    private void solicitarMotivoCancelacion() {
        com.redalerta.client.util.AlertaMensajesHelper.mostrarDialogoCancelarDireccionamiento(
                requireContext(),
                motivo -> {
                    try {
                        long id = Long.parseLong(alerta.getId());
                        viewModel.cancelarDireccionamiento(id, motivo);
                    } catch (Exception ignored) {}
                }
        );
    }

    private void solicitarIniciarGestion() {
        com.redalerta.client.util.AlertaMensajesHelper.mostrarDialogoIniciarGestion(
                requireContext(),
                observacionInicial -> {
                    try {
                        long id = Long.parseLong(alerta.getId());
                        viewModel.iniciarGestionAlerta(id, observacionInicial);
                    } catch (Exception ignored) {}
                }
        );
    }

    private void solicitarResolverAlerta() {
        com.redalerta.client.util.AlertaMensajesHelper.mostrarDialogoResolverAlerta(
                requireContext(),
                informeResolucion -> {
                    try {
                        long id = Long.parseLong(alerta.getId());
                        viewModel.resolverAlerta(id, informeResolucion);
                    } catch (Exception ignored) {}
                }
        );
    }

    private void solicitarRechazarAlerta() {
        com.redalerta.client.util.AlertaMensajesHelper.mostrarDialogoRechazarAlerta(
                requireContext(),
                motivo -> {
                    try {
                        long id = Long.parseLong(alerta.getId());
                        viewModel.rechazarAlerta(id, motivo);
                    } catch (Exception ignored) {}
                }
        );
    }

    private void actualizarEstadoCargaBotones(boolean cargando) {
        if (binding == null) return;
        binding.progressAccionLider.setVisibility(cargando ? View.VISIBLE : View.GONE);
        binding.progressAccionOrganizacion.setVisibility(cargando ? View.VISIBLE : View.GONE);

        binding.btnEvaluarDireccionar.setEnabled(!cargando);
        binding.btnModificarDireccionamiento.setEnabled(!cargando);
        binding.btnCancelarDireccionamiento.setEnabled(!cargando);

        binding.btnIniciarGestion.setEnabled(!cargando);
        binding.btnResolverAlerta.setEnabled(!cargando);
        binding.btnRechazarAlerta.setEnabled(!cargando);
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean enCurso = (state == AlertaLiderViewModel.UiState.LOADING);
            actualizarEstadoCargaBotones(enCurso);
        });

        viewModel.getAlertaDetalle().observe(getViewLifecycleOwner(), al -> {
            if (al != null && alerta != null && al.getId() != null && al.getId().equals(alerta.getId())) {
                this.alerta = al;
                poblarDatos();
                configurarAccionesPorRol();
            }
        });

        viewModel.getAlertaDireccionada().observe(getViewLifecycleOwner(), al -> {
            if (al != null && (alerta == null || alerta.getId() == null || al.getId() == null || al.getId().equals(alerta.getId()))) {
                this.alerta = al;
                poblarDatos();
                configurarAccionesPorRol();
            }
        });

        viewModel.getMensajeExito().observe(getViewLifecycleOwner(), msgExito -> {
            if (msgExito != null && !msgExito.isEmpty()) {
                viewModel.limpiarMensajeExito();
                com.redalerta.client.util.AlertaMensajesHelper.mostrarExito(
                        binding.getRoot(),
                        msgExito
                );
                // Refrescar la bandeja de la organización en segundo plano si aplica
                try {
                    new ViewModelProvider(requireActivity())
                            .get(com.redalerta.client.viewmodel.OrganizacionBandejaViewModel.class)
                            .cargarBandejaOrganizacion();
                } catch (Exception ignored) {}
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                viewModel.limpiarError();
                com.redalerta.client.util.AlertaMensajesHelper.mostrarError(binding.getRoot(), errorMsg);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            binding.mapViewDetalle.onResume();
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            binding.mapViewDetalle.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.mapViewDetalle.onDetach();
        }
        super.onDestroyView();
        binding = null;
    }
}
