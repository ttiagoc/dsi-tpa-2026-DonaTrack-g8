package ar.edu.utn.frba.ddsi.logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonacionDTO {
    private Long id;
    private Double peso;
    private Double volumen;
    private String direccion;
}
