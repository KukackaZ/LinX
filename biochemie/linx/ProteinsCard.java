package biochemie.linx;

import biochemie.Protein;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author Janek
 */
public class ProteinsCard extends Card {
  public static final String PROTEINS_FILE = "proteins";

  private JPanel mainPanel;
  private ArrayList<ProteinPanel> proteins;
  String PATH = ".";

  public ProteinsCard(String id, JButton[] movement) {
    super(id, movement);
    mainPanel = new JPanel();
    proteins = new ArrayList<>(1);
    JButton cleanButton = new JButton("Clean form");
    JButton addButton = new JButton("Add protein");

    mainScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    mainScrollPane.setViewportView(mainPanel);

    addProtein();

    cleanButton.setMnemonic('C');
    cleanButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        clean();
      }
    });

    addButton.setMnemonic('A');
    addButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        addProtein();
        proteins.get(proteins.size()-1).setFocus();
      }
    });

    createLayout(new JButton[] { cleanButton, addButton });

    mainScrollPane.setMinimumSize(null);
    mainScrollPane.setMinimumSize(new Dimension(getMinimumSize().width + 2*mainScrollPane.getVerticalScrollBar().getMaximumSize().width, getMinimumSize().height));
    mainScrollPane.setPreferredSize(null);
    mainScrollPane.setPreferredSize(new Dimension(getPreferredSize().width + 2*mainScrollPane.getVerticalScrollBar().getMaximumSize().width, getPreferredSize().height));
  }

  public void applyDefaults() {
    applyPath();
  }

  public void logPath(String value) {
    defaults.setProperty("Path", value);
    applyPath();
  }

  public void applyPath() {
    if (defaults.containsKey("Path")) {
      PATH = defaults.getProperty("Path");
    }
  }

  public ArrayList<String> getForbiddenNames() {
    ArrayList<String> ret = new ArrayList<>();
    // TODO: V proteinsPanel udělat filtry static a popisy přidat do samostatného objektu
    return ret;
  }

  public boolean check(boolean alert) {
    boolean exist = false;
    for (ProteinPanel protein : proteins) {
      if (!protein.check(alert)) {
        protein.setFocus();
        mainPanel.scrollRectToVisible(protein.getBounds());
        return false;
      }
      if (!protein.isEmpty()) {
        for (ProteinPanel second : proteins) {
          if (protein != second && !second.isEmpty() && protein.getIdentifier().equals(second.getIdentifier())) {
            if (alert) {
              JOptionPane.showMessageDialog(this, "Names of proteins must be unique:" + System.lineSeparator() + protein.getIdentifier(), "Ambiguity", JOptionPane.ERROR_MESSAGE);
            }
            mainPanel.scrollRectToVisible(protein.getBounds());
            second.setFocus();
            return false;
          }
        }
        exist = true;
      }
    }
    
    if (exist) {
      return true;
    }
    if (alert) {
      JOptionPane.showMessageDialog(this, "At least one protein must be specified!", "No input", JOptionPane.ERROR_MESSAGE);
    }
    proteins.get(0).requestFocusInWindow();
    return false;
  }

  public void clean() {
    proteins.clear();
    addProtein();
  }

  public LinkedHashMap<String, Protein> getProteins() {
    LinkedHashMap<String, Protein> ret = new LinkedHashMap(proteins.size());
    if (!check(true)) {
      return ret;
    }
    for (ProteinPanel protein : proteins) {
      if (!protein.isEmpty()) {
        ret.put(protein.getIdentifier(), protein.getProtein());
      }
    }
    return ret;
  }

  public HashSet<String> getProteinNames() {
    HashSet<String> ret = new HashSet();
    for (ProteinPanel protein : proteins) {
      if (!protein.isEmpty()) {
        ret.add(protein.getIdentifier());
      }
    }
    return ret;
  }

  public void setProteins(ArrayList<String[]> value) {
    if (value == null || value.isEmpty()) {
      clean();
      return;
    }
    int i;
    for (i = proteins.size(); i < value.size(); i++) {
      addProtein();
    }
    i = 0;
    for (String[] protein : value) {
      if (protein == null) {
        protein = new String[0];
      }
      switch (protein.length) {
        case 0:
          proteins.get(i).setIdentifier(null);
          proteins.get(i).setShift("1");
          proteins.get(i).setChain("");
          break;
        case 1:
          proteins.get(i).setIdentifier(null);
          proteins.get(i).setShift("1");
          proteins.get(i).setChain(protein[0]);
          break;
        case 2:
          proteins.get(i).setIdentifier(protein[0]);
          proteins.get(i).setShift("1");
          proteins.get(i).setChain(protein[1]);
          break;
        case 3:
          proteins.get(i).setIdentifier(protein[0]);
          proteins.get(i).setShift(protein[1]);
          proteins.get(i).setChain(protein[2]);
          break;
        default:
          proteins.get(i).setIdentifier(protein[0]);
          proteins.get(i).setShift(protein[1]);
          StringBuilder sb = new StringBuilder(protein[2]);
          for (int j = 3; j < protein.length; j++) {
            sb.append(protein[j]);
          }
          proteins.get(i).setChain(sb.toString());
          break;
      }
      i++;
    }
    while (i < proteins.size()) {
      removeActionPerformed(i);
    }
  }

  private void addProtein() {
    addProtein("", "", "");
  }

  private void addProtein(String name, String shift, String protein) {
    proteins.add(new ProteinPanel(this, proteins.size()+1, name, shift, protein));
    proteins.get(0).setAlone(proteins.size() == 1);
    updateLayout();
  }

  public void insertProtein(String name, String shift, String protein) {
    for (ProteinPanel panel : proteins) {
      if (panel.isEmpty()) {
        panel.setIdentifier(name);
        panel.setShift(shift);
        panel.setChain(protein);
        return;
      }
    }
    addProtein(name, shift, protein);
  }

  public void removeActionPerformed(int i) {
    for (int j = i; j < proteins.size(); j++) {
      proteins.get(i-1).setIdentifier(proteins.get(i).isIdentifierDefault() ? null : proteins.get(i).getIdentifier());
      proteins.get(i-1).setShift(Integer.toString(proteins.get(i).getShift()));
      proteins.get(i-1).setChain(proteins.get(i).getChain());
    }
    proteins.remove(proteins.size()-1);

    proteins.get(0).setAlone(proteins.size() == 1);
    updateLayout();
  }

  private void updateLayout() {
    mainPanel.removeAll();
    GroupLayout mainLayout = new GroupLayout(mainPanel);
    mainPanel.setLayout(mainLayout);
    GroupLayout.ParallelGroup pg = mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
    GroupLayout.SequentialGroup sg = mainLayout.createSequentialGroup()
      .addContainerGap();
    for (JPanel panel : proteins) {
      pg.addGroup(mainLayout.createSequentialGroup()
          .addContainerGap()
          .addComponent(panel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
          .addContainerGap());
      sg.addComponent(panel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        .addContainerGap();
    }
    mainLayout.setHorizontalGroup(pg);
    mainLayout.setVerticalGroup(
      mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(sg)
    );
  }
}
