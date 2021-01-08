package biochemie.dbedit;

import biochemie.Defaults;
import biochemie.linx.MeasurementCard;
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
public class FormatsDialog extends DbDialog {
  public static boolean showFormatsDialog(java.awt.Frame parent, ArrayList<String> forbidden) {
    return new FormatsDialog(parent, forbidden).wasEdited();
  }

  private ArrayList<String> forbidden;

  /** Creates new form FormatsDialog */
  public FormatsDialog(java.awt.Frame parent, ArrayList<String> forbidden) {
    super("Formats", parent);
    this.forbidden = forbidden;

    Properties file = Defaults.loadDefaults(MeasurementCard.MSVS_FILE);
    TreeMap<String, String[]> splitted = new TreeMap<>();
    for (String key : file.stringPropertyNames()) {
      splitted.put(key, file.getProperty(key).split(MeasurementCard.SEPARATOR, 5));
    }
    if (splitted.isEmpty() && JOptionPane.showConfirmDialog(this, "It isn't possible to open file. Do you want to overwrite it?", "IO Exception",
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
      dispose();
      return;
    }
    ArrayList<Object[]> lines = new ArrayList<>(file.size()+1);
    for (Map.Entry<String, String[]> entry : splitted.entrySet()) {
      try {
        lines.add(new Object[] { new Name(entry.getKey()), new Regex(entry.getValue()[4]), new Array(entry.getValue()[0]), new Index1(entry.getValue()[1]),
                                 new Index(entry.getValue()[3]), new Index(entry.getValue()[2]) });
      } catch (Exception e) { }
    }
    Object[][] model = new Object[lines.size()+1][3];
    for (int i = 0; i < lines.size(); i++) {
      model[i] = lines.get(i);
    }

    dbTable.setModel(new javax.swing.table.DefaultTableModel(model, new String [] { "Name", "Pattern", "Extensions", "Mass", "Intensity", "Retention time" }) {
      public Class getColumnClass(int columnIndex) {
        switch(columnIndex) {
          case 0:
            return Name.class;
          case 1:
            return Regex.class;
          case 2:
            return Array.class;
          case 3:
            return Index1.class;
          default:
            return Index.class;
        }
      }
    });
    dbTable.getModel().addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        saved = false;
        int lr = dbTable.getRowCount()-1;
        for (int i = 0; i < dbTable.getColumnCount(); i++) {
          if (dbTable.getValueAt(lr, i) != null && !dbTable.getValueAt(lr, i).toString().isEmpty()) {
            ((DefaultTableModel)dbTable.getModel()).addRow(new Object[] { null, null, null, null, null, null });
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

    String[] headers = new String[]{ "pattern", "extensions", "column with masses" };
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
        for (String item : forbidden) {
          if (dbTable.getValueAt(i, 0).equals(item)) {
            JOptionPane.showMessageDialog(this, "Name '" + dbTable.getValueAt(i, 0) + "' is reserved elsewhere, please select another.", "Incomplete input", JOptionPane.WARNING_MESSAGE);
            dbTable.requestFocus();
            dbTable.setRowSelectionInterval(i, i);
            dbTable.setColumnSelectionInterval(0, 0);
            dbTable.editCellAt(i, 0);
            return false;
          }
        }
        for (int j = 1; j <= headers.length; j++) {
          if ((dbTable.getValueAt(i, j) == null || dbTable.getValueAt(i, j).toString().isEmpty())) {
            JOptionPane.showMessageDialog(this, "Format no." + (i+1) + " doesn't have specified " + headers[j-1] + ".", "Incomplete input",
                                          JOptionPane.WARNING_MESSAGE);
            dbTable.requestFocus();
            dbTable.setRowSelectionInterval(i, i);
            dbTable.setColumnSelectionInterval(j, j);
            dbTable.editCellAt(i, j);
            return false;
          }
        }
        for (int j = 0; j < dbTable.getRowCount(); j++) {
          if (i != j && dbTable.getValueAt(j, 0) != null && dbTable.getValueAt(i, 0).toString().trim().equals(dbTable.getValueAt(j, 0).toString().trim())) {
            JOptionPane.showMessageDialog(this, "Format no." + (i+1) + " have the same name as format no." + (j+1) + ".", "Invalid input",
                                          JOptionPane.WARNING_MESSAGE);
            return false;
          }
        }
        rows.setProperty(((Name)dbTable.getValueAt(i, 0)).toString().trim(),
                         ((Array)dbTable.getValueAt(i, 2)).toString() + MeasurementCard.SEPARATOR +
                         ((Index1)dbTable.getValueAt(i, 3)).toString() + MeasurementCard.SEPARATOR +
                         (dbTable.getValueAt(i, 5) == null ? "0" : ((Index)dbTable.getValueAt(i, 5)).toString()) + MeasurementCard.SEPARATOR +
                         (dbTable.getValueAt(i, 4) == null ? "0" : ((Index)dbTable.getValueAt(i, 4)).toString()) + MeasurementCard.SEPARATOR +
                         ((Regex)dbTable.getValueAt(i, 1)).toString());
      }
    }

    if (!Defaults.saveDefaults(MeasurementCard.MSVS_FILE, rows)) {
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
      Logger.getLogger(FormatsDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        FormatsDialog md = null;
        try {
          md = new FormatsDialog(null, new ArrayList());
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
            Logger.getLogger(FormatsDialog.class.getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(FormatsDialog.class.getName()).log(Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(md, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
