package main;

import hamming.file_mngmt.FileManagement;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new GUI.MainWindow().setVisible(true);
        });
    }
}
