package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import ar.edu.utn.frba.ddsi.donaciones.dto.importacion.FilaErrorDto;
import ar.edu.utn.frba.ddsi.donaciones.dto.importacion.ImportacionResponse;

public class ImportacionTarea {

    private final String id;
    private final String nombreArchivo;
    private final String fecha;
    private volatile String estado; // EN_PROCESO, COMPLETADA, FALLIDA
    private final int totalFilas;
    private final AtomicInteger procesadas = new AtomicInteger();
    private final AtomicInteger creados = new AtomicInteger();
    private final AtomicInteger actualizados = new AtomicInteger();
    private final AtomicInteger descartadas = new AtomicInteger();
    private final List<FilaErrorDto> errores = new CopyOnWriteArrayList<>();

    public ImportacionTarea(String id, String nombreArchivo, String fecha, String estado, int totalFilas) {
        this.id = id;
        this.nombreArchivo = nombreArchivo;
        this.fecha = fecha;
        this.estado = estado;
        this.totalFilas = totalFilas;
    }

    public ImportacionTarea(String id, String nombreArchivo, String fecha, String estado,
                            int totalFilas, int procesadas, int creados, int actualizados, int descartadas) {
        this.id = id;
        this.nombreArchivo = nombreArchivo;
        this.fecha = fecha;
        this.estado = estado;
        this.totalFilas = totalFilas;
        this.procesadas.set(procesadas);
        this.creados.set(creados);
        this.actualizados.set(actualizados);
        this.descartadas.set(descartadas);
    }

    public String getId() {
        return id;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getFecha() {
        return fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getTotalFilas() {
        return totalFilas;
    }

    public int getProcesadas() {
        return procesadas.get();
    }

    public int getCreados() {
        return creados.get();
    }

    public int getActualizados() {
        return actualizados.get();
    }

    public int getDescartadas() {
        return descartadas.get();
    }

    public List<FilaErrorDto> getErrores() {
        return errores;
    }

    public void incrementarProcesadas() {
        procesadas.incrementAndGet();
    }

    public void incrementarCreados() {
        creados.incrementAndGet();
    }

    public void incrementarActualizados() {
        actualizados.incrementAndGet();
    }

    public void incrementarDescartadas() {
        descartadas.incrementAndGet();
    }

    public void agregarError(FilaErrorDto error) {
        if (errores.size() < 100) {
            errores.add(error);
        }
    }

    public ImportacionResponse toResponse() {
        int proc = procesadas.get();
        int pct;
        if (totalFilas > 0) {
            pct = (int) Math.min(100, Math.round(((double) proc / totalFilas) * 100));
        } else {
            pct = "COMPLETADA".equalsIgnoreCase(estado) ? 100 : 0;
        }
        return new ImportacionResponse(
                id,
                nombreArchivo,
                fecha,
                estado,
                totalFilas,
                proc,
                creados.get(),
                actualizados.get(),
                descartadas.get(),
                pct,
                new ArrayList<>(errores)
        );
    }
}

