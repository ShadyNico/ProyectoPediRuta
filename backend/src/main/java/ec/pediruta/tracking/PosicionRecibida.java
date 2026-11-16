package ec.pediruta.tracking;

/** Evento interno: llegó una posición nueva (patrón Observer con eventos de Spring). */
public record PosicionRecibida(Posicion posicion) {
}
