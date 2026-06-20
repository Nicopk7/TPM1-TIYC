package hamming.file_mngmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileManagement {

    // ── Extensiones Hamming ──────────────────────────────────────────────────
    public static final String[] EXT_HAMMING   = {".HA1", ".HA2", ".HA3"};
    public static final String[] EXT_ERROR     = {".HE1", ".HE2", ".HE3"};
    public static final String[] EXT_ERROR2    = {".H21", ".H22", ".H23"}; // 2 errores por bloque
    public static final String[] EXT_DEC_ERR   = {".DE1", ".DE2", ".DE3"};
    public static final String[] EXT_DEC_CORR  = {".DC1", ".DC2", ".DC3"};

    // ── Extensiones Huffman ──────────────────────────────────────────────────
    public static final String EXT_HUFFMAN     = ".huf";
    public static final String EXT_HUFFMAN_DEC = ".dhu";

    // ── Extensiones Lab 3: pipeline Huffman → Hamming ────────────────────────
    // Archivo comprimido Huffman y luego protegido con Hamming
    public static final String[] EXT_HUF_HAM  = {".PH1", ".PH2", ".PH3"}; // Protected Huffman
    public static final String[] EXT_HUF_ERR  = {".PE1", ".PE2", ".PE3"}; // con error simple
    public static final String[] EXT_HUF_ERR2 = {".P21", ".P22", ".P23"}; // con doble error
    public static final String[] EXT_HUF_DEC  = {".PD1", ".PD2", ".PD3"}; // desprotegido (sin decomp)
    public static final String   EXT_HUF_FINAL = ".rec";                   // recuperado final

    // ── Índices de bloque ────────────────────────────────────────────────────
    public static final int BLOCK_8     = 0;
    public static final int BLOCK_1024  = 1;
    public static final int BLOCK_16384 = 2;

    // ── Lectura / escritura básica ────────────────────────────────────────────

    public static byte[] readFile(String path) {
        try { return Files.readAllBytes(Paths.get(path)); }
        catch (IOException e) { System.out.println("Error al leer: " + e.getMessage()); return null; }
    }

    public static boolean writeFile(String path, byte[] data) {
        try { Files.write(Paths.get(path), data); return true; }
        catch (IOException e) { System.out.println("Error al escribir: " + e.getMessage()); return false; }
    }

    // ── Helpers de nombres ────────────────────────────────────────────────────

    public static String getBaseName(String path) {
        int dot = path.lastIndexOf('.');
        return dot == -1 ? path : path.substring(0, dot);
    }

    public static String getExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot == -1 ? "" : path.substring(dot).toUpperCase();
    }

    // ── Hamming paths ─────────────────────────────────────────────────────────

    public static String buildHammingPath(String orig, int blockIndex) {
        return getBaseName(orig) + EXT_HAMMING[blockIndex];
    }

    public static String buildErrorPath(String hammingPath) {
        String base = getBaseName(hammingPath);
        int idx = getBlockIndexFromExtension(hammingPath);
        return idx == -1 ? null : base + EXT_ERROR[idx];
    }

    public static String buildError2Path(String hammingPath) {
        String base = getBaseName(hammingPath);
        int idx = getBlockIndexFromExtension(hammingPath);
        return idx == -1 ? null : base + EXT_ERROR2[idx];
    }

    public static String buildDecodedErrorPath(String inputPath) {
        String base = getBaseName(inputPath);
        int idx = getBlockIndexFromExtension(inputPath);
        return idx == -1 ? null : base + EXT_DEC_ERR[idx];
    }

    public static String buildDecodedCorrectedPath(String inputPath) {
        String base = getBaseName(inputPath);
        int idx = getBlockIndexFromExtension(inputPath);
        return idx == -1 ? null : base + EXT_DEC_CORR[idx];
    }

    public static boolean isHammingFile(String path) { return getBlockIndexFromExtension(path) != -1; }

    public static int getBlockSizeBits(int blockIndex) {
        switch (blockIndex) {
            case BLOCK_8:     return 8;
            case BLOCK_1024:  return 1024;
            case BLOCK_16384: return 16384;
            default: throw new IllegalArgumentException("blockIndex inválido");
        }
    }

    // ── Hamming save helpers ──────────────────────────────────────────────────

    public static String saveHammingFile(String txtPath, int blockIndex, byte[] encoded) {
        String out = buildHammingPath(txtPath, blockIndex);
        return writeFile(out, encoded) ? out : null;
    }

    public static String saveErrorFile(String hammingPath, byte[] withErrors) {
        String out = buildErrorPath(hammingPath);
        return (out != null && writeFile(out, withErrors)) ? out : null;
    }

    public static String saveError2File(String hammingPath, byte[] withErrors) {
        String out = buildError2Path(hammingPath);
        return (out != null && writeFile(out, withErrors)) ? out : null;
    }

    public static String saveDecodedError(String inputPath, byte[] decoded) {
        String out = buildDecodedErrorPath(inputPath);
        return (out != null && writeFile(out, decoded)) ? out : null;
    }

    public static String saveDecodedCorrected(String inputPath, byte[] decoded) {
        String out = buildDecodedCorrectedPath(inputPath);
        return (out != null && writeFile(out, decoded)) ? out : null;
    }

    // ── Huffman paths / helpers ───────────────────────────────────────────────

    public static String buildHuffmanPath(String orig) {
        return getBaseName(orig) + EXT_HUFFMAN;
    }

    public static String buildHuffmanDecPath(String hufPath) {
        return getBaseName(hufPath) + EXT_HUFFMAN_DEC;
    }

    public static String saveHuffmanFile(String orig, byte[] compressed) {
        String out = buildHuffmanPath(orig);
        return writeFile(out, compressed) ? out : null;
    }

    public static String saveHuffmanDecFile(String hufPath, byte[] decompressed) {
        String out = buildHuffmanDecPath(hufPath);
        return writeFile(out, decompressed) ? out : null;
    }

    public static boolean isHuffmanFile(String path) {
        return getExtension(path).equalsIgnoreCase(EXT_HUFFMAN);
    }

    // ── Lab3: pipeline Huffman+Hamming paths ─────────────────────────────────

    public static String buildPipelineHamPath(String orig, int blockIndex) {
        return getBaseName(orig) + EXT_HUF_HAM[blockIndex];
    }

    public static String buildPipelineErrPath(String phPath) {
        int idx = getPipelineBlockIndex(phPath);
        return idx == -1 ? null : getBaseName(phPath) + EXT_HUF_ERR[idx];
    }

    public static String buildPipelineErr2Path(String phPath) {
        int idx = getPipelineBlockIndex(phPath);
        return idx == -1 ? null : getBaseName(phPath) + EXT_HUF_ERR2[idx];
    }

    public static String buildPipelineDecPath(String phPath) {
        int idx = getPipelineBlockIndex(phPath);
        return idx == -1 ? null : getBaseName(phPath) + EXT_HUF_DEC[idx];
    }

    public static String buildPipelineFinalPath(String orig) {
        return getBaseName(orig) + EXT_HUF_FINAL;
    }

    public static boolean isPipelineFile(String path) { return getPipelineBlockIndex(path) != -1; }

    public static int getPipelineBlockIndex(String path) {
        String ext = getExtension(path);
        for (int i = 0; i < 3; i++) {
            if (ext.equals(EXT_HUF_HAM[i].toUpperCase())
                    || ext.equals(EXT_HUF_ERR[i].toUpperCase())
                    || ext.equals(EXT_HUF_ERR2[i].toUpperCase())
                    || ext.equals(EXT_HUF_DEC[i].toUpperCase()))
                return i;
        }
        return -1;
    }

    // ── Helper interno ────────────────────────────────────────────────────────

    private static int getBlockIndexFromExtension(String path) {
        String ext = getExtension(path);
        for (int i = 0; i < 3; i++) {
            if (ext.equals(EXT_HAMMING[i])  || ext.equals(EXT_ERROR[i])
                    || ext.equals(EXT_ERROR2[i])   || ext.equals(EXT_DEC_ERR[i])
                    || ext.equals(EXT_DEC_CORR[i])) return i;
        }
        return -1;
    }
}
