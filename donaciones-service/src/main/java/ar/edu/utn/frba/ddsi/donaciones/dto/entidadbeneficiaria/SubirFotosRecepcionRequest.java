package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import java.util.List;

public record SubirFotosRecepcionRequest(
    List<String> fotosUrl
) {}
