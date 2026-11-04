# Flujos de prueba con los datos de demostración

Guía para recorrer DonaTrack de punta a punta con los datos que carga
[`docker/datos-demo/cargar-datos-demo.ps1`](../docker/datos-demo/cargar-datos-demo.ps1). Para cada usuario dice con qué se
encuentra al entrar y qué flujos probar, con el resultado esperado. Lo que hoy no funciona está marcado con ⚠️ y apunta a
[CorreccionesTPA4.md](CorreccionesTPA4.md).

## 1. Preparar el entorno

Necesitás Docker Desktop abierto. Desde la raíz del repo:

```powershell
.\run.ps1 seed
```

Ese comando detiene los servicios, **borra la base**, compila, levanta PostgreSQL y los 4 servicios, y carga los datos.
Tarda 1 o 2 minutos. Al terminar imprime un resumen por usuario.

- Sitio: **http://localhost:7070**
- Cada flujo **modifica los datos** (asignar, confirmar, vencer…). Para volver al punto de partida, correr `.\run.ps1 seed` de nuevo.
- Los números de donación (`#1`, `#2`…) y de ruta coinciden con los de esta guía porque el seed siempre arranca de una base vacía.
- Los mails son de `example.com`: las notificaciones se generan, pero no le llegan a nadie de verdad.

## 2. Cómo iniciar sesión

1. Entrar a http://localhost:7070 y hacer clic en **Ingresar** (arriba a la derecha), o ir directo a http://localhost:7070/login.
2. Completar **Nombre de usuario** y **Contraseña** con los datos de la tabla.
3. Hacer clic en **Ingresar**. Cada rol cae en su panel (`/admin`, `/donante` o `/entidad`) con su menú lateral.
4. Para cambiar de usuario: hacer clic en el perfil (arriba a la derecha) y elegir **Salir**.

Para probar dos roles a la vez sin salir y entrar todo el tiempo, usar una ventana normal y otra de incógnito (o dos navegadores):
la sesión es una cookie.

| Usuario | Contraseña | Rol | Quién es |
|---|---|---|---|
| `admin` | `admin1234` | Administrador | Administrador inicial (lo crea `donaciones-service` al arrancar) |
| `ana` | `demo1234` | Donante · persona humana | Ana Pérez. Donante frecuente (nivel ORO), con donaciones en casi todos los estados |
| `bruno` | `demo1234` | Donante · persona humana | Bruno Díaz. Donante nuevo (nivel inicial) |
| `estrella` | `demo1234` | Donante · persona jurídica (empresa) | Supermercados La Estrella S.A. |
| `manos` | `demo1234` | Donante · persona jurídica (ONG) | Fundación Manos Unidas |
| `comedor` | `demo1234` | Entidad beneficiaria | Comedor Los Pibes |
| `hogar` | `demo1234` | Entidad beneficiaria | Hogar de Ancianos Santa Ana |
| `escuela` | `demo1234` | Entidad beneficiaria | Escuela Comunitaria N 12 |
| `merendero` | `demo1234` | Entidad beneficiaria | Merendero Rayito de Sol |

También existe **Carlos Gómez** (DNI 25444333): un donante que cargó el admin en una donación presencial. **No tiene usuario**.

## 3. Mapa de los datos

### Donaciones

| # | Donante | Bienes | Estado inicial | Entidad | Para probar |
|---|---|---|---|---|---|
| 1 | estrella | 100 fideos 500 g | En traslado | Comedor | Seguimiento del camión AF456DE |
| 2 | estrella | 50 arroz 1 kg | En traslado | Merendero | Misma carga que la #1, segmentada por subcategoría |
| 3 | ana | 40 cuadernos | Lista para entregar | Escuela | Ruta planificada para mañana |
| 4 | manos | 60 pañales | Entregada, **sin confirmar** | Comedor | Confirmar recepción con fotos |
| 5 | ana | 30 jabón y shampoo | Entregada y confirmada (2 fotos) | Hogar | Mapa público con fotos |
| 6 | bruno | 100 lápices | Entregada y confirmada (1 foto) | Escuela | Mapa público |
| 7 | estrella | 80 conservas | Entregada y confirmada (1 foto) | Merendero | Mapa público |
| 8 | ana | 15 zapatillas (usadas) | Entrega fallida | Hogar | Estado con justificación |
| 9 | ana | 24 leche | En depósito, **propuesta pendiente** (2 sugeridas) | — | Aceptar una propuesta |
| 10 | ana | 10 camperas (usadas) | En depósito, **propuesta pendiente** (1 sugerida) | — | Descartar una propuesta |
| 11 | ana | 1 colchón | En depósito (su propuesta fue rechazada) | — | Volver a ejecutar el matchmaking |
| 12 | Carlos | 20 fideos | Vencida | — | Filtro de vencidas en Depósito |
| 13 | Carlos | 12 leche (vence en 2 días) | En depósito | — | Marcar vencida |
| 14 | bruno | 6 guardapolvos | En depósito | — | Matchmaking nuevo |
| 15 | manos | 20 sillas | En depósito | — | Matchmaking sin necesidad compatible |

### Logística

| Ruta | Camión (chofer) | Fecha | Estado | Paradas |
|---|---|---|---|---|
| 1 | AE123BC (Esteban Quiroga) | ayer | Finalizada | Comedor #4, Hogar #5, Escuela #6, Merendero #7 (todas entregadas) |
| 2 | AD789FG (Rubén Acosta) | anteayer | Finalizada | Hogar #8 (no recibida) |
| 3 | AF456DE (Marta Ledesma) | hoy | **En traslado**, a 38,5 km/h | Comedor #1, Merendero #2 |
| 4 | AD789FG (Rubén Acosta) | mañana | Planificada | Escuela #3 |

### Necesidades de cada entidad

- **Comedor:** leche (semanal), fideos (mensual), pañales (extraordinaria).
- **Hogar:** abrigos, calzado y colchones (extraordinarias), jabón y shampoo (mensual).
- **Escuela:** cuadernos y lápices (anual), ropa de niños (extraordinaria).
- **Merendero:** leche (diaria), arroz y conservas (mensual).

---

## 4. Sin sesión (req. 1 a 4)

### 4.1 Landing y legales
1. Entrar a http://localhost:7070.
2. ✅ Se ve el propósito de DonaTrack y links al mapa, al registro, a Ingresar y a "Términos y privacidad" (al pie).

### 4.2 Mapa de entregas (req. 2)
1. Entrar a **Mapa de entregas** (`/mapa`).
2. ✅ Arriba: "4 donaciones entregadas" y "4 entidades". En el mapa hay un marcador por entidad.
3. Hacer clic en cada marcador. ✅ El panel muestra la entidad, su dirección y lo que recibió.
   Hogar, Escuela y Merendero tienen fotos; el Comedor no, porque todavía no confirmó la recepción de los pañales.
4. ⚠️ El mapa es ilustrativo: las posiciones no son las direcciones reales (Correcciones, punto 8).

### 4.3 Registro (req. 3)
1. **Registrarme** → elegir persona, empresa u organización, o entidad beneficiaria.
2. Probar los errores: dejar campos vacíos, poner una contraseña de menos de 8 caracteres, contraseñas que no coinciden y el usuario
   `ana` (ya existe). ✅ Cada error se marca en su campo y no se pierde lo escrito.
3. Registrar una cuenta válida (por ejemplo, el usuario `prueba1`). ✅ Entra directo al panel de su rol.

### 4.4 Inicio de sesión y permisos (req. 4)
1. Ingresar con `ana` / `clave-mal`. ✅ "Usuario o contraseña incorrectos", sin decir cuál de los dos.
2. Ingresar con `ana` / `demo1234`. ✅ Va a `/donante`.
3. Con esa sesión, entrar a http://localhost:7070/admin. ✅ Error 403, "No tenés acceso a esta página".
4. Con sesión, entrar a `/login`. ✅ Redirige al panel (las páginas solo para visitantes no se muestran).

---

## 5. Donante

### 5.1 Mis donaciones y filtros (req. 5) · `ana`
1. Menú **Mis donaciones**. ✅ 6 donaciones: #3, #5, #8, #9, #10 y #11.
2. Estado = **En depósito** → **Filtrar**. ✅ Quedan #9, #10 y #11.
3. Categoría = **Ropa y calzado**. ✅ Quedan #8 y #10 (con el estado en "Todos").
4. Elegir una combinación sin resultados (por ejemplo, Vencida). ✅ Mensaje de lista vacía.
5. **Limpiar**. ✅ Vuelven las 6.
6. ⚠️ No hay filtro por subcategoría, los botones de filtro están desalineados y la fecha y el estado se ven crudos (Correcciones, punto 5).

### 5.2 Entidades beneficiarias (req. 6) · cualquier donante
1. Menú **Entidades**. ✅ Las 4 entidades con su dirección y sus necesidades.
2. ⚠️ No aparece el nombre de las entidades y la pantalla sale sin estilos (Correcciones, punto 6).

### 5.3 Incentivos (req. 7)
| Usuario | Resultado esperado |
|---|---|
| `ana` | "Donante frecuente", 6 donaciones, las 3 misiones cumplidas y 3 insignias |
| `bruno` | "Donante inicial", 2 donaciones: "Primera donación" cumplida, "Donante solidario" 2/3 y "Héroe comunitario" 2/5 |
| `estrella` | "Solo personas humanas", sin misiones. ⚠️ El menú no debería mostrar Incentivos a una jurídica (Correcciones, punto 7) |

**Flujo para subir de misión:** entrar como `admin` → **Recepción** → registrar una donación a nombre de **Bruno Díaz** (ver 7.1) →
volver a entrar como `bruno` → **Incentivos**. ✅ "Donante solidario" 3/3 cumplida, con insignia nueva y nivel PLATA.

### 5.4 Seguimiento en mapa (req. 9)
| Usuario | Resultado esperado |
|---|---|
| `estrella` | Mapa con el camión **AF456DE** (Marta Ledesma, 38,5 km/h) y sus 2 paradas pendientes: Comedor y Merendero |
| `ana` | "No hay entregas en traslado activo": su única donación en logística (#3) va en la ruta de mañana |

### 5.5 Notificaciones (req. 8) · `ana`, `bruno`, `estrella`, `manos`
1. Menú **Notificaciones**.
2. ⚠️ Hoy siempre dice "Bandeja al día": las notificaciones se generan sin destinatario (Correcciones, punto 1).
   Con eso corregido, `ana` debería ver: asignación de #3, #5 y #8, "en camino", entrega de #5, entrega fallida de #8 y las 3 misiones cumplidas.

### 5.6 Panel del donante
- `/donante` dice "Próximamente: registrar donaciones y seguir su estado". El donante no carga donaciones desde la web: lo hace el
  admin en Recepción (req. 15). Ver Correcciones, punto 9.

---

## 6. Entidad beneficiaria

### 6.1 Necesidades (req. 10) · `comedor`
1. Menú **Necesidades**. ✅ Leche (semanal, 40 litros), fideos (mensual, 50 kg) y pañales (extraordinaria, 80 unidades).
2. **+ Nueva necesidad** → enviar vacío. ✅ El modal sigue abierto con los errores marcados.
3. Cargar "Arroz", "¿Cada cuánto la necesitan?" = mensual, 20 kilogramos. ✅ Aparece en el listado con un aviso de éxito.
4. **Eliminar** la necesidad de arroz recién creada. ✅ Pide confirmación nombrando la necesidad y después la borra.
5. **Eliminar** la de **fideos**. ✅ No deja: tiene una donación en camino (#1).

### 6.2 Donaciones asignadas (req. 11)
| Usuario | Resultado esperado |
|---|---|
| `comedor` | #4 pañales "Entregada · falta confirmar" (con botón **Confirmar recepción**) y #1 fideos "En camino" |
| `hogar` | #5 jabón "Recibida" y #8 zapatillas "Entrega fallida", con el motivo |
| `escuela` | #6 lápices "Recibida" y #3 cuadernos "Lista para entregar" |
| `merendero` | #7 conservas "Recibida" y #2 arroz "En camino" |

### 6.3 Confirmar recepción con fotos (req. 12) · `comedor`
1. **Donaciones asignadas** → **Confirmar recepción** en la #4.
2. Enviar sin fotos. ✅ Error: hay que cargar al menos una.
3. Cargar 1 a 5 fotos JPG o PNG de hasta 5 MB. Sirven las que dejó el seed en la carpeta `fotos/` de la raíz del repo
   (`demo-recepcion-*.png`) o cualquier foto propia.
4. Confirmar. ✅ La #4 pasa a "Recibida".
5. Salir y abrir el **Mapa de entregas**: el Comedor Los Pibes ahora muestra la foto.

### 6.4 Seguimiento en mapa (req. 14)
- `comedor` o `merendero` → **Seguimiento**. ✅ Camión AF456DE en el mapa, con chofer, velocidad y las paradas.
- `hogar` → **Seguimiento**. ✅ Sin entregas en traslado.

### 6.5 Notificaciones (req. 13)
- ⚠️ Igual que en el donante: la bandeja aparece vacía (Correcciones, punto 1).

---

## 7. Administrador · `admin` / `admin1234`

### 7.1 Recepción en depósito (req. 15)
1. Menú **Recepción**.
2. En "Donante", buscar `Gomez` o `25444333`. ✅ Aparece Carlos Gómez.
3. Agregar dos bienes de **subcategorías distintas**, por ejemplo 10 "Leche" y 5 "Abrigos" (estado Usado; Ropa pide estado).
4. **Registrar recepción en depósito**. ✅ Se crean **2 donaciones**, una por subcategoría (segmentación automática), en estado En depósito.
5. **+ Nuevo donante**: dar de alta una persona humana desde el modal. ✅ Queda elegida en el formulario.
6. ⚠️ No se puede dar de alta una persona jurídica desde acá (Correcciones, punto 9).

### 7.2 Depósito y vencimientos (req. 16)
1. Menú **Depósito**. ✅ 6 donaciones en depósito (#9, #10, #11, #13, #14 y #15) y 1 vencida (#12, en la pestaña Vencidas).
2. **Marcar vencida** en la **#13** (leche que vence en 2 días). ✅ Pide confirmación y pasa a Vencidas.
3. Si se marca vencida una donación con propuesta pendiente (#9 o #10), esa propuesta ya no se puede aceptar.

### 7.3 Matchmaking (req. 17)
1. Menú **Matchmaking**. ✅ 2 propuestas pendientes: #9 leche (2 entidades sugeridas) y #10 camperas (1).
2. **Ver sugerencias** en la #9. ✅ Modal con Comedor y Merendero, ordenados.
3. Elegir **Comedor Los Pibes** → **Confirmar asignación**. ✅ La propuesta desaparece. Como `comedor`, la #9 aparece en Donaciones asignadas.
4. **Ver sugerencias** en la #10 → **Descartar propuesta**. ✅ La #10 sigue en depósito, sin propuesta.
5. **Ejecutar matchmaking**. ✅ Se generan propuestas para las donaciones en depósito que no tenían: #10, #11 (colchón → Hogar),
   #13 si no se venció (leche), #14 (guardapolvos → Escuela) y #15. La #15 (sillas) no coincide con ninguna necesidad, así que
   sugiere todas las entidades, ordenadas por las que menos recibieron.
6. ⚠️ Los indicadores "12 entidades candidatas" y "85 %" son fijos (Correcciones, otros hallazgos).

### 7.4 Camiones (req. 18)
1. Menú **Camiones**. ✅ 3 camiones: AE123BC, AF456DE y AD789FG.
2. **+ Nuevo camión** → enviar vacío. ✅ Errores en el modal.
3. Crear uno, por ejemplo con patente `AA000ZZ`. ✅ Aparece en el listado.
4. **Editar** ese camión y cambiarle la capacidad. ✅ Se actualiza.
5. **Eliminar** ese camión. ✅ Pide confirmación y lo borra.
   Conviene no borrar los 3 del seed: tienen rutas asociadas.

### 7.5 Importación de donantes (req. 19)
1. Menú **Importar CSV**.
2. Subir [`docker/datos-demo/donantes-importacion.csv`](../docker/datos-demo/donantes-importacion.csv) → **Comenzar importación masiva**.
3. ✅ Barra de progreso. Al terminar: 5 filas, **3 creados** (Lucía Fernández, Martín Sosa y Panadería El Trigal) y
   **2 descartadas** (una sin documento y otra con tipo de persona inválido), con el detalle de cada error.
4. Subir **el mismo archivo otra vez**. ✅ 0 creados y **3 actualizados**: los reconoce por email y no los duplica.
5. ⚠️ El historial también muestra 3 importaciones de ejemplo (IMP-102 a IMP-104) que nunca existieron.
   Los donantes importados **no reciben usuario** y no se puede ver qué se importó en cada proceso (Correcciones, puntos 4 y 9).

---

## 8. Flujo completo de punta a punta

Para mostrar el circuito entero con una donación nueva. Usar dos ventanas: una como `admin` y otra como la entidad o el donante.

1. **admin** → Recepción: registrar 30 "Conservas" a nombre de **Bruno Díaz**.
2. **admin** → Matchmaking → **Ejecutar matchmaking** → **Ver sugerencias** de la donación nueva → asignarla a **Merendero Rayito de Sol**.
3. **merendero** → Donaciones asignadas: ✅ la nueva aparece como asignada.
4. **bruno** → Mis donaciones: ✅ la ve con estado "Asignación realizada". → Incentivos: ✅ 3 donaciones, "Donante solidario" cumplida.
5. La logística (rutas, salida del camión y entrega) no tiene pantallas: se hace por la API de `logistica-service`.
   Para seguir el circuito, en PowerShell (reemplazar `<ID>` por el número de la donación nueva):

   ```powershell
   $t = (Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login -ContentType 'application/json' -Body '{"username":"admin","password":"admin1234"}').accessToken
   $h = @{ Authorization = "Bearer $t" }
   $ruta = Invoke-RestMethod -Method Post http://localhost:8081/api/rutas -Headers $h -ContentType 'application/json' -Body '{"fecha":"2026-11-05","idCamion":1,"paradas":[{"orden":1,"destino":"Av. Eva Peron 4200, Parque Avellaneda, CABA","entidad":4,"entregas":[<ID>]}]}'
   Invoke-RestMethod -Method Put "http://localhost:8081/api/rutas/$($ruta.id)/estado" -Headers $h -ContentType 'application/json' -Body '"EN_TRASLADO"'
   Invoke-RestMethod -Method Post http://localhost:8081/api/camiones/ubicacion/AE123BC -Headers $h -ContentType 'application/json' -Body '{"latitud":-34.64,"longitud":-58.47,"velocidad":25}'
   ```
   ✅ **merendero** y **bruno** → Seguimiento: el camión AE123BC aparece en camino.

   ```powershell
   Invoke-RestMethod -Method Post "http://localhost:8081/api/rutas/$($ruta.id)/paradas/$($ruta.paradas[0].id)/confirmaciones" -Headers $h
   ```
   ✅ **merendero** → Donaciones asignadas: "Entregada · falta confirmar".
6. **merendero** → Confirmar recepción con una foto. ✅ La donación pasa a "Recibida" y aparece en el **Mapa de entregas** público.

## 9. Volver a empezar

```powershell
.\run.ps1 seed
```

Otros comandos útiles: `.\run.ps1 status` (estado de los servicios y de la base), `.\run.ps1 logs donaciones` (log en vivo) y
`.\run.ps1 stop` (detiene todo).
