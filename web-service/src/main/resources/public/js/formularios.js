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
 * - type="file"              -> required, accept (tipos), data-max-archivos y data-max-mb (por archivo)
 * - data-mensaje-requerido   -> el mensaje cuando falta un campo obligatorio, en lugar del generico
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
    if (campo.type === 'file') {
      return errorDeArchivos(campo);
    }
    var valor = (campo.value || '').trim();
    if (!valor) {
      return campo.required ? (campo.dataset.mensajeRequerido || MENSAJES.obligatorio) : null;
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
      var numVal = valor.replace(',', '.');
      var permiteDecimales = campo.step === 'any' || (campo.step && campo.step.indexOf('.') !== -1);
      var regex = permiteDecimales ? /^-?\d+(\.\d+)?$/ : /^-?\d+$/;
      if (!regex.test(numVal)) {
        return permiteDecimales ? 'Ingresá un número válido' : MENSAJES.entero;
      }
      if (campo.min !== '' && Number(numVal) < Number(campo.min)) {
        return campo.min === '1' ? 'Tiene que ser un número mayor a 0' : 'Tiene que ser al menos ' + campo.min;
      }
      if (campo.max !== '' && Number(numVal) > Number(campo.max)) {
        return 'Tiene que ser como máximo ' + campo.max;
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

  function errorDeArchivos(campo) {
    var archivos = Array.prototype.slice.call(campo.files || []);
    if (!archivos.length) {
      return campo.required ? (campo.dataset.mensajeRequerido || MENSAJES.obligatorio) : null;
    }
    var maximo = Number(campo.dataset.maxArchivos);
    if (maximo && archivos.length > maximo) {
      return 'Podés cargar hasta ' + maximo + ' archivos';
    }
    var tipos = campo.accept ? campo.accept.split(',') : [];
    var maxMb = Number(campo.dataset.maxMb);
    for (var i = 0; i < archivos.length; i++) {
      if (tipos.length && tipos.indexOf(archivos[i].type) < 0) {
        return '"' + archivos[i].name + '" no tiene un formato aceptado';
      }
      if (maxMb && archivos[i].size > maxMb * 1024 * 1024) {
        return '"' + archivos[i].name + '" pesa más de ' + maxMb + ' MB';
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
      if (evento.target.tagName === 'SELECT' || evento.target.type === 'file') {
        validar(evento.target);
      }
    });
  });
})();
