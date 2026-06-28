package ar.edu.utn.frba.ddsi.logistica.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaExitosaDTO {
    private Long entidadId;
    private List<Long> donacionIds;
    private String patenteCamion;
    private LocalDateTime fechaHora;
}