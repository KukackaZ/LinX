package biochemie.dbedit;

import biochemie.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;

/**
 *
 * @author Janek
 */
public class ProteasesDialog extends JDialog {
  /**
   * A return status code - returned if Cancel button has been pressed
   */
  public static final int RET_CANCEL = 0;
  /**
   * A return status code - returned if OK button has been pressed
   */
  public static final int RET_OK = 1;

  public static boolean showProteasesDialog(Frame parent) {
    return (new ProteasesDialog(parent)).getReturnStatus() == RET_OK;
  }

  private ButtonGroup buttonGroup;
  private JButton closeButton;
  private JButton topButton;
  private JButton upButton;
  private JButton onButton;
  private JButton downButton;
  private JButton endButton;
  private JButton editButton;
  private JButton addButton;
  private JButton removeButton;
  private LinkedHashMap<String, String[]> proteases;
  private JPanel proteasesPanel;
  private JScrollPane jScrollPane;
  private JSeparator jSeparator1;
  private JSeparator jSeparator2;
  private int returnStatus = RET_CANCEL;

  /** Creates new form NewJFrame */
  public ProteasesDialog(Frame parent) {
    super(parent, true);

    jScrollPane = new JScrollPane();
    proteasesPanel = new JPanel();
    topButton = new JButton();
    upButton = new JButton();
    onButton = new JButton();
    downButton = new JButton();
    endButton = new JButton();
    jSeparator1 = new JSeparator();
    editButton = new JButton();
    addButton = new JButton();
    removeButton = new JButton();
    jSeparator2 = new JSeparator();
    closeButton = new JButton();

    initialize(null);
    jScrollPane.setViewportView(proteasesPanel);

    topButton.setText("Move to top");
    topButton.setMnemonic('T');
    topButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        topButtonActionPerformed(evt);
      }
    });

    upButton.setText("Move up");
    upButton.setMnemonic('U');
    upButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        upButtonActionPerformed(evt);
      }
    });

    onButton.setText("Move on");
    onButton.setMnemonic('O');
    onButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onButtonActionPerformed(evt);
      }
    });

    downButton.setText("Move down");
    downButton.setMnemonic('D');
    downButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downButtonActionPerformed(evt);
      }
    });

    endButton.setText("Move to end");
    endButton.setMnemonic('E');
    endButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        endButtonActionPerformed(evt);
      }
    });

    editButton.setText("Edit protease");
    editButton.setMnemonic('d');
    editButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        editButtonActionPerformed(evt);
      }
    });

    addButton.setText("Add protease");
    addButton.setMnemonic('A');
    addButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        addButtonActionPerformed(evt);
      }
    });

    removeButton.setText("Remove protease");
    removeButton.setMnemonic('R');
    removeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        removeButtonActionPerformed(evt);
      }
    });

    closeButton.setFont(new Font("Tahoma", 1, 14)); // NOI18N
    closeButton.setText("Close");
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        closeButtonActionPerformed(evt);
      }
    });

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        closeDialog(evt);
      }
    });

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jScrollPane)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
          .addComponent(topButton)
          .addComponent(upButton)
          .addComponent(onButton)
          .addComponent(downButton)
          .addComponent(endButton)
          .addComponent(jSeparator1)
          .addComponent(editButton)
          .addComponent(addButton)
          .addComponent(removeButton)
          .addComponent(jSeparator2)
          .addComponent(closeButton))
        .addContainerGap())
    );

    layout.linkSize(SwingConstants.HORIZONTAL, topButton, upButton, onButton, downButton, endButton, jSeparator1, editButton, addButton, closeButton, jSeparator2, removeButton);

    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(topButton)
            .addComponent(upButton)
            .addComponent(onButton)
            .addComponent(downButton)
            .addComponent(endButton)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(editButton)
            .addComponent(addButton)
            .addComponent(removeButton)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jSeparator2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(closeButton))
          .addComponent(jScrollPane))
        .addContainerGap())
    );

    getRootPane().setDefaultButton(closeButton);
    setName("Edit.Proteases");
    setTitle("Edit proteases");
    pack();
    setMinimumSize(new Dimension(Math.max(getMinimumSize().width, 300), getMinimumSize().height));
    Defaults.setWindowDefaults(this, Defaults.getDefaults(getName()));
    revalidate();

    setVisible(true);
  }

  private void initialize(String selected) {
    buttonGroup = new ButtonGroup();
    proteases = new LinkedHashMap<>(0);

    try {
      proteases = ProteasesFile.getProteases();
    } catch (IOException e) {
      if (JOptionPane.showConfirmDialog(this, "File couldn't be loaded, do you want to overwrite it?", "IO Exception", JOptionPane.YES_NO_OPTION,
                                        JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION) {
        doClose();
        return;
      }
    }
    proteasesPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    boolean sep = false;
    for (String name : proteases.keySet()) {
      if (sep) {
        JSeparator jSeparator = new JSeparator(JSeparator.HORIZONTAL);
        jSeparator.setMinimumSize(new Dimension(1, 1));
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 1;
        proteasesPanel.add(jSeparator, gbc);
      } else {
        sep = true;
      }

      JRadioButton jRadioButton = new JRadioButton();
      buttonGroup.add(jRadioButton);
      jRadioButton.setText(name);
      jRadioButton.setActionCommand(name);
      if (selected != null && selected.compareTo(name) == 0) {
        jRadioButton.setSelected(true);
      }
      gbc.anchor = GridBagConstraints.NORTHWEST;
      gbc.gridwidth = 1;
      gbc.insets = new Insets(0, 5, 0, 5);
      gbc.weightx = 0.5;
      gbc.weighty = 0;
      proteasesPanel.add(jRadioButton, gbc);

      RulesPanel rulesPanel = new RulesPanel(proteases.get(name)[0]);
      gbc.gridx = 1;
      gbc.insets = new Insets(3, 5, 1, 5);
      gbc.weightx = 0.5;
      proteasesPanel.add(rulesPanel, gbc);

      JLabel jLabel = new JLabel(Protease.formatMods(proteases.get(name)[1], false));
      jLabel.setToolTipText(Protease.formatMods(proteases.get(name)[1], true));
      gbc.gridx = 2;
      gbc.insets = new Insets(0, 0, 0, 5);
      gbc.weightx = 1;
      proteasesPanel.add(jLabel, gbc);
    }
  }

  /**
   * @return the return status of this dialog - one of RET_OK or RET_CANCEL
   */
  public int getReturnStatus() {
    return returnStatus;
  }

  private void topButtonActionPerformed(ActionEvent evt) {
    if (isSelected()) {
      try {
        ProteasesFile.moveTop(buttonGroup.getSelection().getActionCommand());
        updateList(buttonGroup.getSelection().getActionCommand());
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void upButtonActionPerformed(ActionEvent evt) {
    if (isSelected()) {
      try {
        ProteasesFile.moveUp(buttonGroup.getSelection().getActionCommand());
        updateList(buttonGroup.getSelection().getActionCommand());
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void onButtonActionPerformed(ActionEvent evt) {
    if (isSelected()) {
      try {
        String text = buttonGroup.getSelection().getActionCommand();
        String[] poss = new String[proteases.size()-1];
        String first = null;
        int i = 1;
        int j = 0;
        for (String name : proteases.keySet()) {
          if (name.compareTo(text) != 0) {
            if (first == null) {
              poss[j++] = i + " (before " + name + ")";
            } else if (first.compareTo(text) != 0) {
              poss[j++] = i + " (between " + first + " and " + name + ")";
            }
            i++;
          }
          first = name;
        }
        if (first.compareTo(text) != 0) {
          poss[j++] = i + " (after " + first + ")";
        }
        Object ret = JOptionPane.showInputDialog(this, "Select the position to which you want to move the rule.",
                                              "Specify position", JOptionPane.PLAIN_MESSAGE, null, poss, null);
        if (ret != null) {
          ProteasesFile.moveOn(text, Integer.parseInt(ret.toString().split(" ", 2)[0])-1);
          updateList(buttonGroup.getSelection().getActionCommand());
        }
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void downButtonActionPerformed(ActionEvent evt) {
    if (isSelected()) {
      try {
        ProteasesFile.moveDown(buttonGroup.getSelection().getActionCommand());
        updateList(buttonGroup.getSelection().getActionCommand());
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void endButtonActionPerformed(ActionEvent evt) {
    if (isSelected()) {
      try {
        ProteasesFile.moveEnd(buttonGroup.getSelection().getActionCommand());
        updateList(buttonGroup.getSelection().getActionCommand());
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void addButtonActionPerformed(ActionEvent evt) {
    String added = ProteasesFile.addProtease(this);
    if (added != null) {
      updateList(added);
    }
  }

  private void editButtonActionPerformed(ActionEvent evt) {
    if (isSelected()) {
      String added = ProteasesFile.editProtease(this, buttonGroup.getSelection().getActionCommand(), proteases.get(buttonGroup.getSelection().getActionCommand())[0],
                                                proteases.get(buttonGroup.getSelection().getActionCommand())[1]);
      if (added != null) {
        updateList(added);
      }
    }
  }

  private void removeButtonActionPerformed(ActionEvent evt) {
    if (isSelected()) {
      if (ProteasesFile.removeProtease(this, buttonGroup.getSelection().getActionCommand())) {
        updateList(null);
      }
    }
  }

  private boolean isSelected() {
    if (buttonGroup.getSelection() == null) {
      JOptionPane.showMessageDialog(this, "Please select edited protease first.", "Select protease", JOptionPane.WARNING_MESSAGE);
      return false;
    }
    return true;
  }

  private void updateList(String selected) {
    returnStatus = RET_OK;
    proteasesPanel.removeAll();
    initialize(selected);
    proteasesPanel.revalidate();
    proteasesPanel.repaint();
    jScrollPane.revalidate();
    jScrollPane.repaint();
  }

  private void closeButtonActionPerformed(ActionEvent evt) {
    doClose();
  }

  /**
   * Closes the dialog
   */
  private void closeDialog(WindowEvent evt) {
    doClose();
  }

  private void doClose() {
    Defaults.addDefaults(getName(), Defaults.putWindowDefaults(this, new Properties()));
    setVisible(false);
    dispose();
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
    try {
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//        System.out.println(info.getName());
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception ex) {
      Logger.getLogger(ProteasesDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        ProteasesDialog pd = null;
        try {
          pd = new ProteasesDialog(null);
        } catch (Throwable e) {
          try (PrintWriter pw = new PrintWriter(new FileWriter("error.txt", true))) {
            pw.println(new java.util.Date().toString());
            pw.println("--------------------------------");
            pw.println(e.getMessage());
            pw.println("--------------------------------");
            e.printStackTrace(pw);
            pw.println("--------------------------------");
            if (pd != null) {
              for (String key : pd.proteases.keySet()) {
                pw.append(key).append(":");
                for (String rule : pd.proteases.get(key)) {
                  pw.append(' ').append(rule);
                }
                pw.println();
              }
            }
            pw.append("################################################################" + System.lineSeparator());
            pw.append(System.lineSeparator());
            pw.flush();
          } catch (Exception f) {
            Logger.getLogger(ProteasesDialog.class.getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(ProteasesDialog.class.getName()).log(Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(pd, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
