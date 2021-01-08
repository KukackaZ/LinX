package biochemie.dbedit;

import biochemie.*;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.*;

/**
 * Create Panel of rules of one protease.
 * @author jelij7am
 */
public class RulesPanel extends JPanel {
  private ArrayList<JLabel> jLabels;

  /**
   * Creates new panel of rules.
   * @param rules Array of rules. Empty array isn't allowed
   */
  public RulesPanel(String rules) {
    if (rules.isEmpty()) {
      throw new Error("Empty array isn't allowed. Error code 8855f8585.");
    }
    String[] parts = rules.split(ProteasesFile.RULE_SEPARATOR);
    jLabels = new ArrayList<>(parts.length);
    for (String rule : parts) {
      JLabel jLabel = new JLabel();
      jLabel.setText(rule);
      jLabel.setFont(new Font("Courier New", 0, 12));
      jLabels.add(jLabel);
    }

    GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);
    GroupLayout.ParallelGroup pg = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
    for (JLabel rule : jLabels) {
      pg.addComponent(rule);
    }
    layout.setHorizontalGroup(pg);
    GroupLayout.SequentialGroup sg = layout.createSequentialGroup();
    sg = sg.addComponent(jLabels.get(0));
    for (int i = 1; i < jLabels.size(); i++) {
      sg = sg.addComponent(jLabels.get(i));
    }
    layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addGroup(sg));
  }

  /**
   * Returns rules represented by this panel.
   * @return Rulese represented by this panel.
   */
  public ArrayList<String> getRules() {
    ArrayList<String> ret = new ArrayList<>(jLabels.size());
    for (JLabel jLabel : jLabels) {
      ret.add(jLabel.getText());
    }
    return ret;
  }

  public boolean isElement(java.util.Collection<String> list) {
    for (JLabel jLabel : jLabels) {
      if (!list.contains(jLabel.getText())) {
        return false;
      }
    }
    return true;
  }

  public ArrayList<String> toStrings() {
    ArrayList<String> ret = new ArrayList<>(jLabels.size());
    for (JLabel jLabel : jLabels) {
      ret.add(jLabel.getText());
    }
    return ret;
  }

  public void logRules(java.io.Writer writer) throws IOException {
    for (JLabel jLabel : jLabels) {
      writer.append(jLabel.getText() + "\t");
    }
  }
}
