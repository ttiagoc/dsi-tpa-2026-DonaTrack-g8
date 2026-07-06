package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;

import org.springframework.stereotype.Component;

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
