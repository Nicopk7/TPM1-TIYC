package hamming.file_mngmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileManagement {

    // Constantes de extensiones
    public static final String[] EXT_HAMMING = {".HA1", ".HA2", ".HA3"};
    public static final String[] EXT_ERROR = {".HE1", ".HE2", ".HE3"};
    public static final String[] EXT_DEC_ERR = {".DE1", ".DE2", ".DE3"};
    public static final String[] EXT_DEC_CORR = {".DC1", ".DC2", ".DC3"};

    // Extensiones Huffman
    public static final String EXT_HUFFMAN = ".huf";
    public static final String EXT_HUFFMAN_DEC = ".dhu";

    public static final int BLOCK_8 = 0;
    public static final int BLOCK_1024 = 1;
    public static final int BLOCK_16384 = 2;

    // Lectura y escritura básica


    public static byte[] readFile(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            System.out.println("Error al leer el archivo: " + e.getMessage());
            return null;
        }
    }

    public static boolean writeFile(String path, byte[] dataBytes) {
        try {
            Files.write(Paths.get(path), dataBytes);
            return true;
        } catch (IOException e) {
            System.out.println("Error al escribir en el archivo: " + e.getMessage());
            return false;
        }
    }

    // Gestión de extensiones

    public static String getBaseName(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1) return path;
        return path.substring(0, dotIndex);
    }


    public static String getExtension(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return path.substring(dotIndex).toUpperCase();
    }


    public static String buildHammingPath(String originalPath, int blockIndex) {
        return getBaseName(originalPath) + EXT_HAMMING[blockIndex];
    }


    public static String buildErrorPath(String hammingPath) {
        String base = getBaseName(hammingPath);
        int blockIndex = getBlockIndexFromExtension(hammingPath);
        if (blockIndex == -1) {
            System.out.println("Extensión no reconocida: " + hammingPath);
            return null;
        }
        return base + EXT_ERROR[blockIndex];
    }


    public static String buildDecodedErrorPath(String inputPath) {
        String base = getBaseName(inputPath);
        int blockIndex = getBlockIndexFromExtension(inputPath);
        if (blockIndex == -1) {
            System.out.println("Extensión no reconocida: " + inputPath);
            return null;
        }
        return base + EXT_DEC_ERR[blockIndex];
    }


    public static String buildDecodedCorrectedPath(String inputPath) {
        String base = getBaseName(inputPath);
        int blockIndex = getBlockIndexFromExtension(inputPath);
        if (blockIndex == -1) {
            System.out.println("Extensión no reconocida: " + inputPath);
            return null;
        }
        return base + EXT_DEC_CORR[blockIndex];
    }


    public static boolean isHammingFile(String path) {
        return getBlockIndexFromExtension(path) != -1;
    }


    public static int getBlockSizeBits(int blockIndex) {
        switch (blockIndex) {
            case BLOCK_8:
                return 8;
            case BLOCK_1024:
                return 1024;
            case BLOCK_16384:
                return 16384;
            default:
                throw new IllegalArgumentException("blockIndex inválido: " + blockIndex);
        }
    }

    // Operaciones de alto nivel (leer y guardar con extensión automática)


    public static String saveHammingFile(String txtPath, int blockIndex, byte[] encoded) {
        String outPath = buildHammingPath(txtPath, blockIndex);
        if (writeFile(outPath, encoded)) {
            System.out.println("Archivo protegido guardado: " + outPath);
            return outPath;
        }
        return null;
    }


    public static String saveErrorFile(String hammingPath, byte[] withErrors) {
        String outPath = buildErrorPath(hammingPath);
        if (outPath != null && writeFile(outPath, withErrors)) {
            System.out.println("Archivo con errores guardado: " + outPath);
            return outPath;
        }
        return null;
    }


    public static String saveDecodedError(String inputPath, byte[] decoded) {
        String outPath = buildDecodedErrorPath(inputPath);
        if (outPath != null && writeFile(outPath, decoded)) {
            System.out.println("Archivo decodificado (con errores) guardado: " + outPath);
            return outPath;
        }
        return null;
    }


    public static String saveDecodedCorrected(String inputPath, byte[] decoded) {
        String outPath = buildDecodedCorrectedPath(inputPath);
        if (outPath != null && writeFile(outPath, decoded)) {
            System.out.println("Archivo decodificado y corregido guardado: " + outPath);
            return outPath;
        }
        return null;
    }

    // Helpers internos


    private static int getBlockIndexFromExtension(String path) {
        String ext = getExtension(path);
        for (int i = 0; i < 3; i++) {
            if (ext.equals(EXT_HAMMING[i]) || ext.equals(EXT_ERROR[i]) || ext.equals(EXT_DEC_ERR[i]) || ext.equals(EXT_DEC_CORR[i])) {
                return i;
            }
        }
        return -1;
    }



// Operaciones Huffman


    public static String buildHuffmanPath(String originalPath) {
        return getBaseName(originalPath) + EXT_HUFFMAN;
    }


    public static String buildHuffmanDecPath(String hufPath) {
        return getBaseName(hufPath) + EXT_HUFFMAN_DEC;
    }


    public static String saveHuffmanFile(String originalPath, byte[] compressed) {
        String outPath = buildHuffmanPath(originalPath);
        if (writeFile(outPath, compressed)) {
            System.out.println("Archivo comprimido guardado: " + outPath);
            return outPath;
        }
        return null;
    }


    public static String saveHuffmanDecFile(String hufPath, byte[] decompressed) {
        String outPath = buildHuffmanDecPath(hufPath);
        if (writeFile(outPath, decompressed)) {
            System.out.println("Archivo descomprimido guardado: " + outPath);
            return outPath;
        }
        return null;
    }


    public static boolean isHuffmanFile(String path) {
        return getExtension(path).equalsIgnoreCase(EXT_HUFFMAN);
    }

}
