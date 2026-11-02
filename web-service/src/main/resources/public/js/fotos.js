/*
 * Campo de fotos (<div data-fotos> con un <input type="file" multiple>): muestra cada foto elegida con su
 * miniatura y un boton para quitarla. Se pueden elegir en varias tandas o arrastrarlas sobre el recuadro.
 * La validacion (cantidad, tamanio y formato) la hace formularios.js; el servidor la repite.
 * Sin JavaScript queda el input comun del navegador.
 */
(function () {
  'use strict';

  if (typeof DataTransfer === 'undefined') {
    return;
  }

  document.querySelectorAll('[data-fotos]').forEach(function (campo) {
    var input = campo.querySelector('input[type="file"]');
    var zona = campo.querySelector('.fotos');
    var lista = campo.querySelector('.fotos__lista');
    var elegidas = [];
    var sincronizando = false;

    campo.classList.add('campo--fotos-js');

    // vuelca las elegidas al input (lo que se envia) y avisa el cambio para que se valide
    function sincronizar() {
      var archivos = new DataTransfer();
      elegidas.forEach(function (foto) { archivos.items.add(foto); });
      input.files = archivos.files;
      sincronizando = true;
      input.dispatchEvent(new Event('change', { bubbles: true }));
      sincronizando = false;
      dibujar();
    }

    function dibujar() {
      lista.innerHTML = '';
      elegidas.forEach(function (foto, i) {
        var item = document.createElement('li');
        var imagen = document.createElement('img');
        imagen.alt = '';
        imagen.src = URL.createObjectURL(foto);
        var quitar = document.createElement('button');
        quitar.type = 'button';
        quitar.textContent = '×';
        quitar.setAttribute('aria-label', 'Quitar ' + foto.name);
        quitar.addEventListener('click', function () {
          elegidas.splice(i, 1);
          sincronizar();
        });
        var nombre = document.createElement('small');
        nombre.textContent = foto.name;
        nombre.title = foto.name;
        item.append(imagen, quitar, nombre);
        lista.appendChild(item);
      });
    }

    function agregar(archivos) {
      Array.prototype.forEach.call(archivos, function (foto) { elegidas.push(foto); });
      sincronizar();
    }

    // el navegador reemplaza los archivos en cada eleccion: se suman a los que ya estaban
    input.addEventListener('change', function () {
      if (!sincronizando) {
        agregar(input.files);
      }
    });

    zona.addEventListener('dragover', function (evento) {
      evento.preventDefault();
      zona.classList.add('fotos--encima');
    });
    zona.addEventListener('dragleave', function () {
      zona.classList.remove('fotos--encima');
    });
    zona.addEventListener('drop', function (evento) {
      evento.preventDefault();
      zona.classList.remove('fotos--encima');
      agregar(evento.dataTransfer.files);
    });
  });
})();
