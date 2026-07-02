package ar.edu.utn.frba.ddsi.common.models.entities;

public class Telefono implements CanalContacto {

  @Override
  public void notificar(String valor, String mensaje) {
    System.out.println("[SMS Gateway] Enviando SMS a " + valor + ": " + mensaje);
  }
}