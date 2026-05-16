package hamming;

import hamming.file_mngmt.FileManagement;


public class Hamming {


    public static final int TAM_BLOQUE_8     = 8;
    public static final int TAM_BLOQUE_1024  = 1024;
    public static final int TAM_BLOQUE_16384 = 16384;

    public static final int CTRL_8     = 4;
    public static final int CTRL_1024  = 11;
    public static final int CTRL_16384 = 15;

    public static final int INFO_8     = TAM_BLOQUE_8     - CTRL_8;      //  4
    public static final int INFO_1024  = TAM_BLOQUE_1024  - CTRL_1024;   // 1013
    public static final int INFO_16384 = TAM_BLOQUE_16384 - CTRL_16384;  // 16369


    public static byte[] encode(byte[] datos, int blockIndex) {

        int tamBloque   = getTamBloque(blockIndex);
        int bitsControl = getBitsControl(blockIndex);
        int bitsInfo    = tamBloque - bitsControl;

        int[] streamInfo  = bytesToBits(datos);
        int totalBitsInfo = streamInfo.length;

        int cantBloques          = (int) Math.ceil((double) totalBitsInfo / bitsInfo);
        int totalBitsCodificados = cantBloques * tamBloque;

        int[] streamCodificado = new int[totalBitsCodificados];

        for (int b = 0; b < cantBloques; b++) {

            int[] bloqueInfo = new int[bitsInfo];
            int bitsDisponibles = Math.min(bitsInfo, totalBitsInfo - b * bitsInfo);
            for (int i = 0; i < bitsDisponibles; i++) {
                bloqueInfo[i] = streamInfo[b * bitsInfo + i];
            }

            int[] bloqueHamming = repartirInfo(bloqueInfo, tamBloque);

            calcularParidades(bloqueHamming, tamBloque, bitsControl);

            System.arraycopy(bloqueHamming, 0, streamCodificado, b * tamBloque, tamBloque);
        }

        byte[] streamBytes = bitsToBytes(streamCodificado);
        byte[] resultado   = new byte[8 + streamBytes.length];

        resultado[0] = (byte)(cantBloques >> 24);
        resultado[1] = (byte)(cantBloques >> 16);
        resultado[2] = (byte)(cantBloques >>  8);
        resultado[3] = (byte)(cantBloques);
        resultado[4] = (byte)(totalBitsInfo >> 24);
        resultado[5] = (byte)(totalBitsInfo >> 16);
        resultado[6] = (byte)(totalBitsInfo >>  8);
        resultado[7] = (byte)(totalBitsInfo);

        System.arraycopy(streamBytes, 0, resultado, 8, streamBytes.length);
        return resultado;
    }

    public static byte[] decode(byte[] datos, int blockIndex, boolean corregir, int cantBytesOriginal) {

        int tamBloque   = getTamBloque(blockIndex);
        int bitsControl = getBitsControl(blockIndex);
        int bitsInfo    = tamBloque - bitsControl;

        int cantBloques = ((datos[0] & 0xFF) << 24) | ((datos[1] & 0xFF) << 16)
                | ((datos[2] & 0xFF) <<  8) |  (datos[3] & 0xFF);
        int totalBitsInfo = ((datos[4] & 0xFF) << 24) | ((datos[5] & 0xFF) << 16)
                | ((datos[6] & 0xFF) <<  8) |  (datos[7] & 0xFF);

        byte[] streamSinHeader = new byte[datos.length - 8];
        System.arraycopy(datos, 8, streamSinHeader, 0, streamSinHeader.length);
        int[] streamCodificado = bytesToBits(streamSinHeader);

        int[] streamRecuperado = new int[cantBloques * bitsInfo];

        for (int b = 0; b < cantBloques; b++) {

            int[] bloque = new int[tamBloque];
            System.arraycopy(streamCodificado, b * tamBloque, bloque, 0, tamBloque);

            int sindrome = calcularSindrome(bloque, tamBloque, bitsControl);

            if (corregir && sindrome != 0) {
                int posError = sindrome - 1;
                if (posError < tamBloque) {
                    bloque[posError] ^= 1;
                }
            }

            int[] soloInfo = extraerInfo(bloque, tamBloque, bitsInfo);

            System.arraycopy(soloInfo, 0, streamRecuperado, b * bitsInfo, bitsInfo);
        }

        byte[] resultado = bitsToBytes(streamRecuperado);

        int bytesReales = (int) Math.ceil((double) totalBitsInfo / 8);
        if (bytesReales > 0 && bytesReales <= resultado.length) {
            byte[] recortado = new byte[bytesReales];
            System.arraycopy(resultado, 0, recortado, 0, bytesReales);
            return recortado;
        }

        return resultado;
    }

    private static int[] repartirInfo(int[] info, int tamBloque) {
        int[] bloque = new int[tamBloque];
        int j = 0;
        for (int i = 0; i < tamBloque && j < info.length; i++) {
            if (!esPotenciaDeDos(i + 1)) {  // posición 1-based
                bloque[i] = info[j++];
            }
            // posiciones de paridad quedan en 0
        }
        return bloque;
    }

    private static void calcularParidades(int[] bloque, int tamBloque, int bitsControl) {
        for (int p = 0; p < bitsControl; p++) {
            int posParidad = (1 << p);  // 1, 2, 4, 8, 16, ...
            if (posParidad > tamBloque) break;

            int xor = 0;
            for (int pos = 1; pos <= tamBloque; pos++) {
                if (pos == posParidad) continue;  // excluir la paridad misma
                if ((pos & posParidad) != 0) {
                    xor ^= bloque[pos - 1];       // pos-1: 1-based a 0-based
                }
            }
            bloque[posParidad - 1] = xor;
        }
    }

    private static int calcularSindrome(int[] bloque, int tamBloque, int bitsControl) {
        int sindrome = 0;
        for (int p = 0; p < bitsControl; p++) {
            int posParidad = (1 << p);
            if (posParidad > tamBloque) break;

            int xor = 0;
            for (int pos = 1; pos <= tamBloque; pos++) {
                if ((pos & posParidad) != 0) {
                    xor ^= bloque[pos - 1];
                }
            }
            if (xor != 0) {
                sindrome |= posParidad;
            }
        }
        return sindrome;
    }

    private static int[] extraerInfo(int[] bloque, int tamBloque, int bitsInfo) {
        int[] info = new int[bitsInfo];
        int j = 0;
        for (int i = 0; i < tamBloque && j < bitsInfo; i++) {
            if (!esPotenciaDeDos(i + 1)) {
                info[j++] = bloque[i];
            }
        }
        return info;
    }

    private static int[] bytesToBits(byte[] datos) {
        int[] bits = new int[datos.length * 8];
        for (int i = 0; i < datos.length; i++) {
            int b = datos[i] & 0xFF;  // tratar como unsigned
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + j] = (b >> (7 - j)) & 1;  // MSB primero
            }
        }
        return bits;
    }

    private static byte[] bitsToBytes(int[] bits) {
        int cantBytes = (int) Math.ceil((double) bits.length / 8);
        byte[] datos  = new byte[cantBytes];
        for (int i = 0; i < cantBytes; i++) {
            int valor = 0;
            for (int j = 0; j < 8; j++) {
                int bitIndex = i * 8 + j;
                if (bitIndex < bits.length && bits[bitIndex] == 1) {
                    valor |= (1 << (7 - j));
                }
            }
            datos[i] = (byte) valor;
        }
        return datos;
    }


    private static boolean esPotenciaDeDos(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    public static int getTamBloque(int blockIndex) {
        switch (blockIndex) {
            case FileManagement.BLOCK_8:     return TAM_BLOQUE_8;
            case FileManagement.BLOCK_1024:  return TAM_BLOQUE_1024;
            case FileManagement.BLOCK_16384: return TAM_BLOQUE_16384;
            default: throw new IllegalArgumentException("blockIndex inválido: " + blockIndex);
        }
    }

    public static int getBitsControl(int blockIndex) {
        switch (blockIndex) {
            case FileManagement.BLOCK_8:     return CTRL_8;     //  4
            case FileManagement.BLOCK_1024:  return CTRL_1024;  // 11
            case FileManagement.BLOCK_16384: return CTRL_16384; // 15
            default: throw new IllegalArgumentException("blockIndex inválido: " + blockIndex);
        }
    }

    public static int getBitsInfo(int blockIndex) {
        return getTamBloque(blockIndex) - getBitsControl(blockIndex);
    }
}