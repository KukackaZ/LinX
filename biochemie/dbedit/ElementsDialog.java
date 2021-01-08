package biochemie.dbedit;

import biochemie.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Janek
 */
public class ElementsDialog extends DbDialog {
  public static boolean showElementsDialog(java.awt.Frame parent) {
    return new ElementsDialog(parent).wasEdited();
  }
  public final static String SEPARATOR = "\t";
  
  private static String plus;

  /** Creates new form ModificationsDialog */
  public ElementsDialog(java.awt.Frame parent) {
    super("Elements", parent);

    Properties file = Defaults.loadDefaults(ChemicalElement.FILE);
    plus = file.getProperty("+");
    TreeMap<String, String> splitted = new TreeMap<>();
    for (String key : file.stringPropertyNames()) {
      if (!"+".equals(key)) {
        splitted.put(key, file.getProperty(key).split(SEPARATOR, 2)[0]);
      }
    }
    if (splitted.isEmpty() && JOptionPane.showConfirmDialog(this, "It isn't possible to open file. Do you want to overwrite it?", "IO Exception",
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
      dispose();
      return;
    }
    ArrayList<Object[]> lines = new ArrayList<>(file.size()+1);
    for (Map.Entry<String, String> entry : splitted.entrySet()) {
      Mass w;
      try {
        w = entry.getValue().isEmpty() ? null : new Mass(new BigDecimal(entry.getValue()));
      } catch (Exception e1) {
        w = null;
      }
      lines.add(new Object[] { new Symbol(entry.getKey()), w, });
    }
    Object[][] model = new Object[lines.size()+1][2];
    for (int i = 0; i < lines.size(); i++) {
      model[i] = lines.get(i);
    }

    dbTable.setModel(new javax.swing.table.DefaultTableModel(model, new String [] { "Symbol", "Mass" }) {
      public Class getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Symbol.class : Mass.class;
      }
    });
    dbTable.getModel().addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        saved = false;
        int lr = dbTable.getRowCount()-1;
        for (int i = 0; i < dbTable.getColumnCount(); i++) {
          if (dbTable.getValueAt(lr, i) != null &&
              !dbTable.getValueAt(lr, i).toString().isEmpty()) {
            ((DefaultTableModel)dbTable.getModel()).addRow(new Object[] { null, null });
          }
        }
      }
    });
    dbTable.setDefaultRenderer(Mass.class, new NumberCellRenderer(Defaults.uMassShortFormat));

    rest();
  }

  protected boolean saveInput() {
    if (saved) {
      return true;
    }
    Properties rows = new Properties();
    if (plus != null) {
      rows.setProperty("+", plus);
    }
    for (int i = 0; i < dbTable.getRowCount(); i++) {
      if (dbTable.getValueAt(i, 0) == null || dbTable.getValueAt(i, 0).toString().length() == 0) {
        for (int j = 1; j < dbTable.getColumnCount(); j++) {
          if (dbTable.getValueAt(i, j) != null && dbTable.getValueAt(i, j).toString().length() > 0) {
            JOptionPane.showMessageDialog(this, "Element no." + (i+1) + " doesn't have symbol.", "Incomplete input", JOptionPane.WARNING_MESSAGE);
            dbTable.requestFocus();
            dbTable.setRowSelectionInterval(i, i);
            dbTable.setColumnSelectionInterval(0, 0);
            dbTable.editCellAt(i, 0);
            return false;
          }
        }
      } else {
        if ((dbTable.getValueAt(i, 1) == null || dbTable.getValueAt(i, 1).toString().length() == 0)) {
          JOptionPane.showMessageDialog(this, "Element no." + (i+1) + " doesn't have specified mass.", "Incomplete input",
                                        JOptionPane.WARNING_MESSAGE);
          dbTable.requestFocus();
          dbTable.setRowSelectionInterval(i, i);
          dbTable.setColumnSelectionInterval(1, 1);
          dbTable.editCellAt(i, 1);
          return false;
        }
        for (int j = 0; j < dbTable.getRowCount(); j++) {
          if (i != j && dbTable.getValueAt(j, 0) != null && dbTable.getValueAt(i, 0).toString().trim().equals(dbTable.getValueAt(j, 0).toString().trim())) {
            JOptionPane.showMessageDialog(this, "Element no." + (i+1) + " have the same name as element no." + (j+1) + ".", "Incomplete input",
                                          JOptionPane.WARNING_MESSAGE);
            return false;
          }
        }
        rows.setProperty(dbTable.getValueAt(i, 0).toString().trim(), ((Mass)dbTable.getValueAt(i, 1)).toString(false));
      }
    }
    if (!Defaults.saveDefaults(ChemicalElement.FILE, rows)) {
      return false;
    }
    saved = true;
    edited = true;
    ChemicalElement.reload();
    return true;
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
      Logger.getLogger(ElementsDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        ElementsDialog ed = null;
        try {
          ed = new ElementsDialog(null);
        } catch (Throwable e) {
          try (PrintWriter pw = new PrintWriter(new FileWriter("error.txt", true))) {
            pw.println(new java.util.Date().toString());
            pw.println("--------------------------------");
            pw.println(e.getMessage());
            pw.println("--------------------------------");
            e.printStackTrace(pw);
            pw.println("--------------------------------");
            if (ed != null) {
              for (int i = 0; i < ed.dbTable.getRowCount(); i++) {
                for (int j = 0; j < ed.dbTable.getColumnCount(); j++) {
                  pw.print(ed.dbTable.getValueAt(i, j));
                  pw.append('\t');
                }
                pw.println();
              }
            }
            pw.append("################################################################" + System.lineSeparator());
            pw.append(System.lineSeparator());
            pw.flush();
          } catch (Exception f) {
            Logger.getLogger(ElementsDialog.class.getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(ElementsDialog.class.getName()).log(Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(ed, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
