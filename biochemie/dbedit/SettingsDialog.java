package biochemie.dbedit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SettingsDialog extends JDialog {
  public static final int RET_CANCEL = 0;
  public static final int RET_OK = 1;
  
  public static boolean popup(Frame parent, SettingsPanel[] panels) {
    return new SettingsDialog(parent, panels).getReturnStatus() == RET_OK;
  }

  private JScrollPane mainScrollPane1;
  private JPanel mainPanel;
  private SettingsPanel[] panels;
  private JButton cancelButton;
  private JButton saveButton;
  // End of variables declaration                   

  private int returnStatus = RET_CANCEL;

  public SettingsDialog(Frame parent, SettingsPanel[] panels) {
    super(parent, parent == null ? ModalityType.TOOLKIT_MODAL : DEFAULT_MODALITY_TYPE);

    mainScrollPane1 = new JScrollPane();
    mainPanel = new JPanel();
    this.panels = panels;
    saveButton = new JButton("Save");
    cancelButton = new JButton("Cancel");

    setTitle("Settings");
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        close();
      }
    });

    mainScrollPane1.setBorder(BorderFactory.createEtchedBorder());
    mainScrollPane1.setViewportView(mainPanel);

    GroupLayout jPanel1Layout = new GroupLayout(mainPanel);
    GroupLayout.ParallelGroup horizontalGroup = jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, true);
    for (SettingsPanel settingsPanel : panels) {
      horizontalGroup.addComponent(settingsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE);
    }
    mainPanel.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(jPanel1Layout.createSequentialGroup().addContainerGap()
                                                                          .addGroup(horizontalGroup)
                                                                          .addContainerGap());
    GroupLayout.SequentialGroup verticalGroup = null;
    for (SettingsPanel settingsPanel : panels) {
      if (verticalGroup == null) {
        verticalGroup = jPanel1Layout.createSequentialGroup().addContainerGap();
      } else {
        verticalGroup.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED);
      }
      verticalGroup.addComponent(settingsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
    }
    jPanel1Layout.setVerticalGroup(verticalGroup.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

    saveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        save();
      }
    });
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        cancel();
      }
    });

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(mainScrollPane1)
                                    .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                    .addComponent(saveButton)
                                                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                    .addComponent(cancelButton)
                                                                                    .addContainerGap()));
    layout.setVerticalGroup(layout.createSequentialGroup().addComponent(mainScrollPane1)
                                                          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(saveButton)
                                                                                                                              .addComponent(cancelButton))
                                                          .addContainerGap());

    layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {cancelButton, saveButton});

    getRootPane().setDefaultButton(saveButton);

    pack();

    setMinimumSize(new Dimension(Math.min(getPreferredSize().width, GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width),
                                 Math.min(getPreferredSize().height, GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height)));

    // Close the dialog when Esc is pressed
    String cancelName = "cancel";
    InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
    ActionMap actionMap = getRootPane().getActionMap();
    actionMap.put(cancelName, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        cancel();
      }
    });
    
    setVisible(true);
  }

  public int getReturnStatus() {
    return returnStatus;
  }

  private void save() {
    for (SettingsPanel settingsPanel : panels) {
      settingsPanel.save();
    }
    doClose(RET_OK);
  }                                        

  private void cancel() {
    doClose(RET_CANCEL);
  }                                            

  private void close() {
    int ret = JOptionPane.NO_OPTION;
    for (SettingsPanel settingsPanel : panels) {
      if (settingsPanel.isChanged()) {
        ret = JOptionPane.showConfirmDialog(this, "Some parameters were changed, do you wish to save them?", "Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION,
                                                                                                                                JOptionPane.QUESTION_MESSAGE);
        break;
      }
    }
    switch (ret) {
      case JOptionPane.YES_OPTION:
        save();
      case JOptionPane.NO_OPTION:
        doClose(RET_CANCEL);
        return;
      default:
        break;
    }
  }                            
  
  private void doClose(int retStatus) {
    returnStatus = retStatus;
    setVisible(false);
    dispose();
  }


  public static void main(String args[]) {
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
    try {
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      java.util.logging.Logger.getLogger(JSettingsDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
    }
    //</editor-fold>

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        new SettingsDialog(null, new SettingsPanel[]{ biochemie.Defaults.getSettingsPanel(),
                                                      biochemie.linx.MeasurementCard.getSettingsPanel(),
                                                      biochemie.linx.ResultsCard.getSettingsPanel() });
        System.exit(0);
      }
    });
  }
}
