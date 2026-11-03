package ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion;

public enum CategoriaDonante {
  BRONCE(0, "Donante inicial"),
  PLATA(3, "Donante comprometido"),
  ORO(5, "Donante frecuente"),
  PLATINO(10, "Heroe comunitario");

  private final int donacionesRequeridas;
  private final String titulo;

  CategoriaDonante(int donacionesRequeridas, String titulo) {
    this.donacionesRequeridas = donacionesRequeridas;
    this.titulo = titulo;
  }

  public int getDonacionesRequeridas() {
    return donacionesRequeridas;
  }

  public String getTitulo() {
    return titulo;
  }

  public static CategoriaDonante calcularPara(int cantidadDonaciones) {
    if(cantidadDonaciones >= PLATINO.getDonacionesRequeridas())return PLATINO;
    if(cantidadDonaciones >= ORO.getDonacionesRequeridas())return ORO;
    if(cantidadDonaciones >= PLATA.getDonacionesRequeridas())return PLATA;
    return BRONCE;
  }
}
