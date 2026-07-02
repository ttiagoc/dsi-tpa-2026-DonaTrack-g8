package ar.edu.utn.frba.ddsi.common.utils;

public class GeneradorIdSecuencial {
    private long siguiente;

    public GeneradorIdSecuencial(long valorInicial) {
        this.siguiente = valorInicial;
    }

    public GeneradorIdSecuencial() {
        this.siguiente = 1L;
    }

    public long siguiente() {
        return siguiente++;
    }
}
