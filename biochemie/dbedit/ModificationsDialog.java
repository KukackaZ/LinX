package biochemie.dbedit;

import biochemie.linx.ModificationAbstract;
import biochemie.linx.BondAbstract;
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
public class ModificationsDialog extends DbDialog {
  public static boolean showModificationsDialog(java.awt.Frame parent) {
    return new ModificationsDialog(parent).wasEdited();
  }

  /** Creates new form ModificationsDialog */
  public ModificationsDialog(java.awt.Frame parent) {
    super("Modifications", parent);

    Properties file = Defaults.loadDefaults(ModificationAbstract.FILE);
    TreeMap<String, String[]> splitted = new TreeMap<>();
    for (String key : file.stringPropertyNames()) {
      splitted.put(key, file.getProperty(key).split(ModificationAbstract.SEPARATOR, 4));
    }
    if (splitted.isEmpty() && JOptionPane.showConfirmDialog(this, "It isn't possible to open file. Do you want to overwrite it?", "IO Exception",
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
      dispose();
      return;
    }
    ArrayList<Object[]> lines = new ArrayList<>(file.size()+1);
    for (Map.Entry<String, String[]> entry : splitted.entrySet()) {
      if (entry.getValue().length >= 3) {
        Formula m;
        try {
          m = new Formula(new BigDecimal(entry.getValue()[0]));
        } catch (Exception e1) {
          try {
            m = new Formula(entry.getValue()[0]);
          } catch (Exception e2) {
            m = new Formula("");
          }
        }
        Formula d;
        try {
          d = new Formula(new BigDecimal(entry.getValue()[1]));
        } catch (Exception e1) {
          try {
            d = new Formula(entry.getValue()[1]);
          } catch (Exception e2) {
            d = new Formula("");
          }
        }
        lines.add(new Object[] { new Name(entry.getKey()), m, d, new Specificity(entry.getValue()[2]) });
      }
    }
    Object[][] model = new Object[lines.size()+1][4];
    for (int i = 0; i < lines.size(); i++) {
      model[i] = lines.get(i);
    }

    dbTable.setModel(new javax.swing.table.DefaultTableModel(model, new String [] { "Name", "Mass", "Check Diff", "Specificity" }) {
      public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
          return Name.class;
        } else if (columnIndex == 3) {
          return Specificity.class;
        } else {
          return Formula.class;
        }
      }
    });
    dbTable.getModel().addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        saved = false;
        int lr = dbTable.getRowCount()-1;
        for (int i = 0; i < dbTable.getColumnCount(); i++) {
          if (dbTable.getValueAt(lr, i) != null && !dbTable.getValueAt(lr, i).toString().isEmpty()) {
            ((DefaultTableModel)dbTable.getModel()).addRow(new Object[] { null, null, null, null });
            break;
          }
        }
      }
    });
    dbTable.setDefaultRenderer(Formula.class, new FormulaCellRenderer());

    rest();
  }

  protected boolean saveInput() {
    if (saved) {
      return true;
    }
    Properties bonds = Defaults.loadDefaults(BondAbstract.FILE);
    Properties rows = new Properties();
    for (int i = 0; i < dbTable.getRowCount(); i++) {
      if (dbTable.getValueAt(i, 0) == null || dbTable.getValueAt(i, 0).toString().isEmpty()) {
        for (int j = 1; j < dbTable.getColumnCount(); j++) {
          if (dbTable.getValueAt(i, j) != null && dbTable.getValueAt(i, j).toString().length() > 0) {
            JOptionPane.showMessageDialog(this, "Modification no." + (i+1) + " doesn't have name.", "Incomplete input", JOptionPane.WARNING_MESSAGE);
            dbTable.requestFocus();
            dbTable.setRowSelectionInterval(i, i);
            dbTable.setColumnSelectionInterval(0, 0);
            dbTable.editCellAt(i, 0);
            return false;
          }
        }
      } else {
        if ((dbTable.getValueAt(i, 1) == null || dbTable.getValueAt(i, 1).toString().isEmpty())) {
          JOptionPane.showMessageDialog(this, "Modification no." + (i+1) + " doesn't have specified difference.", "Incomplete input",
                                        JOptionPane.WARNING_MESSAGE);
          dbTable.requestFocus();
          dbTable.setRowSelectionInterval(i, i);
          dbTable.setColumnSelectionInterval(1, 1);
          dbTable.editCellAt(i, 1);
          return false;
        }
        for (int j = 0; j < dbTable.getRowCount(); j++) {
          if (i != j && dbTable.getValueAt(j, 0) != null && dbTable.getValueAt(i, 0).toString().trim().equals(dbTable.getValueAt(j, 0).toString().trim())) {
            JOptionPane.showMessageDialog(this, "Modification no." + (i+1) + " have the same name as modification no." + (j+1) + ".", "Incomplete input",
                                          JOptionPane.WARNING_MESSAGE);
            return false;
          }
        }
        for (String bond : bonds.stringPropertyNames()) {
          if (bond.trim().equals(dbTable.getValueAt(i, 0).toString().trim())) {
            JOptionPane.showMessageDialog(this, "Modification no." + (i+1) + " have the same name as one bond: " + bond.trim() + ".", "Incomplete input",
                                          JOptionPane.WARNING_MESSAGE);
            return false;
          }
        }
        rows.setProperty(dbTable.getValueAt(i, 0) == null ? "" : dbTable.getValueAt(i, 0).toString().trim(),
                         ((Formula)dbTable.getValueAt(i, 1)).toString(false) + ModificationAbstract.SEPARATOR +
                         (dbTable.getValueAt(i, 2) == null ? BigDecimal.ZERO.toPlainString() : ((Formula)dbTable.getValueAt(i, 2)).toString(false)) + ModificationAbstract.SEPARATOR +
                         (dbTable.getValueAt(i, 3) == null ? "" : dbTable.getValueAt(i, 3).toString()));
      }
    }
    if (!Defaults.saveDefaults(ModificationAbstract.FILE, rows)) {
      return false;
    }
    saved = true;
    edited = true;
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
      Logger.getLogger(ModificationsDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        ModificationsDialog md = null;
        try {
          md = new ModificationsDialog(null);
        } catch (Throwable e) {
          try (PrintWriter pw = new PrintWriter(new FileWriter("error.txt", true))) {
            pw.println(new java.util.Date().toString());
            pw.println("--------------------------------");
            pw.println(e.getMessage());
            pw.println("--------------------------------");
            e.printStackTrace(pw);
            pw.println("--------------------------------");
            if (md != null) {
              for (int i = 0; i < md.dbTable.getRowCount(); i++) {
                for (int j = 0; j < md.dbTable.getColumnCount(); j++) {
                  pw.print(md.dbTable.getValueAt(i, j));
                  pw.append('\t');
                }
                pw.println();
              }
            }
            pw.append("################################################################" + System.lineSeparator());
            pw.append(System.lineSeparator());
            pw.flush();
          } catch (Exception f) {
            Logger.getLogger(ModificationsDialog.class.getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(ModificationsDialog.class.getName()).log(Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(md, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
