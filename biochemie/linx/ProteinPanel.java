package biochemie.linx;

import biochemie.*;
import java.awt.Color;
import java.awt.event.*;
import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 *
 * @author Janek
 */
public class ProteinPanel extends JPanel {
  private static final int NAME_LENGTH = 30;
  JButton remove;
  JTextField id;
  JLabel hintLabel;
  JFormattedTextField index;
  JFormattedTextField from;
  JFormattedTextField to;
  JTextArea protein;
  ProteinsCard parent;

  public ProteinPanel(ProteinsCard parent, int i, String identifier, String shift, String chain) {
    this.parent = parent;
    JLabel nameLabel = new JLabel("Name:");
    id = new JTextField(30);
    hintLabel = new JLabel();
    JLabel indexLabel = new JLabel("Starting index:");
    index = new JFormattedTextField(java.text.NumberFormat.getIntegerInstance());
    JLabel fromLabel = new JLabel("Use part from");
    from = new JFormattedTextField(java.text.NumberFormat.getIntegerInstance()) {
      public void commitEdit() throws ParseException {
        if (getText().isEmpty()) {
          setValue(null);
        } else {
          super.commitEdit();
        }
      }
    };
    JLabel toLabel = new JLabel("to");
    to = new JFormattedTextField(java.text.NumberFormat.getIntegerInstance()) {
      public void commitEdit() throws ParseException {
        if (getText().isEmpty()) {
          setValue(null);
        } else {
          super.commitEdit();
        }
      }
    };
    JButton load = new JButton("Load from file");
    remove = new JButton("Remove panel");
    protein = new JTextArea(chain, 4, 109);
    JScrollPane proteinScrollPane = new JScrollPane();

    setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Protein no." + i, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP));
    setName(String.valueOf(i));

    nameLabel.setDisplayedMnemonic('M');
    nameLabel.setLabelFor(id);

    id.setInputVerifier(new InputVerifier() {
      public boolean verify(JComponent input) {
        return checkIdentifier(true);
      }
    });
    id.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) {
        checkIdentifier(false);
        if (getIdentifier().length() > NAME_LENGTH) {
          id.setForeground(Color.red);
        }
      }

      public void removeUpdate(DocumentEvent e) {
        checkIdentifier(false);
        if (getIdentifier().length() <= NAME_LENGTH) {
          id.setForeground(null);
        }
      }

      public void changedUpdate(DocumentEvent e) {
        insertUpdate(e);
        removeUpdate(e);
      }
    });

    changeIcon(false);
    hintLabel.setToolTipText("<html><strong>Protein name cannot:</strong><ul><li>be longer than 30 characters;</li>"
                                                                          + "<li>starts with 'peptide', 'all proteins' or 'within one molecule' (case insensitive); or</li>"
                                                                          + "<li>contains '-', '(', ')', '[', ']', '{' or '}'.</li></ul></html>");

    indexLabel.setDisplayedMnemonic('I');
    indexLabel.setLabelFor(index);

    index.setColumns(5);
    index.setHorizontalAlignment(SwingConstants.TRAILING);
    index.setValue(1);

    fromLabel.setDisplayedMnemonic('F');
    fromLabel.setLabelFor(from);

    from.setColumns(5);
    from.setHorizontalAlignment(SwingConstants.TRAILING);
    from.setValue(null);

    toLabel.setDisplayedMnemonic('T');
    toLabel.setLabelFor(to);

    to.setColumns(5);
    to.setHorizontalAlignment(SwingConstants.TRAILING);
    to.setValue(null);

    load.setMnemonic('O');
    load.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        loadActionPerformed();
      }
    });

    remove.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        removeActionPerformed();
      }
    });
    remove.setEnabled(i != 1);

    protein.setFont(new java.awt.Font("Courier New", 0, 12));
    protein.setLineWrap(true);
    protein.setWrapStyleWord(true);
    protein.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
          if (e.isShiftDown()) {
            protein.transferFocusBackward();
          } else {
            protein.transferFocus();
          }
          e.consume();
        }
      }
    });
    ((AbstractDocument)protein.getDocument()).setDocumentFilter(new DocumentFilter() {
      public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
        if (fb.getDocument().getText(offset, length).equals(" ")) {
          if (protein.getCaretPosition() == offset) {
            replace(fb, offset+1, length, "", null);
          } else {
            replace(fb, offset-1, length, "", null);
          }
        } else {
          replace(fb, offset, length, "", null);
        }
      }

      public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, string, attr);
      }

      public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        String pre = fb.getDocument().getText(0, offset);
        String post = fb.getDocument().getText(offset + length, fb.getDocument().getLength() - offset - length);
        fb.replace(0, fb.getDocument().getLength(), validate(pre + text + post), attrs);
        protein.setCaretPosition(validate(pre + text).length());
      }

      private String validate(String string) {
        return string.replaceAll("[^A-Za-z/]", "").toUpperCase().replaceAll("(([A-Z](/[A-Z]?)*){10})", "$1 ").replaceFirst(" $", "");
      }
    });
    proteinScrollPane.setViewportView(protein);

    GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addComponent(proteinScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addGroup(layout.createSequentialGroup()
            .addComponent(nameLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(id, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            //.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(hintLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(indexLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(index, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(fromLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(from, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(toLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(to, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(load)
            .addComponent(remove)))
        .addContainerGap())
    );
    layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(remove)
          .addComponent(load)
          .addComponent(to, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(toLabel)
          .addComponent(from, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(fromLabel)
          .addComponent(index, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(indexLabel)
          .addComponent(hintLabel)
          .addComponent(id, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(nameLabel))
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(proteinScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap())
    );

    setIdentifier(identifier);
    setShift(shift);
    setChain(chain);

    id.requestFocusInWindow();
  }

  public boolean check(boolean alert) {
    if (isEmpty()) {
      return true;
    }
    if (!checkIdentifier(alert)) {
      return false;
    }
    try {
      index.commitEdit();
      index.setValue(index.getValue());
      from.commitEdit();
      from.setValue(from.getValue());
      to.commitEdit();
      to.setValue(to.getValue());
    } catch (Exception e) {
      return false;
    }
    if (from.getValue() != null && (((Number)index.getValue()).intValue() > ((Number)from.getValue()).intValue() ||
                                    ((Number)from.getValue()).intValue() >= ((Number)index.getValue()).intValue()+getLength()-1)) {
      JOptionPane.showMessageDialog(this, "The start of the used fragment must be within the entered protein.", getIdentifier(), JOptionPane.WARNING_MESSAGE);
      return false;
    }
    if (from.getValue() != null && to.getValue() != null && ((Number)from.getValue()).intValue() > ((Number)to.getValue()).intValue()) {
      JOptionPane.showMessageDialog(this, "Starting index of the fragment cannot be higher than ending index of that fragment.", getIdentifier(), JOptionPane.WARNING_MESSAGE);
      return false;
    }
    if (to.getValue() != null && (((Number)index.getValue()).intValue() >= ((Number)to.getValue()).intValue() ||
                                  ((Number)to.getValue()).intValue() >= ((Number)index.getValue()).intValue()+getLength())) {
      JOptionPane.showMessageDialog(this, "The end of the used fragment must be within the entered protein.", getIdentifier(), JOptionPane.WARNING_MESSAGE);
      return false;
    }
    
    if (!Polymer.isPolymer(protein.getText().toUpperCase().replaceAll(" ", ""), true)) {
      JOptionPane.showMessageDialog(protein, id.getText() + " isn't chain of aminoacids.", "Incorrect protein", JOptionPane.ERROR_MESSAGE);
      protein.requestFocusInWindow();
      return false;
    }
    return true;

  }

  public boolean isEmpty() {
    return protein.getText().replaceAll(" ", "").isEmpty();
  }

  public boolean isIdentifierDefault() {
    return id.getText().trim().equalsIgnoreCase(((TitledBorder)getBorder()).getTitle());
  }
  
  public Protein getProtein() {
    return new Protein(getIdentifier(), getChain(), getShift());
  }

  public String getIdentifier() {
    return id.getText().trim();
  }

  public String getChain() {
    String text = protein.getText().toUpperCase().replaceAll(" ", "");
    TreeMap<Integer, String> insert = new TreeMap();
    if (from.getValue() != null && ((Number)from.getValue()).intValue() > ((Number)index.getValue()).intValue()
                                && ((Number)from.getValue()).intValue() < ((Number)index.getValue()).intValue()+text.length()) {
      insert.put(((Number)from.getValue()).intValue(), "^");
    }
    if (to.getValue() != null && ((Number)to.getValue()).intValue() >= ((Number)index.getValue()).intValue()
                              && ((Number)to.getValue()).intValue() <  ((Number)index.getValue()).intValue()+text.length()-1) {
      insert.put(((Number)to.getValue()).intValue() + 1, "$");
    }
    for (Map.Entry<Integer, String> entry : insert.descendingMap().entrySet()) {
      int position = entry.getKey() - ((Number)index.getValue()).intValue();
      for (char c : entry.getValue().toCharArray()) {
        text = text.replaceFirst("^/*([^/](/+[^/])*){" + position + "}", "$0\\" + c);
      }
    }
    return text;
  }

  public int getLength() {
    return protein.getText().replace(" ", "").replaceAll("^/+", "").replaceAll("/+([^/]|$)", "").length();
  }
  
  public int getShift() {
    return ((Number)index.getValue()).intValue();
  }

  public void setProtein(Protein protein) {
    setIdentifier(protein.getName());
    setShift(Integer.toString(protein.getShift()));
    setChain(protein.toString());
  }

  public void setIdentifier(String identifier) {
    id.setText(identifier == null || identifier.isEmpty() ? "Protein no." + getName() : identifier);
  }

  public void setChain(String chain) {
    String tmp = chain.replaceAll("[^A-Za-z/^$]", "").replaceAll("^/+", "");
    if (tmp.contains("^")) {
      setFrom(Integer.toString(getShift() + tmp.split("\\^", 2)[0].replace("$", "").replaceAll("/+([^/]|$)", "").length()));
    }
    if (tmp.contains("$")) {
      setTo(Integer.toString(getShift() + tmp.split("\\$", 2)[0].replace("^", "").replaceAll("/+([^/]|$)", "").length() - 1));
    }
    protein.setText(chain == null ? "" : chain.replace("^", "").replace("$", ""));
  }

  public void setShift(String shift) {
//    int old = ((Number)index.getValue()).intValue();
    index.setText(shift == null || shift.isEmpty() ? "1" : shift);
    try {
      index.commitEdit();
      index.setValue(index.getValue());
    } catch (ParseException ex) { }
//    old -= ((Number)index.getValue()).intValue();
//    if (from.getValue() != null) {
//      from.setText(Integer.toString(((Number)from.getValue()).intValue() - old));
//      try {
//        from.commitEdit();
//        from.setValue(from.getValue());
//      } catch (ParseException ex) { }
//    }
//    if (from.getValue() != null) {
//      to.setText(Integer.toString(((Number)to.getValue()).intValue() - old));
//      try {
//        to.commitEdit();
//        to.setValue(to.getValue());
//      } catch (ParseException ex) { }
//    }
  }
  
  public void setFrom(String start) {
    from.setText(start);
    try {
      from.commitEdit();
      from.setValue(from.getValue());
    } catch (ParseException ex) { }
  }

  public void setTo(String end) {
    to.setText(end);
    try {
      to.commitEdit();
      to.setValue(to.getValue());
    } catch (ParseException ex) { }
  }

  public void setAlone(boolean alone) {
    remove.setEnabled(!alone);
  }

  public void setFocus() {
    id.requestFocusInWindow();
  }

  private boolean checkIdentifier(boolean alert) {
    String identifier = getIdentifier().toUpperCase();
    if (identifier.length() > NAME_LENGTH) {
      if (alert) {
        JOptionPane.showMessageDialog(id, "Name of the protein cannot be longer than 30 characters.", "Invalid name", JOptionPane.WARNING_MESSAGE);
        id.setCaretPosition(NAME_LENGTH);
      }
      changeIcon(true);
      return false;
    }
    if (identifier.startsWith("PEPTIDES")) {
      if (alert) {
        JOptionPane.showMessageDialog(id, "Name of the protein cannot start with 'Peptides'.", "Invalid name", JOptionPane.WARNING_MESSAGE);
        id.setCaretPosition(0);
      }
      changeIcon(true);
      return false;
    }
    if (identifier.startsWith("ALL PROTEINS")) {
      if (alert) {
        JOptionPane.showMessageDialog(id, "Name of the protein cannot start with 'Peptides'.", "Invalid name", JOptionPane.WARNING_MESSAGE);
        id.setCaretPosition(0);
      }
      changeIcon(true);
      return false;
    }
    if (identifier.startsWith("WITHIN ONE MOLECULE")) {
      if (alert) {
        JOptionPane.showMessageDialog(id, "Name of the protein cannot start with 'Peptides'.", "Invalid name", JOptionPane.WARNING_MESSAGE);
        id.setCaretPosition(0);
      }
      changeIcon(true);
      return false;
    }
    if (identifier.equals("BONDS")) {
      if (alert) {
        JOptionPane.showMessageDialog(id, "Name of the protein cannot contains 'Bonds'.", "Invalid name", JOptionPane.WARNING_MESSAGE);
        id.setCaretPosition(0);
      }
      changeIcon(true);
      return false;
    }
    if (identifier.isEmpty()) {
      if (alert) {
        id.setText("Protein no." + getName());
      } else {
        changeIcon(true);
        return false;
      }
    }
    changeIcon(false);
    return true;
  }
  
  private void changeIcon(boolean warning) {
    hintLabel.setIcon(new ImageIcon(((ImageIcon)UIManager.getIcon("OptionPane." + (warning ? "warning" : "information") + "Icon")).getImage()
                                              .getScaledInstance(id.getPreferredSize().height*2/3, id.getPreferredSize().height*2/3, java.awt.Image.SCALE_SMOOTH)));
  }

  private void loadActionPerformed() {
    javax.swing.filechooser.FileFilter filterAll = new javax.swing.filechooser.FileFilter() {
      public boolean accept(File f) {
        if (f.isDirectory()) {
          return true;
        }
        String ext = f.getName();
        int i = ext.lastIndexOf(".");
        if (i < 0) {
          ext = "";
        } else {
          ext = ext.substring(i+1);
        }
        if (ext != null) {
          if (ext.equals("fasta") ||
                  ext.equals("txt")) {
            return true;
          }
        }
        return false;
      }

      public String getDescription() {
        return "All protein formats [.fasta, .txt]";
      }
    };
    javax.swing.filechooser.FileFilter filterFasta = new javax.swing.filechooser.FileFilter() {
      public boolean accept(File f) {
        if (f.isDirectory()) {
          return true;
        }
        String ext = f.getName();
        int i = ext.lastIndexOf(".");
        if (i < 0) {
          ext = "";
        } else {
          ext = ext.substring(i+1);
        }
        if (ext != null) {
          if (ext.equals("fasta")) {
            return true;
          }
          return false;
        }
        return false;
      }

      public String getDescription() {
        return "Protein in FASTA format [.fasta]";
      }
    };
    javax.swing.filechooser.FileFilter filterSimple = new javax.swing.filechooser.FileFilter() {
      public boolean accept(File f) {
        if (f.isDirectory()) {
          return true;
        }
        String ext = f.getName();
        int i = ext.lastIndexOf(".");
        if (i < 0) {
          ext = "";
        } else {
          ext = ext.substring(i+1);
        }
        if (ext != null) {
          if (ext.equals("txt")) {
            return true;
          }
          return false;
        }
        return false;
      }

      public String getDescription() {
        return "Simple amino-acid chain [.txt]";
      }
    };
    JFileChooser fc = new JFileChooser(parent.PATH);
    fc.setFileFilter(filterAll);
    fc.addChoosableFileFilter(filterFasta);
    fc.addChoosableFileFilter(filterSimple);
    if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      String path;
      try {
        path = java.nio.file.Paths.get(System.getProperty("user.dir")).relativize(fc.getSelectedFile().getParentFile().toPath()).toString();
      } catch (Exception e) {
        path = fc.getSelectedFile().getParentFile().getPath();
      }
      if (path.isEmpty()) {
        path = ".";
      }
      parent.logPath(path);
      try {
        BufferedReader fr = new BufferedReader(new FileReader(fc.getSelectedFile()));
        String line = fr.readLine();
        if (line == null || line.isEmpty()) {
          JOptionPane.showMessageDialog(this, "The file is empty!", "Empty file", JOptionPane.WARNING_MESSAGE);
          return;
        }
        String name = null;
        if (fc.getFileFilter() == filterFasta || (fc.getFileFilter() == filterAll && line.length() > 0 && line.charAt(0) == '>')) {
          name = line;
          line = fr.readLine();
          ArrayList<String> proteins = new ArrayList<>();
          proteins.add(name.replaceFirst("^> *", ""));
          while ((name = fr.readLine()) != null) {
            if (name.length() > 0 && name.charAt(0) == '>') {
              proteins.add(name.replaceFirst("^> *", ""));
            }
          }

          if (proteins.size() == 1) {
            name = proteins.get(0);
          } else {
            proteins.add(0, "ALL");
            name = (String)JOptionPane.showInputDialog(this, "File contains multiple proteins, please select one:", "Select protein",
                                                       JOptionPane.QUESTION_MESSAGE, null, proteins.toArray(), proteins.get(0));
          }

          if (name == null) {
            return;
          }
          if (name.equals("ALL")) {
            setIdentifier("");
            setChain("");
            fr = new BufferedReader(new FileReader(fc.getSelectedFile()));
            name = fr.readLine().replaceFirst("^> *", "");
            line = fr.readLine();
            StringBuilder buff = new StringBuilder(line);
            while ((line = fr.readLine()) != null) {
              if (line.length() == 0) {
                while ((line = fr.readLine()) != null && line.isEmpty()) { }
                if (line == null) {
                  break;
                }
              }
              if (line.charAt(0) == '>') {
                String prot = buff.toString().replaceFirst("\\*.*$", "");
                if (!Polymer.isPolymer(prot, true)) {
                  throw new IOException("'" + name + "' isn't chain of aminoacids.");
                }
                parent.insertProtein(name, "", prot);
                name = line.replaceFirst("^> *", "");
                buff = new StringBuilder();
              } else {
                buff.append(line);
              }
            }
            line = buff.toString().replaceFirst("\\*.*$", "");
            if (!Polymer.isPolymer(line, true)) {
              throw new IOException("'" + name + "' isn't chain of aminoacids.");
            }
            parent.insertProtein(name, "", line);
            return;
          }
          fr = new BufferedReader(new FileReader(fc.getSelectedFile()));
          while (fr.readLine().replaceFirst("^> *", "").compareTo(name.toString()) != 0) { }
          StringBuilder buff = new StringBuilder();
          while ((line = fr.readLine()) != null && (line.length() == 0 || line.charAt(0) != '>')) {
            buff.append(line);
          }
          name = name.replaceFirst(">", "").trim();
          line = buff.toString().replaceFirst("\\*.*$", "");
        } else {
          if (line == null || line.isEmpty()) {
            line = name;
            name = null;
          }
          String next;
          while ((next = fr.readLine()) != null && !next.isEmpty() && Polymer.isPolymer(next, true)) {
            line += next;
          }
        }
        if (!Polymer.isPolymer(line, true)) {
          throw new IOException("It isn't chain of aminoacids:" + System.lineSeparator() + line);
        }
        setIdentifier(name);
        setShift(null);
        setFrom(null);
        setTo(null);
        setChain(line);
        id.requestFocusInWindow();
        if (id.getText().length() > NAME_LENGTH) {
          id.setCaretPosition(NAME_LENGTH);
        }
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Incorrect file", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void removeActionPerformed() {
    if (!isEmpty() && JOptionPane.showConfirmDialog(this, id.getText() + " isn't empty, do you really want to remove it?", "Warning",
                                                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)== JOptionPane.NO_OPTION) {
      return;
    }
    parent.removeActionPerformed(Integer.valueOf(getName()));
  }
}
