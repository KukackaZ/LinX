package biochemie.vyza;

import biochemie.Protein;
import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author Janek
 */
public class ProteinPanel extends JPanel {
  /**
   * Creates new form NewJPanel
   */
  public ProteinPanel(Protein protein, Set<Peptide> peptides) {
    setBorder(BorderFactory.createTitledBorder(protein.getName()));
    setLayout(new java.awt.GridBagLayout());
    
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    
    int max = 0;
    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    for (int i = 0; i < protein.length(); i++) {
      if (max < protein.charsAt(i).size()) {
        max = protein.charsAt(i).size();
      }
      JLabel jl = new JLabel(Integer.toString(i+protein.start()));
      gridBagConstraints.gridx = i;
      gridBagConstraints.gridy = 0;
      add(jl, gridBagConstraints);
      for (Character ch : protein.charsAt(i)) {
        jl = new JLabel(ch.toString(), SwingConstants.CENTER);
        gridBagConstraints.gridy++;
        add(jl, gridBagConstraints);
      }
    }
    max++;
    
    TreeMap<Integer, TreeMap<Integer, ArrayList<Peptide>>> ordered = new TreeMap();
    for (Peptide peptide : peptides) {
      int begin = peptide.getBegin() - protein.start();
      int end = peptide.getEnd() - protein.start();
      if (!ordered.containsKey(begin)) {
        ordered.put(begin, new TreeMap());
      }
      if (!ordered.get(begin).containsKey(end)) {
        ordered.get(begin).put(end, new ArrayList());
      }
      ordered.get(begin).get(end).add(peptide);
    }
    ArrayList<Integer> ends = new ArrayList();
    gridBagConstraints.fill = gridBagConstraints.BOTH;
    for (Integer begin : ordered.keySet()) {
      gridBagConstraints.gridx = begin;
      int index = -1;
      for (Integer end : ordered.get(begin).keySet()) {
        for (Peptide peptide : ordered.get(begin).get(end)) {
          while (++index < ends.size() && begin < ends.get(index)) { }
          if (index == ends.size()) {
            ends.add(end);
          } else {
            ends.set(index, end);
          }
          JPanel jp = new JPanel();
          if (peptide.getInfo() == null || peptide.getInfo().length < 2) {
            jp.setBackground(new Color(255, 0, 0));
          } else {
            int c = 255;
            jp.setBackground(peptide.getInfo()[0].isEmpty() ? peptide.getInfo()[1].isEmpty() && peptide.getInfo()[2].isEmpty() ? new Color(255, 255, 255)
                                                                                                                               : new Color(127, 127, 255)
                                                            : peptide.getInfo()[1].isEmpty() && peptide.getInfo()[2].isEmpty() ? new Color(255, 127, 127)
                                                                                                                               : new Color(191, 63, 191));
          }
          jp.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
          Dimension d = new Dimension(20*peptide.length(), 10);
          jp.setMaximumSize(d);
          jp.setMinimumSize(d);
          jp.setPreferredSize(d);
          if (peptide.getInfo() != null) {
            StringBuilder buf = new StringBuilder();
            for (String line : peptide.getInfo()) {
              buf.append(line).append("<br>");
            }
            if (buf.length() > 0) {
              jp.setToolTipText("<html>" + buf.substring(0, buf.length()-4) + "</html>");
            }
          }
          gridBagConstraints.gridwidth = peptide.length();
          gridBagConstraints.gridy = max+index;
          add(jp, gridBagConstraints);
        }
      }
    }
    
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.gridy = max+ends.size();
    gridBagConstraints.weighty = 1.0;
    for (int i = 0; i < protein.length(); i++) {
      JPanel jp = new JPanel();
      Dimension d = new Dimension(20, 0);
      jp.setMaximumSize(d);
      jp.setMinimumSize(d);
      jp.setPreferredSize(d);
      gridBagConstraints.gridx = i;
      add(jp, gridBagConstraints);
    }
    
    JPanel jp = new JPanel();
    Dimension d = new Dimension(0, 0);
    jp.setMaximumSize(d);
    jp.setMinimumSize(d);
    jp.setPreferredSize(d);
    gridBagConstraints.gridx = protein.length();
    gridBagConstraints.weightx = 1.0;
    add(jp, gridBagConstraints);
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
      java.util.logging.Logger.getLogger(ProteinPanel.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        LinkedHashSet<Peptide> peptides = new LinkedHashSet();
        peptides.add(new Peptide(1, 4, new String[]{ "Ahoj", "No nazdar" }));
        peptides.add(new Peptide(3, 8, null));
        peptides.add(new Peptide(5, 9, null));
        ProteinPanel pp = null;
        try {
          pp = new ProteinPanel(new Protein("Protein no.1", "ABCDEFGHIJ"), peptides);
          JFrame jf = new JFrame("Vyza");
          jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
          jf.setLayout(new FlowLayout());
          jf.add(pp);
          jf.pack();
          jf.setMinimumSize(jf.getMinimumSize());
          jf.setVisible(true);
        } catch (Throwable e) {
          try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("error.txt", true))) {
            pw.println(new java.util.Date().toString());
            pw.println("--------------------------------");
            pw.println(e.getMessage());
            pw.println("--------------------------------");
            e.printStackTrace(pw);
            pw.append("################################################################" + System.lineSeparator());
            pw.append(System.lineSeparator());
            pw.flush();
          } catch (Exception f) {
            java.util.logging.Logger.getLogger(ProteinPanel.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
            java.util.logging.Logger.getLogger(ProteinPanel.class.getName()).log(java.util.logging.Level.SEVERE, null, f);
          } finally {
            JOptionPane.showMessageDialog(pp, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
            System.exit(9);
          }
        }
      }
    });
  }
}
