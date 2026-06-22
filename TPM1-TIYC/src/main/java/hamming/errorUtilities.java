package hamming;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;


public class errorUtilities {

    public static final  int    HEADER_SIZE  = 12; // 3 ints × 4 bytes
    private static final double PROB_ERROR   = 0.5;


    // MODO 1 — UN ERROR MÁXIMO POR MÓDULO

    public static byte[] injectOneError(byte[] datos, int blockIndex) {
        return injectOneError(datos, blockIndex, PROB_ERROR);
    }


    public static byte[] injectOneError(byte[] datos, int blockIndex, double probabilidad) {
        validarProbabilidad(probabilidad);
        if (datos == null || datos.length <= HEADER_SIZE) return datos;

        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int cantBloques = leerInt(datos, 0);

        byte[] resultado = datos.clone();
        int[]  stream    = bytesToBits(datos, HEADER_SIZE, datos.length - HEADER_SIZE);

        Random random = new Random();

        for (int b = 0; b < cantBloques; b++) {
            // hay error en este modulo?
            if (random.nextDouble() < probabilidad) {
                // donde?
                int pos = b * tamBloque + random.nextInt(tamBloque);
                if (pos < stream.length) stream[pos] ^= 1;
            }
        }

        escribirStream(resultado, stream);
        return resultado;
    }


    // MODO 2 — HASTA DOS ERRORES POR MÓDULO

    public static byte[] injectUpToTwoErrors(byte[] datos, int blockIndex) {
        return injectUpToTwoErrors(datos, blockIndex, PROB_ERROR);
    }

    public static byte[] injectUpToTwoErrors(byte[] datos, int blockIndex, double probabilidad) {
        validarProbabilidad(probabilidad);
        if (datos == null || datos.length <= HEADER_SIZE) return datos;

        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int cantBloques = leerInt(datos, 0);

        byte[] resultado = datos.clone();
        int[]  stream    = bytesToBits(datos, HEADER_SIZE, datos.length - HEADER_SIZE);

        Random random = new Random();

        for (int b = 0; b < cantBloques; b++) {

            // hay error en este modulo?
            if (random.nextDouble() < probabilidad) {

                // 1 o 2 ? (50/50)
                int cantErrores = random.nextBoolean() ? 1 : 2;

                if (cantErrores == 1) {
                    // Un solo error
                    int pos = b * tamBloque + random.nextInt(tamBloque);
                    if (pos < stream.length) stream[pos] ^= 1;

                } else {
                    // Dos errores
                    int pos1 = random.nextInt(tamBloque);
                    int pos2;
                    do {
                        pos2 = random.nextInt(tamBloque);
                    } while (pos2 == pos1); // garantizar que sean distintas

                    int absPos1 = b * tamBloque + pos1;
                    int absPos2 = b * tamBloque + pos2;

                    if (absPos1 < stream.length) stream[absPos1] ^= 1;
                    if (absPos2 < stream.length) stream[absPos2] ^= 1;
                }
            }
        }

        escribirStream(resultado, stream);
        return resultado;
    }

    // FECHA DE APERTURA


    public static byte[] setFechaApertura(byte[] datos, LocalDateTime fechaApertura) {
        if (datos == null || datos.length < HEADER_SIZE) return datos;

        byte[] resultado = datos.clone();
        long epochSeconds = fechaApertura
                .toEpochSecond(java.time.ZoneOffset.UTC);

        escribirInt(resultado, 8, (int) epochSeconds);
        return resultado;
    }


    public static boolean verificarFechaApertura(byte[] datos) {
        if (datos == null || datos.length < HEADER_SIZE) return true;

        int epochGuardado = leerInt(datos, 8);
        if (epochGuardado == 0) return true; // sin restricción

        long ahora = LocalDateTime.now()
                .toEpochSecond(java.time.ZoneOffset.UTC);

        return ahora >= epochGuardado;
    }

    public static String getFechaAperturaString(byte[] datos) {
        if (datos == null || datos.length < HEADER_SIZE) return "Sin restricción";

        int epochGuardado = leerInt(datos, 8);
        if (epochGuardado == 0) return "Sin restricción";

        LocalDateTime fecha = LocalDateTime.ofEpochSecond(
                epochGuardado, 0, java.time.ZoneOffset.UTC);
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    // ESTADÍSTICAS

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
                    break; // un módulo cuenta una sola vez
                }
            }
        }
        return modulosConError;
    }

    public static int contarModulosConDosErrores(byte[] original, byte[] conErrores, int blockIndex) {
        if (original == null || conErrores == null) return 0;

        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int cantBloques = leerInt(original, 0);

        int[] streamOrig = bytesToBits(original,   HEADER_SIZE, original.length   - HEADER_SIZE);
        int[] streamErr  = bytesToBits(conErrores, HEADER_SIZE, conErrores.length - HEADER_SIZE);

        int modulos2Errores = 0;
        for (int b = 0; b < cantBloques; b++) {
            int diferencias = 0;
            for (int i = 0; i < tamBloque; i++) {
                int pos = b * tamBloque + i;
                if (pos < streamOrig.length && pos < streamErr.length
                        && streamOrig[pos] != streamErr[pos]) {
                    diferencias++;
                }
            }
            if (diferencias == 2) modulos2Errores++;
        }
        return modulos2Errores;
    }

    public static String resumenErrores(byte[] original, byte[] conErrores, int blockIndex) {
        int cantBloques  = leerInt(original, 0);
        int con1Error    = contarModulosConError(original, conErrores, blockIndex);
        int con2Errores  = contarModulosConDosErrores(original, conErrores, blockIndex);
        double pct       = cantBloques > 0 ? (double) con1Error / cantBloques * 100 : 0;

        if (con2Errores > 0) {
            return String.format(
                    "Errores introducidos: %d módulos de %d total (%.1f%%) — %d con 1 error, %d con 2 errores",
                    con1Error, cantBloques, pct,
                    con1Error - con2Errores, con2Errores);
        }
        return String.format(
                "Errores introducidos: %d módulos de %d total (%.1f%%)",
                con1Error, cantBloques, pct);
    }

    // HELPERS PRIVADOS

    private static void validarProbabilidad(double p) {
        if (p < 0.0 || p > 1.0)
            throw new IllegalArgumentException("Probabilidad debe estar entre 0.0 y 1.0");
    }

    private static void escribirStream(byte[] resultado, int[] stream) {
        byte[] streamBytes = bitsToBytes(stream);
        System.arraycopy(streamBytes, 0, resultado, HEADER_SIZE, streamBytes.length);
    }

    private static int leerInt(byte[] datos, int offset) {
        return ((datos[offset]     & 0xFF) << 24)
                | ((datos[offset + 1] & 0xFF) << 16)
                | ((datos[offset + 2] & 0xFF) <<  8)
                |  (datos[offset + 3] & 0xFF);
    }

    private static void escribirInt(byte[] datos, int offset, int valor) {
        datos[offset]     = (byte)(valor >> 24);
        datos[offset + 1] = (byte)(valor >> 16);
        datos[offset + 2] = (byte)(valor >>  8);
        datos[offset + 3] = (byte)(valor);
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
                if (idx < bits.length && bits[idx] == 1)
                    valor |= (1 << (7 - j));
            }
            datos[i] = (byte) valor;
        }
        return datos;
    }
}