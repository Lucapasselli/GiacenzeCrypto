package com.giacenzecrypto.giacenze_crypto;


import javax.swing.*;
import javax.swing.text.*;
import java.io.*;

public class TextPaneOutputStream extends OutputStream {
    private final JTextPane textPane;

    public TextPaneOutputStream(JTextPane textPane) {
        this.textPane = textPane;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte)b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        String text = new String(b, off, len);
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = textPane.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), text, null);
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
}

