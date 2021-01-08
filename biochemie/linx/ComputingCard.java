package biochemie.linx;

import javax.swing.*;

/**
 *
 * @author Janek
 */
public class ComputingCard extends Card {
  static final int FREE = 0;
  static final int RUN = 1;
  static final int STOP = 2;
  static final int CANCEL = 3;
  static final int FINALIZE = 4;

  private JLabel jLabel;
  private JProgressBar jProgressBar;
  private JScrollPane jScrollPane;
  private JTextArea jTextArea;
  private int status;
  private String last = "";

  public ComputingCard(String id, JButton[] movement) {
    super(id, movement);

    JPanel mainPanel = new JPanel();
    jLabel = new javax.swing.JLabel("Task: 1 of 8");
    jProgressBar = new javax.swing.JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
    jTextArea = new javax.swing.JTextArea();
    jScrollPane = new javax.swing.JScrollPane(jTextArea);

    jProgressBar.setIndeterminate(true);
    jTextArea.setFont(jLabel.getFont());
    jTextArea.setEditable(false);

    mainScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    mainScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    mainScrollPane.setViewportView(mainPanel);

    GroupLayout mainLayout = new GroupLayout(mainPanel);
    mainPanel.setLayout(mainLayout);
    mainLayout.setHorizontalGroup(
      mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(jScrollPane)
          .addComponent(jProgressBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
          .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainLayout.createSequentialGroup()
            .addComponent(jLabel)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );
    mainLayout.setVerticalGroup(
      mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(mainLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
        .addContainerGap())
    );

    createLayout(new JButton[] { });
  }

  public boolean clean(int count) {
    if (!setStatus(RUN)) {
      return false;
    }
    jLabel.setText(jLabel.getText().replaceAll(" [0-9]+ ", " 0 ").replaceAll(" [0-9]+$", " " + count));
    jTextArea.setText("");
    return true;
  }

  public void next() throws InterruptedException {
    checkStatus();
    jLabel.setText(jLabel.getText().replaceAll(" [0-9]+ ", " " + (Integer.parseInt(jLabel.getText().replaceAll("^[^0-9]*", "").replaceAll("[^0-9]*[0-9]+$", ""))+1) + " "));
    jProgressBar.setIndeterminate(true);
    jProgressBar.setStringPainted(false);
  }

  public void next(int max) throws InterruptedException {
    next(Integer.parseInt(jLabel.getText().replaceAll("^[^0-9]*", "").replaceAll("[^0-9]*[0-9]+$", ""))+1, max);
  }

  public void next(int level, int max) throws InterruptedException {
    checkStatus();
    jLabel.setText(jLabel.getText().replaceAll(" [0-9]+ ", " " + level + " "));
    jProgressBar.setIndeterminate(false);
    jProgressBar.setStringPainted(true);
    jProgressBar.setMinimum(0);
    jProgressBar.setMaximum(max);
    jProgressBar.setValue(0);
  }

  public void append(String text) throws InterruptedException {
    last = jTextArea.getText();
    replace(text);
  }

  public void replace(String text) throws InterruptedException {
    checkStatus();
    jTextArea.setText(last + text + "\n");
    jTextArea.setCaretPosition(jTextArea.getText().length());
  }

  public void add(int i) throws InterruptedException {
    checkStatus();
    jProgressBar.setValue(jProgressBar.getValue()+i);
  }

  public int getStatus() {
    return status;
  }

  public synchronized boolean setStatus(int status) {
    switch (status) {
      case RUN:
        if (this.status != FREE) {
          return false;
        }
        break;
      case FREE:
      case CANCEL:
      case STOP:
        if (this.status != RUN && this.status != status) {
          return false;
        }
        break;
      case FINALIZE:
        if (this.status == FREE || this.status == CANCEL) {
          return false;
        }
        break;
      default:
        throw new AssertionError();
    }
    this.status = status;
    return true;
  }
  
  public synchronized void release() throws InterruptedException {
    try {
      checkStatus();
    } finally {
      status = FREE;
    }
  }

  public void checkStatus() throws InterruptedException {
    if (status == CANCEL || status == STOP) {
      System.out.println("Interrupted");
      throw new InterruptedException(status + "");
    }
  }
}
