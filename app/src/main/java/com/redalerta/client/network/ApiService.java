package com.redalerta.client.network;

import com.redalerta.client.model.alerta.*;
import com.redalerta.client.model.auth.*;
import com.redalerta.client.model.common.*;
import com.redalerta.client.model.usuario.*;
import java.util.List;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ═══ Autenticación ═══
    @POST("auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("auth/registro")
    Call<ApiResponse<Usuario>> registrar(@Body RegistroRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();

    // ═══ Catálogos Públicos ═══
    @GET("catalogo/ubicaciones")
    Call<ApiResponse<List<DepartamentoResponse>>> obtenerUbicaciones();

    // ═══ Usuarios / Perfil ═══
    @GET("usuarios/perfil")
    Call<ApiResponse<Usuario>> obtenerPerfil();

    @PUT("usuarios/perfil")
    Call<ApiResponse<Usuario>> actualizarPerfil(@Body ActualizarPerfilRequest request);

    // ═══ Alertas ═══
    @GET("alertas")
    Call<ApiResponse<PageAlertaResponse>> listarAlertas(
            @Query("municipio") String municipio,
            @Query("estado") String estado,
            @Query("fechaInicio") String fechaInicio,
            @Query("fechaFin") String fechaFin,
            @Query("page") Integer page,
            @Query("size") Integer size,
            @Query("sort") String sort
    );

    @GET("alertas/mis-alertas")
    Call<ApiResponse<PageAlertaResponse>> listarMisAlertas(
            @Query("page") Integer page,
            @Query("size") Integer size,
            @Query("sort") String sort
    );

    @GET("alertas/{id}")
    Call<ApiResponse<Alerta>> obtenerAlertaPorId(@Path("id") long id);

    @POST("alertas")
    Call<ApiResponse<Alerta>> crearAlerta(@Body CrearAlertaRequest request);

    @PUT("alertas/{id}")
    Call<ApiResponse<Alerta>> modificarAlerta(@Path("id") long id, @Body ModificarAlertaRequest request);

    @DELETE("alertas/{id}")
    Call<ApiResponse<Void>> eliminarAlerta(@Path("id") long id);

    @Multipart
    @POST("alertas/evidencias/upload")
    Call<ApiResponse<UploadEvidenciaResponse>> subirEvidencia(@Part MultipartBody.Part archivo);

    // ═══ Flujo del Líder Comunitario ═══
    @GET("alertas/organizaciones-elegibles")
    Call<ApiResponse<List<OrganizacionResponse>>> obtenerOrganizacionesElegibles();

    @GET("alertas/lider/direccionadas")
    Call<ApiResponse<PageAlertaResponse>> listarAlertasDireccionadasLider(
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("alertas/{id}/direccionar")
    Call<ApiResponse<Alerta>> direccionarAlerta(
            @Path("id") long id,
            @Body DireccionarAlertaRequest request
    );

    @PUT("alertas/{id}/direccionamiento")
    Call<ApiResponse<Alerta>> modificarDireccionamiento(
            @Path("id") long id,
            @Body DireccionarAlertaRequest request
    );

    @POST("alertas/{id}/cancelar-direccionamiento")
    Call<ApiResponse<Alerta>> cancelarDireccionamiento(
            @Path("id") long id,
            @Body CancelarDireccionamientoRequest request
    );

    // ═══ Flujo de la Organización / Entidad de Socorro ═══
    @GET("alertas/organizacion/bandeja")
    Call<ApiResponse<PageAlertaResponse>> listarBandejaOrganizacion(
            @Query("page") Integer page,
            @Query("size") Integer size,
            @Query("sort") String sort
    );
}
