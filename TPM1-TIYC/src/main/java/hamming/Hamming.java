package hamming;

import hamming.bitutilities.bitUtilities;
import hamming.file_mngmt.FileManagement;

import java.util.BitSet;


public class Hamming {

    // Tamaño total del bloque
    public static final int TAM_BLOQUE_8     = 8;
    public static final int TAM_BLOQUE_1024  = 1024;
    public static final int TAM_BLOQUE_16384 = 16384;

    // Bits de control
    public static final int CTRL_8     = 4;
    public static final int CTRL_1024  = 10;
    public static final int CTRL_16384 = 14;

    // Bits de información
    public static final int INFO_8     = TAM_BLOQUE_8     - CTRL_8;      // 4
    public static final int INFO_1024  = TAM_BLOQUE_1024  - CTRL_1024;   // 1014
    public static final int INFO_16384 = TAM_BLOQUE_16384 - CTRL_16384;  // 16370

    // ENCODE — codifica un archivo completo

    public static byte[] encode(byte[] datos, int blockIndex) {

        int tamBloque   = getTamBloque(blockIndex);
        int bitsControl = getBitsControl(blockIndex);
        int bitsInfo    = tamBloque - bitsControl;

        int totalBitsInfo = datos.length * 8;
        BitSet streamInfo = bitUtilities.bufferToBitset(datos, datos.length);

        int cantBloques           = (int) Math.ceil((double) totalBitsInfo / bitsInfo);
        int totalBitsCodificados  = cantBloques * tamBloque;
        int totalBytesCodificados = (int) Math.ceil((double) totalBitsCodificados / 8);

        BitSet streamCodificado = new BitSet(totalBitsCodificados);

        for (int b = 0; b < cantBloques; b++) {

            int bitsDisponibles = Math.min(bitsInfo, totalBitsInfo - b * bitsInfo);

            BitSet bloqueInfo = new BitSet(bitsInfo);
            for (int i = 0; i < bitsDisponibles; i++) {
                bloqueInfo.set(i, streamInfo.get(b * bitsInfo + i));
            }

            BitSet bloqueHamming = bitUtilities.repartirInfo(bloqueInfo, tamBloque);


            bloqueHamming = calcularParidades(bloqueHamming, tamBloque, bitsControl);

            streamCodificado = bitUtilities.concatBits(streamCodificado, bloqueHamming, b * tamBloque, tamBloque);
        }

        return bitsetToBytes(streamCodificado, totalBytesCodificados);
    }

    // DECODE
    public static byte[] decode(byte[] datos, int blockIndex, boolean corregir, int cantBytesOriginal) {

        int tamBloque   = getTamBloque(blockIndex);
        int bitsControl = getBitsControl(blockIndex);
        int bitsInfo    = tamBloque - bitsControl;

        int totalBitsCodificados = datos.length * 8;
        int cantBloques          = totalBitsCodificados / tamBloque;

        BitSet streamCodificado = bitUtilities.bufferToBitset(datos, datos.length);

        int totalBitsRecuperados = cantBloques * bitsInfo;
        BitSet streamRecuperado  = new BitSet(totalBitsRecuperados);

        for (int b = 0; b < cantBloques; b++) {

            BitSet bloque = new BitSet(tamBloque);
            for (int i = 0; i < tamBloque; i++) {
                bloque.set(i, streamCodificado.get(b * tamBloque + i));
            }

            int sindrome = calcularSindrome(bloque, tamBloque, bitsControl);

            if (corregir && sindrome != 0) {
                int posError = sindrome - 1;
                if (posError < tamBloque) {
                    bloque.flip(posError);
                }
            }
            BitSet soloInfo = extraerInfo(bloque, tamBloque, bitsInfo);
            streamRecuperado = bitUtilities.concatBits(streamRecuperado, soloInfo, b * bitsInfo, bitsInfo);
        }
        int totalBytesRecuperados = (int) Math.ceil((double) totalBitsRecuperados / 8);
        byte[] resultado = bitsetToBytes(streamRecuperado, totalBytesRecuperados);

        if (cantBytesOriginal > 0 && cantBytesOriginal <= resultado.length) {
            byte[] recortado = new byte[cantBytesOriginal];
            System.arraycopy(resultado, 0, recortado, 0, cantBytesOriginal);
            return recortado;
        }

        return resultado;
    }

    // CÁLCULO DE PARIDADES por XOR directo (encode)

    private static BitSet calcularParidades(BitSet bloque, int tamBloque, int bitsControl) {

        for (int p = 0; p < bitsControl; p++) {
            int posParidad = (1 << p); // potencia de 2: 1, 2, 4, 8, 16...

            boolean xorAcum = false;

            for (int pos = 1; pos <= tamBloque; pos++) {
                if (pos == posParidad) continue;

                if ((pos & posParidad) != 0) {
                    xorAcum ^= bloque.get(pos - 1);
                }
            }
            bloque.set(posParidad - 1, xorAcum);
        }

        return bloque;
    }

    // CÁLCULO DE SÍNDROME por XOR directo (decode)

    private static int calcularSindrome(BitSet bloque, int tamBloque, int bitsControl) {

        int sindrome = 0;

        for (int p = 0; p < bitsControl; p++) {
            int posParidad = (1 << p); // 1, 2, 4, 8, ...

            boolean xorAcum = false;

            for (int pos = 1; pos <= tamBloque; pos++) {
                if ((pos & posParidad) != 0) {
                    xorAcum ^= bloque.get(pos - 1);
                }
            }
            if (xorAcum) {
                sindrome |= posParidad;
            }
        }

        return sindrome; // 0 = sin error, N = posición del bit erróneo (1-based)
    }

    // EXTRACCIÓN DE INFORMACIÓN (decode)

    private static BitSet extraerInfo(BitSet bloque, int tamBloque, int bitsInfo) {
        BitSet info = new BitSet(bitsInfo);
        int j = 0;
        for (int i = 0; i < tamBloque; i++) {
            if (!bitUtilities.isPotenciaDeDos(i + 1)) {
                info.set(j, bloque.get(i));
                j++;
            }
        }
        return info;
    }

    // HELPERS DE CONFIGURACIÓN

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
            case FileManagement.BLOCK_8:     return CTRL_8;
            case FileManagement.BLOCK_1024:  return CTRL_1024;
            case FileManagement.BLOCK_16384: return CTRL_16384;
            default: throw new IllegalArgumentException("blockIndex inválido: " + blockIndex);
        }
    }

    public static int getBitsInfo(int blockIndex) {
        return getTamBloque(blockIndex) - getBitsControl(blockIndex);
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