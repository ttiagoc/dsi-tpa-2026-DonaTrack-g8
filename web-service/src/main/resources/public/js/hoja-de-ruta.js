/*
 * Mapa de la hoja de ruta del chofer (templates/chofer/hoja-de-ruta.html.hbs), con Leaflet y OpenStreetMap.
 * Todo sale de atributos data-* que Handlebars ya escapó: el depósito y el camión en #mapa-hoja-ruta y cada
 * parada en su <li data-parada> (las que no tienen coordenadas se listan pero no se marcan).
 * Los textos van al popup con textContent, así un nombre con comillas o < > no rompe nada.
 */
(function () {
  var contenedor = document.getElementById('mapa-hoja-ruta');
  if (!contenedor) return;
  if (typeof L === 'undefined') {
    contenedor.classList.add('hoja-ruta-mapa--sin-mapa');
    contenedor.textContent = 'No se pudo cargar el mapa. Revisá tu conexión: las paradas siguen abajo, en orden.';
    return;
  }

  function numero(valor) {
    var n = parseFloat(valor);
    return isNaN(n) ? null : n;
  }

  function popup(titulo, detalle) {
    var div = document.createElement('div');
    var b = document.createElement('b');
    b.textContent = titulo;
    div.appendChild(b);
    if (detalle) {
      div.appendChild(document.createElement('br'));
      div.appendChild(document.createTextNode(detalle));
    }
    return div;
  }

  function icono(texto, modificador) {
    var div = document.createElement('div');
    div.className = 'marcador-parada' + (modificador ? ' marcador-parada--' + modificador : '');
    div.textContent = texto;
    return L.divIcon({
      className: 'marcador-parada-wrap',
      html: div.outerHTML,
      iconSize: [28, 28],
      iconAnchor: [14, 14],
      popupAnchor: [0, -14]
    });
  }

  var datos = contenedor.dataset;
  var deposito = [numero(datos.depositoLat), numero(datos.depositoLng)];
  var mapa = L.map(contenedor, { scrollWheelZoom: false }).setView(deposito, 13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
  }).addTo(mapa);

  L.marker(deposito, { icon: icono('D', 'deposito') })
    .addTo(mapa)
    .bindPopup(popup('Depósito DonaTrack', 'Av. Medrano 951, Almagro, CABA'));

  var recorrido = [deposito];
  document.querySelectorAll('[data-parada]').forEach(function (parada) {
    var lat = numero(parada.dataset.lat);
    var lng = numero(parada.dataset.lng);
    if (lat === null || lng === null) return;
    var punto = [lat, lng];
    recorrido.push(punto);
    L.marker(punto, { icon: icono(parada.dataset.orden, parada.dataset.resultado) })
      .addTo(mapa)
      .bindPopup(popup('Parada #' + parada.dataset.orden + ' · ' + parada.dataset.nombre, parada.dataset.direccion));
  });

  if (recorrido.length > 1) {
    L.polyline(recorrido, { color: '#1f7a5a', weight: 4, opacity: 0.85, dashArray: '9, 6' }).addTo(mapa);
  }

  var limites = recorrido.slice();
  if (datos.mostrarCamion !== undefined) {
    var camion = [numero(datos.camionLat), numero(datos.camionLng)];
    if (camion[0] !== null && camion[1] !== null) {
      limites.push(camion);
      L.marker(camion, {
        icon: L.divIcon({
          className: 'marcador-parada-wrap',
          html: '<div class="marcador-camion">🚚</div>',
          iconSize: [34, 34],
          iconAnchor: [17, 17],
          popupAnchor: [0, -16]
        }),
        zIndexOffset: 1000
      })
        .addTo(mapa)
        .bindPopup(popup('Tu camión' + (datos.camionPatente ? ' (' + datos.camionPatente + ')' : '')));
    }
  }

  if (limites.length > 1) {
    mapa.fitBounds(limites, { padding: [45, 45], maxZoom: 15 });
  }
  // por si el contenedor cambió de tamaño mientras cargaban las fuentes o el menú lateral
  setTimeout(function () { mapa.invalidateSize(); }, 200);
})();
