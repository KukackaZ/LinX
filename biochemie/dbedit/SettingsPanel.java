package biochemie.dbedit;

import javax.swing.*;

public abstract class SettingsPanel extends JPanel {

  public SettingsPanel(String title) {
    setBorder(BorderFactory.createTitledBorder(title));
    
    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGap(0, 400, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                  .addGap(0, 300, Short.MAX_VALUE)
    );
  }
  
  public boolean isChanged() {
    return false;
  }
  
  public void save() { }
}
