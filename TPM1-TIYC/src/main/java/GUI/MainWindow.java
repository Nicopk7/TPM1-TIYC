package GUI;

import hamming.errorUtilities;
import hamming.Hamming;
import hamming.file_mngmt.FileManagement;
import huffman.Huffman;
import huffman.HuffmanUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.util.Map;

/**
 * Ventana principal — Hamming + Huffman integrados.
 *
 * Layout:
 *   ┌─────────────┬──────────────────────────────┐
 *   │  SIDEBAR    │  TOPBAR                       │
 *   │  ARCHIVO    ├───────────────┬───────────────┤
 *   │  HAMMING    │ Panel izq.    │ Panel der.    │
 *   │  HUFFMAN    │ (original)    │ (resultado)   │
 *   │             ├───────────────┴───────────────┤
 *   │             │  LOG                          │
 *   └─────────────┴──────────────────────────────┘
 */
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
        super("Hamming + Huffman Codec — TPM1-TIYC");
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

        // ── ARCHIVO ──────────────────────────────────────────────────────────
        sidebar.add(sectionLabel("ARCHIVO"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(sideBtn("Cargar archivo", ACCENT_BLUE, e -> accionCargar()));
        sidebar.add(Box.createVerticalStrut(16));

        // ── HAMMING ───────────────────────────────────────────────────────────
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
        sidebar.add(sideBtn("Introducir ≤2 errores  →  .HEx",  BG_ELEVATED, e -> accionIntroducirErrores(true)));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Desproteger con errores  →  .DEx", BG_ELEVATED, e -> accionDecodificar(false)));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Desproteger corrigiendo  →  .DCx", BG_ELEVATED, e -> accionDecodificar(true)));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Establecer fecha apertura",         BG_ELEVATED, e -> accionFechaApertura()));
        sidebar.add(Box.createVerticalStrut(16));

        // ── HUFFMAN ───────────────────────────────────────────────────────────
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

    // ── VISOR DOBLE CON SCROLL ÚNICO ─────────────────────────────────────────
    /**
     * Construye el visor con scroll sincronizado.
     *
     * Cada panel tiene su propio JScrollPane pero comparten el mismo
     * BoundedRangeModel en la barra vertical → se mueven juntos.
     * Los headers quedan fijos arriba del scroll.
     * Cada panel ocupa exactamente el 50% del ancho disponible.
     */
    // ── VISOR DOBLE CON SCROLL SIMULTÁNEO MEJORADO ───────────────────────────
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
        // Desactivamos el scroll automático integrado en el panel izquierdo
        scrollIzq.setWheelScrollingEnabled(false);

        JScrollPane scrollDir = new JScrollPane(txtDerecho);
        scrollDir.setBorder(null);
        scrollDir.getViewport().setBackground(BG_BASE);
        scrollDir.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollDir.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // 1. Redirigir el uso de la rueda del mouse del panel izquierdo al derecho
        scrollIzq.addMouseWheelListener(e -> {
            scrollDir.dispatchEvent(SwingUtilities.convertMouseEvent(scrollIzq, e, scrollDir));
        });

        // 2. Sincronizar las posiciones de forma bidireccional e independiente
        JScrollBar barIzq = scrollIzq.getVerticalScrollBar();
        JScrollBar barDir = scrollDir.getVerticalScrollBar();

        // Bandera de control en un array de un elemento para evitar ciclos infinitos
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

    @SuppressWarnings("unused")
    private JPanel buildPanelDerecho() {
        // Mantenido solo para compatibilidad — ya no se usa
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_BASE);
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        header.setBackground(BG_SURFACE);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        JLabel titulo = label("Resultado", TEXT_PRIMARY, 12);
        titulo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lblTituloDir = label("—", TEXT_MUTED, 11);
        header.add(titulo);
        header.add(lblTituloDir);
        panel.add(header, BorderLayout.NORTH);

        txtDerecho = new JTextPane();
        txtDerecho.setBackground(BG_BASE);
        txtDerecho.setForeground(TEXT_PRIMARY);
        txtDerecho.setCaretColor(TEXT_PRIMARY);
        txtDerecho.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtDerecho.setEditable(false);
        txtDerecho.setBorder(new EmptyBorder(12, 14, 12, 14));

        panel.add(darkScroll(txtDerecho), BorderLayout.CENTER);
        return panel;
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
                "txt", "doc", "wp",
                "HA1", "HA2", "HA3", "HE1", "HE2", "HE3",
                "huf", "dhu"
        ));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        archivoActivo = chooser.getSelectedFile();
        byte[] datos  = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        String ext = FileManagement.getExtension(archivoActivo.getName());
        if (ext.equalsIgnoreCase(".txt") || ext.equalsIgnoreCase(".doc") || ext.equalsIgnoreCase(".wp")) {
            bytesOriginal = datos;
            mostrarTextoIzquierdo(new String(datos));
        } else if (ext.equalsIgnoreCase(".dhu")) {
            // Verificar si es texto o binario recuperado
            if (bytesOriginal != null && datos.length == bytesOriginal.length) {
                mostrarTextoIzquierdo(new String(datos));
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

        // --- MODIFICACIÓN PARA CUMPLIR CON EL PDF ---
        // Permitir archivos de texto o archivos compactados (.huf)
        if (!esTexto(ext) && !ext.equalsIgnoreCase(".huf")) {
            log("ERROR: Solo se puede proteger un archivo de texto o un archivo compactado (.huf)", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        // Mantenemos la extensión original sumando la de Hamming (ej: archivo.huf.HA1)
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
        // Permitir archivos descompactados .dhu también
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

        // Fallback por si la extensión original era .dhu y FileManagement devuelve null
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

        // Diálogo para ingresar fecha y hora
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

        // Auto-detectar índice de bloque desde el nombre del archivo para recuperar el contexto tras descompactar
        String nameUpper = archivoActivo.getName().toUpperCase();
        if (nameUpper.contains(".HA1") || nameUpper.contains(".HE1") || nameUpper.contains(".H21")) blockIndexActivo = FileManagement.BLOCK_8;
        else if (nameUpper.contains(".HA2") || nameUpper.contains(".HE2") || nameUpper.contains(".H22")) blockIndexActivo = FileManagement.BLOCK_1024;
        else if (nameUpper.contains(".HA3") || nameUpper.contains(".HE3") || nameUpper.contains(".H23")) blockIndexActivo = FileManagement.BLOCK_16384;
        actualizarBloqueLabel();
        resaltarBotonBloque(blockIndexActivo);

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        // Verificar fecha de apertura
        if (!errorUtilities.verificarFechaApertura(datos)) {
            String fechaStr = errorUtilities.getFechaAperturaString(datos);
            log("ERROR: Este archivo no se puede abrir hasta el " + fechaStr, DANGER);
            return;
        }

        // --- VENTANA EMERGENTE PARA ERRORES INCORREGIBLES ---
        if (corregir && esArchivoConDobleError(archivoActivo.getName())) {
            JOptionPane.showMessageDialog(this,
                    "Se han detectado módulos con 2 errores.\nHamming no puede corregirlos de manera confiable.\nSe mostrará el texto con los errores originales.",
                    "Errores Incorregibles", JOptionPane.WARNING_MESSAGE);
            corregir = false; // Forzamos a no corregir para que se muestren los errores en el panel
        }

        int cantOriginal    = (bytesOriginal != null) ? bytesOriginal.length : -1;
        byte[] decodificado = Hamming.decode(datos, blockIndexActivo, corregir, cantOriginal);

        String pathSalida = corregir
                ? FileManagement.saveDecodedCorrected(archivoActivo.getAbsolutePath(), decodificado)
                : FileManagement.saveDecodedError(archivoActivo.getAbsolutePath(), decodificado);

        // Fallback por si la extensión era .dhu y FileManagement devuelve null
        if (pathSalida == null) {
            String extDec = corregir ? FileManagement.EXT_DEC_CORR[blockIndexActivo] : FileManagement.EXT_DEC_ERR[blockIndexActivo];
            pathSalida = FileManagement.getBaseName(archivoActivo.getAbsolutePath()) + extDec;
            FileManagement.writeFile(pathSalida, decodificado);
        }

        String textoRecuperado = new String(decodificado);
        lblTituloDir.setText(new File(pathSalida).getName());
        if (bytesOriginal != null) {
            mostrarTextoIzquierdo(new String(bytesOriginal));
            lblTituloIzq.setText("Original (Referencia)");
        }

        if (!corregir && bytesOriginal != null) {
            mostrarTextoConErrores(new String(bytesOriginal), textoRecuperado);
        } else {
            mostrarTextoDerecho(textoRecuperado);
        }

        if (!corregir && bytesOriginal != null) {
            mostrarTextoConErrores(new String(bytesOriginal), textoRecuperado);
        } else {
            mostrarTextoDerecho(textoRecuperado);
        }

        String modo = corregir ? "corrigiendo → .DCx" : "sin corregir → .DEx";
        log("Decodificado " + modo + ": " + new File(pathSalida).getName(), INFO);
        if (corregir && bytesOriginal != null) {
            boolean iguales = new String(bytesOriginal).equals(textoRecuperado);
            log("Igual al original: " + iguales, iguales ? SUCCESS : DANGER);
        }
    }

    // =========================================================================
    // ACCIONES — HUFFMAN
    // =========================================================================

    private void accionCompactar() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());

        if (!esTexto(ext) && !esArchivoHamming(ext) && !esArchivoConError(ext) && !esArchivoConDobleError(archivoActivo.getName())) {
            log("ERROR: Solo se puede compactar archivos de texto o protegidos con Hamming.", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        // SE RESPALDAN LOS BYTES EXACTOS QUE ENTRAN A HUFFMAN
        bytesAntesDeCompactar = datos;

        String pathHuf = archivoActivo.getAbsolutePath() + ".huf";

        boolean ok = Huffman.encode(datos, pathHuf);
        if (!ok) { log("ERROR: No se pudo comprimir el archivo.", DANGER); return; }

        byte[] comprimido = FileManagement.readFile(pathHuf);
        archivoActivo = new File(pathHuf);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(comprimido != null ? comprimido.length + " bytes" : "—");

        mostrarTextoDerecho("[archivo景 comprimido Huffman — "
                + (comprimido != null ? comprimido.length : 0) + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());

        double reduccion = comprimido != null
                ? (1.0 - (double) comprimido.length / datos.length) * 100 : 0;
        log("Archivo compactado: " + archivoActivo.getName()
                + " (" + (comprimido != null ? comprimido.length : 0)
                + " bytes, -" + String.format("%.1f", reduccion) + "% tamaño)", GREEN_INFO);
    }

    private void accionDescompactar() {
        if (!verificarArchivoCargado()) return;
        String pathActual = archivoActivo.getAbsolutePath();
        if (!pathActual.toLowerCase().endsWith(".huf")) {
            log("ERROR: Seleccioná un archivo .huf para descompactar.", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(pathActual);
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        byte[] descomprimido = Huffman.decode(datos);
        if (descomprimido == null) { log("ERROR: No se pudo descomprimir el archivo.", DANGER); return; }

        String pathDhu = pathActual.substring(0, pathActual.length() - 4) + ".dhu";
        boolean guardadoOk = FileManagement.writeFile(pathDhu, descomprimido);
        if (!guardadoOk) { log("ERROR: No se pudo guardar el archivo descomprimido.", DANGER); return; }

        lblTituloDir.setText(new File(pathDhu).getName());
        archivoActivo = new File(pathDhu);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(descomprimido.length + " bytes");

        log("Archivo descompactado: " + archivoActivo.getName() + " (" + descomprimido.length + " bytes)", GREEN_INFO);

        // COMPARACIÓN RESPECTO AL ARCHIVO ENTRANTE DE HUFFMAN
        if (bytesAntesDeCompactar != null) {
            boolean iguales = java.util.Arrays.equals(bytesAntesDeCompactar, descomprimido);
            log("Huffman desc. igual a su entrada original: " + iguales, iguales ? SUCCESS : DANGER);
        }

        // Determinar el formato de visualización basándose en el nombre interno del archivo
        String nombreSinDhu = archivoActivo.getName().substring(0, archivoActivo.getName().length() - 4);
        String extOriginal = FileManagement.getExtension(nombreSinDhu);

        if (esTexto(extOriginal)) {
            mostrarTextoDerecho(new String(descomprimido));
        } else {
            mostrarTextoDerecho("[archivo binario protegido recuperado — " + descomprimido.length + " bytes]");
            log("Nota: El archivo descompactado contiene codificación Hamming. Procedé a desprotegerlo en el panel izquierdo.", INFO);
        }
    }

    private void accionVerEstadisticas() {
        // Buscamos la ruta del archivo .huf activo o su equivalente
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

        // ESTABLECER LA REFERENCIA REAL DE ENTRADA
        // Si la variable de sesión está vacía (ej. se cargó un .huf directo), asumimos que la
        // entrada es idéntica a la salida descomprimida debido a que Huffman es un algoritmo sin pérdidas.
        byte[] referencia = (bytesAntesDeCompactar != null) ? bytesAntesDeCompactar : descomprimido;

        // Calculamos las estadísticas basándonos en el archivo real que entró al compresor
        Map<Byte, String>  codigos     = Huffman.obtenerCodigos(referencia);
        Map<Byte, Integer> frecuencias = Huffman.obtenerFrecuencias(referencia);

        HuffmanUtils stats = new HuffmanUtils(
                referencia, // Se pasa el flujo correcto para evaluar la igualdad exacta bit a bit
                comprimido,
                descomprimido,
                codigos,
                frecuencias);

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

    private JScrollPane darkScroll(JComponent c) {
        JScrollPane scroll = new JScrollPane(c);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_BASE);
        scroll.getVerticalScrollBar().setBackground(BG_SURFACE);
        return scroll;
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

    private boolean esTexto(String ext) {
        return ext.equalsIgnoreCase(".txt") ||
                ext.equalsIgnoreCase(".doc") ||
                ext.equalsIgnoreCase(".wp");
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