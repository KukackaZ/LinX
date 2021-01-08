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
public class BondsDialog extends DbDialog {
  public static boolean showBondsDialog(java.awt.Frame parent) {
    return new BondsDialog(parent).wasEdited();
  }

  /** Creates new form BondsDialog
   * @param parent */
  public BondsDialog(java.awt.Frame parent) {
    super("Bonds", parent);

    Properties file = Defaults.loadDefaults(BondAbstract.FILE);
    TreeMap<String, String[]> splitted = new TreeMap<>();
    for (String key : file.stringPropertyNames()) {
      splitted.put(key, file.getProperty(key).split(BondAbstract.SEPARATOR, 9));
    }
    if (splitted.isEmpty() && JOptionPane.showConfirmDialog(this, "It isn't possible to open bonds file. Do you want to overwrite it?", "IO Exception",
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
      dispose();
      return;
    }
    ArrayList<Object[]> lines = new ArrayList<>(file.size()+1);
    for (Map.Entry<String, String[]> entry : splitted.entrySet()) {
      if (entry.getValue().length >= 6) {
        Formula f;
        Formula fd;
        Formula f1;
        Formula f2;
        Formula fd1;
        Formula fd2;
        try {
          f = new Formula(new BigDecimal(entry.getValue()[0]));
        } catch (Exception e1) {
          try {
            f = new Formula(entry.getValue()[0]);
          } catch (Exception e2) {
            f = new Formula("");
          }
        }
        try {
          fd = entry.getValue()[1].isEmpty() ? null : new Formula(new BigDecimal(entry.getValue()[1]));
        } catch (Exception e1) {
          try {
            fd = new Formula(entry.getValue()[1]);
          } catch (Exception e2) {
            fd = new Formula("");
          }
        }
        try {
          f1 = entry.getValue()[4].isEmpty() ? null : new Formula(new BigDecimal(entry.getValue()[4]));
        } catch (Exception e1) {
          try {
            f1 = new Formula(entry.getValue()[4]);
          } catch (Exception e2) {
            f1 = null;
          }
        }
        try {
          f2 = entry.getValue()[6].isEmpty() ? null : new Formula(new BigDecimal(entry.getValue()[6]));
        } catch (Exception e1) {
          try {
            f2 = new Formula(entry.getValue()[6]);
          } catch (Exception e2) {
            f2 = null;
          }
        }
        try {
          fd1 = entry.getValue()[5].isEmpty() ? (f1 == null ? null : new Formula(BigDecimal.ZERO)) : new Formula(new BigDecimal(entry.getValue()[5]));
        } catch (Exception e1) {
          try {
            fd1 = new Formula(entry.getValue()[5]);
          } catch (Exception e2) {
            fd1 = f1 == null ? null : new Formula(BigDecimal.ZERO);
          }
        }
        try {
          fd2 = entry.getValue()[7].isEmpty() ? (f2 == null ? null : new Formula(BigDecimal.ZERO)) : new Formula(new BigDecimal(entry.getValue()[7]));
        } catch (Exception e1) {
          try {
            fd2 = new Formula(entry.getValue()[7]);
          } catch (Exception e2) {
            fd2 = f2 == null ? null : new Formula(BigDecimal.ZERO);
          }
        }
        lines.add(new Object[] { new Name(entry.getKey()), f, fd, new Specificity(entry.getValue()[2]), new Specificity2(entry.getValue()[3]), f1, fd1, f2, fd2 });
      }
    }
    Object[][] model = new Object[lines.size()+1][9];
    for (int i = 0; i < lines.size(); i++) {
      model[i] = lines.get(i);
    }
    model[model.length-1][4] = new Specificity2("---");

    dbTable.setModel(new javax.swing.table.DefaultTableModel(model, new String [] { "Name", "Mass", "Check Diff", "Specificity 1", "Specificity 2",
                                                                                            "Mass 1", "Check Diff 1", "Mass 2", "Check Diff 2" }) {
      Class[] classes = new Class[] { Name.class, Formula.class, Formula.class, Specificity.class, Specificity2.class, Formula.class, Formula.class, Formula.class, Formula.class };
      public Class getColumnClass(int columnIndex) {
        return classes[columnIndex];
      }
    });
    dbTable.getModel().addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        saved = false;
        int lr = dbTable.getRowCount()-1;
        for (int i = 0; i < dbTable.getColumnCount(); i++) {
          if (!(dbTable.getValueAt(lr, i) == null || dbTable.getValueAt(lr, i).toString().isEmpty() || (i == 4 && dbTable.getValueAt(lr, i).toString().equals("---")))) {
            ((DefaultTableModel)dbTable.getModel()).addRow(new Object[] { null, null, null, null, new Specificity2("---"), null, null, null, null });
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
    Properties mods = Defaults.loadDefaults(ModificationAbstract.FILE);
    Properties rows = new Properties();
    for (int i = 0; i < dbTable.getRowCount(); i++) {
      if (dbTable.getValueAt(i, 0) == null || dbTable.getValueAt(i, 0).toString().length() == 0) {
        for (int j = 1; j < dbTable.getColumnCount(); j++) {
          if (!(dbTable.getValueAt(i, j) == null || dbTable.getValueAt(i, j).toString().isEmpty() ||
                (j == 4 && dbTable.getValueAt(i, j).toString().equals("---")))) {
            JOptionPane.showMessageDialog(this, "Bond no." + (i+1) + " doesn't have name.", "Incomplete input", JOptionPane.WARNING_MESSAGE);
            dbTable.requestFocus();
            dbTable.setRowSelectionInterval(i, i);
            dbTable.setColumnSelectionInterval(0, 0);
            dbTable.editCellAt(i, 0);
            return false;
          }
        }
      } else {
        if ((dbTable.getValueAt(i, 1) == null || dbTable.getValueAt(i, 1).toString().length() == 0)) {
          JOptionPane.showMessageDialog(this, "Bond no." + (i+1) + " doesn't have specified difference.", "Incomplete input",
                                        JOptionPane.WARNING_MESSAGE);
          dbTable.requestFocus();
          dbTable.setRowSelectionInterval(i, i);
          dbTable.setColumnSelectionInterval(1, 1);
          dbTable.editCellAt(i, 1);
          return false;
        }
        if (dbTable.getValueAt(i, 4) != null && dbTable.getValueAt(i, 4).toString().equals("---") &&
            dbTable.getValueAt(i, 7) != null && !dbTable.getValueAt(i, 7).toString().isEmpty()) {
          JOptionPane.showMessageDialog(this, "Second specificity must be entered to specify a 'Mass 2'.", "Invalid input",
                                        JOptionPane.WARNING_MESSAGE);
          dbTable.requestFocus();
          dbTable.setRowSelectionInterval(i, i);
          dbTable.setColumnSelectionInterval(4, 4);
          dbTable.editCellAt(i, 4);
          return false;
        }
        for (int j = 5; j < dbTable.getColumnCount(); j+=2) {
          if ((dbTable.getValueAt(i, j) == null || dbTable.getValueAt(i, j).toString().isEmpty()) &&
              dbTable.getValueAt(i, j+1) != null && !dbTable.getValueAt(i, j+1).toString().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Non-anchored bond mass must be entered to specify its Check Diff.", "Invalid input",
                                          JOptionPane.WARNING_MESSAGE);
            dbTable.requestFocus();
            dbTable.setRowSelectionInterval(i, i);
            dbTable.setColumnSelectionInterval(j, j+1);
            dbTable.editCellAt(i, j+1);
            return false;
          }
        }
        for (int j = 0; j < dbTable.getRowCount(); j++) {
          if (i != j && dbTable.getValueAt(j, 0) != null && dbTable.getValueAt(i, 0).toString().trim().equals(dbTable.getValueAt(j, 0).toString().trim())) {
            JOptionPane.showMessageDialog(this, "Bond no." + (i+1) + " have the same name as bond no." + (j+1) + ".", "Incomplete input",
                                          JOptionPane.WARNING_MESSAGE);
            return false;
          }
        }
        for (String mod : mods.stringPropertyNames()) {
          if (mod.trim().equals(dbTable.getValueAt(i, 0).toString().trim())) {
            JOptionPane.showMessageDialog(this, "Bond no." + (i+1) + " have the same name as one modification: " + mod + ".", "Incomplete input",
                                          JOptionPane.WARNING_MESSAGE);
            return false;
          }
        }
        rows.setProperty(dbTable.getValueAt(i, 0) == null ? "" : dbTable.getValueAt(i, 0).toString().trim(),
                         ((Formula)dbTable.getValueAt(i, 1)).toString(false) + BondAbstract.SEPARATOR +
                         (dbTable.getValueAt(i, 2) == null ? BigDecimal.ZERO.toPlainString() : ((Formula)dbTable.getValueAt(i, 2)).toString(false)) + BondAbstract.SEPARATOR +
                         (dbTable.getValueAt(i, 3) == null ? "" : dbTable.getValueAt(i, 3).toString()) + BondAbstract.SEPARATOR +
                         (dbTable.getValueAt(i, 4) == null ? "" : dbTable.getValueAt(i, 4).toString()) + BondAbstract.SEPARATOR +
                         (dbTable.getValueAt(i, 5) == null ? "" : ((Formula)dbTable.getValueAt(i, 5)).toString(false)) + BondAbstract.SEPARATOR +
                         (dbTable.getValueAt(i, 6) == null ? (dbTable.getValueAt(i, 5) == null ? "" : BigDecimal.ZERO.toPlainString()) : ((Formula)dbTable.getValueAt(i, 6)).toString(false)) + BondAbstract.SEPARATOR +
                         (dbTable.getValueAt(i, 7) == null ? "" : ((Formula)dbTable.getValueAt(i, 7)).toString(false)) + BondAbstract.SEPARATOR +
                         (dbTable.getValueAt(i, 8) == null ? (dbTable.getValueAt(i, 7) == null ? "" : BigDecimal.ZERO.toPlainString()) : ((Formula)dbTable.getValueAt(i, 8)).toString(false)));
      }
    }
    if (!Defaults.saveDefaults(BondAbstract.FILE, rows)) {
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
      Logger.getLogger(BondsDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        BondsDialog bd = null;
        try {
          bd = new BondsDialog(null);
        } catch (Throwable e) {
          try (PrintWriter pw = new PrintWriter(new FileWriter("error.txt", true))) {
            pw.println(new java.util.Date().toString());
            pw.println("--------------------------------");
            pw.println(e.getMessage());
            pw.println("--------------------------------");
            e.printStackTrace(pw);
            pw.println("--------------------------------");
            if (bd != null) {
              for (int i = 0; i < bd.dbTable.getRowCount(); i++) {
                for (int j = 0; j < bd.dbTable.getColumnCount(); j++) {
                  pw.print(bd.dbTable.getValueAt(i, j));
                  pw.append('\t');
                }
                pw.println();
              }
            }
            pw.append("################################################################" + System.lineSeparator());
            pw.append(System.lineSeparator());
            pw.flush();
          } catch (Exception f) {
            Logger.getLogger(BondsDialog.class.getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(BondsDialog.class.getName()).log(Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(bd, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
