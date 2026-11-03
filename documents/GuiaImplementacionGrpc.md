# Guía de Estudio e Implementación: gRPC en DonaTrack (TPA 4)

Este documento explica de punta a punta cómo está diseñado, programado y asegurado el canal **gRPC** entre `logistica-service` y `donaciones-service`, para que cualquier integrante del equipo pueda entenderlo, estudiarlo y defenderlo en la presentación final.

---

## 1. ¿Por qué gRPC y qué reemplaza?

### 1.1 El problema previo (REST sincrónico)
Antes de esta entrega, `logistica-service` se comunicaba con `donaciones-service` haciendo peticiones HTTP tipo `GET` y `PUT` con `RestTemplate`:
* Serializaba objetos Java a texto JSON.
* Abría conexiones HTTP/1.1 para cada bloque de pedidos.
* Parseaba cadenas de texto JSON del otro lado.
* Si cambiaba un endpoint o un nombre de atributo en un JSON, nadie se enteraba hasta que explotaba en runtime con un `NullPointerException` o `404 Not Found`.

### 1.2 La solución adoptada (gRPC sobre HTTP/2)
Se implementó **gRPC** (framework RPC de código abierto desarrollado por Google):
* **Protocol Buffers (Protobuf v3):** Los datos viajan en binario ultra comprimido (campos identificados por números de tag, no por nombres de texto).
* **HTTP/2 multiplexado:** Logística mantiene una única conexión de larga duración abierta hacia Donaciones, por donde viajan múltiples llamadas simultáneas sin bloquearse (*stream multiplexing*).
* **Contrato tipado:** El archivo `donaciones.proto` define las operaciones. El compilador de Maven genera las clases e interfaces Java exactas. Si una firma cambia, el código no compila.

---

## 2. Mapa de Arquitectura y Módulos

```
dsi-tpa-2026-DonaTrack-g8/
├── grpc-contracts/                    <-- [NUEVO] Contratos Protobuf y stubs compilados
│   ├── pom.xml                        <-- Configura protobuf-maven-plugin y os-maven-plugin
│   └── src/main/proto/
│       └── donaciones.proto           <-- Definición de servicios y mensajes
│
├── donaciones-service/                <-- [SERVIDOR gRPC :9090]
│   ├── src/main/java/.../donaciones/
│   │   ├── grpc/
│   │   │   ├── DonacionesGrpcServer.java       <-- Servidor Netty (SmartLifecycle/@PostConstruct)
│   │   │   └── DonacionesGrpcServiceImpl.java  <-- Implementación de los RPCs
│   │   └── security/
│   │       └── ApiKeyGrpcServerInterceptor.java <-- Valida x-api-key del metadata
│   └── src/test/java/.../donaciones/grpc/
│       └── DonacionesGrpcIntegrationTest.java  <-- Test de integración con servidor in-process
│
└── logistica-service/                 <-- [CLIENTE gRPC]
    ├── src/main/java/.../logistica/
    │   ├── config/
    │   │   └── GrpcClientConfig.java           <-- ManagedChannel y BlockingStub
    │   ├── grpc/
    │   │   ├── ApiKeyGrpcClientInterceptor.java <-- Inyecta x-api-key en llamadas
    │   │   └── DonacionesGrpcClient.java        <-- Adaptador de alto nivel para el dominio
    │   └── models/entities/
    │       ├── logistica/PlanificadorDeRutas.java <-- Usa DonacionesGrpcClient
    │       ├── logistica/GestorDeRutas.java       <-- Usa DonacionesGrpcClient
    │       └── eventos/InicioRutaListener.java    <-- Usa DonacionesGrpcClient
```

---

## 3. El Contrato de Servicio (`donaciones.proto`)

Ubicado en `grpc-contracts/src/main/proto/donaciones.proto`.

Define el servicio `DonacionesGrpcService` con los 6 métodos que cubren la totalidad de los requerimientos de logística:

```protobuf
syntax = "proto3";

package ar.edu.utn.frba.ddsi.grpc;

option java_multiple_files = true;
option java_package = "ar.edu.utn.frba.ddsi.grpc";
option java_outer_classname = "DonacionesProto";

service DonacionesGrpcService {
  // 1. Obtiene lotes de donaciones listas para ser ruteadas (PlanificadorDeRutas)
  rpc ListarDonacionesPorEstado (ListarDonacionesPorEstadoRequest) returns (ListarDonacionesPorEstadoResponse);

  // 2. Modifica el estado a LISTA_PARA_ENTREGAR o EN_TRASLADO
  rpc ActualizarEstadoDonacion (ActualizarEstadoDonacionRequest) returns (ActualizarEstadoDonacionResponse);

  // 3. Informa la entrega física de donaciones por parte del camión (GestorDeRutas)
  rpc ConfirmarEntregaDonaciones (ConfirmarEntregaDonacionesRequest) returns (ConfirmarEntregaDonacionesResponse);

  // 4. Obtiene medios de contacto de la entidad para notificar inicio de ruta
  rpc ObtenerContactosEntidad (ObtenerContactosEntidadRequest) returns (ObtenerContactosEntidadResponse);

  // 5. Obtiene el ID del donante a partir del ID de la donación
  rpc ObtenerDonacion (ObtenerDonacionRequest) returns (DonacionDetalleResponse);

  // 6. Obtiene el medio de contacto del donante para enviarle aviso de viaje
  rpc ObtenerContactoDonante (ObtenerContactoDonanteRequest) returns (ContactoDonanteResponse);
}
```

---

## 4. Cómo se compila y genera el código Java

El módulo `grpc-contracts` utiliza dos plugins en Maven:
1. `kr.motd.maven:os-maven-plugin`: Detecta la plataforma operativa (`windows-x86_64`, `linux-x86_64`, etc.) para descargar el binario `protoc` adecuado.
2. `org.xolstice.maven.plugins:protobuf-maven-plugin`: Ejecuta el compilador de Google junto con el generador de stubs gRPC (`protoc-gen-grpc-java`).

Cuando se ejecuta:
```powershell
mvn clean install -pl grpc-contracts
```
Se generan automáticamente en `grpc-contracts/target/generated-sources/protobuf`:
* Las clases de mensajes Java (`ListarDonacionesPorEstadoRequest`, `DonacionItem`, etc.).
* La clase abstracta del servidor: `DonacionesGrpcServiceGrpc.DonacionesGrpcServiceImplBase`.
* El cliente bloqueante sincrónico: `DonacionesGrpcServiceGrpc.DonacionesGrpcServiceBlockingStub`.

Tanto `donaciones-service` como `logistica-service` agregan `grpc-contracts` a su `pom.xml`, obteniendo acceso inmediato a estas clases.

---

## 5. El Servidor en `donaciones-service`

### 5.1 Ciclo de vida (`DonacionesGrpcServer.java`)
Para mantener el mismo patrón arquitectónico que [`JavalinWebServer`](file:///d:/Facultad/Trabajos_Y_Laboratorios/dsi-tpa-2026-DonaTrack-g8/common-lib/src/main/java/ar/edu/utn/frba/ddsi/common/controllers/JavalinWebServer.java):
* Es un `@Component` de Spring anotado con `@Profile("!test")`.
* En `@PostConstruct`, inicia `io.grpc.Server` en el puerto `9090` (configurable mediante `GRPC_PORT`).
* En `@PreDestroy`, ejecuta `server.shutdown()`.
* Al tener `@Profile("!test")`, los tests unitarios y de base de datos (`HSQLDB`) no abren puertos de red innecesariamente.

### 5.2 Implementación del negocio (`DonacionesGrpcServiceImpl.java`)
* Extiende de `DonacionesGrpcServiceGrpc.DonacionesGrpcServiceImplBase`.
* **No duplica lógica de negocio:** Inyecta directamente `DonacionService`, `DonanteService` y `EntidadBeneficiariaService`.
* Traduce los parámetros de Protobuf a los DTOs de dominio existentes, invoca el método del servicio Spring, y devuelve el resultado en el `StreamObserver<Response>`.

---

## 6. El Cliente en `logistica-service`

### 6.1 Canal y Stub (`GrpcClientConfig.java`)
Configura un `ManagedChannel` con `host` y `port` (`DONACIONES_GRPC_HOST` y `DONACIONES_GRPC_PORT`, por defecto `localhost:9090`):
```java
@Bean
public DonacionesGrpcServiceBlockingStub donacionesGrpcBlockingStub(
        ManagedChannel channel,
        ApiKeyGrpcClientInterceptor apiKeyInterceptor) {
    return DonacionesGrpcServiceGrpc.newBlockingStub(channel)
            .withInterceptors(apiKeyInterceptor);
}
```

### 6.2 Adaptador de Alto Nivel (`DonacionesGrpcClient.java`)
Para no acoplar el código de dominio de logística con clases de Protobuf, se creó un adaptador `@Component`:
* Recibe llamadas limpias de Java: `listarPorEstado(estado, limit, offset)`, `actualizarEstado(id, estado, justificacion)`, `confirmarEntrega(...)`.
* Maneja internamente la construcción de mensajes gRPC y el mapeo de retorno a los DTOs propios de logística (`DonacionDTO`, `MedioContacto`).

### 6.3 Clases de dominio refactorizadas
* **`PlanificadorDeRutas.java`:** Pide lotes con `donacionesGrpcClient.listarPorEstado(...)` y marca donaciones planificadas con `donacionesGrpcClient.actualizarEstado(id, "LISTA_PARA_ENTREGAR", ...)`.
* **`GestorDeRutas.java`:** Pasa a `EN_TRASLADO` con `donacionesGrpcClient.actualizarEstado(...)` y confirma entrega final con `donacionesGrpcClient.confirmarEntrega(...)`.
* **`InicioRutaListener.java`:** Obtiene contactos de entidad y donante invocando `donacionesGrpcClient`. *(La llamada para enviar notificaciones sigue usando HTTP RestTemplate hacia `notificaciones-service`).*

---

## 7. Seguridad del Canal (`x-api-key`)

En gRPC no existen los headers tradicionales de HTTP/1.1; se utilizan **Metadatos (`io.grpc.Metadata`)**.

1. **Del lado cliente (`ApiKeyGrpcClientInterceptor.java`):**
   * Antes de salir cada paquete HTTP/2, intercepta la llamada y agrega en los metadatos:
     `x-api-key: <SERVICE_API_KEY>` (por defecto `donatrack-clave-servicios-local`).
2. **Del lado servidor (`ApiKeyGrpcServerInterceptor.java`):**
   * Lee el metadato `x-api-key`.
   * Si la clave no está presente o no coincide con su valor configurado, corta la llamada inmediatamente con:
     `Status.UNAUTHENTICATED` ("Acceso denegado: clave x-api-key ausente o invalida").
   * Ningún cliente no autorizado puede consumir los servicios gRPC de Donaciones.

---

## 8. Verificación y Pruebas

### 8.1 Tests de Integración (`DonacionesGrpcIntegrationTest.java`)
Se implementó un test integral utilizando `InProcessServerBuilder` e `InProcessChannelBuilder`:
1. **Verificación de Seguridad:** Intenta llamar a `listarDonacionesPorEstado` sin enviar la clave y valida que el servidor lance `StatusRuntimeException` con código `UNAUTHENTICATED`.
2. **Verificación Funcional:** Realiza llamadas con la clave correcta y comprueba que la delegación a los servicios de Donaciones y el mapeo de DTOs sea 100% exacto.

### 8.2 Tests Unitarios de Dominio
Se actualizaron los tests de:
* `PlanificadorDeRutasTest.java`: Verifica la interacción con `DonacionesGrpcClient`.
* `GestorDeRutasTest.java`: Verifica el paso a `EN_TRASLADO` y la confirmación de entrega por gRPC.
* `InicioRutaListenerTest.java`: Verifica la consulta de contactos vía gRPC.

---

## 9. Preguntas Típicas de Defensa (Coloquio)

* **¿Por qué gRPC en lugar de GraphQL para esta comunicación?**  
  *Respuesta:* GraphQL está pensado para clientes web/móviles donde se necesita flexibilidad de consulta de datos para evitar *over-fetching*. Entre microservicios internos donde las operaciones son de tipo comando (*procedimientos remotos con tareas específicas de negocio*), gRPC es superior: usa formato binario Protobuf (mucho más rápido de procesar) y multiplexación HTTP/2 nativa.
* **¿Por qué el sitio web sigue usando REST y no gRPC?**  
  *Respuesta:* El navegador web se comunica con `web-service` mediante HTTP y HTML generado por servidor (Handlebars). Los navegadores no soportan llamadas gRPC puras de forma nativa sin proxies complejos (gRPC-Web). REST + Handlebars es el estándar para el frontend liviano, mientras que gRPC brilla en el canal interno inter-microservicios.
* **¿Cómo se autentica el canal gRPC si no hay sesión de usuario?**  
  *Respuesta:* Mediante un secreto compartido de servicio a servicio (`SERVICE_API_KEY`). Se implementaron interceptores (`ServerInterceptor` y `ClientInterceptor`) que viajan en los metadatos de gRPC como `x-api-key`. Si un servicio no envía la clave correcta, el servidor aborta la llamada con `Status.UNAUTHENTICATED`.

