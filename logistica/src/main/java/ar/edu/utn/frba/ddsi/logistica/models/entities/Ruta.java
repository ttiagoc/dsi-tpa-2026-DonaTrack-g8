package ar.edu.utn.frba.ddsi.logistica.models.entities;

import java.time.LocalDate;
import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoRuta;
import lombok.Data;

@Data
public class Ruta {
    private Long id;
    private LocalDate fecha;
    private EstadoRuta estado;
    private Camion camion;
    private Chofer chofer;
    private List<Parada> paradas;
    private Ubicacion ultimaUbicacion;

    public void actualizarUbicacion(Ubicacion nuevaUbicacion) {
    }

    public void iniciar() {
    }

    public void finalizar() {
    }
}
