package com.redalerta.client.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redalerta.client.model.alerta.Alerta;

import java.util.Collections;
import java.util.List;

/**
 * Modelo de estado cerrado (Sealed Interface) para el Feed Comunitario de Alertas.
 * Permite retener el estado en memoria en AlertaViewModel y controlar la transición
 * inmediata entre el efecto Shimmer (Loading) y el listado (Success / Empty / Error).
 */
public sealed interface FeedUiState
        permits FeedUiState.Loading, FeedUiState.Success, FeedUiState.Error, FeedUiState.Empty {

    final class Loading implements FeedUiState {
        public static final Loading INSTANCE = new Loading();

        private Loading() {
        }
    }

    record Success(
            @NonNull List<Alerta> alertas,
            boolean hasNext,
            @Nullable Long nextCursor
    ) implements FeedUiState {
        public Success {
            alertas = alertas != null ? Collections.unmodifiableList(alertas) : Collections.emptyList();
        }
    }

    record Error(@NonNull String mensaje) implements FeedUiState {
        public Error {
            if (mensaje == null || mensaje.trim().isEmpty()) {
                mensaje = "No fue posible cargar las alertas comunitarias.";
            }
        }
    }

    final class Empty implements FeedUiState {
        public static final Empty INSTANCE = new Empty();

        private Empty() {
        }
    }
}
