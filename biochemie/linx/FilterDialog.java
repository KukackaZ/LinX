package biochemie.linx;

import biochemie.Defaults;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.tree.*;

/**
 *
 * @author Janek
 */
public class FilterDialog extends JDialog {
  public static final String ID = "Filtering";
  /**
   * A return status code - returned if Cancel button has been pressed
   */
  public static final int RET_CANCEL = 0;
  /**
   * A return status code - returned if OK button has been pressed
   */
  public static final int RET_OK = 1;
  public static final String EMPTY = "empty";
  public static final String MODIFICATIONS = "modifications";
  public static final String BONDS = "bonds";
  public static final String MATCHES = "matches";
  public static final String ARITHMETIC = "arithmetic";
  public static final String AND = "AND";
  public static final String OR = "OR";
  public static final String TRUE = "TRUE";
  public static final String FALSE = "FALSE";
  public static final String NOT = "NOT";
  public static final String[] TYPES = new String[] { "Assigned", "Modified", "X-linked", "Interpeptide", "Intrapeptide", "Ambiguous", "CONTAINS", "ARITHMETIC" };
  public static final String[] MATCHING = new String[] { "PROTEIN", "CHAIN", "MODIFICATIONS", "BONDS", "RETENTION TIME", "OTHER" };
  public static final String[] ARITHMETICS = new String[] { "EXP. MASS", "THR. MASS", "ERROR", "INTENSITY" };
  public static final String[] COMPARATORS = new String[] { "<", "≤", "≥", ">" };

  private Properties defaults;
  private JPanel addPanel;
  private JCheckBox notCheckBox;
  private JComboBox typeComboBox;
  private JPanel cardPanel;
  private JPanel emptyPanel;
  private JPanel modificationsPanel;
  private JComboBox modificationsComboBox;
  private JPanel bondsPanel;
  private JComboBox bondsComboBox;
  private JPanel matchesPanel;
  private JTextField matchesTextField;
  private JLabel inLabel;
  private JComboBox matchesComboBox;
  private JPanel arithmeticPanel;
  private JComboBox arithmeticComboBox;
  private JComboBox inequalityComboBox;
  private JFormattedTextField arithmeticFormattedTextField;
  private JButton addButton;
  private JScrollPane mainScrollPane;
  private JTree mainTree;
  private DefaultMutableTreeNode activeNode;
  private DefaultMutableTreeNode inactiveNode;
  private JButton andButton;
  private JButton orButton;
  private JButton helpButton;
  private JButton okButton;
  private JButton cancelButton;
  private int returnStatus = RET_CANCEL;
  private JPopupMenu menu = null;
  private DefaultMutableTreeNode[] copy = new DefaultMutableTreeNode[0];
  private DefaultMutableTreeNode edit = null;
  private boolean filter;

  public FilterDialog(Frame parent, Properties defaults) {
    super(parent, "Filters", true);
    this.defaults = defaults;
    addPanel = new JPanel();
    notCheckBox = new JCheckBox(NOT);
    typeComboBox = new JComboBox(TYPES);
    cardPanel = new JPanel();
    emptyPanel = new JPanel();
    matchesPanel = new JPanel();
    modificationsPanel = new JPanel();
    modificationsComboBox = new JComboBox(new Object[1]);
    bondsPanel = new JPanel();
    bondsComboBox = new JComboBox(new Object[1]);
    matchesTextField = new JTextField();
    inLabel = new JLabel("in");
    matchesComboBox = new JComboBox(MATCHING);
    arithmeticPanel = new JPanel();
    arithmeticComboBox = new JComboBox(ARITHMETICS);
    inequalityComboBox = new JComboBox(COMPARATORS);
    arithmeticFormattedTextField = new JFormattedTextField(0);
    addButton = new JButton("Add");
    mainScrollPane = new JScrollPane();
    mainTree = new JTree();
    andButton = new JButton(AND + " node");
    orButton = new JButton(OR + " node");
    helpButton = new JButton("Help");
    okButton = new JButton("OK");
    cancelButton = new JButton("Cancel");

    setName(ID);

    notCheckBox.setMnemonic('N');

    typeComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        switch (typeComboBox.getSelectedIndex()) {
          case 1:
            notCheckBox.setEnabled(true);
            ((CardLayout)cardPanel.getLayout()).show(cardPanel, MODIFICATIONS);
            break;
          case 2:
            notCheckBox.setEnabled(true);
            ((CardLayout)cardPanel.getLayout()).show(cardPanel, BONDS);
            break;
          case 3:
          case 4:
            notCheckBox.setEnabled(false);
            ((CardLayout)cardPanel.getLayout()).show(cardPanel, BONDS);
            break;
          case 6:
            notCheckBox.setEnabled(true);
            ((CardLayout)cardPanel.getLayout()).show(cardPanel, MATCHES);
            break;
          case 7:
            notCheckBox.setEnabled(false);
            ((CardLayout)cardPanel.getLayout()).show(cardPanel, ARITHMETIC);
            break;
          default:
            notCheckBox.setEnabled(true);
            ((CardLayout)cardPanel.getLayout()).show(cardPanel, EMPTY);
        }
      }
    });

    cardPanel.setLayout(new CardLayout());

    GroupLayout emptyLayout = new GroupLayout(emptyPanel);
    emptyPanel.setLayout(emptyLayout);
    emptyLayout.setHorizontalGroup(
      emptyLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGap(0, 0, Short.MAX_VALUE)
    );
    emptyLayout.setVerticalGroup(
      emptyLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGap(0, 0, Short.MAX_VALUE)
    );
    cardPanel.add(emptyPanel, EMPTY);

    GroupLayout modificationsLayout = new GroupLayout(modificationsPanel);
    modificationsPanel.setLayout(modificationsLayout);
    modificationsLayout.setHorizontalGroup(modificationsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                           .addComponent(modificationsComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
    modificationsLayout.setVerticalGroup(modificationsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                         .addComponent(modificationsComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
    cardPanel.add(modificationsPanel, MODIFICATIONS);

    GroupLayout bondsLayout = new GroupLayout(bondsPanel);
    bondsPanel.setLayout(bondsLayout);
    bondsLayout.setHorizontalGroup(bondsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                     .addComponent(bondsComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
    bondsLayout.setVerticalGroup(bondsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                   .addComponent(bondsComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
    cardPanel.add(bondsPanel, BONDS);

    matchesTextField.setPreferredSize(new Dimension(100, 20));

    inLabel.setDisplayedMnemonic('I');
    inLabel.setLabelFor(matchesTextField);

    GroupLayout matchesLayout = new GroupLayout(matchesPanel);
    matchesPanel.setLayout(matchesLayout);
    matchesLayout.setHorizontalGroup(
      matchesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(GroupLayout.Alignment.TRAILING, matchesLayout.createSequentialGroup()
        .addComponent(matchesTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(inLabel)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(matchesComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
    );
    matchesLayout.setVerticalGroup(
      matchesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(matchesLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(matchesComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addComponent(inLabel)
        .addComponent(matchesTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
    );
    cardPanel.add(matchesPanel, MATCHES);

    arithmeticComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        switch (arithmeticComboBox.getSelectedIndex()) {
          case 2:
            arithmeticFormattedTextField.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(biochemie.Defaults.sPpmFormat)));
            break;
          case 3:
            arithmeticFormattedTextField.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(biochemie.Defaults.intensityFormat)));
            break;
          default:
            arithmeticFormattedTextField.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(biochemie.Defaults.uMassShortFormat)));
        }
      }
    });

    GroupLayout arithmeticLayout = new GroupLayout(arithmeticPanel);
    arithmeticPanel.setLayout(arithmeticLayout);
    arithmeticLayout.setHorizontalGroup(
      arithmeticLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(GroupLayout.Alignment.TRAILING, arithmeticLayout.createSequentialGroup()
        .addComponent(arithmeticComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(inequalityComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(arithmeticFormattedTextField, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE))
    );
    arithmeticLayout.setVerticalGroup(
      arithmeticLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(arithmeticLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(arithmeticFormattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addComponent(inequalityComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addComponent(arithmeticComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
    );

    cardPanel.add(arithmeticPanel, ARITHMETIC);

    addButton.setMnemonic('A');
    addButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        DefaultMutableTreeNode node;
        String text = typeComboBox.getSelectedItem().toString().toUpperCase();
        switch (typeComboBox.getSelectedIndex()) {
          case 0:
          case 5:
            node = new DefaultMutableTreeNode((notCheckBox.isSelected() ? "NOT " : "") + text, false);
            break;
          case 1:
            if (notCheckBox.isSelected()) {
              text = NOT + ' ' + text;
            }
            if (modificationsComboBox.getSelectedItem() != null) {
              text += " BY " + modificationsComboBox.getSelectedItem().toString();
            }
            node = new DefaultMutableTreeNode(text, false);
            break;
          case 2:
            if (notCheckBox.isSelected()) {
              text = "NOT " + text;
            }
            if (bondsComboBox.getSelectedItem() != null) {
              text += " BY " + bondsComboBox.getSelectedItem().toString();
            }
            node = new DefaultMutableTreeNode(text, false);
            break;
          case 3:
          case 4:
            if (bondsComboBox.getSelectedItem() != null) {
              text += " " + bondsComboBox.getSelectedItem().toString();
            }
            node = new DefaultMutableTreeNode(text, false);
            break;
          case 6:
            try {
              Pattern.compile(matchesTextField.getText());
              node = new DefaultMutableTreeNode(matchesComboBox.getSelectedItem().toString().toUpperCase() + (notCheckBox.isSelected() ? ' ' + NOT + ' ' : " ") + text
                                                + ": " + matchesTextField.getText(), false);
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(FilterDialog.this, ex.getLocalizedMessage(), "Syntax Error", JOptionPane.ERROR_MESSAGE);
              return;
            }
            break;
          case 7:
            node = new DefaultMutableTreeNode(arithmeticComboBox.getSelectedItem().toString().toUpperCase() + " " + inequalityComboBox.getSelectedItem() + " "
                                              + arithmeticFormattedTextField.getText(), false);
            break;
          default:
            JOptionPane.showMessageDialog(FilterDialog.this, "No option was selected.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (edit != null) {
          edit.setUserObject(node.getUserObject());
          edit = null;
        } else if (mainTree.getSelectionCount() == 0) {
          activeNode.add(node);
        } else {
          for (TreePath path : mainTree.getSelectionPaths()) {
            if (!((DefaultMutableTreeNode)path.getLastPathComponent()).getAllowsChildren()) {
              path = path.getParentPath();
            }
            ((DefaultMutableTreeNode)path.getLastPathComponent()).add((DefaultMutableTreeNode)node.clone());
          }
        }
        updateTree();
      }
    });

    GroupLayout jPanel1Layout = new GroupLayout(addPanel);
    addPanel.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(notCheckBox)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(typeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(cardPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(addButton)
        .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(typeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addComponent(notCheckBox))
      .addComponent(cardPanel, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
      .addComponent(addButton)
    );

    DefaultMutableTreeNode treeNode1 = new DefaultMutableTreeNode("root");
    activeNode = new DefaultMutableTreeNode("ACTIVE RULES");
    treeNode1.add(activeNode);
    inactiveNode = new DefaultMutableTreeNode("INACTIVE");
    treeNode1.add(inactiveNode);
    mainTree.setCellRenderer(new DefaultTreeCellRenderer(){
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value == edit) {
          c.setForeground(Color.RED);
        }
        return c;
      }
    });
    mainTree.setModel(new DefaultTreeModel(treeNode1));
    mainTree.setRootVisible(false);
    mainTree.setShowsRootHandles(true);
    mainTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (e.isControlDown()) {
          if (e.getKeyCode() == KeyEvent.VK_S) {
            switchSelected();
          } else if (e.getKeyCode() == KeyEvent.VK_X) {
            copySelected();
            removeSelected();
          } else if (e.getKeyCode() == KeyEvent.VK_C) {
            copySelected();
          } else if (e.getKeyCode() == KeyEvent.VK_V) {
            pasteSelected();
          }
        } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
          removeSelected();
        } else if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU && mainTree.getSelectionCount() > 0) {
          int[] rows = mainTree.getSelectionRows();
          Arrays.sort(rows);
          mainTree.scrollRowToVisible(rows[rows.length-1]);
          mainTree.scrollRowToVisible(rows[0]);
          Rectangle rect = null;
          int max = 0;
          for (int i = rows.length-1; i >= 0; i--) {
            rect = mainTree.getRowBounds(rows[i]);
            max = Math.max(max, rect.x+rect.width);
          }
          showPopup(e.getComponent(), max, rect.y+rect.height/2);
        }
      }
    });
    mainTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        mouse(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        mouse(e);
      }

      private void mouse(MouseEvent e) {
        if (e.isPopupTrigger()) {
          if (mainTree.getSelectionCount() == 0) {
            TreePath path = mainTree.getPathForLocation(e.getX(), e.getY());
            if (path == null) {
              return;
            }
            mainTree.setSelectionPath(path);
          }
          showPopup(e.getComponent(), e.getX(), e.getY());
        }
      }
    });
    mainScrollPane.setViewportView(mainTree);
    updateTree();

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        doClose(RET_CANCEL);
      }
    });

    andButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(AND, true);
        if (mainTree.getSelectionCount() == 0) {
          activeNode.add(node);
        } else {
          for (TreePath path : mainTree.getSelectionPaths()) {
            if (!((DefaultMutableTreeNode)path.getLastPathComponent()).getAllowsChildren()) {
              path = path.getParentPath();
            }
            ((DefaultMutableTreeNode)path.getLastPathComponent()).add((DefaultMutableTreeNode)node.clone());
          }
        }
        updateTree();
      }
    });

    orButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(OR, true);
        if (mainTree.getSelectionCount() == 0) {
          activeNode.add(node);
        } else {
          for (TreePath path : mainTree.getSelectionPaths()) {
            if (!((DefaultMutableTreeNode)path.getLastPathComponent()).getAllowsChildren()) {
              path = path.getParentPath();
            }
            ((DefaultMutableTreeNode)path.getLastPathComponent()).add((DefaultMutableTreeNode)node.clone());
          }
        }
        updateTree();
      }
    });
    
    helpButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          Desktop.getDesktop().open(new java.io.File("Help", "Filters.html"));
        } catch (Exception ex) {
          System.out.println(ex.getMessage());
          ex.printStackTrace();
        }
      }
    });

    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        doClose(RET_OK);
      }
    });

    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        doClose(RET_CANCEL);
      }
    });

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(andButton)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(orButton)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(helpButton)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(cancelButton)
        .addContainerGap())
      .addComponent(addPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
      .addComponent(mainScrollPane)
    );

    layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {andButton, orButton});
    layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {cancelButton, okButton});

    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(addPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(mainScrollPane)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(cancelButton)
          .addComponent(okButton)
          .addComponent(helpButton)
          .addComponent(orButton)
          .addComponent(andButton))
        .addContainerGap())
    );

    getRootPane().setDefaultButton(okButton);

    pack();

    // Close the dialog when Esc is pressed
    String cancelName = "cancel";
    InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
    ActionMap actionMap = getRootPane().getActionMap();
    actionMap.put(cancelName, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (menu == null || !menu.isVisible()) {
          doClose(RET_CANCEL);
        } else {
          menu.setVisible(false);
        }
      }
    });

    pack();
    setMinimumSize(new Dimension(getMinimumSize().width, Math.max(getMinimumSize().height, 300)));
    
    update();
  }

  public boolean showFilterDialog(boolean filter) {
    this.filter = filter;
    check();
    setVisible(true);
    if (returnStatus == RET_OK) {
      defaults.setProperty(ID + "\tactive", saveTree(activeNode, new Integer[]{ 0 }));
      defaults.setProperty(ID + "\tinactive", saveTree(inactiveNode, new Integer[]{ -1 }));
      return true;
    }
    return false;
  }

  public void update() {
    Vector vector = new Vector();
    vector.add(null);
    Properties file = Defaults.loadDefaults(ModificationAbstract.FILE);
    for (String key : new TreeSet<String>(file.stringPropertyNames())) {
      vector.add(key);
    }
    file = Defaults.loadDefaults(BondAbstract.FILE);
    for (String key : new TreeSet<String>(file.stringPropertyNames())) {
      BigDecimal[] mods = new BondAbstract(key, file.getProperty(key)).getMods();
      for (BigDecimal mod : mods) {
        if (mod != null) {
          vector.add(key);
          break;
        }
      }
    }
    modificationsComboBox.setModel(new DefaultComboBoxModel(vector));
    vector = new Vector();
    vector.add(null);
    for (String key : new TreeSet<String>(file.stringPropertyNames())) {
      vector.add(key);
    }
    bondsComboBox.setModel(new DefaultComboBoxModel(vector));
    
    returnStatus = RET_CANCEL;
    check();
  }

  private void check() {
    if (returnStatus == RET_CANCEL) {
      activeNode.removeAllChildren();
      inactiveNode.removeAllChildren();
      copy = new DefaultMutableTreeNode[0];
      if (defaults.containsKey(ID + "\tactive")) {
        loadChilds(new StringBuilder(defaults.getProperty(ID + "\tactive").toUpperCase()), new TreePath(activeNode.getPath()));
      }
      if (defaults.containsKey(ID + "\tinactive")) {
        loadChilds(new StringBuilder(defaults.getProperty(ID + "\tinactive").toUpperCase()), new TreePath(inactiveNode.getPath()));
      }
      updateTree();
      modificationsComboBox.setSelectedIndex(0);
      bondsComboBox.setSelectedIndex(0);
      matchesComboBox.setSelectedIndex(0);
      arithmeticComboBox.setSelectedIndex(0);
      inequalityComboBox.setSelectedIndex(0);
      arithmeticFormattedTextField.setValue(0);
      typeComboBox.setSelectedIndex(0);
      matchesTextField.setText("");
      mainTree.setSelectionPaths(null);
    }
  }

  private void loadChilds(StringBuilder input, TreePath path) {
    while (input.length() > 0) {
      switch (input.charAt(0)) {
        case ')':
          input.deleteCharAt(0);
          return;
        case ';':
        case '(':
          input.deleteCharAt(0);
      }
      loadTree(input, path);
    }
  }

  private void loadTree(StringBuilder input, TreePath path) {
    while (input.length() > 0 && Character.isWhitespace(input.charAt(0))) {
      input.deleteCharAt(0);
    }
    int i = input.length();
    for (String separator : new String[]{ "(", ";", ")" }) {
      int j = input.indexOf(separator);
      if (j >= 0 && j < i) {
        i = j;
      }
    }
    int j = i;
    while (j > 0 && Character.isWhitespace(input.charAt(j-1))) {
      j--;
    }
    String curr = input.substring(0, j);
    input.delete(0, i);
    mainTree.setSelectionPath(path);

    if (curr.equals(AND) || curr.equals(OR)) {
      DefaultMutableTreeNode node;
      if (curr.equals(AND)) {
        node = new DefaultMutableTreeNode(AND, true);
      } else {
        node = new DefaultMutableTreeNode(OR, true);
      }
      ((DefaultMutableTreeNode)path.getLastPathComponent()).add(node);
      if (input.charAt(0) != '(') {
        return;
      }
      loadChilds(input, path.pathByAddingChild(node));
      return;
    }

    typeComboBox.setSelectedIndex(curr.contains(' ' + TYPES[6] + " [") ? 6 : 7);
    for (String type : TYPES) {
      if (curr.startsWith(type.toUpperCase()) || curr.startsWith(NOT + ' ' + type.toUpperCase())) {
        notCheckBox.setSelected(curr.startsWith(NOT + ' '));
        typeComboBox.setSelectedItem(type);
        break;
      }
    }
    switch (typeComboBox.getSelectedIndex()) {
      case 1:
        modificationsComboBox.setSelectedIndex(0);
        if (curr.contains("[")) {
          try {
            String index = ID + '\t' + curr.substring(curr.indexOf('[')+1, curr.lastIndexOf(']'));
            if (defaults.containsKey(index)) {
              modificationsComboBox.setSelectedItem(defaults.getProperty(index));
            }
          } catch (Exception e) { }
        }
        break;
      case 2:
      case 3:
      case 4:
        bondsComboBox.setSelectedIndex(0);
        if (curr.contains("[")) {
          try {
            String index = ID + '\t' + curr.substring(curr.indexOf('[')+1, curr.lastIndexOf(']'));
            if (defaults.containsKey(index)) {
              bondsComboBox.setSelectedItem(defaults.getProperty(index));
            }
          } catch (Exception e) { }
        }
        break;
      case 6:
        try {
          notCheckBox.setSelected(curr.contains(' ' + NOT + ' ' + TYPES[6] + " ["));
          matchesComboBox.setSelectedIndex(-1);
          for (String match : MATCHING) {
            if (curr.contains(match)) {
              matchesComboBox.setSelectedItem(match);
              break;
            }
          }
          if (matchesComboBox.getSelectedIndex() == -1) {
            return;
          }
          String index = ID + '\t' + curr.substring(curr.indexOf('[')+1, curr.lastIndexOf(']'));
          if (defaults.containsKey(index)) {
            Pattern.compile(defaults.getProperty(index));
            matchesTextField.setText(defaults.getProperty(index));
          }
        } catch (Exception e) { }
        break;
      case 7:
        try {
          arithmeticComboBox.setSelectedIndex(-1);
          for (String arit : ARITHMETICS) {
            if (curr.startsWith(arit)) {
              arithmeticComboBox.setSelectedItem(arit);
              break;
            }
          }
          if (arithmeticComboBox.getSelectedIndex() == -1) {
            return;
          }
          inequalityComboBox.setSelectedIndex(-1);
          for (String comp : COMPARATORS) {
            if (curr.contains(comp)) {
              inequalityComboBox.setSelectedItem(comp);
              break;
            }
          }
          if (inequalityComboBox.getSelectedIndex() == -1) {
            return;
          }
          arithmeticFormattedTextField.setText(curr.substring(curr.lastIndexOf(' ')+1));
          arithmeticFormattedTextField.commitEdit();
          arithmeticFormattedTextField.setValue(arithmeticFormattedTextField.getValue());
        } catch (Exception e) { }
        break;
    }
    addButton.doClick();
  }

  private String saveTree(DefaultMutableTreeNode node, Integer[] i) {
    StringBuilder ret = new StringBuilder();
    for (int j = 0; j < node.getChildCount(); j++) {
      if (j != 0) {
        ret.append("; ");
      }
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(j);
      if (child.getAllowsChildren()) {
        ret.append(child.getUserObject().equals(AND) ? "AND(" : "OR(");
        ret.append(saveTree(child, i));
        ret.append(")");
      } else {
        String string = child.getUserObject().toString();
        if (string.contains(": ")) {
          String[] parts = string.split(": ", 2);
          defaults.setProperty(ID + '\t' + i[0], parts[1]);
          ret.append(parts[0]).append(" [").append(i[0] < 0 ? i[0]-- : i[0]++).append(']');
        } else if (string.startsWith(TYPES[3].toUpperCase() + " ") || string.startsWith(TYPES[4].toUpperCase() + " ")) {
          String[] parts = string.split(" ", 2);
          defaults.setProperty(ID + '\t' + i[0], parts[1]);
          ret.append(parts[0]).append(" [").append(i[0] < 0 ? i[0]-- : i[0]++).append(']');
        } else if (string.contains(" BY ")) {
          String[] parts = string.split(" BY ", 2);
          defaults.setProperty(ID + '\t' + i[0], parts[1]);
          ret.append(parts[0]).append(" BY [").append(i[0] < 0 ? i[0]-- : i[0]++).append(']');
        } else {
          ret.append(string);
        }
      }
    }
    return ret.toString();
  }

  public RowFilter getRowFilter() {
    if (activeNode.getChildCount() == 0) {
      return null;
    }
    return getRowFilter(activeNode);
  }

  private RowFilter getRowFilter(DefaultMutableTreeNode node) {
    if (node.getAllowsChildren()) {
      if (node.getChildCount() == 1) {
        return getRowFilter((DefaultMutableTreeNode)node.getChildAt(0));
      }
      ArrayList<RowFilter<Object, Object>> rows = new ArrayList(node.getChildCount());
      for (int i = 0; i < node.getChildCount(); i++) {
        rows.add(getRowFilter((DefaultMutableTreeNode)node.getChildAt(i)));
      }
      if (node.getUserObject().equals(AND)) { // AND
        return RowFilter.andFilter(rows);
      } // else OR
      return RowFilter.orFilter(rows);
    }

    if (node.getUserObject().equals(TRUE)) {
      return RowFilter.andFilter(new ArrayList(0));
    }
    if (node.getUserObject().equals(FALSE)) {
      return RowFilter.orFilter(new ArrayList(0));
    }

    if (node.getUserObject().equals(TYPES[0].toUpperCase())) { // ASSIGNED
      return new NullFilter(true, 1);
    }
    if (node.getUserObject().equals(NOT + " " + TYPES[0].toUpperCase())) { // NOT ASSIGNED
      return new NullFilter(false, 1);
    }

    for (int i = 1; i < 3; i++) {
      if (node.getUserObject().equals(TYPES[i].toUpperCase())) { // MODIFIED | X-LINKED
        return new ArrayRegexFilter("[^| ]", false, i+4);
      }
      if (node.getUserObject().toString().startsWith(TYPES[i].toUpperCase())) { // MODIFIED | X-LINKED BY {}
        return new ArrayRegexFilter("(^|\\| |, |; )" + Pattern.quote(node.getUserObject().toString().substring(TYPES[i].length() + 4)) + " \\(", false, i+4);
      }
      if (node.getUserObject().equals(NOT + " " + TYPES[i].toUpperCase())) { // NOT MODIFIED | X-LINKED
        return new ArrayRegexFilter("(^|\\| )( ?\\||$)", false, i+4);
      }
      if (node.getUserObject().toString().startsWith(NOT + " " + TYPES[i].toUpperCase())) { // NOT MODIFIED | X-LINKED BY {}
        return new ArrayRegexFilter("(^|\\| )(?!([^(]*\\([^)]*\\)[,;] )*" +
                                    Pattern.quote(node.getUserObject().toString().substring(TYPES[i].length() + 8)) + " \\()", false, i+4);
      }
    }

    if (node.getUserObject().equals(TYPES[3].toUpperCase())) { // INTERPEPTIDE
      return new ArrayRegexFilter("-", false, 4);
    }
    if (node.getUserObject().toString().startsWith(TYPES[3].toUpperCase())) { // INTERPEPTIDE {}
      return new ArrayRegexFilter("(^|\\| |, |; )" + Pattern.quote(node.getUserObject().toString().substring(TYPES[3].length() + 1)) + " \\(([A-Z]+\\.)[^;]+; (?!\\2)", false, 6);
    }
    if (node.getUserObject().equals(TYPES[4].toUpperCase())) { // INTRAPEPTIDE
      return new ArrayRegexFilter("^[^-]*$", false, 4);
    }
    if (node.getUserObject().toString().startsWith(TYPES[4].toUpperCase())) { // INTRAPEPTIDE {}
      return new ArrayRegexFilter("(^|\\| |, |; )" + Pattern.quote(node.getUserObject().toString().substring(TYPES[3].length() + 1)) + " \\(([A-Z]+\\.|)[^;.]+; \\2", false, 6);
    }

    if (node.getUserObject().equals(TYPES[5].toUpperCase())) { // AMBIGUOUS
      return new UniqueFilter(true, 3, 4);
    }
    if (node.getUserObject().equals(NOT + " " + TYPES[5].toUpperCase())) { // UNIQUE
      return new UniqueFilter(false, 3, 4);
    }

    // TODO: BORDER

    String string = node.getUserObject().toString();
     if (string.contains(": ")) { // MATCHES
       int i = 0;
       while (!string.startsWith(MATCHING[i])) {
         i++;
       }
       if (i < 4) {
         i += 3;
       } else {
         i += 4;
       }
       String[] split = string.split(": ", 2);
       return new ArrayRegexFilter(split[1], split[0].contains(' ' + NOT + ' '), i);
     }

    int i = 0; // ARITHMETICS
    while (!string.startsWith(ARITHMETICS[i])) {
      i++;
    }
    string = string.substring(ARITHMETICS[i].length()+1);
    Number n;
    try {
      switch (i) {
        case 2:
          n = biochemie.Defaults.sPpmFormat.parse(string.substring(2)).doubleValue();
          break;
        case 3:
          i = 7;
          n = biochemie.Defaults.intensityFormat.parse(string.substring(2)).longValue();
          break;
        default:
          n = biochemie.Defaults.uMassShortFormat.parse(string.substring(2)).doubleValue();
          break;
      }
    } catch (ParseException ex) {
      throw new InputMismatchException("'" + string.substring(2) + "' cannot be parsed as number.");
    }
    return new ArrayNumberFilter(string.charAt(0), n, i);
  }

  public ArrayList<String> toStringRowFilter() {
    check();
    return toStringRowFilter(new ArrayList<String>(), activeNode, "");
  }

  private ArrayList<String> toStringRowFilter(ArrayList<String> list, DefaultMutableTreeNode node, String prefix) {
    for (int i = 0; i < node.getChildCount(); i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      list.add(prefix + child.getUserObject());
      if (child.getAllowsChildren()) {
        toStringRowFilter(list, child, prefix + "  ");
      }
    }
    return list;
  }

  public void fromStringRowFilter(ArrayList<String> set) {
    if (toStringRowFilter().equals(set)) {
      return;
    }

    if (activeNode.getChildCount() > 0 || inactiveNode.getChildCount() > 0) {
      boolean copy = activeNode.getChildCount() > 0;
    
      if (toStringRowFilter(new ArrayList<String>(), inactiveNode, "").equals(set)) { // Nastavované je totožné s neaktivními
        inactiveNode.removeAllChildren();
      }
      if (toStringRowFilter().equals(toStringRowFilter(new ArrayList<String>(), inactiveNode, ""))) { // Aktivní je totožné s neaktivním
        copy = false;
      }
      for (int i = 0; i < inactiveNode.getChildCount(); i++) {
        ArrayList<String> node = toStringRowFilter(new ArrayList<String>(), (DefaultMutableTreeNode)inactiveNode.getChildAt(i), "");
        // Aktivní je jedním z prvků mezi neaktivními
        if ((activeNode.getChildCount() == 1 && toStringRowFilter(new ArrayList<String>(), (DefaultMutableTreeNode)activeNode.getChildAt(0), "").equals(node)) ||
            (((DefaultMutableTreeNode)inactiveNode.getChildAt(i)).getUserObject().equals(OR) && toStringRowFilter().equals(node))) {
          copy = false;
        }
        // Nastavované je jedním z prvků mezi neaktivními
        if (node.equals((set))) {
          inactiveNode.remove(i);
        }
      }

      if (copy) {
        DefaultMutableTreeNode node;
        if (activeNode.getChildCount() > 1) {
          node = new DefaultMutableTreeNode(OR, true);
          inactiveNode.add(node);
        } else {
          node = inactiveNode;
        }
        mainTree.setSelectionPath(new TreePath(activeNode.getPath()));
        copySelected();
        removeSelected();
        mainTree.setSelectionPath(new TreePath(node.getPath()));
        pasteSelected();
      } else {
        activeNode.removeAllChildren();
      }
    }

    Stack<Integer> prefixes = new Stack();
    prefixes.push(0);
    DefaultMutableTreeNode parent = activeNode;
    for (String row : set) {
      try {
        while (!row.matches("^\\s{" + prefixes.peek() + "}.*")) {
          parent = (DefaultMutableTreeNode)parent.getParent();
          prefixes.pop();
        }
        mainTree.setSelectionPath(new TreePath(parent));

        String[] cond = row.replaceFirst("^\\s+", "").split(":\\s?", 2);
        String upper = cond[0].toUpperCase();

        if (upper.startsWith(AND) || upper.startsWith(OR)) {
          parent.add(parent = new DefaultMutableTreeNode(upper.startsWith(AND) ? AND : OR, true));
          prefixes.push(row.split("\\S", 2)[0].length() + 1);
        } else {
          typeComboBox.setSelectedIndex(cond.length == 1 ? 7 : 6);
          for (String type : TYPES) {
            if (upper.startsWith(type.toUpperCase())) {
              notCheckBox.setSelected(false);
              typeComboBox.setSelectedItem(type);
              break;
            }
            if (upper.startsWith(NOT + " " + type.toUpperCase())) {
              notCheckBox.setSelected(true);
              typeComboBox.setSelectedItem(type);
              break;
            }
          }
          switch (typeComboBox.getSelectedIndex()) {
            case 1:
              modificationsComboBox.setSelectedIndex(0);
              try {
                int index = upper.indexOf(" BY ");
                if (index >= 0) {
                  modificationsComboBox.setSelectedItem(cond[0].substring(index + 4));
                }
              } catch (Exception e) { }
              break;
            case 2:
              bondsComboBox.setSelectedIndex(0);
              try {
                int index = upper.indexOf(" BY ");
                if (index >= 0) {
                  bondsComboBox.setSelectedItem(cond[0].substring(index + 4));
                }
              } catch (Exception e) { }
              break;
            case 3:
            case 4:
              bondsComboBox.setSelectedIndex(0);
              try {
                if (cond[0].contains(" ")) {
                  bondsComboBox.setSelectedItem(cond[0].substring(cond[0].indexOf(" ") + 1));
                }
              } catch (Exception e) { }
              break;
            case 6:
              matchesComboBox.setSelectedIndex(-1);
              for (String col : MATCHING) {
                if (upper.startsWith(col.toUpperCase())) {
                  matchesComboBox.setSelectedItem(col);
                  break;
                }
              }
              if (matchesComboBox.getSelectedIndex() == -1) {
                continue;
              }
              notCheckBox.setSelected(upper.endsWith(" " + NOT + " " + TYPES[6]));
              matchesTextField.setText(cond[1]);
              break;
            case 7:
              arithmeticComboBox.setSelectedIndex(-1);
              for (String col : ARITHMETICS) {
                if (upper.startsWith(col.toUpperCase())) {
                  arithmeticComboBox.setSelectedItem(col);
                  break;
                }
              }
              if (arithmeticComboBox.getSelectedIndex() == -1) {
                continue;
              }
              inequalityComboBox.setSelectedIndex(-1);
              for (String ine : COMPARATORS) {
                if (upper.contains(ine.toUpperCase())) {
                  inequalityComboBox.setSelectedItem(ine);
                  break;
                }
              }
              if (inequalityComboBox.getSelectedIndex() == -1) {
                continue;
              }
              arithmeticFormattedTextField.setText(upper.split((String)inequalityComboBox.getSelectedItem(), 2)[1].trim().split("\\s", 2)[0]);
              arithmeticFormattedTextField.commitEdit();
              arithmeticFormattedTextField.setValue(arithmeticFormattedTextField.getValue());
              break;
          }

          addButton.doClick();
        }
      } catch (Exception e) {
        System.out.print("");
      }
    }
    notCheckBox.setSelected(false);
    typeComboBox.setSelectedIndex(0);
    updateTree();
    defaults.setProperty(ID + "\tactive", saveTree(activeNode, new Integer[]{ 0 }));
    defaults.setProperty(ID + "\tinactive", saveTree(inactiveNode, new Integer[]{ -1 }));
  }

  private void updateTree() {
    ArrayDeque<DefaultMutableTreeNode> list = new ArrayDeque();
    list.add(activeNode);
    list.add(inactiveNode);
    DefaultMutableTreeNode curr;
    while ((curr = list.poll()) != null) {
      if (curr.getAllowsChildren() && curr.getChildCount() == 0 && curr.getParent() != mainTree.getModel().getRoot()) {
        if (((DefaultMutableTreeNode)curr.getParent()).getUserObject().equals(AND)) {
          curr.add(new DefaultMutableTreeNode(TRUE, false));
        } else {
          curr.add(new DefaultMutableTreeNode(FALSE, false));
        }
      }
      for (int i = curr.getChildCount()-1; i >= 0; i--) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode)curr.getChildAt(i);
        if (child.getAllowsChildren()) {
          list.add(child);
        } else if ((child.getUserObject().equals(TRUE) || child.getUserObject().equals(FALSE)) && curr.getChildCount() > 1) {
          curr.remove(i);
        }
      }
    }
    expandNode(new TreePath(activeNode.getPath()), activeNode);
    expandNode(new TreePath(inactiveNode.getPath()), inactiveNode);
    mainTree.updateUI();
  }

  private void expandNode(TreePath path, TreeNode node) {
    mainTree.expandPath(path);
    for (int i = 0; i < node.getChildCount(); i++) {
      expandNode(path.pathByAddingChild(node.getChildAt(i)), node.getChildAt(i));
    }
  }

  private void showPopup(Component c, int x, int y) {
    menu = new JPopupMenu("Filtering");
    JMenuItem jmi;
    jmi = new JMenuItem("Activate/ Deactivate", 'A');
    jmi.setAccelerator(KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK));
    jmi.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        switchSelected();
      }
    });
    menu.add(jmi);
    jmi = new JMenuItem("Edit", 'E');
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)mainTree.getSelectionPath().getLastPathComponent();
    jmi.setEnabled(mainTree.getSelectionCount() == 1 && !(dmtn.getAllowsChildren() || TRUE.equals(dmtn.getUserObject()) || FALSE.equals(dmtn.getUserObject())));
    jmi.setAccelerator(KeyStroke.getKeyStroke('E', InputEvent.CTRL_DOWN_MASK));
    jmi.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          edit = (DefaultMutableTreeNode)mainTree.getSelectionPath().getLastPathComponent();
          String[] value = edit.getUserObject().toString().split(": ", 2);
          if (value.length == 1) {
            typeComboBox.setSelectedIndex(7);
            for (int i = 0; i < TYPES.length-2; i++) {
              if (value[0].startsWith(TYPES[i].toUpperCase())) {
                notCheckBox.setSelected(false);
                typeComboBox.setSelectedIndex(i);
                break;
              } else if (value[0].startsWith(NOT + " " + TYPES[i].toUpperCase())) {
                notCheckBox.setSelected(true);
                value[0] = value[0].substring(NOT.length() + 1);
                typeComboBox.setSelectedIndex(i);
                break;
              }
            }
            switch (typeComboBox.getSelectedIndex()) {
              case 1:
                modificationsComboBox.setSelectedIndex(0);
                try {
                  String[] parts = value[0].split(" ", 3);
                  if (parts.length >= 3) {
                    modificationsComboBox.setSelectedItem(parts[2]);
                  }
                } catch (Exception ex) { }
                break;
              case 2:
                bondsComboBox.setSelectedIndex(0);
                try {
                  String[] parts = value[0].split(" ", 3);
                  if (parts.length >= 3) {
                    bondsComboBox.setSelectedItem(parts[2]);
                  }
                } catch (Exception ex) { }
                break;
              case 3:
              case 4:
                bondsComboBox.setSelectedIndex(0);
                try {
                  String[] parts = value[0].split(" ", 2);
                  if (parts.length >= 2) {
                    bondsComboBox.setSelectedItem(parts[1]);
                  }
                } catch (Exception ex) { }
                break;
              case 7:
                for (int i = 0; i < ARITHMETICS.length; i++) {
                  if (value[0].startsWith(ARITHMETICS[i].toUpperCase())) {
                    arithmeticComboBox.setSelectedIndex(i);
                    break;
                  }
                }
                for (int i = 0; i < COMPARATORS.length; i++) {
                  if (value[0].contains(COMPARATORS[i])) {
                    inequalityComboBox.setSelectedIndex(i);
                    break;
                  }
                }
                arithmeticFormattedTextField.setText(value[0].substring(value[0].lastIndexOf(' ')+1));
                arithmeticFormattedTextField.commitEdit();
                arithmeticFormattedTextField.setValue(arithmeticFormattedTextField.getValue());
                break;
            }
          } else {
            notCheckBox.setSelected(value[0].contains(NOT));
            typeComboBox.setSelectedIndex(6);
            matchesTextField.setText(value[1]);
            for (int i = 0; i < MATCHING.length; i++) {
              if (value[0].startsWith(MATCHING[i].toUpperCase())) {
                matchesComboBox.setSelectedIndex(i);
                break;
              }
            }
          }
          mainTree.repaint();
          typeComboBox.requestFocus();
        } catch (Exception ex) { }
      }
    });
    menu.add(jmi);
    jmi = new JMenuItem("Delete", 'D');
    jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
    jmi.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        removeSelected();
      }
    });
    menu.add(jmi);
    menu.addSeparator();
    jmi = new JMenuItem("Cut", 'U');
    jmi.setAccelerator(KeyStroke.getKeyStroke('X', InputEvent.CTRL_DOWN_MASK));
    jmi.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        copySelected();
        removeSelected();
      }
    });
    menu.add(jmi);
    jmi = new JMenuItem("Copy", 'O');
    jmi.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.CTRL_DOWN_MASK));
    jmi.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        copySelected();
      }
    });
    menu.add(jmi);
    jmi = new JMenuItem("Paste", 'P');
    if (copy.length == 0) {
      jmi.setEnabled(false);
    }
    jmi.setAccelerator(KeyStroke.getKeyStroke('V', InputEvent.CTRL_DOWN_MASK));
    jmi.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        pasteSelected();
      }
    });
    menu.add(jmi);
    menu.show(c, x, y);
  }

  private void copySelected() {
    if (mainTree.getSelectionPaths() == null) {
      return;
    }
    LinkedHashSet<DefaultMutableTreeNode> nodes = new LinkedHashSet();
    for (TreePath path : mainTree.getSelectionPaths()) {
      if (path.getLastPathComponent() == activeNode) {
        for (int i = 0; i < activeNode.getChildCount(); i++) {
          nodes.add((DefaultMutableTreeNode)activeNode.getChildAt(i));
        }
      } else if (path.getLastPathComponent() == inactiveNode) {
        for (int i = 0; i < inactiveNode.getChildCount(); i++) {
          nodes.add((DefaultMutableTreeNode)inactiveNode.getChildAt(i));
        }
      } else {
        nodes.add((DefaultMutableTreeNode)path.getLastPathComponent());
      }
    }
    copy = new DefaultMutableTreeNode[nodes.size()];
    int i = 0;
    for (DefaultMutableTreeNode node : nodes) {
      copy[i++] = clone(node);
    }
  }

  private void removeSelected() {
    if (mainTree.getSelectionPaths() == null) {
      return;
    }
    for (TreePath path : mainTree.getSelectionPaths()) {
      if (path.getLastPathComponent() == activeNode) {
        activeNode.removeAllChildren();
      } else if (path.getLastPathComponent() == inactiveNode) {
        inactiveNode.removeAllChildren();
      } else {
        ((DefaultMutableTreeNode)path.getParentPath().getLastPathComponent()).remove((DefaultMutableTreeNode)path.getLastPathComponent());
      }
      mainTree.removeSelectionPath(path);
    }
    updateTree();
  }

  private void pasteSelected() {
    if (mainTree.getSelectionPaths() == null) {
      return;
    }
    HashSet<DefaultMutableTreeNode> nodes = new HashSet();
    for (TreePath path : mainTree.getSelectionPaths()) {
      if (((DefaultMutableTreeNode)path.getLastPathComponent()).getAllowsChildren()) {
        nodes.add((DefaultMutableTreeNode)path.getLastPathComponent());
      } else {
        nodes.add((DefaultMutableTreeNode)path.getParentPath().getLastPathComponent());
      }
    }
    for (DefaultMutableTreeNode parent : nodes) {
      for (DefaultMutableTreeNode child : copy) {
        parent.add(clone(child));
      }
    }
    updateTree();
  }

  private void switchSelected() {
    if (mainTree.getSelectionPaths() == null) {
      return;
    }
    DefaultMutableTreeNode[] tmp = copy;
    LinkedHashSet<TreePath> active = new LinkedHashSet();
    LinkedHashSet<TreePath> inactive = new LinkedHashSet();
    for (TreePath path : mainTree.getSelectionPaths()) {
      if (path.getPathCount() > 2) {
        (path.getPathComponent(1) == activeNode ? active : inactive).add(path);
      } else {
        DefaultMutableTreeNode cn;
        LinkedHashSet<TreePath> cl;
        if (path.getLastPathComponent() == activeNode) {
          cn = activeNode;
          cl = active;
        } else {
          cn = inactiveNode;
          cl = inactive;
        }
        for (int i = 0; i < cn.getChildCount(); i++) {
          cl.add(path.pathByAddingChild(cn.getChildAt(i)));
        }
      }
    }
    if (!inactive.isEmpty()) {
      mainTree.setSelectionPaths(inactive.toArray(new TreePath[inactive.size()]));
      copySelected();
      removeSelected();
      mainTree.setSelectionPath(new TreePath(activeNode.getPath()));
      pasteSelected();
    }
    if (!active.isEmpty()) {
      mainTree.setSelectionPaths(active.toArray(new TreePath[active.size()]));
      copySelected();
      removeSelected();
      mainTree.setSelectionPath(new TreePath(inactiveNode.getPath()));
      pasteSelected();
    }
    mainTree.clearSelection();
    copy = tmp;
  }

  private DefaultMutableTreeNode clone(DefaultMutableTreeNode node) {
    DefaultMutableTreeNode clone = (DefaultMutableTreeNode)node.clone();
    for (int i = 0; i < node.getChildCount(); i++) {
      clone.add(clone((DefaultMutableTreeNode)node.getChildAt(i)));
    }
    return clone;
  }

  private void doClose(int retStatus) {
    returnStatus = retStatus;
    setVisible(false);
    dispose();
  }

  private static class NullFilter<M> extends RowFilter<M, Object> {
    private int column;
    private boolean not;

    NullFilter(boolean not, int column) {
      if (column < 0) { throw new IllegalArgumentException("Index must be non-negative."); }
      this.column = column;
      this.not = not;
    }

    public boolean include(Entry<? extends M, ? extends Object> value) {
      if (column >= value.getValueCount()) {
        return false;
      }

      Object val = value.getValue(column);
      if (val == null) {
        return !not;
      }
      if (val instanceof Object[]) {
        for (Object v : (Object[])val) {
          if (v == null ^ not) {
            return true;
          }
        }
      }
      return false;
    }
  }

  private static class UniqueFilter<M> extends RowFilter<M, Object> {
    private int[] columns;
    private boolean not; // TRUE => Alespoň jeden sloupec obsahuje více hodnot; FALSE => Všechny sloupce obsahují jednu hodnotu

    UniqueFilter(boolean not, int... columns) {
      for (int column : columns) {
        if (column < 0) { throw new IllegalArgumentException("Index must be non-negative."); }
      }
      this.columns = columns;
      this.not = not;
    }

    public boolean include(Entry<? extends M, ? extends Object> value) {
      for (int column : columns) {
        if (column >= value.getValueCount()) {
          return false;
        }

        Object val = value.getValue(column);
        if (val == null || !(val instanceof Object[])) {
          return false;
        }

        Object[] v = (Object[])val;
        switch(v.length) {
          case 0:
            return false;
          case 1:
            if (v[0] == null) {
              return false; // NOT ASSIGNED
            }
            break;
          default:
            return not;
        }
      }

      return !not;
    }
  }

  private static class ArrayRegexFilter<M> extends RowFilter<M, String[]> {
    private int column;
    private Matcher matcher;
    private boolean not; // TRUE => Žádný nesplňuje; FALSE => Alespoň jeden splňuje

    ArrayRegexFilter(String regex, boolean not, int column) {
      if (column < 0) { throw new IllegalArgumentException("Index must be non-negative."); }
      if (regex == null) { throw new IllegalArgumentException("Pattern must be non-null"); }
      this.column = column;
      this.matcher = Pattern.compile(regex).matcher("");
      this.not = not;
    }

    public boolean include(Entry<? extends M, ? extends String[]> value) {
      if (column >= value.getValueCount()) {
        return false;
      }

      Object val = value.getValue(column);
      if (val != null && val instanceof String[]) {
        boolean ret = false;
        for (String v : (String[])val) {
          if (v != null) {
            matcher.reset(v);
            if (matcher.find()) {
              return !not;
            }
            ret = not;
          }
        }
        return ret;
      } else {
        return false;
      }
    }
  }

  private static class ArrayNumberFilter<M, N extends Number & Comparable<N>> extends RowFilter<M, N[]> {
    private int column;
    private N number;
    private char type;

    ArrayNumberFilter(char type, N number, int column) {
      if (column < 0) { throw new IllegalArgumentException("Index must be non-negative."); }
      if (number == null) { throw new IllegalArgumentException("Number must be non-null."); }
      if (type != '<' && type != '≤' && type != '≥' && type != '>') { throw new IllegalArgumentException("Unknown comparator: " + type + "."); }
      this.column = column;
      this.number = number;
      this.type = type;
    }

    public boolean include(Entry<? extends M, ? extends N[]> value) {
      if (column >= value.getValueCount()) {
        return false;
      }

      Object val = value.getValue(column);
      if (val != null && (val instanceof Number[]) && (val instanceof Comparable[])) {
        for (Comparable v : (Comparable[])val) {
          if (v == null) {
            continue;
          }
          switch (type) {
            case '<':
              if (v.compareTo(number) < 0) {
                return true;
              }
              break;
            case '≤':
              if (v.compareTo(number) <= 0) {
                return true;
              }
              break;
            case '=':
              if (v.compareTo(number) == 0) {
                return true;
              }
              break;
            case '≥':
              if (v.compareTo(number) >= 0) {
                return true;
              }
              break;
            case '>':
              if (v.compareTo(number) > 0) {
                return true;
              }
              break;
          }
        }
      }

      return false;
    }
  }
}
