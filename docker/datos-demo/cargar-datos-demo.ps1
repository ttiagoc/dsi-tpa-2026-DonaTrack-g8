<#
.SYNOPSIS
    Carga datos de demostracion para recorrer todos los flujos de DonaTrack.

.DESCRIPTION
    Todo pasa por las APIs de los servicios (no por SQL), asi los estados, el historial,
    las notificaciones y los incentivos se generan con las mismas reglas que en la app.
    Pensado para correr sobre una base vacia: .\run.ps1 seed  (limpia, levanta y carga).

    Los contactos usan dominios example.com: notificaciones-service intenta mandarlos,
    el proveedor los rechaza y quedan guardados como no completados (se ven igual en la web).

    Usuarios (contrasenia demo1234 salvo el admin):
      admin / admin1234       Administrador
      ana, bruno              Donantes persona humana
      estrella, manos         Donantes persona juridica
      comedor, hogar, escuela, merendero   Entidades beneficiarias
#>
[CmdletBinding()]
param(
    [string]$DonacionesUrl = 'http://localhost:8080/api',
    [string]$LogisticaUrl = 'http://localhost:8081/api',
    [string]$AdminUsername = $(if ($env:ADMIN_USERNAME) { $env:ADMIN_USERNAME } else { 'admin' }),
    [string]$AdminPassword = $(if ($env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD } else { 'admin1234' }),
    # Carpeta donde el web-service publica las fotos (web.fotos.directorio, por defecto ./fotos)
    [string]$FotosDir = (Join-Path (Resolve-Path (Join-Path $PSScriptRoot '..\..')) 'fotos')
)

$ErrorActionPreference = 'Stop'
$Password = 'demo1234'

function Write-Step([string]$m) { Write-Host "==> $m" -ForegroundColor Cyan }
function Write-Ok([string]$m) { Write-Host "    $m" -ForegroundColor Green }

# ---------------------------------------------------------------- HTTP

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        $Body = $null,
        [string]$Token = $null,
        [switch]$RawBody
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = $Url; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $Body) {
        $json = if ($RawBody) { [string]$Body } else { $Body | ConvertTo-Json -Depth 10 }
        $params['Body'] = [Text.Encoding]::UTF8.GetBytes($json)
        $params['ContentType'] = 'application/json; charset=utf-8'
    }
    try {
        $resp = Invoke-WebRequest @params
    } catch {
        $detalle = ''
        if ($_.Exception.Response) {
            $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
            $detalle = $reader.ReadToEnd()
        }
        throw "$Method $Url fallo: $($_.Exception.Message) $detalle"
    }
    if (-not $resp.Content) { return $null }
    # Invoke-WebRequest de PS 5.1 no decodifica bien UTF-8 si falta el charset
    $texto = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    if (-not $texto) { return $null }
    # ConvertFrom-Json de PS 5.1 devuelve un array JSON como un solo objeto; asi se desarma en elementos
    $obj = $texto | ConvertFrom-Json
    return $obj
}

function Login([string]$u, [string]$p) {
    (Invoke-Api POST "$DonacionesUrl/auth/login" @{ username = $u; password = $p }).accessToken
}

function Email([string]$v) { @{ tipo = 'EMAIL'; valor = $v } }

# ---------------------------------------------------------------- Fotos de recepcion

function New-FotoDemo([string]$Nombre, [string]$Texto, [string]$ColorHex) {
    Add-Type -AssemblyName System.Drawing
    if (-not (Test-Path $FotosDir)) { New-Item -ItemType Directory -Path $FotosDir | Out-Null }
    $bmp = New-Object Drawing.Bitmap 800, 600
    $g = [Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $g.Clear([Drawing.ColorTranslator]::FromHtml($ColorHex))
    $blanco = New-Object Drawing.SolidBrush ([Drawing.Color]::FromArgb(235, 255, 255, 255))
    $g.FillRectangle($blanco, 60, 200, 680, 200)
    $fuente = New-Object Drawing.Font 'Segoe UI', 30, ([Drawing.FontStyle]::Bold)
    $fuenteChica = New-Object Drawing.Font 'Segoe UI', 16
    $tinta = New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml('#1f2937'))
    $centro = New-Object Drawing.StringFormat
    $centro.Alignment = 'Center'
    $g.DrawString($Texto, $fuente, $tinta, (New-Object Drawing.RectangleF 60, 245, 680, 60), $centro)
    $g.DrawString('Foto de demostracion - DonaTrack', $fuenteChica, $tinta, (New-Object Drawing.RectangleF 60, 320, 680, 40), $centro)
    $ruta = Join-Path $FotosDir $Nombre
    $bmp.Save($ruta, [Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
    return "/fotos/$Nombre"
}

# ================================================================= 1. Admin y catalogo

Write-Step 'Sesion de administrador'
$admin = Login $AdminUsername $AdminPassword
Write-Ok "Sesion iniciada como $AdminUsername"

Write-Step 'Catalogo (categorias y subcategorias)'
$catalogo = [ordered]@{
    'Alimentos'          = @{ pideEstado = $false; esPerecedero = $true;  subs = @('Leche', 'Fideos secos', 'Arroz', 'Conservas') }
    'Ropa y calzado'     = @{ pideEstado = $true;  esPerecedero = $false; subs = @('Abrigos', 'Calzado', 'Ropa de ninos') }
    'Higiene personal'   = @{ pideEstado = $false; esPerecedero = $false; subs = @('Jabon y shampoo', 'Panales') }
    'Utiles escolares'   = @{ pideEstado = $false; esPerecedero = $false; subs = @('Cuadernos', 'Lapices y lapiceras') }
    'Muebles y colchones' = @{ pideEstado = $true; esPerecedero = $false; subs = @('Colchones', 'Sillas y mesas') }
}
foreach ($nombre in $catalogo.Keys) {
    $c = $catalogo[$nombre]
    $cat = Invoke-Api POST "$DonacionesUrl/categorias" @{ nombre = $nombre; pideEstado = $c.pideEstado; esPerecedero = $c.esPerecedero } $admin
    foreach ($sub in $c.subs) {
        Invoke-Api POST "$DonacionesUrl/categorias/$($cat.id)/subcategorias" @{ nombre = $sub } $admin | Out-Null
    }
    Write-Ok "$nombre ($($c.subs -join ', '))"
}

# ================================================================= 2. Entidades beneficiarias

Write-Step 'Entidades beneficiarias (con usuario)'
function New-Entidad($usuario, $razonSocial, $direccion, $telefono, $correo) {
    $r = Invoke-Api POST "$DonacionesUrl/auth/registro/entidad-beneficiaria" @{
        username = $usuario; password = $Password
        entidad  = @{ razonSocial = $razonSocial; direccion = $direccion; telefono = $telefono; correoRepresentantes = @((Email $correo)) }
    }
    Write-Ok "$razonSocial  (usuario: $usuario, id $($r.usuario.perfilId))"
    return [pscustomobject]@{ id = $r.usuario.perfilId; token = $r.accessToken; direccion = $direccion; nombre = $razonSocial }
}
$comedor   = New-Entidad 'comedor'   'Comedor Los Pibes'            'Av. Rivadavia 6500, Flores, CABA'      '1145671234' 'comedor.lospibes@example.com'
$hogar     = New-Entidad 'hogar'     'Hogar de Ancianos Santa Ana'  'Av. San Juan 3100, San Cristobal, CABA' '1149012345' 'hogar.santaana@example.com'
$escuela   = New-Entidad 'escuela'   'Escuela Comunitaria N 12'     'Calle Lavalleja 850, Villa Crespo, CABA' '1148556677' 'escuela12@example.com'
$merendero = New-Entidad 'merendero' 'Merendero Rayito de Sol'      'Av. Eva Peron 4200, Parque Avellaneda, CABA' '1146223344' 'rayitodesol@example.com'

Write-Step 'Necesidades'
function New-Necesidad($entidad, $sub, $tipo, $periodo, $descripcion, $cantidad, $unidad) {
    Invoke-Api POST "$DonacionesUrl/entidad-beneficiaria/$($entidad.id)/necesidades" @{
        subcategoria = @{ nombre = $sub }; tipoNecesidad = $tipo; periodo = $periodo
        descripcion = $descripcion; cantidad = $cantidad; unidad = $unidad
    } $entidad.token | Out-Null
    $cada = if ($periodo) { $periodo.ToLower() } else { 'extraordinaria' }
    Write-Ok "$($entidad.nombre): $cantidad $($unidad.ToLower()) de $sub ($cada)"
}
New-Necesidad $comedor   'Leche'              'recurrente'     'SEMANAL' 'Leche para el desayuno de 60 chicos'          40  'LITROS'
New-Necesidad $comedor   'Fideos secos'       'recurrente'     'MENSUAL' 'Fideos para el almuerzo de lunes a viernes'   50  'KILOGRAMOS'
New-Necesidad $comedor   'Panales'            'extraordinaria' $null     'Panales talle G para la sala maternal'        80  'UNIDADES'
New-Necesidad $hogar     'Abrigos'            'extraordinaria' $null     'Camperas y frazadas para el invierno'         30  'UNIDADES'
New-Necesidad $hogar     'Jabon y shampoo'    'recurrente'     'MENSUAL' 'Higiene diaria de 25 residentes'              40  'UNIDADES'
New-Necesidad $hogar     'Calzado'            'extraordinaria' $null     'Pantuflas y zapatillas comodas'               20  'UNIDADES'
New-Necesidad $hogar     'Colchones'          'extraordinaria' $null     'Colchones de una plaza para renovar camas'    6   'UNIDADES'
New-Necesidad $escuela   'Cuadernos'          'recurrente'     'ANUAL'   'Cuadernos para el ciclo lectivo'              200 'UNIDADES'
New-Necesidad $escuela   'Lapices y lapiceras' 'recurrente'    'ANUAL'   'Kits de utiles para primer grado'             150 'UNIDADES'
New-Necesidad $escuela   'Ropa de ninos'      'extraordinaria' $null     'Guardapolvos y ropa talle 6 a 10'             40  'UNIDADES'
New-Necesidad $merendero 'Leche'              'recurrente'     'DIARIO'  'Leche para la merienda'                       10  'LITROS'
New-Necesidad $merendero 'Arroz'              'recurrente'     'MENSUAL' 'Arroz para las viandas'                       30  'KILOGRAMOS'
New-Necesidad $merendero 'Conservas'          'recurrente'     'MENSUAL' 'Conservas de atun y legumbres'                60  'UNIDADES'

# ================================================================= 3. Donantes

Write-Step 'Donantes (con usuario)'
function New-Humana($usuario, $nombre, $apellido, $nacimiento, $dni, $genero, $direccion, $correo) {
    $r = Invoke-Api POST "$DonacionesUrl/auth/registro/persona-humana" @{
        username = $usuario; password = $Password
        donante  = @{ nombre = $nombre; apellido = $apellido; fechaNacimiento = $nacimiento; dni = $dni; genero = $genero
                      direccion = $direccion; contactos = @((Email $correo)); contactoPredeterminado = (Email $correo) }
    }
    Write-Ok "$nombre $apellido  (usuario: $usuario, id $($r.usuario.perfilId))"
    return [pscustomobject]@{ id = $r.usuario.perfilId; token = $r.accessToken }
}
function New-Juridica($usuario, $razonSocial, $rubro, $tipo, $cuit, $repNombre, $repApellido, $correo) {
    $r = Invoke-Api POST "$DonacionesUrl/auth/registro/persona-juridica" @{
        username = $usuario; password = $Password
        donante  = @{ razonSocial = $razonSocial; rubro = $rubro; tipo = $tipo; cuit = $cuit
                      representantes = @(@{ nombre = $repNombre; apellido = $repApellido; correo = $correo })
                      contactos = @((Email $correo)); contactoPredeterminado = (Email $correo) }
    }
    Write-Ok "$razonSocial  (usuario: $usuario, id $($r.usuario.perfilId))"
    return [pscustomobject]@{ id = $r.usuario.perfilId; token = $r.accessToken }
}
$ana      = New-Humana   'ana'      'Ana'   'Perez' '1992-04-15' '35123456' 'F' 'Av. Medrano 951, Almagro, CABA' 'ana.perez@example.com'
$bruno    = New-Humana   'bruno'    'Bruno' 'Diaz'  '1987-09-02' '33987654' 'M' 'Av. Cabildo 2200, Belgrano, CABA' 'bruno.diaz@example.com'
$estrella = New-Juridica 'estrella' 'Supermercados La Estrella S.A.' 'Supermercados' 'EMPRESA' '30-71111111-5' 'Carla' 'Gimenez' 'donaciones@laestrella.example.com'
$manos    = New-Juridica 'manos'    'Fundacion Manos Unidas' 'Asistencia social' 'ONG' '30-72222222-6' 'Diego' 'Ruiz' 'contacto@manosunidas.example.com'

Write-Step 'Donante cargado por el administrador (donacion presencial, sin usuario)'
$carlos = Invoke-Api POST "$DonacionesUrl/donantes/persona-humana" @{
    nombre = 'Carlos'; apellido = 'Gomez'; fechaNacimiento = '1975-01-20'; dni = '25444333'; genero = 'M'
    direccion = 'Av. Corrientes 4000, Almagro, CABA'
    contactos = @((Email 'carlos.gomez@example.com')); contactoPredeterminado = (Email 'carlos.gomez@example.com')
} $admin
Write-Ok "Carlos Gomez (id $($carlos.id))"

# ================================================================= 4. Donaciones

$hoy = Get-Date
function Fecha([int]$dias) { $hoy.AddDays($dias).ToString('yyyy-MM-dd') }
function Bien($sub, $descripcion, $cantidad, $peso, $volumen, $estado = 'NUEVO', $vence = $null) {
    @{ descripcion = $descripcion; cantidad = $cantidad; pesoKgPorUnidad = $peso; volumenM3PorUnidad = $volumen
       estado = $estado; fechaVencimiento = $vence; subcategoria = @{ nombre = $sub } }
}
# devuelve las donaciones en que se segmento la carga, indexadas por subcategoria
function New-Donacion($donanteId, $token, $descripcion, [object[]]$bienes) {
    $r = @(Invoke-Api POST "$DonacionesUrl/donaciones" @{ descripcion = $descripcion; idDonante = $donanteId; bienes = $bienes } $token)
    $porSub = @{}
    foreach ($d in $r) { $porSub[$d.subcategoria] = $d.id }
    Write-Ok "$descripcion -> $($r.Count) donacion(es): $(($r | ForEach-Object { "#$($_.id) $($_.subcategoria)" }) -join ', ')"
    return $porSub
}

Write-Step 'Donaciones que pasan por matchmaking'
$dEstrella1 = New-Donacion $estrella.id $estrella.token 'Excedente de stock de almacen' @(
    (Bien 'Fideos secos' 'Fideos tallarines 500 g' 100 0.5 0.001 'NUEVO' (Fecha 300)),
    (Bien 'Arroz' 'Arroz largo fino 1 kg' 50 1.0 0.0012 'NUEVO' (Fecha 360)))
$dAnaCuadernos = New-Donacion $ana.id $ana.token 'Utiles que sobraron de la oficina' @((Bien 'Cuadernos' 'Cuadernos tapa dura 48 hojas' 40 0.3 0.0008))
$dManos   = New-Donacion $manos.id $manos.token 'Colecta de panales' @((Bien 'Panales' 'Panales talle G x 30' 60 1.2 0.01))
$dAnaJabon = New-Donacion $ana.id $ana.token 'Kit de higiene' @((Bien 'Jabon y shampoo' 'Jabon de tocador y shampoo' 30 0.4 0.0006))
$dBruno   = New-Donacion $bruno.id $bruno.token 'Lapices para la escuela del barrio' @((Bien 'Lapices y lapiceras' 'Caja de 12 lapices de colores' 100 0.15 0.0003))
$dEstrella2 = New-Donacion $estrella.id $estrella.token 'Conservas de la semana' @((Bien 'Conservas' 'Lata de atun 170 g' 80 0.2 0.0003 'NUEVO' (Fecha 500)))
$dAnaCalzado = New-Donacion $ana.id $ana.token 'Zapatillas en buen estado' @((Bien 'Calzado' 'Zapatillas talle 40' 15 0.8 0.006 'USADO'))
$dAnaLeche = New-Donacion $ana.id $ana.token 'Leche para merenderos' @((Bien 'Leche' 'Leche larga vida 1 L' 24 1.03 0.001 'NUEVO' (Fecha 45)))
$dAnaAbrigos = New-Donacion $ana.id $ana.token 'Ropa de invierno' @((Bien 'Abrigos' 'Camperas de abrigo adulto' 10 1.1 0.02 'USADO'))
$dAnaColchon = New-Donacion $ana.id $ana.token 'Colchon de una plaza' @((Bien 'Colchones' 'Colchon de espuma 1 plaza' 1 9.0 0.35 'USADO'))

Write-Step 'Matchmaking'
Invoke-Api POST "$DonacionesUrl/matchmaking/ejecuciones" @{} $admin | Out-Null
$propuestas = @(Invoke-Api GET "$DonacionesUrl/matchmaking/pendientes" -Token $admin)
$propuestaDe = @{}
foreach ($p in $propuestas) { $propuestaDe[[long]$p.donacionId] = $p }
Write-Ok "$($propuestas.Count) propuestas generadas"

function Aceptar($donacionId, $entidad) {
    $p = $propuestaDe[[long]$donacionId]
    if (-not $p) { throw "No hay propuesta para la donacion $donacionId" }
    Invoke-Api PUT "$DonacionesUrl/matchmaking/propuestas/$($p.id)/estado" @{ estado = 'ACEPTADO'; entidadId = $entidad.id } $admin | Out-Null
    Write-Ok "Donacion #$donacionId asignada a $($entidad.nombre)"
}
Aceptar $dEstrella1['Fideos secos'] $comedor
Aceptar $dEstrella1['Arroz'] $merendero
Aceptar $dAnaCuadernos['Cuadernos'] $escuela
Aceptar $dManos['Panales'] $comedor
Aceptar $dAnaJabon['Jabon y shampoo'] $hogar
Aceptar $dBruno['Lapices y lapiceras'] $escuela
Aceptar $dEstrella2['Conservas'] $merendero
Aceptar $dAnaCalzado['Calzado'] $hogar
# rechazada: vuelve a quedar en deposito, sin propuesta, para que el admin vuelva a correr el matchmaking
$pColchon = $propuestaDe[[long]$dAnaColchon['Colchones']]
Invoke-Api PUT "$DonacionesUrl/matchmaking/propuestas/$($pColchon.id)/estado" @{ estado = 'RECHAZADO' } $admin | Out-Null
Write-Ok "Propuesta de la donacion #$($dAnaColchon['Colchones']) (colchon) rechazada"
Write-Ok "Quedan pendientes: leche #$($dAnaLeche['Leche']) y abrigos #$($dAnaAbrigos['Abrigos'])"

# ================================================================= 5. Logistica

Write-Step 'Camiones'
function New-Camion($patente, $vol, $alto, $carga, $nombre, $apellido) {
    $c = Invoke-Api POST "$LogisticaUrl/camiones" @{ patente = $patente; capacidadVolumen = $vol; altura = $alto; capacidadCarga = $carga
                                                     chofer = @{ nombre = $nombre; apellido = $apellido } } $admin
    Write-Ok "$patente ($nombre $apellido)"
    return $c
}
$camion1 = New-Camion 'AE123BC' 30.0 3.8 8000.0 'Esteban' 'Quiroga'
$camion2 = New-Camion 'AF456DE' 18.0 3.2 4500.0 'Marta'   'Ledesma'
$camion3 = New-Camion 'AD789FG' 12.0 2.9 3000.0 'Ruben'   'Acosta'

function Parada($orden, $entidad, [long[]]$donaciones) {
    @{ orden = $orden; destino = $entidad.direccion; entidad = $entidad.id; entregas = $donaciones }
}
function New-Ruta($fecha, $camion, [object[]]$paradas) {
    Invoke-Api POST "$LogisticaUrl/rutas" @{ fecha = $fecha; idCamion = $camion.id; paradas = $paradas } $admin
}
function Set-EstadoRuta($ruta, $estado) {
    Invoke-Api PUT "$LogisticaUrl/rutas/$($ruta.id)/estado" "`"$estado`"" $admin -RawBody | Out-Null
}
function ParadaDe($ruta, $entidad) { @($ruta.paradas | Where-Object { $_.entidad -eq $entidad.id })[0] }

Write-Step 'Ruta de ayer: entregas realizadas'
$ruta1 = New-Ruta (Fecha -1) $camion1 @(
    (Parada 1 $comedor @($dManos['Panales'])),
    (Parada 2 $hogar @($dAnaJabon['Jabon y shampoo'])),
    (Parada 3 $escuela @($dBruno['Lapices y lapiceras'])),
    (Parada 4 $merendero @($dEstrella2['Conservas'])))
Set-EstadoRuta $ruta1 'EN_TRASLADO'
Invoke-Api POST "$LogisticaUrl/camiones/ubicacion/$($camion1.patente)" @{ latitud = -34.6158; longitud = -58.4333; velocidad = 32.0 } $admin | Out-Null
foreach ($e in @($comedor, $hogar, $escuela, $merendero)) {
    Invoke-Api POST "$LogisticaUrl/rutas/$($ruta1.id)/paradas/$((ParadaDe $ruta1 $e).id)/confirmaciones" $null $admin | Out-Null
}
Set-EstadoRuta $ruta1 'FINALIZADA'
Write-Ok "Ruta #$($ruta1.id) ($($camion1.patente)): 4 paradas entregadas y ruta finalizada"

Write-Step 'Ruta con entrega fallida'
$ruta0 = New-Ruta (Fecha -2) $camion3 @((Parada 1 $hogar @($dAnaCalzado['Calzado'])))
Set-EstadoRuta $ruta0 'EN_TRASLADO'
Invoke-Api POST "$LogisticaUrl/rutas/$($ruta0.id)/paradas/$($ruta0.paradas[0].id)/fallos" $null $admin | Out-Null
Set-EstadoRuta $ruta0 'FINALIZADA'
Invoke-Api PUT "$DonacionesUrl/donaciones/$($dAnaCalzado['Calzado'])/estado" @{
    estado = 'ENTREGA_FALLIDA'; justificacion = 'El hogar estaba cerrado al llegar el camion. Se reprograma la entrega.' } $admin | Out-Null
Write-Ok "Donacion #$($dAnaCalzado['Calzado']) (calzado) en ENTREGA_FALLIDA"

Write-Step 'Ruta de hoy: camion en camino (seguimiento en vivo)'
$ruta2 = New-Ruta (Fecha 0) $camion2 @(
    (Parada 1 $comedor @($dEstrella1['Fideos secos'])),
    (Parada 2 $merendero @($dEstrella1['Arroz'])))
Set-EstadoRuta $ruta2 'EN_TRASLADO'
Invoke-Api POST "$LogisticaUrl/camiones/ubicacion/$($camion2.patente)" @{ latitud = -34.6280; longitud = -58.4560; velocidad = 38.5 } $admin | Out-Null
Write-Ok "Ruta #$($ruta2.id) ($($camion2.patente)) EN_TRASLADO hacia Comedor Los Pibes y Merendero Rayito de Sol"

Write-Step 'Ruta de manana: planificada'
$ruta3 = New-Ruta (Fecha 1) $camion3 @((Parada 1 $escuela @($dAnaCuadernos['Cuadernos'])))
Invoke-Api PUT "$DonacionesUrl/donaciones/$($dAnaCuadernos['Cuadernos'])/estado" @{
    estado = 'LISTA_PARA_ENTREGAR'; justificacion = "Incluida en la ruta del $(Fecha 1) ($($camion3.patente))" } $admin | Out-Null
Write-Ok "Ruta #$($ruta3.id) ($($camion3.patente)) PLANIFICADA; cuadernos #$($dAnaCuadernos['Cuadernos']) LISTA_PARA_ENTREGAR"

# ================================================================= 6. Recepcion por las entidades

Write-Step 'Recepciones confirmadas por las entidades (con fotos)'
$fotoJabon1 = New-FotoDemo 'demo-recepcion-jabon-1.png' 'Kits de higiene recibidos' '#0f766e'
$fotoJabon2 = New-FotoDemo 'demo-recepcion-jabon-2.png' 'Residentes con sus kits' '#0e7490'
$fotoLapices = New-FotoDemo 'demo-recepcion-lapices.png' 'Lapices de colores en el aula' '#b45309'
$fotoConservas = New-FotoDemo 'demo-recepcion-conservas.png' 'Conservas en la despensa' '#7c3aed'
Invoke-Api POST "$DonacionesUrl/donaciones/$($dAnaJabon['Jabon y shampoo'])/recepcion" @{ fotosUrl = @($fotoJabon1, $fotoJabon2) } $hogar.token | Out-Null
Invoke-Api POST "$DonacionesUrl/donaciones/$($dBruno['Lapices y lapiceras'])/recepcion" @{ fotosUrl = @($fotoLapices) } $escuela.token | Out-Null
Invoke-Api POST "$DonacionesUrl/donaciones/$($dEstrella2['Conservas'])/recepcion" @{ fotosUrl = @($fotoConservas) } $merendero.token | Out-Null
Write-Ok 'Hogar Santa Ana, Escuela N 12 y Merendero confirmaron su recepcion'
Write-Ok "Panales #$($dManos['Panales']) quedan ENTREGADA sin confirmar (para probar la pantalla con 'comedor')"

# ================================================================= 7. Deposito

Write-Step 'Donaciones que quedan en deposito (para el admin)'
$dCarlos = New-Donacion $carlos.id $admin 'Donacion presencial en el deposito' @(
    (Bien 'Leche' 'Leche entera sachet 1 L' 12 1.03 0.001 'NUEVO' (Fecha 2)),
    (Bien 'Fideos secos' 'Fideos mostachol 500 g' 20 0.5 0.001 'NUEVO' (Fecha -3)))
Invoke-Api PUT "$DonacionesUrl/donaciones/$($dCarlos['Fideos secos'])/estado" @{
    estado = 'VENCIDA'; justificacion = 'Producto vencido detectado en el control del deposito.' } $admin | Out-Null
Write-Ok "Fideos #$($dCarlos['Fideos secos']) marcados VENCIDA; leche #$($dCarlos['Leche']) vence en 2 dias"
$dBruno2 = New-Donacion $bruno.id $bruno.token 'Ropa que les quedo chica a mis hijos' @((Bien 'Ropa de ninos' 'Guardapolvos talle 8' 6 0.3 0.002 'USADO'))
$dManos2 = New-Donacion $manos.id $manos.token 'Sillas para comedores' @((Bien 'Sillas y mesas' 'Sillas plasticas apilables' 20 2.5 0.05 'NUEVO'))

# ================================================================= 8. Importacion de donantes

Write-Step 'Importacion masiva de donantes (CSV)'
$csv = [IO.File]::ReadAllBytes((Join-Path $PSScriptRoot 'donantes-importacion.csv'))
$imp = Invoke-WebRequest -Method POST -Uri "$DonacionesUrl/donantes/importaciones" -Body $csv -ContentType 'text/csv' `
    -Headers @{ Authorization = "Bearer $admin"; 'X-Filename' = 'donantes-importacion.csv' } -UseBasicParsing
$tarea = $imp.Content | ConvertFrom-Json
Start-Sleep -Seconds 2
$tarea = Invoke-Api GET "$DonacionesUrl/donantes/importaciones/$($tarea.id)" -Token $admin
Write-Ok "Importacion $($tarea.estado): $($tarea | ConvertTo-Json -Compress -Depth 1)"

# ================================================================= Resumen

Write-Step 'Listo'
Write-Host @"
    Sitio: http://localhost:7070   (contrasenia de todos: $Password, salvo admin / $AdminPassword)

    admin      Matchmaking con 2 propuestas pendientes (leche, abrigos) y 4 donaciones en deposito sin propuesta
               (colchon rechazado, leche que vence en 2 dias, ropa de ninos, sillas): "Ejecutar" genera nuevas.
               Deposito con una donacion vencida. 3 camiones. Importacion con filas creadas y con errores.
    ana        Donante ORO: donaciones en todos los estados (deposito, asignada, lista, entregada, fallida).
    bruno      Donante nuevo con una donacion entregada y otra en deposito.
    estrella   Empresa: una carga segmentada en fideos + arroz, hoy en camino.
    manos      ONG: panales entregados sin confirmar, sillas en deposito.
    comedor    Panales para confirmar recepcion; fideos en camino (seguimiento).
    hogar      Jabon recibido con fotos; calzado con entrega fallida.
    escuela    Lapices recibidos; cuadernos listos para la ruta de manana.
    merendero  Conservas recibidas; arroz en camino.
"@
