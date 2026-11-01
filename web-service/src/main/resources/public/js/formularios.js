/*
 * Validacion en el navegador de los formularios con "novalidate" (todos los del sitio).
 * Muestra los errores igual que el servidor (.campo--error + .campo__error) y no envia si hay alguno.
 * El servidor vuelve a validar todo (Formulario.java): esto solo evita el ida y vuelta.
 *
 * Reglas, segun los atributos del campo:
 * - required                 -> obligatorio
 * - type="email"             -> nombre@dominio.algo
 * - type="tel"               -> de 8 a 15 numeros; se permiten +, espacios, guiones y parentesis
 * - type="number" + min      -> numero entero mayor o igual a min
 * - minlength                -> largo minimo
 * - data-igual-a="otroCampo" -> tiene que coincidir con otroCampo (repetir contrasenia)
 */
(function () {
  'use strict';

  var EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
  var TELEFONO = /^\+?[0-9\s()\-]+$/;
  var MENSAJES = {
    obligatorio: 'Este campo es obligatorio',
    email: 'Ingresá un email válido, por ejemplo nombre@dominio.com',
    telefono: 'Ingresá un teléfono válido: de 8 a 15 números (podés usar +, espacios y guiones)',
    entero: 'Ingresá un número entero',
    distintos: 'Las contraseñas no coinciden'
  };

  function errorDe(campo) {
    var valor = (campo.value || '').trim();
    if (!valor) {
      return campo.required ? MENSAJES.obligatorio : null;
    }
    if (campo.type === 'email' && !EMAIL.test(valor)) {
      return MENSAJES.email;
    }
    if (campo.type === 'tel') {
      var digitos = valor.replace(/\D/g, '').length;
      if (!TELEFONO.test(valor) || digitos < 8 || digitos > 15) {
        return MENSAJES.telefono;
      }
    }
    if (campo.type === 'number') {
      if (!/^-?\d+$/.test(valor)) {
        return MENSAJES.entero;
      }
      if (campo.min !== '' && Number(valor) < Number(campo.min)) {
        return campo.min === '1' ? 'Tiene que ser un número mayor a 0' : 'Tiene que ser al menos ' + campo.min;
      }
    }
    if (campo.minLength > 0 && valor.length < campo.minLength) {
      return 'Tiene que tener al menos ' + campo.minLength + ' caracteres';
    }
    if (campo.dataset.igualA) {
      var otro = campo.form.elements[campo.dataset.igualA];
      if (otro && otro.value !== campo.value) {
        return MENSAJES.distintos;
      }
    }
    return null;
  }

  function mostrar(campo, mensaje) {
    var contenedor = campo.closest('.campo');
    if (!contenedor) {
      return;
    }
    contenedor.querySelectorAll('.campo__error').forEach(function (e) { e.remove(); });
    contenedor.classList.toggle('campo--error', !!mensaje);
    var visible = visibleDe(campo);
    if (!mensaje) {
      visible.removeAttribute('aria-invalid');
      visible.removeAttribute('aria-describedby');
      return;
    }
    var error = document.createElement('small');
    error.className = 'campo__error';
    error.id = (campo.name || campo.id) + '-error';
    error.textContent = mensaje;
    contenedor.appendChild(error);
    visible.setAttribute('aria-invalid', 'true');
    visible.setAttribute('aria-describedby', error.id);
  }

  // un select con buscador (selector-buscador.js) queda oculto: el foco y el aria van en su caja de texto
  function visibleDe(campo) {
    if (campo.hidden && campo.closest('.campo')) {
      return campo.closest('.campo').querySelector('input:not([type="hidden"])') || campo;
    }
    return campo;
  }

  function campos(form) {
    return Array.prototype.filter.call(form.elements, function (e) {
      return /^(INPUT|SELECT|TEXTAREA)$/.test(e.tagName) && !e.disabled
        && !/^(hidden|submit|button|reset|checkbox|radio)$/.test(e.type);
    });
  }

  function validar(campo) {
    var mensaje = errorDe(campo);
    mostrar(campo, mensaje);
    return !mensaje;
  }

  document.querySelectorAll('form[novalidate]').forEach(function (form) {
    form.addEventListener('submit', function (evento) {
      var invalidos = campos(form).filter(function (campo) { return !validar(campo); });
      if (invalidos.length) {
        evento.preventDefault();
        evento.stopImmediatePropagation();
        visibleDe(invalidos[0]).focus();
      }
    });
    // al salir de un campo se valida; si ya tenia error, se revalida mientras se escribe
    form.addEventListener('focusout', function (evento) {
      if (campos(form).indexOf(evento.target) >= 0 && evento.target.value) {
        validar(evento.target);
      }
    });
    form.addEventListener('input', function (evento) {
      var contenedor = evento.target.closest && evento.target.closest('.campo--error');
      if (contenedor && campos(form).indexOf(evento.target) >= 0) {
        validar(evento.target);
      }
    });
    form.addEventListener('change', function (evento) {
      if (evento.target.tagName === 'SELECT') {
        validar(evento.target);
      }
    });
  });
})();
