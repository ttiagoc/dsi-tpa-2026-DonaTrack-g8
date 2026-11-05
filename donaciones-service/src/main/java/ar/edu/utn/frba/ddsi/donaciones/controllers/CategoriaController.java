package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static ar.edu.utn.frba.ddsi.common.security.Rol.ADMINISTRADOR;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.services.CategoriaService;
import io.javalin.Javalin;

@Component
public class CategoriaController implements JavalinController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/categorias", ctx -> {
            ctx.json(categoriaService.obtenerTodas());
        });

        app.get("/api/subcategorias", ctx -> {
            ctx.json(categoriaService.obtenerTodasLasSubcategorias());
        });

        app.post("/api/categorias", ctx -> {
            CategoriaRequest request = ctx.bodyAsClass(CategoriaRequest.class);
            ctx.status(201).json(categoriaService.crear(request));
        }, ADMINISTRADOR);

        app.put("/api/categorias/{categoriaId}", ctx -> {
            Long categoriaId = Long.parseLong(ctx.pathParam("categoriaId"));
            CategoriaRequest request = ctx.bodyAsClass(CategoriaRequest.class);
            ctx.json(categoriaService.actualizar(categoriaId, request));
        }, ADMINISTRADOR);

        app.get("/api/categorias/{categoriaId}/subcategorias", ctx -> {
            Long categoriaId = Long.parseLong(ctx.pathParam("categoriaId"));
            ctx.json(categoriaService.obtenerSubcategorias(categoriaId));
        });

        app.post("/api/categorias/{categoriaId}/subcategorias", ctx -> {
            Long categoriaId = Long.parseLong(ctx.pathParam("categoriaId"));
            SubcategoriaRequest request = ctx.bodyAsClass(SubcategoriaRequest.class);
            ctx.status(201).json(categoriaService.crearSubcategoria(categoriaId, request));
        }, ADMINISTRADOR);

        app.put("/api/subcategorias/{subcategoriaId}", ctx -> {
            Long subcategoriaId = Long.parseLong(ctx.pathParam("subcategoriaId"));
            SubcategoriaRequest request = ctx.bodyAsClass(SubcategoriaRequest.class);
            ctx.json(categoriaService.actualizarSubcategoria(subcategoriaId, request));
        }, ADMINISTRADOR);

        // Baja: solo si nada la usa (si no, 400 con el motivo). Borrar una categoria borra sus subcategorias.
        app.get("/api/categorias/{categoriaId}/usos", ctx -> {
            ctx.json(categoriaService.usosCategoria(Long.parseLong(ctx.pathParam("categoriaId"))));
        }, ADMINISTRADOR);

        app.delete("/api/categorias/{categoriaId}", ctx -> {
            categoriaService.eliminarCategoria(Long.parseLong(ctx.pathParam("categoriaId")));
            ctx.status(204);
        }, ADMINISTRADOR);

        app.get("/api/subcategorias/{subcategoriaId}/usos", ctx -> {
            ctx.json(categoriaService.usosSubcategoria(Long.parseLong(ctx.pathParam("subcategoriaId"))));
        }, ADMINISTRADOR);

        app.delete("/api/subcategorias/{subcategoriaId}", ctx -> {
            categoriaService.eliminarSubcategoria(Long.parseLong(ctx.pathParam("subcategoriaId")));
            ctx.status(204);
        }, ADMINISTRADOR);
    }
}
