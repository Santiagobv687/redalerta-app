package com.redalerta.client.model;

import java.io.Serializable;
import java.util.List;

/**
 * Corresponde a PageAlertaResponseDto / ApiResponsePageAlertaResponseDto en OpenAPI.
 */
public class PageAlertaResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private long totalElements;
    private int totalPages;
    private int size;
    private int number;
    private boolean first;
    private boolean last;
    private List<Alerta> content;

    public PageAlertaResponse() {
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public List<Alerta> getContent() {
        return content;
    }

    public void setContent(List<Alerta> content) {
        this.content = content;
    }
}
