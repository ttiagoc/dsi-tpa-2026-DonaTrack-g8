/*
 * Selector con buscador: convierte cualquier <select data-buscador="Texto de ayuda"> en una caja donde se escribe
 * para filtrar las opciones (sin importar mayusculas ni tildes). Respeta los <optgroup> como titulos.
 * El <select> original queda oculto y es el que se envia, asi que el servidor no cambia.
 * Sin JavaScript se ve el <select> comun. La lista se abre con un clic, al escribir o con la flecha hacia abajo;
 * flechas para moverse, Enter para elegir, Escape para cerrar.
 */
(function () {
  'use strict';

  function normalizar(texto) {
    return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  }

  function crear(select) {
    var id = select.id;
    var envoltorio = document.createElement('div');
    envoltorio.className = 'buscador';
    var entrada = document.createElement('input');
    entrada.type = 'text';
    entrada.id = id;
    entrada.className = 'buscador__entrada';
    entrada.autocomplete = 'off';
    entrada.placeholder = select.dataset.buscador || 'Escribí para buscar';
    entrada.setAttribute('role', 'combobox');
    entrada.setAttribute('aria-autocomplete', 'list');
    entrada.setAttribute('aria-expanded', 'false');
    var lista = document.createElement('ul');
    lista.className = 'buscador__lista';
    lista.id = id + '-opciones';
    lista.setAttribute('role', 'listbox');
    lista.hidden = true;
    entrada.setAttribute('aria-controls', lista.id);
    if (select.getAttribute('aria-invalid')) {
      entrada.setAttribute('aria-invalid', 'true');
    }

    // el label apunta al id: pasa a la caja de texto
    select.id = id + '-valor';
    select.hidden = true;
    select.tabIndex = -1;
    select.parentNode.insertBefore(envoltorio, select);
    envoltorio.appendChild(entrada);
    envoltorio.appendChild(lista);

    var opciones = Array.prototype.filter.call(select.options, function (o) { return o.value; })
      .map(function (o) {
        var grupo = o.parentNode.tagName === 'OPTGROUP' ? o.parentNode.label : null;
        return { opcion: o, texto: o.text, grupo: grupo, buscable: normalizar(o.text + ' ' + (grupo || '')) };
      });
    var visibles = [];
    var activa = -1;

    function elegido() {
      return select.value ? select.options[select.selectedIndex].text : '';
    }

    function pintar(filtro) {
      var buscado = normalizar(filtro || '');
      var grupoActual;
      lista.innerHTML = '';
      visibles = [];
      activa = -1;
      entrada.removeAttribute('aria-activedescendant');
      opciones.forEach(function (op) {
        if (buscado && op.buscable.indexOf(buscado) < 0) {
          return;
        }
        if (op.grupo && op.grupo !== grupoActual) {
          var titulo = document.createElement('li');
          titulo.className = 'buscador__grupo';
          titulo.setAttribute('role', 'presentation');
          titulo.textContent = op.grupo;
          lista.appendChild(titulo);
        }
        grupoActual = op.grupo;
        var item = document.createElement('li');
        item.className = 'buscador__opcion';
        item.id = lista.id + '-' + visibles.length;
        item.setAttribute('role', 'option');
        item.setAttribute('aria-selected', String(op.opcion.selected && !!select.value));
        item.textContent = op.texto;
        item.addEventListener('mousedown', function (e) {
          e.preventDefault();
          elegir(op);
        });
        lista.appendChild(item);
        visibles.push({ op: op, item: item });
      });
      if (!visibles.length) {
        var vacio = document.createElement('li');
        vacio.className = 'buscador__vacio';
        vacio.textContent = 'No hay coincidencias';
        lista.appendChild(vacio);
      }
    }

    function abrir() {
      pintar(entrada.value === elegido() ? '' : entrada.value);
      lista.hidden = false;
      entrada.setAttribute('aria-expanded', 'true');
    }

    function cerrar() {
      lista.hidden = true;
      entrada.setAttribute('aria-expanded', 'false');
      entrada.removeAttribute('aria-activedescendant');
    }

    function elegir(op) {
      op.opcion.selected = true;
      entrada.value = op.texto;
      cerrar();
      select.dispatchEvent(new Event('change', { bubbles: true }));
    }

    function mover(paso) {
      if (!visibles.length) {
        return;
      }
      activa = (activa + paso + visibles.length) % visibles.length;
      visibles.forEach(function (v, i) { v.item.classList.toggle('buscador__opcion--activa', i === activa); });
      visibles[activa].item.scrollIntoView({ block: 'nearest' });
      entrada.setAttribute('aria-activedescendant', visibles[activa].item.id);
    }

    entrada.value = elegido();
    // no se abre solo con el foco: al volver un formulario con errores, el modal enfoca este campo y taparia los mensajes
    entrada.addEventListener('click', function () { if (lista.hidden) abrir(); });
    entrada.addEventListener('input', function () {
      pintar(entrada.value);
      lista.hidden = false;
      entrada.setAttribute('aria-expanded', 'true');
    });
    entrada.addEventListener('keydown', function (e) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        if (lista.hidden) abrir();
        mover(e.key === 'ArrowDown' ? 1 : -1);
      } else if (e.key === 'Enter' && !lista.hidden) {
        // con una sola coincidencia, Enter la elige sin tener que bajar con la flecha
        var op = activa >= 0 ? visibles[activa] : (visibles.length === 1 ? visibles[0] : null);
        if (op) {
          e.preventDefault();
          elegir(op.op);
        }
      } else if (e.key === 'Escape' && !lista.hidden) {
        e.preventDefault();
        e.stopPropagation();
        entrada.value = elegido();
        cerrar();
      }
    });
    // al salir: si borro el texto, no queda nada elegido; si escribio algo sin elegir, vuelve a lo elegido
    entrada.addEventListener('blur', function () {
      cerrar();
      if (!entrada.value.trim() && select.value) {
        select.selectedIndex = 0;
        select.dispatchEvent(new Event('change', { bubbles: true }));
      }
      entrada.value = elegido();
    });
  }

  document.querySelectorAll('select[data-buscador]').forEach(crear);
})();
