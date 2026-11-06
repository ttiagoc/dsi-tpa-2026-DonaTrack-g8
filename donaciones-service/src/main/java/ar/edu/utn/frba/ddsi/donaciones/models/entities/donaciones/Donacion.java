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
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "donacion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Donacion {
  public static final int MAXIMO_FOTOS_RECEPCION = 5;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "registro_donacion_id", nullable = false)
  private RegistroDonacion registroDonacion;

  @ManyToOne
  @JoinColumn(name = "donante_id")
  private Donante donante;

  @ManyToOne
  @JoinColumn(name = "entidad_beneficiaria_asignada_id")
  private EntidadBeneficiaria entidadBeneficiariaAsignada;

  @ManyToOne
  @JoinColumn(name = "necesidad_id")
  private Necesidad necesidad;

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

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_actual", length = 20, nullable = false)
  private TipoEstadoDonacion estadoActual;

  // lista indexada: la PK de cambio_estado es (donacion_id, orden) y agregar un estado es un solo INSERT
  @ElementCollection
  @CollectionTable(name = "cambio_estado", joinColumns = @JoinColumn(name = "donacion_id"))
  @OrderColumn(name = "orden")
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
    registroDonacion.agregarDonacion(this);
  }

  public void agregarBien(Bien bien) {
    this.bienes.add(bien);
  }

  private void registrarEstadoInicial(LocalDateTime fecha) {
    this.registrarEstado(fecha, TipoEstadoDonacion.EN_DEPOSITO, "Ingreso al depósito por segmentación automática");
  }

  private void registrarEstado(LocalDateTime fecha, TipoEstadoDonacion estado, String justificacion) {
    this.registrarEstado(fecha, estado, justificacion, null);
  }

  private void registrarEstado(LocalDateTime fecha, TipoEstadoDonacion estado, String justificacion,
      String patenteCamion) {
    CambioEstado cambioEstado = new CambioEstado(fecha, estado, justificacion, patenteCamion);

    this.historialEstados.add(cambioEstado);
    this.estadoActual = estado;
  }

  public TipoEstadoDonacion estadoActual() {
    return this.estadoActual;
  }

  public void confirmarEntrega(String patenteCamion, LocalDateTime fechaEntrega) {
    this.registrarEstado(fechaEntrega, TipoEstadoDonacion.ENTREGADA, "Entregado", patenteCamion);
  }

  // req. 12: la entidad confirma que recibio una donacion ya entregada, con fotos de lo que llego
  public void confirmarRecepcion(List<String> fotos) {
    if (this.estadoActual != TipoEstadoDonacion.ENTREGADA) {
      throw new BusinessException("Solo se puede confirmar la recepción de una donación entregada");
    }
    if (this.recepcionConfirmada()) {
      throw new BusinessException("La recepción de esta donación ya fue confirmada");
    }
    if (fotos == null || fotos.isEmpty()) {
      throw new BusinessException("Cargá al menos una foto de lo que recibieron");
    }
    if (fotos.size() > MAXIMO_FOTOS_RECEPCION) {
      throw new BusinessException("Se pueden cargar hasta " + MAXIMO_FOTOS_RECEPCION + " fotos");
    }
    if (this.fotosRecepcion == null) {
      this.fotosRecepcion = new ArrayList<>();
    }
    this.fotosRecepcion.addAll(fotos);
  }

  public boolean recepcionConfirmada() {
    return this.fotosRecepcion != null && !this.fotosRecepcion.isEmpty();
  }

  public LocalDateTime fechaDelEstadoActual() {
    return this.historialEstados.isEmpty() ? this.fecha
        : this.historialEstados.get(this.historialEstados.size() - 1).getFecha();
  }

  public String justificacionDelEstadoActual() {
    if (this.historialEstados == null || this.historialEstados.isEmpty()) {
      return null;
    }
    return this.historialEstados.get(this.historialEstados.size() - 1).getJustificacion();
  }

  // la ultima asignacion: si volvio al deposito y se reasigno, cuenta la nueva
  public LocalDateTime fechaAsignacion() {
    return this.historialEstados.stream()
        .filter(cambio -> cambio.getEstado() == TipoEstadoDonacion.ASIGNACION_REALIZADA)
        .map(CambioEstado::getFecha)
        .reduce((primero, ultimo) -> ultimo)
        .orElse(null);
  }

  public String patenteCamionDeLaEntrega() {
    return this.historialEstados.stream()
        .filter(cambio -> cambio.getEstado() == TipoEstadoDonacion.ENTREGADA)
        .map(CambioEstado::getPatenteCamion)
        .filter(java.util.Objects::nonNull) // una entrega cargada sin camion no tiene patente
        .reduce((primero, ultimo) -> ultimo)
        .orElse(null);
  }

  public Double cantidadBienesRecibidos() {
    return this.cantidadBienesRecibidos(UnidadDeMedida.UNIDADES);
  }

  // cuanto aportan los bienes medidos en la unidad de la necesidad (unidades, kilogramos o litros)
  public Double cantidadBienesRecibidos(UnidadDeMedida unidad) {
    if (this.bienes == null)
      return 0.0;

    return this.bienes.stream()
        .mapToDouble(unidad::cantidadDe)
        .sum();
  }

  public Boolean estaDentroDelPeriodoActual(Periodo periodo) {
    return periodo != null && periodo.incluye(this.fecha);
  }

  public void cambiarEstado(TipoEstadoDonacion nuevoEstado, String justificacion) {
    this.registrarEstado(LocalDateTime.now(), nuevoEstado, justificacion);
  }

  // la entidad, la necesidad y el estado cambian juntos: no hay donacion asignada sin entidad
  public void asignarA(EntidadBeneficiaria entidad, Necesidad necesidad) {
    if (this.estadoActual != TipoEstadoDonacion.EN_DEPOSITO) {
      throw new BusinessException("Solo se puede asignar una donacion que esta en deposito");
    }
    this.entidadBeneficiariaAsignada = entidad;
    if (necesidad != null) {
      necesidad.asignarDonacion(this);
    }
    this.registrarEstado(LocalDateTime.now(), TipoEstadoDonacion.ASIGNACION_REALIZADA,
        "Donación asignada a la entidad: " + entidad.getId());
  }

  public void volverADeposito(String justificacion) {
    if (this.necesidad != null) {
      this.necesidad.liberarDonacion(this);
    }
    this.entidadBeneficiariaAsignada = null;
    this.registrarEstado(LocalDateTime.now(), TipoEstadoDonacion.EN_DEPOSITO, justificacion);
  }

  public void reprogramarEntrega(String justificacion) {
    if (this.estadoActual != TipoEstadoDonacion.ENTREGA_FALLIDA || this.entidadBeneficiariaAsignada == null) {
      throw new BusinessException("Solo se puede reprogramar una donacion con entrega fallida que tenga entidad asignada");
    }
    String detalle = (justificacion != null && !justificacion.isBlank())
        ? justificacion
        : "Envio reprogramado para proxima ruta hacia " + this.entidadBeneficiariaAsignada.getRazonSocial();
    this.registrarEstado(LocalDateTime.now(), TipoEstadoDonacion.ASIGNACION_REALIZADA, detalle);
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
