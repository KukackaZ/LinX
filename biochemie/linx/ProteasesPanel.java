package biochemie.linx;

import biochemie.*;
import biochemie.dbedit.RulesPanel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import javax.swing.*;

/**
 * Display list of known proteases.
 * @author jelij7am
 */
public class ProteasesPanel extends JPanel {
  private DigestCard parent;
  private int i;
  private JLabel titleLabel;
  private JButton closeButton;
  private JScrollPane jScrollPane;
  private JPanel jPanel;
  private LinkedHashMap<JCheckBox, JComponent[]> proteases;
  private LinkedHashMap<RulesPanel, JCheckBox> rules;
  private LinkedHashMap<JLabel, JCheckBox> modifications;
  private JLabel error;
  private Component curr = null;
  private MouseListener mouseListener = null;
  private JCheckBox first;
  private JCheckBox last;
  private LinkedHashMap<String, String[]> backup;

  /**
   * Creates new list of proteases.
   */
  public ProteasesPanel(DigestCard parent, int i) {
    this.parent = parent;
    this.mouseListener = new MouseListener() {

      @Override
      public void mouseClicked(MouseEvent e) { }

      @Override
      public void mousePressed(MouseEvent e) {
        curr = e.getComponent();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        show(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        curr = e.getComponent();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        curr = null;
      }
    };
    initialize();
    setLevel(i);
  }
  
  public void setLevel(int i) {
    this.i = i;
    titleLabel.setText("Level no. " + i);
  }
  
  public void setCloseable(boolean enable) {
    closeButton.setEnabled(enable);
    closeButton.setVisible(enable);
  }

  public final void initialize() {
    titleLabel = new JLabel();
    closeButton = new JButton(UIManager.getIcon("InternalFrame.closeIcon"));
    jScrollPane = new JScrollPane();

    setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));

    closeButton.setDisabledIcon(new ImageIcon());
    closeButton.setHorizontalAlignment(SwingConstants.CENTER);
    closeButton.setVerticalAlignment(SwingConstants.CENTER);
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        parent.removeProteasePanel(ProteasesPanel.this);
      }
    });
    
    jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    jScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)), jScrollPane.getBorder()));

    jPanel = new JPanel();
    jPanel.setLayout(new java.awt.GridBagLayout());
    initialize2();

    jScrollPane.setViewportView(jPanel);

    GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(layout.createParallelGroup()
            .addGroup(layout.createSequentialGroup().addComponent(titleLabel)
              .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(closeButton, closeButton.getIcon().getIconWidth(), closeButton.getIcon().getIconWidth(), closeButton.getIcon().getIconWidth()))
            .addComponent(jScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
    layout.setVerticalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup()
              .addComponent(titleLabel)
              .addComponent(closeButton, closeButton.getIcon().getIconHeight(), closeButton.getIcon().getIconHeight(), closeButton.getIcon().getIconHeight()))
            .addComponent(jScrollPane, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE));

    // Bohužel to neumí započítat samo.
    recomputeSize();
  }

  private void initialize2() {
    first = null;
    last = null;
    GridBagConstraints gbc = new GridBagConstraints();
    
    try {
      proteases = new LinkedHashMap<>();
      rules = new LinkedHashMap<>();
      modifications = new LinkedHashMap<>();
      backup = ProteasesFile.getProteases();
      String prev = null;
      for (String name : backup.keySet()) {
        if (prev != null) {
          apendProtease(prev, backup.get(prev), false, gbc);
          apendSeparator(gbc);
        }
        prev = name;
      }
      if (prev == null) {
        throw new IOException("List of proteases is empty.\nAdd at least one protease please.\nError code 8787978E6.");
      } else {
        gbc.weighty = 1;
        apendProtease(prev, backup.get(prev), false, gbc);
      }
    } catch (IOException ex) {
      error = new JLabel();
      error.setText(ex.getMessage());
      jPanel.add(error);
    }
  }

  private void recomputeSize() {
    setPreferredSize(null);
    if (jScrollPane.getVerticalScrollBar().isVisible()) {
      setPreferredSize(new Dimension(getPreferredSize().width + jScrollPane.getVerticalScrollBar().getMaximumSize().width, 200));
    }
//    setMinimumSize(new Dimension(getPreferredSize().width, 100));
  }

  public void reload() {
    String setted = logSelected();
    jPanel.removeAll();
    initialize2();
    validate();
    repaint();
    setSelected(setted);
  }

  public boolean isEmpty() {
    for (JCheckBox jCheckBox : proteases.keySet()) {
      if (jCheckBox.isSelected()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns rules of proteases selected on this panel.
   * @return Rules of proteases selected on this panel.
   */
  public ArrayList<Protease> getSelected(boolean ignoreMods) {
    ArrayList<Protease> ret = new ArrayList<>();
    for (JCheckBox jCheckBox : proteases.keySet()) {
      if (jCheckBox.isSelected()) {
        for (String rule : backup.get(jCheckBox.getText())[0].split(ProteasesFile.RULE_SEPARATOR)) {
          ret.add(new Protease(jCheckBox.getText(), rule, backup.get(jCheckBox.getText())[1], ignoreMods));
        }
      }
    }
    return ret;
  }

  public String logSelected() {
    StringBuilder buff = new StringBuilder();
    for (JCheckBox jCheckBox : proteases.keySet()) {
      if (jCheckBox.isSelected()) {
        buff.append('\t').append(jCheckBox.getText()).append(" (");
        if (((JLabel)proteases.get(jCheckBox)[1]).getText().isEmpty()) {
          buff.append(' ');
          for (String rule : ((RulesPanel)proteases.get(jCheckBox)[0]).toStrings()) {
            buff.append(rule).append(' ');
          }
        } else {
          buff.append("{ ");
          for (String rule : ((RulesPanel)proteases.get(jCheckBox)[0]).toStrings()) {
            buff.append(rule).append(' ');
          }
          buff.append("}, ").append(((JLabel)proteases.get(jCheckBox)[1]).getText());
        }
        buff.append(')');
      }
    }

    return buff.length() == 0 ? "" : buff.substring(1);
  }

  public String logSelectedShort() {
    StringBuilder ret = new StringBuilder();
    for (JCheckBox jCheckBox : proteases.keySet()) {
      if (jCheckBox.isSelected()) {
        if (ret.length() != 0) {
          ret.append(", ");
        }
        ret.append(jCheckBox.getText());
      }
    }
    return ret.toString();
  }

  public void setSelected(String list) {
    if (!list.contains(" (")) {
      return;
    }
    for (String protease : list.split("\\t")) {
      for (JCheckBox jcb : proteases.keySet()) {
        if (jcb.getText().equals(protease.substring(0, protease.lastIndexOf(" (")))) {
          jcb.setSelected(true);
        }
      }
    }
  }

  public void clean() {
    for (JCheckBox jCheckBox : proteases.keySet()) {
      jCheckBox.setSelected(false);
    }
  }

  /**
   * Add protease into list.
   * @param name Name of protease.
   * @param definition Definition of protease.
   * @param gbc Constraints on layout.
   */
  private void apendProtease(String name, String[] definition, boolean selected, GridBagConstraints gbc) {
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;

    JCheckBox jCheckBox = new JCheckBox(name, selected);
    jCheckBox.addMouseListener(mouseListener);
    gbc.gridx = 0;
    gbc.insets = new Insets(0, 0, 0, 5);
    gbc.weightx = 0.5;
    jPanel.add(jCheckBox, gbc);
    if (first == null) {
      first = jCheckBox;
    }
    last = jCheckBox;

    RulesPanel rulesPanel = new RulesPanel(definition[0]);
    rulesPanel.addMouseListener(mouseListener);
    gbc.gridx = 1;
    gbc.insets = new Insets(3, 5, 1, 0);
    gbc.weightx = 0.5;
    jPanel.add(rulesPanel, gbc);

    JLabel jLabel = new JLabel(Protease.formatMods(definition[1], false));
    jLabel.setToolTipText(Protease.formatMods(definition[1], true));
    jLabel.addMouseListener(mouseListener);
    gbc.gridx = 2;
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.weightx = 1;
    jPanel.add(jLabel, gbc);

    jPanel.addMouseListener(mouseListener);

    proteases.put(jCheckBox, new JComponent[] { rulesPanel, jLabel });
    rules.put(rulesPanel, jCheckBox);
    modifications.put(jLabel, jCheckBox);
  }

  private void show(MouseEvent e) {
    if (SwingUtilities.isRightMouseButton(e) && (proteases.containsKey(curr) || rules.containsKey(curr) || modifications.containsKey(curr))) {
      final Component caller = curr;
      JPopupMenu jpm = new JPopupMenu();
      JMenuItem jmi = new JMenuItem("Edit");
      jmi.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String name = null;
          if (proteases.containsKey(caller)) {
            name = ((JCheckBox)caller).getText();
          } else if (rules.containsKey(caller)) {
            name = rules.get(caller).getText();
          } else if (modifications.containsKey(caller)) {
            name = modifications.get(caller).getText();
          }
          if (name != null) {
            if (ProteasesFile.editProtease((Frame)SwingUtilities.getRoot(parent), name, backup.get(name)[0], backup.get(name)[1]) != null) {
              parent.updateProteasesPanels();
            }
          }
        }
      });
      jpm.add(jmi);
      jmi = new JMenuItem("Remove");
      jmi.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (proteases.containsKey(caller)) {
            if (ProteasesFile.removeProtease((Frame)SwingUtilities.getRoot(parent), ((JCheckBox)caller).getText())) {
              parent.updateProteasesPanels();
            }
          } else if (rules.containsKey(caller)) {
            if (ProteasesFile.removeProtease((Frame)SwingUtilities.getRoot(parent), rules.get(caller).getText())) {
              parent.updateProteasesPanels();
            }
          } else if (modifications.containsKey(caller)) {
            if (ProteasesFile.removeProtease((Frame)SwingUtilities.getRoot(parent), modifications.get(caller).getText())) {
              parent.updateProteasesPanels();
            }
          }
        }
      });
      jpm.add(jmi);
      jpm.addSeparator();
      jmi = new JMenuItem("Move to top");
      if (caller == first || caller == proteases.get(first)[0] || caller == proteases.get(first)[1]) {
        jmi.setEnabled(false);
      } else {
        jmi.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            try {
              if (proteases.containsKey(caller)) {
                ProteasesFile.moveTop(((JCheckBox)caller).getText());
              } else if (rules.containsKey(caller)) {
                ProteasesFile.moveTop(rules.get(caller).getText());
              } else if (modifications.containsKey(caller)) {
                ProteasesFile.moveTop(modifications.get(caller).getText());
              }
              parent.updateProteasesPanels();
            } catch (IOException ex) {
              JOptionPane.showMessageDialog(parent, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        });
      }
      jpm.add(jmi);
      jmi = new JMenuItem("Move up");
      if (caller == first || caller == proteases.get(first)[0] || caller == proteases.get(first)[1]) {
        jmi.setEnabled(false);
      } else {
        jmi.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            try {
              if (proteases.containsKey(caller)) {
                ProteasesFile.moveUp(((JCheckBox)caller).getText());
              } else if (rules.containsKey(caller)) {
                ProteasesFile.moveUp(rules.get(caller).getText());
              } else if (modifications.containsKey(caller)) {
                ProteasesFile.moveUp(modifications.get(caller).getText());
              }
              parent.updateProteasesPanels();
            } catch (IOException ex) {
              JOptionPane.showMessageDialog(parent, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        });
      }
      jpm.add(jmi);
      jmi = new JMenuItem("Move on");
      jmi.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            String text = null;
            if (proteases.containsKey(caller)) {
              text = ((JCheckBox)caller).getText();
            } else if (rules.containsKey(caller)) {
              text = rules.get(caller).getText();
            } else if (modifications.containsKey(caller)) {
              text = modifications.get(caller).getText();
            }
            String[] poss = new String[proteases.size()-1];
            String first = null;
            int i = 1;
            int j = 0;
            for (JCheckBox name : proteases.keySet()) {
              if (name.getText().compareTo(text) != 0) {
                if (first == null) {
                  poss[j++] = i + " (before " + name.getText() + ")";
                } else if (first.compareTo(text) != 0) {
                  poss[j++] = i + " (between " + first + " and " + name.getText() + ")";
                }
                i++;
              }
              first = name.getText();
            }
            if (first.compareTo(text) != 0) {
              poss[j++] = i + " (after " + first + ")";
            }
            Object ret = JOptionPane.showInputDialog(parent, "Select the position to which you want to move the rule.",
                                                  "Specify position", JOptionPane.PLAIN_MESSAGE, null, poss, null);
            if (ret != null) {
              ProteasesFile.moveOn(text, Integer.parseInt(ret.toString().split(" ", 2)[0])-1);
              parent.updateProteasesPanels();
            }
          } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
          }
        }
      });
      jpm.add(jmi);
      jmi = new JMenuItem("Move down");
      if (caller == last || caller == proteases.get(last)[0] || caller == proteases.get(first)[1]) {
        jmi.setEnabled(false);
      } else {
        jmi.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            try {
              if (proteases.containsKey(caller)) {
                ProteasesFile.moveDown(((JCheckBox)caller).getText());
              } else if (rules.containsKey(caller)) {
                ProteasesFile.moveDown(rules.get(caller).getText());
              } else if (modifications.containsKey(caller)) {
                ProteasesFile.moveDown(modifications.get(caller).getText());
              }
              parent.updateProteasesPanels();
            } catch (IOException ex) {
              JOptionPane.showMessageDialog(parent, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        });
      }
      jpm.add(jmi);
      jmi = new JMenuItem("Move to end");
      if (caller == last || caller == proteases.get(last)[0] || caller == proteases.get(first)[1]) {
        jmi.setEnabled(false);
      } else {
        jmi.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            try {
              if (proteases.containsKey(caller)) {
                ProteasesFile.moveEnd(((JCheckBox)caller).getText());
              } else if (rules.containsKey(caller)) {
                ProteasesFile.moveEnd(rules.get(caller).getText());
              } else if (modifications.containsKey(caller)) {
                ProteasesFile.moveEnd(modifications.get(caller).getText());
              }
              parent.updateProteasesPanels();
            } catch (IOException ex) {
              JOptionPane.showMessageDialog(parent, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        });
      }
      jpm.add(jmi);

      jpm.show(e.getComponent(), e.getX(), e.getY());
    } else if (rules.containsKey(curr)) {
      rules.get(curr).setSelected(!rules.get(curr).isSelected());
    } else if (modifications.containsKey(curr)) {
      modifications.get(curr).setSelected(!modifications.get(curr).isSelected());
    }
  }

  /**
   * Add separator in panel
   * @param gbc Constraints on layout.
   */
  private void apendSeparator(GridBagConstraints gbc) {
    JSeparator jSeparator = new JSeparator(JSeparator.HORIZONTAL);
    jSeparator.setMinimumSize(new Dimension(1, 1));
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.weightx = 1;
    jPanel.add(jSeparator, gbc);
  }
}
