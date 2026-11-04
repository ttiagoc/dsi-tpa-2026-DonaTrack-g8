/*
 * Manejo global de modales (<dialog class="modal">).
 * - Si un modal viene abierto desde el servidor (SSR, por ejemplo con errores o ?nuevo),
 *   lo eleva a Top Layer con showModal() para que tenga backdrop y quede centrado.
 * - Los botones con data-abre="id-del-modal" abren el modal sin recargar la página.
 * - Los botones con data-cierra cierran el modal.
 * - Soporta cerrar con click en el backdrop o con la tecla Escape (comportamiento nativo).
 */
(function () {
  'use strict';

  function elevarModalesAbiertos() {
    document.querySelectorAll('dialog.modal[open]').forEach(function (dialog) {
      if (typeof dialog.showModal === 'function') {
        dialog.close();
        dialog.showModal();
      }
    });
  }

  // Ejecutar cuando el DOM esté listo
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', elevarModalesAbiertos);
  } else {
    elevarModalesAbiertos();
  }

  // Delegación de eventos para data-abre y data-cierra
  document.addEventListener('click', function (e) {
    var botonAbre = e.target.closest('[data-abre]');
    if (botonAbre) {
      var id = botonAbre.dataset.abre;
      var modal = document.getElementById(id);
      if (modal && typeof modal.showModal === 'function') {
        if (window.location.search) {
          return;
        }
        e.preventDefault();
        modal.showModal();
        return;
      }
    }

    var botonCierra = e.target.closest('[data-cierra]');
    if (botonCierra) {
      var dialog = botonCierra.closest('dialog');
      if (dialog && typeof dialog.close === 'function') {
        e.preventDefault();
        dialog.close();
        if (window.location.search) {
          var url = new URL(window.location.href);
          url.search = '';
          window.history.replaceState({}, '', url.pathname);
        }
      }
    }
  });

  // Cerrar al hacer click en el backdrop
  document.addEventListener('click', function (e) {
    if (e.target.tagName === 'DIALOG' && e.target.classList.contains('modal')) {
      var rect = e.target.getBoundingClientRect();
      var clickeadoAdentro = (
        rect.top <= e.clientY && e.clientY <= rect.top + rect.height &&
        rect.left <= e.clientX && e.clientX <= rect.left + rect.width
      );
      if (!clickeadoAdentro) {
        e.target.close();
        if (window.location.search) {
          var url = new URL(window.location.href);
          url.search = '';
          window.history.replaceState({}, '', url.pathname);
        }
      }
    }
  });
})();
