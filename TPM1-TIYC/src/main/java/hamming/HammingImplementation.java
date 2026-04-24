package hamming;

import hamming.bitutilities.bitUtilities;
import hamming.file_mngmt.FileManagement;

import java.util.BitSet;

/**
 * Codificador Hamming.
 *
 * Soporta tres tamaños de módulo total (info + control):
 *   BLOCK_8     →  4 bits info + 4 bits control  (módulo de  8 bits)  → .HA1
 *   BLOCK_1024  → 64 bits info + 10 bits control  (módulo de 74... ver nota)
 *   BLOCK_16384 → ...
 *
 * NOTA IMPORTANTE sobre "módulo de N bits":
 *   El enunciado dice que el módulo (bloque completo = info + control) mide
 *   8, 1024 o 16384 bits. Entonces:
 *     - Para módulo de    8 bits: bitsControl=4, bitsInfo=4   (2^4=16 >= 4+4+1=9 ✓)
 *     - Para módulo de 1024 bits: bitsControl=10, bitsInfo=1014 (2^10=1024 >= 1014+10+1 ✓)
 *     - Para módulo de 16384 bits: bitsControl=14, bitsInfo=16370 (2^14=16384 >= 16370+14+1 ✓)
 *
 * Flujo por bloque:
 *   1. Leer bitsInfo bits del archivo fuente
 *   2. repartirInfo()        → ubica los bits de info en las posiciones no-paridad
 *   3. calcularParidades()   → usa la matriz G para calcular y poner los bits de control
 *   4. Resultado: bloque Hamming completo de tamañoBloque bits
 */
public class HammingEncoder {

    // -------------------------------------------------------------------------
    // Constantes de configuración para cada modo
    // -------------------------------------------------------------------------

    // Tamaño total del bloque (info + control)
    public static final int TAM_BLOQUE_8     = 8;
    public static final int TAM_BLOQUE_1024  = 1024;
    public static final int TAM_BLOQUE_16384 = 16384;

    // Bits de control para cada modo
    public static final int CTRL_8     = 4;
    public static final int CTRL_1024  = 10;
    public static final int CTRL_16384 = 14;

    // Bits de información para cada modo (= tamBloque - bitsControl)
    public static final int INFO_8     = TAM_BLOQUE_8     - CTRL_8;      // 4
    public static final int INFO_1024  = TAM_BLOQUE_1024  - CTRL_1024;   // 1014
    public static final int INFO_16384 = TAM_BLOQUE_16384 - CTRL_16384;  // 16370

    // -------------------------------------------------------------------------
    // Método principal: codifica un archivo completo
    // -------------------------------------------------------------------------

    /**
     * Toma el contenido de un archivo .txt (como bytes) y lo codifica
     * con Hamming según el tamaño de bloque elegido.
     *
     * @param datos      Bytes del archivo original (de FileManagement.readFile)
     * @param blockIndex FileManagement.BLOCK_8, BLOCK_1024 o BLOCK_16384
     * @return           Bytes del archivo codificado, listos para guardar como .HAx
     */
    public static byte[] encode(byte[] datos, int blockIndex) {

        // Determinar parámetros según el modo
        int tamBloque   = getTamBloque(blockIndex);
        int bitsControl = getBitsControl(blockIndex);
        int bitsInfo    = tamBloque - bitsControl;

        // Convertir todo el archivo a un stream de bits
        int totalBitsInfo = datos.length * 8;
        BitSet streamInfo = bitUtilities.bufferToBitset(datos, datos.length);

        // Calcular cuántos bloques necesitamos (el último puede quedar incompleto)
        int cantBloques = (int) Math.ceil((double) totalBitsInfo / bitsInfo);

        // El archivo codificado tendrá cantBloques * tamBloque bits
        int totalBitsCodificados = cantBloques * tamBloque;
        int totalBytesCodificados = (int) Math.ceil((double) totalBitsCodificados / 8);

        BitSet streamCodificado = new BitSet(totalBitsCodificados);

        // Construir matriz generadora G (se calcula una sola vez por sesión)
        boolean[][] matrizG = bitUtilities.newMatrizGeneradora(bitsInfo, bitsControl, tamBloque);

        // Procesar bloque por bloque
        for (int b = 0; b < cantBloques; b++) {

            int bitsDisponibles = Math.min(bitsInfo, totalBitsInfo - b * bitsInfo);

            // 1. Extraer los bitsInfo bits de este bloque desde el stream
            BitSet bloqueInfo = new BitSet(bitsInfo);
            for (int i = 0; i < bitsDisponibles; i++) {
                bloqueInfo.set(i, streamInfo.get(b * bitsInfo + i));
            }
            // Si es el último bloque y está incompleto, los bits restantes quedan en 0 (padding)

            // 2. Distribuir los bits de info en las posiciones no-paridad
            BitSet bloqueHamming = bitUtilities.repartirInfo(bloqueInfo, tamBloque);

            // 3. Calcular y colocar los bits de paridad
            bloqueHamming = calcularParidades(bloqueHamming, tamBloque, bitsInfo, bitsControl, matrizG);

            // 4. Copiar el bloque codificado al stream de salida
            int posInicio = b * tamBloque;
            streamCodificado = bitUtilities.concatBits(streamCodificado, bloqueHamming, posInicio, tamBloque);
        }

        // Convertir el stream codificado a bytes y retornar
        return bitsetToBytes(streamCodificado, totalBytesCodificados);
    }

    // -------------------------------------------------------------------------
    // Cálculo de bits de paridad
    // -------------------------------------------------------------------------

    /**
     * Dado un bloque con los bits de información ya distribuidos en sus posiciones,
     * calcula y coloca los bits de paridad usando la matriz G.
     *
     * La matriz G tiene:
     *   filas    = bitsInfo    (una fila por bit de información)
     *   columnas = bitsControl (una columna por bit de paridad)
     *
     * Para calcular el bit de paridad C_i:
     *   C_i = XOR de todos los bits de info I_j donde matrizG[j][i] == true
     *
     * @param bloque    BitSet con info distribuida y paridades en 0
     * @param tamBloque Tamaño total del bloque
     * @param bitsInfo  Cantidad de bits de información
     * @param bitsCtrl  Cantidad de bits de control
     * @param matrizG   Matriz generadora precalculada
     * @return          El mismo bloque con los bits de paridad calculados
     */
    private static BitSet calcularParidades(BitSet bloque, int tamBloque,
                                            int bitsInfo, int bitsCtrl,
                                            boolean[][] matrizG) {

        // Extraer solo los bits de información en orden (para multiplicar por G)
        BitSet soloInfo = new BitSet(bitsInfo);
        int j = 0;
        for (int i = 0; i < tamBloque; i++) {
            if (!bitUtilities.isPotenciaDeDos(i + 1)) {
                soloInfo.set(j, bloque.get(i));
                j++;
            }
        }

        // Para cada bit de control C_i: C_i = XOR de I_j donde G[j][i] = true
        int posParidad = 0;
        for (int pos = 0; pos < tamBloque; pos++) {
            if (bitUtilities.isPotenciaDeDos(pos + 1)) {
                // Esta es una posición de paridad (potencia de 2)
                // Calcular su valor como XOR de los bits de info correspondientes
                boolean xorAcum = false;
                for (int k = 0; k < bitsInfo; k++) {
                    if (matrizG[k][posParidad]) {
                        xorAcum ^= soloInfo.get(k);
                    }
                }
                bloque.set(pos, xorAcum);
                posParidad++;
            }
        }

        return bloque;
    }

    // -------------------------------------------------------------------------
    // Helpers de configuración
    // -------------------------------------------------------------------------

    /**
     * Retorna el tamaño total del bloque (info + control) según el índice.
     */
    public static int getTamBloque(int blockIndex) {
        switch (blockIndex) {
            case FileManagement.BLOCK_8:     return TAM_BLOQUE_8;
            case FileManagement.BLOCK_1024:  return TAM_BLOQUE_1024;
            case FileManagement.BLOCK_16384: return TAM_BLOQUE_16384;
            default: throw new IllegalArgumentException("blockIndex inválido: " + blockIndex);
        }
    }

    /**
     * Retorna la cantidad de bits de control para el índice dado.
     */
    public static int getBitsControl(int blockIndex) {
        switch (blockIndex) {
            case FileManagement.BLOCK_8:     return CTRL_8;
            case FileManagement.BLOCK_1024:  return CTRL_1024;
            case FileManagement.BLOCK_16384: return CTRL_16384;
            default: throw new IllegalArgumentException("blockIndex inválido: " + blockIndex);
        }
    }

    /**
     * Retorna la cantidad de bits de información para el índice dado.
     */
    public static int getBitsInfo(int blockIndex) {
        return getTamBloque(blockIndex) - getBitsControl(blockIndex);
    }

    // -------------------------------------------------------------------------
    // Conversión BitSet → byte[]
    // -------------------------------------------------------------------------

    /**
     * Convierte un BitSet a un array de bytes de longitud 'cantBytes'.
     * Recorre el BitSet de a 8 bits y arma cada byte (MSB primero).
     */
    private static byte[] bitsetToBytes(BitSet bitset, int cantBytes) {
        byte[] resultado = new byte[cantBytes];
        for (int i = 0; i < cantBytes; i++) {
            int valor = 0;
            for (int b = 0; b < 8; b++) {
                if (bitset.get(i * 8 + b)) {
                    valor |= (1 << (7 - b));
                }
            }
            // Convertir a signed byte (Java usa complemento a 2)
            resultado[i] = (byte) (valor > 127 ? valor - 256 : valor);
        }
        return resultado;
    }
}