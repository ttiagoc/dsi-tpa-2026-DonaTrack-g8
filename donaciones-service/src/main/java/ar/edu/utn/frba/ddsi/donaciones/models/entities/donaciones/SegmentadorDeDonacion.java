package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class SegmentadorDeDonacion {
    public List<Donacion> segmentarDonacion(RegistroDonacion registroDonacion) {
        List<Bien> bienes = registroDonacion.getBienes();
        List<Donacion> nuevasDonaciones = bienes.stream()
                .collect(Collectors.groupingBy(Bien::generarKey))
                .values().stream()
                .map(bienesAgrupados -> {
                    Donacion donacion = new Donacion(bienesAgrupados.getFirst(), registroDonacion.getFecha());
                    bienesAgrupados.stream().skip(1).forEach(donacion::agregarBien);
                    return donacion;
                })
                .toList();
        return nuevasDonaciones;
    }
}
