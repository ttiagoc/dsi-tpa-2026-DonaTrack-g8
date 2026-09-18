package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "camion")
@Getter
@Setter
@NoArgsConstructor
public class Camion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patente", length = 20)
    private String patente;

    @Column(name = "capacidad_volumen")
    private Double capacidadVolumen;

    @Column(name = "altura")
    private Double altura;

    @Column(name = "capacidad_carga")
    private Double capacidadCarga;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "nombre", column = @Column(name = "chofer_nombre")),
        @AttributeOverride(name = "apellido", column = @Column(name = "chofer_apellido"))
    })
    private Chofer chofer;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitud", column = @Column(name = "ubicacion_latitud")),
        @AttributeOverride(name = "longitud", column = @Column(name = "ubicacion_longitud")),
        @AttributeOverride(name = "timestamp", column = @Column(name = "ubicacion_timestamp")),
        @AttributeOverride(name = "velocidad", column = @Column(name = "ubicacion_velocidad"))
    })
    private Ubicacion ubicacion;

    public Camion(String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, Chofer chofer) {
        this.id = null;
        this.patente = patente;
        this.capacidadVolumen = capacidadVolumen;
        this.altura = altura;
        this.capacidadCarga = capacidadCarga;
        this.chofer = chofer;
    }

    public void actualizarUbicacion(Ubicacion nuevaUbicacion) {
        this.ubicacion = nuevaUbicacion;
    }
}
