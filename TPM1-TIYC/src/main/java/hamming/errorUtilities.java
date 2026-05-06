package hamming;

import hamming.bitutilities.bitUtilities;


import java.util.BitSet;
import java.util.Random;


public class errorUtilities {


    private static final double PROB_ERROR = 0.5;

    // METODO PRINCIPAL


    public static byte[] injectErrors(byte[] datos, int blockIndex) {

        int tamBloque = Hamming.getTamBloque(blockIndex);

        int totalBits   = datos.length * 8;
        int cantBloques = totalBits / tamBloque;

        // Trabajamos sobre una copia para no modificar el original
        byte[] resultado = datos.clone();
        BitSet stream    = bitUtilities.bufferToBitset(resultado, resultado.length);

        Random random = new Random();

        for (int b = 0; b < cantBloques; b++) {

            // Decisión 1: ¿se introduce error?
            if (random.nextDouble() < PROB_ERROR) {

                // Decisión 2: ¿en qué posición?
                int posEnBloque  = random.nextInt(tamBloque);
                int posEnStream  = b * tamBloque + posEnBloque;

                // Cambiar bit
                stream.flip(posEnStream);
            }
        }


        return bitsetToBytes(stream, resultado.length);
    }


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

    // ESTADÍSTICAS — útil para mostrar en la GUI


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
                    break; // máximo un error por módulo
                }
            }
        }

        return modulosConError;
    }


    public static String resumenErrores(byte[] original, byte[] conErrores, int blockIndex) {
        int tamBloque   = Hamming.getTamBloque(blockIndex);
        int totalBits   = original.length * 8;
        int cantBloques = totalBits / tamBloque;
        int conError    = contarModulosConError(original, conErrores, blockIndex);
        double porcentaje = (double) conError / cantBloques * 100;

        return String.format("Errores introducidos: %d módulos de %d total (%.1f%%)",
                conError, cantBloques, porcentaje);
    }


    // CONVERSIÓN BitSet → byte[]

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