package com.redalerta.client.network;

import com.redalerta.client.model.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ═══ Auth ═══
    @POST("api/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<ApiResponse<Usuario>> registrar(@Body RegistroRequest request);

    // ═══ Alertas ═══
    @POST("api/alertas")
    Call<ApiResponse<Alerta>> crearAlerta(@Body CrearAlertaRequest request);
}