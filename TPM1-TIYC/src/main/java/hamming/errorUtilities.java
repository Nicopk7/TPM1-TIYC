package hamming;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Inyector de errores para archivos Hamming (.HAx).
 *
 * Provee dos modos de inyección:
 *
 *   MODO 1 — Un error máximo por módulo (LAB 1):
 *     - Random si hay error en el módulo
 *     - Random en qué posición
 *
 *   MODO 2 — Hasta dos errores por módulo (LAB 3):
 *     - Random si hay error en el módulo
 *     - Random si son 1 o 2 errores
 *     - Random en qué posiciones (distintas entre sí)
 *
 * También implementa la fecha de apertura:
 *   El archivo .HAx puede llevar embebida una fecha/hora mínima de apertura.
 *   Si la fecha actual no la cumple, desproteger retorna null.
 *
 * ESTRUCTURA DEL ENCABEZADO .HAx:
 *   [0-3]   cantBloques   (int)
 *   [4-7]   totalBitsInfo (int)
 *   [8-11]  fechaApertura en epoch seconds (int, 0 = sin restricción)
 *   [12..]  stream codificado
 *
 * NOTA: el encabezado ahora es de 12 bytes (antes 8).
 *       HammingCodec debe actualizar HEADER_SIZE = 12.
 */
public class errorUtilities {

    public static final  int    HEADER_SIZE  = 12; // 3 ints × 4 bytes
    private static final double PROB_ERROR   = 0.5;

    // =========================================================================
    // MODO 1 — UN ERROR MÁXIMO POR MÓDULO
    // =========================================================================

    public static byte[] injectOneError(byte[] datos, int blockIndex) {
        return injectOneError(datos, blockIndex, PROB_ERROR);
    }

    /**
     * Introduce máximo UN error por módulo.
     *
     * Por cada módulo:
     *   - Random si hay error (según probabilidad)
     *   - Random en qué posición del módulo
     *
     * @param datos        Bytes del archivo .HAx
     * @param blockIndex   Tamaño de bloque
     * @param probabilidad Probabilidad de error por módulo (0.0 a 1.0)
     */
    public static byte[] injectOneError(byte[] datos, int blockIndex, double probabilidad) {
        validarProbabilidad(probabilidad);
        if (datos == null || datos.length <= HEADER_SIZE) return datos;

        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int cantBloques = leerInt(datos, 0);

        byte[] resultado = datos.clone();
        int[]  stream    = bytesToBits(datos, HEADER_SIZE, datos.length - HEADER_SIZE);

        Random random = new Random();

        for (int b = 0; b < cantBloques; b++) {
            // ¿Hay error en este módulo?
            if (random.nextDouble() < probabilidad) {
                // ¿En qué posición?
                int pos = b * tamBloque + random.nextInt(tamBloque);
                if (pos < stream.length) stream[pos] ^= 1;
            }
        }

        escribirStream(resultado, stream);
        return resultado;
    }

    // =========================================================================
    // MODO 2 — HASTA DOS ERRORES POR MÓDULO
    // =========================================================================

    public static byte[] injectUpToTwoErrors(byte[] datos, int blockIndex) {
        return injectUpToTwoErrors(datos, blockIndex, PROB_ERROR);
    }

    /**
     * Introduce hasta DOS errores por módulo, todo aleatorio.
     *
     * Por cada módulo:
     *   1. Random si hay error en el módulo (según probabilidad)
     *   2. Si hay error: random si son 1 o 2 errores (50/50)
     *   3. Random en qué posiciones (distintas entre sí)
     *
     * Con 2 errores, Hamming los detecta pero NO los puede corregir
     * (el síndrome da una posición falsa). Por eso el enunciado pide
     * poder "detectar 2 errores" como modo separado.
     *
     * @param datos        Bytes del archivo .HAx
     * @param blockIndex   Tamaño de bloque
     * @param probabilidad Probabilidad de que un módulo reciba errores
     */
    public static byte[] injectUpToTwoErrors(byte[] datos, int blockIndex, double probabilidad) {
        validarProbabilidad(probabilidad);
        if (datos == null || datos.length <= HEADER_SIZE) return datos;

        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int cantBloques = leerInt(datos, 0);

        byte[] resultado = datos.clone();
        int[]  stream    = bytesToBits(datos, HEADER_SIZE, datos.length - HEADER_SIZE);

        Random random = new Random();

        for (int b = 0; b < cantBloques; b++) {

            // Decisión 1: ¿hay error en este módulo?
            if (random.nextDouble() < probabilidad) {

                // Decisión 2: ¿1 o 2 errores? (50/50)
                int cantErrores = random.nextBoolean() ? 1 : 2;

                if (cantErrores == 1) {
                    // Un solo error en posición aleatoria
                    int pos = b * tamBloque + random.nextInt(tamBloque);
                    if (pos < stream.length) stream[pos] ^= 1;

                } else {
                    // Dos errores en posiciones distintas
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

    // =========================================================================
    // FECHA DE APERTURA
    // =========================================================================

    /**
     * Escribe la fecha de apertura en el encabezado del archivo .HAx.
     *
     * La fecha se guarda en los bytes [8-11] como segundos desde el epoch
     * truncados a int (válido hasta el año 2038, suficiente para el proyecto).
     *
     * @param datos          Bytes del archivo .HAx
     * @param fechaApertura  Fecha/hora mínima para abrir el archivo
     * @return               Copia del archivo con la fecha embebida
     */
    public static byte[] setFechaApertura(byte[] datos, LocalDateTime fechaApertura) {
        if (datos == null || datos.length < HEADER_SIZE) return datos;

        byte[] resultado = datos.clone();
        long epochSeconds = fechaApertura
                .toEpochSecond(java.time.ZoneOffset.UTC);

        // Guardar en bytes [8-11]
        escribirInt(resultado, 8, (int) epochSeconds);
        return resultado;
    }

    /**
     * Verifica si la fecha actual permite abrir el archivo.
     *
     * @param datos Bytes del archivo .HAx
     * @return      true si se puede abrir (fecha actual >= fecha apertura, o sin restricción)
     */
    public static boolean verificarFechaApertura(byte[] datos) {
        if (datos == null || datos.length < HEADER_SIZE) return true;

        int epochGuardado = leerInt(datos, 8);
        if (epochGuardado == 0) return true; // sin restricción

        long ahora = LocalDateTime.now()
                .toEpochSecond(java.time.ZoneOffset.UTC);

        return ahora >= epochGuardado;
    }

    /**
     * Retorna la fecha de apertura embebida en el archivo como String legible.
     * Si no tiene fecha, retorna "Sin restricción".
     */
    public static String getFechaAperturaString(byte[] datos) {
        if (datos == null || datos.length < HEADER_SIZE) return "Sin restricción";

        int epochGuardado = leerInt(datos, 8);
        if (epochGuardado == 0) return "Sin restricción";

        LocalDateTime fecha = LocalDateTime.ofEpochSecond(
                epochGuardado, 0, java.time.ZoneOffset.UTC);
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    // =========================================================================
    // ESTADÍSTICAS
    // =========================================================================

    /**
     * Cuenta cuántos módulos tienen al menos un bit diferente
     * entre el original y el archivo con errores.
     */
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

    /**
     * Cuenta cuántos módulos tienen exactamente 2 errores.
     * Útil para mostrar en estadísticas del modo doble error.
     */
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

    /**
     * Genera el resumen de errores para el log de la GUI.
     * Detecta automáticamente si hubo módulos con 2 errores.
     */
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

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

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