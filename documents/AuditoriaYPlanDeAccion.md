# Auditoría Integral y Plan de Acción — DonaTrack

Este documento contiene la auditoría completa del proyecto **DonaTrack** frente a las consignas de las tres entregas del Trabajo Práctico Anual (TPA 1, TPA 2 y TPA 3) de Diseño de Sistemas de Información (UTN FRBA), junto con el **diagnóstico detallado de faltantes, la solución técnica propuesta para cada uno y una checklist operativa** para guiar la implementación paso a paso.

---

## 1. Resumen Ejecutivo de Cumplimiento

| Entrega | Alcance Principal | Grado de Cumplimiento | Estado General |
| :--- | :--- | :---: | :--- |
| **Entrega 1** | Modelado de objetos, Donantes, Donaciones, Segmentación, Necesidades, Estados y Notificaciones base. | **92%** | **Excelente base de dominio**. Dominio, segmentación y auditoría de estados plenamente implementados. Falta exponer la importación CSV en un endpoint REST. |
| **Entrega 2** | Microservicios REST, Matchmaking, Integración Logística (planificador externo, telemetría), Notificaciones reales. | **82%** | **Muy avanzado**. Falta automatizar calendarización (`@Scheduled`) de tareas nocturnas, persistir rutas al recibir el callback externo y procesar donaciones sobrantes. |
| **Entrega 3** | Persistencia JPA, esquemas aislados por servicio, tests HSQLDB, PostgreSQL local, DER y justificaciones. | **95%** | **Sobresaliente**. Mapeo ORM riguroso, repositorios limpios con `jpa-extras`, HSQLDB para tests y perfiles PostgreSQL bien documentados. |

---

## 2. Relevamiento Requisito por Requisito

### 2.1. Entrega 1: Arquitectura y Modelado en Objetos — Parte I

- **Gestión de Donantes (`HUMANA` y `JURIDICA`) — ✅ CUMPLIDO:**
  - `PersonaHumana` modela nombre, apellido, edad, DNI, género, dirección, medios de contacto obligatorios (email) y opcionales, y contacto predeterminado.
  - `PersonaJuridica` modela razón social, CUIT, rubro, tipo de organización (ONG, Empresa, etc.), representantes y contacto predeterminado.
  - CRUD REST completo expuesto en `DonanteController`.
- **Segmentación Automática de Donaciones — ✅ CUMPLIDO:**
  - `RegistroDonacion` representa la carga única del donante.
  - `SegmentadorDeDonacion` agrupa por `Bien::generarKey` discriminando por subcategoría (unidad mínima), fecha de vencimiento (si es perecedero) y estado nuevo/usado (si aplica).
  - Genera instancias independientes de `Donacion` en estado inicial `EN_DEPOSITO`.
- **Entidades Beneficiarias y Necesidades — ✅ CUMPLIDO:**
  - `EntidadBeneficiaria` con razón social, dirección, teléfono y correo de representantes.
  - `Necesidad` implementa el patrón Strategy:
    - `NecesidadRecurrente`: evalúa satisfacción dentro del período actual.
    - `NecesidadExtraordinaria`: evalúa satisfacción acumulando donaciones parciales hasta cubrir o superar la cantidad requerida.
- **Máquina de Estados y Auditoría — ✅ CUMPLIDO:**
  - 7 estados contemplados: `EN_DEPOSITO`, `ASIGNACION_REALIZADA`, `LISTA_PARA_ENTREGAR`, `EN_TRASLADO`, `ENTREGADA`, `ENTREGA_FALLIDA`, `VENCIDA`.
  - `CambioEstado` es un `@ElementCollection` con `@OrderColumn` que registra fecha, estado, justificación y patente del camión sin sobreescribir la auditoría histórica.
- **Importación Masiva de Donantes (CSV) — ⚠️ PARCIAL:**
  - La clase `ImportadorDeDonantes` parsea el formato CSV, realiza el upsert por email y emite notificación de bienvenida.
  - **Faltante:** No está expuesto ningún endpoint HTTP en `DonanteController` para disparar la importación.

---

### 2.2. Entrega 2: Arquitectura y Modelado en Objetos — Parte II

- **Algoritmos de Asignación (Matchmaking) — ✅ CUMPLIDO:**
  - `CompatibilidadSemantica` y `PrioridadSubAtendidos` generan rankings de hasta 10 entidades.
  - `MotorDeMatchmaking` calcula la intersección; si hay coincidencias la propone, y si no, propone la unión para decisión del administrador.
  - Endpoints REST para consultar propuestas y aceptarlas/rechazarlas (`MatchmakingController`).
- **Ejecución en Horarios de Baja Carga / Asincrónica — ⚠️ PARCIAL:**
  - `DonacionesServiceApplication` incluye `@EnableScheduling`, pero no hay métodos `@Scheduled` programados para ejecutar el matchmaking ni el barrido de donantes inactivos (>20 días).
- **Notificaciones Multi-canal Reales — ✅ CUMPLIDO:**
  - Integración concreta con **Resend** (Email) y **Twilio** (SMS y WhatsApp) mediante el patrón Adapter (`ResendAdapter`, `TwilioAdapter`).
  - Eventos de inactividad, donación asignada, inicio de ruta y entrega exitosa.
  - **Faltante menor:** En `EntregaFallidaListener`, la consigna exige notificar al donante, a la entidad **y a las personas administradoras del sistema** (actualmente solo notifica a los dos primeros).
- **Integración con Planificador Externo de Rutas — ⚠️ PARCIAL:**
  - Lotes de máx 100 donaciones (`TAMANO_LOTE_DONACIONES = 100` en `PlanificadorDeRutas`).
  - URL de callback expuesta en `POST /api/rutas/planificaciones/resultados`.
  - **Faltante 1:** En `PlanificadorDeRutas.ejecutarPlanificacion()`, no se instancian ni persisten las entidades `Ruta` y `Parada` en la base de datos de logística.
  - **Faltante 2:** No se procesan ni reintentan las donaciones devueltas como no asignadas (`donacionesSobrantes` en el DTO).
- **Monitoreo en Tiempo Real — ✅ CUMPLIDO:**
  - Se seleccionó la Opción 1 (Dispositivo GPS con telemetría: latitud, longitud, velocidad, timestamp).
  - Endpoints para recepción de telemetría y consulta de ubicación actual por ruta y camiones activos.
- **División en Microservicios y APIs REST — ✅ CUMPLIDO:**
  - Separación en `donaciones-service`, `logistica-service` y `notificaciones-service`.
  - Comunicación exclusivamente HTTP/REST.

---

### 2.3. Entrega 3: Persistencia de Datos

- **Aislamiento de Esquemas — ✅ CUMPLIDO:**
  - Bases independientes: `donaciones`, `logistica` y `notificaciones`.
  - No hay claves foráneas físicas entre esquemas; los vínculos son identificadores simples (`Long`).
- **ORM y jpa-extras — ✅ CUMPLIDO:**
  - Uso de JPA estándar (`javax.persistence.*`) con Hibernate y `jpa-extras` (`WithSimplePersistenceUnit`).
  - Repositorios genéricos `RepositorioJpa<T>` y `PersistenceContextController` para limpieza de contexto por request.
- **Bases de Datos para Tests y Producción Local — ✅ CUMPLIDO:**
  - HSQLDB en memoria para la ejecución de tests automatizados (`mvn test`).
  - PostgreSQL para perfiles locales configurables mediante variables de entorno.
- **Documentación y Artefactos Entregables — ⚠️ PARCIAL:**
  - `DER.puml`: Completo y validado.
  - `JustificacionesDisenoRelacional.md`: Excelente nivel de justificación de desnormalizaciones y PKs compuestas.
  - `Clases.puml`: Actualizado con los tres servicios.
  - `Despliegue.puml`: Claro y completo.
  - **Faltante 1:** `CasosDeUso.puml` solo refleja la Entrega 1. Falta incorporar los casos de uso de Logística, Chofer, Monitoreo GPS y Matchmaking de la Entrega 2.
  - **Faltante 2:** `Readme.md` no lista `logistica-service` y tiene desactualizado el puerto de `notificaciones-service` (figura 8081 en vez de 8082).

---

## 3. Matriz de Hallazgos y Faltantes

| ID | Prioridad | Módulo | Descripción del Faltante | Impacto |
| :---: | :---: | :--- | :--- | :--- |
| **H-01** | 🔴 **Alta** | `logistica-service` | Al recibir el callback del planificador, no se persisten las rutas ni las paradas, y se descartan las donaciones sobrantes. | Funcionalidad troncal de logística incompleta en base de datos. |
| **H-02** | 🔴 **Alta** | `donaciones` / `logistica` | Faltan las propiedades `rest.*` en los `application.properties` por defecto (`rest.donaciones-url`, `rest.notificaciones-url`, `rest.logistica-url`). | Fallos en tiempo de ejecución (`NullPointerException`) al invocar endpoints entre microservicios. |
| **H-03** | 🟡 **Media** | `donaciones-service` | `ImportadorDeDonantes` no tiene endpoint REST expuesto en `DonanteController`. | No se puede ejecutar la importación masiva por API. |
| **H-04** | 🟡 **Media** | `donaciones-service` | No hay tareas programadas con `@Scheduled` para ejecutar el matchmaking y la detección de donantes inactivos en horarios de baja carga. | Dependencia exclusiva de disparadores manuales HTTP. |
| **H-05** | 🟡 **Media** | `donaciones-service` | `EntregaFallidaListener` no notifica a los administradores del sistema como exige la consigna 2. | Falta de aviso a administradores para replanificación. |
| **H-06** | 🟢 **Baja** | `documents` | `CasosDeUso.puml` no incluye los casos de uso de la Entrega 2. | Inconsistencia documental formal. |
| **H-07** | 🟢 **Baja** | Raíz | `Readme.md` omite `logistica-service` e indica un puerto incorrecto para notificaciones. | Confusión al levantar los servicios en local. |

---

## 4. Detalle de Soluciones Técnicas Propuestas

### Tarea 1: Persistencia de Rutas/Paradas y Manejo de Donaciones Sobrantes (H-01)
- **Ubicación:** `logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/logistica/PlanificadorDeRutas.java`
- **Problema:** En `ejecutarPlanificacion(EjecutarPlanificacionRequest request)`, solo se recorren las donaciones para cambiarles el estado a `LISTA_PARA_ENTREGAR` mediante llamada HTTP a Donaciones. No se guarda ninguna `Ruta` ni `Parada` en `RutaRepository`, y el listado de donaciones sobrantes no se conserva ni reintenta.
- **Solución Propuesta:**
  1. Inyectar `RutaRepository` en `PlanificadorDeRutas`.
  2. Al procesar cada `CamionPlanificacionRequest` recibido en el callback:
     - Buscar el `Camion` por su id.
     - Crear una nueva `Ruta(LocalDate.now().plusDays(1), camion, paradas)`.
     - Por cada `DireccionRequest`, crear una `Parada(orden, destino, entidadId, donacionIds)`.
     - Guardar la ruta completa mediante `rutaRepository.save(ruta)`.
  3. Para las donaciones planificadas, mantener el cambio de estado a `LISTA_PARA_ENTREGAR`.
  4. Para las `donacionesNoAsignadas()` devueltas por el planificador:
     - Registrar en log su condición de no asignadas.
     - Conservar su estado en `ASIGNACION_REALIZADA` para que la próxima ejecución del planificador las vuelva a incluir automáticamente en un lote posterior.

### Tarea 2: Definición de Propiedades de Red por Defecto (H-02)
- **Ubicación:**
  - `donaciones-service/src/main/resources/application.properties`
  - `logistica-service/src/main/resources/application.properties`
- **Problema:** Las clases `RestDonacionesConfig` y `RestLogisticaConfig` mapean el prefijo `rest`, pero los archivos `.properties` no definen valores por defecto para `rest.donaciones-url`, `rest.notificaciones-url` ni `rest.logistica-url`.
- **Solución Propuesta:**
  - En `donaciones-service/src/main/resources/application.properties`:
    ```properties
    rest.notificaciones-url=${REST_NOTIFICACIONES_URL:http://localhost:8082/api}
    ```
  - En `logistica-service/src/main/resources/application.properties`:
    ```properties
    rest.donaciones-url=${REST_DONACIONES_URL:http://localhost:8080/api}
    rest.notificaciones-url=${REST_NOTIFICACIONES_URL:http://localhost:8082/api}
    rest.logistica-url=${REST_LOGISTICA_URL:http://localhost:8081/api}
    ```
  - De esta manera funcionan tanto en desarrollo local por defecto como sobreescritos en Docker o staging mediante variables de entorno.

### Tarea 3: Endpoint REST para Importación Masiva de Donantes (H-03)
- **Ubicación:**
  - `donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonanteController.java`
  - `donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonanteService.java`
- **Problema:** `ImportadorDeDonantes` existe como componente pero no puede invocarse por API.
- **Solución Propuesta:**
  1. Crear un DTO `ImportarCsvRequest(String rutaArchivo)`.
  2. Agregar en `DonanteController`:
     ```java
     app.post("/api/donantes/importacion", ctx -> {
         ImportarCsvRequest request = ctx.bodyAsClass(ImportarCsvRequest.class);
         importadorDeDonantes.importarDonantes(request.rutaArchivo());
         ctx.status(200).result("Importación finalizada con éxito");
     });
     ```
  3. Asegurar que las llamadas a notificaciones se realicen de forma asincrónica con manejo de excepciones para no bloquear la lectura de miles de registros.

### Tarea 4: Automatización de Tareas Programadas en Horarios de Baja Carga (H-04)
- **Ubicación:** `donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/scheduler/TareasProgramadasDonaciones.java`
- **Problema:** La consigna 2 exige ejecutar los algoritmos de asignación y verificaciones en horarios de baja carga. `@EnableScheduling` está activo, pero no hay `@Scheduled`.
- **Solución Propuesta:**
  1. Crear una clase `@Component` dedicada a las tareas calendarizadas.
  2. Implementar un cron nocturno (ej. 02:00 AM y 03:00 AM, parametrizable):
     ```java
     @Scheduled(cron = "${cron.matchmaking:0 0 2 * * ?}")
     public void ejecutarMatchmakingNocturno() {
         motorDeMatchmaking.procesarMatchmaking();
     }

     @Scheduled(cron = "${cron.inactividad:0 0 3 * * ?}")
     public void verificarInactividadNocturna() {
         gestorDeEventos.verificarInactividadDonantes();
     }
     ```
  3. En `logistica-service`, agregar igualmente la tarea programada nocturna para invocar `planificadorDeRutas.planificarRutas()` de cara a la jornada del día siguiente.

### Tarea 5: Notificación a Administradores en Entrega Fallida (H-05)
- **Ubicación:** `donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/eventos/EntregaFallidaListener.java`
- **Problema:** La consigna 2 establece que ante una entrega fallida se notifique a la entidad beneficiaria, al donante **y a personas administradoras del sistema**.
- **Solución Propuesta:**
  1. Agregar una propiedad `notificaciones.admin-email` configurable.
  2. En `EntregaFallidaListener.ejecutar(...)`, además de notificar a donante y entidad, despachar una notificación al email administrativo informando id de la donación, motivo y estado de replanificación.

### Tarea 6: Actualización del Diagrama de Casos de Uso (H-06)
- **Ubicación:** `documents/CasosDeUso.puml`
- **Problema:** El diagrama quedó congelado en la Entrega 1.
- **Solución Propuesta:**
  - Incorporar el actor **Chofer** con los casos de uso:
    - `Iniciar Ruta`
    - `Consultar Ruta Asignada`
  - Incorporar los casos de uso del **Administrador**:
    - `Monitorear Camiones en Tiempo Real`
    - `Evaluar y Aceptar/Rechazar Propuestas de Matchmaking`
  - Incorporar en **Entidad Beneficiaria**:
    - `Confirmar Recepción de Donación`
    - `Subir Fotos de Recepción`
  - Incorporar el actor secundario **Componente Externo de Planificación de Rutas**.

### Tarea 7: Actualización de Documentación Raíz (H-07)
- **Ubicación:** `Readme.md`
- **Problema:** Estructura de carpetas y puertos desactualizados.
- **Solución Propuesta:**
  - Corregir el árbol del proyecto para incluir los tres microservicios:
    - `donaciones-service` (Puerto 8080)
    - `logistica-service` (Puerto 8081)
    - `notificaciones-service` (Puerto 8082)
  - Actualizar los comandos `mvn spring-boot:run` y las instrucciones de Docker.

---

## 5. Checklist de Progreso de Tareas

A medida que se aborde cada tarea, se marcará su casilla para mantener visible el estado del proyecto:

- [x] **Tarea 1 (H-01):** Persistir `Ruta` y `Parada` en `logistica-service` tras el callback del planificador y gestionar donaciones no asignadas.
- [ ] **Tarea 2 (H-02):** Configurar propiedades de red `rest.*` en `application.properties` de `donaciones-service` y `logistica-service`.
- [ ] **Tarea 3 (H-03):** Exponer endpoint REST para la importación masiva de donantes CSV en `DonanteController`.
- [ ] **Tarea 4 (H-04):** Implementar calendarización nocturna con `@Scheduled` para matchmaking, detección de inactividad y planificación logística.
- [ ] **Tarea 5 (H-05):** Incluir notificación a administradores en `EntregaFallidaListener`.
- [ ] **Tarea 6 (H-06):** Actualizar `documents/CasosDeUso.puml` con los casos de uso y actores de Entrega 2.
- [ ] **Tarea 7 (H-07):** Actualizar `Readme.md` con los tres servicios y sus respectivos puertos y comandos.
