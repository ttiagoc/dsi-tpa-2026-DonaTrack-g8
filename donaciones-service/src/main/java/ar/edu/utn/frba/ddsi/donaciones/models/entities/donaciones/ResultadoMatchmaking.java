package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resultado_matchmaking")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultadoMatchmaking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "donacion_id", nullable = false)
    private Donacion donacion;

    @ManyToMany
    @JoinTable(name = "resultado_matchmaking_entidades_sugeridas",
            joinColumns = @JoinColumn(name = "resultado_matchmaking_id"),
            inverseJoinColumns = @JoinColumn(name = "entidad_beneficiaria_id"))
    private List<EntidadBeneficiaria> entidadesSugeridas;

    @Column(name = "fecha_ejecucion")
    private LocalDateTime fechaEjecucion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoPropuesta estado;

    public ResultadoMatchmaking(Donacion donacion, List<EntidadBeneficiaria> entidadesSugeridas) {
        this.donacion = donacion;
        this.entidadesSugeridas = entidadesSugeridas;
        this.fechaEjecucion = LocalDateTime.now();
        this.estado = EstadoPropuesta.PENDIENTE;
    }
}
