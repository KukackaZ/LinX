package biochemie.linx;

import biochemie.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

/**
 *
 * @author Janek
 */
public class BondsCard extends Card {
  private static String[] TARGETS = new String[] { "All proteins", "Within one molecule" };
  private static String[] TYPES;
  private static String[] POSITIONS = new String[] { "All" };
  private static String TEMPLATE = "OR Select template";
  private static String TEMPLATES_FILE = "bonds.template";
  private static String BONDS_SEPARATOR = "\n";
  private static String BOND_SEPARATOR = "\t";

  private ModificationsCard modificationsCard;
  private TreeMap<String, BondAbstract> bonds;
  private TreeMap<String, ArrayList<String[]>> templates;
  private LinkedHashMap<String, Protein> proteins;
  private JComboBox target1ComboBox;
  private JComboBox type1ComboBox;
  private JComboBox position1ComboBox;
  private JComboBox target2ComboBox;
  private JComboBox type2ComboBox;
  private JComboBox position2ComboBox;
  private JComboBox bondsComboBox;
  private JCheckBox specificCheckBox;
  private JComboBox templatesComboBox;
  private JTable inputTable;
  private TreeMap<String, BondAbstract> backup;
  private ArrayList<Object[]> edited;

  public BondsCard(String id, JButton[] movement, ModificationsCard modificationsCard) {
    super(id, movement);
    this.modificationsCard = modificationsCard;
    ChemicalElement.addChangeListener(new ChangeListener() {@Override
      public void stateChanged(ChangeEvent e) {
        reloadBonds();
      }
    });
    loadBonds();
    TYPES = Monomer.getNames().toArray(new String[0]);
    proteins = new LinkedHashMap<>(1);
    JPanel mainPanel = new JPanel();
    target1ComboBox = new JComboBox(new String[] { TARGETS[0], "Protein" });
    type1ComboBox = new JComboBox(TYPES);
    position1ComboBox = new JComboBox(POSITIONS);
    target2ComboBox = new JComboBox(new String[] { TARGETS[0], TARGETS[1], "Protein" });
    type2ComboBox = new JComboBox(TYPES);
    position2ComboBox = new JComboBox(POSITIONS);
    bondsComboBox = new JComboBox(bonds.keySet().toArray());
    specificCheckBox = new JCheckBox("Specific", true);
    templatesComboBox = new JComboBox();
    final JButton insertButton = new JButton("Insert");
    inputTable = new JTable(new javax.swing.table.DefaultTableModel(new Object [][] { { null, null, null, null, null, null, null } },
                              new String [] { "Target 1", "Types 1", "Positions 1", "Target 2", "Types 2", "Positions 2", "Cross-link" }) {
        public Class getColumnClass(int columnIndex) {
          return String.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
          return false;
        }
      }) {
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);

        if (edited.isEmpty()) {
          c.setForeground(null);
          return c;
        }

        for (Object[] line : edited) {
          boolean same = true;
          for (int i = 0; i < inputTable.getColumnCount(); i++) {
            if (!line[i].equals(inputTable.getValueAt(row, i))) {
              same = false;
              break;
            }
          }
          if (same) {
            c.setForeground(Color.RED);
            return c;
          }
        }

        c.setForeground(null);
        return c;
      }
    };
    JScrollPane inputScrollPane = new JScrollPane(inputTable);
    final JButton cleanButton = new JButton("Clean form");
    final JButton removeButton = new JButton("Remove selected");
    final JButton editButton = new JButton("Edit selected");

    setFocusTraversalPolicyProvider(true);

    setFocusTraversalPolicy(new FocusTraversalPolicy() {
      private Component[] order;

      public Component getComponentAfter(Container aContainer, Component aComponent) {
        int i = indexOf(aComponent) + 1;
        if (i <= 0 || i >= order.length) {
          return null;
        }
        Component next = order[i];
        if (next.isEnabled()) {
          return next;
        }
        return getComponentAfter(aContainer, next);
      }

      public Component getComponentBefore(Container aContainer, Component aComponent) {
        int i = indexOf(aComponent) - 1;
        if (i < 0) {
          return null;
        }
        Component next = order[i];
        if (next.isEnabled()) {
          return next;
        }
        return getComponentBefore(aContainer, next);
      }

      private int indexOf(Component component) {
        for (int i = 0; i < order.length; i++) {
          if (order[i].equals(component) ||
              (order[i] instanceof JComboBox &&
               ((JComboBox)order[i]).getEditor().getEditorComponent().equals(component))) {
            return i;
          }
        }
        return -1;
      }

      public Component getFirstComponent(Container aContainer) {
        Component first = order[0];
        if (first.isFocusable()) {
          return first;
        }
        return getComponentAfter(aContainer, first);
      }

      public Component getLastComponent(Container aContainer) {
        Component last = order[order.length-1];
        if (last.isFocusable()) {
          return last;
        }
        return getComponentBefore(aContainer, last);
      }

      public Component getDefaultComponent(Container aContainer) {
        return getFirstComponent(aContainer);
      }

      public FocusTraversalPolicy init(JButton[] movement) {
        order = new Component[12 + movement.length];
        order[0] = target1ComboBox;
        order[1] = type1ComboBox;
        order[2] = position1ComboBox;
        order[3] = target2ComboBox;
        order[4] = type2ComboBox;
        order[5] = position2ComboBox;
        order[6] = specificCheckBox;
        order[7] = bondsComboBox;
        order[8] = insertButton;
        order[9] = cleanButton;
        order[10] = removeButton;
        order[11] = editButton;
        for (int i = 0; i < movement.length; i++) {
          order[i+12] = movement[i];
        }
        return this;
      }
    }.init(movement));

    mainScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    mainScrollPane.setViewportView(mainPanel);

    proteins.put("Protein", new Protein("Protein", ""));

    target1ComboBox.addActionListener(new TargetListener(target1ComboBox, type1ComboBox));

    target2ComboBox.addActionListener(new TargetListener(target2ComboBox, type2ComboBox));

    type1ComboBox.setEditable(true);
    type1ComboBox.setMaximumRowCount(25);
    type1ComboBox.addActionListener(new TypeListener(target1ComboBox, type1ComboBox, position1ComboBox));
    ((JTextField)type1ComboBox.getEditor().getEditorComponent()).setInputVerifier(new InputVerifier() {
      public boolean verify(JComponent input) {
        return verifyType(type1ComboBox);
      }
    });
    ((JTextField)type1ComboBox.getEditor().getEditorComponent()).addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        ((JTextField)type1ComboBox.getEditor().getEditorComponent()).selectAll();
      }
    });

    type2ComboBox.setEditable(true);
    type2ComboBox.setMaximumRowCount(25);
    type2ComboBox.addActionListener(new TypeListener(target2ComboBox, type2ComboBox, position2ComboBox));
    ((JTextField)type2ComboBox.getEditor().getEditorComponent()).setInputVerifier(new InputVerifier() {
      public boolean verify(JComponent input) {
        return verifyType(type2ComboBox);
      }
    });
    ((JTextField)type2ComboBox.getEditor().getEditorComponent()).addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        ((JTextField)type2ComboBox.getEditor().getEditorComponent()).selectAll();
      }
    });

    position1ComboBox.setEditable(true);
    position1ComboBox.setMaximumRowCount(20);
    position1ComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        formatPosition(position1ComboBox);
      }
    });
    ((JTextField)position1ComboBox.getEditor().getEditorComponent()).setInputVerifier(new InputVerifier() {
      public boolean verify(JComponent input) {
        return verifyPosition(position1ComboBox);
      }
    });
    ((JTextField)position1ComboBox.getEditor().getEditorComponent()).addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        ((JTextField)position1ComboBox.getEditor().getEditorComponent()).selectAll();
      }
    });

    position2ComboBox.setEditable(true);
    position2ComboBox.setMaximumRowCount(20);
    position2ComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        formatPosition(position2ComboBox);
      }
    });
    ((JTextField)position2ComboBox.getEditor().getEditorComponent()).setInputVerifier(new InputVerifier() {
      public boolean verify(JComponent input) {
        return verifyPosition(position2ComboBox);
      }
    });
    ((JTextField)position2ComboBox.getEditor().getEditorComponent()).addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        ((JTextField)position2ComboBox.getEditor().getEditorComponent()).selectAll();
      }
    });

    bondsComboBox.setMaximumRowCount(20);

    cleanInput();

    specificCheckBox.setMnemonic('F');
    specificCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (specificCheckBox.isSelected()) {
          filterBonds();
        } else {
          bondsComboBox.setModel(new DefaultComboBoxModel<>(bonds.keySet().toArray()));
        }
      }
    });

    loadTemplates();
    templatesComboBox.setMaximumRowCount(20);

    insertButton.setMnemonic('S');
    insertButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        insert(true);
      }
    });

    TableRowSorter<javax.swing.table.TableModel> trs = new TableRowSorter<>(inputTable.getModel());
    trs.setComparator(0, new TargetComparator(target1ComboBox));
    trs.setComparator(1, new TypeComparator());
    trs.setComparator(2, new PositionComparator());
    trs.setComparator(3, new TargetComparator(target2ComboBox));
    trs.setComparator(4, new TypeComparator());
    trs.setComparator(5, new PositionComparator());
    trs.setSortsOnUpdates(true);
    inputTable.setRowSorter(trs);
    inputTable.getTableHeader().setReorderingAllowed(false);
    inputTable.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if ((inputTable.getRowCount() > 1 || inputTable.getValueAt(0, 0) != null) && inputTable.getSelectedRowCount() > 0) {
          if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU && inputTable.getSelectedRowCount() > 0) {
            inputTable.scrollRectToVisible(inputTable.getCellRect(inputTable.getSelectedRow(), 0, true));
            java.awt.Rectangle a = inputTable.getCellRect(inputTable.getSelectedRow(), 0, true);
            java.awt.Rectangle b = inputTable.getCellRect(inputTable.getSelectedRow(), 6, true);
            showPopupMenu((a.x + b.x + b.width)/2, a.y + a.height / 2);
          } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            deleteSelected();
          }
        }
      }
    });
    inputTable.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
          if (!inputTable.isRowSelected(inputTable.rowAtPoint(e.getPoint()))) {
            inputTable.setRowSelectionInterval(inputTable.rowAtPoint(e.getPoint()), inputTable.rowAtPoint(e.getPoint()));
          }
          if (inputTable.getRowCount() > 1 || inputTable.getValueAt(0, 0) != null) {
            showPopupMenu(e.getPoint().x, e.getPoint().y);
          }
        }
      }
    });

    GroupLayout layout = new GroupLayout(mainPanel);
    mainPanel.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addComponent(inputScrollPane)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(target1ComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(target2ComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(type1ComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(type2ComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(position1ComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(position2ComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(bondsComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(specificCheckBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, Short.MAX_VALUE)
            .addComponent(templatesComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(insertButton)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(target1ComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(type1ComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(position1ComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(bondsComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(specificCheckBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(templatesComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(insertButton))
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
          .addComponent(target2ComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(type2ComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(position2ComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(inputScrollPane, GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
        .addContainerGap())
    );

    cleanButton.setMnemonic('C');
    cleanButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        clean();
      }
    });

    removeButton.setMnemonic('R');
    removeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        deleteSelected();
      }
    });

    editButton.setMnemonic('E');
    editButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        editSelected();
      }
    });

    createLayout(new JButton[] { cleanButton, removeButton, editButton });

    backup = new TreeMap<>();
    edited = new ArrayList(0);
  }

  public void logDefaults() {
    Defaults.putTableDefaults(inputTable, defaults);
  }

  public void applyDefaults() {
    Defaults.setTableDefaults(inputTable, defaults);
  }

  public ArrayList<String> logBonds() {
    ArrayList<String> ret = new ArrayList<>(inputTable.getRowCount());
    if (inputTable.getValueAt(0, 0) == null) {
      return ret;
    }
    for (int i = 0; i < inputTable.getRowCount(); i++) {
      StringBuilder buff = new StringBuilder();
      for (int j = 0; j < inputTable.getColumnCount(); j++) {
        buff.append('\t').append(String.valueOf(inputTable.getValueAt(i, j)));
      }
      ret.add(buff.substring(1));
    }
    return ret;
  }

  public String logBondsShort() {
    StringBuilder ret = new StringBuilder();
    if (inputTable.getValueAt(0, 0) == null) {
      return "";
    }
    for (int i = 0; i < inputTable.getRowCount(); i++) {
      if (i != 0) {
        ret.append("\t|\t");
      }
      ret.append(inputTable.getValueAt(i, 0)).append(" ➜ ")
         .append(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().lastIndexOf('(')+1)).append(" ➜ ")
         .append(inputTable.getValueAt(i, 2)).append(" , ")
         .append(inputTable.getValueAt(i, 3)).append(" ➜ ")
         .append(inputTable.getValueAt(i, 4).toString().charAt(inputTable.getValueAt(i, 4).toString().lastIndexOf('(')+1)).append(" ➜ ")
         .append(inputTable.getValueAt(i, 5)).append(" - ")
         .append(inputTable.getValueAt(i, 6).toString().substring(0, inputTable.getValueAt(i, 6).toString().lastIndexOf('(')-1));
    }
    return ret.toString();
  }

  public ArrayList<BondReal> getBonds(boolean check) {
    if (inputTable.getValueAt(0, 0) == null) {
      return new ArrayList<BondReal>(0);
    }
    ArrayList<BondReal> ret = new ArrayList(inputTable.getRowCount());
    for (int i = 0; i < inputTable.getRowCount(); i++) {
      Set<String> proteins1;
      if (TARGETS[0].equals(inputTable.getValueAt(i, 0))) {
        proteins1 = proteins.keySet();
      } else {
        proteins1 = new HashSet(1);
        proteins1.add((String)inputTable.getValueAt(i, 0));
      }
      Set<String> proteins2;
      if (TARGETS[0].equals(inputTable.getValueAt(i, 3))) {
        proteins2 = proteins.keySet();
      } else {
        proteins2 = new HashSet(1);
        proteins2.add(TARGETS[1].equals(inputTable.getValueAt(i, 3)) ? null : (String)inputTable.getValueAt(i, 3));
      }
      char[] aminoacids1 = Monomer.resolveShortcut(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2)).toCharArray();
      char[] aminoacids2 = Monomer.resolveShortcut(inputTable.getValueAt(i, 4).toString().charAt(inputTable.getValueAt(i, 4).toString().length()-2)).toCharArray();
      String key = ((String)inputTable.getValueAt(i, 6));
      for (String protein1 : proteins1) {
        for (String protein2 : proteins2) {
          for (char aa1 : aminoacids1) {
            for (char aa2 : aminoacids2) {
              ret.add(new BondReal(key.substring(0, inputTable.getValueAt(i, 6).toString().lastIndexOf('(')-1), backup.get(key).getBond(),
                                   check ? backup.get(key).getCheckDiff() : BigDecimal.ZERO,
                                   protein1, aa1,
                                   inputTable.getValueAt(i, 2).equals(POSITIONS[0]) ? null : (Integer.parseInt((String)inputTable.getValueAt(i, 2))-proteins.get(protein1).start()),
                                   protein2, aa2,
                                   inputTable.getValueAt(i, 5).equals(POSITIONS[0]) ? null : (Integer.parseInt((String)inputTable.getValueAt(i, 5))-proteins.get(protein2 == null ? protein1 : protein2).start())));
            }
          }
        }
      }
    }
    return ret;
  }

  public void setProteins(LinkedHashMap<String, Protein> proteins) {
    // K čemu tu tohle bylo?
//    for (Protein protein : proteins.values()) {
//      protein.reset();
//    }
    HashMap<String, String> dict = new HashMap<>(this.proteins.size());
    dict.put((String)target1ComboBox.getItemAt(0), (String)target1ComboBox.getItemAt(0));
    dict.put((String)target2ComboBox.getItemAt(1), (String)target2ComboBox.getItemAt(1));
    for (Protein proteinOld : this.proteins.values()) {
      //proteinOld.reset();
      for (Protein proteinNew : proteins.values()) {
        if (proteinOld.getProtein().equals(proteinNew.getProtein()) &&
            (!dict.containsKey(proteinOld.getName()) || proteinOld.getName().equals(proteinNew.getName()))) {
          dict.put(proteinOld.getName(), proteinNew.getName());
        }
      }
    }

    edited.clear();
    for (int i = inputTable.getRowCount()-1; i >= 0; i--) {
      if (dict.containsKey(inputTable.getValueAt(i, 0)) && dict.containsKey(inputTable.getValueAt(i, 3))) {
        for (int j = 0; j < 6; j+=3) {
          Protein oldProtein = this.proteins.get(TARGETS[1].equals(inputTable.getValueAt(i, j)) ? inputTable.getValueAt(i, 0) : inputTable.getValueAt(i, j));
          Protein newProtein = proteins.get(dict.get(TARGETS[1].equals(inputTable.getValueAt(i, j)) ? inputTable.getValueAt(i, 0) : inputTable.getValueAt(i, j)));
          if (!POSITIONS[0].equals(inputTable.getValueAt(i, j+2)) && oldProtein != null && newProtein != null && oldProtein.getShift() != newProtein.getShift()) {
            inputTable.setValueAt(Integer.parseInt((String)inputTable.getValueAt(i, j+2)) - oldProtein.getShift() + newProtein.getShift() + "", i, j+2);
          }
        }
        inputTable.setValueAt(dict.get(inputTable.getValueAt(i, 0)), i, 0);
        inputTable.setValueAt(dict.get(inputTable.getValueAt(i, 3)), i, 3);
        // Vymazání můstků na zmutované pozice
        if (!TARGETS[0].equals(inputTable.getValueAt(i, 0))) {
          if (inputTable.getValueAt(i, 2).toString().equals(POSITIONS[0])) {
            if (proteins.get(inputTable.getValueAt(i, 0)).positions(inputTable.getValueAt(i, 1).toString().charAt(0)).isEmpty()) {
              ((DefaultTableModel)inputTable.getModel()).removeRow(i);
              continue;
            }
          } else {
            if (!proteins.get(inputTable.getValueAt(i, 0)).charsAt(Integer.parseInt(inputTable.getValueAt(i, 2).toString())-proteins.get(inputTable.getValueAt(i, 0)).start()).contains(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2))) {
              ((DefaultTableModel)inputTable.getModel()).removeRow(i);
              continue;
            }
          }
        }
        String target = (String)(TARGETS[1].equals(inputTable.getValueAt(i, 3)) ? inputTable.getValueAt(i, 0) : inputTable.getValueAt(i, 3));
        if (!TARGETS[0].equals(target)) {
          if (inputTable.getValueAt(i, 5).toString().equals(POSITIONS[0])) {
            if (proteins.get(target).positions(inputTable.getValueAt(i, 4).toString().charAt(0)).isEmpty()) {
              ((DefaultTableModel)inputTable.getModel()).removeRow(i);
              continue;
            }
          } else {
            if (!proteins.get(target).charsAt(Integer.parseInt(inputTable.getValueAt(i, 5).toString())-proteins.get(target).start()).contains(inputTable.getValueAt(i, 4).toString().charAt(0))) {
              ((DefaultTableModel)inputTable.getModel()).removeRow(i);
              continue;
            }
          }
        }
      } else {
        ((DefaultTableModel)inputTable.getModel()).removeRow(i);
      }
    }
    if (inputTable.getRowCount() == 0) {
      ((DefaultTableModel)inputTable.getModel()).addRow(new Object[] { null, null, null, null, null, null, null });
    }

    target1ComboBox.removeAllItems();
    target2ComboBox.removeAllItems();
    target1ComboBox.insertItemAt(TARGETS[0], target1ComboBox.getItemCount());
    target2ComboBox.insertItemAt(TARGETS[0], target2ComboBox.getItemCount());
    target2ComboBox.insertItemAt(TARGETS[1], target2ComboBox.getItemCount());
    for (String name : proteins.keySet()) {
      target1ComboBox.insertItemAt(name, target1ComboBox.getItemCount());
      target2ComboBox.insertItemAt(name, target2ComboBox.getItemCount());
    }
    this.proteins = proteins;
    
    ArrayList<String> bonds = logBonds();
    clean();
    setBonds(bonds);
  }

  public void setBonds(ArrayList<String> bonds) {
    edited.clear();
    boolean selected = specificCheckBox.isSelected();
    specificCheckBox.setSelected(false);
    for (String string : bonds) {
      try {
        String[] parts = string.split("\\t");
        target1ComboBox.setSelectedItem(parts[0]);
        type1ComboBox.setSelectedItem(parts[1]);
        position1ComboBox.setSelectedItem(parts[2]);
        target2ComboBox.setSelectedItem(parts[3]);
        type2ComboBox.setSelectedItem(parts[4]);
        position2ComboBox.setSelectedItem(parts[5]);
        String tmp = parts[6].substring(0, parts[6].lastIndexOf(" ("));
        bondsComboBox.setSelectedItem(tmp);
        if (target1ComboBox.getSelectedItem().equals(parts[0]) && type1ComboBox.getSelectedItem().equals(parts[1]) && verifyType(type1ComboBox) &&
            position1ComboBox.getSelectedItem().equals(parts[2]) && verifyPosition(position1ComboBox) &&
            target2ComboBox.getSelectedItem().equals(parts[3]) && type2ComboBox.getSelectedItem().equals(parts[4]) && verifyType(type2ComboBox) &&
            position2ComboBox.getSelectedItem().equals(parts[5]) && verifyPosition(position2ComboBox) && bondsComboBox.getSelectedItem().equals(tmp)) {
          insert(false);
        }
      } catch (Exception e) {
        System.out.println(e);
      }
    }
    specificCheckBox.setSelected(selected);
    cleanInput();
  }

  public boolean check(boolean alert) {
    if (inputTable.getRowCount() == 1 && (inputTable.getValueAt(0, 0) == null || inputTable.getValueAt(0, 0).toString().isEmpty())) {
      return true;
    }
    for (int i = 0; i < inputTable.getRowCount(); i++) {
      // Uživatel se mohl vrátit a přidat něco, co může způsobit problém
      if (!(modificationsCard.verifyInput(this, (String)inputTable.getValueAt(i, 0), (String)inputTable.getValueAt(i, 1), (String)inputTable.getValueAt(i, 2), alert ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE) &&
            modificationsCard.verifyInput(this, TARGETS[1].equals(inputTable.getValueAt(i, 3)) ? (String)inputTable.getValueAt(i, 0) :
                                          (String)inputTable.getValueAt(i, 3), (String)inputTable.getValueAt(i, 4), (String)inputTable.getValueAt(i, 5), alert ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE))) {
        return false;
      }

      for (int j = 0; j < 6; j+=3) {
        boolean none = true;
        for (char aa : Monomer.resolveShortcut(inputTable.getValueAt(i, j+1).toString().charAt(inputTable.getValueAt(i, j+1).toString().length()-2)).toCharArray()) {
          if (inputTable.getValueAt(i, j+2).toString().equals(POSITIONS[0])) { // Position = All
            if (TARGETS[0].equals(inputTable.getValueAt(i, j)) || (TARGETS[1].equals(inputTable.getValueAt(i, j)) && TARGETS[0].equals(inputTable.getValueAt(i, 0)))) {
              // Target = All || Target = {previous} = All
              for (Protein protein : proteins.values()) {
                if (!protein.positions(aa).isEmpty()) {
                  none = false;
                  break;
                }
              }
              if (!none) {
                break;
              }
            } else {
              if (!proteins.get(TARGETS[1].equals(inputTable.getValueAt(i, j)) ? inputTable.getValueAt(i, 0) : inputTable.getValueAt(i, j)).positions(aa).isEmpty()) {
                none = false;
                break;
              }
            }
          } else { // Position = {local} => Target = {specific} || Target = {previous} = {specific}
            Protein protein = proteins.get(TARGETS[1].equals(inputTable.getValueAt(i, j)) ? inputTable.getValueAt(i, 0) : inputTable.getValueAt(i, j));
            if (protein.charsAt(Integer.parseInt(inputTable.getValueAt(i, j+2).toString())-protein.start()).contains(aa)) {
              none = false;
              break;
            }
          }
        }
        if (none) {
          // TODO Ve výpisu zobrazit i okolí místa
          JOptionPane.showMessageDialog(this, "There is unused bond." + System.lineSeparator() + inputTable.getValueAt(i, 0) + " - " +
                                              inputTable.getValueAt(i, 1) + " - " + inputTable.getValueAt(i, 2) + " - " + inputTable.getValueAt(i, 3) + " - " +
                                              inputTable.getValueAt(i, 4) + " - " + inputTable.getValueAt(i, 5) + " - " + inputTable.getValueAt(i, 6) +
                                              System.lineSeparator() + "This can occur when bond is inserted on the mutated position and mutation is subsequently removed.",
                                        "Invalid input", JOptionPane.WARNING_MESSAGE);
          return false;
        }
      }
    }
    release();
    return true;
  }

  public void release() {
    if (type1ComboBox.getEditor().getEditorComponent().hasFocus() && !verifyType(type1ComboBox)) {
      type1ComboBox.setSelectedIndex(-1);
    }
    if (type2ComboBox.getEditor().getEditorComponent().hasFocus() && !verifyType(type2ComboBox)) {
      type2ComboBox.setSelectedIndex(-1);
    }
    if (position1ComboBox.getEditor().getEditorComponent().hasFocus() && !verifyPosition(position1ComboBox)) {
      position1ComboBox.setSelectedIndex(-1);
    }
    if (position2ComboBox.getEditor().getEditorComponent().hasFocus() && !verifyPosition(position2ComboBox)) {
      position2ComboBox.setSelectedIndex(-1);
    }
  }

  public void applyBonds(boolean check) {
    if (inputTable.getRowCount() == 1 && inputTable.getValueAt(0, 0) == null) {
      return;
    }
    for (int i = 0; i < inputTable.getRowCount(); i++) {
      String key = inputTable.getValueAt(i, 6).toString();
      String mod = key.substring(0, inputTable.getValueAt(i, 6).toString().lastIndexOf('(')-1);
      if (inputTable.getValueAt(i, 0).equals(inputTable.getValueAt(i, 3)) || TARGETS[1].equals(inputTable.getValueAt(i, 3)) ||
          inputTable.getValueAt(i, 0).equals(TARGETS[0]) || TARGETS[0].equals(inputTable.getValueAt(i, 3))) { // Stejné proteiny - můstek může být v rámci jednoho peptidu
        String name = (TARGETS[0].equals(inputTable.getValueAt(i, 0)) && !TARGETS[1].equals(inputTable.getValueAt(i, 3))) ?
                      inputTable.getValueAt(i, 3).toString() : inputTable.getValueAt(i, 0).toString();
        BigDecimal mass = backup.get(key).getBond();
        BigDecimal diff = check ? backup.get(key).getCheckDiff() : BigDecimal.ZERO;
        for (char aa1 : Monomer.resolveShortcut(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2)).toCharArray()) {
          for (char aa2 : Monomer.resolveShortcut(inputTable.getValueAt(i, 4).toString().charAt(inputTable.getValueAt(i, 4).toString().length()-2)).toCharArray()) {
            if (name.equals(TARGETS[0])) {
              for (String protein : proteins.keySet()) {
                proteins.get(protein).addVariableBond(new BondReal(mod, mass, diff,
                             protein, aa1, inputTable.getValueAt(i, 2).equals(POSITIONS[0]) ? null : (Integer.parseInt(inputTable.getValueAt(i, 2).toString())-proteins.get(protein).start()),
                             TARGETS[1].equals(inputTable.getValueAt(i, 3)) ? null : protein,
                                      aa2, inputTable.getValueAt(i, 5).equals(POSITIONS[0]) ? null : (Integer.parseInt(inputTable.getValueAt(i, 5).toString())-proteins.get(protein).start())));
              }
            } else {
              proteins.get(name).addVariableBond(new BondReal(mod, mass, diff,
                                name, aa1, inputTable.getValueAt(i, 2).equals(POSITIONS[0]) ? null : (Integer.parseInt(inputTable.getValueAt(i, 2).toString())-proteins.get(name).start()),
                                TARGETS[1].equals(inputTable.getValueAt(i, 3)) ? null : name,
                                      aa2, inputTable.getValueAt(i, 5).equals(POSITIONS[0]) ? null : (Integer.parseInt(inputTable.getValueAt(i, 5).toString())-proteins.get(name).start())));
            }
          }
        }
      }
      if (!(inputTable.getValueAt(i, 0).equals(inputTable.getValueAt(i, 3)) || TARGETS[1].equals(inputTable.getValueAt(i, 3)))) {
        // Update min/max hmotnosti - propagace poloviny můstku na části;
        BigDecimal mass = backup.get(key).getBond().divide(new BigDecimal(2));
        for (int j = 0; j < 6; j+=3) {
          String name = inputTable.getValueAt(i, j).toString();
          Collection<Protein> ps;
          if (TARGETS[0].equals(name)) {
            ps = proteins.values();
          } else {
            ps = new ArrayList<>(1);
            ps.add(proteins.get(name));
          }
          for (char aa : Monomer.resolveShortcut(inputTable.getValueAt(i, j+1).toString().charAt(inputTable.getValueAt(i, j+1).toString().length()-2)).toCharArray()) {
            if (inputTable.getValueAt(i, j+2).equals(POSITIONS[0])) {
              for (Protein protein : ps) {
                protein.updateMinMax(aa, mass);
              }
            } else {
              for (Protein protein : ps) {
                protein.updateMinMax(Integer.parseInt(inputTable.getValueAt(i, j+2).toString())-protein.start(), aa, mass);
              }
            }
          }
        }
      }

      if (backup.get(key).getMod(0) != null) {
        String name = inputTable.getValueAt(i, 0).toString();
        BigDecimal mass = backup.get(key).getMod(0);
        BigDecimal diff = check ? backup.get(key).getCheckDiff(0) : BigDecimal.ZERO;
        for (char aa : Monomer.resolveShortcut(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2)).toCharArray()) {
          if (name.equals(TARGETS[0])) {
            if (inputTable.getValueAt(i, 2).equals(POSITIONS[0])) {
              for (Protein protein : proteins.values()) {
                protein.addVariableModification(new ModificationReal(mod, mass, diff, name, aa, null), 0);
              }
            } else {
              for (Protein protein : proteins.values()) {
                protein.addVariableModification(new ModificationReal(mod, mass, diff, name, aa, Integer.parseInt(inputTable.getValueAt(i, 2).toString())-protein.start()), 0);
              }
            }
          } else {
            if (inputTable.getValueAt(i, 2).equals(POSITIONS[0])) {
              proteins.get(name).addVariableModification(new ModificationReal(mod, mass, diff, name, aa, null), 0);
            } else {
              proteins.get(name).addVariableModification(new ModificationReal(mod, mass, diff, name, aa, Integer.parseInt(inputTable.getValueAt(i, 2).toString())-proteins.get(name).start()), 0);
            }
          }
        }
      }
      if (backup.get(key).getMod(1) != null) {
        String name = TARGETS[1].equals(inputTable.getValueAt(i, 3)) ? inputTable.getValueAt(i, 0).toString() : inputTable.getValueAt(i, 3).toString();
        BigDecimal mass = backup.get(key).getMod(1);
        BigDecimal diff = check ? backup.get(key).getCheckDiff(1) : BigDecimal.ZERO;
        for (char aa : Monomer.resolveShortcut(inputTable.getValueAt(i, 4).toString().charAt(inputTable.getValueAt(i, 4).toString().length()-2)).toCharArray()) {
          if (name.equals(TARGETS[0])) {
            if (inputTable.getValueAt(i, 5).equals(POSITIONS[0])) {
              for (Protein protein : proteins.values()) {
                protein.addVariableModification(new ModificationReal(mod, mass, diff, name, aa, null), 0);
              }
            } else {
              for (Protein protein : proteins.values()) {
                protein.addVariableModification(new ModificationReal(mod, mass, diff, name, aa, Integer.parseInt(inputTable.getValueAt(i, 5).toString())-protein.start()), 0);
              }
            }
          } else {
            if (inputTable.getValueAt(i, 5).equals(POSITIONS[0])) {
              proteins.get(name).addVariableModification(new ModificationReal(mod, mass, diff, name, aa, null), 0);
            } else {
              proteins.get(name).addVariableModification(new ModificationReal(mod, mass, diff, name, aa, Integer.parseInt(inputTable.getValueAt(i, 5).toString())-proteins.get(name).start()), 0);
            }
          }
        }
      }
    }
  }

  private void loadBonds() {
    bonds = new TreeMap();
    Properties file = Defaults.loadDefaults(BondAbstract.FILE);
    for (String key : file.stringPropertyNames()) {
      bonds.put(key, new BondAbstract(key, file.getProperty(key)));
    }
  }

  public void reloadBonds() {
    loadBonds();
    bondsComboBox.setModel(new DefaultComboBoxModel<>(bonds.keySet().toArray()));
    if (specificCheckBox.isSelected()) {
      filterBonds();
    }
  }

  private void loadTemplates() {
    templates = new TreeMap();
    Properties file = Defaults.loadDefaults(TEMPLATES_FILE);
    for (String key : file.stringPropertyNames()) {
      String[] parts = file.getProperty(key).split(BONDS_SEPARATOR);
      ArrayList<String[]> template = new ArrayList(parts.length);
      for (String string : parts) {
        String[] mod = string.split(BOND_SEPARATOR);
        if (mod.length == 3) {
          template.add(mod);
        }
      }
      templates.put(key, template);
    }
    templatesComboBox.removeAllItems();
    templatesComboBox.addItem(TEMPLATE);
    for (String key : templates.keySet()) {
      templatesComboBox.addItem(key);
    }
    templatesComboBox.setEnabled(!templates.isEmpty());
  }

  private void filterBonds() {
    if (type1ComboBox.getItemCount() == 0 || type2ComboBox.getItemCount() == 0) {
      bondsComboBox.setModel(new DefaultComboBoxModel<>(new Object[0]));
      return;
    }
    // Profiltrování podle specificity
    HashSet<Character> selected1 = new HashSet<>();
    if (((String)type1ComboBox.getSelectedItem()).matches(".*\\(.\\)")) {
      selected1.add(((String)type1ComboBox.getSelectedItem()).charAt(((String)type1ComboBox.getSelectedItem()).length()-2));
    } else {
      for (char c : ((String)type1ComboBox.getSelectedItem()).replaceAll("[ ,]", "").toCharArray()) {
        selected1.add(c);
      }
    }
    HashSet<Character> selected2 = new HashSet<>();
    if (((String)type2ComboBox.getSelectedItem()).matches(".*\\(.\\)")) {
      selected2.add(((String)type2ComboBox.getSelectedItem()).charAt(((String)type2ComboBox.getSelectedItem()).length()-2));
    } else {
      for (char c : ((String)type2ComboBox.getSelectedItem()).replaceAll("[ ,]", "").toCharArray()) {
        selected2.add(c);
      }
    }
    HashSet<Character> resolved1 = new HashSet();
    for (Character character : selected1) {
      if (Monomer.isShortcut(character)) {
        for (char ch : Monomer.resolveShortcut(character).toCharArray()) {
          resolved1.add(ch);
        }
      } else {
        resolved1.add(character);
      }
    }
    HashSet<Character> resolved2 = new HashSet();
    for (Character character : selected2) {
      if (Monomer.isShortcut(character)) {
        for (char ch : Monomer.resolveShortcut(character).toCharArray()) {
          resolved2.add(ch);
        }
      } else {
        resolved2.add(character);
      }
    }
    for (String string : bonds.keySet()) {
      boolean[] add = new boolean[] { true, false };
      for (Character character1 : resolved1) {
        if (!(bonds.get(string).specific(0, character1))) {
          add[0] = false;
          if (!add[1]) {
            bondsComboBox.removeItem(string);
            break;
          }
        }
        if (!(bonds.get(string).specific(1, character1))) {
          add[1] = false;
          if (!add[0]) {
            bondsComboBox.removeItem(string);
            break;
          }
        }
      }
      for (Character character2 : resolved2) {
        if (!(bonds.get(string).specific(1, character2))) {
          add[0] = false;
          if (!add[1]) {
            bondsComboBox.removeItem(string);
            break;
          }
        }
        if (!(bonds.get(string).specific(0, character2))) {
          add[1] = false;
          if (!add[0]) {
            bondsComboBox.removeItem(string);
            break;
          }
        }
      }
    }
  }

  public void clean() {
    cleanTable();
    cleanInput();
  }

  private void cleanTable() {
    ((DefaultTableModel)inputTable.getModel()).setRowCount(1);
    for (int i = 0; i < inputTable.getColumnCount(); i++) {
      inputTable.setValueAt(null, 0, i);
    }
  }

  private void cleanInput() {
    target1ComboBox.setSelectedIndex(0);
    type1ComboBox.setSelectedIndex(type1ComboBox.getItemCount() == 0 ? -1 : 0);
    position1ComboBox.setSelectedIndex(0);
    target2ComboBox.setSelectedIndex(0);
    type2ComboBox.setSelectedIndex(type2ComboBox.getItemCount() == 0 ? -1 : 0);
    position2ComboBox.setSelectedIndex(0);
    bondsComboBox.setSelectedIndex(bondsComboBox.getItemCount() == 0 ? -1 : 0);
    target1ComboBox.requestFocus();
  }

  private void showPopupMenu(int x, int y) {
    JPopupMenu popupMenu = new JPopupMenu();

    JMenuItem mi = new JMenuItem("Delete", 'D');
    mi.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        deleteSelected();
      }
    });
    popupMenu.add(mi);
    mi = new JMenuItem("Edit", 'E');
    mi.setEnabled(canEdit());
    mi.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        editSelected();
      }
    });
    popupMenu.add(mi);
    mi = new JMenuItem("Create template", 'T');
    mi.setEnabled(canTemplate());
    mi.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        templateSelected();
      }
    });
    popupMenu.add(mi);

    popupMenu.show(inputTable, x, y);
  }

  private void deleteSelected() {
    if (inputTable.getSelectedRowCount() == inputTable.getRowCount()) {
      cleanTable();
      inputTable.setRowSelectionInterval(0, 0);
    } else {
      int position = delete(inputTable.getSelectedRows());
      if (position >= inputTable.getRowCount()) {
        position = inputTable.getRowCount()-1;
      }
      if (position >= 0) {
        inputTable.setRowSelectionInterval(position, position);
      }
    }
  }
  
  private int delete(int[] remove) {
    Arrays.sort(remove);
    boolean all = remove.length >= inputTable.getRowCount() && (remove.length == 0 || (remove[0] <= 0 && remove[remove.length-1] >= inputTable.getRowCount()-1));
    if (all) {
      for (int i = 1; i < remove.length; i++) {
        if (remove[i]-remove[i-1] > 1 && 0 < remove[i] && remove[i-1] < inputTable.getRowCount()-1) {
          all = false;
          break;
        }
      }
    }
    if (all) {
      cleanTable();
      return 0;
    } else if (remove.length > 0) {
      for (int i = remove.length; i-- > 0; ) {
        if (0 <= remove[i] && remove[i] < inputTable.getRowCount()) {
          ((DefaultTableModel)inputTable.getModel()).removeRow(inputTable.convertRowIndexToModel(remove[i]));
        }
      }
      return remove[0];
    }
    return -1;
  }

  private boolean canEdit() {
    switch (inputTable.getSelectedRowCount()) {
      case 1:
        return true;
      case 0:
        return false;
    }

    int[] selected = inputTable.getSelectedRows();
    for (int j : new int[]{ 6, 3, 0 }) {
      for (int i = 1; i < selected.length; i++) {
        if (!inputTable.getValueAt(selected[i-1], j).equals(inputTable.getValueAt(selected[i], j))) {
          return false;
        }
      }
    }
    ArrayList<Byte> diffs = new ArrayList(2);
    for (byte[] a : new byte[][]{ new byte[]{ 1, 2 }, new byte[]{ 4, 5 } }) {
      boolean same = true;
      for (byte j : a) {
        for (int i = 1; i < selected.length; i++) {
          if (!inputTable.getValueAt(selected[i-1], j).equals(inputTable.getValueAt(selected[i], j))) {
            if (same) {
              same = false;
              diffs.add(j);
              break;
            } else {
              return false;
            }
          }
        }
      }
    }
    if (diffs.size() < 2) {
      return true;
    }
    HashMap<String, TreeSet<String>> pairs = new HashMap();
    for (int i = 0; i < selected.length; i++) {
      if (!pairs.containsKey(inputTable.getValueAt(selected[i], diffs.get(0)))) {
        pairs.put((String)inputTable.getValueAt(selected[i], diffs.get(0)), new TreeSet<String>());
      }
      pairs.get(inputTable.getValueAt(selected[i], diffs.get(0))).add((String)inputTable.getValueAt(selected[i], diffs.get(1)));
    }
    TreeSet<String> sample = pairs.get(inputTable.getValueAt(selected[0], diffs.get(0)));
    for (TreeSet<String> treeSet : pairs.values()) {
      if (!treeSet.equals(sample)) {
        return false;
      }
    }

    return true;
  }

  private void editSelected() {
    if (inputTable.getSelectedRowCount() == 0) {
      JOptionPane.showMessageDialog(this, "Some row must be selected.", "No item selected", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    if (inputTable.getValueAt(0, 0) == null || inputTable.getValueAt(0, 0).equals("")) {
      JOptionPane.showMessageDialog(this, "No item is inserted.", "No item selected", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    if (!canEdit()) {
      JOptionPane.showMessageDialog(this, "Incompatible items selected.", "Incompatible items", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    edited.clear();

    int[] selected = inputTable.getSelectedRows();
    target1ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 0));
    target2ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 3));
    if (inputTable.getSelectedRowCount() == 1) {
      type1ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 1));
      position1ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 2));
      type2ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 4));
      position2ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 5));
    } else {
      String same = "";
      for (int i = 1; i < selected.length; i++) {
        if (!inputTable.getValueAt(selected[i-1], 1).equals(inputTable.getValueAt(selected[i], 1))) {
          same += inputTable.getValueAt(selected[i], 1).toString().charAt(inputTable.getValueAt(selected[i], 1).toString().length()-2);
        }
      }
      if (same.isEmpty()) {
        type1ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 1));
      } else {
        type1ComboBox.setSelectedItem(inputTable.getValueAt(selected[0], 1).toString().charAt(inputTable.getValueAt(selected[0], 1).toString().length()-2) + same);
      }
      same = "";
      for (int i = 1; i < selected.length; i++) {
        if (!inputTable.getValueAt(selected[i-1], 2).equals(inputTable.getValueAt(selected[i], 2))) {
          same += " " + inputTable.getValueAt(selected[i], 2);
        }
      }
      if (same.isEmpty()) {
        position1ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 2));
      } else {
        position1ComboBox.setSelectedItem(inputTable.getValueAt(selected[0], 2) + same);
      }
      same = "";
      for (int i = 1; i < selected.length; i++) {
        if (!inputTable.getValueAt(selected[i-1], 4).equals(inputTable.getValueAt(selected[i], 4))) {
          same += inputTable.getValueAt(selected[i], 4).toString().charAt(inputTable.getValueAt(selected[i], 4).toString().length()-2);
        }
      }
      if (same.isEmpty()) {
        type2ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 4));
      } else {
        type2ComboBox.setSelectedItem(inputTable.getValueAt(selected[0], 4).toString().charAt(inputTable.getValueAt(selected[0], 4).toString().length()-2) + same);
      }
      same = "";
      for (int i = 1; i < selected.length; i++) {
        if (!inputTable.getValueAt(selected[i-1], 5).equals(inputTable.getValueAt(selected[i], 5))) {
          same += " " + inputTable.getValueAt(selected[i], 5);
        }
      }
      if (same.isEmpty()) {
        position2ComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 5));
      } else {
        position2ComboBox.setSelectedItem(inputTable.getValueAt(selected[0], 5) + same);
      }
    }

    String mod = ((String)inputTable.getValueAt(inputTable.getSelectedRow(), 6)).split(" \\(")[0];
    bondsComboBox.setSelectedItem(mod);
    if (!bondsComboBox.getSelectedItem().equals(mod)) {
      if (specificCheckBox.isSelected()) {
        specificCheckBox.doClick();
        bondsComboBox.setSelectedItem(mod);
      }
      if (!bondsComboBox.getSelectedItem().equals(mod)) {
        JOptionPane.showMessageDialog(inputTable, "Modification wasn't found in the list of modifications!"  + System.lineSeparator() +
                                      "It was probably removed or renamed.", "Modification not found", JOptionPane.ERROR_MESSAGE);
        inputTable.setRowSelectionInterval(inputTable.getSelectedRow(), inputTable.getSelectedRow());
        return;
      }
    }

    for (int i : selected) {
      Object[] e = new Object[inputTable.getColumnCount()];
      for (int j = 0; j < e.length; j++) {
        e[j] = inputTable.getValueAt(i, j);
      }
      edited.add(e);
    }
    inputTable.repaint();
    target1ComboBox.requestFocus();
  }

  private boolean canTemplate() {
    if (inputTable.getSelectedRowCount() == 0 || inputTable.getValueAt(inputTable.getSelectedRow(), 0) == null) {
      return false;
    }

    return true;
  }

  public void templateSelected() {
    if (!canTemplate()) {
      return;
    }

    LinkedHashSet<String> lhs = new LinkedHashSet(inputTable.getSelectedRowCount());
    for (int i : inputTable.getSelectedRows()) {
      lhs.add(inputTable.getValueAt(i, 1).toString() + BOND_SEPARATOR + inputTable.getValueAt(i, 4) + BOND_SEPARATOR + inputTable.getValueAt(i, 6));
    }
    StringBuilder sb = new StringBuilder();
    for (String s : lhs) {
      sb.append(BONDS_SEPARATOR).append(s);
    }
    JPanel jpn = new JPanel();
    GroupLayout gl = new GroupLayout(jpn);
    jpn.setLayout(gl);
    JLabel jlh = new JLabel("Created template:");
    jpn.add(jlh);
    JTextArea jta = new JTextArea(sb.substring(1));
    jta.setEditable(false);
    jta.setTabSize(10);
    JScrollPane jsp = new JScrollPane(jta);
    jpn.add(jsp);
    JLabel jlf = new JLabel("Please enter the name for the saved profile:");
    jpn.add(jlf);
    gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addComponent(jlh)
      .addComponent(jsp)
      .addComponent(jlf));
    gl.setVerticalGroup(gl.createSequentialGroup()
      .addComponent(jlh)
      .addComponent(jsp)
      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
      .addComponent(jlf));
    if (jpn.getPreferredSize().getWidth() > Toolkit.getDefaultToolkit().getScreenSize().width/2) {
      jpn.setPreferredSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width/2,
                                         Math.min(jpn.getPreferredSize().height + jsp.getHorizontalScrollBar().getPreferredSize().height,
                                                  Toolkit.getDefaultToolkit().getScreenSize().height/2)));
    }
    if (jpn.getPreferredSize().getHeight() > Toolkit.getDefaultToolkit().getScreenSize().height/2) {
      jpn.setPreferredSize(new Dimension(Math.min(jpn.getPreferredSize().width + jsp.getVerticalScrollBar().getPreferredSize().width,
                                                  Toolkit.getDefaultToolkit().getScreenSize().width/2),
                                         Toolkit.getDefaultToolkit().getScreenSize().height/2));
    }

    String name = null;
    while (true) {
      name = JOptionPane.showInputDialog(this, jpn, "Enter template title", JOptionPane.PLAIN_MESSAGE);
      if (name == null) {
        return;
      }
      if (name.isEmpty()) {
        continue;
      }
      if (!templates.containsKey(name)) { // Test, zda je jméno v pořádku - unikátní nebo přepsat
        break;
      }
      JPanel jpc = new JPanel();
      GroupLayout glc = new GroupLayout(jpc);
      jpc.setLayout(glc);
      jlh = new JLabel("There already exists a profile with entered name!");
      jpc.add(jlh);
      JLabel jls = new JLabel("Stored template:");
      jpc.add(jls);
      JLabel jln = new JLabel("New template:");
      jpc.add(jln);
      StringBuilder sbs = new StringBuilder();
      for (String[] mod : templates.get(name)) {
        for (int i = 0; i < mod.length; i++) {
          sbs.append(i == 0 ? BONDS_SEPARATOR : BOND_SEPARATOR).append(mod[i]);
        }
      }
      JTextArea jtas = new JTextArea(sbs.substring(1));
      jtas.setEditable(false);
      jtas.setTabSize(10);
      JScrollPane jsps = new JScrollPane(jtas);
      jpc.add(jsps);
      JTextArea jtan = new JTextArea(jta.getText());
      jtan.setEditable(false);
      jtan.setTabSize(10);
      JScrollPane jspn = new JScrollPane(jtan);
      jpc.add(jspn);
      jlf = new JLabel("Do you wish to overwrite it?");
      jpc.add(jlf);
      glc.setHorizontalGroup(glc.createParallelGroup(GroupLayout.Alignment.LEADING)
        .addComponent(jlh)
        .addGroup(glc.createSequentialGroup()
          .addGroup(glc.createParallelGroup()
            .addComponent(jls)
            .addComponent(jsps))
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addGroup(glc.createParallelGroup()
            .addComponent(jln)
            .addComponent(jspn)))
        .addComponent(jlf));
      glc.setVerticalGroup(glc.createSequentialGroup()
        .addComponent(jlh)
        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        .addGroup(glc.createParallelGroup()
          .addComponent(jls)
          .addComponent(jln))
        .addGroup(glc.createParallelGroup()
          .addComponent(jsps)
          .addComponent(jspn))
        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        .addComponent(jlf));
      if (jpc.getPreferredSize().getWidth() > Toolkit.getDefaultToolkit().getScreenSize().width*0.75) {
        jpc.setPreferredSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width*3/4,
                                           Math.min(jpc.getPreferredSize().height + jsp.getHorizontalScrollBar().getPreferredSize().height,
                                                    Toolkit.getDefaultToolkit().getScreenSize().height/2)));
      }
      if (jpc.getPreferredSize().getHeight() > Toolkit.getDefaultToolkit().getScreenSize().height/2) {
        jpc.setPreferredSize(new Dimension(Math.min(jpc.getPreferredSize().width + 2*jsp.getVerticalScrollBar().getPreferredSize().width,
                                                    Toolkit.getDefaultToolkit().getScreenSize().width*3/4),
                                           Toolkit.getDefaultToolkit().getScreenSize().height/2));
      }

      int ret = JOptionPane.showConfirmDialog(this, jpc, "Name collision", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
      if (ret == JOptionPane.CANCEL_OPTION || ret == JOptionPane.CLOSED_OPTION) {
        return;
      }
      if (ret == JOptionPane.YES_OPTION) {
        break;
      }
    }
    Properties file = Defaults.loadDefaults(TEMPLATES_FILE);
    file.setProperty(name, jta.getText());
    Defaults.saveDefaults(TEMPLATES_FILE, file);
    loadTemplates();
  }

  private void insert(boolean alert) {
    if (templatesComboBox.getSelectedIndex() == 0 && ((type1ComboBox.getEditor().getEditorComponent().hasFocus() && !verifyType(type1ComboBox)) ||
                                                      (type2ComboBox.getEditor().getEditorComponent().hasFocus() && !verifyType(type2ComboBox)) ||
                                                      (position1ComboBox.getEditor().getEditorComponent().hasFocus() && !verifyPosition(position1ComboBox)) ||
                                                      (position2ComboBox.getEditor().getEditorComponent().hasFocus() && !verifyPosition(position2ComboBox)))) {
      return;
    }
    // Zřejmě zatrhnuto filtrování dle specificity a tam žádná možnost není.
    if (templatesComboBox.getSelectedIndex() == 0 && bondsComboBox.getSelectedItem() == null) {
      if (alert) {
        JOptionPane.showMessageDialog(this, "No bond is selected!", "Cannot insert", JOptionPane.WARNING_MESSAGE);
        bondsComboBox.requestFocus();
      }
      return;
    }

    ArrayList<Integer> replace = new ArrayList(edited.size());
    if (!edited.isEmpty()) {
      for (int i = 0; i < inputTable.getRowCount(); i++) {
        for (Object[] line : edited) {
          boolean equal = true;
          for (int j = 0; j < inputTable.getColumnCount(); j++) {
            if (!inputTable.getValueAt(i, j).equals(line[j])) {
              equal = false;
              break;
            }
          }
          if (equal) {
            replace.add(i);
          }
        }
      }
    }

    if (templatesComboBox.getSelectedIndex() > 0) {
      String selected = templatesComboBox.getSelectedItem().toString();
      boolean specificity = specificCheckBox.isSelected();
      position1ComboBox.setSelectedIndex(0);
      position2ComboBox.setSelectedIndex(0);
      specificCheckBox.setSelected(false);
      templatesComboBox.setSelectedIndex(0);
      int ins = inputTable.getRowCount();
      for (String[] mod : templates.get(selected)) {
        type1ComboBox.setSelectedItem(mod[0]);
        if (!mod[0].equals(type1ComboBox.getSelectedItem())) {
          continue;
        }
        type2ComboBox.setSelectedItem(mod[1]);
        if (!mod[1].equals(type2ComboBox.getSelectedItem())) {
          continue;
        }
        String m = mod[2].replaceFirst(" \\(.*\\)$", "");
        bondsComboBox.setSelectedItem(m);
        if (!m.equals(bondsComboBox.getSelectedItem())) {
          continue;
        }
        ins = insertBond(replace, ins, true);
      }
      specificCheckBox.setSelected(specificity);
    } else {
      insertBond(replace, inputTable.getRowCount(), alert);
    }
  }

  private int insertBond(ArrayList<Integer> replace, int ins, boolean alert) {
    // Je jednodušší smazat, než řešit multivklady
    if (inputTable.getRowCount() == 1 && inputTable.getValueAt(0, 0) == null) {
      replace.add(0);
    }
    
    String[] aas1 = type1ComboBox.getSelectedIndex() < 0 ? ((String)type1ComboBox.getSelectedItem()).split(", ")
                                                       : new String[] { (String)type1ComboBox.getSelectedItem() };
    String[] positions1 = position1ComboBox.getSelectedIndex() < 0 ? ((String)position1ComboBox.getSelectedItem()).split(", ")
                                                                 : new String[] { (String)position1ComboBox.getSelectedItem() };
    String[] aas2 = type2ComboBox.getSelectedIndex() < 0 ? ((String)type2ComboBox.getSelectedItem()).split(", ")
                                                       : new String[] { (String)type2ComboBox.getSelectedItem() };
    String[] positions2 = position2ComboBox.getSelectedIndex() < 0 ? ((String)position2ComboBox.getSelectedItem()).split(", ")
                                                                 : new String[] { (String)position2ComboBox.getSelectedItem() };
    for (int i = 0; i < aas1.length; i++) {
      aas1[i] = longer(aas1[i]);
    }
    for (int i = 0; i < aas2.length; i++) {
      aas2[i] = longer(aas2[i]);
    }
    TreeMap<String, TreeMap<String, TreeMap<String, TreeSet<String>>>> comb = new TreeMap();
    boolean empty = true;
    for (String aa1 : aas1) {
      comb.put(aa1, new TreeMap<String, TreeMap<String, TreeSet<String>>>());
      for (String position1 : positions1) {
        comb.get(aa1).put(position1, new TreeMap<String, TreeSet<String>>());
        for (String aa2 : aas2) {
        comb.get(aa1).get(position1).put(aa2, new TreeSet<String>());
          for (String position2 : positions2) {
            if (verifyInput(replace, aa1, position1, aa2, position2, alert)) {
              comb.get(aa1).get(position1).get(aa2).add(position2);
              empty = false;
            }
          }
        }
      }
    }
    if (empty) {
      return ins;
    }

    if (!edited.isEmpty()) {
      int[] rem = new int[replace.size()];
      for (int i = 0; i < replace.size(); i++) {
        rem[i] = replace.get(i);
      }
      ins = delete(rem);
      edited.clear();
    }
    if (inputTable.getRowCount() == 1 && inputTable.getValueAt(0, 0) == null) {
      ((DefaultTableModel)inputTable.getModel()).removeRow(0);
    }
    if (ins < 0) {
      ins = 0;
    } else if (ins > inputTable.getRowCount()) {
      ins = inputTable.getRowCount();
    }

    String name = bondsComboBox.getSelectedItem().toString();
    String bod = name + " (" + Defaults.sMassShortFormat.format(bonds.get(name).getBond());
    if (bonds.get(name).getCheckDiff().compareTo(BigDecimal.ZERO) != 0) {
      bod += "/ " + Defaults.sMassShortFormat.format(bonds.get(name).getCheckDiff());
    }
    boolean add = false;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bonds.get(name).getMods().length; i++) {
      sb.append("; ");
      if (bonds.get(name).getMod(i) == null) {
        sb.append("---");
      } else {
        add = true;
        sb.append(Defaults.sMassShortFormat.format(bonds.get(name).getMod(i)));
        if (bonds.get(name).getCheckDiff(i).compareTo(BigDecimal.ZERO) != 0) {
          sb.append("/ ").append(Defaults.sMassShortFormat.format(bonds.get(name).getCheckDiff(i)));
        }
      }
    }
    if (add) {
      bod += sb.toString();
    }
    bod += ")";
    for (String aa1 : comb.keySet()) {
      for (String position1 : comb.get(aa1).keySet()) {
        for (String aa2 : comb.get(aa1).get(position1).keySet()) {
          for (String position2 : comb.get(aa1).get(position1).get(aa2)) {
            ((DefaultTableModel)inputTable.getModel()).insertRow(ins++, new Object[] { target1ComboBox.getSelectedItem().toString(), aa1, position1,
                                                                                   target2ComboBox.getSelectedItem().toString(), aa2, position2, bod});
          }
        }
      }
    }
    backup.put(bod, bonds.get(name));
    return ins;
  }

  private boolean verifyInput(ArrayList<Integer> skip, String type1, String position1, String type2, String position2, boolean alert) {
    if (!(modificationsCard.verifyInput(this, (String)target1ComboBox.getSelectedItem(), type1, position1, alert ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE) &&
          modificationsCard.verifyInput(this, TARGETS[1].equals(target2ComboBox.getSelectedItem()) ? (String)target1ComboBox.getSelectedItem() :
                                        (String)target2ComboBox.getSelectedItem(), type2, position2, alert ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE))) {
      return false;
    }


    if (type1.matches(".*\\(.\\)")) {
      type1 = type1.substring(type1.length()-2, type1.length()-1);
    }
    if (type2.matches(".*\\(.\\)")) {
      type2 = type2.substring(type2.length()-2, type2.length()-1);
    }
    for (int i = 0; i < inputTable.getRowCount(); i++) {
      if (skip.contains(i)) {
        continue;
      }
      
      boolean new1old1 = true;
      boolean new2old2 = true;

      // Když se jedná o různé proteiny, nemůže nastat kolize
      if (!(target1ComboBox.getSelectedItem().equals(inputTable.getValueAt(i, 0)) ||
            target1ComboBox.getSelectedItem().equals(TARGETS[0]) || TARGETS[0].equals(inputTable.getValueAt(i, 0)))) {
        new1old1 = false;
      }
      if (!(target2ComboBox.getSelectedItem().equals(inputTable.getValueAt(i, 3)) ||
            target2ComboBox.getSelectedItem().equals(TARGETS[0]) || TARGETS[0].equals(inputTable.getValueAt(i, 3)) ||
            (TARGETS[1].equals(inputTable.getValueAt(i, 3)) &&
             (target2ComboBox.getSelectedItem().equals(inputTable.getValueAt(i, 0)) || target2ComboBox.getSelectedItem().equals(target1ComboBox.getSelectedItem()) ||
              (target1ComboBox.getSelectedItem().equals(TARGETS[0]) && TARGETS[0].equals(inputTable.getValueAt(i, 0))))) ||
            (TARGETS[1].equals(target2ComboBox.getSelectedItem()) &&
             (target1ComboBox.getSelectedItem().equals(inputTable.getValueAt(i, 3)) || inputTable.getValueAt(i, 0).equals(inputTable.getValueAt(i, 3)) ||
              (target1ComboBox.getSelectedItem().equals(TARGETS[0]) && TARGETS[0].equals(inputTable.getValueAt(i, 0))))))) {
        new2old2 = false;
      }

      // Když jde o odlišné pozice, tak nás to netrápí
      if (!(position1.equals(POSITIONS[0]) || inputTable.getValueAt(i, 2).equals(POSITIONS[0]) ||
            ((String)inputTable.getValueAt(i, 2)).matches("(^|[^0-9])" + position1 + "($|[^0-9])"))) {
        new1old1 = false;
      }
      if (!(position2.equals(POSITIONS[0]) || inputTable.getValueAt(i, 5).equals(POSITIONS[0]) ||
            ((String)inputTable.getValueAt(i, 5)).matches("(^|[^0-9])" + position2 + "($|[^0-9])"))) {
        new2old2 = false;
      }

      // Když se ptáme na jiné písmeno, nemůže nastat kolize
      boolean intersect = false;
      for (char t1 : Monomer.resolveShortcut(type1.charAt(0)).toCharArray()) {
        for (char t2 : Monomer.resolveShortcut(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2)).toCharArray()) {
          if (t1 == t2) {
            intersect = true;
            break;
          }
        }
      }
      if (!intersect) {
        new1old1 = false;
      } else {
        intersect = false;
      }
      for (char t1 : Monomer.resolveShortcut(type2.charAt(0)).toCharArray()) {
        for (char t2 : Monomer.resolveShortcut(inputTable.getValueAt(i, 4).toString().charAt(inputTable.getValueAt(i, 4).toString().length()-2)).toCharArray()) {
          if (t1 == t2) {
            intersect = true;
            break;
          }
        }
      }
      if (!intersect) {
        new2old2 = false;
      }

      // Opakované vkládání můstku
      if (((new1old1 && new2old2) /* || (new1old2 && new2old1) */) && ((String)inputTable.getValueAt(i, 6)).split(" \\(")[0].equals(bondsComboBox.getSelectedItem())) {
        if (alert) {
          alertCollision("This bond has already been inserted on this position", i, type1, position1, type2, position2);
        }
        return false;
      }
    }
    return true;
  }

  private boolean verifyType(JComboBox typeComboBox) {
    String inp = ((JTextField)typeComboBox.getEditor().getEditorComponent()).getText().toUpperCase();
    for (int i = 0; i < typeComboBox.getItemCount(); i++) {
      String string = ((String)typeComboBox.getItemAt(i));
      if (string.toUpperCase().equals(inp)) {
        return true;
      }
      if (inp.startsWith(string.toUpperCase().replaceFirst(" \\(.\\)", "")) || (string.toUpperCase().startsWith(inp) && inp.length() > 1)) {
        return true;
      }
    }
    for (Character shortcut : Monomer.getShortcuts()) {
      inp = inp.replace(shortcut.toString(), Monomer.resolveShortcut(shortcut));
    }
    TreeSet<Character> chars = new TreeSet<>();
    for (int j = 0; j < inp.length(); j++) {
      for (int i = 0; i < typeComboBox.getItemCount(); i++) {
        if (((String)typeComboBox.getItemAt(i)).matches(".*\\(.\\)") &&
            inp.charAt(j) == ((String)typeComboBox.getItemAt(i)).charAt(((String)typeComboBox.getItemAt(i)).length()-2)) {
          chars.add(inp.charAt(j));
          break;
        }
      }
    }
    if (chars.isEmpty()) {
      return false;
    }
    if (chars.size() == 1) {
      char c = chars.iterator().next();
      for (String string : TYPES) {
        if (string.contains("(" + c + ")")) {
          return true;
        }
      }
    } else {
      return true;
    }
    return false;
  }

  private boolean verifyPosition(JComboBox positionComboBox) {
    String inp = ((JTextField)positionComboBox.getEditor().getEditorComponent()).getText();
    for (int i = 0; i < positionComboBox.getItemCount(); i++) {
      if (inp.equals(positionComboBox.getItemAt(i))) {
        return true;
      }
    }
    if ((inp.contains("All") || inp.contains("*"))) {
      return true;
    }
    TreeSet<String> valid = new TreeSet<>(new Comparator<String>() {
      public int compare(String o1, String o2) {
        return Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2));
      }
    });
    String[] parts = inp.replaceAll("[^0-9]+", " ").replaceAll(" 0+", " ").replaceFirst("^0+", "").split(" +");
    for (String string : parts) {
      for (int i = 1; i < positionComboBox.getItemCount(); i++) {
        if (string.equals(positionComboBox.getItemAt(i).toString())) {
          valid.add(string);
          break;
        }
      }
    }
    if (valid.isEmpty()) {
      return false;
    }
    return true;
  }

  private boolean formatType(JComboBox typeComboBox) {
    String inp = ((JTextField)typeComboBox.getEditor().getEditorComponent()).getText().toUpperCase();
    for (int i = 0; i < typeComboBox.getItemCount(); i++) {
      String string = ((String)typeComboBox.getItemAt(i));
      if (string.toUpperCase().equals(inp)) {
        if (i != typeComboBox.getSelectedIndex()) {
          typeComboBox.setSelectedIndex(i);
          return false;
        }
        return true;
      }
      if (inp.startsWith(string.toUpperCase().replaceFirst(" \\(.\\)", "")) || (string.toUpperCase().startsWith(inp) && inp.length() > 1)) {
        typeComboBox.setSelectedItem(string);
        return true;
      }
    }
    TreeSet<Character> chars = new TreeSet<>(new Comparator() {
      public int compare(Object o1, Object o2) {
        return compareTypes(o1.toString(), o2.toString());
      }
    });
    for (int j = 0; j < inp.length(); j++) {
      for (int i = 0; i < typeComboBox.getItemCount(); i++) {
        if (((String)typeComboBox.getItemAt(i)).matches(".*\\(.\\)") &&
            inp.charAt(j) == ((String)typeComboBox.getItemAt(i)).charAt(((String)typeComboBox.getItemAt(i)).length()-2)) {
          chars.add(inp.charAt(j));
          break;
        }
      }
    }
    if (chars.isEmpty()) {
      ((JTextField)typeComboBox.getEditor().getEditorComponent()).requestFocus();
      return false;
    }
    if (chars.size() == 1) {
      char c = chars.iterator().next();
      for (String string : TYPES) {
        if (string.contains("(" + c + ")")) {
          typeComboBox.setSelectedItem(string);
        }
      }
    } else {
      String tmp = chars.toString().substring(1, chars.toString().length()-1);
      if (tmp.equals(typeComboBox.getSelectedItem())) {
        return true;
      }
      typeComboBox.setSelectedItem(tmp);
      return true;
    }
    ((JTextField)typeComboBox.getEditor().getEditorComponent()).requestFocus();
    return false;
  }

  private void formatPosition(JComboBox positionComboBox) {
    String inp = ((JTextField)positionComboBox.getEditor().getEditorComponent()).getText();
    for (int i = 0; i < positionComboBox.getItemCount(); i++) {
      if (inp.equals(positionComboBox.getItemAt(i))) {
        return;
      }
    }
    if ((inp.contains("All") || inp.contains("*"))) {
      positionComboBox.setSelectedIndex(0);
      return;
    }
    TreeSet<String> valid = new TreeSet<>(new Comparator<String>() {
      public int compare(String o1, String o2) {
        return Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2));
      }
    });
    String[] parts = inp.replaceAll("[^0-9]+", " ").replaceAll(" 0+", " ").replaceFirst("^0+", "").split(" +");
    for (String string : parts) {
      for (int i = 1; i < positionComboBox.getItemCount(); i++) {
        if (string.equals(positionComboBox.getItemAt(i).toString())) {
          valid.add(string);
          break;
        }
      }
    }
    if (valid.isEmpty()) {
      ((JTextField)positionComboBox.getEditor().getEditorComponent()).requestFocus();
      return;
    }
    if (valid.toString().substring(1, valid.toString().length()-1).equals(inp)) {
      return;
    }
    positionComboBox.setSelectedItem(valid.toString().substring(1, valid.toString().length()-1));
  }

  private String longer(String str) {
    for (int i = 0; i < TYPES.length; i++) {
      if (TYPES[i].contains("(" + str + ")")) {
        return TYPES[i];
      }
    }
    return str;
  }

  private class TargetListener implements ActionListener {
    private JComboBox targetComboBox;
    private JComboBox typeComboBox;

    public TargetListener(JComboBox targetComboBox, JComboBox typeComboBox) {
      this.targetComboBox = targetComboBox;
      this.typeComboBox = typeComboBox;
    }

    public void actionPerformed(ActionEvent e) {
      if (targetComboBox.getSelectedItem() == null) {
        return;
      }

      Object oldType = typeComboBox.getSelectedItem();
      typeComboBox.setModel(new DefaultComboBoxModel<>(TYPES));
      HashSet<Character> chars;
      JComboBox target;
      if (TARGETS[1].equals(targetComboBox.getSelectedItem())) {
        target = target1ComboBox;
      } else {
        target = targetComboBox;
      }
      if (target.getSelectedItem().equals(TARGETS[0])) {
        chars = new HashSet();
        for (Protein protein : proteins.values()) {
          chars.addAll(protein.domain());
        }
      } else {
        chars = proteins.get(target.getSelectedItem()).domain();
      }
      for (int i = 2; i < TYPES.length; i++) {
        String string = TYPES[i];
        if (string.matches(".*\\(.\\)")) {
          boolean contains = false;
          for (char c : Monomer.resolveShortcut(string.charAt(string.length()-2)).toCharArray()) {
            if (chars.contains(c)) {
              contains = true;
              break;
            }
          }
          if (!contains) {
            typeComboBox.removeItem(string);
          }
        }
      }

      typeComboBox.setSelectedIndex(typeComboBox.getItemCount() == 0 ? -1 : 0);
      typeComboBox.setSelectedItem(oldType);
    }
  }

  private class TypeListener implements ActionListener {
    private JComboBox targetComboBox;
    private JComboBox typeComboBox;
    private JComboBox positionComboBox;

    public TypeListener(JComboBox targetComboBox, JComboBox typeComboBox, JComboBox positionComboBox) {
      this.targetComboBox = targetComboBox;
      this.typeComboBox = typeComboBox;
      this.positionComboBox = positionComboBox;
    }

    public void actionPerformed(ActionEvent e) {
      if (typeComboBox.getSelectedItem() == null) {
        return;
      }

      // Validace vstupu, případná úprava InputVerifier je problém použít, kvůli případnému formátování
      if (typeComboBox.getSelectedIndex() < 0 && !formatType(typeComboBox)) {
        return;
      }

      Object oldPosition = positionComboBox.getSelectedItem();
      positionComboBox.setModel(new DefaultComboBoxModel<>(POSITIONS));
      JComboBox target;
      if (TARGETS[1].equals(targetComboBox.getSelectedItem())) {
        target = target1ComboBox;
      } else {
        target = targetComboBox;
      }
      String type = (String)typeComboBox.getSelectedItem();
      char c;
      if (proteins.containsKey(target.getSelectedItem()) && (type).matches(".*\\([A-Z]\\)") && Monomer.isMonomer(c = type.charAt(type.length()-2))) { // Lokální modifikace
        positionComboBox.setEditable(true);
        positionComboBox.setEnabled(true);
        for (Integer integer : proteins.get(target.getSelectedItem()).positions(c)) {
          positionComboBox.addItem(String.valueOf(integer+proteins.get(target.getSelectedItem()).start()));
        }

        if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == null) {
          positionComboBox.requestFocus();
        }
      } else { // Jsou možné jen globální pozice
        positionComboBox.setEditable(false);
        positionComboBox.setEnabled(false);
      }

      Object oldBond = bondsComboBox.getSelectedItem();
      bondsComboBox.setEditable(false);
      bondsComboBox.setModel(new DefaultComboBoxModel<>(bonds.keySet().toArray()));
      specificCheckBox.setEnabled(true);
      if (specificCheckBox.isSelected()) {
        filterBonds();
      }

      bondsComboBox.setSelectedIndex(bondsComboBox.getItemCount() > 0 ? 0 : -1);
      if (!(oldBond == null || oldBond.toString().isEmpty())) {
        bondsComboBox.setSelectedItem(oldBond);
      }
      positionComboBox.setSelectedIndex(positionComboBox.getItemCount() > 0 ? 0 : -1);
      positionComboBox.setSelectedItem(oldPosition);
    }
  }

  private class TargetComparator implements Comparator<String> {
    private JComboBox jComboBox;

    public TargetComparator(JComboBox jComboBox) {
      this.jComboBox = jComboBox;
    }

    public int compare(String o1, String o2) {
      if (o1.equals(o2)) {
        return 0;
      }
      for (int i = 0; i < jComboBox.getItemCount(); i++) {
        if (o1.equals(jComboBox.getItemAt(i))) {
          return -1;
        }
        if (o2.equals(jComboBox.getItemAt(i))) {
          return 1;
        }
      }
      return o1.compareTo(o2);
    }
  }

  private class TypeComparator implements Comparator<String> {
    public int compare(String o1, String o2) {
      if (o1.equals(o2)) {
        return 0;
      }
      return compareTypes(o1, o2);
    }
  }

  private class PositionComparator implements Comparator<String> {
    public int compare(String o1, String o2) {
      if (o1.equals(o2)) {
        return 0;
      }
      if (o1.equals(POSITIONS[0])) {
        return -1;
      }
      if (o2.equals(POSITIONS[0])) {
        return 1;
      }
      if (!o1.contains(",")) {
        if (o2.contains(",")) {
          if (o2.matches("(^|(.*, ))" + o1 + "($|(, .*))")) {
            return -1;
          }
          return Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2.split(", ", 2)[0]));
        } else {
          return Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2));
        }
      }
      if (!o2.contains(",")) {
        if (o1.matches("(^|(.*, ))" + o2 + "($|(, .*))")) {
          return 1;
        }
        return Integer.compare(Integer.parseInt(o1.split(", ", 2)[0]), Integer.parseInt(o2));
      }
      String[] s1 = o1.split(", ");
      String[] s2 = o2.split(", ");
      for (int i = 0; i < Math.min(s1.length, s2.length); i++) {
        if (!s1[i].equals(s2[i])) {
          return Integer.compare(Integer.parseInt(s1[i]), Integer.parseInt(s2[i]));
        }
      }
      return Integer.compare(s1.length, s2.length);
    }
  }

  private int compareTypes(String s1, String s2) {
    int v1 = -1;
    int v2 = -1;
    for (int i = 0; i < TYPES.length; i++) {
      if (TYPES[i].equals(s1)) {
        v1 = i;
      }
      if (TYPES[i].equals(s2)) {
        v2 = i;
      }
    }
    if (v1 >= 0 && v2 >= 0) {
      return Integer.signum(v1-v2);
    }
    if (v1 >= 0) {
      return -1;
    }
    if (v2 >= 0) {
      return 1;
    }
    return s1.compareTo(s2);
  }

  private void alertCollision(String message, int i, String type1, String position1, String type2, String position2) {
    JOptionPane.showMessageDialog(this, message + ":" + System.lineSeparator() + "(" + (i+1) + ") " + inputTable.getValueAt(i, 0) + " - " +
            inputTable.getValueAt(i, 1) + " - " + inputTable.getValueAt(i, 2) + " - "+ inputTable.getValueAt(i, 3) + " - " + inputTable.getValueAt(i, 4) +
            " - " + inputTable.getValueAt(i, 5) + " - "+ inputTable.getValueAt(i, 6) + System.lineSeparator() + "vs. " +
            target1ComboBox.getSelectedItem() + " - " + longer(type1) + " - " + position1 + " - " + target2ComboBox.getSelectedItem() +
            " - " + longer(type2) + " - " + position2 + " - " + bondsComboBox.getSelectedItem(), "Conflict", JOptionPane.WARNING_MESSAGE);
  }
}
