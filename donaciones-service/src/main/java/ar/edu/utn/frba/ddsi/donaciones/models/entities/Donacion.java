package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Donacion {
  private Subcategoria subcategoria;
  private Boolean esUsado;
  private LocalDate fechaVencimiento;
  private List<Bien> bienes;
  private List<CambioEstado> historialEstados;

  // Un constructor personalizado para inicializarse a partir de un Bien base y una fecha
  public Donacion(Bien bienBase, LocalDate fecha) {
    this.subcategoria = bienBase.getSubcategoria();
    this.esUsado = bienBase.getEsUsado();
    this.fechaVencimiento = bienBase.getFechaVencimiento();
    this.bienes = new ArrayList<>();
    this.historialEstados = new ArrayList<>();

    this.agregarBien(bienBase);
    this.registrarEstadoInicial(fecha);
  }

  // Encapsula la lista: no devolvemos la lista para que la modifiquen desde afuera
  public void agregarBien(Bien bien) {
    this.bienes.add(bien);
  }

  // Encapsula la lógica de su propio estado inicial
  private void registrarEstadoInicial(LocalDate fecha) {
    this.registrarEstado(fecha, TipoEstadoDonacion.EN_DEPOSITO, "Ingreso al depósito por segmentación automática");
  }

  private void registrarEstado(LocalDate fecha, TipoEstadoDonacion estado, String justificacion) {
    CambioEstado cambioEstado = new CambioEstado(fecha, estado, justificacion);

    this.historialEstados.add(cambioEstado);
  }

  public TipoEstadoDonacion estadoActual() {
    return this.historialEstados.getLast().getEstado();
  }

  public void confirmarEntrega() {
    this.registrarEstado(LocalDate.now(), TipoEstadoDonacion.ENTREGADA, "Entregado");
  }
}
