package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ruta")
@Data
@NoArgsConstructor
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoRuta estado;

    @ManyToOne
    @JoinColumn(name = "camion_id")
    private Camion camion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ruta_id")
    @OrderBy("orden ASC")
    private List<Parada> paradas = new ArrayList<>();

    public Ruta(LocalDate fecha, Camion camion, List<Parada> paradas) {
        this.id = null;
        this.fecha = fecha;
        this.estado = EstadoRuta.PLANIFICADA;
        this.camion = camion;
        this.paradas = (paradas != null) ? new ArrayList<>(paradas) : new ArrayList<>();
    }

    public void iniciar() {
        this.estado = EstadoRuta.EN_TRASLADO;
    }

    public void finalizar() {
        this.estado = EstadoRuta.FINALIZADA;
    }
}
