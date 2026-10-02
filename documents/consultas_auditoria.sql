-- =============================================================================
-- DONATRACK — CONSULTAS DE AUDITORÍA Y TRAZABILIDAD (ENTREGA 3 - DSI UTN FRBA)
-- =============================================================================
-- Este archivo contiene las consultas SQL para auditar el funcionamiento
-- integral del sistema sobre los tres esquemas de datos:
-- 1. donaciones (PostgreSQL localhost:5432/donaciones)
-- 2. logistica  (PostgreSQL localhost:5432/logistica)
-- 3. notificaciones (PostgreSQL localhost:5432/notificaciones)
-- =============================================================================

-- =============================================================================
-- 1. ESQUEMA: donaciones
-- =============================================================================

-- 1.1 Trazabilidad Integral de Donaciones
-- Muestra el ciclo de vida de cada donación: donante origen, subcategoría,
-- entidad asignada y estado actual.
SELECT 
    d.id AS donacion_id,
    d.fecha AS fecha_ingreso,
    s.nombre AS subcategoria,
    c.nombre AS categoria,
    d.estado_actual,
    CASE 
        WHEN don.tipo_donante = 'PERSONA_HUMANA' THEN don.nombre || ' ' || don.apellido
        WHEN don.tipo_donante = 'PERSONA_JURIDICA' THEN don.razon_social
    END AS donante,
    don.tipo_donante,
    eb.razon_social AS entidad_asignada,
    d.fecha_vencimiento
FROM donacion d
JOIN subcategoria s ON d.subcategoria_id = s.id
JOIN categoria c ON s.categoria_id = c.id
JOIN donante don ON d.donante_id = don.id
LEFT JOIN entidad_beneficiaria eb ON d.entidad_beneficiaria_asignada_id = eb.id
ORDER BY d.id DESC;


-- 1.2 Auditoría Histórica de Estados (CambioEstado - ElementCollection)
-- Demuestra el registro inmutable de transiciones con orden, fecha y justificación.
SELECT 
    ce.donacion_id,
    ce.orden,
    ce.estado,
    ce.justificacion,
    ce.patente_camion,
    ce.fecha
FROM cambio_estado ce
ORDER BY ce.donacion_id DESC, ce.orden ASC;


-- 1.3 Donantes y sus Medios de Contacto Polimórficos
SELECT 
    d.id,
    d.tipo_donante,
    COALESCE(d.nombre || ' ' || d.apellido, d.razon_social) AS nombre_o_razon_social,
    d.dni,
    d.cuit,
    d.tipo_organizacion,
    dc.tipo_contacto,
    dc.valor AS contacto_valor,
    (d.contacto_predeterminado_tipo_contacto || ': ' || d.contacto_predeterminado_valor) AS predeterminado
FROM donante d
LEFT JOIN donante_contactos dc ON d.id = dc.donante_id
ORDER BY d.id DESC;


-- 1.4 Entidades Beneficiarias y sus Necesidades
SELECT 
    eb.id AS entidad_id,
    eb.razon_social,
    eb.direccion,
    n.id AS necesidad_id,
    s.nombre AS subcategoria_requerida,
    n.tipo_necesidad,
    n.periodo,
    n.cantidad AS cantidad_solicitada,
    COUNT(d.id) AS donaciones_cubiertas
FROM entidad_beneficiaria eb
LEFT JOIN necesidad n ON eb.id = n.entidad_beneficiaria_id
LEFT JOIN subcategoria s ON n.subcategoria_id = s.id
LEFT JOIN donacion d ON d.necesidad_id = n.id
GROUP BY eb.id, eb.razon_social, eb.direccion, n.id, s.nombre, n.tipo_necesidad, n.periodo, n.cantidad
ORDER BY eb.id DESC;


-- 1.5 Propuestas de Matchmaking Generadas por los Algoritmos
SELECT 
    rm.id AS propuesta_id,
    rm.donacion_id,
    rm.fecha_ejecucion,
    rm.estado AS estado_propuesta,
    STRING_AGG(eb.razon_social, ', ') AS entidades_sugeridas
FROM resultado_matchmaking rm
LEFT JOIN resultado_matchmaking_entidades_sugeridas rmes ON rm.id = rmes.resultado_matchmaking_id
LEFT JOIN entidad_beneficiaria eb ON rmes.entidades_sugeridas_id = eb.id
GROUP BY rm.id, rm.donacion_id, rm.fecha_ejecucion, rm.estado
ORDER BY rm.id DESC;


-- =============================================================================
-- 2. ESQUEMA: logistica
-- =============================================================================

-- 2.1 Flota de Camiones y Choferes
SELECT 
    c.id,
    c.patente,
    c.chofer_nombre || ' ' || c.chofer_apellido AS chofer,
    c.capacidad_carga AS peso_max_kg,
    c.capacidad_volumen AS volumen_max_m3,
    c.altura AS altura_max_m
FROM camion c
ORDER BY c.id DESC;


-- 2.2 Rutas, Estados y Paradas
SELECT 
    r.id AS ruta_id,
    r.fecha,
    r.estado AS estado_ruta,
    c.patente AS camion_patente,
    c.chofer_nombre || ' ' || c.chofer_apellido AS chofer,
    p.id AS parada_id,
    p.orden,
    p.destino,
    p.estado AS estado_parada,
    p.entidad_id
FROM ruta r
JOIN camion c ON r.camion_id = c.id
LEFT JOIN parada p ON r.id = p.ruta_id
ORDER BY r.id DESC, p.orden ASC;


-- 2.3 Donaciones asignadas a cada parada
SELECT 
    p.ruta_id,
    p.orden AS parada_orden,
    p.destino,
    pd.donacion_id
FROM parada p
JOIN parada_donaciones pd ON p.id = pd.parada_id
ORDER BY p.ruta_id DESC, p.orden ASC;


-- =============================================================================
-- 3. ESQUEMA: notificaciones
-- =============================================================================

-- 3.1 Historial de Notificaciones Emitidas
SELECT 
    n.id,
    n.fecha_de_envio,
    n.tipo_evento,
    n.contacto_tipo_contacto AS canal,
    n.contacto_valor AS destinatario,
    n.mensaje,
    n.completada,
    n.donacion_id,
    n.destinatario_id
FROM notificacion n
ORDER BY n.id DESC;
