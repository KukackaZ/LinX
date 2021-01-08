package biochemie.linx;

import biochemie.Defaults;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author Janek
 */
public abstract class Card extends JPanel {
  protected JScrollPane mainScrollPane;
  protected JButton[] leftButtons;
  protected JButton[] rightButtons;
  protected Properties defaults;

  protected Card(String id, JButton[] rightButtons) {
    setName(id);
    this.mainScrollPane = new JScrollPane();
//    this.leftButtons = new JButton[0];
    this.rightButtons = rightButtons;
    this.defaults = Defaults.getDefaults(getName());
  }

  protected void createLayout(JButton[] leftButtons) {
    this.leftButtons = leftButtons;
    createLayout();
  }

  private void createLayout() {
    GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);
    GroupLayout.SequentialGroup sg = layout.createSequentialGroup();
    if (leftButtons.length > 0) {
      sg.addContainerGap();
      sg.addComponent(leftButtons[0]);
      for (int i = 1; i < leftButtons.length; i++) {
        sg.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(leftButtons[i]);
      }
    }
    sg.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
    if (rightButtons.length > 0) {
      sg.addComponent(rightButtons[0]);
      for (int i = 1; i < rightButtons.length; i++) {
        sg.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(rightButtons[i]);
      }
      sg.addContainerGap();
    }
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(sg)
      .addComponent(mainScrollPane)
    );
    GroupLayout.ParallelGroup pg = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
    for (JButton jButton : leftButtons) {
      pg.addComponent(jButton);
    }
    for (JButton jButton : rightButtons) {
      pg.addComponent(jButton);
    }
    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addComponent(mainScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(pg)
        .addContainerGap())
    );
  }

  public void applyDefaults() { }

  public final void saveDefaults() {
    logDefaults();
    Defaults.addDefaults(getName(), defaults);
  }

  protected void logDefaults() { }
}
