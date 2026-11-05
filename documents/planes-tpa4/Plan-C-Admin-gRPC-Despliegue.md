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

- [x] Confirmado con el equipo: el servicio extraído en la Entrega 2 es `logistica-service` (extrayendo camiones, rutas, monitoreo y planificación del monolito original). El canal reemplazado por gRPC sobre HTTP/2 (`:9090`) es `logistica-service` → `donaciones-service`.

### 3.2 Tareas
- [x] `.proto` con los métodos que se usan de verdad (`donaciones.proto` en `grpc-contracts`):
  - `ListarDonacionesPorEstado(estado, limit, offset)`
  - `ActualizarEstadoDonacion(donacionId, estado, justificacion)`
  - `ConfirmarEntregaDonaciones(entidadId, donacionIds, patente, fechaHora)`
  - `ObtenerContactosEntidad(entidadId)`
  - `ObtenerDonacion(donacionId)`
  - `ObtenerContactoDonante(donanteId)`
- [x] Ubicación del `.proto`: en módulo nuevo `grpc-contracts` en el reactor raíz.
- [x] `protobuf-maven-plugin` + `os-maven-plugin` + `grpc-netty-shaded` / `grpc-protobuf` / `grpc-stub` configurados sin colisiones.
- [x] Servidor gRPC en donaciones, en puerto configurable (`GRPC_PORT`, default 9090), que reutiliza los `Service` existentes (`DonacionService`, `DonanteService`, `EntidadBeneficiariaService`).
- [x] Cliente en logística con `ManagedChannel` configurable por variable de entorno (`DONACIONES_GRPC_HOST` / `DONACIONES_GRPC_PORT`) y adaptador `DonacionesGrpcClient` que reemplaza `RestTemplate` en `PlanificadorDeRutas`, `GestorDeRutas` e `InicioRutaListener`.
- [x] Mantener los endpoints REST de donaciones: los sigue usando el `web-service`.
- [x] **Autenticación del canal gRPC** (ver 3.3): `ServerInterceptor` en donaciones y `ClientInterceptor` en logística con `SERVICE_API_KEY`.

### 3.3 Coordinación con la autenticación de la Persona A
La Persona A protege la API REST de donaciones con JWT y roles (contrato, matriz de roles y pendientes en [Plan-A-Plataforma-Publico-Entidad.md](Plan-A-Plataforma-Publico-Entidad.md), sección 2.1). Las llamadas de logística de la tabla 3.1 no tienen usuario, así que se resuelven así:

- **Mientras siguen por REST** (lo hace A): logística manda el header `X-Api-Key` con el valor de la variable `SERVICE_API_KEY`, agregado por un interceptor del `RestTemplate`. Donaciones lo trata como rol `SISTEMA`. Al tomar la rama de gRPC, partir de esa versión para no pisar el interceptor.
- **Al pasar a gRPC** (lo hace C): el servidor gRPC no pasa por Javalin, así que no lo cubre la autenticación de A. Hay que agregar:
  - [x] `ServerInterceptor` (`ApiKeyGrpcServerInterceptor`) en donaciones que valida `x-api-key` en los metadata contra `SERVICE_API_KEY` (si no coincide, `Status.UNAUTHENTICATED`).
  - [x] `ClientInterceptor` (`ApiKeyGrpcClientInterceptor`) en logística que agrega `x-api-key` a cada llamada.
  - [x] Avisar a A cuando se mergee: merge realizado con `entrega-4`; los endpoints REST de donaciones se conservan activos para el `web-service`.
- **Despliegue:** definir `SERVICE_API_KEY` (y `JWT_SECRET`, el mismo en todos los servicios que validen tokens) como variables de entorno en donaciones y logística. Si el proveedor tiene red privada, no exponer el puerto gRPC; la clave se mantiene igual.
- [x] Resuelto con el equipo: `notificaciones-service` opera dentro de la red privada inter-servicios de contenedores/VPC.
- [x] Probar el flujo completo planificación → inicio de ruta → entrega verificado tanto en tests de integración gRPC (`DonacionesGrpcIntegrationTest`), tests E2E reales con PostgreSQL y configuración compose.

## 4. Pantallas de administración (`web-service`, después de pusheada la Tarea 0 a `entrega-4`)

| Req | Ruta sugerida | Backend existente | Backend a agregar |
|---|---|---|---|
| 15 | `/admin/donantes/nuevo`, `/admin/donaciones/nueva` | `POST /api/donantes/persona-humana`, `POST /api/donantes/persona-juridica`, `POST /api/donaciones`, `GET /api/categorias` | Buscar donante por DNI/CUIT para asociar la donación (**verificar** si existe) |
| 16 | `/admin/deposito` | `GET /api/donaciones/estado/{estado}`, `PUT /api/donaciones/{id}/estado` | — |
| 17 | `/admin/matchmaking` | `POST /api/matchmaking/ejecuciones`, `GET /api/matchmaking/pendientes`, `PUT /api/matchmaking/propuestas/{id}/estado` | — (verificar que la respuesta traiga el ranking de cada algoritmo) |
| 18 | `/admin/camiones` | `GET/POST/PUT/DELETE /api/camiones` (logística) | — |
| 19 | `/admin/importaciones` | `ImportadorDeDonantes` (clase de dominio, sin endpoint) | Endpoint + procesamiento asíncrono |

- [x] Biblioteca de componentes reutilizables de UI (`TipoBoton`, `BotonHelper`, `EstadoHelper`, `FechaHelper`, `concat`, y partials `encabezado`, `listado`, `acciones-fila`, `filtros`, `modal-confirmar`).
- [x] Migración y estandarización completa de pantallas de administración (`camiones`, `deposito`, `matchmaking`, `recepcion`, `importaciones`) con componentes reutilizables.
- [x] Alta rápida de personas jurídicas en modal de recepción y vinculación con API `POST /api/donantes/persona-juridica`.
- [x] Otorgar usuario con credenciales a donantes importados por CSV (`ImportadorDeDonantes`) y limpieza de datos simulados fijos en estadísticas y tareas.
- [x] Prevención de doble clic e indicador de carga con spinner accesible (`cargando.js` y `.boton--cargando`).
- [x] ABM de categorías y subcategorías (`/admin/categorias`, no lo pide la consigna pero sin él el catálogo solo se cargaba con el script de datos demo): alta y edición en modales; baja solo de lo que ninguna donación ni necesidad usa (borrar una categoría borra sus subcategorías). Backend nuevo (rol `ADMINISTRADOR`): `PUT` y `DELETE` de `/api/categorias/{id}` y `/api/subcategorias/{id}`, más `GET .../usos`. Bocetos `15c` a `15g`.
- [x] Cierre del ciclo de vida logístico y rol `CHOFER` (detalle en [`documents/PlanCicloDeVidaDonacion.md`](../PlanCicloDeVidaDonacion.md)): planificación de hojas de ruta en `/admin/rutas` (bocetos `20a` y `20b`), vinculación de cuenta de chofer en `/admin/camiones` (boceto `18b`), portal operativo del chofer en `/chofer` con mapa, inicio de recorrido, confirmación y reporte de no recepción (bocetos `18d` y `18e`), y gestión de entregas fallidas en `/admin/deposito?filtro=fallidas` (bocetos `16` y `16c`).

### 4.0 Proteger la API de logística
Logística utiliza `common-lib`, implementando el mismo `AutenticacionHandler` que donaciones para proteger endpoints con JWT y API Key:
- [x] `/api/camiones/*` y la planificación de rutas (`/api/rutas/planificaciones`) protegidos con `ADMINISTRADOR` y `SISTEMA`. Lo llama el `web-service` con el JWT del admin.
- [x] Endpoints que llaman otros servicios (como el callback `/api/rutas/planificaciones/resultados`) → `SISTEMA`: el que llama manda `X-Api-Key` con `SERVICE_API_KEY`, agregado por el interceptor de `RestTemplate`.
- [x] Los endpoints de seguimiento para donante y entidad (`/api/rutas/activas?...`) admiten consulta para el seguimiento en frontend.
- [x] Verificar que `JWT_SECRET` y `SERVICE_API_KEY` lleguen a logística en local, en Docker y en la nube (en `docker-compose.yml` y `application.properties` configurados).
- [x] Tests implementados en `LogisticaAuthTest`: sin token → 401; donante → 403; admin → 200/204; sistema con API key → 200/204.

### 4.1 Importación CSV (req. 19)
- [x] `POST /api/donantes/importaciones` (multipart/stream) → responde 202 con el id de la importación; el archivo se procesa en segundo plano.
- [x] `GET /api/donantes/importaciones/{id}` → estado, filas procesadas, donantes creados/actualizados, errores. La pantalla consulta este estado y se actualiza en vivo.
- [x] Procesar de forma secuencial/lotes sin cargar las más de 10.000 filas en memoria de una vez (streaming con `BufferedReader`).
- [x] Gestión de padrón de donantes: se crean o actualizan donantes en batch en la base de datos para habilitar la recepción masiva de donaciones, emitiendo notificación asíncrona de onboarding a su medio de contacto.
- [x] Notificaciones de alta asíncronas para evitar saturar el servicio en cargas masivas.

## 5. Despliegue (requerimiento 4)

### 5.1 Elección del proveedor
- [x] Comparar opciones (verificado y documentado en [`documents/InformeOpcionesDespliegue.md`](../InformeOpcionesDespliegue.md)) evaluando las 5 alternativas: Railway + Neon, Oracle Cloud Always Free, Render + Supabase, Cloudflare Tunnels y DISILAB (UTN FRBA), analizando soporte gRPC HTTP/2, separación de nodos, persistencia de fotos y cold-starts.
- [ ] La consigna prefiere el servicio principal y el extraído en **nodos diferentes**.
- [ ] Todo el equipo tiene que tener acceso al despliegue.

### 5.2 Tareas
- [x] Base de datos administrada con las bases `donaciones`, `logistica` y `notificaciones` (`docker/postgres/init-databases.sql`).
- [x] `web-service/Dockerfile` y `web-service` en `docker-compose.yml` (perfil `full`): los hizo A junto con la Tarea 0. Definidas las variables en la nube (`DONACIONES_API_URL`, `COOKIE_SECURE=true`).
- [x] **Fotos de recepción (req. 12, A):** el `web-service` las guarda en el disco, en `FOTOS_DIR` (en `docker-compose.yml`, el volumen `fotos-data`). En la nube se monta un volumen persistente en esa carpeta para evitar pérdida de datos.
- [x] **Sacar URLs fijas a localhost**:
  - `GestorPlanificacionRutas.java` parametrizado con `logistica.callback-url` (`LOGISTICA_CALLBACK_URL`);
  - `common-lib/src/main/resources/application.yml` parametrizado con variables de entorno;
  - `application.properties` en donaciones y logística parametrizados con `NOTIFICACIONES_API_URL`, `DONACIONES_API_URL`, `LOGISTICA_API_URL`.
  Todo sale de variables de entorno con fallbacks locales.
- [ ] Desplegar **los 4 servicios**: web (público), donaciones, logística y notificaciones (el plan original se olvidaba de notificaciones).
- [ ] Seguridad: los backends no deberían quedar abiertos a Internet. Usar red privada del proveedor o, si no hay, un secreto compartido (header) entre el web y los backends. Secretos (DB, Resend, Twilio, admin) solo como variables de entorno.
- [ ] Cookie `Secure` activa en producción (coordinar la variable con la Persona A).
- [ ] Prueba de humo desde la URL pública: registro, login, mapa, asignación y seguimiento.
- [ ] Documentar en el README cómo desplegar y las variables de entorno.

## 6. Dependencias con el resto
- **Recibe de A:** la Tarea 0 (para las pantallas), el servicio para crear usuarios (para el CSV) y la autenticación con `X-Api-Key` entre logística y donaciones (sección 3.3).
- **Entrega a A y B:** las variables de entorno y las URLs del despliegue; el despliegue de prueba temprano.
- **Avisar a B** si cambian los DTOs de donaciones al pasar a gRPC (los endpoints REST que usa el web no deberían cambiar).
- **Permisos que cambió A (req. 12):** `POST /api/donaciones/recepciones` (entrega del camión) y `POST /api/donaciones/{id}/fotos` ya no aceptan el rol de entidad: quedan para `SISTEMA` (logística) y `ADMINISTRADOR`. La entidad confirma la recepción en `POST /api/donaciones/{id}/recepcion`. Al pasar logística → donaciones a gRPC, la confirmación de entrega del camión sigue siendo de logística y la de la entidad no se toca.
- **Página común:** `/mapa` (req. 2) es pública. Su link está en el navbar y en la sección común "Plataforma" del menú lateral, no en el bloque del administrador.
- **Archivos compartidos:** `pom.xml` raíz (módulo gRPC), `docker-compose.yml`, `navbar.hbs` (links de admin), `styles.css`.
- **Layout con sesión:** cambió a **menú lateral + barra superior** (ver "Convenciones de pantallas" en [`AGENTS.md`](../../AGENTS.md#convenciones-de-pantallas) y el boceto `documents/bocetos/10-necesidades.png`). A es dueño del layout; C arma el bloque del administrador en el menú lateral (secciones e íconos para las pantallas 15–19). Las pantallas de admin siguen la convención de listado + modal (con cruz para cerrar, ver `AGENTS.md`) y confirmación al eliminar. La barra superior ya no tiene título: va en el `<h1>` de cada página.

## 7. Entregables de documentación
- [x] Bocetos de las pantallas 15–19 en `documents/bocetos/`, **antes** de implementarlas. Seguir el flujo de [`AGENTS.md`](../../AGENTS.md#flujo-para-cada-pantalla-nueva): boceto (en lo posible en Figma), PNG con formato `punto-nombre.png` y después el código.
- [x] `Despliegue.puml` actualizado: nodos cloud, web-service, canal gRPC sobre HTTP/2 (:9090), base administrada y volumen persistente.
- [x] Justificación: gRPC vs GraphQL (por qué gRPC para comunicación entre servicios) en [`entregables/JustificacionGrpc.md`](../../entregables/JustificacionGrpc.md) y guía técnica completa en [`documents/GuiaImplementacionGrpc.md`](../GuiaImplementacionGrpc.md).
- [x] Comparación de proveedores cloud y topología elegida en [`documents/InformeOpcionesDespliegue.md`](../InformeOpcionesDespliegue.md).
- [x] Diagrama de arquitectura actualizado (topología cloud multi-nodo en `InformeOpcionesDespliegue.md` y `Despliegue.puml`).

## 8. Cronograma
**[COMPLETAR]** Fechas por bloque. Sugerido: días 1–2 despliegue de prueba + elección del proveedor; días 2–4 gRPC; después el CSV y las pantallas de admin; al final, el despliegue definitivo y la prueba de humo.

Rama: todo en `entrega-4` hasta que esté terminado el trabajo de todo el equipo.
