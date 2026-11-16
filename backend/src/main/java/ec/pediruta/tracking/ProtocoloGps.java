package ec.pediruta.tracking;

import java.util.regex.Pattern;

/**
 * Protocolo de texto propio para el socket TCP (tema 1.3).
 *
 * Una línea por posición, campos separados por punto y coma:
 *
 * <pre>
 *   PLACA;LATITUD;LONGITUD[;VELOCIDAD_KMH][;MARCA_DE_TIEMPO_MS]
 *   PBA-1234;-0.2531;-79.1720;38.5;1763322000123
 * </pre>
 *
 * El servidor responde una línea: "OK" o "ERROR motivo".
 * Se usa punto como separador decimal, sin importar el idioma del equipo.
 */
public final class ProtocoloGps {

    private static final Pattern PLACA = Pattern.compile("[A-Z]{3}-\\d{3,4}");

    private ProtocoloGps() {
    }

    public static Posicion leerLinea(String linea, String origen) {
        if (linea == null || linea.isBlank()) {
            throw new IllegalArgumentException("línea vacía");
        }
        String[] campos = linea.trim().split(";", -1);
        if (campos.length < 3 || campos.length > 5) {
            throw new IllegalArgumentException("formato esperado PLACA;LAT;LON[;VELOCIDAD][;MARCA_MS]");
        }
        double lat = numero(campos[1], "latitud");
        double lon = numero(campos[2], "longitud");
        double velocidad = campos.length > 3 && !campos[3].isBlank() ? numero(campos[3], "velocidad") : 0;
        Long marca = campos.length > 4 && !campos[4].isBlank() ? entero(campos[4]) : null;
        return crear(campos[0], lat, lon, velocidad, marca, origen);
    }

    /** Valida y arma la posición. Lanza IllegalArgumentException si algo no cuadra. */
    public static Posicion crear(String placa, double lat, double lon, double velocidadKmh,
                                 Long enviadoEnMs, String origen) {
        String limpia = placa == null ? "" : placa.trim().toUpperCase();
        if (!PLACA.matcher(limpia).matches()) {
            throw new IllegalArgumentException("placa inválida: " + limpia);
        }
        if (!(lat >= -90 && lat <= 90)) {
            throw new IllegalArgumentException("latitud fuera de rango");
        }
        if (!(lon >= -180 && lon <= 180)) {
            throw new IllegalArgumentException("longitud fuera de rango");
        }
        if (!(velocidadKmh >= 0 && velocidadKmh <= 200)) {
            throw new IllegalArgumentException("velocidad fuera de rango");
        }
        double redondeada = Math.round(velocidadKmh * 10) / 10.0;
        return new Posicion(limpia, lat, lon, redondeada, System.currentTimeMillis(), enviadoEnMs, origen);
    }

    private static double numero(String texto, String campo) {
        try {
            return Double.parseDouble(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " no es un número: " + texto);
        }
    }

    private static Long entero(String texto) {
        try {
            return Long.parseLong(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("marca de tiempo no es un número: " + texto);
        }
    }
}
