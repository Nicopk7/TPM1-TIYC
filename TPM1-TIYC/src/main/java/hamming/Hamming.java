package hamming;

import hamming.bitutilities.bitUtilities;
import hamming.file_mngmt.FileManagement;

import java.util.BitSet;

/**
 * Codec Hamming — codificación y decodificación en un solo lugar.
 *
 * Soporta tres tamaños de módulo total (info + control):
 *   BLOCK_8     →  4 bits info + 4 bits control  (módulo de  8 bits)  → .HA1
 *   BLOCK_1024  → 1014 bits info + 10 bits control (módulo de 1024 bits) → .HA2
 *   BLOCK_16384 → 16370 bits info + 14 bits control (módulo de 16384 bits) → .HA3
 *
 * Implementación por XOR directo de posiciones (sin matrices G/H):
 *
 * ── ENCODE ──────────────────────────────────────────────────────────────────
 *   1. bufferToBitset()    → archivo .txt a stream de bits
 *   2. repartirInfo()      → ubica bits de info en posiciones no-paridad
 *   3. calcularParidades() → cada bit de paridad P en posición 2^i hace XOR
 *                            de todas las posiciones del bloque que tienen
 *                            el bit i activo en su representación binaria
 *   4. concatBits()        → ensambla bloques en stream final → .HAx
 *
 * ── DECODE ──────────────────────────────────────────────────────────────────
 *   1. bufferToBitset()    → archivo .HAx o .HEx a stream de bits
 *   2. calcularSindrome()  → mismo XOR por posición sobre el bloque recibido.
 *                            Si síndrome == 0: sin error.
 *                            Si síndrome != 0: su valor es la posición del error (1-based)
 *   3. corregir (opcional) → flipea el bit en la posición indicada por el síndrome
 *   4. extraerInfo()       → descarta posiciones de paridad, devuelve solo info
 *   5. resultado → .DCx (corregido) o .DEx (con errores, sin corregir)
 */
public class Hamming {

    // =========================================================================
    // CONSTANTES DE CONFIGURACIÓN
    // =========================================================================

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

    // =========================================================================
    // ENCODE — codifica un archivo completo
    // =========================================================================

    /**
     * Toma el contenido de un archivo .txt (como bytes) y lo codifica
     * con Hamming según el tamaño de bloque elegido.
     *
     * @param datos      Bytes del archivo original (de FileManagement.readFile)
     * @param blockIndex FileManagement.BLOCK_8, BLOCK_1024 o BLOCK_16384
     * @return           Bytes del archivo codificado, listos para guardar como .HAx
     */
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

            // 1. Extraer bitsInfo bits del stream original
            BitSet bloqueInfo = new BitSet(bitsInfo);
            for (int i = 0; i < bitsDisponibles; i++) {
                bloqueInfo.set(i, streamInfo.get(b * bitsInfo + i));
            }
            // Bits faltantes en el último bloque quedan en 0 (padding)

            // 2. Distribuir info en posiciones no-paridad
            BitSet bloqueHamming = bitUtilities.repartirInfo(bloqueInfo, tamBloque);

            // 3. Calcular y colocar bits de paridad por XOR directo de posiciones
            bloqueHamming = calcularParidades(bloqueHamming, tamBloque, bitsControl);

            // 4. Agregar bloque al stream de salida
            streamCodificado = bitUtilities.concatBits(streamCodificado, bloqueHamming, b * tamBloque, tamBloque);
        }

        return bitsetToBytes(streamCodificado, totalBytesCodificados);
    }

    // =========================================================================
    // DECODE — decodifica un archivo .HAx o .HEx
    // =========================================================================

    /**
     * Decodifica un archivo Hamming (.HAx o .HEx) y recupera los bytes originales.
     *
     * @param datos      Bytes del archivo codificado (leído con FileManagement.readFile)
     * @param blockIndex FileManagement.BLOCK_8, BLOCK_1024 o BLOCK_16384
     * @param corregir   true  → corrige el error antes de extraer info → genera .DCx
     *                   false → extrae info sin corregir              → genera .DEx
     * @param cantBytesOriginal Tamaño del archivo original en bytes.
     *                          Necesario para descartar el padding del último bloque.
     *                          Si no se conoce, pasar -1 (se retorna todo sin recortar).
     * @return Bytes recuperados del archivo original
     */
    public static byte[] decode(byte[] datos, int blockIndex, boolean corregir, int cantBytesOriginal) {

        int tamBloque   = getTamBloque(blockIndex);
        int bitsControl = getBitsControl(blockIndex);
        int bitsInfo    = tamBloque - bitsControl;

        int totalBitsCodificados = datos.length * 8;
        int cantBloques          = totalBitsCodificados / tamBloque;

        BitSet streamCodificado = bitUtilities.bufferToBitset(datos, datos.length);

        // Stream de salida: solo bits de información recuperados
        int totalBitsRecuperados = cantBloques * bitsInfo;
        BitSet streamRecuperado  = new BitSet(totalBitsRecuperados);

        for (int b = 0; b < cantBloques; b++) {

            // 1. Extraer el bloque completo del stream codificado
            BitSet bloque = new BitSet(tamBloque);
            for (int i = 0; i < tamBloque; i++) {
                bloque.set(i, streamCodificado.get(b * tamBloque + i));
            }

            // 2. Calcular síndrome por XOR directo de posiciones
            int sindrome = calcularSindrome(bloque, tamBloque, bitsControl);

            // 3. Corregir si se pidió Y hay error (sindrome != 0)
            if (corregir && sindrome != 0) {
                // sindrome - 1 porque las posiciones Hamming son 1-based
                // pero el BitSet es 0-based
                int posError = sindrome - 1;
                if (posError < tamBloque) {
                    bloque.flip(posError);
                }
            }

            // 4. Extraer solo los bits de información (descartar paridades)
            BitSet soloInfo = extraerInfo(bloque, tamBloque, bitsInfo);

            // 5. Agregar al stream recuperado
            streamRecuperado = bitUtilities.concatBits(streamRecuperado, soloInfo, b * bitsInfo, bitsInfo);
        }

        // Convertir stream recuperado a bytes
        int totalBytesRecuperados = (int) Math.ceil((double) totalBitsRecuperados / 8);
        byte[] resultado = bitsetToBytes(streamRecuperado, totalBytesRecuperados);

        // Recortar al tamaño original si se conoce (elimina padding)
        if (cantBytesOriginal > 0 && cantBytesOriginal <= resultado.length) {
            byte[] recortado = new byte[cantBytesOriginal];
            System.arraycopy(resultado, 0, recortado, 0, cantBytesOriginal);
            return recortado;
        }

        return resultado;
    }

    // =========================================================================
    // CÁLCULO DE PARIDADES por XOR directo (encode)
    // =========================================================================

    /**
     * Calcula y coloca los bits de paridad en un bloque Hamming usando XOR directo.
     *
     * Cada bit de paridad está en una posición que es potencia de 2: 1, 2, 4, 8...
     * Un bit de paridad en posición P cubre todas las posiciones del bloque
     * que tienen el bit correspondiente a P activo en su representación binaria.
     *
     * Ejemplo para bloque de 8 bits (posiciones 1 a 8):
     *   P1 (001) cubre posiciones: 1, 3, 5, 7  → tienen bit 0 en 1
     *   P2 (010) cubre posiciones: 2, 3, 6, 7  → tienen bit 1 en 1
     *   P4 (100) cubre posiciones: 4, 5, 6, 7  → tienen bit 2 en 1
     *   P8 no cubre nada en un bloque de 8 (sería posición 9 en adelante)
     *
     * El valor de cada paridad = XOR de todos los bits que cubre (paridad par).
     *
     * @param bloque     BitSet con info ya distribuida en posiciones no-paridad (0-based)
     * @param tamBloque  Tamaño total del bloque
     * @param bitsControl Cantidad de bits de paridad
     * @return           El mismo bloque con los bits de paridad calculados
     */
    private static BitSet calcularParidades(BitSet bloque, int tamBloque, int bitsControl) {

        // Recorremos cada posición de paridad (potencias de 2: 1, 2, 4, 8, ...)
        for (int p = 0; p < bitsControl; p++) {
            int posParidad = (1 << p); // potencia de 2: 1, 2, 4, 8, 16...

            boolean xorAcum = false;

            // Recorremos todas las posiciones del bloque (1-based)
            for (int pos = 1; pos <= tamBloque; pos++) {
                if (pos == posParidad) continue; // no incluir la paridad en su propio cálculo

                // Si esta posición tiene el bit p activo → está cubierta por esta paridad
                if ((pos & posParidad) != 0) {
                    xorAcum ^= bloque.get(pos - 1); // pos-1 porque BitSet es 0-based
                }
            }

            // Guardar el resultado en la posición de paridad (0-based)
            bloque.set(posParidad - 1, xorAcum);
        }

        return bloque;
    }

    // =========================================================================
    // CÁLCULO DE SÍNDROME por XOR directo (decode)
    // =========================================================================

    /**
     * Calcula el síndrome de un bloque Hamming usando XOR directo de posiciones.
     *
     * Es exactamente la misma lógica que calcularParidades pero aplicada al
     * bloque completo recibido (incluyendo sus bits de paridad).
     *
     * Para cada bit de paridad P en posición 2^i:
     *   S_i = XOR de todos los bits en posiciones que tienen el bit i activo
     *         (incluyendo la propia paridad esta vez)
     *
     * Si el bloque llegó sin error  → todos los S_i = 0 → síndrome = 0
     * Si hay un error en posición E → los bits S_i forman la representación
     *   binaria de E → síndrome = E (1-based)
     *
     * @param bloque     BitSet del bloque completo recibido
     * @param tamBloque  Tamaño total del bloque
     * @param bitsControl Cantidad de bits de paridad
     * @return           Síndrome: 0 si no hay error, o posición del error (1-based)
     */
    private static int calcularSindrome(BitSet bloque, int tamBloque, int bitsControl) {

        int sindrome = 0;

        for (int p = 0; p < bitsControl; p++) {
            int posParidad = (1 << p); // 1, 2, 4, 8, ...

            boolean xorAcum = false;

            // Esta vez SÍ incluimos la posición de paridad en el XOR
            for (int pos = 1; pos <= tamBloque; pos++) {
                if ((pos & posParidad) != 0) {
                    xorAcum ^= bloque.get(pos - 1);
                }
            }

            // Si este bit del síndrome es 1, activamos el bit p en el resultado
            if (xorAcum) {
                sindrome |= posParidad;
            }
        }

        return sindrome; // 0 = sin error, N = posición del bit erróneo (1-based)
    }

    // =========================================================================
    // EXTRACCIÓN DE INFORMACIÓN (decode)
    // =========================================================================

    /**
     * Dado un bloque Hamming completo (con paridades), extrae solo los bits
     * de información descartando las posiciones de paridad (potencias de 2).
     *
     * @param bloque    BitSet del bloque completo
     * @param tamBloque Tamaño total del bloque
     * @param bitsInfo  Cantidad de bits de información a extraer
     * @return          BitSet con solo los bitsInfo bits de información
     */
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

    // =========================================================================
    // HELPERS DE CONFIGURACIÓN
    // =========================================================================

    /** Retorna el tamaño total del bloque según el índice. */
    public static int getTamBloque(int blockIndex) {
        switch (blockIndex) {
            case FileManagement.BLOCK_8:     return TAM_BLOQUE_8;
            case FileManagement.BLOCK_1024:  return TAM_BLOQUE_1024;
            case FileManagement.BLOCK_16384: return TAM_BLOQUE_16384;
            default: throw new IllegalArgumentException("blockIndex inválido: " + blockIndex);
        }
    }

    /** Retorna la cantidad de bits de control para el índice dado. */
    public static int getBitsControl(int blockIndex) {
        switch (blockIndex) {
            case FileManagement.BLOCK_8:     return CTRL_8;
            case FileManagement.BLOCK_1024:  return CTRL_1024;
            case FileManagement.BLOCK_16384: return CTRL_16384;
            default: throw new IllegalArgumentException("blockIndex inválido: " + blockIndex);
        }
    }

    /** Retorna la cantidad de bits de información para el índice dado. */
    public static int getBitsInfo(int blockIndex) {
        return getTamBloque(blockIndex) - getBitsControl(blockIndex);
    }

    // =========================================================================
    // CONVERSIÓN BitSet → byte[]
    // =========================================================================

    /**
     * Convierte un BitSet a un array de bytes de longitud 'cantBytes'.
     * Recorre el BitSet de a 8 bits armando cada byte (MSB primero).
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
            resultado[i] = (byte) (valor > 127 ? valor - 256 : valor);
        }
        return resultado;
    }
}