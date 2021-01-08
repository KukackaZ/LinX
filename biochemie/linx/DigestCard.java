package biochemie.linx;

import biochemie.*;
import biochemie.dbedit.ProteasesDialog;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;

/**
 *
 * @author Janek
 */
public class DigestCard extends Card {
  private JFrame parent;
  private JPanel levelsPanel;
  private ArrayList<ProteasesPanel> levels;
  private JSpinner mcSpinner;
  private JCheckBox skipCheckBox;

  public DigestCard(String id, JButton[] movement, JFrame parent) {
    super(id, movement);
    this.parent = parent;

    //<editor-fold defaultstate="collapsed" desc=" Proteases Selection ">
    JPanel proteasesPanel = new JPanel();
    levelsPanel = new JPanel();
    levels = new ArrayList<>(1);
    JButton addButton = new JButton("Add level");
    JSeparator separator = new JSeparator();
    JButton insertButton = new JButton("Add protease");
    JButton editButton = new JButton("Edit proteases");

    proteasesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Proteases", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP));
    proteasesPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    ChemicalElement.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        updateProteasesPanels();
      }
    });
    levelsPanel.setLayout(new BoxLayout(levelsPanel, BoxLayout.LINE_AXIS));
    addProteasesPanel();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    proteasesPanel.add(levelsPanel, gbc);

    addButton.setMnemonic('D');
    addButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        addButtonActionPerformed();
      }
    });
    gbc.anchor = GridBagConstraints.SOUTH;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridheight = 1;
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 1;
    proteasesPanel.add(addButton, gbc);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.gridy = 1;
    gbc.insets = new Insets(5, 0, 5, 0);
    gbc.weighty = 0;
    proteasesPanel.add(separator, gbc);

    insertButton.setMnemonic('P');
    insertButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        insertButtonActionPerformed();
      }
    });
    gbc.gridy = 2;
    gbc.insets = new Insets(0, 0, 0, 0);
    proteasesPanel.add(insertButton, gbc);

    editButton.setMnemonic('E');
    editButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        editButtonActionPerformed();
      }
    });
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.gridy = 3;
    gbc.weighty = 1;
    proteasesPanel.add(editButton, gbc);

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc=" Constraints on Peptides ">
    JPanel constraintsPanel = new JPanel();
    JLabel mcLabel1 = new JLabel("Allowed");
    mcSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
    JLabel mcLabel2 = new JLabel("missed-cleavages.");
    skipCheckBox = new JCheckBox("Don't cleave on modification.");

    constraintsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Constraints", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP));

    mcLabel1.setDisplayedMnemonic('A');
    mcLabel1.setLabelFor(mcSpinner);

    skipCheckBox.setMnemonic('N');

    cleanConstraints();

    GroupLayout constraintsPanelLayout = new GroupLayout(constraintsPanel);
    constraintsPanel.setLayout(constraintsPanelLayout);
    constraintsPanelLayout.setHorizontalGroup(
      constraintsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(constraintsPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(constraintsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(constraintsPanelLayout.createSequentialGroup()
            .addGap(22)
            .addComponent(mcLabel1)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(mcSpinner, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(mcLabel2))
          .addComponent(skipCheckBox))
        .addContainerGap())
    );
    constraintsPanelLayout.setVerticalGroup(
      constraintsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(constraintsPanelLayout.createSequentialGroup()
        .addGroup(constraintsPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(mcLabel2)
          .addComponent(mcSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(mcLabel1))
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(skipCheckBox)
        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc=" Main Layout ">
    JPanel mainPanel = new JPanel();

    mainScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    mainScrollPane.setViewportView(mainPanel);

    GroupLayout layout = new GroupLayout(mainPanel);
    mainPanel.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addComponent(constraintsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(proteasesPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(proteasesPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(constraintsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addContainerGap())
    );
    //</editor-fold>

    JButton cleanButton = new JButton("Clean form");
    cleanButton.setMnemonic('C');
    cleanButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        clean();
      }
    });

    createLayout(new JButton[] { cleanButton });

    setMinimumSize(null);
    setMinimumSize(new Dimension(getMinimumSize().width, getMinimumSize().height + mainScrollPane.getHorizontalScrollBar().getMaximumSize().height));
    setPreferredSize(null);
    setPreferredSize(new Dimension(getPreferredSize().width, getPreferredSize().height + mainScrollPane.getHorizontalScrollBar().getMaximumSize().height));
  }

  public int getLevels() {
    int count = 0;
    for (ProteasesPanel level : levels) {
      if (!level.isEmpty()) {
        count++;
      }
    }
    return count;
  }

  public ArrayList<Collection<Protease>> getProteases() {
    ArrayList<Collection<Protease>> proteases = new ArrayList<>(levels.size());
    for (ProteasesPanel level : levels) {
      ArrayList<Protease> selected = level.getSelected(!skipCheckBox.isSelected());
      if (selected.size() > 0) {
        proteases.add(selected);
      }
    }
    return proteases;
  }

  public int getMCLimit() {
    return (Integer)mcSpinner.getValue();
  }

  public void setProteases(ArrayList<String> proteasesLevels) {
    for (int i = levels.size(); i < proteasesLevels.size(); i++) {
      addProteasesPanel();
    }
    for (int i = 0; i < proteasesLevels.size(); i++) {
      levels.get(i).setSelected(proteasesLevels.get(i));
    }
    for (int i = proteasesLevels.size(); i < levels.size(); i++) {
      levels.get(i).clean();
    }
    validate();
  }

  public void setConstraints(ArrayList<String> constraints) {
    cleanConstraints();
    for (String constraint : constraints) {
      if (constraint.contains("Maximal allowed number of missed-cleavages is ")) {
        mcSpinner.setValue(Integer.valueOf(constraint.replaceAll("[^0-9]", "")));
      } else if (constraint.equals("Cleave on modified aminoacids.")) {
        skipCheckBox.setSelected(false);
      } else if (constraint.equals("Don't cleave on modified aminoacids.")) {
        skipCheckBox.setSelected(true);
      }
    }
  }

  public void clean() {
    levels.clear();
    levelsPanel.removeAll();
    addProteasesPanel();
    validate();
    cleanConstraints();
  }

  public void cleanConstraints() {
    mcSpinner.setValue(Integer.valueOf(0));
    skipCheckBox.setSelected(false);
  }

  public ArrayList<String> logProteases() {
    ArrayList<String> ret = new ArrayList(1);
    for (ProteasesPanel level : levels) {
      String selected = level.logSelected();
      if (!selected.isEmpty()) {
        ret.add(selected);
      }
    }
    return ret;
  }

  public String logProteasesShort() {
    StringBuilder ret = new StringBuilder();
    for (ProteasesPanel level : levels) {
      String selected = level.logSelectedShort();
      if (!selected.isEmpty()) {
        if (ret.length() != 0) {
          ret.append("; ");
        }
        ret.append(selected);
      }
    }
    return ret.toString();
  }

  public ArrayList<String> logSettings() {
    ArrayList<String> ret = new ArrayList<String>(2);
    ret.add("Maximal allowed number of missed-cleavages is " + mcSpinner.getValue() + ".");
    ret.add((skipCheckBox.isSelected() ? "Don't c" : "C") + "leave on modified aminoacids.");
    return ret;
  }

  private void addButtonActionPerformed() {
    addProteasesPanel();
    validate();
  }

  private void insertButtonActionPerformed() {
    if (ProteasesFile.addProtease(parent) != null) {
      updateProteasesPanels();
    }
  }

  private void editButtonActionPerformed() {
    if (ProteasesDialog.showProteasesDialog(parent)) {
      updateProteasesPanels();
    }
  }

  public void updateProteasesPanels() {
    for (ProteasesPanel panel : levels) {
      panel.reload();
    }
  }

  private void addProteasesPanel() {
    ProteasesPanel pp = new ProteasesPanel(this, levels.size() + 1);
    if (levels.isEmpty()) {
      pp.setCloseable(false);
    } else {
      pp.setCloseable(true);
      levels.get(0).setCloseable(true);
    }
    levels.add(pp);
    levelsPanel.add(pp);
  }
  
  public void removeProteasePanel(ProteasesPanel panel) {
    int index = levels.indexOf(panel);
    if (index >= 0) {
      for (int i = index+1; i < levels.size(); i++) {
        levels.get(i).setLevel(i);
      }
      levels.remove(index);
      levelsPanel.remove(panel);
      if (levels.size() == 1) {
        levels.get(0).setCloseable(false);
      }
      levelsPanel.revalidate();
    }
  }
}
