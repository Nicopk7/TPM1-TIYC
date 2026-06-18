package GUI;

import hamming.Hamming;
import hamming.errorUtilities;
import hamming.file_mngmt.FileManagement;
import huffman.Huffman;
import huffman.HuffmanUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.FileDialog;
import java.io.File;
import java.util.List;
import java.util.Map;

public class MainWindow extends JFrame {

    // ── PALETA ────────────────────────────────────────────────────────────────
    private static final Color BG_BASE      = new Color(0x1E1E1E);
    private static final Color BG_SURFACE   = new Color(0x252526);
    private static final Color BG_ELEVATED  = new Color(0x2D2D2D);
    private static final Color BORDER       = new Color(0x3C3C3C);
    private static final Color TEXT_PRIMARY = new Color(0xD4D4D4);
    private static final Color TEXT_MUTED   = new Color(0x858585);
    private static final Color TEXT_HINT    = new Color(0x555555);
    private static final Color ACCENT_BLUE  = new Color(0x0E639C);
    private static final Color ACCENT_GREEN = new Color(0x1B6B3A);
    private static final Color ACCENT_PURP  = new Color(0x6B3A8B);
    private static final Color SUCCESS      = new Color(0x4EC9B0);
    private static final Color WARNING      = new Color(0xDCDCAA);
    private static final Color DANGER       = new Color(0xF44747);
    private static final Color INFO         = new Color(0x9CDCFE);
    private static final Color GREEN_INFO   = new Color(0x6BBF6B);

    // ── ESTADO ────────────────────────────────────────────────────────────────
    private File   archivoActivo    = null;
    private int    blockIndexActivo = FileManagement.BLOCK_8;
    private byte[] bytesOriginal    = null;

    // ── COMPONENTES ──────────────────────────────────────────────────────────
    private JLabel    lblArchivoActivo;
    private JLabel    lblTamano;
    private JLabel    lblBloque;
    private JTextArea txtIzquierdo;
    private JTextPane txtDerecho;
    private JLabel    lblTituloIzq;
    private JLabel    lblTituloDir;
    private JTextArea txtLog;
    private JButton   btnHA1, btnHA2, btnHA3;

    // ── CONSTRUCTOR ───────────────────────────────────────────────────────────
    public MainWindow() {
        super("Hamming + Huffman — TPM1-TIYC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 720);
        setMinimumSize(new Dimension(960, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_BASE);
        setLayout(new BorderLayout());
        add(buildSidebar(),  BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);
        log("Listo. Cargá un archivo para comenzar.", TEXT_MUTED);
    }

    // ── SIDEBAR ───────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sb = new JPanel();
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setBackground(BG_SURFACE);
        sb.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));
        sb.setPreferredSize(new Dimension(245, 0));

        sb.add(Box.createVerticalStrut(14));

        sb.add(sectionLabel("ARCHIVO"));
        sb.add(Box.createVerticalStrut(5));
        sb.add(sideBtn("Cargar archivo", ACCENT_BLUE, e -> accionCargar()));
        sb.add(Box.createVerticalStrut(14));

        sb.add(sectionLabel("LAB 1 — HAMMING"));
        sb.add(Box.createVerticalStrut(5));
        btnHA1 = sideBtn("Proteger 8 bits  →  .HA1",      BG_ELEVATED, e -> accionProteger(FileManagement.BLOCK_8));
        btnHA2 = sideBtn("Proteger 1024 bits  →  .HA2",   BG_ELEVATED, e -> accionProteger(FileManagement.BLOCK_1024));
        btnHA3 = sideBtn("Proteger 16384 bits  →  .HA3",  BG_ELEVATED, e -> accionProteger(FileManagement.BLOCK_16384));
        sb.add(btnHA1); sb.add(Box.createVerticalStrut(3));
        sb.add(btnHA2); sb.add(Box.createVerticalStrut(3));
        sb.add(btnHA3); sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("Introducir 1 error  →  .HEx",      BG_ELEVATED, e -> accionIntroducirErrores()));
        sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("Desproteger con errores  →  .DEx", BG_ELEVATED, e -> accionDecodificar(false)));
        sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("Desproteger corrigiendo  →  .DCx", BG_ELEVATED, e -> accionDecodificar(true)));
        sb.add(Box.createVerticalStrut(14));

        sb.add(sectionLabel("LAB 2 — HUFFMAN"));
        sb.add(Box.createVerticalStrut(5));
        sb.add(sideBtn("Compactar  →  .huf",    ACCENT_GREEN, e -> accionCompactar()));
        sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("Descompactar  →  .dhu", BG_ELEVATED,  e -> accionDescompactar()));
        sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("Ver estadísticas",       BG_ELEVATED,  e -> accionVerEstadisticas()));
        sb.add(Box.createVerticalStrut(14));

        sb.add(sectionLabel("LAB 3 — PIPELINE COMPLETO"));
        sb.add(Box.createVerticalStrut(5));
        sb.add(sideBtn("▶  Comprimir + Proteger  →  .PHx", ACCENT_PURP, e -> accionPipelineComprimir()));
        sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("Introducir 1 error  →  .PEx",       BG_ELEVATED, e -> accionPipelineError1()));
        sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("Introducir 2 errores  →  .P2x",     BG_ELEVATED, e -> accionPipelineError2()));
        sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("Desproteger sin corregir  →  .PDx", BG_ELEVATED, e -> accionPipelineDescode(false)));
        sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("Desproteger corrigiendo  →  .PDx",  BG_ELEVATED, e -> accionPipelineDescode(true)));
        sb.add(Box.createVerticalStrut(3));
        sb.add(sideBtn("◀  Descomprimir + Recuperar  →  .rec", BG_ELEVATED, e -> accionPipelineRecuperar()));

        sb.add(Box.createVerticalGlue());
        return sb;
    }

    // ── ÁREA PRINCIPAL ────────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG_BASE);
        main.add(buildTopBar(),   BorderLayout.NORTH);
        main.add(buildViewer(),   BorderLayout.CENTER);
        main.add(buildLogPanel(), BorderLayout.SOUTH);
        return main;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setBackground(BG_SURFACE);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        bar.add(label("Archivo activo:", TEXT_MUTED, 12));
        lblArchivoActivo = label("—", TEXT_PRIMARY, 12);
        lblArchivoActivo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        bar.add(lblArchivoActivo);

        lblTamano = badge("—", INFO);
        bar.add(lblTamano);

        bar.add(label("|", TEXT_HINT, 12));
        bar.add(label("Bloque Hamming:", TEXT_MUTED, 12));
        lblBloque = label("8 bits (.HA1)", TEXT_PRIMARY, 12);
        bar.add(lblBloque);
        return bar;
    }

    private JPanel buildViewer() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(BG_BASE);

        JPanel headers = new JPanel(new GridLayout(1, 2, 1, 0));
        headers.setBackground(BORDER);

        JPanel hIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        hIzq.setBackground(BG_SURFACE);
        hIzq.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        JLabel tIzq = label("Original", TEXT_PRIMARY, 12);
        tIzq.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lblTituloIzq = label("—", TEXT_MUTED, 11);
        hIzq.add(tIzq); hIzq.add(lblTituloIzq);

        JPanel hDir = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        hDir.setBackground(BG_SURFACE);
        hDir.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        JLabel tDir = label("Resultado", TEXT_PRIMARY, 12);
        tDir.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lblTituloDir = label("—", TEXT_MUTED, 11);
        hDir.add(tDir); hDir.add(lblTituloDir);

        headers.add(hIzq); headers.add(hDir);
        container.add(headers, BorderLayout.NORTH);

        txtIzquierdo = new JTextArea();
        txtIzquierdo.setBackground(BG_BASE);
        txtIzquierdo.setForeground(TEXT_PRIMARY);
        txtIzquierdo.setCaretColor(TEXT_PRIMARY);
        txtIzquierdo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtIzquierdo.setEditable(false);
        txtIzquierdo.setLineWrap(true);
        txtIzquierdo.setWrapStyleWord(true);
        txtIzquierdo.setBorder(new EmptyBorder(12, 14, 12, 14));

        txtDerecho = new JTextPane();
        txtDerecho.setBackground(BG_BASE);
        txtDerecho.setForeground(TEXT_PRIMARY);
        txtDerecho.setCaretColor(TEXT_PRIMARY);
        txtDerecho.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtDerecho.setEditable(false);
        txtDerecho.setBorder(new EmptyBorder(12, 14, 12, 14));

        JScrollPane scrollIzq = new JScrollPane(txtIzquierdo);
        scrollIzq.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));
        scrollIzq.getViewport().setBackground(BG_BASE);
        scrollIzq.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollIzq.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollIzq.setWheelScrollingEnabled(false);

        JScrollPane scrollDir = new JScrollPane(txtDerecho);
        scrollDir.setBorder(null);
        scrollDir.getViewport().setBackground(BG_BASE);
        scrollDir.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollDir.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scrollIzq.addMouseWheelListener(e ->
                scrollDir.dispatchEvent(SwingUtilities.convertMouseEvent(scrollIzq, e, scrollDir)));

        JScrollBar barIzq = scrollIzq.getVerticalScrollBar();
        JScrollBar barDir = scrollDir.getVerticalScrollBar();
        boolean[] syncing = {false};
        barIzq.addAdjustmentListener(e -> { if (!syncing[0]) { syncing[0]=true; barDir.setValue(e.getValue()); syncing[0]=false; }});
        barDir.addAdjustmentListener(e -> { if (!syncing[0]) { syncing[0]=true; barIzq.setValue(e.getValue()); syncing[0]=false; }});

        JPanel paneles = new JPanel(new GridLayout(1, 2, 0, 0));
        paneles.setBackground(BG_BASE);
        paneles.add(scrollIzq);
        paneles.add(scrollDir);
        container.add(paneles, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_SURFACE);
        panel.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        panel.setPreferredSize(new Dimension(0, 100));
        txtLog = new JTextArea();
        txtLog.setBackground(BG_SURFACE);
        txtLog.setForeground(TEXT_MUTED);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        txtLog.setEditable(false);
        txtLog.setBorder(new EmptyBorder(8, 14, 8, 14));
        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_SURFACE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── ACCIONES — ARCHIVO ────────────────────────────────────────────────────
    private void accionCargar() {
        FileDialog fd = new FileDialog(this, "Seleccionar archivo", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> {
            String n = name.toUpperCase();
            return n.endsWith(".TXT") || n.endsWith(".DOC") || n.endsWith(".WP")
                    || n.endsWith(".HA1") || n.endsWith(".HA2") || n.endsWith(".HA3")
                    || n.endsWith(".HE1") || n.endsWith(".HE2") || n.endsWith(".HE3")
                    || n.endsWith(".H21") || n.endsWith(".H22") || n.endsWith(".H23")
                    || n.endsWith(".HUF") || n.endsWith(".DHU")
                    || n.endsWith(".PH1") || n.endsWith(".PH2") || n.endsWith(".PH3")
                    || n.endsWith(".PE1") || n.endsWith(".PE2") || n.endsWith(".PE3")
                    || n.endsWith(".P21") || n.endsWith(".P22") || n.endsWith(".P23")
                    || n.endsWith(".PD1") || n.endsWith(".PD2") || n.endsWith(".PD3")
                    || n.endsWith(".REC");
        });
        fd.setVisible(true);
        if (fd.getFile() == null) return;

        archivoActivo = new File(fd.getDirectory(), fd.getFile());
        byte[] datos  = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        String ext = FileManagement.getExtension(archivoActivo.getName());
        if (esTexto(ext) || ext.equalsIgnoreCase(".DHU") || ext.equalsIgnoreCase(".REC")) {
            bytesOriginal = esTexto(ext) ? datos : bytesOriginal;
            txtIzquierdo.setText(new String(datos));
        } else {
            txtIzquierdo.setText("[archivo binario — " + datos.length + " bytes]");
        }

        int bi = FileManagement.getPipelineBlockIndex(archivoActivo.getAbsolutePath());
        if (bi == -1) {
            String e2 = ext;
            if (e2.matches("\\.(HA|HE|H2|DE|DC)[123]")) {
                bi = Integer.parseInt(e2.substring(e2.length()-1)) - 1;
            }
        }
        if (bi != -1) {
            blockIndexActivo = bi;
            actualizarBloqueLabel();
            resaltarBotonBloque(bi);
        }

        lblTituloIzq.setText(archivoActivo.getName());
        limpiarDerecho();
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(datos.length + " bytes");
        log("Cargado: " + archivoActivo.getName() + " (" + datos.length + " bytes)", SUCCESS);
    }

    // ── ACCIONES — LAB 1 HAMMING ──────────────────────────────────────────────
    private void accionProteger(int blockIndex) {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!esTexto(ext)) { log("ERROR: Solo se puede proteger .txt/.doc/.wp", DANGER); return; }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        byte[] codificado = Hamming.encode(datos, blockIndex);
        String pathHA = FileManagement.saveHammingFile(archivoActivo.getAbsolutePath(), blockIndex, codificado);
        if (pathHA == null) { log("ERROR: No se pudo guardar.", DANGER); return; }

        blockIndexActivo = blockIndex;
        archivoActivo = new File(pathHA);
        actualizarBloqueLabel(); resaltarBotonBloque(blockIndex);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(codificado.length + " bytes");
        mostrarTextoDerecho("[archivo Hamming — " + codificado.length + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());

        double overhead = ((double)(codificado.length - datos.length) / datos.length) * 100;
        log("Protegido: " + archivoActivo.getName()
                + " (" + codificado.length + " bytes, +" + String.format("%.0f", overhead) + "% overhead)", SUCCESS);
    }

    private void accionIntroducirErrores() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!esArchivoHamming(ext)) { log("ERROR: Seleccioná un archivo .HAx.", DANGER); return; }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer.", DANGER); return; }

        byte[] conErrores = errorUtilities.injectErrors(datos, blockIndexActivo);
        String pathHE = FileManagement.saveErrorFile(archivoActivo.getAbsolutePath(), conErrores);
        if (pathHE == null) { log("ERROR: No se pudo guardar.", DANGER); return; }

        archivoActivo = new File(pathHE);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(conErrores.length + " bytes");
        mostrarTextoDerecho("[archivo con errores — " + conErrores.length + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());
        log("Errores introducidos (1/módulo): " + archivoActivo.getName(), WARNING);
        log(errorUtilities.resumenErrores(datos, conErrores, blockIndexActivo), WARNING);
    }

    private void accionDecodificar(boolean corregir) {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!esArchivoHamming(ext) && !esArchivoConError(ext)) {
            log("ERROR: Seleccioná un archivo .HAx o .HEx.", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer.", DANGER); return; }

        Hamming.DecodeResult dr = Hamming.decodeDetallado(datos, blockIndexActivo, corregir);

        if (corregir && dr.tieneDobleError()) {
            mostrarAlertaDobleError(dr.bloquesConDobleError);
        }

        String pathSalida = corregir
                ? FileManagement.saveDecodedCorrected(archivoActivo.getAbsolutePath(), dr.datos)
                : FileManagement.saveDecodedError(archivoActivo.getAbsolutePath(), dr.datos);
        if (pathSalida == null) { log("ERROR: No se pudo guardar.", DANGER); return; }

        String textoRecuperado = new String(dr.datos);
        lblTituloDir.setText(new File(pathSalida).getName());

        if (!corregir && bytesOriginal != null)
            mostrarTextoConErrores(new String(bytesOriginal), textoRecuperado);
        else if (corregir && dr.tieneDobleError() && bytesOriginal != null)
            mostrarTextoConErrores(new String(bytesOriginal), textoRecuperado);
        else
            mostrarTextoDerecho(textoRecuperado);

        String modo = corregir ? "corrigiendo → .DCx" : "sin corregir → .DEx";
        log("Decodificado " + modo + ": " + new File(pathSalida).getName(), INFO);
        if (corregir) {
            if (!dr.bloqueCorregidos.isEmpty())
                log("Bloques corregidos (1 error): " + dr.bloqueCorregidos.size(), SUCCESS);
            if (dr.tieneDobleError())
                log("⚠ Bloques con 2 errores (no corregibles): " + dr.bloquesConDobleError.size(), DANGER);
        }
    }

    // ── ACCIONES — LAB 2 HUFFMAN ──────────────────────────────────────────────
    private void accionCompactar() {
        if (!verificarArchivoCargado()) return;
        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer.", DANGER); return; }

        String pathHuf = FileManagement.buildHuffmanPath(archivoActivo.getAbsolutePath());
        if (!Huffman.encode(datos, pathHuf)) { log("ERROR: No se pudo comprimir.", DANGER); return; }

        byte[] comprimido = FileManagement.readFile(pathHuf);
        archivoActivo = new File(pathHuf);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(comprimido != null ? comprimido.length + " bytes" : "—");
        mostrarTextoDerecho("[archivo comprimido Huffman — " + (comprimido != null ? comprimido.length : 0) + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());

        double red = comprimido != null ? (1.0 - (double) comprimido.length / datos.length) * 100 : 0;
        log("Compactado: " + archivoActivo.getName()
                + " (" + (comprimido!=null?comprimido.length:0)
                + " bytes, -" + String.format("%.1f", red) + "% tamaño)", GREEN_INFO);
    }

    private void accionDescompactar() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!ext.equalsIgnoreCase(".HUF")) { log("ERROR: Seleccioná un .huf.", DANGER); return; }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer.", DANGER); return; }

        byte[] decomp = Huffman.decode(datos);
        if (decomp == null) { log("ERROR: No se pudo descomprimir.", DANGER); return; }

        String pathDhu = FileManagement.saveHuffmanDecFile(archivoActivo.getAbsolutePath(), decomp);
        if (pathDhu == null) { log("ERROR: No se pudo guardar.", DANGER); return; }

        mostrarTextoDerecho(new String(decomp));
        lblTituloDir.setText(new File(pathDhu).getName());
        boolean iguales = bytesOriginal != null && java.util.Arrays.equals(bytesOriginal, decomp);
        log("Descompactado: " + new File(pathDhu).getName() + " (" + decomp.length + " bytes)", GREEN_INFO);
        if (bytesOriginal != null) log("Igual al original: " + iguales, iguales ? SUCCESS : DANGER);
    }

    private void accionVerEstadisticas() {
        if (bytesOriginal == null) { log("ERROR: Cargá el archivo original primero.", DANGER); return; }

        String pathHuf = FileManagement.buildHuffmanPath(archivoActivo.getAbsolutePath());
        if (archivoActivo.getName().endsWith(".huf")) pathHuf = archivoActivo.getAbsolutePath();

        byte[] comprimido = FileManagement.readFile(pathHuf);
        if (comprimido == null) { log("ERROR: No se encontró .huf. Compactá primero.", DANGER); return; }

        byte[] descomp = Huffman.decode(comprimido);
        Map<Byte, String>  codigos     = Huffman.obtenerCodigos(bytesOriginal);
        Map<Byte, Integer> frecuencias = Huffman.obtenerFrecuencias(bytesOriginal);

        HuffmanUtils stats = new HuffmanUtils(bytesOriginal, comprimido, descomp, codigos, frecuencias);

        JTextArea txt = new JTextArea(stats.generarReporte());
        txt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txt.setEditable(false);
        txt.setBackground(BG_BASE);
        txt.setForeground(TEXT_PRIMARY);
        txt.setBorder(new EmptyBorder(12, 14, 12, 14));

        JScrollPane scroll = new JScrollPane(txt);
        scroll.setPreferredSize(new Dimension(560, 440));
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_BASE);

        JDialog dlg = new JDialog(this, "Estadísticas Huffman", true);
        dlg.setBackground(BG_BASE);
        dlg.getContentPane().setBackground(BG_BASE);
        dlg.add(scroll);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        log(stats.resumenCorto(), GREEN_INFO);
    }

    // ── ACCIONES — LAB 3 PIPELINE ─────────────────────────────────────────────
    private void accionPipelineComprimir() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!esTexto(ext)) { log("ERROR: El pipeline requiere un archivo de texto (.txt/.doc/.wp).", DANGER); return; }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer.", DANGER); return; }

        String tempHuf = archivoActivo.getAbsolutePath() + ".tmp.huf";
        if (!Huffman.encode(datos, tempHuf)) { log("ERROR: Falló la compresión Huffman.", DANGER); return; }

        byte[] comprimido = FileManagement.readFile(tempHuf);
        new File(tempHuf).delete();
        if (comprimido == null) { log("ERROR: No se pudo leer el comprimido temporal.", DANGER); return; }

        int blockIndex = elegirBloquePipeline();
        if (blockIndex == -1) return;

        byte[] protegido = Hamming.encode(comprimido, blockIndex);

        String pathPH = FileManagement.buildPipelineHamPath(archivoActivo.getAbsolutePath(), blockIndex);
        if (!FileManagement.writeFile(pathPH, protegido)) { log("ERROR: No se pudo guardar .PHx.", DANGER); return; }

        blockIndexActivo = blockIndex;
        archivoActivo = new File(pathPH);
        actualizarBloqueLabel(); resaltarBotonBloque(blockIndex);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(protegido.length + " bytes");
        mostrarTextoDerecho("[Huffman+Hamming — " + protegido.length + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());

        double redHuf = (1.0 - (double) comprimido.length / datos.length) * 100;
        double overhead = ((double)(protegido.length - comprimido.length) / comprimido.length) * 100;
        log("Pipeline: texto(" + datos.length + " B)"
                + " → Huffman(" + comprimido.length + " B, -" + String.format("%.1f", redHuf) + "%)"
                + " → Hamming(" + protegido.length + " B, +" + String.format("%.0f", overhead) + "%)"
                + " → " + archivoActivo.getName(), GREEN_INFO);
    }

    private void accionPipelineError1() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!ext.matches("\\.(PH[123])")) { log("ERROR: Seleccioná un archivo .PHx.", DANGER); return; }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer.", DANGER); return; }

        byte[] conErr = errorUtilities.injectErrors(datos, blockIndexActivo);
        String pathPE = FileManagement.buildPipelineErrPath(archivoActivo.getAbsolutePath());
        if (pathPE == null || !FileManagement.writeFile(pathPE, conErr)) {
            log("ERROR: No se pudo guardar .PEx.", DANGER); return;
        }

        archivoActivo = new File(pathPE);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(conErr.length + " bytes");
        mostrarTextoDerecho("[pipeline con 1 error/módulo — " + conErr.length + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());
        log("Pipeline — 1 error/módulo: " + archivoActivo.getName(), WARNING);
        log(errorUtilities.resumenErrores(datos, conErr, blockIndexActivo), WARNING);
    }

    private void accionPipelineError2() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!ext.matches("\\.(PH[123])")) { log("ERROR: Seleccioná un archivo .PHx.", DANGER); return; }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer.", DANGER); return; }

        byte[] conErr2 = errorUtilities.injectDoubleErrors(datos, blockIndexActivo);
        String pathP2 = FileManagement.buildPipelineErr2Path(archivoActivo.getAbsolutePath());
        if (pathP2 == null || !FileManagement.writeFile(pathP2, conErr2)) {
            log("ERROR: No se pudo guardar .P2x.", DANGER); return;
        }

        archivoActivo = new File(pathP2);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(conErr2.length + " bytes");
        mostrarTextoDerecho("[pipeline con 2 errores/módulo — " + conErr2.length + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());
        log("Pipeline — 2 errores/módulo: " + archivoActivo.getName(), WARNING);
        log(errorUtilities.resumenErrores(datos, conErr2, blockIndexActivo), WARNING);
    }

    private void accionPipelineDescode(boolean corregir) {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!ext.matches("\\.(PH|PE|P2)[123]")) {
            log("ERROR: Seleccioná un .PHx, .PEx o .P2x.", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer.", DANGER); return; }

        Hamming.DecodeResult dr = Hamming.decodeDetallado(datos, blockIndexActivo, corregir);

        if (corregir && dr.tieneDobleError()) {
            mostrarAlertaDobleError(dr.bloquesConDobleError);
        }

        String pathPD = FileManagement.buildPipelineDecPath(archivoActivo.getAbsolutePath());
        if (pathPD == null || !FileManagement.writeFile(pathPD, dr.datos)) {
            log("ERROR: No se pudo guardar .PDx.", DANGER); return;
        }

        archivoActivo = new File(pathPD);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(dr.datos.length + " bytes");
        mostrarTextoDerecho("[desprotegido (aún Huffman) — " + dr.datos.length + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());

        String modo = corregir ? "corrigiendo" : "sin corregir";
        log("Pipeline desprotegido (" + modo + "): " + archivoActivo.getName(), INFO);
        if (corregir && !dr.bloqueCorregidos.isEmpty())
            log("  Bloques corregidos: " + dr.bloqueCorregidos.size(), SUCCESS);
        if (dr.tieneDobleError())
            log("  ⚠ Bloques con doble error (no corregibles): " + dr.bloquesConDobleError.size(), DANGER);
    }

    private void accionPipelineRecuperar() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!ext.matches("\\.(PD[123])")) {
            log("ERROR: Seleccioná un archivo .PDx (desprotegido de pipeline).", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer.", DANGER); return; }

        byte[] recuperado = Huffman.decode(datos);
        if (recuperado == null) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo descomprimir el archivo.\n" +
                            "Es probable que los datos estén demasiado corrompidos por errores no corregibles.\n" +
                            "Intentá usar un .PDx generado con corrección activa.",
                    "Error de descompresión",
                    JOptionPane.ERROR_MESSAGE);
            log("ERROR: No se pudo descomprimir (datos corrompidos).", DANGER);
            return;
        }

        String pathRec = FileManagement.buildPipelineFinalPath(archivoActivo.getAbsolutePath());
        if (!FileManagement.writeFile(pathRec, recuperado)) {
            log("ERROR: No se pudo guardar .rec.", DANGER); return;
        }

        String textoRec = new String(recuperado);
        lblTituloDir.setText(new File(pathRec).getName());

        if (bytesOriginal != null) {
            boolean iguales = java.util.Arrays.equals(bytesOriginal, recuperado);
            if (iguales) {
                mostrarTextoDerecho(textoRec);
                log("✓ Recuperación perfecta: el archivo es idéntico al original.", SUCCESS);
            } else {
                mostrarTextoConErrores(new String(bytesOriginal), textoRec);
                log("⚠ Recuperado con diferencias respecto al original.", WARNING);
            }
        } else {
            mostrarTextoDerecho(textoRec);
            log("Archivo recuperado (sin original cargado para comparar).", INFO);
        }

        archivoActivo = new File(pathRec);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(recuperado.length + " bytes");
        log("Pipeline completo → " + archivoActivo.getName() + " (" + recuperado.length + " bytes)", GREEN_INFO);
    }

    // ── HELPERS DE ACCIONES ───────────────────────────────────────────────────
    private int elegirBloquePipeline() {
        String[] textos = {
                "8 bits   (.PH1)  — para archivos pequeños",
                "1024 bits (.PH2) — equilibrado",
                "16384 bits (.PH3) — para archivos grandes"
        };

        JButton[] botones = new JButton[textos.length];
        final int[] seleccion = {-1};

        for (int i = 0; i < textos.length; i++) {
            final int index = i;
            botones[i] = new JButton(textos[i]);
            botones[i].setBackground(ACCENT_PURP);
            botones[i].setForeground(Color.WHITE);
            botones[i].setFocusPainted(false);

            botones[i].addActionListener(e -> {
                seleccion[0] = index;
                java.awt.Window win = SwingUtilities.getWindowAncestor((JButton)e.getSource());
                if (win != null) {
                    win.dispose();
                }
            });
        }

        Object origBack = UIManager.get("OptionPane.background");
        Object origBorder = UIManager.get("OptionPane.border");
        Object origIconBorder = UIManager.get("OptionPane.iconBorder");
        Object origButtonArea = UIManager.get("OptionPane.buttonAreaBorder");

        UIManager.put("OptionPane.background", BG_BASE);
        UIManager.put("Panel.background", BG_BASE);
        UIManager.put("OptionPane.border", new EmptyBorder(14, 14, 14, 14));
        UIManager.put("OptionPane.iconBorder", new EmptyBorder(0, 0, 0, 0));
        UIManager.put("OptionPane.buttonAreaBorder", new EmptyBorder(0, 0, 0, 0));


        JOptionPane.showOptionDialog(
                this,
                "Seleccioná el tamaño de bloque Hamming para el pipeline:",
                "Tamaño de bloque — Lab 3",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                botones, // Pasamos tus botones custom
                botones[1]
        );

        UIManager.put("OptionPane.background", origBack);
        UIManager.put("Panel.background", origBack);
        UIManager.put("OptionPane.border", origBorder);
        UIManager.put("OptionPane.iconBorder", origIconBorder);
        UIManager.put("OptionPane.buttonAreaBorder", origButtonArea);

        if (seleccion[0] < 0) { log("Operación cancelada.", TEXT_MUTED); return -1; }
        return seleccion[0];
    }

    private void mostrarAlertaDobleError(List<Integer> bloques) {
        int total = bloques.size();
        String primeros = bloques.subList(0, Math.min(10, total)).toString();

        JTextArea detalle = new JTextArea(
                "Se detectaron " + total + " módulo(s) con 2 errores que NO pueden corregirse.\n\n" +
                        "Hamming(n,k) corrige 1 error por bloque y detecta 2, pero no puede\n" +
                        "corregir 2 errores simultáneos — estos bloques quedan con datos incorrectos.\n\n" +
                        "Primeros bloques afectados: " + primeros + (total > 10 ? " ..." : "") + "\n\n" +
                        "El archivo decodificado mostrará en ROJO los caracteres afectados."
        );
        detalle.setEditable(false);
        detalle.setOpaque(false);
        detalle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        detalle.setForeground(TEXT_PRIMARY); // Usando constante global de texto

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_BASE); // Seteado a 0x1E1E1E
        panel.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel icono = new JLabel("⚠  DOBLE ERROR DETECTADO", SwingConstants.CENTER);
        icono.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        icono.setForeground(DANGER);
        panel.add(icono, BorderLayout.NORTH);
        panel.add(detalle, BorderLayout.CENTER);

        JOptionPane optPane = new JOptionPane(panel, JOptionPane.ERROR_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[]{"Entendido"});
        JDialog dialog = optPane.createDialog(this, "Módulos con doble error — no corregibles");

        dialog.setBackground(BG_BASE);
        dialog.getContentPane().setBackground(BG_BASE); // Forzar fondo 0x1E1E1E
        dialog.setVisible(true);
    }

    // ── MOSTRAR TEXTO ────────────────────────────────────────────────XXXXXXXX─
    private void mostrarTextoDerecho(String texto) {
        txtDerecho.setText("");
        StyledDocument doc = txtDerecho.getStyledDocument();
        SimpleAttributeSet est = new SimpleAttributeSet();
        StyleConstants.setForeground(est, TEXT_PRIMARY);
        StyleConstants.setFontFamily(est, Font.MONOSPACED);
        StyleConstants.setFontSize(est, 13);
        StyleConstants.setBold(est, false);
        try { doc.insertString(0, texto, est); } catch (BadLocationException e) { e.printStackTrace(); }
        txtDerecho.setCaretPosition(0);
    }

    private void mostrarTextoConErrores(String original, String conErrores) {
        txtDerecho.setText("");
        StyledDocument doc = txtDerecho.getStyledDocument();

        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setForeground(normal, TEXT_PRIMARY);
        StyleConstants.setFontFamily(normal, Font.MONOSPACED);
        StyleConstants.setFontSize(normal, 13);
        StyleConstants.setBold(normal, false);

        SimpleAttributeSet error = new SimpleAttributeSet();
        StyleConstants.setForeground(error, DANGER);
        StyleConstants.setFontFamily(error, Font.MONOSPACED);
        StyleConstants.setFontSize(error, 13);
        StyleConstants.setBold(error, true);

        for (int i = 0; i < conErrores.length(); i++) {
            char c = conErrores.charAt(i);
            boolean diff = (i >= original.length()) || (c != original.charAt(i));
            try {
                doc.insertString(doc.getLength(), String.valueOf(c), diff ? error : normal);
            } catch (BadLocationException e) { e.printStackTrace(); }
        }
        txtDerecho.setCaretPosition(0);
    }

    // ── HELPERS UI ────────────────────────────────────────────────────────────
    private JLabel sectionLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(TEXT_HINT);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        lbl.setBorder(new EmptyBorder(0, 14, 0, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JButton sideBtn(String texto, Color bg, java.awt.event.ActionListener action) {
        JButton btn = new JButton(texto);
        btn.setBackground(bg);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.addActionListener(action);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private JLabel label(String texto, Color color, int size) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(color);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
        return lbl;
    }

    private JLabel badge(String texto, Color color) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(color);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1, true),
                new EmptyBorder(1, 6, 1, 6)));
        return lbl;
    }

    private void limpiarDerecho() {
        txtDerecho.setText("");
        lblTituloDir.setText("—");
    }

    private void log(String mensaje, Color color) {
        SwingUtilities.invokeLater(() -> {
            txtLog.setForeground(color);
            txtLog.append(mensaje + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void actualizarBloqueLabel() {
        String[] nombres = {"8 bits (.HA1)", "1024 bits (.HA2)", "16384 bits (.HA3)"};
        lblBloque.setText(nombres[blockIndexActivo]);
    }

    private void resaltarBotonBloque(int bi) {
        btnHA1.setBackground(bi == FileManagement.BLOCK_8     ? ACCENT_BLUE : BG_ELEVATED);
        btnHA2.setBackground(bi == FileManagement.BLOCK_1024  ? ACCENT_BLUE : BG_ELEVATED);
        btnHA3.setBackground(bi == FileManagement.BLOCK_16384 ? ACCENT_BLUE : BG_ELEVATED);
    }

    private boolean verificarArchivoCargado() {
        if (archivoActivo == null) { log("ERROR: Primero cargá un archivo.", DANGER); return false; }
        return true;
    }

    private boolean esTexto(String ext) {
        return ext.equalsIgnoreCase(".txt") || ext.equalsIgnoreCase(".doc") || ext.equalsIgnoreCase(".wp");
    }

    private boolean esArchivoHamming(String ext) {
        return ext.matches("(?i)\\.(HA|HE|H2|DE|DC)[123]");
    }

    private boolean esArchivoConError(String ext) {
        return ext.matches("(?i)\\.(HE|H2)[123]");
    }

    // ── ENTRY POINT ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Configurar FlatLaf primero
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");

                // Forzar UI Manager global de JOptionPane a respetar la constante BG_BASE (0x1E1E1E)
                UIManager.put("OptionPane.background", BG_BASE);
                UIManager.put("Panel.background", BG_BASE);
                UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);

            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                    UIManager.put("Button.background", new Color(0x2D2D2D));
                    UIManager.put("Button.select",     new Color(0x3C3C3C));
                } catch (Exception ignored) {}
            }
            new MainWindow().setVisible(true);
        });
    }
}