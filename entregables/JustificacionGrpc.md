# Justificación de Diseño — Comunicación Inter-Servicios: gRPC vs GraphQL

**Materia:** Diseño de Sistemas de Información (UTN FRBA)  
**Proyecto:** DonaTrack — Plataforma de Gestión y Logística de Donaciones  
**Entrega:** TPA 4 — Arquitectura de Integración (Requerimiento 3)

---

## 1. Contexto del Problema

En la arquitectura de microservicios de **DonaTrack**, el servicio extraído (`logistica-service`) necesita interactuar de forma intensiva y sincrónica con el servicio central (`donaciones-service`):
1. **Planificación de rutas:** Consulta lotes de donaciones en estado `ASIGNACION_REALIZADA` y actualiza masivamente sus estados a `LISTA_PARA_ENTREGAR`.
2. **Ejecución de rutas:** Notifica el inicio de viaje pasando estados a `EN_TRASLADO`.
3. **Cierre de entregas:** Registra la confirmación física de entrega con los IDs de paradas y camiones.
4. **Notificaciones de trayecto:** Consulta en tiempo real los datos y medios de contacto de donantes y entidades beneficiarias.

Originalmente, esta comunicación se realizaba mediante llamadas HTTP/REST (`RestTemplate` con payloads JSON). La consigna de la Entrega 4 exige evaluar e incorporar una tecnología moderna de integración de alto rendimiento entre ambos componentes.

---

## 2. Comparativa Tecnológica: gRPC vs GraphQL

| Criterio | gRPC (Google Remote Procedure Call) | GraphQL | REST Tradicional (Línea Base) |
|---|---|---|---|
| **Formato de Serialización** | **Binario (Protocol Buffers - Protobuf)**: Ultracompacto, sin nombres de campos redundantes en el payload. | **Texto plano (JSON)**: Verboso, requiere serialización y parseo de strings en cada salto. | **Texto plano (JSON)**: Verboso. |
| **Protocolo de Transporte** | **HTTP/2 nativo**: Multiplexación sobre una única conexión TCP, compresión de encabezados HPACK, soporte de streaming bidireccional. | **HTTP/1.1 o HTTP/2**: Generalmente sobre HTTP POST tradicional. | **HTTP/1.1**: Conexiones concurrentes múltiples, bloqueo de cabeza de línea (*head-of-line blocking*). |
| **Definición de Contrato** | **Estricto en `.proto`**: Tipado fuerte, validado en tiempo de compilación. Generación automática de clases/stubs en Java. | **Schema SDL (Schema Definition Language)**: Fuertemente tipado en el schema, pero habitualmente validado en tiempo de ejecución. | **Flexible o informal**: Depende de documentación externa (OpenAPI/Swagger) sin garantías en compilación. |
| **Caso de Uso Óptimo** | **Comunicación interna Backend-to-Backend (Microservicios)** con alta frecuencia de mensajes y baja latencia. | **Comunicación Frontend-to-Backend (BFF)** con clientes móviles/web heterogéneos que necesitan evitar *over-fetching*. | **APIs públicas / Web universal**: Amplia compatibilidad con navegadores y clientes HTTP estándar. |
| **Sobrecarga de CPU** | **Mínima**: La deserialización binaria de Protobuf es hasta 5 a 10 veces más rápida que parsear árboles JSON. | **Media/Alta**: El servidor debe parsear el AST (árbol sintáctico) de la consulta GraphQL y resolver campo por campo. | **Media**: Parseo estándar de JSON. |
| **Operaciones de Acción (RPC)** | **Natural**: Modela procedimientos remotos como métodos (`ActualizarEstadoDonacion`, `ConfirmarEntregaDonaciones`). | **Inadecuado o Forzado**: Diseñado para grafos de entidades; las acciones de negocio deben modelarse como *Mutations* complejas. | **Mediante verbos HTTP**: `PUT`, `POST`, `PATCH` con semántica REST. |

---

## 3. Justificación de la Elección de gRPC

Se seleccionó **gRPC sobre HTTP/2 con Protobuf** para la comunicación entre `logistica-service` y `donaciones-service` por las siguientes razones de arquitectura:

### 3.1 Naturaleza orientada a operaciones (RPC)
El flujo entre Logística y Donaciones **no es una consulta de grafos exploratorios**, sino una secuencia de **llamadas a procedimientos bien definidos**: *"actualizá el estado de la donación X"*, *"dame el lote de donaciones para planificar"*, *"confirmá la entrega del camión Y"*. En este escenario, GraphQL introduce una complejidad innecesaria (árboles de ejecución, resolutores desacoplados y sobrecarga de queries complejas), mientras que gRPC mapea de forma 1:1 estas operaciones a métodos tipados.

### 3.2 Rendimiento y Eficiencia de Red (HTTP/2 + Protobuf)
1. **Reducción del payload:** Las donaciones viajan en formato binario codificado con tags numéricos (varints). No se transmiten strings repetitivos como `"direccion":`, `"volumen":`, `"peso":` en cada elemento del lote.
2. **Multiplexación de conexiones:** Logística mantiene un único canal de larga duración (`ManagedChannel`) hacia Donaciones. Múltiples hilos concurrentes de planificación y actualización reutilizan la misma conexión TCP sin sufrir bloqueos ni tener que abrir y cerrar sockets continuamente.

### 3.3 Seguridad en Tiempo de Compilación (Type-Safety)
Al definir el contrato en `grpc-contracts/src/main/proto/donaciones.proto`, el plugin `protobuf-maven-plugin` genera stubs e interfaces Java en ambos servicios. Si Donaciones modifica la firma de un método o un tipo de dato, el proyecto **no compila**, detectando inconsistencias antes de llegar a los tests o a producción.

---

## 4. Arquitectura de Seguridad del Canal gRPC

A diferencia de las APIs REST públicas que se autentican con tokens JWT de usuario en el navegador, el canal gRPC es **estrictamente interno (servicio a servicio)**.

1. **Autenticación por Metadata (`x-api-key`):**
   * El cliente en `logistica-service` incluye un `ApiKeyGrpcClientInterceptor` que inyecta en los metadatos gRPC la clave secreta compartida (`SERVICE_API_KEY`).
   * El servidor en `donaciones-service` cuenta con un `ApiKeyGrpcServerInterceptor` que intercepta toda llamada entrante: si la clave no coincide, cierra la conexión inmediatamente con código gRPC `Status.UNAUTHENTICATED` (código HTTP/2 equivalente a 401), sin llegar a ejecutar código de negocio.
2. **Aislamiento en Red Privada:**
   * En el despliegue cloud (Docker / Render / Railway), el puerto gRPC (`9090`) solo se expone hacia la red privada interna del cluster, permaneciendo inaccesible desde el Internet público.

