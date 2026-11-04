package ar.edu.utn.frba.ddsi.web.dto.donantes;

import java.util.List;

public record PersonaJuridicaAdminRequest(
        String razonSocial,
        String rubro,
        String tipo,
        String cuit,
        List<RepresentanteDto> representantes,
        List<MedioContactoDto> contactos,
        MedioContactoDto contactoPredeterminado
) {
    public record RepresentanteDto(String nombre, String apellido, String rol) {}
}
