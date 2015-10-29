
package personal;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.SQLException;

public class gui extends javax.swing.JFrame implements KeyListener {
    Esquema esquema;
    
    public gui(Esquema e) {
        initComponents();
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        this.esquema = e;
        this.commLine.addKeyListener(this);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        commLine = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        outArea = new javax.swing.JEditorPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        helpArea = new javax.swing.JEditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ADMINISTRACION PERSONAL");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        commLine.setFont(new java.awt.Font("Ubuntu", 0, 24)); // NOI18N
        commLine.setName("commandLine"); // NOI18N
        commLine.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                commLineKeyTyped(evt);
            }
        });

        outArea.setContentType("text/html"); // NOI18N
        outArea.setName("outArea"); // NOI18N
        jScrollPane3.setViewportView(outArea);

        helpArea.setContentType("text/html"); // NOI18N
        jScrollPane2.setViewportView(helpArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(commLine, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 896, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(commLine, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void commLineKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_commLineKeyTyped
        String txt = this.commLine.getText() + evt.getKeyChar();
        txt = txt.toUpperCase();
        try {
            if (esquema.validarComando(txt, false)) {
                this.helpArea.setText(this.esquema.getCmdHelp(txt));
            }
        } catch (SQLException ex) {
            this.outArea.setText(ex.toString());
        }
    }//GEN-LAST:event_commLineKeyTyped

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        this.commLine.requestFocus();  
    }//GEN-LAST:event_formWindowOpened

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField commLine;
    private javax.swing.JEditorPane helpArea;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JEditorPane outArea;
    // End of variables declaration//GEN-END:variables

    @Override
    public void keyTyped(KeyEvent e) {
        char charecter = e.getKeyChar();
        if (Character.isLowerCase(charecter)) {
            e.setKeyChar(Character.toUpperCase(charecter));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER) {
            String txt = this.commLine.getText();
            if (esquema.validarComando(txt, true) == Esquema.VALID) {
                outArea.setText("");
                String out = this.esquema.processCommand(txt);
                this.outArea.setText(out);
                this.commLine.setText("");
            }
        }
    }
        
}