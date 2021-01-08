package biochemie.dbedit;

import biochemie.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Janek
 */
public class ProteaseDialog extends JDialog {
  private JLabel jLabel1;
  private String previousName;
  private JTextField nameField;
  private JLabel jLabel2;
  private JScrollPane jScrollPane1;
  private JTextArea rulesArea;
  private JCheckBox simpleCheckBox;
  private JPanel advancedPanel;
  private JLabel leftLabel;
  private JLabel rightLabel;
  private JTextField leftTextField;
  private JTextField rightTextField;
  private JCheckBox leftCheckBox;
  private JCheckBox rightCheckBox;
  private JButton okButton;
  private JButton cancelButton;
  private String added = null;

  /**
   * Creates new pre-filled form ProteaseDialog.
   * @param parent The owner Dialog from which the dialog is displayed or <code>null</code> if this dialog has no owner.
   * @param modal Specifies whether dialog blocks user input to other top-level windows when shown.
   * If <code>true</code>, the modality type property is set to <code>DEFAULT_MODALITY_TYPE</code>, otherwise the dialog is modeless.
   * @param name Name of protease.
   * @param rules Array of rules.
   */
  public ProteaseDialog(java.awt.Window parent, String name, String rules, String modifications) {
    super(parent, DEFAULT_MODALITY_TYPE);

    jLabel1 = new JLabel("Name of protease:");
    previousName = name.toUpperCase();
    nameField = new JTextField(name);
    jLabel2 = new JLabel("Cleavage rules:");
    jScrollPane1 = new JScrollPane();
    rulesArea = new JTextArea();
    simpleCheckBox = new JCheckBox("Simple protease", false);
    advancedPanel = new JPanel();
    leftLabel = new JLabel(" Left side:");
    rightLabel = new JLabel(" Right side:");
    leftTextField = new JTextField();
    rightTextField = new JTextField();
    leftCheckBox = new JCheckBox("Lock C-terminus");
    rightCheckBox = new JCheckBox("Lock N-terminus");
    okButton = new JButton();
    cancelButton = new JButton();

    setName("Edit.Protease");
    setTitle((name.compareTo("") == 0 ? "Add" : "Edit") + " protease");
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        closeDialog(evt);
      }
    });

    nameField.setFont(new java.awt.Font("Courier New", 0, 12));

    rulesArea.setColumns(16);
    rulesArea.setFont(new java.awt.Font("Courier New", 0, 12));
    rulesArea.setRows(1);
    rulesArea.setTabSize(4);
    rulesArea.setText(rules.replace(" ", "\n"));
    jScrollPane1.setViewportView(rulesArea);

    simpleCheckBox.setMnemonic('S');
    simpleCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        advancedPanel.setVisible(!simpleCheckBox.isSelected());
        setMinimumSize(null);
        setPreferredSize(null);
        setMinimumSize(getPreferredSize());
      }
    });

    if (modifications.isEmpty()) {
      simpleCheckBox.doClick();
    } else {
      String[] s = modifications.split(";", 2);
      if (s[0].contains(Protease.LOCK)) {
        leftCheckBox.setSelected(true);
        s[0] = s[0].replace(Protease.LOCK, "");
      }
      try {
        leftTextField.setText(Defaults.sMassFullFormat.format(new BigDecimal(s[0])));
      } catch (Exception e) {
        leftTextField.setText(s[0]);
      }
      if (s[1].contains(Protease.LOCK)) {
        s[1] = s[1].replace(Protease.LOCK, "");
        rightCheckBox.setSelected(true);
      }
      try {
        rightTextField.setText(Defaults.sMassFullFormat.format(new BigDecimal(s[1])));
      } catch (Exception e) {
        rightTextField.setText(s[1]);
      }
    }

    GroupLayout advancedLayout = new GroupLayout(advancedPanel);
    advancedPanel.setLayout(advancedLayout);
    advancedLayout.setHorizontalGroup(
      advancedLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(advancedLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        .addGroup(advancedLayout.createSequentialGroup()
          .addGroup(advancedLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(leftLabel)
            .addComponent(leftTextField)
            .addComponent(leftCheckBox))
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addGroup(advancedLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(rightLabel)
            .addComponent(rightTextField)
            .addComponent(rightCheckBox)))));

    advancedLayout.setVerticalGroup(
      advancedLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(GroupLayout.Alignment.TRAILING, advancedLayout.createSequentialGroup()
        .addGroup(advancedLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(leftLabel)
          .addComponent(rightLabel))
        .addGroup(advancedLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(leftTextField)
          .addComponent(rightTextField))
        .addGroup(advancedLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(leftCheckBox)
          .addComponent(rightCheckBox))
        .addContainerGap()));

    advancedPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Modifications of termini", TitledBorder.LEADING, TitledBorder.TOP));

    okButton.setText("OK");
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        okButtonActionPerformed(evt);
      }
    });

    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        cancelButtonActionPerformed(evt);
      }
    });

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(jLabel2)
              .addComponent(jLabel1))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(jScrollPane1, GroupLayout.Alignment.TRAILING)
              .addComponent(nameField)))
          .addComponent(advancedPanel)
          .addGroup(layout.createSequentialGroup()
            .addComponent(simpleCheckBox)
            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, 0, Short.MAX_VALUE)
            .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(cancelButton)))
        .addContainerGap()));

    layout.linkSize(SwingConstants.HORIZONTAL, new java.awt.Component[]{cancelButton, okButton});

    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(nameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel1))
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addComponent(jLabel2)
          .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE))
        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        .addComponent(advancedPanel)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(simpleCheckBox)
          .addComponent(cancelButton)
          .addComponent(okButton))
        .addContainerGap()));

    getRootPane().setDefaultButton(okButton);

    pack();
    setMinimumSize(getPreferredSize());
    Defaults.setWindowDefaults(this, Defaults.getDefaults(getName()));
    revalidate();

    // Close the dialog when Esc is pressed
    String cancelName = "cancel";
    InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
    ActionMap actionMap = getRootPane().getActionMap();
    actionMap.put(cancelName, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        doClose(false);
      }
    });
  }

  /**
   * @return the return status of this dialog - one of RET_OK or RET_CANCEL
   */
  public String getAdded() {
    return added;
  }

  private String toString(ArrayList<String> rules) {
    Iterator<String> it = rules.iterator();
    if (!it.hasNext()) {
      return "";
    }
    StringBuilder ret = new StringBuilder(it.next());
    while (it.hasNext()) {
      ret.append("\n" + it.next());
    }
    return ret.toString();
  }

  private void okButtonActionPerformed(ActionEvent evt) {
    try {
      // Ošetření vstupu.
      String[] inp = rulesArea.getText().split("\\n");
      ArrayList<String> ruleField = new ArrayList<>(inp.length);
      for (int i = 0; i < inp.length; i++) {
        if (!contains(ruleField, inp[i])) {
          ruleField.add(inp[i]);
        }
      }
      for (String rule : ruleField) {
        Protease.checkProtease(rule);
      }
      String[] def = new String[] { "", "" };

      if (nameField.getText().compareTo("") == 0) {
        throw new java.lang.NullPointerException();
      }
      if (nameField.getText().contains(" ")) {
        JOptionPane.showMessageDialog(this, "Name couldn't contains space.", "Invalid name", JOptionPane.ERROR_MESSAGE);
        throw new java.util.InputMismatchException();
      }

      for (JTextField jTextField : new JTextField[] { leftTextField, rightTextField }) {
        try {
          ChemicalElement.evaluate(jTextField.getText());
        } catch (Exception e) {
          try {
            jTextField.setText(Defaults.sMassFullFormat.format(Defaults.sMassFullFormat.parse(jTextField.getText())));
          } catch (Exception f) {
            try {
              jTextField.setText(Defaults.sMassFullFormat.format(Defaults.sMassFullFormat.parse(Defaults.sMassFullFormat.getPositivePrefix() + jTextField.getText())));
            } catch (Exception g) {
              JOptionPane.showMessageDialog(this, "Invalid format of modification." + System.lineSeparator() + jTextField.getText(), "Unknown input", JOptionPane.ERROR_MESSAGE);
              throw new InputMismatchException();
            }
          }
        }
      }

      // Načtení proteáz
      LinkedHashMap<String, String[]> file = null;
      try {
        file = ProteasesFile.getProteases();
      } catch (IOException ex) {
        if (JOptionPane.showConfirmDialog(this, "File is corrupted, should it be rewritten?", "Reading error",
                                          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
          doClose(false);
        }
      }
      if (file == null) {
        file = new LinkedHashMap<String, String[]>(1);
      }
      String newName = nameField.getText().trim().toUpperCase();

      // Pořadí proteáz pro uložení do souboru.
      ArrayList<String> order = new ArrayList<>(file.keySet().size());
      HashMap<String, String> upper = new HashMap<>(file.keySet().size());
      for (String name : file.keySet()) {
        String up = name.toUpperCase().trim();
        order.add(up);
        upper.put(up, name);
      }

      // Test, zda soubor již proteázu s daným jménem neobsahuje.
      if (!previousName.equals(newName) && order.contains(newName)) {
        String[] poss = { "Overwrite", "Back", "Cancel" };
        int ret = JOptionPane.showOptionDialog(this,
                        "Protease with the chosen name already present in the list, do you wish to overwrite the existing rules?",
                                               "Affirmation", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, poss, poss[2]);
        switch (ret) {
          case JOptionPane.CLOSED_OPTION:
          case 2:
            doClose(false);
            return;
          case 1:
            return;
          case 0:
            if (order.contains(previousName)) {
              file.remove(upper.get(previousName));
              order.remove(previousName);
              upper.remove(previousName);
            }
            file.remove(upper.get(newName));
            file.put(nameField.getText(), def);
            upper.put(newName, nameField.getText());
            break;
        }
      } else if (!previousName.equals("") && order.contains(previousName)) {
        file.remove(upper.get(previousName));
        file.put(nameField.getText(), def);
        if (!previousName.equals(newName)) {
          order.set(order.indexOf(previousName), newName);
          upper.remove(previousName);
        }
        upper.put(nameField.getText().toUpperCase(), nameField.getText());
      } else {
        file.put(nameField.getText(), def);
        order.add(newName);
        upper.put(newName, nameField.getText());
      }

      // Test, zda není příslušné místo štěpení již definováno.
      for (String[] rls : file.values()) {
        String[] rules = rls[0].split(" ");
        for (String rule : ruleField) {
          if (contains(rules, rule)) {
          String[] poss = { "Insert", "Back", "Cancel" };
          switch (JOptionPane.showOptionDialog(this, "Cleavage rule '" + rule + "' already present in the protease list, add anyway?",
                                               "Affirmation", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, poss, poss[2])) {
            case JOptionPane.CLOSED_OPTION:
            case 2:
              doClose(false);
            case 1:
              return;
            case 0:
              break;
            }
          }
        }
      }

      def[0] = ruleField.toString().substring(1, ruleField.toString().length()-1).replace(",", "");
      if (advancedPanel.isVisible() && (leftCheckBox.isSelected() || rightCheckBox.isSelected() || !leftTextField.getText().isEmpty() || !rightTextField.getText().isEmpty())) {
        if (!leftTextField.getText().isEmpty()) {
          try {
            def[1] += Defaults.sMassFullFormat.parse(leftTextField.getText()).toString();
          } catch (Exception e) {
            def[1] += leftTextField.getText();
          }
        }
        if (leftCheckBox.isSelected()) {
          def[1] += Protease.LOCK;
        }
        def[1] += ";";
        if (rightCheckBox.isSelected()) {
          def[1] += Protease.LOCK;
        }
        if (!rightTextField.getText().isEmpty()) {
          try {
            def[1] += Defaults.sMassFullFormat.parse(rightTextField.getText()).toString();
          } catch (Exception e) {
            def[1] += rightTextField.getText();
          }
        }
      }

      file.put(upper.get(newName), def);

      LinkedHashMap<String, String[]> output = new LinkedHashMap<>(file.size());
      for (String name : order) {
        output.put(upper.get(name), file.get(upper.get(name)));
      }

      ProteasesFile.saveProteases(output);

      doClose(true);
    } catch (java.util.regex.PatternSyntaxException e) {
      JOptionPane.showMessageDialog(this, e.getMessage(), "This isn't valid regexp", JOptionPane.ERROR_MESSAGE);
    } catch (java.lang.NullPointerException e ) {
      JOptionPane.showMessageDialog(this, "Please, specify a name.", "Invalid name", JOptionPane.ERROR_MESSAGE);
    } catch (java.util.InputMismatchException e) {
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, e.getMessage(), "Update couldn't be saved", JOptionPane.ERROR_MESSAGE);
    }
  }

  private boolean contains(java.util.Collection<String> rules, String rule) {
    for (String item : rules) {
      if (rule.compareTo(item) == 0) {
        return true;
      }
    }
    return false;
  }

  private boolean contains(String[] rules, String rule) {
    for (String item : rules) {
      if (rule.compareTo(item) == 0) {
        return true;
      }
    }
    return false;
  }

  private void cancelButtonActionPerformed(ActionEvent evt) {
    doClose(false);
  }

  /**
   * Closes the dialog
   */
  private void closeDialog(WindowEvent evt) {
    doClose(false);
  }

  private void doClose(boolean retStatus) {
    if (retStatus) {
      added = nameField.getText();
    }
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
      Logger.getLogger(ProteaseDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        ProteaseDialog pd = null;
        try {
          pd = new ProteaseDialog(null, "", "", "");
          pd.setVisible(true);
        } catch (Throwable e) {
          try (PrintWriter pw = new PrintWriter(new FileWriter("error.txt", true))) {
            pw.println(new java.util.Date().toString());
            pw.println("--------------------------------");
            pw.println(e.getMessage());
            pw.println("--------------------------------");
            e.printStackTrace(pw);
            pw.println("--------------------------------");
            if (pd != null) {
              pw.append(pd.previousName).append(System.lineSeparator());
              pw.append(pd.nameField.getText()).append(System.lineSeparator());
              pw.append(pd.rulesArea.getText()).append(System.lineSeparator());
              pw.append(pd.leftTextField.getText()).append(Protease.SEPARATOR).append(pd.rightTextField.getText()).append(System.lineSeparator());
              pw.append(pd.leftCheckBox.isSelected()? "LOCK" : "").append(Protease.SEPARATOR).append(pd.rightCheckBox.isSelected()? "LOCK" : "").append(System.lineSeparator());
            }
            pw.append("################################################################" + System.lineSeparator());
            pw.append(System.lineSeparator());
            pw.flush();
          } catch (Exception f) {
            Logger.getLogger(ProteaseDialog.class.getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(ProteaseDialog.class.getName()).log(Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(pd, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
