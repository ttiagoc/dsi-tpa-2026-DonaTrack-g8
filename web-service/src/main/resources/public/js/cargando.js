// Indicador de carga en formularios (evita doble click y da feedback en operaciones largas)
(function () {
  document.addEventListener('submit', function (e) {
    var form = e.target;
    if (!form || e.defaultPrevented) return;

    var btn = form.querySelector('button[type="submit"], input[type="submit"]');
    if (!btn) return;

    var textoCargando = btn.dataset.cargando || form.dataset.cargando;
    if (!textoCargando) {
      // Si no tiene texto explícito pero es un submit seguro, damos feedback básico
      if (form.method && form.method.toLowerCase() === 'post') {
        textoCargando = 'Procesando…';
      } else {
        return;
      }
    }

    // Esperar a la siguiente microtarea para que formularios.js valide primero si corresponde
    setTimeout(function () {
      if (e.defaultPrevented) return;

      btn.dataset.textoOriginal = btn.innerHTML;
      btn.classList.add('boton--cargando');
      btn.disabled = true;
      form.setAttribute('aria-busy', 'true');

      btn.innerHTML = '<svg class="icono" aria-hidden="true"><use href="#i-reloj"/></svg> ' + textoCargando;
    }, 0);
  });

  // Restaurar el botón al volver con el botón "Atrás" del navegador
  window.addEventListener('pageshow', function () {
    document.querySelectorAll('.boton--cargando').forEach(function (btn) {
      if (btn.dataset.textoOriginal) {
        btn.innerHTML = btn.dataset.textoOriginal;
        delete btn.dataset.textoOriginal;
      }
      btn.classList.remove('boton--cargando');
      btn.disabled = false;
    });
    document.querySelectorAll('[aria-busy="true"]').forEach(function (form) {
      form.removeAttribute('aria-busy');
    });
  });
})();
