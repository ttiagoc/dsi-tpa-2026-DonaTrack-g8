/*
 * Selector "Persona Humana / Persona Jurídica" del formulario de donante (partials/form-donante).
 * Muestra los campos del tipo elegido y deshabilita los del otro, así no se validan ni se envían.
 * Al editar no hay selector (el tipo no se cambia) y este script no hace nada.
 */
(function () {
  var selector = document.querySelector('[data-tipo-donante]');
  if (!selector) return;

  var radioHumana = document.getElementById('radio-humana');
  var radioJuridica = document.getElementById('radio-juridica');
  var labelHumana = document.getElementById('label-humana');
  var labelJuridica = document.getElementById('label-juridica');
  var camposHumana = document.getElementById('campos-humana');
  var camposJuridica = document.getElementById('campos-juridica');

  function mostrar(seccion, visible) {
    if (!seccion) return;
    seccion.hidden = !visible;
    seccion.querySelectorAll('input, select').forEach(function (campo) {
      campo.disabled = !visible;
    });
  }

  function actualizar() {
    var esJuridica = radioJuridica.checked;
    labelHumana.classList.toggle('selector-toggle__opcion--activa', !esJuridica);
    labelJuridica.classList.toggle('selector-toggle__opcion--activa', esJuridica);
    mostrar(camposHumana, !esJuridica);
    mostrar(camposJuridica, esJuridica);
  }

  radioHumana.addEventListener('change', actualizar);
  radioJuridica.addEventListener('change', actualizar);
  actualizar();
})();
