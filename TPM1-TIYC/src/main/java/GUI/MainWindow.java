package GUI;

import hamming.errorUtilities;
import hamming.Hamming;
import hamming.file_mngmt.FileManagement;
import hamming.file_mngmt.LectorDocumentos;
import huffman.Huffman;
import huffman.HuffmanUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.util.Map;


public class MainWindow extends JFrame {

    // =========================================================================
    // PALETA DARK
    // =========================================================================
    private static final Color BG_BASE       = new Color(0x1E1E1E);
    private static final Color BG_SURFACE    = new Color(0x252526);
    private static final Color BG_ELEVATED   = new Color(0x2D2D2D);
    private static final Color BORDER        = new Color(0x3C3C3C);
    private static final Color TEXT_PRIMARY  = new Color(0xD4D4D4);
    private static final Color TEXT_MUTED    = new Color(0x858585);
    private static final Color TEXT_HINT     = new Color(0x555555);
    private static final Color ACCENT_BLUE   = new Color(0x0E639C);
    private static final Color ACCENT_GREEN  = new Color(0x1B6B3A);
    private static final Color SUCCESS       = new Color(0x4EC9B0);
    private static final Color WARNING       = new Color(0xDCDCAA);
    private static final Color DANGER        = new Color(0xF44747);
    private static final Color INFO          = new Color(0x9CDCFE);
    private static final Color GREEN_INFO    = new Color(0x6BBF6B);

    // =========================================================================
    // ESTADO
    // =========================================================================
    private File   archivoActivo    = null;
    private int    blockIndexActivo = FileManagement.BLOCK_8;
    private byte[] bytesOriginal    = null;
    private byte[] bytesAntesDeCompactar = null;

    // =========================================================================
    // COMPONENTES
    // =========================================================================
    private JLabel    lblArchivoActivo;
    private JLabel    lblTamano;
    private JLabel    lblBloque;
    private JTextPane txtIzquierdo;
    private JTextPane txtDerecho;
    private JLabel    lblTituloIzq;
    private JLabel    lblTituloDir;
    private JTextArea txtLog;

    private JButton btnHA1, btnHA2, btnHA3;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public MainWindow() {
        super("Hamming + Huffman — TPM1-TIYC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 680);
        setMinimumSize(new Dimension(900, 520));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_BASE);
        setLayout(new BorderLayout());

        add(buildSidebar(),  BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);

        log("Listo. Cargá un archivo para comenzar.", TEXT_MUTED);
    }

    // =========================================================================
    // SIDEBAR
    // =========================================================================
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SURFACE);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));
        sidebar.setPreferredSize(new Dimension(230, 0));

        sidebar.add(Box.createVerticalStrut(16));

        sidebar.add(sectionLabel("ARCHIVO"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(sideBtn("Cargar archivo", ACCENT_BLUE, e -> accionCargar()));
        sidebar.add(Box.createVerticalStrut(16));

        sidebar.add(sectionLabel("HAMMING"));
        sidebar.add(Box.createVerticalStrut(6));
        btnHA1 = sideBtn("Proteger 8 bits  →  .HA1",      BG_ELEVATED, e -> accionProteger(FileManagement.BLOCK_8));
        btnHA2 = sideBtn("Proteger 1024 bits  →  .HA2",   BG_ELEVATED, e -> accionProteger(FileManagement.BLOCK_1024));
        btnHA3 = sideBtn("Proteger 16384 bits  →  .HA3",  BG_ELEVATED, e -> accionProteger(FileManagement.BLOCK_16384));
        sidebar.add(btnHA1);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnHA2);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnHA3);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Introducir 1 error  →  .HEx",     BG_ELEVATED, e -> accionIntroducirErrores(false)));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Introducir hasta 2 errores  →  .HEx",  BG_ELEVATED, e -> accionIntroducirErrores(true)));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Desproteger con errores  →  .DEx", BG_ELEVATED, e -> accionDecodificar(false)));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Desproteger corrigiendo  →  .DCx", BG_ELEVATED, e -> accionDecodificar(true)));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Establecer fecha apertura",         BG_ELEVATED, e -> accionFechaApertura()));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Ver estadísticas Hamming",          BG_ELEVATED, e -> accionVerEstadisticasHamming()));
        sidebar.add(Box.createVerticalStrut(16));

        sidebar.add(sectionLabel("HUFFMAN"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(sideBtn("Compactar  →  .huf",    ACCENT_GREEN, e -> accionCompactar()));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Descompactar  →  .dhu", BG_ELEVATED,  e -> accionDescompactar()));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Ver estadísticas",       BG_ELEVATED,  e -> accionVerEstadisticas()));

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    // =========================================================================
    // ÁREA PRINCIPAL
    // =========================================================================
    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG_BASE);
        main.add(buildTopBar(),   BorderLayout.NORTH);
        main.add(buildViewer(),   BorderLayout.CENTER);
        main.add(buildLogPanel(), BorderLayout.SOUTH);
        return main;
    }

    // ── TOPBAR ────────────────────────────────────────────────────────────────
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

        JPanel headerIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        headerIzq.setBackground(BG_SURFACE);
        headerIzq.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        JLabel tituloIzq = label("Original", TEXT_PRIMARY, 12);
        tituloIzq.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lblTituloIzq = label("—", TEXT_MUTED, 11);
        headerIzq.add(tituloIzq);
        headerIzq.add(lblTituloIzq);

        JPanel headerDir = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        headerDir.setBackground(BG_SURFACE);
        headerDir.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        JLabel tituloDir = label("Resultado", TEXT_PRIMARY, 12);
        tituloDir.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lblTituloDir = label("—", TEXT_MUTED, 11);
        headerDir.add(tituloDir);
        headerDir.add(lblTituloDir);

        headers.add(headerIzq);
        headers.add(headerDir);
        container.add(headers, BorderLayout.NORTH);

        txtIzquierdo = new JTextPane();
        txtIzquierdo.setBackground(BG_BASE);
        txtIzquierdo.setForeground(TEXT_PRIMARY);
        txtIzquierdo.setCaretColor(TEXT_PRIMARY);
        txtIzquierdo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtIzquierdo.setEditable(false);
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

        scrollIzq.addMouseWheelListener(e -> {
            scrollDir.dispatchEvent(SwingUtilities.convertMouseEvent(scrollIzq, e, scrollDir));
        });

        JScrollBar barIzq = scrollIzq.getVerticalScrollBar();
        JScrollBar barDir = scrollDir.getVerticalScrollBar();

        boolean[] isSyncing = {false};

        barIzq.addAdjustmentListener(e -> {
            if (isSyncing[0]) return;
            isSyncing[0] = true;
            barDir.setValue(e.getValue());
            isSyncing[0] = false;
        });

        barDir.addAdjustmentListener(e -> {
            if (isSyncing[0]) return;
            isSyncing[0] = true;
            barIzq.setValue(e.getValue());
            isSyncing[0] = false;
        });

        JPanel paneles = new JPanel(new GridLayout(1, 2, 0, 0));
        paneles.setBackground(BG_BASE);
        paneles.add(scrollIzq);
        paneles.add(scrollDir);

        container.add(paneles, BorderLayout.CENTER);
        return container;
    }

    // ── LOG ───────────────────────────────────────────────────────────────────
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

    // =========================================================================
    // ACCIONES — ARCHIVO
    // =========================================================================

    private void accionCargar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos compatibles",
                // Documentos fuente
                "txt", "doc", "docx", "wp", "pdf",
                // Hamming codificado
                "HA1", "HA2", "HA3",
                // Hamming con 1 error
                "HE1", "HE2", "HE3",
                // Hamming con 2 errores
                "H21", "H22", "H23",
                // Hamming desprotegido con errores
                "DE1", "DE2", "DE3",
                // Hamming desprotegido corregido
                "DC1", "DC2", "DC3",
                // Huffman comprimido / descomprimido
                "huf", "dhu",
                // Pipeline Huffman+Hamming protegido
                "PH1", "PH2", "PH3",
                // Pipeline con error simple
                "PE1", "PE2", "PE3",
                // Pipeline con doble error
                "P21", "P22", "P23",
                // Pipeline desprotegido (aún comprimido)
                "PD1", "PD2", "PD3",
                // Archivo final recuperado
                "rec"
        ));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        archivoActivo = chooser.getSelectedFile();
        byte[] datos  = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        String ext = FileManagement.getExtension(archivoActivo.getName());

        if (LectorDocumentos.esFormatoSoportado(archivoActivo.getName())) {
            bytesOriginal = datos;
            mostrarTextoIzquierdo(LectorDocumentos.extraerTextoParaVista(archivoActivo.getName(), datos));
        } else if (ext.equalsIgnoreCase(".dhu")) {
            if (bytesOriginal != null && datos.length == bytesOriginal.length) {
                String nombreOriginal = FileManagement.getBaseName(archivoActivo.getName());
                mostrarTextoIzquierdo(LectorDocumentos.extraerTextoParaVista(nombreOriginal, datos));
            } else {
                mostrarTextoIzquierdo("[archivo binario — " + datos.length + " bytes]");
            }
        } else {
            mostrarTextoIzquierdo("[archivo binario — " + datos.length + " bytes]");
        }

        lblTituloIzq.setText(archivoActivo.getName());
        limpiarDerecho();
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(datos.length + " bytes");
        log("Archivo cargado: " + archivoActivo.getName() + " (" + datos.length + " bytes)", SUCCESS);
    }

    // =========================================================================
    // ACCIONES — HAMMING
    // =========================================================================

    private void accionProteger(int blockIndex) {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());

        if (!LectorDocumentos.esFormatoSoportado(archivoActivo.getName()) && !ext.equalsIgnoreCase(".huf")) {
            log("ERROR: Solo se puede proteger un documento soportado o un archivo compactado (.huf)", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        byte[] codificado = Hamming.encode(datos, blockIndex);
        String pathHA     = archivoActivo.getAbsolutePath() + FileManagement.EXT_HAMMING[blockIndex];
        boolean guardadoOk = FileManagement.writeFile(pathHA, codificado);

        if (!guardadoOk) { log("ERROR: No se pudo guardar el archivo protegido.", DANGER); return; }

        blockIndexActivo = blockIndex;
        archivoActivo    = new File(pathHA);
        actualizarBloqueLabel();
        resaltarBotonBloque(blockIndex);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(codificado.length + " bytes");
        mostrarTextoDerecho("[archivo Hamming — " + codificado.length + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());

        double overhead = ((double)(codificado.length - datos.length) / datos.length) * 100;
        log("Archivo protegido: " + archivoActivo.getName()
                + " (" + codificado.length + " bytes, +" + String.format("%.0f", overhead) + "% overhead)", SUCCESS);
    }

    private void accionIntroducirErrores(boolean dobleError) {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!esArchivoHamming(ext) && !ext.equalsIgnoreCase(".dhu")) {
            log("ERROR: Seleccioná primero un archivo .HAx (o su versión descompactada .dhu).", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        byte[] conErrores = dobleError
                ? errorUtilities.injectUpToTwoErrors(datos, blockIndexActivo)
                : errorUtilities.injectOneError(datos, blockIndexActivo);

        String pathHE = dobleError
                ? FileManagement.saveError2File(archivoActivo.getAbsolutePath(), conErrores)
                : FileManagement.saveErrorFile(archivoActivo.getAbsolutePath(), conErrores);

        if (pathHE == null) {
            String extErr = dobleError ? FileManagement.EXT_ERROR2[blockIndexActivo] : FileManagement.EXT_ERROR[blockIndexActivo];
            pathHE = FileManagement.getBaseName(archivoActivo.getAbsolutePath()) + extErr;
            FileManagement.writeFile(pathHE, conErrores);
        }

        archivoActivo = new File(pathHE);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(conErrores.length + " bytes");
        mostrarTextoDerecho("[archivo con errores — " + conErrores.length + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());

        String modo = dobleError ? "≤2 errores por módulo" : "1 error máximo por módulo";
        log("Errores introducidos (" + modo + "): " + archivoActivo.getName(), WARNING);
        log(errorUtilities.resumenErrores(datos, conErrores, blockIndexActivo), WARNING);
    }

    private void accionFechaApertura() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!esArchivoHamming(ext)) {
            log("ERROR: Seleccioná un archivo .HAx para establecer la fecha de apertura.", DANGER); return;
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setBackground(BG_BASE);
        JLabel lblFecha = new JLabel("Fecha (dd/MM/yyyy):");
        JLabel lblHora  = new JLabel("Hora (HH:mm):");
        lblFecha.setForeground(TEXT_PRIMARY);
        lblHora.setForeground(TEXT_PRIMARY);
        JTextField txtFecha = new JTextField("31/12/2026");
        JTextField txtHora  = new JTextField("23:59");
        panel.add(lblFecha); panel.add(txtFecha);
        panel.add(lblHora);  panel.add(txtHora);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Establecer fecha de apertura", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            java.time.LocalDateTime fechaApertura =
                    java.time.LocalDateTime.parse(txtFecha.getText().trim()
                            + " " + txtHora.getText().trim(), fmt);

            byte[] datos    = FileManagement.readFile(archivoActivo.getAbsolutePath());
            byte[] modificado = errorUtilities.setFechaApertura(datos, fechaApertura);
            FileManagement.writeFile(archivoActivo.getAbsolutePath(), modificado);

            log("Fecha de apertura establecida: " + txtFecha.getText()
                    + " " + txtHora.getText(), INFO);

        } catch (Exception ex) {
            log("ERROR: Formato de fecha inválido. Usá dd/MM/yyyy y HH:mm", DANGER);
        }
    }

    private void accionDecodificar(boolean corregir) {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());

        if (!esArchivoHamming(ext) && !esArchivoConError(ext) && !esArchivoConDobleError(archivoActivo.getName()) && !ext.equalsIgnoreCase(".dhu")) {
            log("ERROR: Seleccioná un archivo .HAx, .HEx, .H2x o .dhu.", DANGER); return;
        }

        String nameUpper = archivoActivo.getName().toUpperCase();
        if (nameUpper.contains(".HA1") || nameUpper.contains(".HE1") || nameUpper.contains(".H21")) blockIndexActivo = FileManagement.BLOCK_8;
        else if (nameUpper.contains(".HA2") || nameUpper.contains(".HE2") || nameUpper.contains(".H22")) blockIndexActivo = FileManagement.BLOCK_1024;
        else if (nameUpper.contains(".HA3") || nameUpper.contains(".HE3") || nameUpper.contains(".H23")) blockIndexActivo = FileManagement.BLOCK_16384;
        actualizarBloqueLabel();
        resaltarBotonBloque(blockIndexActivo);

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        if (!errorUtilities.verificarFechaApertura(datos)) {
            String fechaStr = errorUtilities.getFechaAperturaString(datos);
            log("ERROR: Este archivo no se puede abrir hasta el " + fechaStr, DANGER);
            return;
        }

        if (corregir && esArchivoConDobleError(archivoActivo.getName())) {
            JOptionPane.showMessageDialog(this,
                    "Se han detectado módulos con 2 errores.\nHamming no puede corregirlos de manera confiable.\nSe mostrará el texto con los errores originales.",
                    "Errores Incorregibles", JOptionPane.WARNING_MESSAGE);
            corregir = false;
        }

        int cantOriginal    = (bytesOriginal != null) ? bytesOriginal.length : -1;
        byte[] decodificado = Hamming.decode(datos, blockIndexActivo, corregir, cantOriginal);

        String pathSalida = corregir
                ? FileManagement.saveDecodedCorrected(archivoActivo.getAbsolutePath(), decodificado)
                : FileManagement.saveDecodedError(archivoActivo.getAbsolutePath(), decodificado);

        if (pathSalida == null) {
            String extDec = corregir ? FileManagement.EXT_DEC_CORR[blockIndexActivo] : FileManagement.EXT_DEC_ERR[blockIndexActivo];
            pathSalida = FileManagement.getBaseName(archivoActivo.getAbsolutePath()) + extDec;
            FileManagement.writeFile(pathSalida, decodificado);
        }

        File fSalida = new File(pathSalida);
        lblTituloDir.setText(fSalida.getName());

        // ── Actualizar archivo activo al resultado del decode ─────────────────
        archivoActivo = fSalida;
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(decodificado.length + " bytes");

        // --- VISUALIZACIÓN ---
        // Reconstruir el nombre base del archivo original (sin extensiones Hamming ni Huffman)
        // ej: "documento.txt.huf.HA1" → base "documento.txt.huf" → baseName sin .huf = "documento.txt"
        String baseConHuf = FileManagement.getBaseName(fSalida.getName()); // quita .DC1/.DE1
        String baseName;
        if (baseConHuf.toLowerCase().endsWith(".huf")) {
            // Pipeline Huffman+Hamming: el nombre real está debajo del .huf
            baseName = FileManagement.getBaseName(baseConHuf); // quita .huf → "documento.txt"
        } else {
            baseName = baseConHuf; // flujo simple Hamming directo
        }

        if (bytesOriginal != null && LectorDocumentos.esFormatoSoportado(baseName)) {
            mostrarTextoIzquierdo(LectorDocumentos.extraerTextoParaVista(baseName, bytesOriginal));
            lblTituloIzq.setText("Original (Referencia)");
        }

        // Panel derecho: si viene del pipeline Huffman+Hamming, los bytes decodificados
        // son Huffman-comprimidos, no texto legible todavía.
        boolean esPipelineHuffmanHamming = baseConHuf.toLowerCase().endsWith(".huf");

        if (esPipelineHuffmanHamming) {
            mostrarTextoDerecho("[datos Huffman desprotegidos — " + decodificado.length + " bytes]\n\nProcedé a Descompactar para obtener el archivo original.");
            log("Desprotegido OK. El contenido es Huffman-comprimido: usá 'Descompactar' para recuperar el original.", INFO);
        } else if (LectorDocumentos.esFormatoSoportado(baseName)) {
            String textoRecuperado = LectorDocumentos.extraerTextoParaVista(baseName, decodificado);

            if (!corregir && bytesOriginal != null && baseName.toLowerCase().endsWith(".txt")) {
                String textoOriginal = LectorDocumentos.extraerTextoParaVista(baseName, bytesOriginal);
                mostrarTextoConErrores(textoOriginal, textoRecuperado);
            } else {
                mostrarTextoDerecho(textoRecuperado);
                if (!corregir && !baseName.toLowerCase().endsWith(".txt")) {
                    log("Nota: Al no ser TXT plano, las corrupciones binarias pueden alterar la estructura visual.", WARNING);
                }
            }
        } else {
            mostrarTextoDerecho("[archivo binario — " + decodificado.length + " bytes]");
        }

        String modo = corregir ? "corrigiendo → .DCx" : "sin corregir → .DEx";
        log("Decodificado " + modo + ": " + fSalida.getName(), INFO);
        if (corregir && bytesOriginal != null) {
            boolean iguales = java.util.Arrays.equals(bytesOriginal, decodificado);
            log("Igual al original: " + iguales, iguales ? SUCCESS : DANGER);
        }
    }

    // =========================================================================
    // ACCIONES — HUFFMAN
    // =========================================================================

    private void accionCompactar() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());

        if (!LectorDocumentos.esFormatoSoportado(archivoActivo.getName()) && !esArchivoHamming(ext) && !esArchivoConError(ext) && !esArchivoConDobleError(archivoActivo.getName())) {
            log("ERROR: Solo se puede compactar documentos válidos o protegidos con Hamming.", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        bytesAntesDeCompactar = datos;
        String pathHuf = archivoActivo.getAbsolutePath() + ".huf";

        boolean ok = Huffman.encode(datos, pathHuf);
        if (!ok) { log("ERROR: No se pudo comprimir el archivo.", DANGER); return; }

        byte[] comprimido = FileManagement.readFile(pathHuf);
        archivoActivo = new File(pathHuf);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(comprimido != null ? comprimido.length + " bytes" : "—");

        mostrarTextoDerecho("[archivo comprimido Huffman — " + (comprimido != null ? comprimido.length : 0) + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());

        double reduccion = comprimido != null ? (1.0 - (double) comprimido.length / datos.length) * 100 : 0;
        log("Archivo compactado: " + archivoActivo.getName() + " (" + (comprimido != null ? comprimido.length : 0) + " bytes, -" + String.format("%.1f", reduccion) + "% tamaño)", GREEN_INFO);
    }

    private void accionDescompactar() {
        if (!verificarArchivoCargado()) return;
        String pathActual   = archivoActivo.getAbsolutePath();
        String nombreActual = archivoActivo.getName();

        // ── Detectar tipo de entrada ──────────────────────────────────────────
        // Caso A: .huf directo               → flujo simple Huffman
        // Caso B: .huf.DCx (corregido)       → pipeline OK, datos limpios
        // Caso C: .huf.DEx / .huf.H2x (con errores sin corregir) → datos posiblemente corruptos
        boolean esHufDirecto   = pathActual.toLowerCase().endsWith(".huf");
        String  baseDecodif    = FileManagement.getBaseName(nombreActual);
        boolean esDecodifDeHuf = !esHufDirecto && baseDecodif.toLowerCase().endsWith(".huf");

        if (!esHufDirecto && !esDecodifDeHuf) {
            log("ERROR: El archivo activo no es .huf ni un archivo desprotegido (.DCx/.DEx) que envuelva Huffman.", DANGER);
            return;
        }

        // Detectar si el archivo tiene errores sin corregir
        String extActual        = FileManagement.getExtension(nombreActual);
        boolean tieneErrores    = esArchivoConError(extActual)
                || esArchivoConDobleError(nombreActual)
                || extActual.equalsIgnoreCase(".DE1")
                || extActual.equalsIgnoreCase(".DE2")
                || extActual.equalsIgnoreCase(".DE3");

        if (tieneErrores) {
            int opcion = JOptionPane.showConfirmDialog(this,
                    "El archivo fue desprotegido SIN corregir errores.\n" +
                            "Los datos Huffman pueden estar corruptos.\n\n" +
                            "Intentar descompactar de todas formas permite ver\n" +
                            "el daño que producen los errores no corregidos.\n\n" +
                            "¿Continuar?",
                    "Advertencia — Datos con errores",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (opcion != JOptionPane.YES_OPTION) return;
        }

        byte[] datos = FileManagement.readFile(pathActual);
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        byte[] descomprimido = Huffman.decode(datos);

        // ── Calcular ruta de salida ───────────────────────────────────────────
        String baseReal;
        if (esDecodifDeHuf) {
            baseReal = FileManagement.getBaseName(FileManagement.getBaseName(pathActual));
        } else {
            baseReal = pathActual.substring(0, pathActual.length() - 4);
        }
        String nombreReal = new File(baseReal).getName(); // ej: "documento.txt"

        // ── Huffman falló completamente por la corrupción ─────────────────────
        if (descomprimido == null) {
            if (tieneErrores) {
                log("Descompresión FALLIDA: los errores corrompieron la estructura Huffman de manera irreparable.", DANGER);
                log("Conclusión: este archivo requería corrección de errores antes de poder descomprimirse.", WARNING);
                mostrarTextoDerecho(
                        "⚠ DESCOMPRESIÓN FALLIDA\n\n" +
                                "Los errores introducidos corrompieron la tabla o el\n" +
                                "stream de bits Huffman de manera irreparable.\n\n" +
                                "El decoder no pudo reconstruir el archivo.\n\n" );
                lblTituloDir.setText("⚠ Falló — " + nombreActual);
            } else {
                log("ERROR: No se pudo descomprimir el archivo.", DANGER);
            }
            return;
        }

        // ── Guardar resultado ─────────────────────────────────────────────────
        String pathDhu     = baseReal + ".dhu";
        boolean guardadoOk = FileManagement.writeFile(pathDhu, descomprimido);
        if (!guardadoOk) { log("ERROR: No se pudo guardar el archivo descomprimido.", DANGER); return; }

        archivoActivo = new File(pathDhu);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(descomprimido.length + " bytes");
        lblTituloDir.setText((tieneErrores ? "⚠ Corrupto — " : "Recuperado — ") + archivoActivo.getName());

        log("Archivo descompactado: " + archivoActivo.getName() + " (" + descomprimido.length + " bytes)",
                tieneErrores ? WARNING : GREEN_INFO);

        // ── Verificación de integridad ────────────────────────────────────────
        if (bytesOriginal != null) {
            boolean igualesAlOriginal = java.util.Arrays.equals(bytesOriginal, descomprimido);
            log("Recuperado igual al archivo original: " + (igualesAlOriginal ? "SÍ ✓" : "NO ✗ (hay diferencias por errores no corregidos)"),
                    igualesAlOriginal ? SUCCESS : DANGER);
        } else if (bytesAntesDeCompactar != null) {
            boolean iguales = java.util.Arrays.equals(bytesAntesDeCompactar, descomprimido);
            log("Descomprimido igual a la entrada de Huffman: " + iguales, iguales ? SUCCESS : DANGER);
        }

        // ── Visualización ─────────────────────────────────────────────────────
        if (LectorDocumentos.esFormatoSoportado(nombreReal)) {
            if (bytesOriginal != null) {
                mostrarTextoIzquierdo(LectorDocumentos.extraerTextoParaVista(nombreReal, bytesOriginal));
                lblTituloIzq.setText("Original");
            }
            if (tieneErrores && bytesOriginal != null && nombreReal.toLowerCase().endsWith(".txt")) {
                // Mostrar diff resaltando caracteres dañados en rojo
                String textoOriginal    = LectorDocumentos.extraerTextoParaVista(nombreReal, bytesOriginal);
                String textoRecuperado  = LectorDocumentos.extraerTextoParaVista(nombreReal, descomprimido);
                mostrarTextoConErrores(textoOriginal, textoRecuperado);
                log("Los caracteres en rojo muestran el daño producido por los errores no corregidos.", WARNING);
            } else {
                mostrarTextoDerecho(LectorDocumentos.extraerTextoParaVista(nombreReal, descomprimido));
            }
        } else {
            mostrarTextoDerecho("[archivo binario recuperado — " + descomprimido.length + " bytes]");
        }
    }

    // =========================================================================
    // ESTADÍSTICAS HAMMING
    // =========================================================================
    private void accionVerEstadisticasHamming() {
        if (!verificarArchivoCargado()) return;

        // ── Recopilar datos del archivo activo ────────────────────────────────
        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        String ext = FileManagement.getExtension(archivoActivo.getName());

        // Determinar si el archivo activo tiene estructura Hamming (encabezado de 12 bytes)
        boolean tieneEstructuraHamming =
                esArchivoHamming(ext) || esArchivoConError(ext) || esArchivoConDobleError(archivoActivo.getName());

        if (!tieneEstructuraHamming) {
            log("ERROR: Cargá un archivo Hamming (.HAx, .HEx, .H2x) para ver sus estadísticas.", DANGER);
            return;
        }

        // ── Leer encabezado Hamming ───────────────────────────────────────────
        int cantBloques   = leerIntDesde(datos, 0);
        int totalBitsInfo = leerIntDesde(datos, 4);
        int tamBloque     = Hamming.getTamBloque(blockIndexActivo);
        int bitsControl   = Hamming.getBitsControl(blockIndexActivo);
        int bitsInfo      = Hamming.getBitsInfo(blockIndexActivo);

        int bytesOrigEst  = (int) Math.ceil((double) totalBitsInfo / 8);
        int bytesCodif    = datos.length - 12;  // sin encabezado
        double overhead   = bytesOrigEst > 0
                ? (double) bytesCodif / bytesOrigEst * 100 - 100
                : 0;
        double eficiencia = (double) bitsInfo / tamBloque * 100;
        double redundancia= (double) bitsControl / tamBloque * 100;

        // ── Estadísticas de errores (si hay archivo original en memoria) ──────
        String seccionErrores = "";
        if (bytesOriginal != null) {
            // Intentar encontrar el archivo .HEx correspondiente
            String pathHE = FileManagement.buildErrorPath(archivoActivo.getAbsolutePath());
            byte[] conErrores = (pathHE != null) ? FileManagement.readFile(pathHE) : null;

            if (conErrores == null && (esArchivoConError(ext) || esArchivoConDobleError(archivoActivo.getName()))) {
                conErrores = datos; // el archivo activo ya ES el archivo con errores
            }

            if (conErrores != null) {
                // Necesitamos el .HAx original para comparar
                String pathHA = FileManagement.buildHammingPath(
                        FileManagement.getBaseName(archivoActivo.getAbsolutePath()), blockIndexActivo);
                byte[] hammingOriginal = FileManagement.readFile(pathHA);

                if (hammingOriginal != null) {
                    int modulosConError     = errorUtilities.contarModulosConError(hammingOriginal, conErrores, blockIndexActivo);
                    int modulosCon2Errores  = errorUtilities.contarModulosConDosErrores(hammingOriginal, conErrores, blockIndexActivo);
                    int modulosCon1Error    = modulosConError - modulosCon2Errores;
                    double pctAfectados     = cantBloques > 0 ? (double) modulosConError / cantBloques * 100 : 0;

                    seccionErrores = String.format(
                            "\n  ESTADÍSTICAS DE ERRORES\n  ──────────────────────────────────────────\n" +
                                    "  %-30s : %,d\n" +
                                    "  %-30s : %,d  (%.2f%%)\n" +
                                    "  %-30s : %,d\n" +
                                    "  %-30s : %,d\n",
                            "Total de módulos",         cantBloques,
                            "Módulos con error",        modulosConError, pctAfectados,
                            "  → con 1 error (corregibles)", modulosCon1Error,
                            "  → con 2 errores (detectables)", modulosCon2Errores
                    );
                }
            }
        }

        // ── Tabla comparativa de bloques ──────────────────────────────────────
        StringBuilder tablaComparativa = new StringBuilder();
        tablaComparativa.append("\n  COMPARATIVA ENTRE TAMAÑOS DE BLOQUE\n");
        tablaComparativa.append("  ──────────────────────────────────────────────────────────\n");
        tablaComparativa.append(String.format("  %-10s  %-8s  %-8s  %-8s  %-10s  %-10s\n",
                "Bloque", "Info", "Control", "Efic.%", "Redund.%", "Overhead%"));
        tablaComparativa.append("  ──────────────────────────────────────────────────────────\n");

        int[] bloques  = {FileManagement.BLOCK_8, FileManagement.BLOCK_1024, FileManagement.BLOCK_16384};
        String[] nombres = {"8 bits", "1024 bits", "16384 bits"};
        for (int i = 0; i < 3; i++) {
            int tb  = Hamming.getTamBloque(bloques[i]);
            int bc  = Hamming.getBitsControl(bloques[i]);
            int bi  = Hamming.getBitsInfo(bloques[i]);
            double ef  = (double) bi / tb * 100;
            double red = (double) bc / tb * 100;
            // Overhead teórico respecto a los bits de info
            double oh  = (double) bc / bi * 100;
            String marca = (bloques[i] == blockIndexActivo) ? " ◄" : "";
            tablaComparativa.append(String.format("  %-10s  %-8d  %-8d  %-8.2f  %-10.2f  %-10.2f%s\n",
                    nombres[i], bi, bc, ef, red, oh, marca));
        }

        // ── Construir reporte completo ────────────────────────────────────────
        String fechaStr = errorUtilities.getFechaAperturaString(datos);

        String reporte = String.format(
                "══════════════════════════════════════════════\n" +
                        "      ESTADÍSTICAS DE PROTECCIÓN HAMMING      \n" +
                        "══════════════════════════════════════════════\n\n" +
                        "  ARCHIVO\n  ──────────────────────────────────────────\n" +
                        "  %-30s : %s\n" +
                        "  %-30s : %s\n\n" +
                        "  PARÁMETROS DEL BLOQUE ACTIVO\n  ──────────────────────────────────────────\n" +
                        "  %-30s : %,d bits\n" +
                        "  %-30s : %,d bits\n" +
                        "  %-30s : %,d bits\n" +
                        "  %-30s : %.2f%%\n" +
                        "  %-30s : %.2f%%\n\n" +
                        "  TAMAÑOS DE ARCHIVO\n  ──────────────────────────────────────────\n" +
                        "  %-30s : %,d bytes\n" +
                        "  %-30s : %,d bytes\n" +
                        "  %-30s : +%.1f%%\n\n" +
                        "  ESTRUCTURA\n  ──────────────────────────────────────────\n" +
                        "  %-30s : %,d bloques\n" +
                        "  %-30s : %,d bits\n" +
                        "  %-30s : %s\n",
                "Archivo",           archivoActivo.getName(),
                "Fecha de apertura", fechaStr,
                "Tamaño de bloque",  tamBloque,
                "Bits de información", bitsInfo,
                "Bits de control (paridad)", bitsControl,
                "Eficiencia",        eficiencia,
                "Redundancia",       redundancia,
                "Original estimado", bytesOrigEst,
                "Codificado",        datos.length,
                "Overhead",          overhead,
                "Total de módulos",  cantBloques,
                "Bits de info totales", totalBitsInfo,
                "Corrección",        "1 error/bloque  |  Detección: 2 errores/bloque"
        );

        reporte += seccionErrores;
        reporte += tablaComparativa.toString();
        reporte += "══════════════════════════════════════════════\n";

        // ── Mostrar en diálogo ────────────────────────────────────────────────
        JTextArea txtStats = new JTextArea(reporte);
        txtStats.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtStats.setEditable(false);
        txtStats.setBackground(BG_BASE);
        txtStats.setForeground(TEXT_PRIMARY);
        txtStats.setBorder(new EmptyBorder(12, 14, 12, 14));

        JScrollPane scroll = new JScrollPane(txtStats);
        scroll.setPreferredSize(new Dimension(560, 460));
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_BASE);

        JDialog dialog = new JDialog(this, "Estadísticas Hamming", true);
        dialog.setBackground(BG_BASE);
        dialog.getContentPane().setBackground(BG_BASE);
        dialog.add(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        log("Estadísticas Hamming mostradas para: " + archivoActivo.getName(), INFO);
    }

    // Helper para leer un int big-endian desde un byte[]
    private int leerIntDesde(byte[] datos, int offset) {
        return ((datos[offset]     & 0xFF) << 24)
                | ((datos[offset + 1] & 0xFF) << 16)
                | ((datos[offset + 2] & 0xFF) <<  8)
                |  (datos[offset + 3] & 0xFF);
    }

    private void accionVerEstadisticas() {
        String pathHuf = archivoActivo != null ? archivoActivo.getAbsolutePath() : "";
        if (!pathHuf.toLowerCase().endsWith(".huf")) {
            if (archivoActivo != null) {
                pathHuf = archivoActivo.getAbsolutePath() + ".huf";
                if (!new File(pathHuf).exists()) {
                    pathHuf = FileManagement.buildHuffmanPath(archivoActivo.getAbsolutePath());
                }
            }
        }

        File fHuf = new File(pathHuf);
        if (!fHuf.exists()) {
            log("ERROR: No se encontró el archivo .huf. Primero compactá el archivo.", DANGER); return;
        }

        byte[] comprimido = FileManagement.readFile(pathHuf);
        if (comprimido == null) { log("ERROR: No se pudo leer el archivo .huf.", DANGER); return; }

        byte[] descomprimido = Huffman.decode(comprimido);
        if (descomprimido == null) { log("ERROR: No se pudo decodificar el archivo .huf.", DANGER); return; }

        byte[] referencia = (bytesAntesDeCompactar != null) ? bytesAntesDeCompactar : descomprimido;

        Map<Byte, String>  codigos     = Huffman.obtenerCodigos(referencia);
        Map<Byte, Integer> frecuencias = Huffman.obtenerFrecuencias(referencia);

        HuffmanUtils stats = new HuffmanUtils(referencia, comprimido, descomprimido, codigos, frecuencias);

        JTextArea txtStats = new JTextArea(stats.generarReporte());
        txtStats.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtStats.setEditable(false);
        txtStats.setBackground(BG_BASE);
        txtStats.setForeground(TEXT_PRIMARY);
        txtStats.setBorder(new EmptyBorder(12, 14, 12, 14));

        JScrollPane scroll = new JScrollPane(txtStats);
        scroll.setPreferredSize(new Dimension(520, 420));
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_BASE);

        JDialog dialog = new JDialog(this, "Estadísticas Huffman", true);
        dialog.setBackground(BG_BASE);
        dialog.getContentPane().setBackground(BG_BASE);
        dialog.add(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        log(stats.resumenCorto(), GREEN_INFO);
    }

    // =========================================================================
    // MOSTRAR TEXTO EN PANEL DERECHO
    // =========================================================================
    private void mostrarTextoIzquierdo(String texto) {
        txtIzquierdo.setText("");
        StyledDocument doc = txtIzquierdo.getStyledDocument();
        SimpleAttributeSet estilo = new SimpleAttributeSet();
        StyleConstants.setForeground(estilo, TEXT_PRIMARY);
        StyleConstants.setFontFamily(estilo, Font.MONOSPACED);
        StyleConstants.setFontSize(estilo, 13);
        StyleConstants.setBold(estilo, false);
        try {
            doc.insertString(0, texto, estilo);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        txtIzquierdo.setCaretPosition(0);
    }

    private void mostrarTextoDerecho(String texto) {
        txtDerecho.setText("");
        StyledDocument doc = txtDerecho.getStyledDocument();
        SimpleAttributeSet estilo = new SimpleAttributeSet();
        StyleConstants.setForeground(estilo, TEXT_PRIMARY);
        StyleConstants.setFontFamily(estilo, Font.MONOSPACED);
        StyleConstants.setFontSize(estilo, 13);
        StyleConstants.setBold(estilo, false);
        try {
            doc.insertString(0, texto, estilo);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        txtDerecho.setCaretPosition(0);
    }

    private void mostrarTextoConErrores(String textoOriginal, String textoConErrores) {
        txtDerecho.setText("");
        StyledDocument doc = txtDerecho.getStyledDocument();

        SimpleAttributeSet estiloNormal = new SimpleAttributeSet();
        StyleConstants.setForeground(estiloNormal, TEXT_PRIMARY);
        StyleConstants.setFontFamily(estiloNormal, Font.MONOSPACED);
        StyleConstants.setFontSize(estiloNormal, 13);
        StyleConstants.setBold(estiloNormal, false);

        SimpleAttributeSet estiloError = new SimpleAttributeSet();
        StyleConstants.setForeground(estiloError, DANGER);
        StyleConstants.setFontFamily(estiloError, Font.MONOSPACED);
        StyleConstants.setFontSize(estiloError, 13);
        StyleConstants.setBold(estiloError, true);

        for (int i = 0; i < textoConErrores.length(); i++) {
            char c = textoConErrores.charAt(i);
            boolean hayError = (i >= textoOriginal.length()) || (c != textoOriginal.charAt(i));
            try {
                doc.insertString(doc.getLength(), String.valueOf(c),
                        hayError ? estiloError : estiloNormal);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        txtDerecho.setCaretPosition(0);
    }

    // =========================================================================
    // HELPERS DE UI
    // =========================================================================

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
        btn.setBorder(new EmptyBorder(9, 14, 9, 14));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
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

    private void resaltarBotonBloque(int blockIndex) {
        btnHA1.setBackground(blockIndex == FileManagement.BLOCK_8     ? ACCENT_BLUE : BG_ELEVATED);
        btnHA2.setBackground(blockIndex == FileManagement.BLOCK_1024  ? ACCENT_BLUE : BG_ELEVATED);
        btnHA3.setBackground(blockIndex == FileManagement.BLOCK_16384 ? ACCENT_BLUE : BG_ELEVATED);
    }

    private boolean verificarArchivoCargado() {
        if (archivoActivo == null) { log("ERROR: Primero cargá un archivo.", DANGER); return false; }
        return true;
    }

    private boolean esArchivoHamming(String ext) {
        return ext.equalsIgnoreCase(".HA1") || ext.equalsIgnoreCase(".HA2") || ext.equalsIgnoreCase(".HA3");
    }

    private boolean esArchivoConError(String ext) {
        return ext.equalsIgnoreCase(".HE1") || ext.equalsIgnoreCase(".HE2") || ext.equalsIgnoreCase(".HE3");
    }

    private boolean esArchivoConDobleError(String name) {
        String upper = name.toUpperCase();
        return upper.contains(".H21") || upper.contains(".H22") || upper.contains(".H23") ||
                upper.contains(".P21") || upper.contains(".P22") || upper.contains(".P23");
    }

    // =========================================================================
    // ENTRY POINT
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
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