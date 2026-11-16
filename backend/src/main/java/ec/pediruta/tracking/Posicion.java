package ec.pediruta.tracking;

/**
 * Última ubicación conocida de un camión.
 *
 * @param placa         placa del camión, por ejemplo PBA-1234
 * @param lat           latitud
 * @param lon           longitud
 * @param velocidadKmh  velocidad reportada por el dispositivo
 * @param recibidoEnMs  momento en que llegó al servidor (milisegundos)
 * @param enviadoEnMs   momento en que el dispositivo la envió, si lo informa (para medir latencia)
 * @param origen        "simulador" (socket TCP) o "celular" (WebSocket)
 */
public record Posicion(
        String placa,
        double lat,
        double lon,
        double velocidadKmh,
        long recibidoEnMs,
        Long enviadoEnMs,
        String origen) {
}
