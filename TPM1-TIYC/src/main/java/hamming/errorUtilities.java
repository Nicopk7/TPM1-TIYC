package hamming;

import hamming.file_mngmt.FileManagement;
import java.util.Random;

public class errorUtilities {

    private static final double PROB_ERROR  = 0.5;
    private static final int    HEADER_SIZE = 8;

    // ── 1 error aleatorio por módulo (comportamiento original) ────────────────

    public static byte[] injectErrors(byte[] datos, int blockIndex) {
        return injectErrors(datos, blockIndex, PROB_ERROR);
    }

    public static byte[] injectErrors(byte[] datos, int blockIndex, double probabilidad) {
        if (probabilidad < 0.0 || probabilidad > 1.0)
            throw new IllegalArgumentException("Probabilidad debe estar entre 0.0 y 1.0");
        if (datos == null || datos.length <= HEADER_SIZE) return datos;

        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int cantBloques = leerInt(datos, 0);
        byte[] resultado = datos.clone();
        int[] stream = bytesToBits(datos, HEADER_SIZE, datos.length - HEADER_SIZE);

        Random random = new Random();
        for (int b = 0; b < cantBloques; b++) {
            if (random.nextDouble() < probabilidad) {
                int posEnBloque = random.nextInt(tamBloque);
                int posEnStream = b * tamBloque + posEnBloque;
                if (posEnStream < stream.length) stream[posEnStream] ^= 1;
            }
        }

        byte[] streamBytes = bitsToBytes(stream);
        System.arraycopy(streamBytes, 0, resultado, HEADER_SIZE, streamBytes.length);
        return resultado;
    }

    // ── 2 errores programados por módulo ──────────────────────────────────────

    /**
     * Inyecta 2 errores por módulo en forma programada:
     * - Aleatorio si ese módulo recibe errores (probabilidad PROB_ERROR).
     * - Si sí, aleatorio en qué 2 posiciones distintas dentro del módulo.
     */
    public static byte[] injectDoubleErrors(byte[] datos, int blockIndex) {
        return injectDoubleErrors(datos, blockIndex, PROB_ERROR);
    }

    public static byte[] injectDoubleErrors(byte[] datos, int blockIndex, double probabilidad) {
        if (probabilidad < 0.0 || probabilidad > 1.0)
            throw new IllegalArgumentException("Probabilidad debe estar entre 0.0 y 1.0");
        if (datos == null || datos.length <= HEADER_SIZE) return datos;

        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int cantBloques = leerInt(datos, 0);
        byte[] resultado = datos.clone();
        int[] stream = bytesToBits(datos, HEADER_SIZE, datos.length - HEADER_SIZE);

        Random random = new Random();
        for (int b = 0; b < cantBloques; b++) {
            // Decidir aleatoriamente si este módulo recibe errores
            if (random.nextDouble() < probabilidad) {
                // Elegir 2 posiciones distintas dentro del bloque
                int pos1 = random.nextInt(tamBloque);
                int pos2;
                do { pos2 = random.nextInt(tamBloque); } while (pos2 == pos1);

                int idx1 = b * tamBloque + pos1;
                int idx2 = b * tamBloque + pos2;
                if (idx1 < stream.length) stream[idx1] ^= 1;
                if (idx2 < stream.length) stream[idx2] ^= 1;
            }
        }

        byte[] streamBytes = bitsToBytes(stream);
        System.arraycopy(streamBytes, 0, resultado, HEADER_SIZE, streamBytes.length);
        return resultado;
    }

    // ── ESTADÍSTICAS ──────────────────────────────────────────────────────────

    public static int contarModulosConError(byte[] original, byte[] conErrores, int blockIndex) {
        if (original == null || conErrores == null) return 0;
        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int cantBloques = leerInt(original, 0);
        int[] streamOrig = bytesToBits(original,   HEADER_SIZE, original.length   - HEADER_SIZE);
        int[] streamErr  = bytesToBits(conErrores, HEADER_SIZE, conErrores.length - HEADER_SIZE);
        int modulosConError = 0;
        for (int b = 0; b < cantBloques; b++) {
            for (int i = 0; i < tamBloque; i++) {
                int pos = b * tamBloque + i;
                if (pos < streamOrig.length && pos < streamErr.length
                        && streamOrig[pos] != streamErr[pos]) {
                    modulosConError++;
                    break;
                }
            }
        }
        return modulosConError;
    }

    public static String resumenErrores(byte[] original, byte[] conErrores, int blockIndex) {
        int cantBloques   = leerInt(original, 0);
        int conError      = contarModulosConError(original, conErrores, blockIndex);
        double porcentaje = cantBloques > 0 ? (double) conError / cantBloques * 100 : 0;
        return String.format("Errores introducidos: %d módulos de %d total (%.1f%%)",
                conError, cantBloques, porcentaje);
    }

    // ── HELPERS PRIVADOS ─────────────────────────────────────────────────────

    private static int leerInt(byte[] datos, int offset) {
        return ((datos[offset]     & 0xFF) << 24)
                | ((datos[offset + 1] & 0xFF) << 16)
                | ((datos[offset + 2] & 0xFF) <<  8)
                |  (datos[offset + 3] & 0xFF);
    }

    private static int[] bytesToBits(byte[] datos, int desde, int cant) {
        int[] bits = new int[cant * 8];
        for (int i = 0; i < cant; i++) {
            int b = datos[desde + i] & 0xFF;
            for (int j = 0; j < 8; j++)
                bits[i * 8 + j] = (b >> (7 - j)) & 1;
        }
        return bits;
    }

    private static byte[] bitsToBytes(int[] bits) {
        int cantBytes = (int) Math.ceil((double) bits.length / 8);
        byte[] datos  = new byte[cantBytes];
        for (int i = 0; i < cantBytes; i++) {
            int valor = 0;
            for (int j = 0; j < 8; j++) {
                int idx = i * 8 + j;
                if (idx < bits.length && bits[idx] == 1) valor |= (1 << (7 - j));
            }
            datos[i] = (byte) valor;
        }
        return datos;
    }
}