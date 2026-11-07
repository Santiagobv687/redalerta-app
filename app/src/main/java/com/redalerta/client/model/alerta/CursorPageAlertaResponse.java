package com.redalerta.client.model.alerta;

import java.io.Serializable;
import java.util.List;

/**
 * Corresponde a CursorPageResponseDtoAlertaResponseDto en OpenAPI (GET /api/v1/alertas/feed).
 * Estructura de respuesta paginada por cursor (Keyset Pagination) para scroll infinito.
 */
public class CursorPageAlertaResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Alerta> content;
    private Long nextCursor;
    private boolean hasNext;
    private int size;

    public CursorPageAlertaResponse() {
    }

    public List<Alerta> getContent() {
        return content;
    }

    public void setContent(List<Alerta> content) {
        this.content = content;
    }

    public Long getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(Long nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
