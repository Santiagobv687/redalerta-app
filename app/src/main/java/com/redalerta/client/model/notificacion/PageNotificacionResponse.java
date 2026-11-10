package com.redalerta.client.model.notificacion;

import java.io.Serializable;
import java.util.List;

/**
 * Corresponde a PageNotificacionResponseDto en OpenAPI.
 */
public class PageNotificacionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private long totalElements;
    private int totalPages;
    private int size;
    private int number;
    private boolean first;
    private boolean last;
    private boolean empty;
    private int numberOfElements;
    private List<NotificacionResponse> content;

    public PageNotificacionResponse() {
    }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }

    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }

    public int getNumberOfElements() { return numberOfElements; }
    public void setNumberOfElements(int numberOfElements) { this.numberOfElements = numberOfElements; }

    public List<NotificacionResponse> getContent() { return content; }
    public void setContent(List<NotificacionResponse> content) { this.content = content; }
}
