package biochemie.vyza;

import biochemie.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;

/**
 *
 * @author Janek
 */
public class Vyza extends JFrame {
  private static final String MAIN = "Vyza";
  
  public Vyza(LinkedHashMap<Protein, Set<Peptide>> data) {
    this(data, DISPOSE_ON_CLOSE);
  }

  public Vyza(LinkedHashMap<Protein, Set<Peptide>> data, int operation) {
    Properties defaults = Defaults.getDefaults(MAIN);
    setName(MAIN);
    setTitle(MAIN);
    setDefaultCloseOperation(operation);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        doClose();
      }
    });

    final JPanel jPanel = new JPanel();
    JScrollPane jScrollPane = new JScrollPane(jPanel);
    jPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent e) {
        if (!e.isPopupTrigger()) {
          return;
        }
        JFileChooser jfc = new JFileChooser();
        for (String ext : javax.imageio.ImageIO.getWriterFileSuffixes()) {
          switch (ext.toLowerCase()) {
            case "png":
              jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                  if (f.isDirectory()) {
                    return true;
                  }
                  int i = f.getName().lastIndexOf('.');
                  String extension = i < 0 ? "" : f.getName().substring(i).toLowerCase();
                  return extension.equals(".png");
                }

                @Override
                public String getDescription() {
                  return "Portable Network Graphic [.png]";
                }
              });
              break;
            case "bmp":
              jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                  if (f.isDirectory()) {
                    return true;
                  }
                  int i = f.getName().lastIndexOf('.');
                  String extension = i < 0 ? "" : f.getName().substring(i).toLowerCase();
                  return extension.equals(".bmp");
                }

                @Override
                public String getDescription() {
                  return "Windows Bitmap [.bmp]";
                }
              });
              break;
            case "jpg":
              jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                  if (f.isDirectory()) {
                    return true;
                  }
                  int i = f.getName().lastIndexOf('.');
                  String extension = i < 0 ? "" : f.getName().substring(i).toLowerCase();
                  return extension.equals(".jpg") || extension.equals(".jpeg");
                }

                @Override
                public String getDescription() {
                  return "JPEG File Interchange Format [.jpg]";
                }
              });
              break;
            case "gif":
              jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                  if (f.isDirectory()) {
                    return true;
                  }
                  int i = f.getName().lastIndexOf('.');
                  String extension = i < 0 ? "" : f.getName().substring(i).toLowerCase();
                  return extension.equals(".gif");
                }

                @Override
                public String getDescription() {
                  return "Graphics Interchange Format [.gif]";
                }
              });
              break;
          }
        }
        jfc.setAcceptAllFileFilterUsed(false);
        if (jfc.getChoosableFileFilters().length > 0) {
          jfc.setFileFilter(jfc.getChoosableFileFilters()[0]);
        }
        while (jfc.showSaveDialog(Vyza.this) == JFileChooser.APPROVE_OPTION) {
          String d = jfc.getFileFilter().getDescription();
          String ext = d.substring(d.lastIndexOf('[')+2, d.lastIndexOf(']'));
          File name = jfc.getSelectedFile().getName().endsWith(ext) ? jfc.getSelectedFile() : new File(jfc.getSelectedFile().getAbsolutePath() + "." + ext);
          if (name.exists()) {
            int res = JOptionPane.showConfirmDialog(Vyza.this, "There exists a file with the chosen name." + System.lineSeparator() + "Do you wish to override it?",
                                                    "Override?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (res == JOptionPane.NO_OPTION) {
              continue;
            } else if (res != JOptionPane.YES_OPTION) {
              break;
            }
          }
          java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(jPanel.getWidth(), jPanel.getHeight(), java.awt.image.BufferedImage.TYPE_INT_BGR);
          jPanel.paint(bi.getGraphics());
          try {
            javax.imageio.ImageIO.write(bi, ext, name);
            break;
          } catch (Exception ex) {
            if (JOptionPane.showConfirmDialog(Vyza.this, "It was not possible to save file, do you wish to try it again?" + System.lineSeparator() + System.lineSeparator() +
                                                         ex.getMessage(), "Unexpected exception", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION) {
              break;
            }
          }
        }
      }
    });

    GroupLayout mainLayout = new GroupLayout(jPanel);
    jPanel.setLayout(mainLayout);
    GroupLayout.ParallelGroup pg = mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
    GroupLayout.SequentialGroup sg = mainLayout.createSequentialGroup();
    boolean first = true;
    for (Map.Entry<Protein, Set<Peptide>> peptides : data.entrySet()) {
      JPanel protein = new ProteinPanel(peptides.getKey(), peptides.getValue());
      pg.addComponent(protein, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
      if (first) {
        sg.addContainerGap();
        first = false;
      } else {
        sg.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
      }
      sg.addComponent(protein, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE);
    }
    mainLayout.setHorizontalGroup(
      mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(mainLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(pg)
        .addContainerGap())
    );
    mainLayout.setVerticalGroup(
      mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(sg
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(jScrollPane)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(jScrollPane)
    );

    pack();
    setMinimumSize(getMinimumSize());
    Defaults.setWindowDefaults(this, Defaults.getDefaults(getName()));
    if (defaults.containsKey("ExtendedState")) {
      setExtendedState(Integer.parseInt(defaults.getProperty("ExtendedState")));
    }
    revalidate();
  }

  private void doClose() {
    Properties dict = new Properties();
    dict.setProperty("ExtendedState", String.valueOf(getExtendedState()));
    setExtendedState(NORMAL);
    Defaults.putWindowDefaults(this, dict);
    Defaults.addDefaults(getName(), dict);
  }
  
  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    /* Set the Nimbus look and feel */
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
    /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
     * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
     */
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
      Logger.getLogger(Vyza.class.getName()).log(Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        LinkedHashMap<Protein, Set<Peptide>> data = new LinkedHashMap<>();
        LinkedHashSet<Peptide> peptides = new LinkedHashSet();
        peptides.add(new Peptide(1, 4, new String[]{ "Ahoj", "No nazdar" }));
        peptides.add(new Peptide(3, 8, null));
        peptides.add(new Peptide(5, 9, null));
        data.put(new Protein("Protein no.1", "ABCDEFGHIJ"), peptides);
        peptides = new LinkedHashSet();
        peptides.add(new Peptide(1, 4, new String[]{ "Ahoj", "No nazdar" }));
        peptides.add(new Peptide(3, 8, null));
        peptides.add(new Peptide(5, 9, null));
        peptides.add(new Peptide(4, 5, null));
        data.put(new Protein("Protein no.2", "RSTUVWXYZ"), peptides);
        Vyza vyza = null;
        try {
          vyza = new Vyza(data, EXIT_ON_CLOSE);
          vyza.setVisible(true);
        } catch (Throwable e) {
          try (PrintWriter pw = new PrintWriter(new FileWriter("error.txt", true))) {
            pw.println(new java.util.Date().toString());
            pw.println("--------------------------------");
            pw.println(e.getMessage());
            pw.println("--------------------------------");
            e.printStackTrace(pw);
            pw.println("--------------------------------");
            if (vyza == null) {
              // TODO Vypsat vykreslované peptidy, příp. i parametry zobrazení.
            }
            pw.append("################################################################" + System.lineSeparator());
            pw.append(System.lineSeparator());
            pw.flush();
          } catch (Exception f) {
            Logger.getLogger(Vyza.class.getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(Vyza.class.getName()).log(Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(vyza, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
