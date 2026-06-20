package hamming;

import hamming.file_mngmt.FileManagement;

/**
 * Codec Hamming — codificación y decodificación en un solo lugar.
 *
 * Reescrito con arrays de int[] para el manejo de bits, eliminando la
 * dependencia de BitSet que causaba desalineamiento en bloques grandes.
 *
 * Soporta tres tamaños de módulo total (info + control):
 *   BLOCK_8     →  4 bits info + 4 bits control  (módulo de  8 bits)  → .HA1
 *   BLOCK_1024  → 1014 bits info + 10 bits control (módulo de 1024 bits) → .HA2
 *   BLOCK_16384 → 16370 bits info + 14 bits control (módulo de 16384 bits) → .HA3
 *
 * ── ENCODE ──────────────────────────────────────────────────────────────────
 *   1. bytesToBits()       → archivo completo como int[] de 0s y 1s
 *   2. repartirInfo()      → ubica bits de info en posiciones no-paridad
 *   3. calcularParidades() → XOR directo por posición → llena bits de control
 *   4. bitsToBytes()       → stream codificado → bytes → .HAx
 *
 * ── DECODE ──────────────────────────────────────────────────────────────────
 *   1. bytesToBits()       → archivo .HAx o .HEx como int[]
 *   2. calcularSindrome()  → XOR directo sobre bloque recibido
 *                            síndrome == 0: sin error
 *                            síndrome != 0: posición del bit erróneo (1-based)
 *   3. corregir (opcional) → flipea el bit indicado por el síndrome
 *   4. extraerInfo()       → descarta posiciones de paridad
 *   5. bitsToBytes()       → bytes recuperados → .DCx o .DEx
 */
public class Hamming {

    // =========================================================================
    // CONSTANTES DE CONFIGURACIÓN
    // =========================================================================

    public static final int TAM_BLOQUE_8     = 8;
    public static final int TAM_BLOQUE_1024  = 1024;
    public static final int TAM_BLOQUE_16384 = 16384;

    // IMPORTANTE: para un bloque de N bits, la posición N es paridad si N es potencia de 2.
    // TAM=1024=2^10 → posiciones paridad: 1,2,4,8,16,32,64,128,256,512,1024 → 11 bits control
    // TAM=16384=2^14 → posiciones paridad: 1,2,...,8192,16384 → 15 bits control
    public static final int CTRL_8     = 4;
    public static final int CTRL_1024  = 11;
    public static final int CTRL_16384 = 15;

    public static final int INFO_8     = TAM_BLOQUE_8     - CTRL_8;      //  4
    public static final int INFO_1024  = TAM_BLOQUE_1024  - CTRL_1024;   // 1013
    public static final int INFO_16384 = TAM_BLOQUE_16384 - CTRL_16384;  // 16369

    // =========================================================================
    // ENCODE
    // =========================================================================

    public static byte[] encode(byte[] datos, int blockIndex) {

        int tamBloque   = getTamBloque(blockIndex);
        int bitsControl = getBitsControl(blockIndex);
        int bitsInfo    = tamBloque - bitsControl;

        // Convertir archivo completo a stream de bits
        int[] streamInfo  = bytesToBits(datos);
        int totalBitsInfo = streamInfo.length;

        int cantBloques          = (int) Math.ceil((double) totalBitsInfo / bitsInfo);
        int totalBitsCodificados = cantBloques * tamBloque;

        int[] streamCodificado = new int[totalBitsCodificados];

        for (int b = 0; b < cantBloques; b++) {

            // 1. Extraer bitsInfo bits de este bloque (con padding de 0 si es el último)
            int[] bloqueInfo = new int[bitsInfo];
            int bitsDisponibles = Math.min(bitsInfo, totalBitsInfo - b * bitsInfo);
            for (int i = 0; i < bitsDisponibles; i++) {
                bloqueInfo[i] = streamInfo[b * bitsInfo + i];
            }
            // bits restantes quedan en 0 (padding)

            // 2. Distribuir info en posiciones no-paridad del bloque Hamming
            int[] bloqueHamming = repartirInfo(bloqueInfo, tamBloque);

            // 3. Calcular paridades por XOR directo
            calcularParidades(bloqueHamming, tamBloque, bitsControl);

            // 4. Copiar bloque al stream de salida
            System.arraycopy(bloqueHamming, 0, streamCodificado, b * tamBloque, tamBloque);
        }

        // Encabezado de 12 bytes:
        //   [0-3]  cantBloques   (int)
        //   [4-7]  totalBitsInfo (int)
        //   [8-11] fechaApertura (int, epoch seconds, 0 = sin restricción)
        //   [12..] stream codificado
        byte[] streamBytes = bitsToBytes(streamCodificado);
        byte[] resultado   = new byte[12 + streamBytes.length];

        // cantBloques → bytes 0-3
        resultado[0] = (byte)(cantBloques   >> 24);
        resultado[1] = (byte)(cantBloques   >> 16);
        resultado[2] = (byte)(cantBloques   >>  8);
        resultado[3] = (byte)(cantBloques);
        // totalBitsInfo → bytes 4-7
        resultado[4] = (byte)(totalBitsInfo >> 24);
        resultado[5] = (byte)(totalBitsInfo >> 16);
        resultado[6] = (byte)(totalBitsInfo >>  8);
        resultado[7] = (byte)(totalBitsInfo);
        // fechaApertura → bytes 8-11 (0 = sin restricción)
        resultado[8]  = 0;
        resultado[9]  = 0;
        resultado[10] = 0;
        resultado[11] = 0;

        System.arraycopy(streamBytes, 0, resultado, 12, streamBytes.length);
        return resultado;
    }

    // =========================================================================
    // DECODE
    // =========================================================================

    public static byte[] decode(byte[] datos, int blockIndex, boolean corregir, int cantBytesOriginal) {

        int tamBloque   = getTamBloque(blockIndex);
        int bitsControl = getBitsControl(blockIndex);
        int bitsInfo    = tamBloque - bitsControl;

        // Encabezado de 12 bytes: [cantBloques 4B][totalBitsInfo 4B][fechaApertura 4B]
        int cantBloques = ((datos[0] & 0xFF) << 24) | ((datos[1] & 0xFF) << 16)
                | ((datos[2] & 0xFF) <<  8) |  (datos[3] & 0xFF);
        int totalBitsInfo = ((datos[4] & 0xFF) << 24) | ((datos[5] & 0xFF) << 16)
                | ((datos[6] & 0xFF) <<  8) |  (datos[7] & 0xFF);
        // fechaApertura en bytes [8-11] — verificada por ErrorInjector.verificarFechaApertura()

        // Stream real: saltar los 12 bytes del encabezado
        byte[] streamSinHeader = new byte[datos.length - 12];
        System.arraycopy(datos, 12, streamSinHeader, 0, streamSinHeader.length);
        int[] streamCodificado = bytesToBits(streamSinHeader);

        int[] streamRecuperado = new int[cantBloques * bitsInfo];

        for (int b = 0; b < cantBloques; b++) {

            // 1. Extraer bloque completo
            int[] bloque = new int[tamBloque];
            System.arraycopy(streamCodificado, b * tamBloque, bloque, 0, tamBloque);

            // 2. Calcular síndrome
            int sindrome = calcularSindrome(bloque, tamBloque, bitsControl);

            // 3. Corregir si corresponde
            if (corregir && sindrome != 0) {
                int posError = sindrome - 1; // síndrome es 1-based, array es 0-based
                if (posError < tamBloque) {
                    bloque[posError] ^= 1;   // flip del bit erróneo
                }
            }

            // 4. Extraer solo bits de información
            int[] soloInfo = extraerInfo(bloque, tamBloque, bitsInfo);

            // 5. Agregar al stream recuperado
            System.arraycopy(soloInfo, 0, streamRecuperado, b * bitsInfo, bitsInfo);
        }

        byte[] resultado = bitsToBytes(streamRecuperado);

        // Recortar padding usando totalBitsInfo del encabezado (fuente de verdad)
        // Esto elimina los bits de relleno del último bloque con exactitud.
        int bytesReales = (int) Math.ceil((double) totalBitsInfo / 8);
        if (bytesReales > 0 && bytesReales <= resultado.length) {
            byte[] recortado = new byte[bytesReales];
            System.arraycopy(resultado, 0, recortado, 0, bytesReales);
            return recortado;
        }

        return resultado;
    }

    // =========================================================================
    // LÓGICA HAMMING
    // =========================================================================

    /**
     * Distribuye los bits de información en las posiciones no-paridad.
     * Las posiciones de paridad (potencias de 2: 1,2,4,8,...) quedan en 0.
     *
     * @param info      bits de información (indexados desde 0)
     * @param tamBloque tamaño total del bloque Hamming
     * @return          array de tamBloque bits con info distribuida
     */
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

    /**
     * Calcula y coloca los bits de paridad usando XOR directo de posiciones.
     *
     * La paridad en posición 2^p cubre todas las posiciones del bloque
     * cuya representación binaria tiene el bit p activo.
     * No incluye la propia posición de paridad en el cálculo.
     *
     * Modifica el array bloque in-place.
     */
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

    /**
     * Calcula el síndrome de un bloque recibido.
     *
     * Igual que calcularParidades pero SÍ incluye la posición de paridad.
     * Si síndrome == 0 → no hay error.
     * Si síndrome != 0 → indica la posición del bit erróneo (1-based).
     */
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

    /**
     * Extrae solo los bits de información de un bloque Hamming completo,
     * descartando las posiciones de paridad (potencias de 2).
     */
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

    // =========================================================================
    // CONVERSIÓN bytes ↔ bits
    // =========================================================================

    /**
     * Convierte un array de bytes a un array de bits (int[] con 0s y 1s).
     * MSB primero dentro de cada byte.
     * Ejemplo: byte 0b10110010 → [1,0,1,1,0,0,1,0]
     */
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

    /**
     * Convierte un array de bits (int[] con 0s y 1s) a bytes.
     * Rellena con 0s si la longitud no es múltiplo de 8.
     */
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

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** Detecta si n es potencia de 2 usando enmascaramiento. */
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