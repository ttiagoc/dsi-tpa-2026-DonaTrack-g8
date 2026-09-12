package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "donacion")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Donacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "registro_donacion_id", nullable = false)
  private RegistroDonacion registroDonacion;

  // donante y fecha se copian del registro (desnormalizados, ver DER)
  @ManyToOne
  @JoinColumn(name = "donante_id")
  private Donante donante;

  @ManyToOne
  @JoinColumn(name = "entidad_beneficiaria_asignada_id")
  private EntidadBeneficiaria entidadBeneficiariaAsignada;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id")
  private Subcategoria subcategoria;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_bienes", length = 20)
  private EstadoBien estadoBienes;

  @Column(name = "fecha_vencimiento")
  private LocalDate fechaVencimiento;

  @ElementCollection
  @CollectionTable(name = "donacion_bienes", joinColumns = @JoinColumn(name = "donacion_id"))
  private List<Bien> bienes;

  private LocalDateTime fecha;

  // el estado actual es el ultimo cambio: se ordena por fecha al leer
  @ElementCollection
  @CollectionTable(name = "donacion_historial_estados", joinColumns = @JoinColumn(name = "donacion_id"))
  @OrderBy("fecha ASC")
  private List<CambioEstado> historialEstados;

  @ElementCollection
  @CollectionTable(name = "donacion_fotos_recepcion", joinColumns = @JoinColumn(name = "donacion_id"))
  @Column(name = "foto_url", length = 500)
  private List<String> fotosRecepcion;

  public Donacion(Bien bienBase, LocalDateTime fecha) {
    this.subcategoria = bienBase.getSubcategoria();
    this.estadoBienes = bienBase.getEstadoBien();
    this.fechaVencimiento = bienBase.getFechaVencimiento();
    this.bienes = new ArrayList<>();
    this.historialEstados = new ArrayList<>();
    this.fecha = fecha;
    this.fotosRecepcion = new ArrayList<>();

    this.agregarBien(bienBase);
    this.registrarEstadoInicial(fecha);
  }

  public Donacion(RegistroDonacion registroDonacion, Bien bienBase) {
    this(bienBase, registroDonacion.getFecha());
    this.registroDonacion = registroDonacion;
    this.donante = registroDonacion.getDonante();
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

  public Double calcularPesoTotal() {
    return this.bienes.stream()
        .mapToDouble(bien -> bien.calcularPesoTotal())
        .sum();
  }

  public Double calcularVolumenTotal() {
    return this.bienes.stream()
        .mapToDouble(bien -> bien.calcularVolumenTotal())
        .sum();
  }

  public String obtenerDireccion() {
    if (this.estadoActual() == TipoEstadoDonacion.EN_DEPOSITO) {
      return null;
    }
    return this.entidadBeneficiariaAsignada.getDireccion();
  }
}
