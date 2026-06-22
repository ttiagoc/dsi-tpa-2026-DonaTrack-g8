package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.common.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import lombok.Data;

@Data
public class Donacion {
  private Long id;
  private Donante donante;
  private EntidadBeneficiaria entidadBeneficiariaAsignada;
  private Subcategoria subcategoria;
  private EstadoBien estadoBienes;
  private LocalDate fechaVencimiento;
  private List<Bien> bienes;
  private LocalDateTime fecha;
  private List<CambioEstado> historialEstados;

  public Donacion(Bien bienBase, LocalDateTime fecha) {
    this.subcategoria = bienBase.getSubcategoria();
    this.estadoBienes = bienBase.getEstadoBien();
    this.fechaVencimiento = bienBase.getFechaVencimiento();
    this.bienes = new ArrayList<>();
    this.historialEstados = new ArrayList<>();
    this.fecha = fecha;

    this.agregarBien(bienBase);
    this.registrarEstadoInicial(fecha);
  }

  public void agregarBien(Bien bien) {
    this.bienes.add(bien);
  }

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
    if (this.bienes == null)
      return 0.0;

    return this.bienes.stream()
        .mapToDouble(bien -> bien.getCantidad() != null ? bien.getCantidad() : 0)
        .sum();
  }

  public Boolean estaDentroDelPeriodoActual(Periodo periodo) {
    return periodo != null && periodo.incluye(this.fecha);
  }

  public void cambiarEstado(TipoEstadoDonacion nuevoEstado, String justificacion) {
    this.registrarEstado(LocalDateTime.now(), nuevoEstado, justificacion);
  }
}
