package ar.edu.utn.frba.ddsi.donaciones.services.matchmaking;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;

@Component
public class CompatibilidadSemantica implements AlgoritmoAsignacion {
    @Override
    public List<EntidadBeneficiaria> generarRanking(Donacion donacion, List<EntidadBeneficiaria> entidadesCandidatas) {
        return entidadesCandidatas.stream()
                .filter(e -> e.getNecesidades().stream()
                        .anyMatch(n -> n.getSubcategoria().equals(donacion.getSubcategoria())))
                .limit(10)
                .toList();
    }
}
