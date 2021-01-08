package biochemie.linx;

import java.awt.event.*;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 *
 * @author Janek
 */
public class MeasurementDialog extends JDialog {
  /** A return status code - returned if Cancel button has been pressed */
  public static final int RET_CANCEL = 0;
  /** A return status code - returned if OK button has been pressed */
  public static final int RET_OK = 1;

  private JLabel infoLabel;
  private JSeparator separator;
  private JLabel separatorLabel;
  private JTextField separatorTextField;
  private JLabel massLabel;
  private JSpinner massSpinner;
  private JLabel intensityLabel;
  private JSpinner intensitySpinner;
  private JLabel timeLabel;
  private JSpinner timeSpinner;
  private JButton okButton;
  private JButton cancelButton;
  private int returnStatus = RET_CANCEL;

  public MeasurementDialog(java.awt.Frame parent) {
    super(parent, true);

    infoLabel = new JLabel("Please define structure of the file.");
    separator = new JSeparator(SwingConstants.HORIZONTAL);
    separatorLabel = new JLabel("Separator:");
    separatorTextField = new JTextField("\\t");
    massLabel = new JLabel("Mass is in column no.");
    massSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
    intensityLabel = new JLabel("Intensity is in column no.");
    intensitySpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
    timeLabel = new JLabel("Retention time is in column no.");
    timeSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
    okButton = new JButton("OK");
    cancelButton = new JButton("Cancel");

    separatorLabel.setDisplayedMnemonic('S');
    separatorLabel.setLabelFor(separatorTextField);

    massLabel.setDisplayedMnemonic('M');
    massLabel.setLabelFor(massSpinner);

    intensityLabel.setDisplayedMnemonic('I');
    intensityLabel.setLabelFor(intensitySpinner);

    timeLabel.setDisplayedMnemonic('R');
    timeLabel.setLabelFor(timeSpinner);

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        closeDialog(evt);
      }
    });

    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        okButtonActionPerformed(evt);
      }
    });

    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        cancelButtonActionPerformed(evt);
      }
    });
    InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    ActionMap actionMap = getRootPane().getActionMap();
    actionMap.put("cancel", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        doClose(RET_CANCEL);
      }
    });

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(infoLabel)
            .addGap(0, 0, Short.MAX_VALUE))
          .addComponent(separator)
          .addGroup(layout.createSequentialGroup()
            .addComponent(separatorLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(separatorTextField))
          .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(massLabel)
              .addComponent(intensityLabel)
              .addComponent(timeLabel))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(massSpinner, GroupLayout.PREFERRED_SIZE, 50, Short.MAX_VALUE)
              .addComponent(intensitySpinner, GroupLayout.PREFERRED_SIZE, 50, Short.MAX_VALUE)
              .addComponent(timeSpinner, GroupLayout.PREFERRED_SIZE, 50, Short.MAX_VALUE)))
          .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGap(0, 0, Short.MAX_VALUE)
            .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(cancelButton)))
        .addContainerGap())
    );

    layout.linkSize(SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

    layout.setVerticalGroup(
      layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(infoLabel)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(separator, GroupLayout.PREFERRED_SIZE, 4, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(separatorTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(separatorLabel))
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(massSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(massLabel))
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(intensitySpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(intensityLabel))
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(timeSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(timeLabel))
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(cancelButton)
          .addComponent(okButton))
        .addContainerGap()
    );

    getRootPane().setDefaultButton(okButton);

    pack();
    setVisible(true);
  }

  /** @return the return status of this dialog - one of RET_OK or RET_CANCEL */
  public int getReturnStatus() {
      return returnStatus;
  }

  public String getSeparatorTextField() {
    return separatorTextField.getText();
  }

  public int getMassSpinner() {
    return (Integer)massSpinner.getValue();
  }

  public int getIntensitySpinner() {
    return (Integer)intensitySpinner.getValue();
  }

  public int getTimeSpinner() {
    return (Integer)timeSpinner.getValue();
  }

  private void okButtonActionPerformed(ActionEvent evt) {
    if (separatorTextField.getText().isEmpty()) {
      JOptionPane.showMessageDialog(this, "Separator field cannot be empty.", "Invalid input", JOptionPane.WARNING_MESSAGE);
      return;
    }
    try {
      Pattern.compile(separatorTextField.getText());
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Separator pattern isn't valid.\n################################\n" + e.getMessage(), "Invalid input", JOptionPane.WARNING_MESSAGE);
      return;
    }
    if ((Integer)massSpinner.getValue() == 0) {
      JOptionPane.showMessageDialog(this, "Mass column must be specified.", "Invalid input", JOptionPane.WARNING_MESSAGE);
      return;
    }
    if (((Integer)intensitySpinner.getValue() != 0 || (Integer)timeSpinner.getValue() != 0) && (massSpinner.getValue().equals(intensitySpinner.getValue()) ||
            massSpinner.getValue().equals(timeSpinner.getValue()) || intensitySpinner.getValue().equals(timeSpinner.getValue()))) {
      JOptionPane.showMessageDialog(this, "Columns must be unique.", "Invalid input", JOptionPane.WARNING_MESSAGE);
      return;
    }
    doClose(RET_OK);
  }

  private void cancelButtonActionPerformed(ActionEvent evt) {
    doClose(RET_CANCEL);
  }

  /**
 * Closes the dialog
 */
  private void closeDialog(WindowEvent evt) {
    doClose(RET_CANCEL);
  }

  private void doClose(int retStatus) {
    returnStatus = retStatus;
    setVisible(false);
    dispose();
  }
}
