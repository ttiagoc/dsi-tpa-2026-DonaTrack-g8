# Plan base — Persona C: Administración, gRPC y Despliegue

> Archivo base para que quien tome esta parte arme su plan detallado. Completar las secciones marcadas con **[COMPLETAR]**.

## 1. Alcance

| Bloque | Requisitos de la consigna |
|---|---|
| Persona administradora | 15 (registrar donantes y donaciones), 16 (marcar vencidas), 17 (elegir entidad según matchmaking), 18 (ABM de camiones), 19 (importar CSV de más de 10.000 filas) |
| Integración | Requerimiento 3: **gRPC** como reemplazo de HTTP entre `donaciones-service` y `logistica-service` |
| Despliegue | Requerimiento 4: sistema en la nube accesible públicamente |
| Documentación | Bocetos de las pantallas de admin, `Despliegue.puml`, justificación gRPC vs GraphQL y topología cloud |

## 2. Qué se puede hacer sin esperar la Tarea 0

La Tarea 0 (web-service + login, Persona A) tarda 1–2 días. Mientras tanto son independientes:

1. **gRPC** (no toca el web).
2. **Despliegue de prueba** de los servicios actuales, para detectar problemas temprano.
3. **Backend de la importación CSV** asíncrona (solo la parte de crear usuarios depende de A).
4. **Bocetos** de las pantallas 15–19.

## 3. gRPC (requerimiento 3)

### 3.1 Dirección correcta (corrige el plan original)
El plan original ponía el servidor en logística y un `LogisticaClientRest` en donaciones, que **no existe**. En el código real las llamadas van de **logística → donaciones**:

| Clase en `logistica-service` | Llamada actual a donaciones |
|---|---|
| `PlanificadorDeRutas` | GET donaciones por estado (`/api/donaciones/estado/{estado}`), PUT de estado |
| `GestorDeRutas` | PUT de estado de donación, POST de eventos (inicio de ruta / entrega) |
| `InicioRutaListener` | GET de donación por id, GET de contacto del donante |

Por lo tanto: **servidor gRPC en `donaciones-service`, cliente gRPC en `logistica-service`.**

- [ ] **[COMPLETAR]** Confirmar con el equipo cuál fue el servicio "extraído en la entrega 2" (la consigna habla de ese canal) y releer la justificación de la entrega 2.

### 3.2 Tareas
- [ ] `.proto` con los métodos que se usan de verdad, por ejemplo:
  - `ListarDonacionesPorEstado(estado, limit, offset)`
  - `ActualizarEstadoDonacion(donacionId, estado, justificacion)`
  - `ObtenerDonacion(donacionId)`
  - `ObtenerContactoDonante(donanteId)`
  - `NotificarInicioRuta(...)` / `ConfirmarEntrega(...)`
- [ ] **[COMPLETAR]** Ubicación del `.proto`: en `common-lib` o en un módulo nuevo `grpc-contracts` (recomendado: no meter el plugin de protobuf en `common-lib`, que usan todos).
- [ ] `protobuf-maven-plugin` + `grpc-netty-shaded` / `grpc-protobuf` / `grpc-stub`. Verificar que no choquen con las versiones fijadas por el BOM de Spring Boot (ya hubo conflictos con Jetty y jaxb, ver comentarios en los pom).
- [ ] Servidor gRPC en donaciones, en un puerto propio (`GRPC_PORT`, por ejemplo 9090), que reutilice los `Service` existentes (no duplicar lógica).
- [ ] Cliente en logística con `ManagedChannel` configurable por variable de entorno (`DONACIONES_GRPC_HOST` / `DONACIONES_GRPC_PORT`) que reemplace las llamadas con `RestTemplate` de la tabla.
- [ ] Mantener los endpoints REST de donaciones: los sigue usando el `web-service`.
- [ ] Probar el flujo completo planificación → inicio de ruta → entrega con docker compose.

## 4. Pantallas de administración (`web-service`, después de mergeada la Tarea 0)

| Req | Ruta sugerida | Backend existente | Backend a agregar |
|---|---|---|---|
| 15 | `/admin/donantes/nuevo`, `/admin/donaciones/nueva` | `POST /api/donantes/persona-humana`, `POST /api/donantes/persona-juridica`, `POST /api/donaciones`, `GET /api/categorias` | Buscar donante por DNI/CUIT para asociar la donación (**verificar** si existe) |
| 16 | `/admin/deposito` | `GET /api/donaciones/estado/{estado}`, `PUT /api/donaciones/{id}/estado` | — |
| 17 | `/admin/matchmaking` | `POST /api/matchmaking/ejecuciones`, `GET /api/matchmaking/pendientes`, `PUT /api/matchmaking/propuestas/{id}/estado` | — (verificar que la respuesta traiga el ranking de cada algoritmo) |
| 18 | `/admin/camiones` | `GET/POST/PUT/DELETE /api/camiones` (logística) | — |
| 19 | `/admin/importaciones` | `ImportadorDeDonantes` (clase de dominio, sin endpoint) | Endpoint + procesamiento asíncrono |

### 4.1 Importación CSV (req. 19)
- [ ] `POST /api/donantes/importaciones` (multipart) → responde 202 con el id de la importación; el archivo se procesa en segundo plano.
- [ ] `GET /api/donantes/importaciones/{id}` → estado, filas procesadas, usuarios creados, errores. La pantalla consulta este estado.
- [ ] Procesar en lotes (transacción cada N filas), sin cargar las más de 10.000 filas en memoria de una vez. Probar con un CSV de 10.000+ filas y medir el tiempo.
- [ ] Crear el `Usuario` de cada donante con el servicio que expone la Persona A en la Tarea 0. **[COMPLETAR]** ¿Cómo se entrega la contraseña inicial? (por ejemplo, una temporal enviada por la notificación existente y que se cambia en el primer login).
- [ ] Hoy `ImportadorDeDonantes` notifica por `RestTemplate` a notificaciones-service: revisar que el volumen no la sature (envío en lotes o asíncrono).

## 5. Despliegue (requerimiento 4)

### 5.1 Elección del proveedor
- [ ] **[COMPLETAR]** Comparar al menos 2 opciones (lo pide la consigna) en: horas gratis por mes para **4 servicios**, red privada entre servicios, soporte HTTP/2 para gRPC, que el servicio se "duerma" y cuánto tarda en volver, límites de la base de datos.
  - Opciones: Render / Railway + Neon / Supabase, Oracle / AWS / Azure free tier, DISILAB.
- [ ] La consigna prefiere el servicio principal y el extraído en **nodos diferentes**.
- [ ] Todo el equipo tiene que tener acceso al despliegue.

### 5.2 Tareas
- [ ] Base de datos administrada con las bases `donaciones`, `logistica` y `notificaciones` (`docker/postgres/init-databases.sql`).
- [ ] `web-service/Dockerfile` (copiar el patrón multi-etapa de `donaciones-service/Dockerfile`) y agregar `web-service` al `docker-compose.yml` (perfil `full`).
- [ ] **Sacar URLs fijas a localhost**:
  - `GestorPlanificacionRutas.java` arma el callback con `http://localhost:8081/...`;
  - `common-lib/src/main/resources/application.yml` tiene las URLs por defecto.
  Todo tiene que salir de variables de entorno.
- [ ] Desplegar **los 4 servicios**: web (público), donaciones, logística y notificaciones (el plan original se olvidaba de notificaciones).
- [ ] Seguridad: los backends no deberían quedar abiertos a Internet. Usar red privada del proveedor o, si no hay, un secreto compartido (header) entre el web y los backends. Secretos (DB, Resend, Twilio, admin) solo como variables de entorno.
- [ ] Cookie `Secure` activa en producción (coordinar la variable con la Persona A).
- [ ] Prueba de humo desde la URL pública: registro, login, mapa, asignación y seguimiento.
- [ ] Documentar en el README cómo desplegar y las variables de entorno.

## 6. Dependencias con el resto
- **Recibe de A:** la Tarea 0 (para las pantallas) y el servicio para crear usuarios (para el CSV).
- **Entrega a A y B:** las variables de entorno y las URLs del despliegue; el despliegue de prueba temprano.
- **Avisar a B** si cambian los DTOs de donaciones al pasar a gRPC (los endpoints REST que usa el web no deberían cambiar).
- **Archivos compartidos:** `pom.xml` raíz (módulo gRPC), `docker-compose.yml`, `navbar.hbs` (links de admin), `styles.css`.

## 7. Entregables de documentación
- [ ] Bocetos de las pantallas 15–19 en `documents/bocetos/`, **antes** de implementarlas.
- [ ] `Despliegue.puml` actualizado: nodos cloud, web-service, canal gRPC, base administrada.
- [ ] Justificación: gRPC vs GraphQL (por qué gRPC para comunicación entre servicios), comparación de proveedores cloud y topología elegida.
- [ ] Diagrama de arquitectura actualizado (el del plan original tiene la flecha gRPC al revés y le falta web → notificaciones).

## 8. Cronograma
**[COMPLETAR]** Fechas por bloque. Sugerido: días 1–2 despliegue de prueba + elección del proveedor; días 2–4 gRPC; después el CSV y las pantallas de admin; al final, el despliegue definitivo y la prueba de humo.

Ramas sugeridas: `feat/grpc`, `feat/deploy`, `feat/admin-web`.
