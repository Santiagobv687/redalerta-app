package com.redalerta.client.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    public static String obtenerTiempoTranscurrido(String fechaIso) {
        if (fechaIso == null || fechaIso.trim().isEmpty()) {
            return "Hace un momento";
        }

        try {
            SimpleDateFormat format;
            if (fechaIso.contains("T")) {
                format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", new Locale("es", "CO"));
            } else if (fechaIso.contains("-") && fechaIso.contains(":")) {
                format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("es", "CO"));
            } else {
                format = new SimpleDateFormat("yyyy-MM-dd - HH:mm", new Locale("es", "CO"));
            }

            Date fechaEv = format.parse(fechaIso);
            if (fechaEv == null) return fechaIso;

            long diffMillis = System.currentTimeMillis() - fechaEv.getTime();
            if (diffMillis < 0) diffMillis = 0;

            long min = diffMillis / (60 * 1000);
            long horas = diffMillis / (60 * 60 * 1000);
            long dias = diffMillis / (24 * 60 * 60 * 1000);

            if (min < 1) {
                return "Hace un momento";
            } else if (min < 60) {
                return "Hace " + min + " min";
            } else if (horas < 24) {
                return "Hace " + horas + (horas == 1 ? " hora" : " horas");
            } else if (dias < 7) {
                return "Hace " + dias + (dias == 1 ? " día" : " días");
            } else {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, yyyy", new Locale("es", "CO"));
                return outputFormat.format(fechaEv);
            }
        } catch (Exception e) {
            return fechaIso;
        }
    }
}
