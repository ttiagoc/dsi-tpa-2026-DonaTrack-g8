#!/usr/bin/env python3
"""
Carga datos de demostración para recorrer todos los flujos de DonaTrack.
Equivalente multiplataforma (macOS / Linux / Windows) de cargar-datos-demo.ps1.

Todo pasa por las APIs de los servicios (no por SQL), así los estados, el historial,
las notificaciones y los incentivos se generan con las mismas reglas que en la app.
"""

import os
import sys
import json
import time
import struct
import zlib
import urllib.request
import urllib.error
from datetime import datetime, timedelta
from pathlib import Path

# Configuración por defecto
DONACIONES_URL = os.environ.get('DONACIONES_API_URL', 'http://localhost:8080/api')
LOGISTICA_URL = os.environ.get('LOGISTICA_API_URL', 'http://localhost:8081/api')
ADMIN_USER = os.environ.get('ADMIN_USERNAME', 'admin')
ADMIN_PASS = os.environ.get('ADMIN_PASSWORD', 'admin1234')
PASSWORD = 'demo1234'

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent.parent
FOTOS_DIRS = [
    PROJECT_ROOT / 'web-service' / 'fotos',
    PROJECT_ROOT / 'fotos'
]

def step(msg):
    print(f"\033[1;36m==> {msg}\033[0m")

def ok(msg):
    print(f"    \033[1;32m{msg}\033[0m")

def fail(msg):
    print(f"    \033[1;31m{msg}\033[0m")

def invoke_api(method, url, body=None, token=None, content_type='application/json; charset=utf-8', raw_body=False):
    headers = {}
    if token:
        headers['Authorization'] = f"Bearer {token}"
    
    data = None
    if body is not None:
        headers['Content-Type'] = content_type
        if raw_body or isinstance(body, bytes):
            data = body if isinstance(body, bytes) else str(body).encode('utf-8')
        else:
            data = json.dumps(body).encode('utf-8')
            
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read()
            if not content:
                return None
            try:
                return json.loads(content.decode('utf-8'))
            except Exception:
                return content.decode('utf-8')
    except urllib.error.HTTPError as e:
        err_body = e.read().decode('utf-8', errors='replace')
        raise RuntimeError(f"{method} {url} falló ({e.code}): {e.reason} -> {err_body}")

def login(u, p):
    res = invoke_api('POST', f"{DONACIONES_URL}/auth/login", {'username': u, 'password': p})
    return res['accessToken']

def email_contact(v):
    return {'tipo': 'EMAIL', 'valor': v}

def new_foto_demo(filename, label, color_hex):
    # Generar un archivo PNG válido de 800x600 con color de fondo
    r = int(color_hex[1:3], 16)
    g = int(color_hex[3:5], 16)
    b = int(color_hex[5:7], 16)
    w, h = 800, 600

    row_bg = b'\x00' + bytes([r, g, b] * w)
    # Franja blanca central
    row_badge = b'\x00' + bytes([r, g, b] * 60) + bytes([245, 245, 245] * 680) + bytes([r, g, b] * 60)

    raw = bytearray()
    for y in range(h):
        if 200 <= y < 400:
            raw.extend(row_badge)
        else:
            raw.extend(row_bg)

    compressed = zlib.compress(bytes(raw))
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)
    png_data = (
        b'\x89PNG\r\n\x1a\n'
        + struct.pack('>I', len(ihdr)) + b'IHDR' + ihdr + struct.pack('>I', zlib.crc32(b'IHDR' + ihdr))
        + struct.pack('>I', len(compressed)) + b'IDAT' + compressed + struct.pack('>I', zlib.crc32(b'IDAT' + compressed))
        + struct.pack('>I', 0) + b'IEND' + struct.pack('>I', zlib.crc32(b'IEND'))
    )

    for fdir in FOTOS_DIRS:
        fdir.mkdir(parents=True, exist_ok=True)
        with open(fdir / filename, 'wb') as f:
            f.write(png_data)

    return f"/fotos/{filename}"

def main():
    print("=" * 70)
    print("  DonaTrack - Carga de Datos de Demostración (Seed)")
    print("=" * 70)

    # 1. Admin y catálogo
    step("Sesión de administrador")
    admin_token = login(ADMIN_USER, ADMIN_PASS)
    ok(f"Sesión iniciada como {ADMIN_USER}")

    step("Catálogo (categorías y subcategorías)")
    catalogo = {
        'Alimentos': {'pideEstado': False, 'esPerecedero': True, 'subs': ['Leche', 'Fideos secos', 'Arroz', 'Conservas']},
        'Ropa y calzado': {'pideEstado': True, 'esPerecedero': False, 'subs': ['Abrigos', 'Calzado', 'Ropa de ninos']},
        'Higiene personal': {'pideEstado': False, 'esPerecedero': False, 'subs': ['Jabon y shampoo', 'Panales']},
        'Utiles escolares': {'pideEstado': False, 'esPerecedero': False, 'subs': ['Cuadernos', 'Lapices y lapiceras']},
        'Muebles y colchones': {'pideEstado': True, 'esPerecedero': False, 'subs': ['Colchones', 'Sillas y mesas']}
    }
    for nombre, c in catalogo.items():
        cat = invoke_api('POST', f"{DONACIONES_URL}/categorias", {
            'nombre': nombre,
            'pideEstado': c['pideEstado'],
            'esPerecedero': c['esPerecedero']
        }, token=admin_token)
        cat_id = cat['id']
        for sub in c['subs']:
            invoke_api('POST', f"{DONACIONES_URL}/categorias/{cat_id}/subcategorias", {'nombre': sub}, token=admin_token)
        ok(f"{nombre} ({', '.join(c['subs'])})")

    # 2. Entidades beneficiarias
    step("Entidades beneficiarias (con usuario)")
    def new_entidad(usuario, razon_social, direccion, telefono, correo):
        r = invoke_api('POST', f"{DONACIONES_URL}/auth/registro/entidad-beneficiaria", {
            'username': usuario,
            'password': PASSWORD,
            'entidad': {
                'razonSocial': razon_social,
                'direccion': direccion,
                'telefono': telefono,
                'correoRepresentantes': [email_contact(correo)]
            }
        })
        perfil_id = r['usuario']['perfilId']
        ok(f"{razon_social} (usuario: {usuario}, id {perfil_id})")
        return {
            'id': perfil_id,
            'token': r['accessToken'],
            'direccion': direccion,
            'nombre': razon_social
        }

    comedor = new_entidad('comedor', 'Comedor Los Pibes', 'Av. Rivadavia 6500, Flores, CABA', '1145671234', 'comedor.lospibes@example.com')
    hogar = new_entidad('hogar', 'Hogar de Ancianos Santa Ana', 'Av. San Juan 3100, San Cristobal, CABA', '1149012345', 'hogar.santaana@example.com')
    escuela = new_entidad('escuela', 'Escuela Comunitaria N 12', 'Calle Lavalleja 850, Villa Crespo, CABA', '1148556677', 'escuela12@example.com')
    merendero = new_entidad('merendero', 'Merendero Rayito de Sol', 'Av. Eva Peron 4200, Parque Avellaneda, CABA', '1146223344', 'rayitodesol@example.com')

    step("Necesidades")
    def new_necesidad(entidad, sub, tipo, periodo, descripcion, cantidad, unidad):
        invoke_api('POST', f"{DONACIONES_URL}/entidad-beneficiaria/{entidad['id']}/necesidades", {
            'subcategoria': {'nombre': sub},
            'tipoNecesidad': tipo,
            'periodo': periodo,
            'descripcion': descripcion,
            'cantidad': cantidad,
            'unidad': unidad
        }, token=entidad['token'])
        cada = periodo.lower() if periodo else 'extraordinaria'
        ok(f"{entidad['nombre']}: {cantidad} {unidad.lower()} de {sub} ({cada})")

    new_necesidad(comedor, 'Leche', 'recurrente', 'SEMANAL', 'Leche para el desayuno de 60 chicos', 40, 'LITROS')
    new_necesidad(comedor, 'Fideos secos', 'recurrente', 'MENSUAL', 'Fideos para el almuerzo de lunes a viernes', 50, 'KILOGRAMOS')
    new_necesidad(comedor, 'Panales', 'extraordinaria', None, 'Panales talle G para la sala maternal', 80, 'UNIDADES')
    new_necesidad(hogar, 'Abrigos', 'extraordinaria', None, 'Camperas y frazadas para el invierno', 30, 'UNIDADES')
    new_necesidad(hogar, 'Jabon y shampoo', 'recurrente', 'MENSUAL', 'Higiene diaria de 25 residentes', 40, 'UNIDADES')
    new_necesidad(hogar, 'Calzado', 'extraordinaria', None, 'Pantuflas y zapatillas comodas', 20, 'UNIDADES')
    new_necesidad(hogar, 'Colchones', 'extraordinaria', None, 'Colchones de una plaza para renovar camas', 6, 'UNIDADES')
    new_necesidad(escuela, 'Cuadernos', 'recurrente', 'ANUAL', 'Cuadernos para el ciclo lectivo', 200, 'UNIDADES')
    new_necesidad(escuela, 'Lapices y lapiceras', 'recurrente', 'ANUAL', 'Kits de utiles para primer grado', 150, 'UNIDADES')
    new_necesidad(escuela, 'Ropa de ninos', 'extraordinaria', None, 'Guardapolvos y ropa talle 6 a 10', 40, 'UNIDADES')
    new_necesidad(merendero, 'Leche', 'recurrente', 'DIARIO', 'Leche para la merienda', 10, 'LITROS')
    new_necesidad(merendero, 'Arroz', 'recurrente', 'MENSUAL', 'Arroz para las viandas', 30, 'KILOGRAMOS')
    new_necesidad(merendero, 'Conservas', 'recurrente', 'MENSUAL', 'Conservas de atun y legumbres', 60, 'UNIDADES')

    # 3. Donantes
    step("Donantes (con usuario)")
    def new_humana(usuario, nombre, apellido, nacimiento, dni, genero, direccion, correo):
        r = invoke_api('POST', f"{DONACIONES_URL}/auth/registro/persona-humana", {
            'username': usuario,
            'password': PASSWORD,
            'donante': {
                'nombre': nombre,
                'apellido': apellido,
                'fechaNacimiento': nacimiento,
                'dni': dni,
                'genero': genero,
                'direccion': direccion,
                'contactos': [email_contact(correo)],
                'contactoPredeterminado': email_contact(correo)
            }
        })
        perfil_id = r['usuario']['perfilId']
        ok(f"{nombre} {apellido} (usuario: {usuario}, id {perfil_id})")
        return {'id': perfil_id, 'token': r['accessToken']}

    def new_juridica(usuario, razon_social, rubro, tipo, cuit, rep_nombre, rep_apellido, correo):
        r = invoke_api('POST', f"{DONACIONES_URL}/auth/registro/persona-juridica", {
            'username': usuario,
            'password': PASSWORD,
            'donante': {
                'razonSocial': razon_social,
                'rubro': rubro,
                'tipo': tipo,
                'cuit': cuit,
                'representantes': [{'nombre': rep_nombre, 'apellido': rep_apellido, 'correo': correo}],
                'contactos': [email_contact(correo)],
                'contactoPredeterminado': email_contact(correo)
            }
        })
        perfil_id = r['usuario']['perfilId']
        ok(f"{razon_social} (usuario: {usuario}, id {perfil_id})")
        return {'id': perfil_id, 'token': r['accessToken']}

    ana = new_humana('ana', 'Ana', 'Perez', '1992-04-15', '35123456', 'F', 'Av. Medrano 951, Almagro, CABA', 'ana.perez@example.com')
    bruno = new_humana('bruno', 'Bruno', 'Diaz', '1987-09-02', '33987654', 'M', 'Av. Cabildo 2200, Belgrano, CABA', 'bruno.diaz@example.com')
    estrella = new_juridica('estrella', 'Supermercados La Estrella S.A.', 'Supermercados', 'EMPRESA', '30-71111111-5', 'Carla', 'Gimenez', 'donaciones@laestrella.example.com')
    manos = new_juridica('manos', 'Fundacion Manos Unidas', 'Asistencia social', 'ONG', '30-72222222-6', 'Diego', 'Ruiz', 'contacto@manosunidas.example.com')

    step("Donante cargado por el administrador (donación presencial, sin usuario)")
    carlos = invoke_api('POST', f"{DONACIONES_URL}/donantes/persona-humana", {
        'nombre': 'Carlos', 'apellido': 'Gomez', 'fechaNacimiento': '1975-01-20', 'dni': '25444333', 'genero': 'M',
        'direccion': 'Av. Corrientes 4000, Almagro, CABA',
        'contactos': [email_contact('carlos.gomez@example.com')],
        'contactoPredeterminado': email_contact('carlos.gomez@example.com')
    }, token=admin_token)
    ok(f"Carlos Gomez (id {carlos['id']})")

    # 4. Donaciones
    hoy = datetime.now()
    def fecha(dias):
        return (hoy + timedelta(days=dias)).strftime('%Y-%m-%d')

    def bien(sub, desc, cant, peso, vol, estado='NUEVO', vence=None):
        b = {
            'descripcion': desc,
            'cantidad': cant,
            'pesoKgPorUnidad': peso,
            'volumenM3PorUnidad': vol,
            'estado': estado,
            'subcategoria': {'nombre': sub}
        }
        if vence:
            b['fechaVencimiento'] = vence
        return b

    def new_donacion(donante_id, token, descripcion, bienes):
        r = invoke_api('POST', f"{DONACIONES_URL}/donaciones", {
            'descripcion': descripcion,
            'idDonante': donante_id,
            'bienes': bienes
        }, token=token)
        if isinstance(r, dict):
            r = [r]
        por_sub = {}
        sub_list = []
        for d in r:
            por_sub[d['subcategoria']] = d['id']
            sub_list.append(f"#{d['id']} {d['subcategoria']}")
        ok(f"{descripcion} -> {len(r)} donación(es): {', '.join(sub_list)}")
        return por_sub

    step("Donaciones que pasan por matchmaking")
    dEstrella1 = new_donacion(estrella['id'], estrella['token'], 'Excedente de stock de almacen', [
        bien('Fideos secos', 'Fideos tallarines 500 g', 100, 0.5, 0.001, 'NUEVO', fecha(300)),
        bien('Arroz', 'Arroz largo fino 1 kg', 50, 1.0, 0.0012, 'NUEVO', fecha(360))
    ])
    dAnaCuadernos = new_donacion(ana['id'], ana['token'], 'Utiles que sobraron de la oficina', [
        bien('Cuadernos', 'Cuadernos tapa dura 48 hojas', 40, 0.3, 0.0008)
    ])
    dManos = new_donacion(manos['id'], manos['token'], 'Colecta de panales', [
        bien('Panales', 'Panales talle G x 30', 60, 1.2, 0.01)
    ])
    dAnaJabon = new_donacion(ana['id'], ana['token'], 'Kit de higiene', [
        bien('Jabon y shampoo', 'Jabon de tocador y shampoo', 30, 0.4, 0.0006)
    ])
    dBruno = new_donacion(bruno['id'], bruno['token'], 'Lapices para la escuela del barrio', [
        bien('Lapices y lapiceras', 'Caja de 12 lapices de colores', 100, 0.15, 0.0003)
    ])
    dEstrella2 = new_donacion(estrella['id'], estrella['token'], 'Conservas de la semana', [
        bien('Conservas', 'Lata de atun 170 g', 80, 0.2, 0.0003, 'NUEVO', fecha(500))
    ])
    dAnaCalzado = new_donacion(ana['id'], ana['token'], 'Zapatillas en buen estado', [
        bien('Calzado', 'Zapatillas talle 40', 15, 0.8, 0.006, 'USADO')
    ])
    dAnaLeche = new_donacion(ana['id'], ana['token'], 'Leche para merenderos', [
        bien('Leche', 'Leche larga vida 1 L', 24, 1.03, 0.001, 'NUEVO', fecha(45))
    ])
    dAnaAbrigos = new_donacion(ana['id'], ana['token'], 'Ropa de invierno', [
        bien('Abrigos', 'Camperas de abrigo adulto', 10, 1.1, 0.02, 'USADO')
    ])
    dAnaColchon = new_donacion(ana['id'], ana['token'], 'Colchon de una plaza', [
        bien('Colchones', 'Colchon de espuma 1 plaza', 1, 9.0, 0.35, 'USADO')
    ])

    step("Matchmaking")
    invoke_api('POST', f"{DONACIONES_URL}/matchmaking/ejecuciones", {}, token=admin_token)
    propuestas = invoke_api('GET', f"{DONACIONES_URL}/matchmaking/pendientes", token=admin_token)
    if isinstance(propuestas, dict):
        propuestas = [propuestas]
    propuesta_de = {int(p['donacionId']): p for p in propuestas}
    ok(f"{len(propuestas)} propuestas generadas")

    def aceptar(donacion_id, entidad):
        p = propuesta_de.get(int(donacion_id))
        if not p:
            raise RuntimeError(f"No hay propuesta para la donación {donacion_id}")
        invoke_api('PUT', f"{DONACIONES_URL}/matchmaking/propuestas/{p['id']}/estado", {
            'estado': 'ACEPTADO', 'entidadId': entidad['id']
        }, token=admin_token)
        ok(f"Donación #{donacion_id} asignada a {entidad['nombre']}")

    aceptar(dEstrella1['Fideos secos'], comedor)
    aceptar(dEstrella1['Arroz'], merendero)
    aceptar(dAnaCuadernos['Cuadernos'], escuela)
    aceptar(dManos['Panales'], comedor)
    aceptar(dAnaJabon['Jabon y shampoo'], hogar)
    aceptar(dBruno['Lapices y lapiceras'], escuela)
    aceptar(dEstrella2['Conservas'], merendero)
    aceptar(dAnaCalzado['Calzado'], hogar)

    pColchon = propuesta_de.get(int(dAnaColchon['Colchones']))
    if pColchon:
        invoke_api('PUT', f"{DONACIONES_URL}/matchmaking/propuestas/{pColchon['id']}/estado", {
            'estado': 'RECHAZADO'
        }, token=admin_token)
        ok(f"Propuesta de la donación #{dAnaColchon['Colchones']} (colchón) rechazada")
    ok(f"Quedan pendientes: leche #{dAnaLeche['Leche']} y abrigos #{dAnaAbrigos['Abrigos']}")

    # 5. Logística
    step("Camiones")
    def new_camion(patente, vol, alto, carga, nombre, apellido):
        c = invoke_api('POST', f"{LOGISTICA_URL}/camiones", {
            'patente': patente, 'capacidadVolumen': vol, 'altura': alto, 'capacidadCarga': carga,
            'chofer': {'nombre': nombre, 'apellido': apellido}
        }, token=admin_token)
        ok(f"{patente} ({nombre} {apellido})")
        return c

    camion1 = new_camion('AE123BC', 30.0, 3.8, 8000.0, 'Esteban', 'Quiroga')
    camion2 = new_camion('AF456DE', 18.0, 3.2, 4500.0, 'Marta', 'Ledesma')
    camion3 = new_camion('AD789FG', 12.0, 2.9, 3000.0, 'Ruben', 'Acosta')

    step("Cuentas de acceso para choferes")
    def new_usuario_chofer(camion, username):
        invoke_api('POST', f"{DONACIONES_URL}/auth/chofer", {
            'camionId': camion['id'],
            'username': username,
            'password': PASSWORD
        }, token=admin_token)
        ok(f"Usuario {username} vinculado al camión {camion['patente']}")

    new_usuario_chofer(camion2, 'chofer1')
    new_usuario_chofer(camion1, 'chofer2')
    new_usuario_chofer(camion3, 'chofer3')

    def parada(orden, entidad, donaciones):
        return {'orden': orden, 'destino': entidad['direccion'], 'entidad': entidad['id'], 'entregas': donaciones}

    def new_ruta(f_str, camion, paradas):
        return invoke_api('POST', f"{LOGISTICA_URL}/rutas", {
            'fecha': f_str, 'idCamion': camion['id'], 'paradas': paradas
        }, token=admin_token)

    def set_estado_ruta(ruta, estado):
        invoke_api('PUT', f"{LOGISTICA_URL}/rutas/{ruta['id']}/estado",
                   f'"{estado}"', token=admin_token, raw_body=True)

    def parada_de(ruta, entidad):
        for p in ruta['paradas']:
            if p['entidad'] == entidad['id']:
                return p
        return None

    step("Ruta de ayer: entregas realizadas")
    ruta1 = new_ruta(fecha(-1), camion1, [
        parada(1, comedor, [dManos['Panales']]),
        parada(2, hogar, [dAnaJabon['Jabon y shampoo']]),
        parada(3, escuela, [dBruno['Lapices y lapiceras']]),
        parada(4, merendero, [dEstrella2['Conservas']])
    ])
    set_estado_ruta(ruta1, 'EN_TRASLADO')
    invoke_api('POST', f"{LOGISTICA_URL}/camiones/ubicacion/{camion1['patente']}", {
        'latitud': -34.5986, 'longitud': -58.4202, 'velocidad': 0.0
    }, token=admin_token)
    for e in [comedor, hogar, escuela, merendero]:
        p = parada_de(ruta1, e)
        if p:
            invoke_api('POST', f"{LOGISTICA_URL}/rutas/{ruta1['id']}/paradas/{p['id']}/confirmaciones", {}, token=admin_token)
    set_estado_ruta(ruta1, 'FINALIZADA')
    ok(f"Ruta #{ruta1['id']} ({camion1['patente']}): 4 paradas entregadas y ruta finalizada")

    step("Ruta con entrega fallida")
    ruta0 = new_ruta(fecha(-2), camion3, [
        parada(1, hogar, [dAnaCalzado['Calzado']])
    ])
    set_estado_ruta(ruta0, 'EN_TRASLADO')
    invoke_api('POST', f"{LOGISTICA_URL}/rutas/{ruta0['id']}/paradas/{ruta0['paradas'][0]['id']}/fallos", {
        'motivo': 'El hogar estaba cerrado al llegar el camion. Se reprograma la entrega.'
    }, token=admin_token)
    ok(f"Donación #{dAnaCalzado['Calzado']} (calzado) en ENTREGA_FALLIDA y Ruta #{ruta0['id']} finalizada")

    step("Ruta de hoy: camión en camino (seguimiento en vivo)")
    ruta2 = new_ruta(fecha(0), camion2, [
        parada(1, comedor, [dEstrella1['Fideos secos']]),
        parada(2, merendero, [dEstrella1['Arroz']])
    ])
    set_estado_ruta(ruta2, 'EN_TRASLADO')
    invoke_api('POST', f"{LOGISTICA_URL}/camiones/ubicacion/{camion2['patente']}", {
        'latitud': -34.5986, 'longitud': -58.4202, 'velocidad': 0.0
    }, token=admin_token)
    ok(f"Ruta #{ruta2['id']} ({camion2['patente']}) EN_TRASLADO hacia Comedor Los Pibes y Merendero Rayito de Sol")

    step("Ruta de mañana: planificada")
    ruta3 = new_ruta(fecha(1), camion3, [
        parada(1, escuela, [dAnaCuadernos['Cuadernos']])
    ])
    invoke_api('PUT', f"{DONACIONES_URL}/donaciones/{dAnaCuadernos['Cuadernos']}/estado", {
        'estado': 'LISTA_PARA_ENTREGAR',
        'justificacion': f"Incluida en la ruta del {fecha(1)} ({camion3['patente']})"
    }, token=admin_token)
    ok(f"Ruta #{ruta3['id']} ({camion3['patente']}) PLANIFICADA; cuadernos #{dAnaCuadernos['Cuadernos']} LISTA_PARA_ENTREGAR")

    # 6. Recepción por las entidades
    step("Recepciones confirmadas por las entidades (con fotos)")
    fotoJabon1 = new_foto_demo('demo-recepcion-jabon-1.png', 'Kits de higiene recibidos', '#0f766e')
    fotoJabon2 = new_foto_demo('demo-recepcion-jabon-2.png', 'Residentes con sus kits', '#0e7490')
    fotoLapices = new_foto_demo('demo-recepcion-lapices.png', 'Lapices de colores en el aula', '#b45309')
    fotoConservas = new_foto_demo('demo-recepcion-conservas.png', 'Conservas en la despensa', '#7c3aed')

    invoke_api('POST', f"{DONACIONES_URL}/donaciones/{dAnaJabon['Jabon y shampoo']}/recepcion", {
        'fotosUrl': [fotoJabon1, fotoJabon2]
    }, token=hogar['token'])
    invoke_api('POST', f"{DONACIONES_URL}/donaciones/{dBruno['Lapices y lapiceras']}/recepcion", {
        'fotosUrl': [fotoLapices]
    }, token=escuela['token'])
    invoke_api('POST', f"{DONACIONES_URL}/donaciones/{dEstrella2['Conservas']}/recepcion", {
        'fotosUrl': [fotoConservas]
    }, token=merendero['token'])
    ok("Hogar Santa Ana, Escuela N 12 y Merendero confirmaron su recepción")
    ok(f"Pañales #{dManos['Panales']} quedan ENTREGADA sin confirmar (para probar la pantalla con 'comedor')")

    # 7. Depósito
    step("Donaciones que quedan en depósito (para el admin)")
    dCarlos = new_donacion(carlos['id'], admin_token, 'Donación presencial en el depósito', [
        bien('Leche', 'Leche entera sachet 1 L', 12, 1.03, 0.001, 'NUEVO', fecha(2)),
        bien('Fideos secos', 'Fideos mostachol 500 g', 20, 0.5, 0.001, 'NUEVO', fecha(-3))
    ])
    invoke_api('PUT', f"{DONACIONES_URL}/donaciones/{dCarlos['Fideos secos']}/estado", {
        'estado': 'VENCIDA',
        'justificacion': 'Producto vencido detectado en el control del depósito.'
    }, token=admin_token)
    ok(f"Fideos #{dCarlos['Fideos secos']} marcados VENCIDA; leche #{dCarlos['Leche']} vence en 2 días")
    dBruno2 = new_donacion(bruno['id'], bruno['token'], 'Ropa que les quedó chica a mis hijos', [
        bien('Ropa de ninos', 'Guardapolvos talle 8', 6, 0.3, 0.002, 'USADO')
    ])
    dManos2 = new_donacion(manos['id'], manos['token'], 'Sillas para comedores', [
        bien('Sillas y mesas', 'Sillas plásticas apilables', 20, 2.5, 0.05, 'NUEVO')
    ])

    # 8. Importación de donantes
    step("Importación masiva de donantes (CSV)")
    csv_path = SCRIPT_DIR / 'donantes-importacion.csv'
    if csv_path.exists():
        with open(csv_path, 'rb') as f:
            csv_bytes = f.read()
        headers = {'Authorization': f"Bearer {admin_token}", 'X-Filename': 'donantes-importacion.csv', 'Content-Type': 'text/csv'}
        req = urllib.request.Request(f"{DONACIONES_URL}/donantes/importaciones", data=csv_bytes, headers=headers, method='POST')
        try:
            with urllib.request.urlopen(req) as resp:
                tarea = json.loads(resp.read().decode('utf-8'))
                time.sleep(2)
                tarea = invoke_api('GET', f"{DONACIONES_URL}/donantes/importaciones/{tarea['id']}", token=admin_token)
                ok(f"Importación {tarea.get('estado')}: {json.dumps(tarea, ensure_ascii=False)}")
        except Exception as e:
            fail(f"Error al importar CSV: {e}")

    # Resumen final
    step("Listo")
    print(f"""
    Sitio: http://localhost:7070   (contraseña de todos: {PASSWORD}, salvo admin / {ADMIN_PASS})

    admin      Matchmaking con 2 propuestas pendientes (leche, abrigos) y 4 donaciones en depósito sin propuesta
               (colchón rechazado, leche que vence en 2 días, ropa de niños, sillas): "Ejecutar" genera nuevas.
               Depósito con una donación vencida. 3 camiones. Importación con filas creadas y con errores.
    ana        Donante ORO: donaciones en todos los estados (depósito, asignada, lista, entregada, fallida).
    bruno      Donante nuevo con una donación entregada y otra en depósito.
    estrella   Empresa: una carga segmentada en fideos + arroz, hoy en camino.
    manos      ONG: pañales entregados sin confirmar, sillas en depósito.
    comedor    Pañales para confirmar recepción; fideos en camino (seguimiento).
    hogar      Jabón recibido con fotos; calzado con entrega fallida.
    escuela    Lápices recibidos; cuadernos listos para la ruta de mañana.
    merendero  Conservas recibidas; arroz en camino.
    chofer1    Marta Ledesma (camión AF456DE): hoja de ruta EN_TRASLADO hoy con 2 paradas pendientes.
    chofer2    Esteban Quiroga (camión AE123BC): sin ruta activa hoy (disponible para nuevas planificaciones).
    chofer3    Rubén Acosta (camión AD789FG): hoja de ruta PLANIFICADA mañana lista para iniciar recorrido.
    """)

if __name__ == '__main__':
    main()

