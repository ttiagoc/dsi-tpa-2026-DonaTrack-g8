package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoParada;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden")
    private Integer orden;

    @Column(name = "destino")
    private String destino;

    @Column(name = "entidad_id")
    private Long entidadId;

    // donde queda la sede de la entidad segun Georef (la manda donaciones al planificar). Nulas en paradas
    // viejas o si la entidad todavia no se pudo geocodificar: ahi se usa la aproximacion de GeocodificadorCaba
    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoParada estado = EstadoParada.PENDIENTE;

    @ElementCollection
    @CollectionTable(name = "parada_donaciones", joinColumns = @JoinColumn(name = "parada_id"))
    @Column(name = "donacion_id")
    private List<Long> donacionIds = new ArrayList<>();

    public Parada(Long id, Integer orden, String destino, Long entidadId, List<Long> donacionIds) {
        this(id, orden, destino, entidadId, null, null, EstadoParada.PENDIENTE,
                donacionIds != null ? donacionIds : new ArrayList<>());
    }

    /** Latitud y longitud de la parada: la real si se conoce; si no, una aproximada a partir de la direccion. */
    public double[] coordenadas() {
        if (latitud != null && longitud != null) {
            return new double[] { latitud, longitud };
        }
        return GeocodificadorCaba.ubicar(destino, entidadId);
    }

    public void marcarEntregada() {
        this.estado = EstadoParada.ENTREGADA;
    }

    public void marcarNoRecibida() {
        this.estado = EstadoParada.NO_RECIBIDA;
    }
}
