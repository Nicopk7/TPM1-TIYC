package GUI;

import hamming.errorUtilities;
import hamming.Hamming;
import hamming.file_mngmt.FileManagement;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;


public class MainWindow extends JFrame {


    // PALETA DARK

    private static final Color BG_BASE      = new Color(0x1E1E1E);
    private static final Color BG_SURFACE   = new Color(0x252526);
    private static final Color BG_ELEVATED  = new Color(0x2D2D2D);
    private static final Color BORDER       = new Color(0x3C3C3C);
    private static final Color TEXT_PRIMARY = new Color(0xD4D4D4);
    private static final Color TEXT_MUTED   = new Color(0x858585);
    private static final Color TEXT_HINT    = new Color(0x555555);
    private static final Color ACCENT_BLUE  = new Color(0x0E639C);
    private static final Color SUCCESS      = new Color(0x4EC9B0);
    private static final Color WARNING      = new Color(0xDCDCAA);
    private static final Color DANGER       = new Color(0xF44747);
    private static final Color INFO         = new Color(0x9CDCFE);


    // ESTADO

    private File   archivoActivo    = null;
    private int    blockIndexActivo = FileManagement.BLOCK_8;
    private byte[] bytesOriginal    = null; // bytes del .txt cargado, para comparar errores


    // COMPONENTES

    private JLabel    lblArchivoActivo;
    private JLabel    lblTamano;
    private JLabel    lblBloque;
    private JTextArea txtIzquierdo;  // panel original  — texto plano
    private JTextPane txtDerecho;    // panel resultado — soporta estilos por carácter
    private JLabel    lblTituloIzq;
    private JLabel    lblTituloDir;
    private JTextArea txtLog;

    private JButton btnHA1, btnHA2, btnHA3;


    // CONSTRUCTOR

    public MainWindow() {
        super("Hamming 2026 — TPM1-TIYC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 640);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_BASE);
        setLayout(new BorderLayout());

        add(buildSidebar(),  BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);

        log("Listo. Cargá un archivo .txt para comenzar.", TEXT_MUTED);
    }


    // SIDEBAR

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SURFACE);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));
        sidebar.setPreferredSize(new Dimension(220, 0));

        sidebar.add(Box.createVerticalStrut(16));

        sidebar.add(sectionLabel("ARCHIVO"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(sideBtn("Cargar .txt", ACCENT_BLUE, e -> accionCargar()));
        sidebar.add(Box.createVerticalStrut(16));

        sidebar.add(sectionLabel("PROTEGER"));
        sidebar.add(Box.createVerticalStrut(6));
        btnHA1 = sideBtn("Hamming 8 bits  →  .HA1",     BG_ELEVATED, e -> accionProteger(FileManagement.BLOCK_8));
        btnHA2 = sideBtn("Hamming 1024 bits  →  .HA2",  BG_ELEVATED, e -> accionProteger(FileManagement.BLOCK_1024));
        btnHA3 = sideBtn("Hamming 16384 bits  →  .HA3", BG_ELEVATED, e -> accionProteger(FileManagement.BLOCK_16384));
        sidebar.add(btnHA1);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnHA2);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnHA3);
        sidebar.add(Box.createVerticalStrut(16));

        sidebar.add(sectionLabel("OPERACIONES"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(sideBtn("Introducir errores  →  .HEx",      BG_ELEVATED, e -> accionIntroducirErrores()));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Desproteger con errores  →  .DEx", BG_ELEVATED, e -> accionDecodificar(false)));
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(sideBtn("Desproteger corrigiendo  →  .DCx", BG_ELEVATED, e -> accionDecodificar(true)));

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }


    // ÁREA PRINCIPAL

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
        bar.add(label("Bloque:", TEXT_MUTED, 12));
        lblBloque = label("8 bits (.HA1)", TEXT_PRIMARY, 12);
        bar.add(lblBloque);

        return bar;
    }

    // ── VISOR DOBLE ───────────────────────────────────────────────────────────
    private JSplitPane buildViewer() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildPanelIzquierdo(),
                buildPanelDerecho());
        split.setResizeWeight(0.5);
        split.setDividerSize(1);
        split.setBackground(BORDER);
        split.setBorder(null);
        return split;
    }


    private JPanel buildPanelIzquierdo() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_BASE);

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        header.setBackground(BG_SURFACE);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        JLabel titulo = label("Original", TEXT_PRIMARY, 12);
        titulo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lblTituloIzq = label("—", TEXT_MUTED, 11);
        header.add(titulo);
        header.add(lblTituloIzq);
        panel.add(header, BorderLayout.NORTH);

        // Área de texto
        txtIzquierdo = new JTextArea();
        txtIzquierdo.setBackground(BG_BASE);
        txtIzquierdo.setForeground(TEXT_PRIMARY);
        txtIzquierdo.setCaretColor(TEXT_PRIMARY);
        txtIzquierdo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtIzquierdo.setEditable(false);
        txtIzquierdo.setLineWrap(true);
        txtIzquierdo.setWrapStyleWord(true);
        txtIzquierdo.setBorder(new EmptyBorder(12, 14, 12, 14));

        panel.add(darkScroll(txtIzquierdo), BorderLayout.CENTER);
        return panel;
    }


    private JPanel buildPanelDerecho() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_BASE);

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        header.setBackground(BG_SURFACE);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        JLabel titulo = label("Recuperado", TEXT_PRIMARY, 12);
        titulo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lblTituloDir = label("—", TEXT_MUTED, 11);
        header.add(titulo);
        header.add(lblTituloDir);
        panel.add(header, BorderLayout.NORTH);

        // JTextPane: soporte de estilos por carácter
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
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        txtLog.setEditable(false);
        txtLog.setBorder(new EmptyBorder(8, 14, 8, 14));

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_SURFACE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }


    // ACCIONES


    private void accionCargar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos compatibles (*.txt, *.HA1, *.HA2, *.HA3, *.HE1, *.HE2, *.HE3)",
                "txt", "HA1", "HA2", "HA3", "HE1", "HE2", "HE3"
        ));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        archivoActivo = chooser.getSelectedFile();
        byte[] datos  = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        String ext = FileManagement.getExtension(archivoActivo.getName());
        if (ext.equalsIgnoreCase(".txt")) {
            bytesOriginal = datos;
            txtIzquierdo.setText(new String(datos));
            lblTituloIzq.setText(archivoActivo.getName());
        } else {
            txtIzquierdo.setText("[archivo binario — " + datos.length + " bytes]");
            lblTituloIzq.setText(archivoActivo.getName());
        }

        limpiarDerecho();
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(datos.length + " bytes");
        log("Archivo cargado: " + archivoActivo.getName() + " (" + datos.length + " bytes)", SUCCESS);
    }

    private void accionProteger(int blockIndex) {
        if (!verificarArchivoCargado()) return;
        if (!FileManagement.getExtension(archivoActivo.getAbsolutePath()).equalsIgnoreCase(".txt")) {
            log("ERROR: Solo se puede proteger un archivo .txt", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        byte[] codificado = Hamming.encode(datos, blockIndex);
        String pathHA     = FileManagement.saveHammingFile(archivoActivo.getAbsolutePath(), blockIndex, codificado);
        if (pathHA == null) { log("ERROR: No se pudo guardar el archivo protegido.", DANGER); return; }

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

    private void accionIntroducirErrores() {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!esArchivoHamming(ext)) {
            log("ERROR: Seleccioná primero un archivo .HAx para introducir errores.", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        byte[] conErrores = errorUtilities.injectErrors(datos, blockIndexActivo);
        String pathHE     = FileManagement.saveErrorFile(archivoActivo.getAbsolutePath(), conErrores);
        if (pathHE == null) { log("ERROR: No se pudo guardar el archivo con errores.", DANGER); return; }

        archivoActivo = new File(pathHE);
        lblArchivoActivo.setText(archivoActivo.getName());
        lblTamano.setText(conErrores.length + " bytes");
        mostrarTextoDerecho("[archivo con errores — " + conErrores.length + " bytes]");
        lblTituloDir.setText(archivoActivo.getName());

        log("Errores introducidos: " + archivoActivo.getName(), WARNING);
        log(errorUtilities.resumenErrores(datos, conErrores, blockIndexActivo), WARNING);
    }

    private void accionDecodificar(boolean corregir) {
        if (!verificarArchivoCargado()) return;
        String ext = FileManagement.getExtension(archivoActivo.getAbsolutePath());
        if (!esArchivoHamming(ext) && !esArchivoConError(ext)) {
            log("ERROR: Seleccioná un archivo .HAx o .HEx para decodificar.", DANGER); return;
        }

        byte[] datos = FileManagement.readFile(archivoActivo.getAbsolutePath());
        if (datos == null) { log("ERROR: No se pudo leer el archivo.", DANGER); return; }

        int cantOriginal    = (bytesOriginal != null) ? bytesOriginal.length : -1;
        byte[] decodificado = Hamming.decode(datos, blockIndexActivo, corregir, cantOriginal);

        String pathSalida = corregir
                ? FileManagement.saveDecodedCorrected(archivoActivo.getAbsolutePath(), decodificado)
                : FileManagement.saveDecodedError(archivoActivo.getAbsolutePath(), decodificado);

        if (pathSalida == null) { log("ERROR: No se pudo guardar el archivo decodificado.", DANGER); return; }

        String textoRecuperado = new String(decodificado);
        lblTituloDir.setText(new File(pathSalida).getName());

        if (!corregir && bytesOriginal != null) {
            // ← aquí se colorean en rojo los caracteres con error
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


    // MOSTRAR TEXTO EN PANEL DERECHO (JTextPane)



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

        // Estilo para caracteres correctos
        SimpleAttributeSet estiloNormal = new SimpleAttributeSet();
        StyleConstants.setForeground(estiloNormal, TEXT_PRIMARY);
        StyleConstants.setFontFamily(estiloNormal, Font.MONOSPACED);
        StyleConstants.setFontSize(estiloNormal, 13);
        StyleConstants.setBold(estiloNormal, false);

        // Estilo para caracteres con error → ROJO + negrita
        SimpleAttributeSet estiloError = new SimpleAttributeSet();
        StyleConstants.setForeground(estiloError, DANGER);
        StyleConstants.setFontFamily(estiloError, Font.MONOSPACED);
        StyleConstants.setFontSize(estiloError, 13);
        StyleConstants.setBold(estiloError, true);

        for (int i = 0; i < textoConErrores.length(); i++) {
            char c = textoConErrores.charAt(i);

            // Hay error si la posición no existe en el original o el carácter es distinto
            boolean hayError = (i >= textoOriginal.length()) || (c != textoOriginal.charAt(i));

            try {
                doc.insertString(doc.getLength(),
                        String.valueOf(c),
                        hayError ? estiloError : estiloNormal);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        txtDerecho.setCaretPosition(0);
    }


    // HELPERS DE UI


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
                new EmptyBorder(1, 6, 1, 6)
        ));
        return lbl;
    }

    private JScrollPane darkScroll(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
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
        if (archivoActivo == null) {
            log("ERROR: Primero cargá un archivo.", DANGER);
            return false;
        }
        return true;
    }

    private boolean esArchivoHamming(String ext) {
        return ext.equalsIgnoreCase(".HA1") ||
                ext.equalsIgnoreCase(".HA2") ||
                ext.equalsIgnoreCase(".HA3");
    }

    private boolean esArchivoConError(String ext) {
        return ext.equalsIgnoreCase(".HE1") ||
                ext.equalsIgnoreCase(".HE2") ||
                ext.equalsIgnoreCase(".HE3");
    }


    // ENTRY POINT

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // FlatLaf dark — respeta setBackground y setOpaque correctamente
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
            } catch (Exception e) {
                // Fallback a Metal puro si FlatLaf no está en el classpath
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