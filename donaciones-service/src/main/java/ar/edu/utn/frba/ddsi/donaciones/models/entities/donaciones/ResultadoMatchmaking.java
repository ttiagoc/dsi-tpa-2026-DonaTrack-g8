package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultadoMatchmaking {
    private Long id;
    private Donacion donacion;
    private List<EntidadBeneficiaria> entidadesSugeridas;
    private LocalDateTime fechaEjecucion;
    private EstadoPropuesta estado;

    public ResultadoMatchmaking(Donacion donacion, List<EntidadBeneficiaria> entidadesSugeridas) {
        this.donacion = donacion;
        this.entidadesSugeridas = entidadesSugeridas;
        this.fechaEjecucion = LocalDateTime.now();
        this.estado = EstadoPropuesta.PENDIENTE;
    }
}