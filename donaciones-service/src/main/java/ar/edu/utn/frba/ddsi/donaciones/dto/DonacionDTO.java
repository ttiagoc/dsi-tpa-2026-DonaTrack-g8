package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonacionDTO {
    private Long id;
    private Double peso;
    private Double volumen;
    private String direccion;
}
