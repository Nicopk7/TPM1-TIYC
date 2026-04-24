package hamming.file_mngmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Maneja toda la lectura/escritura de archivos y la gestión de extensiones.
 *
 * Extensiones del proyecto:
 *   .HA1 / .HA2 / .HA3  → Archivo protegido con Hamming (bloques 8 / 1024 / 16384 bits)
 *   .HE1 / .HE2 / .HE3  → Archivo protegido con errores introducidos
 *   .DE1 / .DE2 / .DE3  → Archivo decodificado CON errores (sin corrección)
 *   .DC1 / .DC2 / .DC3  → Archivo decodificado y CORREGIDO
 */
public class FileManagement {

    // -------------------------------------------------------------------------
    // Constantes de extensiones
    // -------------------------------------------------------------------------

    public static final String[] EXT_HAMMING   = {".HA1", ".HA2", ".HA3"};
    public static final String[] EXT_ERROR     = {".HE1", ".HE2", ".HE3"};
    public static final String[] EXT_DEC_ERR   = {".DE1", ".DE2", ".DE3"};
    public static final String[] EXT_DEC_CORR  = {".DC1", ".DC2", ".DC3"};

    /** Índice de bloque: 0 = 8 bits, 1 = 1024 bits, 2 = 16384 bits */
    public static final int BLOCK_8     = 0;
    public static final int BLOCK_1024  = 1;
    public static final int BLOCK_16384 = 2;

    // -------------------------------------------------------------------------
    // Lectura y escritura básica
    // -------------------------------------------------------------------------

    /**
     * Lee un archivo completo en modo binario y retorna sus bytes.
     * Equivalente a fopen(path, "rb") de C.
     *
     * @param path Ruta del archivo
     * @return Array de bytes del archivo, o null si hubo error
     */
    public static byte[] readFile(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            System.out.println("Error al leer el archivo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Escribe un array de bytes en un archivo (modo binario).
     *
     * @param path      Ruta destino
     * @param dataBytes Datos a escribir
     * @return true si la escritura fue exitosa
     */
    public static boolean writeFile(String path, byte[] dataBytes) {
        try {
            Files.write(Paths.get(path), dataBytes);
            return true;
        } catch (IOException e) {
            System.out.println("Error al escribir en el archivo: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Gestión de extensiones
    // -------------------------------------------------------------------------

    /**
     * Quita la extensión de un path y retorna solo el nombre base.
     * Ejemplo: "C:/docs/texto.txt" → "C:/docs/texto"
     */
    public static String getBaseName(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1) return path;
        return path.substring(0, dotIndex);
    }

    /**
     * Retorna la extensión de un archivo en mayúsculas.
     * Ejemplo: "texto.HA1" → ".HA1"
     */
    public static String getExtension(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return path.substring(dotIndex).toUpperCase();
    }

    /**
     * Construye el path del archivo Hamming protegido.
     * Ejemplo: "texto.txt", BLOCK_8 → "texto.HA1"
     *
     * @param originalPath Path del .txt original
     * @param blockIndex   BLOCK_8, BLOCK_1024 o BLOCK_16384
     */
    public static String buildHammingPath(String originalPath, int blockIndex) {
        return getBaseName(originalPath) + EXT_HAMMING[blockIndex];
    }

    /**
     * Construye el path del archivo con errores introducidos.
     * Ejemplo: "texto.HA1" → "texto.HE1"
     */
    public static String buildErrorPath(String hammingPath) {
        String base = getBaseName(hammingPath);
        int blockIndex = getBlockIndexFromExtension(hammingPath);
        if (blockIndex == -1) {
            System.out.println("Extensión no reconocida: " + hammingPath);
            return null;
        }
        return base + EXT_ERROR[blockIndex];
    }

    /**
     * Construye el path del archivo decodificado CON errores (sin corregir).
     * Ejemplo: "texto.HA1" o "texto.HE1" → "texto.DE1"
     */
    public static String buildDecodedErrorPath(String inputPath) {
        String base = getBaseName(inputPath);
        int blockIndex = getBlockIndexFromExtension(inputPath);
        if (blockIndex == -1) {
            System.out.println("Extensión no reconocida: " + inputPath);
            return null;
        }
        return base + EXT_DEC_ERR[blockIndex];
    }

    /**
     * Construye el path del archivo decodificado y CORREGIDO.
     * Ejemplo: "texto.HA1" o "texto.HE1" → "texto.DC1"
     */
    public static String buildDecodedCorrectedPath(String inputPath) {
        String base = getBaseName(inputPath);
        int blockIndex = getBlockIndexFromExtension(inputPath);
        if (blockIndex == -1) {
            System.out.println("Extensión no reconocida: " + inputPath);
            return null;
        }
        return base + EXT_DEC_CORR[blockIndex];
    }

    /**
     * Detecta si un path es un archivo Hamming válido (.HAx o .HEx).
     */
    public static boolean isHammingFile(String path) {
        return getBlockIndexFromExtension(path) != -1;
    }

    /**
     * Retorna el tamaño de bloque en bits según el índice.
     * 0 → 8, 1 → 1024, 2 → 16384
     */
    public static int getBlockSizeBits(int blockIndex) {
        switch (blockIndex) {
            case BLOCK_8:     return 8;
            case BLOCK_1024:  return 1024;
            case BLOCK_16384: return 16384;
            default: throw new IllegalArgumentException("blockIndex inválido: " + blockIndex);
        }
    }

    // -------------------------------------------------------------------------
    // Operaciones de alto nivel (leer y guardar con extensión automática)
    // -------------------------------------------------------------------------

    /**
     * Lee un archivo .txt y lo guarda como .HAx con Hamming aplicado.
     * La codificación real la hace HammingEncoder; este método maneja los paths.
     *
     * @param txtPath    Path del archivo .txt fuente
     * @param blockIndex Tamaño de bloque
     * @param encoded    Bytes ya codificados por HammingEncoder
     * @return Path donde se guardó el archivo, o null si falló
     */
    public static String saveHammingFile(String txtPath, int blockIndex, byte[] encoded) {
        String outPath = buildHammingPath(txtPath, blockIndex);
        if (writeFile(outPath, encoded)) {
            System.out.println("Archivo protegido guardado: " + outPath);
            return outPath;
        }
        return null;
    }

    /**
     * Guarda el archivo con errores introducidos (.HEx).
     *
     * @param hammingPath Path del .HAx fuente
     * @param withErrors  Bytes con errores inyectados por ErrorInjector
     * @return Path donde se guardó, o null si falló
     */
    public static String saveErrorFile(String hammingPath, byte[] withErrors) {
        String outPath = buildErrorPath(hammingPath);
        if (outPath != null && writeFile(outPath, withErrors)) {
            System.out.println("Archivo con errores guardado: " + outPath);
            return outPath;
        }
        return null;
    }

    /**
     * Guarda el archivo decodificado SIN corrección (.DEx).
     */
    public static String saveDecodedError(String inputPath, byte[] decoded) {
        String outPath = buildDecodedErrorPath(inputPath);
        if (outPath != null && writeFile(outPath, decoded)) {
            System.out.println("Archivo decodificado (con errores) guardado: " + outPath);
            return outPath;
        }
        return null;
    }

    /**
     * Guarda el archivo decodificado y CORREGIDO (.DCx).
     */
    public static String saveDecodedCorrected(String inputPath, byte[] decoded) {
        String outPath = buildDecodedCorrectedPath(inputPath);
        if (outPath != null && writeFile(outPath, decoded)) {
            System.out.println("Archivo decodificado y corregido guardado: " + outPath);
            return outPath;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------------

    /**
     * Dado un path con extensión .HA1/.HA2/.HA3/.HE1/.HE2/.HE3/.DE1... etc.,
     * retorna el índice de bloque (0, 1 o 2), o -1 si no reconoce la extensión.
     */
    private static int getBlockIndexFromExtension(String path) {
        String ext = getExtension(path);
        for (int i = 0; i < 3; i++) {
            if (ext.equals(EXT_HAMMING[i])  || ext.equals(EXT_ERROR[i])    || ext.equals(EXT_DEC_ERR[i])  || ext.equals(EXT_DEC_CORR[i])) {
                return i;
            }
        }
        return -1;
    }
}
