package ar.edu.utn.frba.ddsi.logistica.models.entities;

import java.time.LocalDate;
import java.util.List;

import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Ruta {
    private Long id;
    private LocalDate fecha;
    private EstadoRuta estado;
    private Camion camion;
    private Chofer chofer;
    private List<Parada> paradas;
    private Ubicacion ultimaUbicacion;

    public Ruta(LocalDate fecha, Camion camion, List<Parada> paradas) {
        this.id = null;
        this.fecha = fecha;
        this.estado = EstadoRuta.PLANIFICADA;
        this.camion = camion;
        this.paradas = paradas;
    }

    public void actualizarUbicacion(Ubicacion nuevaUbicacion) {
        this.ultimaUbicacion = nuevaUbicacion;
    }

    public void iniciar() {
        this.estado = EstadoRuta.EN_TRASLADO;
    }

    public void finalizar() {
        this.estado = EstadoRuta.FINALIZADA;
    }
}
