package ar.edu.utn.frba.ddsi.donaciones.services.matchmaking;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;

public interface AlgoritmoAsignacion {
    List<EntidadBeneficiaria> generarRanking(Donacion donacion, List<EntidadBeneficiaria> entidadesCandidatas);
}
