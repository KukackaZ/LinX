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
public class ParsersDialog extends DbDialog {
  private static String FILE_MARK = "<FILE>";

  public static boolean showMeasurementsDialog(java.awt.Frame parent, String title, String filepath, ArrayList<String> forbidden) {
    return new ParsersDialog(parent, title, filepath, forbidden).wasEdited();
  }

  private String file;
  private ArrayList<String> forbidden;

  /** Creates new form AnalysersDialog */
  public ParsersDialog(java.awt.Frame parent, String title, String filepath, ArrayList<String> forbidden) {
    super(title + " parsers", parent, "Commands must contain '" + FILE_MARK + "' representing the selected filepath.");
    this.file = filepath;
    this.forbidden = forbidden;

    Properties file = Defaults.loadDefaults(this.file);
    if (file.isEmpty() && JOptionPane.showConfirmDialog(this, "It isn't possible to open file. Do you want to overwrite it?", "IO Exception",
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
      dispose();
      return;
    }
    ArrayList<Object[]> lines = new ArrayList<>(file.size()+1);
    for (Map.Entry<Object, Object> entry : file.entrySet()) {
      try {
        String[] value = entry.getValue().toString().split(MeasurementCard.SEPARATOR, 3);
        lines.add(new Object[] { new Name(entry.getKey().toString()), new Array(value[0]), value.length < 2 ? null : value[1], value.length < 3 ? null : value[2] });
      } catch (Exception e) { }
    }
    Object[][] model = new Object[lines.size()+1][3];
    for (int i = 0; i < lines.size(); i++) {
      model[i] = lines.get(i);
    }

    dbTable.setModel(new javax.swing.table.DefaultTableModel(model, new String [] { "Name", "Extension", "Standard parsing", "Quiet parsing" }) {
      public Class getColumnClass(int columnIndex) {
        switch(columnIndex) {
          case 0:
            return Name.class;
          case 1:
            return Array.class;
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
            ((DefaultTableModel)dbTable.getModel()).addRow(new Object[] { null, null, null, null });
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
        for (int j = 0; j < dbTable.getRowCount(); j++) {
          if (i != j && dbTable.getValueAt(j, 0) != null && dbTable.getValueAt(i, 0).toString().trim().equals(dbTable.getValueAt(j, 0).toString().trim())) {
            JOptionPane.showMessageDialog(this, "Format' no." + (i+1) + " " + (j+1) + " have the same name '" + dbTable.getValueAt(j, 0) + "'.", "Incomplete input",
                                          JOptionPane.WARNING_MESSAGE);
            return false;
          }
        }
        if (dbTable.getValueAt(i, 1) == null || dbTable.getValueAt(i, 1).toString().isEmpty()) {
          JOptionPane.showMessageDialog(this, "Format no." + (i+1) + " doesn't have assigned extension.", "Incomplete input",
                                          JOptionPane.WARNING_MESSAGE);
          return false;
        }
        for (int j = 2; j < dbTable.getColumnCount(); j++) {
          String cell = (String)dbTable.getValueAt(i, j);
          if (cell == null || !cell.contains(FILE_MARK)) {
            if (j == 2 || !(cell == null || cell.isEmpty())) {
              if (JOptionPane.showConfirmDialog(this, "Format '" + dbTable.getValueAt(i, 0) + "' (line #" + (i+1) + ") does not contains '" + FILE_MARK +
                                                      "' mark within the command for " + dbTable.getColumnName(j) + " (" + cell + ")." + System.lineSeparator() +
                                                      "The '" + FILE_MARK + "' indicates location of a filename selected for parsing within the command" +
                                                      System.lineSeparator() + "Do you wish to append it to the end of command?",
                                                "Invalid input", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                if (cell == null || cell.isEmpty()) {
                  cell = FILE_MARK;
                } else {
                  if (!Character.isWhitespace(cell.charAt(cell.length()-1))) {
                    cell += " ";
                  }
                  cell += FILE_MARK;
                }
                dbTable.setValueAt(cell, i, j);
              } else {
                return false;
              }
            }
          } else if (cell.indexOf(FILE_MARK, cell.indexOf(FILE_MARK) + FILE_MARK.length()) >= 0 &&
                      JOptionPane.showConfirmDialog(this, "Format '" + dbTable.getValueAt(i, 0) + "' (line #" + (i+1) + ") contains multiple '" + FILE_MARK +
                                                          "' mark within the command for " + dbTable.getColumnName(j) + " (" + cell + ")." +
                                                          System.lineSeparator() + "Are you sure, it is not a mistake?",
                                                    "Invalid input", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
              return false;
          }
        }
        rows.setProperty(((Name)dbTable.getValueAt(i, 0)).toString().trim(), (dbTable.getValueAt(i, 1) == null ? "" : ((Array)dbTable.getValueAt(i, 1)).toString()) +
                                                                             MeasurementCard.SEPARATOR + dbTable.getValueAt(i, 2).toString() + MeasurementCard.SEPARATOR +
                                                                             (dbTable.getValueAt(i, 3) == null ? "" : dbTable.getValueAt(i, 3).toString()));
      }
    }

    if (!Defaults.saveDefaults(file, rows)) {
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
      Logger.getLogger(ParsersDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    //</editor-fold>
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        ParsersDialog md = null;
        try {
          md = new ParsersDialog(null, "Test", "test.txt", new ArrayList());
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
            Logger.getLogger(ParsersDialog.class.getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(ParsersDialog.class.getName()).log(Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(md, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
