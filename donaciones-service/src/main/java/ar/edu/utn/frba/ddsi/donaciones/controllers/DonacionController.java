package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.Arrays;
import java.util.List;

import static ar.edu.utn.frba.ddsi.common.security.Rol.ADMINISTRADOR;
import static ar.edu.utn.frba.ddsi.common.security.Rol.CHOFER;
import static ar.edu.utn.frba.ddsi.common.security.Rol.DONANTE;
import static ar.edu.utn.frba.ddsi.common.security.Rol.ENTIDAD_BENEFICIARIA;
import static ar.edu.utn.frba.ddsi.common.security.Rol.SISTEMA;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.security.Autorizacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.SubirFotosRecepcionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import io.javalin.Javalin;

@Component
public class DonacionController implements JavalinController {

    private final DonacionService donacionService;

    public DonacionController(DonacionService donacionService) {
        this.donacionService = donacionService;
    }

    @Override
      public void registerRoutes(Javalin app) {
        // GET /api/donaciones con filtros (donanteId, entidadId, estado, categoriaId, subcategoriaIds)
        app.get("/api/donaciones", ctx -> {
          String donanteIdStr = ctx.queryParam("donanteId");
          Long donanteId = (donanteIdStr != null && !donanteIdStr.isBlank()) ? Long.parseLong(donanteIdStr) : null;
          if (donanteId != null) {
            Autorizacion.exigirPropietario(ctx, DONANTE, donanteId);
          }

          String entidadIdStr = ctx.queryParam("entidadId");
          Long entidadId = (entidadIdStr != null && !entidadIdStr.isBlank()) ? Long.parseLong(entidadIdStr) : null;
          if (entidadId != null) {
            Autorizacion.exigirPropietario(ctx, ENTIDAD_BENEFICIARIA, entidadId);
          }

          if (donanteId == null && entidadId == null) {
            Autorizacion.exigir(Autorizacion.esPrivilegiado(ctx));
          }

          String estado = ctx.queryParam("estado");
          String categoriaIdStr = ctx.queryParam("categoriaId");
          Long categoriaId = (categoriaIdStr != null && !categoriaIdStr.isBlank()) ? Long.parseLong(categoriaIdStr) : null;

          List<Long> subcategoriaIds = null;
          String subcatStr = ctx.queryParam("subcategoriaIds");
          if (subcatStr != null && !subcatStr.isBlank()) {
            subcategoriaIds = Arrays.stream(subcatStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
          }

          ctx.json(donacionService.buscar(donanteId, estado, categoriaId, subcategoriaIds, entidadId));
        }, DONANTE, ENTIDAD_BENEFICIARIA, ADMINISTRADOR, SISTEMA);

        // publico (req. 2, mapa de entregas): las donaciones entregadas por entidad, sin datos del donante.
        app.get("/api/donaciones/entregadas", ctx -> ctx.json(donacionService.obtenerEntregasPorEntidad()));

        // la ve su donante, la entidad a la que se asigno, el chofer en reparto, el administrador y los servicios
        app.get("/api/donaciones/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            DonacionResponse donacion = donacionService.obtenerPorId(id);
            Autorizacion.exigir(Autorizacion.esPrivilegiado(ctx)
                    || Autorizacion.usuario(ctx).rol() == CHOFER
                    || Autorizacion.esPropietario(ctx, DONANTE, donacion.donanteId())
                    || Autorizacion.esPropietario(ctx, ENTIDAD_BENEFICIARIA, donacion.entidadAsignadaId()));
            ctx.json(donacion);
        }, DONANTE, ENTIDAD_BENEFICIARIA, CHOFER, ADMINISTRADOR, SISTEMA);

        // un donante solo registra donaciones a su nombre; el administrador, a nombre de cualquiera
        app.post("/api/donaciones", ctx -> {
            DonacionRequest request = ctx.bodyAsClass(DonacionRequest.class);
            Autorizacion.exigirPropietario(ctx, DONANTE, request.idDonante());
            ctx.status(201).json(donacionService.crear(request));
        }, DONANTE, ADMINISTRADOR);

        app.delete("/api/donaciones/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            if (!donacionService.eliminar(id)) {
                throw new ResourceNotFoundException("No se encontro una donacion con el id: " + id);
            }
            ctx.status(204);
        }, ADMINISTRADOR);

        app.put("/api/donaciones/{id}/estado", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            EstadoDonacionRequest request = ctx.bodyAsClass(EstadoDonacionRequest.class);
            ctx.json(donacionService.cambiarEstado(id, request));
        }, ADMINISTRADOR, SISTEMA);

        app.get("/api/donaciones/estado/{estado}", ctx -> {
            String estado = ctx.pathParam("estado");
            int limit = Integer.parseInt(ctx.queryParam("limit"));
            String offsetStr = ctx.queryParam("offset");
            int offset = (offsetStr != null && !offsetStr.isBlank()) ? Integer.parseInt(offsetStr) : 0;
            ctx.json(donacionService.obtenerDonacionesSegunEstado(estado, limit, offset));
        }, ADMINISTRADOR, SISTEMA);

        // carga directa de fotos (solo el administrador); la entidad las carga al confirmar la recepcion
        app.post("/api/donaciones/{id}/fotos", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            SubirFotosRecepcionRequest request = ctx.bodyAsClass(SubirFotosRecepcionRequest.class);
            donacionService.subirFotosRecepcion(id, request);
            ctx.status(204);
        }, ADMINISTRADOR);

        // req. 12: la entidad confirma que recibio una donacion entregada y carga las URLs de sus fotos
        app.post("/api/donaciones/{id}/recepcion", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Autorizacion.exigirPropietario(ctx, ENTIDAD_BENEFICIARIA, donacionService.obtenerPorId(id).entidadAsignadaId());
            SubirFotosRecepcionRequest request = ctx.bodyAsClass(SubirFotosRecepcionRequest.class);
            ctx.json(donacionService.confirmarRecepcion(id, request));
        }, ENTIDAD_BENEFICIARIA, ADMINISTRADOR);

        // entrega del camion: la informa logistica (la entidad confirma la recepcion en /{id}/recepcion)
        app.post("/api/donaciones/recepciones", ctx -> {
            ConfirmacionEntregaExitosaRequest request = ctx.bodyAsClass(ConfirmacionEntregaExitosaRequest.class);
            donacionService.confirmarEntregaExitosa(request);
            ctx.status(204);
        }, ADMINISTRADOR, SISTEMA);
    }
}
