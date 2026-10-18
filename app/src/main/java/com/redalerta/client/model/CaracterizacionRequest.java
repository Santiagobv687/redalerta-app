package com.redalerta.client.model;

import java.io.Serializable;

/**
 * Corresponde a CaracterizacionRequestDto / CaracterizacionResponseDto en OpenAPI.
 * Generos permitidos: "MASCULINO", "FEMENINO", "OTRO".
 * Formato fecha: "YYYY-MM-DD".
 */
public class CaracterizacionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fechaNacimiento;
    private String genero;
    private Boolean enGestacionLactancia = false;
    private Integer numeroIntegrantesFamilia = 1;
    private Boolean tieneMenoresDeEdad = false;
    private Integer cantidadMenores = 0;
    private Boolean tieneMiembroDiscapacidad = false;
    private String especificacionDiscapacidad;
    private Boolean perteneceGrupoEtnico = false;
    private String especificacionGrupoEtnico;

    public CaracterizacionRequest() {
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public Boolean getEnGestacionLactancia() {
        return enGestacionLactancia;
    }

    public void setEnGestacionLactancia(Boolean enGestacionLactancia) {
        this.enGestacionLactancia = enGestacionLactancia;
    }

    public Integer getNumeroIntegrantesFamilia() {
        return numeroIntegrantesFamilia;
    }

    public void setNumeroIntegrantesFamilia(Integer numeroIntegrantesFamilia) {
        this.numeroIntegrantesFamilia = numeroIntegrantesFamilia;
    }

    public Boolean getTieneMenoresDeEdad() {
        return tieneMenoresDeEdad;
    }

    public void setTieneMenoresDeEdad(Boolean tieneMenoresDeEdad) {
        this.tieneMenoresDeEdad = tieneMenoresDeEdad;
    }

    public Integer getCantidadMenores() {
        return cantidadMenores;
    }

    public void setCantidadMenores(Integer cantidadMenores) {
        this.cantidadMenores = cantidadMenores;
    }

    public Boolean getTieneMiembroDiscapacidad() {
        return tieneMiembroDiscapacidad;
    }

    public void setTieneMiembroDiscapacidad(Boolean tieneMiembroDiscapacidad) {
        this.tieneMiembroDiscapacidad = tieneMiembroDiscapacidad;
    }

    public String getEspecificacionDiscapacidad() {
        return especificacionDiscapacidad;
    }

    public void setEspecificacionDiscapacidad(String especificacionDiscapacidad) {
        this.especificacionDiscapacidad = especificacionDiscapacidad;
    }

    public Boolean getPerteneceGrupoEtnico() {
        return perteneceGrupoEtnico;
    }

    public void setPerteneceGrupoEtnico(Boolean perteneceGrupoEtnico) {
        this.perteneceGrupoEtnico = perteneceGrupoEtnico;
    }

    public String getEspecificacionGrupoEtnico() {
        return especificacionGrupoEtnico;
    }

    public void setEspecificacionGrupoEtnico(String especificacionGrupoEtnico) {
        this.especificacionGrupoEtnico = especificacionGrupoEtnico;
    }
}
