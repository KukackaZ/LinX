package biochemie.dbedit;

import biochemie.Defaults;
import biochemie.linx.ResultsCard;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Janek
 */
public class AnalysersDialog extends DbDialog {
  public static boolean showAnalysersDialog(java.awt.Frame parent) {
    return new AnalysersDialog(parent).wasEdited();
  }

  /** Creates new form AnalysersDialog */
  public AnalysersDialog(java.awt.Frame parent) {
    super("Analysers", parent);

    Properties file = Defaults.loadDefaults(ResultsCard.ANALYSERS);
    if (file.isEmpty() && JOptionPane.showConfirmDialog(this, "It isn't possible to open file. Do you want to overwrite it?", "IO Exception",
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
      dispose();
      return;
    }
    ArrayList<Object[]> lines = new ArrayList<>(file.size()+1);
    for (Map.Entry<Object, Object> entry : file.entrySet()) {
      try {
        lines.add(new Object[] { new Name(entry.getKey().toString()), entry.getValue().toString() });
      } catch (Exception e) { }
    }
    Object[][] model = new Object[lines.size()+1][3];
    for (int i = 0; i < lines.size(); i++) {
      model[i] = lines.get(i);
    }

    dbTable.setModel(new javax.swing.table.DefaultTableModel(model, new String [] { "Name", "Command" }) {
      public Class getColumnClass(int columnIndex) {
        switch(columnIndex) {
          case 0:
            return Name.class;
          default:
            return String.class;
        }
      }
    });
    dbTable.getModel().addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        saved = false;
        int lr = dbTable.getRowCount()-1;
        for (int i = 0; i < dbTable.getColumnCount(); i++) {
          if (dbTable.getValueAt(lr, i) != null && !dbTable.getValueAt(lr, i).toString().isEmpty()) {
            ((DefaultTableModel)dbTable.getModel()).addRow(new Object[] { null, null });
            break;
          }
        }
      }
    });

    rest();
  }

  protected boolean saveInput() {
    if (saved) {
      return true;
    }
    Properties rows = new Properties();

    for (int i = 0; i < dbTable.getRowCount(); i++) {
      if (dbTable.getValueAt(i, 0) == null || dbTable.getValueAt(i, 0).toString().isEmpty()) {
        for (int j = 1; j < dbTable.getColumnCount(); j++) {
          if (dbTable.getValueAt(i, j) != null && dbTable.getValueAt(i, j).toString().length() > 0) {
            JOptionPane.showMessageDialog(this, "Format no." + (i+1) + " doesn't have name.", "Incomplete input", JOptionPane.WARNING_MESSAGE);
            dbTable.requestFocus();
            dbTable.setRowSelectionInterval(i, i);
            dbTable.setColumnSelectionInterval(0, 0);
            dbTable.editCellAt(i, 0);
            return false;
          }
        }
      } else {
        for (int j = 0; j < dbTable.getRowCount(); j++) {
          if (i != j && dbTable.getValueAt(j, 0) != null && dbTable.getValueAt(i, 0).toString().trim().equals(dbTable.getValueAt(j, 0).toString().trim())) {
            JOptionPane.showMessageDialog(this, "Format no." + (i+1) + " have the same name as format no." + (j+1) + ".", "Incomplete input",
                                          JOptionPane.WARNING_MESSAGE);
            return false;
          }
        }
        rows.setProperty(((Name)dbTable.getValueAt(i, 0)).toString().trim(), dbTable.getValueAt(i, 1).toString());
      }
    }

    if (!Defaults.saveDefaults(ResultsCard.ANALYSERS, rows)) {
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
      Logger.getLogger(AnalysersDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        AnalysersDialog md = null;
        try {
          md = new AnalysersDialog(null);
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
            Logger.getLogger(AnalysersDialog.class.getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(AnalysersDialog.class.getName()).log(Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(md, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
