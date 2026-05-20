package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Donacion {
  private Subcategoria subcategoria;
  private Boolean esUsado;
  private LocalDate fechaVencimiento;
  private List<Bien> bienes;
  private LocalDateTime fecha;
  private List<CambioEstado> historialEstados;

  // Un constructor personalizado para inicializarse a partir de un Bien base y una fecha
  public Donacion(Bien bienBase, LocalDateTime fecha) {
    this.subcategoria = bienBase.getSubcategoria();
    this.esUsado = bienBase.getEsUsado();
    this.fechaVencimiento = bienBase.getFechaVencimiento();
    this.bienes = new ArrayList<>();
    this.historialEstados = new ArrayList<>();
    this.fecha = fecha;

    this.agregarBien(bienBase);
    this.registrarEstadoInicial(fecha);
  }

  // Encapsula la lista: no devolvemos la lista para que la modifiquen desde afuera
  public void agregarBien(Bien bien) {
    this.bienes.add(bien);
  }

  // Encapsula la lógica de su propio estado inicial
  private void registrarEstadoInicial(LocalDateTime fecha) {
    this.registrarEstado(fecha, TipoEstadoDonacion.EN_DEPOSITO, "Ingreso al depósito por segmentación automática");
  }

  private void registrarEstado(LocalDateTime fecha, TipoEstadoDonacion estado, String justificacion) {
    CambioEstado cambioEstado = new CambioEstado(fecha, estado, justificacion);

    this.historialEstados.add(cambioEstado);
  }

  public TipoEstadoDonacion estadoActual() {
    return this.historialEstados.getLast().getEstado();
  }

  public void confirmarEntrega() {
    this.registrarEstado(LocalDateTime.now(), TipoEstadoDonacion.ENTREGADA, "Entregado");
  }

  public Double cantidadBienesRecibidos() {
    if (this.bienes == null) return 0.0;

    return this.bienes.stream()
        .mapToDouble(bien -> bien.getCantidad() != null ? bien.getCantidad() : 0)
        .sum();
  }

  public Boolean estaDentroDelPeriodoActual(Periodo periodo) {
    if (periodo == null) return false;

    LocalDateTime fechaActual = LocalDateTime.now();

    return switch (periodo) {
      case DIARIO -> this.fecha.toLocalDate().isEqual(fechaActual.toLocalDate());

      case SEMANAL -> {
        // Compara que estén en la misma semana del mismo año
        int semanaDonacion = this.fecha.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int semanaAhora = fechaActual.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int anioDonacion = this.fecha.get(IsoFields.WEEK_BASED_YEAR);
        int anioAhora = fechaActual.get(IsoFields.WEEK_BASED_YEAR);
        yield (semanaDonacion == semanaAhora) && (anioDonacion == anioAhora);
      }

      case MENSUAL -> (this.fecha.getMonth() == fechaActual.getMonth()) &&
          (this.fecha.getYear() == fechaActual.getYear());

      case ANUAL -> this.fecha.getYear() == fechaActual.getYear();
    };
  }
}
