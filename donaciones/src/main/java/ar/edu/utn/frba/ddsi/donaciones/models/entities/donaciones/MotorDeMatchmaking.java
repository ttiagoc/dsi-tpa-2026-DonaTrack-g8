package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MotorDeMatchmaking {
    private List<AlgoritmoAsignacion> algoritmos;

    public ResultadoMatchmaking ejecutarMatchmaking(Donacion donacion, List<EntidadBeneficiaria> entidadesCandidatas) {
        if (algoritmos.isEmpty()) {
            return new ResultadoMatchmaking(donacion, new ArrayList<>());
        }

        List<List<EntidadBeneficiaria>> resultadosRankings = algoritmos.stream()
                .map(algoritmo -> algoritmo.generarRanking(donacion, entidadesCandidatas))
                .toList();

        List<EntidadBeneficiaria> rankingBase = resultadosRankings.get(0);
        List<EntidadBeneficiaria> coincidenciaInterseccion = new ArrayList<>(rankingBase);

        for (int i = 1; i < resultadosRankings.size(); i++) {
            coincidenciaInterseccion.retainAll(resultadosRankings.get(i));
        }

        List<EntidadBeneficiaria> sugerenciasFinales;
        if (!coincidenciaInterseccion.isEmpty()) {
            sugerenciasFinales = coincidenciaInterseccion;
        } else {
            List<EntidadBeneficiaria> todasLasSugeridas = new ArrayList<>();
            for (List<EntidadBeneficiaria> ranking : resultadosRankings) {
                for (EntidadBeneficiaria e : ranking) {
                    if (!todasLasSugeridas.contains(e)) {
                        todasLasSugeridas.add(e);
                    }
                }
            }
            sugerenciasFinales = todasLasSugeridas;
        }

        return new ResultadoMatchmaking(donacion, sugerenciasFinales);
    }
}