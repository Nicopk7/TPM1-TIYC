package hamming;

import hamming.bitutilities.bitUtilities;
import hamming.file_mngmt.FileManagement;

import java.util.BitSet;
import java.util.Random;

/**
 * Inyector de errores para archivos Hamming (.HAx).
 *
 * Por cada módulo del archivo codificado, decide aleatoriamente:
 *   1. Si se introduce un error en ese módulo (o no)
 *   2. En qué posición del módulo se introduce el error
 *
 * Máximo UN error por módulo, tal como pide el enunciado.
 */
public class errorUtilities {

    /**
     * Probabilidad de que un módulo reciba un error (0.0 a 1.0).
     * 0.5 = el 50% de los módulos tendrán un error.
     * Podés ajustarlo o hacerlo configurable desde la GUI.
     */
    private static final double PROB_ERROR = 0.5;

    // =========================================================================
    // MÉTODO PRINCIPAL
    // =========================================================================

    /**
     * Recorre el archivo codificado módulo por módulo e introduce errores
     * aleatorios. Máximo un error por módulo.
     *
     * @param datos      Bytes del archivo .HAx (leído con FileManagement.readFile)
     * @param blockIndex FileManagement.BLOCK_8, BLOCK_1024 o BLOCK_16384
     * @return           Nuevo array de bytes con errores introducidos → guardar como .HEx
     */
    public static byte[] injectErrors(byte[] datos, int blockIndex) {

        int tamBloque = Hamming.getTamBloque(blockIndex);

        int totalBits   = datos.length * 8;
        int cantBloques = totalBits / tamBloque;

        // Trabajamos sobre una copia para no modificar el original
        byte[] resultado = datos.clone();
        BitSet stream    = bitUtilities.bufferToBitset(resultado, resultado.length);

        Random random = new Random();

        for (int b = 0; b < cantBloques; b++) {

            // Decisión 1: ¿se introduce error en este módulo?
            if (random.nextDouble() < PROB_ERROR) {

                // Decisión 2: ¿en qué posición del módulo?
                // nextInt(tamBloque) da un valor entre 0 y tamBloque-1
                int posEnBloque  = random.nextInt(tamBloque);
                int posEnStream  = b * tamBloque + posEnBloque;

                // Flipear el bit elegido
                stream.flip(posEnStream);
            }
        }

        // Convertir el stream modificado de vuelta a bytes
        return bitsetToBytes(stream, resultado.length);
    }

    /**
     * Igual que injectErrors pero con probabilidad configurable.
     *
     * @param datos       Bytes del archivo .HAx
     * @param blockIndex  Índice de bloque
     * @param probabilidad Probabilidad de error por módulo (0.0 a 1.0)
     * @return            Bytes con errores introducidos
     */
    public static byte[] injectErrors(byte[] datos, int blockIndex, double probabilidad) {

        if (probabilidad < 0.0 || probabilidad > 1.0)
            throw new IllegalArgumentException("La probabilidad debe estar entre 0.0 y 1.0");

        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int totalBits   = datos.length * 8;
        int cantBloques = totalBits / tamBloque;

        byte[] resultado = datos.clone();
        BitSet stream    = bitUtilities.bufferToBitset(resultado, resultado.length);

        Random random = new Random();

        for (int b = 0; b < cantBloques; b++) {
            if (random.nextDouble() < probabilidad) {
                int posEnBloque = random.nextInt(tamBloque);
                int posEnStream = b * tamBloque + posEnBloque;
                stream.flip(posEnStream);
            }
        }

        return bitsetToBytes(stream, resultado.length);
    }

    // =========================================================================
    // ESTADÍSTICAS — útil para mostrar en la GUI
    // =========================================================================

    /**
     * Cuenta cuántos módulos tienen al menos un bit diferente entre
     * el archivo original (.HAx) y el archivo con errores (.HEx).
     * Útil para mostrar en pantalla cuántos errores se introdujeron.
     *
     * @param original   Bytes del archivo .HAx
     * @param conErrores Bytes del archivo .HEx
     * @param blockIndex Índice de bloque
     * @return           Cantidad de módulos con error
     */
    public static int contarModulosConError(byte[] original, byte[] conErrores, int blockIndex) {

        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int totalBits   = original.length * 8;
        int cantBloques = totalBits / tamBloque;

        BitSet streamOrig = bitUtilities.bufferToBitset(original,    original.length);
        BitSet streamErr  = bitUtilities.bufferToBitset(conErrores, conErrores.length);

        int modulosConError = 0;

        for (int b = 0; b < cantBloques; b++) {
            for (int i = 0; i < tamBloque; i++) {
                int pos = b * tamBloque + i;
                if (streamOrig.get(pos) != streamErr.get(pos)) {
                    modulosConError++;
                    break; // máximo un error por módulo, alcanza con encontrar uno
                }
            }
        }

        return modulosConError;
    }

    /**
     * Retorna un String con el resumen de errores introducidos.
     * Útil para el log o panel de estado de la GUI.
     *
     * Ejemplo de salida:
     *   "Errores introducidos: 3 módulos de 8 total (37.5%)"
     */
    public static String resumenErrores(byte[] original, byte[] conErrores, int blockIndex) {
        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int totalBits   = original.length * 8;
        int cantBloques = totalBits / tamBloque;
        int conError    = contarModulosConError(original, conErrores, blockIndex);
        double porcentaje = (double) conError / cantBloques * 100;

        return String.format("Errores introducidos: %d módulos de %d total (%.1f%%)",
                conError, cantBloques, porcentaje);
    }

    // =========================================================================
    // CONVERSIÓN BitSet → byte[]
    // =========================================================================

    private static byte[] bitsetToBytes(BitSet bitset, int cantBytes) {
        byte[] resultado = new byte[cantBytes];
        for (int i = 0; i < cantBytes; i++) {
            int valor = 0;
            for (int b = 0; b < 8; b++) {
                if (bitset.get(i * 8 + b)) {
                    valor |= (1 << (7 - b));
                }
            }
            resultado[i] = (byte) (valor > 127 ? valor - 256 : valor);
        }
        return resultado;
    }
}