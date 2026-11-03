package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.gamificacion.IncentivosResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.gamificacion.InsigniaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.gamificacion.MisionProgresoResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion.CategoriaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion.Insignia;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion.Mision;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion.ProgresoMision;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.gamificacion.MisionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.gamificacion.ProgresoMisionRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.GamificacionService;

@Service
public class GamificacionServiceImpl implements GamificacionService {

    private final DonanteRepository donanteRepository;
    private final MisionRepository misionRepository;
    private final ProgresoMisionRepository progresoMisionRepository;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    public GamificacionServiceImpl(
        DonanteRepository donanteRepository,
        MisionRepository misionRepository,
        ProgresoMisionRepository progresoMisionRepository,
        RestTemplate restTemplate,
        RestDonacionesConfig config) {
      this.donanteRepository = donanteRepository;
      this.misionRepository = misionRepository;
      this.progresoMisionRepository = progresoMisionRepository;
      this.restTemplate = restTemplate;
      this.config = config;
    }


  @Override
  public IncentivosResponse obtenerIncentivos(Long donanteId) {
    Donante donante = donanteRepository.findById(donanteId)
        .orElseThrow(() -> new ResourceNotFoundException("No se encontró el donante: " + donanteId));
    if (!(donante instanceof PersonaHumana)) {
      return new IncentivosResponse(donanteId, "NO_APLICA", "Solo personas humanas", 0, List.of(), List.of());
    }

    asegurarMisionesBase();

    int totalDonaciones = donante.getDonaciones() != null ? donante.getDonaciones().size() : 0;
    CategoriaDonante categoria = CategoriaDonante.calcularPara(totalDonaciones);

    List<Mision> todasLasMisiones = misionRepository.findAll();
    List<MisionProgresoResponse> misionesResponses = new ArrayList<>();
    List<InsigniaResponse> insigniasObtenidas = new ArrayList<>();

    for (Mision m : todasLasMisiones) {
      ProgresoMision progreso = progresoMisionRepository.findByDonanteIdAndMisionId(donanteId, m.getId())
          .orElseGet(() -> progresoMisionRepository.save(new ProgresoMision(donanteId, m)));

      // actualizamos si cambia la cantidad
      if (!progreso.getCumplida()) {
        progreso.actualizarProgreso(totalDonaciones);
        progreso = progresoMisionRepository.save(progreso);
      }

      int porcentaje = Math.min(100, (int) Math.round(((double) progreso.getProgresoActual() / m.getMeta()) * 100));

      InsigniaResponse insigniaResp = new InsigniaResponse(
          m.getInsignia().getId(),
          m.getInsignia().getNombre(),
          m.getInsignia().getDescripcion(),
          m.getInsignia().getIconoUrl()
      );

      misionesResponses.add(new MisionProgresoResponse(
          m.getCodigo(),
          m.getNombre(),
          m.getDescripcion(),
          progreso.getProgresoActual(),
          m.getMeta(),
          progreso.getCumplida(),
          porcentaje,
          insigniaResp
      ));

      if (progreso.getCumplida()) {
        insigniasObtenidas.add(insigniaResp);
      }
    }

    return new IncentivosResponse(
        donanteId,
        categoria.name(),
        categoria.getTitulo(),
        totalDonaciones,
        misionesResponses,
        insigniasObtenidas
    );
  }



  @Override
  public void evaluarProgreso(Long donanteId) {
    Donante donante = donanteRepository.findById(donanteId).orElse(null);
    if (donante == null || !(donante instanceof PersonaHumana)) {
      return;
    }

    asegurarMisionesBase();

    int totalDonaciones = donante.getDonaciones() != null ? donante.getDonaciones().size() : 0;
    CategoriaDonante categoriaActual = CategoriaDonante.calcularPara(totalDonaciones);

    for (Mision m : misionRepository.findAll()) {
      ProgresoMision progreso = progresoMisionRepository.findByDonanteIdAndMisionId(donanteId, m.getId())
          .orElseGet(() -> new ProgresoMision(donanteId, m));
      boolean acabaDeCumplirse = progreso.actualizarProgreso(totalDonaciones);
      progresoMisionRepository.save(progreso);

      if (acabaDeCumplirse) {
        enviarNotificacionAsync(
            donante.getContactoPredeterminado(),
            "¡Felicitaciones! Cumpliste la misión '" + m.getNombre() + "' y ganaste la insignia '" + m.getInsignia().getNombre() + "'.",
            "MISION_CUMPLIDA",
            donanteId
        );
      }
    }

    // si con esta donacion alcanza una nueva categoria
    if (totalDonaciones == categoriaActual.getDonacionesRequeridas() && categoriaActual != CategoriaDonante.BRONCE) {
      enviarNotificacionAsync(
          donante.getContactoPredeterminado(),
          "¡Subiste de nivel! Tu categoría como donante ahora es: " + categoriaActual.getTitulo() + " (" + categoriaActual.name() + ").",
          "SUBIDA_CATEGORIA",
          donanteId
      );
    }
  }

  private void asegurarMisionesBase() {
    if (!misionRepository.findAll().isEmpty()) {
      return;
    }
    Insignia ins1 = new Insignia("Primer Paso", "Realizaste tu primera donación", "/img/insignias/primer-paso.svg");
    Mision mis1 = new Mision("PRIMERA_DONACION", "Primera donación", "Realizá tu primera donación en DonaTrack", 1, ins1);
    misionRepository.save(mis1);

    Insignia ins2 = new Insignia("Solidario", "Alcanzaste 3 donaciones", "/img/insignias/solidario.svg");
    Mision mis2 = new Mision("TRES_DONACIONES", "Donante solidario", "Completá 3 donaciones en la plataforma", 3, ins2);
    misionRepository.save(mis2);

    Insignia ins3 = new Insignia("Héroe", "Alcanzaste 5 donaciones", "/img/insignias/heroe.svg");
    Mision mis3 = new Mision("CINCO_DONACIONES", "Héroe comunitario", "Completá 5 donaciones en la plataforma", 5, ins3);
    misionRepository.save(mis3);
  }

  private void enviarNotificacionAsync(MedioContacto contacto, String mensaje, String tipoEvento, Long donanteId) {
    if (contacto == null) return;
    NotificacionRequest request = new NotificacionRequest(
        contacto.getValor(),
        contacto.getTipoContacto(),
        mensaje
    );
    String url = config.getNotificacionesUrl() + "/notificaciones";
    CompletableFuture.runAsync(() -> {
      try {
        restTemplate.postForObject(url, request, Void.class);
      } catch (Exception e) {
        System.err.println("Error enviando notificación de gamificación: " + e.getMessage());
      }
    });

  }
}