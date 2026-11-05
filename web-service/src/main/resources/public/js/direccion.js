// Direccion en campos separados (registro de entidad): mientras se escribe, la busca con Georef
// (GET /direcciones/ubicar) y la muestra en un mapa para que la persona verifique que es la correcta.
// El servidor la vuelve a verificar al registrar, asi que sin JavaScript el formulario funciona igual.
(function () {
  var grupo = document.querySelector('[data-direccion]');
  if (!grupo) return;

  var campos = ['calle', 'altura', 'localidad', 'provincia'].map(function (id) { return document.getElementById(id); });
  var estado = grupo.querySelector('[data-direccion-estado]');
  var lienzo = grupo.querySelector('[data-direccion-mapa]');
  var mapa = null;
  var marcador = null;
  var espera = null;
  var ultimaBusqueda = '';
  var pedido = 0;

  function valores() {
    return campos.map(function (c) { return c.value.trim(); });
  }

  function mostrar(clase, texto, normalizada) {
    grupo.classList.remove('direccion--encontrada', 'direccion--error');
    if (clase) grupo.classList.add(clase);
    estado.removeAttribute('id');
    estado.textContent = '';
    if (normalizada) {
      estado.appendChild(document.createTextNode('✓ La encontramos: '));
      var fuerte = document.createElement('strong');
      fuerte.textContent = normalizada;
      estado.appendChild(fuerte);
      estado.appendChild(document.createTextNode('. Si el punto no es el lugar correcto, revisá los datos.'));
    } else {
      estado.textContent = texto;
    }
  }

  function ubicarEnMapa(latitud, longitud) {
    if (!window.L) return;
    if (!mapa) {
      mapa = L.map(lienzo, { scrollWheelZoom: false });
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
      }).addTo(mapa);
    }
    mapa.invalidateSize(); // el mapa estaba oculto hasta encontrar la direccion
    var punto = [latitud, longitud];
    if (marcador) {
      marcador.setLatLng(punto);
    } else {
      var icono = L.divIcon({ className: 'mapa__icono', html: '<span class="mapa__marcador direccion__marcador"></span>', iconSize: null });
      marcador = L.marker(punto, { icon: icono, keyboard: false, interactive: false }).addTo(mapa);
    }
    mapa.setView(punto, 16);
  }

  function buscar() {
    var v = valores();
    var completa = v.every(function (x) { return x !== ''; });
    if (!completa) {
      ultimaBusqueda = '';
      mostrar(null, 'Completá la calle, la altura, la localidad y la provincia para ver la dirección en el mapa.');
      return;
    }
    if (!/^\d{1,5}$/.test(v[1])) {
      ultimaBusqueda = '';
      mostrar('direccion--error', 'La altura tiene que ser solo el número de la calle, por ejemplo 6500.');
      return;
    }
    var consulta = new URLSearchParams({ calle: v[0], altura: v[1], localidad: v[2], provincia: v[3] }).toString();
    if (consulta === ultimaBusqueda) return;
    ultimaBusqueda = consulta;
    var este = ++pedido;
    mostrar(grupo.classList.contains('direccion--encontrada') ? 'direccion--encontrada' : null, 'Buscando la dirección…');

    fetch('/direcciones/ubicar?' + consulta, { headers: { Accept: 'application/json' } })
      .then(function (r) { return r.json().then(function (cuerpo) { return { ok: r.ok, status: r.status, cuerpo: cuerpo }; }); })
      .then(function (r) {
        if (este !== pedido) return; // ya se escribio otra cosa
        if (r.ok) {
          mostrar('direccion--encontrada', null, r.cuerpo.normalizada || v.join(', '));
          ubicarEnMapa(r.cuerpo.latitud, r.cuerpo.longitud);
        } else if (r.status === 400) {
          mostrar('direccion--error', r.cuerpo.mensaje);
        } else {
          ultimaBusqueda = ''; // se puede reintentar
          mostrar(null, r.cuerpo.mensaje || 'No pudimos buscar la dirección ahora. La vamos a verificar cuando te registres.');
        }
      })
      .catch(function () {
        if (este !== pedido) return;
        ultimaBusqueda = '';
        mostrar(null, 'No pudimos buscar la dirección ahora. La vamos a verificar cuando te registres.');
      });
  }

  function programar() {
    clearTimeout(espera);
    espera = setTimeout(buscar, 700);
  }

  campos.forEach(function (campo) {
    campo.addEventListener('input', programar);
    campo.addEventListener('change', buscar);
  });

  // si la pagina volvio con datos (por un error en otro campo), se muestra lo que ya se habia escrito
  if (valores().every(function (x) { return x !== ''; })) buscar();
})();
