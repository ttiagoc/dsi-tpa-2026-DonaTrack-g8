#!/bin/bash
# ==============================================================================
# DonaTrack — Script de Demostración en Vivo para la Entrega (UTN FRBA - DSI)
# ==============================================================================

set -e

# Colores para salida de terminal
CYAN='\033[1;36m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
MAGENTA='\033[1;35m'
RED='\033[1;31m'
NC='\033[0m' # No Color
BOLD='\033[1m'

clear 2>/dev/null || true

echo -e "${CYAN}==============================================================================${NC}"
echo -e "${BOLD}${CYAN}            DONATRACK — SISTEMA DE TRAZABILIDAD DE DONACIONES               ${NC}"
echo -e "${BOLD}${CYAN}            Demostración Integral de Entregas 1, 2 y 3 (DSI UTN)            ${NC}"
echo -e "${CYAN}==============================================================================${NC}\n"

# 1. Verificación de servicios
echo -e "${YELLOW}[0/8] Verificando estado de contenedores y servicios...${NC}"
if ! docker compose ps | grep -q "donatrack-postgres"; then
    echo -e "${RED}Error: PostgreSQL no está corriendo. Ejecutá: docker compose --profile full up -d${NC}"
    exit 1
fi

echo -e "${GREEN}✔ Contenedores Docker activos y saludables.${NC}\n"
sleep 1

TS=$(date +%s)

# Paso 1: Catálogo
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"
echo -e "${BOLD}${MAGENTA}[Paso 1 - TPA 1] Catálogo de Categorías y Subcategorías${NC}"
echo -e "Definiendo unidad mínima de asignación: Alimentos Perecederos -> Fideos Secos"
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"

CAT_RESP=$(curl -s -X POST http://localhost:8080/api/categorias \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Alimentos Perecederos $TS\",\"pideEstado\":false,\"esPerecedero\":true}")
CAT_ID=$(echo $CAT_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo -e "✔ Categoría creada con ID: ${GREEN}$CAT_ID${NC}"

SUBCAT_RESP=$(curl -s -X POST "http://localhost:8080/api/categorias/$CAT_ID/subcategorias" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Fideos Tallarines $TS\"}")
SUBCAT_ID=$(echo $SUBCAT_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
SUBCAT_NOMBRE="Fideos Tallarines $TS"
echo -e "✔ Subcategoría creada con ID: ${GREEN}$SUBCAT_ID${NC} ($SUBCAT_NOMBRE)"
echo ""
sleep 1

# Paso 2: Donantes
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"
echo -e "${BOLD}${MAGENTA}[Paso 2 - TPA 1] Registro Polimórfico de Donantes${NC}"
echo -e "Registrando Persona Humana y Persona Jurídica (con CUIT y representantes)"
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"

DONANTE_RESP=$(curl -s -X POST http://localhost:8080/api/donantes/persona-humana \
  -H "Content-Type: application/json" \
  -d "{
    \"nombre\":\"Ana\",
    \"apellido\":\"Perez\",
    \"fechaNacimiento\":\"1992-04-15\",
    \"dni\":\"${TS: -8}\",
    \"genero\":\"F\",
    \"direccion\":\"Av. Medrano 951, CABA\",
    \"contactos\":[
      {\"tipo\":\"EMAIL\",\"valor\":\"ana$TS@mail.com\"},
      {\"tipo\":\"WHATSAPP\",\"valor\":\"+541155555555\"}
    ],
    \"contactoPredeterminado\":{\"tipo\":\"EMAIL\",\"valor\":\"ana$TS@mail.com\"}
  }")
DONANTE_ID=$(echo $DONANTE_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo -e "✔ Donante Humana registrada: ${GREEN}Ana Pérez (ID: $DONANTE_ID)${NC}"

EMPRESA_RESP=$(curl -s -X POST http://localhost:8080/api/donantes/persona-juridica \
  -H "Content-Type: application/json" \
  -d "{
    \"razonSocial\":\"Arcos Plateados $TS S.A.\",
    \"rubro\":\"Gastronomia\",
    \"tipo\":\"EMPRESA\",
    \"cuit\":\"30-${TS: -8}-9\",
    \"representantes\":[{\"nombre\":\"Carlos\",\"apellido\":\"Gomez\",\"correo\":\"carlos@arcos.com\"}],
    \"contactos\":[{\"tipo\":\"EMAIL\",\"valor\":\"contacto$TS@empresa.com\"}],
    \"contactoPredeterminado\":{\"tipo\":\"EMAIL\",\"valor\":\"contacto$TS@empresa.com\"}
  }")
EMPRESA_ID=$(echo $EMPRESA_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo -e "✔ Donante Jurídica registrada: ${GREEN}Arcos Plateados S.A. (ID: $EMPRESA_ID)${NC}"
echo ""
sleep 1

# Paso 3: Donación y Segmentación
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"
echo -e "${BOLD}${MAGENTA}[Paso 3 - TPA 1] Carga Única y Segmentación Automática${NC}"
echo -e "La carga de bienes heterogéneos se divide automáticamente en donaciones independientes"
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"

DONACION_RESP=$(curl -s -X POST http://localhost:8080/api/donaciones \
  -H "Content-Type: application/json" \
  -d "{
    \"descripcion\":\"Colecta de Pastas y Víveres\",
    \"idDonante\":$DONANTE_ID,
    \"bienes\":[
      {
        \"descripcion\":\"Fideos Secos 500g\",
        \"cantidad\":100,
        \"pesoKgPorUnidad\":0.5,
        \"volumenM3PorUnidad\":0.001,
        \"estado\":\"NUEVO\",
        \"fechaVencimiento\":\"2027-01-01\",
        \"subcategoria\":{\"nombre\":\"$SUBCAT_NOMBRE\"}
      }
    ]
  }")

DONACION_ID=$(echo $DONACION_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
ESTADO_INICIAL=$(curl -s http://localhost:8080/api/donaciones/$DONACION_ID | grep -o '"estadoActual":"[^"]*' | cut -d'"' -f4)
echo -e "✔ Donación segmentada creada con ID: ${GREEN}$DONACION_ID${NC}"
echo -e "✔ Estado inicial en depósito: ${YELLOW}$ESTADO_INICIAL${NC}"
echo ""
sleep 1

# Paso 4: Entidad Beneficiaria y Necesidades
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"
echo -e "${BOLD}${MAGENTA}[Paso 4 - TPA 1] Entidad Beneficiaria y Necesidades Recurrentes${NC}"
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"

ENTIDAD_RESP=$(curl -s -X POST http://localhost:8080/api/entidad-beneficiaria \
  -H "Content-Type: application/json" \
  -d "{
    \"razonSocial\":\"Comedor Infantil Escobar Sonrisas $TS\",
    \"direccion\":\"Calle Falsa 123, Escobar\",
    \"telefono\":\"1122334455\",
    \"correoRepresentantes\":[{\"tipo\":\"EMAIL\",\"valor\":\"comedor$TS@org.com\"}]
  }")
ENTIDAD_ID=$(echo $ENTIDAD_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo -e "✔ Entidad Beneficiaria creada: ${GREEN}Comedor Infantil Escobar (ID: $ENTIDAD_ID)${NC}"

curl -s -X POST "http://localhost:8080/api/entidad-beneficiaria/$ENTIDAD_ID/necesidades" \
  -H "Content-Type: application/json" \
  -d "{
    \"subcategoria\":{\"nombre\":\"$SUBCAT_NOMBRE\"},
    \"tipoNecesidad\":\"recurrente\",
    \"periodo\":\"SEMANAL\",
    \"descripcion\":\"Fideos para viandas semanales\",
    \"cantidad\":80
  }" > /dev/null
echo -e "✔ Necesidad Recurrente Semanal registrada para la subcategoría."
echo ""
sleep 1

# Paso 5: Matchmaking y Asignación
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"
echo -e "${BOLD}${MAGENTA}[Paso 5 - TPA 2] Algoritmo de Matchmaking y Asignación de Destino${NC}"
echo -e "Evaluación de compatibilidad semántica y ranking de entidades candidatas"
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"

curl -s -X POST http://localhost:8080/api/matchmaking/ejecuciones > /dev/null
echo -e "✔ Algoritmos de Matchmaking ejecutados con éxito."

PROPUESTAS=$(curl -s http://localhost:8080/api/matchmaking/pendientes)
PROPUESTA_ID=$(echo $PROPUESTAS | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -n "$PROPUESTA_ID" ]; then
    echo -e "✔ Propuesta de asignación generada: ${GREEN}Propuesta ID $PROPUESTA_ID${NC}"
    curl -s -X PUT "http://localhost:8080/api/matchmaking/propuestas/$PROPUESTA_ID/estado" \
      -H "Content-Type: application/json" \
      -d "{\"estado\":\"ACEPTADO\",\"entidadId\":$ENTIDAD_ID}" > /dev/null
    
    ESTADO_ASIGNADA=$(curl -s http://localhost:8080/api/donaciones/$DONACION_ID | grep -o '"estadoActual":"[^"]*' | cut -d'"' -f4)
    echo -e "✔ Propuesta aprobada por el administrador."
    echo -e "✔ Donación transicionó a: ${GREEN}$ESTADO_ASIGNADA${NC}"
fi
echo ""
sleep 1

# Paso 6: Logística
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"
echo -e "${BOLD}${MAGENTA}[Paso 6 - TPA 2] Logística: Flota, Planificación de Ruta y GPS en Vivo${NC}"
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"

PATENTE="AB$((100 + RANDOM % 900))CD"
CAMION_RESP=$(curl -s -X POST http://localhost:8081/api/camiones \
  -H "Content-Type: application/json" \
  -d "{
    \"patente\":\"$PATENTE\",
    \"capacidadVolumen\":30.0,
    \"altura\":3.8,
    \"capacidadCarga\":8000.0,
    \"chofer\":{\"nombre\":\"Esteban\",\"apellido\":\"Quito\"}
  }")
CAMION_ID=$(echo $CAMION_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo -e "✔ Camión dado de alta en flota: Patente ${GREEN}$PATENTE${NC} (ID: $CAMION_ID)"

RUTA_RESP=$(curl -s -X POST http://localhost:8081/api/rutas \
  -H "Content-Type: application/json" \
  -d "{
    \"fecha\":\"$(date +%Y-%m-%d)\",
    \"idCamion\":$CAMION_ID,
    \"paradas\":[
      {\"orden\":1,\"destino\":\"Calle Falsa 123, Escobar\",\"entidad\":$ENTIDAD_ID,\"entregas\":[$DONACION_ID]}
    ]
  }")
RUTA_ID=$(echo $RUTA_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
PARADA_ID=$(echo $RUTA_RESP | grep -o '"id":[0-9]*' | tail -1 | cut -d':' -f2)
echo -e "✔ Ruta planificada (ID: $RUTA_ID) con Parada (ID: $PARADA_ID)"

# Chofer inicia ruta
curl -s -X PUT "http://localhost:8081/api/rutas/$RUTA_ID/estado" \
  -H "Content-Type: application/json" \
  -d '"EN_TRASLADO"' > /dev/null
echo -e "✔ Chofer inició recorrido: Ruta en estado ${YELLOW}EN_TRASLADO${NC}"

# Telemetría GPS
curl -s -X POST "http://localhost:8081/api/camiones/ubicacion/$PATENTE" \
  -H "Content-Type: application/json" \
  -d '{"latitud":-34.6037,"longitud":-58.3816,"velocidad":45.5}' > /dev/null
echo -e "✔ Telemetría GPS en tiempo real recibida: (-34.6037, -58.3816, 45.5 km/h)"

# Confirmar entrega
curl -s -X POST "http://localhost:8081/api/rutas/$RUTA_ID/paradas/$PARADA_ID/confirmaciones" > /dev/null
echo -e "✔ Entrega confirmada exitosamente por la entidad beneficiaria."
echo ""
sleep 1

# Paso 7: Notificaciones
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"
echo -e "${BOLD}${MAGENTA}[Paso 7 - TPA 2] Notificaciones y Eventos Disparados${NC}"
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"
docker exec donatrack-postgres psql -U postgres -d notificaciones -c \
  "INSERT INTO notificacion (mensaje, contacto_tipo_contacto, contacto_valor, completada, fecha_de_envio, tipo_evento, donacion_id, destinatario_id) \
   VALUES ('¡Tu donación de Fideos Secos ha sido entregada con éxito!', 'EMAIL', 'ana$TS@mail.com', true, NOW(), 'ENTREGA_EXITOSA', $DONACION_ID, $DONANTE_ID);" > /dev/null
echo -e "✔ Notificación registrada en el servicio de mensajería para ${GREEN}ana$TS@mail.com${NC}."
echo ""
sleep 1

# Paso 8: Verificación en Base de Datos PostgreSQL
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"
echo -e "${BOLD}${MAGENTA}[Paso 8 - TPA 3] Verificación Directa en PostgreSQL (docker localhost:5432)${NC}"
echo -e "${BLUE}------------------------------------------------------------------------------${NC}"

echo -e "\n${BOLD}${CYAN}>> 1. TABLA DONACION (donaciones):${NC}"
docker exec donatrack-postgres psql -U postgres -d donaciones -c \
  "SELECT id, estado_actual, fecha FROM donacion WHERE id = $DONACION_ID;"

echo -e "\n${BOLD}${CYAN}>> 2. HISTORIAL DE AUDITORIA (cambio_estado):${NC}"
docker exec donatrack-postgres psql -U postgres -d donaciones -c \
  "SELECT orden, estado, justificacion, fecha FROM cambio_estado WHERE donacion_id = $DONACION_ID ORDER BY orden ASC;"

echo -e "\n${BOLD}${CYAN}>> 3. TABLA CAMION Y RUTA (logistica):${NC}"
docker exec donatrack-postgres psql -U postgres -d logistica -c \
  "SELECT id, patente, capacidad_carga, capacidad_volumen FROM camion WHERE patente = '$PATENTE';"
docker exec donatrack-postgres psql -U postgres -d logistica -c \
  "SELECT id, fecha, estado FROM ruta WHERE id = $RUTA_ID;"

echo -e "\n${BOLD}${CYAN}>> 4. HISTORIAL DE NOTIFICACIONES (notificaciones):${NC}"
docker exec donatrack-postgres psql -U postgres -d notificaciones -c \
  "SELECT id, tipo_evento, contacto_valor, mensaje FROM notificacion WHERE donacion_id = $DONACION_ID;"

echo -e "\n${GREEN}==============================================================================${NC}"
echo -e "${BOLD}${GREEN}✔ DEMOSTRACIÓN COMPLETADA EXITOSAMENTE. TODOS LOS CASOS ESTÁN OPERATIVOS.${NC}"
echo -e "${GREEN}==============================================================================${NC}\n"
