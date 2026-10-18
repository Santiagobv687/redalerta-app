package com.redalerta.client.model.auth;

import com.redalerta.client.model.usuario.*;

import java.io.Serializable;

/**
 * Corresponde a RegistroRequestDto en OpenAPI.
 */
public class RegistroRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nombre;
    private DocumentoRequest documento = new DocumentoRequest();
    private UbicacionRequest ubicacion = new UbicacionRequest();
    private String email;
    private String password;
    private String telefono;
    private CaracterizacionRequest caracterizacion = new CaracterizacionRequest();

    // Campos temporales UI
    private transient String apellido;
    private transient String localidad;
    private transient String barrio;
    private transient String direccion;
    private transient String rol;

    public RegistroRequest() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
        if (this.nombre != null && apellido != null && !this.nombre.contains(apellido)) {
            this.nombre = (this.nombre.trim() + " " + apellido.trim()).trim();
        }
    }

    public DocumentoRequest getDocumento() {
        return documento;
    }

    public void setDocumento(DocumentoRequest documento) {
        this.documento = documento;
    }

    public UbicacionRequest getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(UbicacionRequest ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCorreo() {
        return email;
    }

    public void setCorreo(String correo) {
        this.email = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getContrasena() {
        return password;
    }

    public void setContrasena(String contrasena) {
        this.password = contrasena;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public CaracterizacionRequest getCaracterizacion() {
        return caracterizacion;
    }

    public void setCaracterizacion(CaracterizacionRequest caracterizacion) {
        this.caracterizacion = caracterizacion;
    }

    // Helpers UI
    public String getTipoDocumento() {
        return documento != null ? documento.getTipo() : null;
    }

    public void setTipoDocumento(String tipoDocumento) {
        if (this.documento == null) this.documento = new DocumentoRequest();
        if (tipoDocumento == null) return;
        String t = tipoDocumento.toUpperCase();
        if (t.contains("CE") || t.contains("EXTRANJERÍA") || t.contains("EXTRANJERIA") || t.contains("TARJETA_EXTRANJERIA")) {
            this.documento.setTipo("TARJETA_EXTRANJERIA");
        } else if (t.contains("PASAPORTE") || t.contains("PAS")) {
            this.documento.setTipo("PASAPORTE");
        } else {
            this.documento.setTipo("CEDULA");
        }
    }

    public String getNumeroDocumento() {
        return documento != null ? documento.getNumero() : null;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        if (this.documento == null) this.documento = new DocumentoRequest();
        this.documento.setNumero(numeroDocumento);
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
        if (this.ubicacion == null) this.ubicacion = new UbicacionRequest();
        this.ubicacion.setMunicipio(localidad);
        if (this.ubicacion.getDepartamento() == null) {
            this.ubicacion.setDepartamento("Quindío");
        }
    }

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
        actualizarDireccionResidencia();
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
        actualizarDireccionResidencia();
    }

    private void actualizarDireccionResidencia() {
        if (this.ubicacion == null) this.ubicacion = new UbicacionRequest();
        String dirFinal = (direccion != null ? direccion : "") + (barrio != null ? ", " + barrio : "");
        this.ubicacion.setDireccionResidencia(dirFinal.trim());
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
