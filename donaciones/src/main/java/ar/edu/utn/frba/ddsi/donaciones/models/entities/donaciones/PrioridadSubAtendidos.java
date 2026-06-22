package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;

public class PrioridadSubAtendidos implements AlgoritmoAsignacion {
    @Override
    public List<EntidadBeneficiaria> generarRanking(Donacion donacion, List<EntidadBeneficiaria> entidadesCandidatas) {
        LocalDateTime limiteTrimestre = LocalDateTime.now().minusMonths(3);
        return entidadesCandidatas.stream()
                .sorted(Comparator.comparingLong(entidad -> contarDonacionesRecibidas(entidad, limiteTrimestre)))
                .limit(10)
                .toList();
    }

    private long contarDonacionesRecibidas(EntidadBeneficiaria entidad, LocalDateTime desdeFecha) {
        if (entidad.getNecesidades() == null)
            return 0;

        return entidad.getNecesidades().stream()
                .flatMap(necesidad -> necesidad.getDonacionesAsignadas().stream())
                .filter(d -> d.getFecha() != null && d.getFecha().isAfter(desdeFecha))
                .count();
    }
}