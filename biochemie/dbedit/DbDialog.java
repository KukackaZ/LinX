package biochemie.dbedit;

import biochemie.ChemicalElement;
import biochemie.Defaults;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 *
 * @author Janek
 */
public abstract class DbDialog extends JDialog {
  protected Properties defaults;
  protected boolean saved;
  protected JScrollPane dbScrollPane;
  protected JTable dbTable;
  protected JButton deleteButton;
  protected JButton saveButton;
  protected JButton closeButton;
  protected boolean edited;

  /** Creates new form DbDialog
   * @param id
   * @param parent */
  public DbDialog(String id, java.awt.Frame parent) {
    this(id, parent, null);
  }

  /** Creates new form DbDialog
   * @param id
   * @param parent
   * @param comment */
  public DbDialog(String id, java.awt.Frame parent, String comment) {
    super(parent, parent == null ? ModalityType.TOOLKIT_MODAL : DEFAULT_MODALITY_TYPE);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    setName("Edit." + id);
    setTitle("Edit " + id.toLowerCase());
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        doClose();
      }
    });

    defaults = Defaults.getDefaults(getName());
    saved = true;
    dbScrollPane = new JScrollPane();
    dbTable = new JTable();
    deleteButton = new JButton("Delete row");
    saveButton = new JButton("Save");
    closeButton = new JButton("Close");
    edited = false;

    dbTable.setCellSelectionEnabled(true);
    dbTable.setDefaultEditor(Array.class, new GeneralCellEditor(Array.class, this));
    dbTable.setDefaultEditor(Formula.class, new GeneralCellEditor(Formula.class, this));
    dbTable.setDefaultEditor(Name.class, new GeneralCellEditor(Name.class, this));
    dbTable.setDefaultEditor(Regex.class, new GeneralCellEditor(Regex.class, this));
    dbTable.setDefaultEditor(Specificity.class, new GeneralCellEditor(Specificity.class, this));
    dbTable.setDefaultEditor(Specificity2.class, new GeneralCellEditor(Specificity2.class, this));
    dbTable.setDefaultEditor(Symbol.class, new GeneralCellEditor(Symbol.class, this));
    dbTable.setDefaultEditor(Mass.class, new GeneralCellEditor(Mass.class, this));
    dbTable.setDefaultEditor(Index.class, new GeneralCellEditor(Index.class, this));
    dbTable.setDefaultEditor(Index1.class, new GeneralCellEditor(Index1.class, this));

    dbScrollPane.setViewportView(dbTable);

    deleteButton.setMnemonic('D');
    deleteButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int[] selected = dbTable.getSelectedRows();
        Arrays.sort(selected);
        if (selected.length > 0 && selected[selected.length-1] == dbTable.getRowCount()-1) {

          ((DefaultTableModel)dbTable.getModel()).addRow(new Object[1]);
        }
        for (int i = selected.length-1; i >= 0; i--) {
          ((DefaultTableModel)dbTable.getModel()).removeRow(selected[i]);
        }
        saved = false;
        int c = dbTable.getEditingColumn() < 0 ? dbTable.getSelectedColumn() < 0 ? 0 : dbTable.getSelectedColumn() : dbTable.getEditingColumn();
        int r = selected[0] < dbTable.getRowCount() ? selected[0] : dbTable.getRowCount()-1;
        if (dbTable.getCellEditor() != null) {
          dbTable.getCellEditor().cancelCellEditing();
        }
        dbTable.setColumnSelectionInterval(c, c);
        dbTable.setRowSelectionInterval(r, r);
        dbTable.requestFocus();
      }
    });

    saveButton.setMnemonic('S');
    saveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TableCellEditor cellEditor = dbTable.getCellEditor();
        if ((cellEditor == null || cellEditor.stopCellEditing()) && saveInput()) {
          JOptionPane.showMessageDialog(saveButton, "File has been saved succesfully.", "Finished", JOptionPane.INFORMATION_MESSAGE);
        }
      }
    });

    closeButton.setFont(new java.awt.Font("Tahoma", 1, 11));
    closeButton.setMnemonic('C');
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doClose();
      }
    });

    GroupLayout layout = new GroupLayout(getContentPane());

    GroupLayout.SequentialGroup sg = layout.createSequentialGroup()
                                           .addComponent(deleteButton);
    GroupLayout.ParallelGroup pg = layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                         .addComponent(deleteButton);
    if (!(comment == null || comment.isEmpty())) {
      JLabel label = new JLabel(comment);
      sg = sg.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
             .addComponent(label);
      pg = pg.addComponent(label);
    }

    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addComponent(dbScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
          .addGroup(sg
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(saveButton)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(closeButton)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(dbScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(pg
          .addComponent(closeButton)
          .addComponent(saveButton))
        .addContainerGap())
    );
  }

  public boolean wasEdited() {
    return edited;
  }

  protected void rest() {
    pack();
    setMinimumSize(new Dimension(Math.min(500, getMinimumSize().width), getMinimumSize().height));
    Defaults.setWindowDefaults(this, defaults);
    revalidate();
    Defaults.setTableDefaults(dbTable, defaults);
    setVisible(true);
  }

  private void doClose() {
    TableCellEditor cellEditor = dbTable.getCellEditor();
    if ((cellEditor != null && cellEditor.stopCellEditing()) || !saved) {
      int ret = JOptionPane.showConfirmDialog(this, "Changes aren't saved, do you want to save them?", "Not saved", JOptionPane.YES_NO_OPTION,
                                                JOptionPane.WARNING_MESSAGE);
      if (ret == JOptionPane.CLOSED_OPTION || (ret == JOptionPane.YES_OPTION && !saveInput())) {
        return;
      }
    }
    Defaults.putTableDefaults(dbTable, defaults);
    Defaults.putWindowDefaults(this, defaults);
    Defaults.addDefaults(getName(), defaults);
    setVisible(false);
    dispose();
  }

  protected abstract boolean saveInput();

  //<editor-fold defaultstate="collapsed" desc=" Attributes ">
  protected class Array {
    String array;

    public Array(String array) throws IOException {
      if (array == null || array.isEmpty()) {
        this.array = "";
      } else {
        if (array.contains(":")) {
          throw new IOException("Extensions cannot contain ':'.");
        }
        for (String ext : array.split("[ ,]+")) {
          if (ext.indexOf('.') != 0 || ext.lastIndexOf('.') == ext.length() - 1) {
            throw new IOException("Extensions must begin with '.' and cannot end with '.'.");
          }
          File tmp = File.createTempFile("test", ext);
          tmp.delete();
        }
        this.array = array.replaceAll("[ ,]+", ", ");
      }
    }

    public String toString() {
      return array;
    }
  }

  protected class Name {
    String name;

    public Name(String name) {
      if (name.contains("(") || name.contains(")") || name.contains("[") || name.contains("]") || name.contains("{") || name.contains("}")) {
        throw new IllegalArgumentException("Name cannot contain brackets.");
      }
      if (name.contains("\t")) {
        throw new IllegalArgumentException("Name cannot contain tabs.");
      }
      if (name.contains("|")) {
        throw new IllegalArgumentException("Name cannot contain vertical bar.");
      }
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }

  protected class Symbol {
    String symbol;

    public Symbol(String symbol) {
      if (!symbol.matches("[A-Za-z]*")) {
        throw new IllegalArgumentException("Symbol can contains only letters.");
      }
      if (!symbol.matches("[A-Z][a-z]*")) {
        symbol = symbol.substring(0, 1).toUpperCase() + symbol.substring(1).toLowerCase();
      }
      this.symbol = symbol;
    }

    public String toString() {
      return symbol;
    }
  }

  protected class Formula {
    BigDecimal value;
    String formula;

    public Formula(BigDecimal value) {
      this.value = value;
      this.formula = null;
    }

    public Formula(String formula) {
      try {
        this.value = (BigDecimal)Defaults.sMassFullFormat.parse(formula);
        this.formula = null;
      } catch (Exception e) {
        try {
          this.value = (BigDecimal)Defaults.sMassFullFormat.parse(Defaults.sMassFullFormat.getPositivePrefix() + formula);
          this.formula = null;
        } catch (Exception f) {
          this.value = ChemicalElement.evaluate(formula);
          this.formula = formula;
        }
      }
    }

    public String toString() {
      return toString(true);
    }

    public String toString(boolean localized) {
      if (formula == null) {
        return localized ? Defaults.sMassFullFormat.format(value) : value.toPlainString();
      }
      return formula;
    }

    public String toTitle() {
      return formula == null ? Defaults.sMassShortFormat.format(value) : (formula + " (" + Defaults.sMassShortFormat.format(value) + ')');
    }
  }

  protected class Regex {
    String regex;

    public Regex(String regex) {
      java.util.regex.Pattern.compile(regex);
      this.regex = regex;
    }

    public String toString() {
      return regex;
    }
  }

  protected class Specificity {
    String specificity;

    public Specificity(String specificity) {
      TreeSet<Character> chars = new TreeSet<>();
      for (char c : specificity.toUpperCase().replaceAll("[^A-Z^$]", "").replace('^', '<').replace('$', '>').toCharArray()) {
        chars.add(c);
      }
      this.specificity = chars.toString();
      this.specificity = this.specificity.substring(1, this.specificity.length()-1);
      this.specificity = this.specificity.replace('<', '^').replace('>', '$');
    }

    public String toString() {
      return specificity;
    }
  }

  protected class Specificity2 {
    String specificity;

    public Specificity2(String specificity) {
      if (specificity == null || specificity.contains("-") || specificity.equals("null")) {
        this.specificity = null;
        return;
      }
      TreeSet<Character> chars = new TreeSet<>();
      for (char c : specificity.toUpperCase().replaceAll("[^A-Z^$]", "").replace('^', '<').replace('$', '>').toCharArray()) {
        chars.add(c);
      }
      this.specificity = chars.toString();
      this.specificity = this.specificity.substring(1, this.specificity.length()-1);
      this.specificity = this.specificity.replace('<', '^').replace('>', '$');
    }

    public String toString() {
      return specificity == null ? "---" : specificity;
    }
  }

  protected class Mass extends Number {
    BigDecimal value;

    public Mass(BigDecimal mass) {
      this.value = mass;
      check();
    }

    public Mass(String mass) throws ParseException {
      value = (BigDecimal)Defaults.uMassFullFormat.parse(mass);
      check();
    }

    private void check() {
      if (value.signum() < 0) {
        value = value.negate();
      }
    }

    public String toString() {
      return toString(true);
    }

    public String toString(boolean localized) {
      return localized ? Defaults.uMassFullFormat.format(value) : value.toPlainString();
    }

    public int intValue() {
      return value.intValue();
    }
    public long longValue() {
      return value.longValue();
    }
    public float floatValue() {
      return value.floatValue();
    }
    public double doubleValue() {
      return value.doubleValue();
    }
  }

  protected class Index extends Number {
    int value;

    public Index(int index) {
      this.value = index;
      check();
    }

    public Index(String index) {
      value = Integer.parseInt(index);
      check();
    }

    private void check() {
      if (value < 0) {
        value = -value;
      }
    }

    public String toString() {
      return value + "";
    }

    public int intValue() {
      return value;
    }
    public long longValue() {
      return value;
    }
    public float floatValue() {
      return value;
    }
    public double doubleValue() {
      return value;
    }
  }

  protected class Index1 extends Number {
    int value;

    public Index1(int index) {
      this.value = index;
      check();
    }

    public Index1(String index) {
      value = Integer.parseInt(index);
      check();
    }

    private void check() {
      if (value == 0) {
        throw new NumberFormatException("Index cannot be '0'.");
      }
      if (value < 0) {
        value = -value;
      }
    }

    public String toString() {
      return value + "";
    }

    public int intValue() {
      return value;
    }
    public long longValue() {
      return value;
    }
    public float floatValue() {
      return value;
    }
    public double doubleValue() {
      return value;
    }
  }
  //</editor-fold>

  protected class GeneralCellEditor extends DefaultCellEditor {
    private Object value;
    private Class cl;
    private DbDialog parent;

    public GeneralCellEditor(Class cl, DbDialog parent) {
      super(new JTextField());
      getComponent().setName("Table.editor");
      this.cl = cl;
      this.parent = parent;
    }

    public boolean stopCellEditing() {
      Object o = super.getCellEditorValue();
      String s;
      if (o == null || "".equals(s = o.toString())) {
        value = null;
        return super.stopCellEditing();
      }

      try {
        value = cl.getConstructor(DbDialog.class, String.class).newInstance(parent, s);
      } catch (Exception e) {
        ((JComponent)getComponent()).setBorder(new javax.swing.border.LineBorder(Color.red));
        return false;
      }
      return super.stopCellEditing();
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      ((JComponent)getComponent()).setBorder(new javax.swing.border.LineBorder(Color.black));
      if (value instanceof Number && !(value instanceof Formula) && getComponent() instanceof JTextField) {
        ((JTextField)getComponent()).setHorizontalAlignment(JTextField.TRAILING);
      }
      return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    public Object getCellEditorValue() {
      return value;
    }
  }

  protected class NumberCellRenderer extends DefaultTableCellRenderer.UIResource {
    DecimalFormat df;

    public NumberCellRenderer(DecimalFormat decimalFormat) {
      super();
      df = decimalFormat;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    public void setValue(Object value) {
      try {
        setText((value == null) ? "" : df.format(value));
      } catch (Exception e) {
        try {
          setText(df.format(df.parse(value.toString())));
        } catch (Exception f) { }
      }
    }
  }

  protected class FormulaCellRenderer extends DefaultTableCellRenderer.UIResource {
    public void setValue(Object value) {
      try {
        setText(((Formula)value).toTitle());
      } catch (Exception e) {
        setText(value == null ? "" : value.toString());
      }
    }
  }
}
