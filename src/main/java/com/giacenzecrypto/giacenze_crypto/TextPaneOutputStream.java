package com.giacenzecrypto.giacenze_crypto;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.io.IOException;
import java.io.OutputStream;

public class TextPaneOutputStream extends OutputStream {
    private final JTextPane textPane;
    private final StringBuilder buffer = new StringBuilder();
    private final int maxBufferSize = 4096;  // Aggiorna ogni 4 KB
    private final int maxDocumentLength = 5000; // Mantieni max 50k caratteri nel textpane

    public TextPaneOutputStream(JTextPane textPane) {
        this.textPane = textPane;

        // Avvia un timer Swing per flush periodico (ogni 100ms)
        new Timer(100, e -> flushBufferToTextPane()).start();
    }

    @Override
    public synchronized void write(int b) throws IOException {
        buffer.append((char) b);
        if (buffer.length() >= maxBufferSize) {
            flushBufferToTextPane();
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        buffer.append(new String(b, off, len));
        if (buffer.length() >= maxBufferSize) {
            flushBufferToTextPane();
        }
    }

    private synchronized void flushBufferToTextPane() {
        if (buffer.length() == 0) return;

        final String text = buffer.toString();
        buffer.setLength(0);

        SwingUtilities.invokeLater(() -> {
            try {
                // Aggiunge testo al documento
                textPane.getDocument().insertString(
                        textPane.getDocument().getLength(),
                        text,
                        null
                );

                // Se supera maxDocumentLength, taglia le prime righe
                if (textPane.getDocument().getLength() > maxDocumentLength) {
                    textPane.getDocument().remove(
                            0,
                            textPane.getDocument().getLength() - maxDocumentLength
                    );
                }

                // Scroll automatico verso il basso
                textPane.setCaretPosition(textPane.getDocument().getLength());
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });
    }
}


