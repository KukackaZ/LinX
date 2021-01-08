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
public class ModificationsCard extends Card {
  private static String[] TARGETS = new String[] { "All proteins" };
  private static String[] TYPES;
  private static String MUTATION = "Mutation";
  private static String[] POSITIONS = new String[] { "All" };
  private static String[] PRESENCES = new String[] { "Fixed", "Variable" };
  private static String TEMPLATE = "OR Select template";
  private static String TEMPLATES_FILE = "modifications.template";
  private static String MODS_SEPARATOR = "\n";
  private static String MOD_SEPARATOR = "\t";

  private TreeMap<String, ModificationAbstract> modifications;
  private TreeMap<String, ArrayList<String[]>> templates;
  private LinkedHashMap<String, Protein> proteins;
  private int levels;
  private ArrayList<Collection<Protease>> proteases;
  private JComboBox targetComboBox;
  private JComboBox typeComboBox;
  private JComboBox positionComboBox;
  private JComboBox presenceComboBox;
  private JComboBox modificationsComboBox;
  private JCheckBox specificCheckBox;
  private JComboBox templatesComboBox;
  private JTable inputTable;
  private TreeMap<String, ModificationAbstract> backup;
  private ArrayList<Object[]> edited;

  public ModificationsCard(String id, JButton[] movement) {
    super(id, movement);
    ChemicalElement.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        reloadMods();
      }
    });
    loadMods();
    {
      ArrayList<String> names = Monomer.getNames();
      names.add(MUTATION);
      TYPES = new String[names.size()];
      TYPES = names.toArray(TYPES);
    }
    proteins = new LinkedHashMap<>(1);
    proteases = new ArrayList<>(0);
    JPanel mainPanel = new JPanel();
    targetComboBox = new JComboBox(new String[] { TARGETS[0], "Protein", "Peptides" });
    typeComboBox = new JComboBox(TYPES);
    positionComboBox = new JComboBox(POSITIONS);
    presenceComboBox = new JComboBox(PRESENCES);
    modificationsComboBox = new JComboBox(modifications.keySet().toArray());
    specificCheckBox = new JCheckBox("Specific", true);
    templatesComboBox = new JComboBox();
    JButton insertButton = new JButton("Insert");
    inputTable = new JTable(new javax.swing.table.DefaultTableModel(new Object [][] { { null, null, null, null, null } },
                                                                    new String [] { "Target", "Types", "Positions", "Presence", "Modification" }) {
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
    JButton cleanButton = new JButton("Clean form");
    JButton removeButton = new JButton("Remove selected");
    JButton editButton = new JButton("Edit selected");

    mainScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    mainScrollPane.setViewportView(mainPanel);

    proteins.put("Protein", new Protein("Protein", ""));
    levels = 1;

    targetComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Object oldType = typeComboBox.getSelectedItem();
        Object oldMod = modificationsComboBox.getSelectedItem();
        int oldModP = modificationsComboBox.getSelectedIndex();
        typeComboBox.setModel(new DefaultComboBoxModel<>(TYPES));
        // Filtrování podle toho, co se skutečně vyskytuje je možná zbytečné, když se mutace přidávají až tady...
        HashSet<Character> chars = new HashSet<>(20);
        if (proteins.containsKey(targetComboBox.getSelectedItem())) {
          chars = proteins.get(targetComboBox.getSelectedItem()).domain();
        } else {
          typeComboBox.removeItemAt(TYPES.length-1);
          for (Protein protein : proteins.values()) {
            chars.addAll(protein.domain());
          }
        }
        for (int i = 2; i < TYPES.length-1; i++) {
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

        typeComboBox.setSelectedIndex(0);
        typeComboBox.setSelectedItem(oldType);
        modificationsComboBox.setSelectedIndex(modificationsComboBox.getItemCount() > 0 ? 0 : -1);
        modificationsComboBox.setSelectedItem(oldMod);
        if (modificationsComboBox.getSelectedIndex() < 0 && oldModP >= 0) {
          modificationsComboBox.setSelectedIndex(modificationsComboBox.getItemCount() > 0 ? 0 : -1);
        }
        typeComboBox.requestFocus();
      }
    });

    typeComboBox.setEditable(true);
    typeComboBox.setMaximumRowCount(25);
    typeComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        typeActionListener();
      }
    });
    ((JTextField)typeComboBox.getEditor().getEditorComponent()).setInputVerifier(new InputVerifier() {
      public boolean verify(JComponent input) {
        return verifyType(typeComboBox);
      }
    });
    ((JTextField)typeComboBox.getEditor().getEditorComponent()).addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        ((JTextField)typeComboBox.getEditor().getEditorComponent()).selectAll();
      }
    });

    positionComboBox.setEditable(true);
    positionComboBox.setMaximumRowCount(20);
    positionComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (typeComboBox.getSelectedItem().equals(MUTATION)) {
          Object oldMod = modificationsComboBox.getSelectedItem();
          modificationsComboBox.setModel(new DefaultComboBoxModel<>(TYPES));
          modificationsComboBox.removeItemAt(TYPES.length-1);
          modificationsComboBox.removeItemAt(1);
          modificationsComboBox.removeItemAt(0);
          for (int i = modificationsComboBox.getItemCount()-1; i >=0; i--) {
            boolean remove = true;
            String item = modificationsComboBox.getItemAt(i).toString();
            for (Character c : Monomer.resolveShortcut(item.charAt(item.length()-2)).toCharArray()) {
              if (!positionComboBox.getSelectedItem().toString().contains(c.toString())) {
                remove = false;
                break;
              }
            }
            if (remove) {
              modificationsComboBox.removeItemAt(i);
            }
          }
          if (modificationsComboBox.getItemCount() > 0) {
            modificationsComboBox.setSelectedIndex(0);
          }
          if (oldMod != null) {
            modificationsComboBox.setSelectedItem(oldMod);
          }
        } else if (positionComboBox.getSelectedIndex() < 0) {
          formatPosition();
        }
      }
    });
    ((JTextField)positionComboBox.getEditor().getEditorComponent()).setInputVerifier(new InputVerifier() {
      public boolean verify(JComponent input) {
        return verifyPosition();
      }
    });
    ((JTextField)positionComboBox.getEditor().getEditorComponent()).addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        ((JTextField)positionComboBox.getEditor().getEditorComponent()).selectAll();
      }
    });

    modificationsComboBox.setMaximumRowCount(20);
    modificationsComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (modificationsComboBox.getSelectedIndex() < 0 && modificationsComboBox.getSelectedItem() != null) {
          formatType(modificationsComboBox);
        }
      }
    });
    ((JTextField)modificationsComboBox.getEditor().getEditorComponent()).setInputVerifier(new InputVerifier() {
      public boolean verify(JComponent input) {
        return verifyType(modificationsComboBox);
      }
    });

    cleanInput();

    specificCheckBox.setMnemonic('F');
    specificCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Object oldMod = modificationsComboBox.getSelectedItem();
        if (specificCheckBox.isSelected()) {
          filterModifications();
        } else {
          modificationsComboBox.setModel(new DefaultComboBoxModel<>(modifications.keySet().toArray()));
        }
        if (modificationsComboBox.getItemCount() > 0) {
          modificationsComboBox.setSelectedIndex(0);
        }
        modificationsComboBox.setSelectedItem(oldMod);
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
    trs.setComparator(0, new Comparator<String>() {
      public int compare(String o1, String o2) {
        if (o1.equals(o2)) {
          return 0;
        }
        for (int i = 0; i < targetComboBox.getItemCount(); i++) {
          if (o1.equals(targetComboBox.getItemAt(i))) {
            return -1;
          }
          if (o2.equals(targetComboBox.getItemAt(i))) {
            return 1;
          }
        }
        return o1.compareTo(o2);
      }
    });
    trs.setComparator(1, new Comparator<String>() {
      public int compare(String o1, String o2) {
        if (o1.equals(o2)) {
          return 0;
        }
        if (o1.equals(MUTATION)) {
          return -1;
        }
        if (o2.equals(MUTATION)) {
          return 1;
        }
        return compareTypes(o1, o2);
      }
    });
    trs.setComparator(2, new Comparator<String>() {
      public int compare(String o1, String o2) {
        if (o1.equals(o2)) {
          return 0;
        }
        if (o1.equals(POSITIONS[0])) {
          return o2.contains("(") ? 1 : -1;
        }
        if (o2.equals(POSITIONS[0])) {
          return o1.contains("(") ? -1 : 1;
        }
        if (o1.contains("(") && !o2.contains("(")) {
          return -1;
        }
        if (!o1.contains("(") && o2.contains("(")) {
          return 1;
        }
        if (o1.contains("(") /* && o2.contains("(") */) {
          return Integer.compare(Integer.parseInt(o1.split(" ", 2)[0]), Integer.parseInt(o2.split(" ", 2)[0]));
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
    });
    trs.setComparator(4, new Comparator<String>() {
      public int compare(String o1, String o2) {
        if (modifications.containsKey(o1)) {
          if (modifications.containsKey(o2)) {
            return o1.compareTo(o2);
          }
          return 1;
        }
        if (modifications.containsKey(o2)) {
          return -1;
        }
        return compareTypes(o1, o2);
      }
    });
    trs.setSortsOnUpdates(true);
    inputTable.setRowSorter(trs);
    inputTable.getTableHeader().setReorderingAllowed(false);
    inputTable.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if ((inputTable.getRowCount() > 1 || inputTable.getValueAt(0, 0) != null) && inputTable.getSelectedRowCount() > 0) {
          if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU && inputTable.getSelectedRowCount() > 0) {
            inputTable.scrollRectToVisible(inputTable.getCellRect(inputTable.getSelectedRow(), 0, true));
            java.awt.Rectangle a = inputTable.getCellRect(inputTable.getSelectedRow(), 0, true);
            java.awt.Rectangle b = inputTable.getCellRect(inputTable.getSelectedRow(), 4, true);
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
    inputScrollPane.setViewportView(inputTable);

    GroupLayout layout = new GroupLayout(mainPanel);
    mainPanel.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addComponent(inputScrollPane)
          .addGroup(layout.createSequentialGroup()
            .addComponent(targetComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(typeComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(positionComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(presenceComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(modificationsComboBox, 150, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
          .addComponent(targetComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(typeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(positionComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(presenceComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(modificationsComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(specificCheckBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(templatesComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(insertButton))
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

  public ArrayList<String> logModifications() {
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

  public String logModificationsShort() {
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
         .append(inputTable.getValueAt(i, 2)).append(" ➜ ")
         .append(inputTable.getValueAt(i, 3).toString().substring(0, 3)).append(" - ")
         .append(inputTable.getValueAt(i, 4).toString().substring(0, inputTable.getValueAt(i, 4).toString().lastIndexOf('(')-1));
    }
    return ret.toString();
  }

  public LinkedHashMap<String, Protein> getProteins() {
    return proteins;
  }

  public void setProteins(LinkedHashMap<String, Protein> proteins) {
    // K čemu tu tohle bylo??? Aby se porovnaly konfigurace bez přidaných mutací.
//    for (Protein protein : proteins.values()) {
//      protein.reset();
//    }
    HashMap<String, String> dict = new HashMap<>(this.proteins.size());
    dict.put((String)targetComboBox.getItemAt(0), (String)targetComboBox.getItemAt(0));
    for (Protein proteinOld : this.proteins.values()) {
      //proteinOld.reset();
      for (Protein proteinNew : proteins.values()) {
        if (proteinOld.getProtein().equals(proteinNew.getProtein()) &&
            (!dict.containsKey(proteinOld.getName()) || proteinOld.getName().equals(proteinNew.getName()))) {
          dict.put(proteinOld.getName(), proteinNew.getName());
        }
      }
    }
    // Peptides...
    for (int i = this.proteins.size() + 1; i < targetComboBox.getItemCount(); i++) {
      dict.put((String)targetComboBox.getItemAt(i), (String)targetComboBox.getItemAt(i));
    }

    edited.clear();
    for (int i = inputTable.getRowCount()-1; i >= 0; i--) {
      if (dict.containsKey(inputTable.getValueAt(i, 0))) {
        Protein oldProtein = this.proteins.get(inputTable.getValueAt(i, 0));
        Protein newProtein = proteins.get(dict.get(inputTable.getValueAt(i, 0)));
        if (!POSITIONS[0].equals(inputTable.getValueAt(i, 2)) && oldProtein != null && newProtein != null && oldProtein.getShift()!= newProtein.getShift()) {
          inputTable.setValueAt(Integer.parseInt((String)inputTable.getValueAt(i, 2)) - oldProtein.getShift() + newProtein.getShift()+ "", i, 2);
        }
        inputTable.setValueAt(dict.get(inputTable.getValueAt(i, 0)), i, 0);
        if (inputTable.getValueAt(i, 1).equals(MUTATION)) {
          int index = Integer.parseInt((String)inputTable.getValueAt(i, 2)) - newProtein.start();
          if (index >= 0 && index < newProtein.length()) {
            for (String string : ((String)inputTable.getValueAt(i, 4)).split(", ")) {
              newProtein.addMutation(index, string.charAt(0));
            }
          }
        }
      } else {
        ((DefaultTableModel)inputTable.getModel()).removeRow(i);
      }
    }
    if (inputTable.getRowCount() == 0) {
      ((DefaultTableModel)inputTable.getModel()).addRow(new Object[] { null, null, null, null, null });
    }

    for (String protein : this.proteins.keySet()) {
      targetComboBox.removeItem(protein);
    }
    int i = 1;
    for (String name : proteins.keySet()) {
      targetComboBox.insertItemAt(name, i++);
    }
    this.proteins = proteins;
    
    // Validation - pozice mimo výřez; aminokyselina se ve výřezu nevyskytuje...
    ArrayList<String> modifications = logModifications();
    clean();
    setModifications(modifications);
  }

  private void setDigest(int i) {
    if (i <= 0) {
      throw new IllegalArgumentException("It must contains at least one level.");
    }
    if (i > levels) {
      if (levels == 1) {
        for (int j = 0; j < inputTable.getRowCount(); j++) {
          if (targetComboBox.getItemAt(proteins.size()).equals(inputTable.getValueAt(j, 0))) {
            inputTable.setValueAt("Peptides in level no.1", j, 0);
          }
        }
        targetComboBox.removeItemAt(proteins.size()+1);
        levels = 0;
      }
      for (int j = levels; j < i; j++) {
        targetComboBox.addItem("Peptides in level no." + (j+1));
      }
    } else if (i < levels) {
      for (int j = proteins.size() + levels; j > proteins.size() + i; j--) {
        for (int k = 0; k < inputTable.getRowCount(); k++) {
          if (targetComboBox.getItemAt(j).equals(inputTable.getValueAt(k, 0))) {
            ((DefaultTableModel)inputTable.getModel()).removeRow(k);
          }
        }
        targetComboBox.removeItemAt(j);
      }
      if (i == 1) {
        for (int j = 0; j < inputTable.getRowCount(); j++) {
          if (targetComboBox.getItemAt(proteins.size()).equals(inputTable.getValueAt(j, 0))) {
            inputTable.setValueAt("Peptides", j, 0);
          }
        }
        targetComboBox.removeItemAt(proteins.size()+1);
        targetComboBox.addItem("Peptides");
      }
    }
    levels = i;
  }

  public void setProteases(ArrayList<Collection<Protease>> proteases) {
    setDigest(proteases.isEmpty() ? 1 : proteases.size());
    this.proteases = proteases;
  }

  public void setModifications(ArrayList<String> modifications) {
    edited.clear();
    boolean selected = specificCheckBox.isSelected();
    specificCheckBox.setSelected(false);
    // Mutace
    for (String string : modifications) {
      try {
        String[] parts = string.split("\\t");
        if (parts[1].equals(MUTATION)) {
          targetComboBox.setSelectedItem(parts[0]);
          typeComboBox.setSelectedItem(parts[1]);
          positionComboBox.setSelectedIndex(Integer.parseInt(parts[2])-1);
          modificationsComboBox.setSelectedItem(parts[4]);
          insert(false);
        }
      } catch (Exception e) { }
    }
    // Modifikace
    for (String string : modifications) {
      try {
        String[] parts = string.split("\\t");
        if (!parts[1].equals(MUTATION)) {
          targetComboBox.setSelectedItem(parts[0]);
          typeComboBox.setSelectedItem(parts[1]);
          positionComboBox.setSelectedItem(parts[2]);
          presenceComboBox.setSelectedItem(parts[3]);
          String tmp = parts[4].substring(0, parts[4].lastIndexOf(" ("));
          modificationsComboBox.setSelectedItem(tmp);
          if (targetComboBox.getSelectedItem().equals(parts[0]) && typeComboBox.getSelectedItem().equals(parts[1]) && verifyType(typeComboBox) &&
              positionComboBox.getSelectedItem().equals(parts[2]) && verifyPosition() && presenceComboBox.getSelectedItem().equals(parts[3]) &&
              modificationsComboBox.getSelectedItem().equals(tmp)) {
            insert(false);
          }
        }
      } catch (Exception e) { }
    }
    specificCheckBox.setSelected(selected);
    cleanInput();
  }

  public boolean check() {
    if (inputTable.getRowCount() == 1 && (inputTable.getValueAt(0, 0) == null || inputTable.getValueAt(0, 0).toString().isEmpty())) {
      return true;
    }
    if (!edited.isEmpty() && edited.get(0)[1].equals(MUTATION)) {
      for (int i = 0; i < inputTable.getRowCount(); i++) {
        boolean match = true; // Mohla být mezi tím smazána
        for (int j = 0; j < inputTable.getColumnCount(); j++) {
          if (!edited.get(0)[j].equals(inputTable.getValueAt(i, j))) {
            match = false;
            break;
          }
        }
        if (match) {
          String[] aas = edited.get(0)[4].toString().split(", ");
          Protein protein = proteins.get(edited.get(0)[0]);
          int pos = Integer.parseInt(edited.get(0)[2].toString()) - protein.start();
          for (String aa : aas) {
            protein.addMutation(pos, aa.charAt(0));
          }
          break;
        }
      }
      edited.clear();
    }
    for (int i = 0; i < inputTable.getRowCount(); i++) {
      if (!inputTable.getValueAt(i, 4).toString().contains("(")) {
        continue;
      }
      boolean isnt = true;
      for (char aa : Monomer.resolveShortcut(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2)).toCharArray()) {
        if (inputTable.getValueAt(i, 2).toString().equals(POSITIONS[0])) { // Position = All
          if (proteins.containsKey(inputTable.getValueAt(i, 0))) { // Target = {one protein}
            if (!proteins.get(inputTable.getValueAt(i, 0)).positions(aa).isEmpty()) {
              isnt = false;
              break;
            }
          } else {
            for (Protein protein : proteins.values()) {
              if (!protein.positions(aa).isEmpty()) {
                isnt = false;
                break;
              }
            }
            if (!isnt) {
              break;
            }
          }
        } else {
          if (proteins.get(inputTable.getValueAt(i, 0)).charsAt(Integer.parseInt(inputTable.getValueAt(i, 2).toString())-proteins.get(inputTable.getValueAt(i, 0)).start())
                .contains(aa)) {
            isnt = false;
            break;
          }
        }
      }
      if (isnt) {
        JOptionPane.showMessageDialog(this, "There is unused modification." + System.lineSeparator() + inputTable.getValueAt(i, 0) + " - " +
            inputTable.getValueAt(i, 1) + " - " + inputTable.getValueAt(i, 2) + " - "+ inputTable.getValueAt(i, 3) + " - " + inputTable.getValueAt(i, 4) +
            System.lineSeparator() + "This can occur when modification is inserted on the mutated position and mutation is subsequently removed",
            "Invalid input", JOptionPane.WARNING_MESSAGE);
        return false;
      }
      //////////////
      String name = ((String)inputTable.getValueAt(i, 4));
      String mass = name.substring(name.lastIndexOf('(')+1, name.lastIndexOf(')')).split("/ ")[0];
      name = name.substring(0, name.lastIndexOf('(')-1);
      if (!modifications.containsKey(name)) {
        JOptionPane.showMessageDialog(this, "Modification '" + name + "' isn't in the list of modifications. That can occur, when you edit database of " +
                                            "modifications after insertion of modification." + System.lineSeparator() + inputTable.getValueAt(i, 0) + " - " +
                                            inputTable.getValueAt(i, 1) + " - " + inputTable.getValueAt(i, 2) + " - "+ inputTable.getValueAt(i, 3) + " - " +
                                            inputTable.getValueAt(i, 4), "Invalid input", JOptionPane.WARNING_MESSAGE);
          return false;
      }
      if (!mass.equals(Defaults.sMassShortFormat.format(modifications.get(name).getModification()))) {
        if (JOptionPane.showConfirmDialog(this, "Modification '" + name + "' has invalid mass. That can occur, when you edit database of elements after " +
                                                "insertion of modification." + System.lineSeparator() + inputTable.getValueAt(i, 0) + " - " +
                                                inputTable.getValueAt(i, 1) + " - " + inputTable.getValueAt(i, 2) + " - "+ inputTable.getValueAt(i, 3) + " - " +
                                                inputTable.getValueAt(i, 4) + System.lineSeparator() + System.lineSeparator() + "Do you wish to update mass?",
                                                "Invalid input", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
          inputTable.setValueAt(name + " (" + Defaults.sMassShortFormat.format(modifications.get(name).getModification()) + ")", i, 4);
        } else {
          return false;
        }
      }
    }
    release();
    return true;
  }

  public void release() {
    if (typeComboBox.getEditor().getEditorComponent().hasFocus() && !verifyType(typeComboBox)) {
      typeComboBox.setSelectedIndex(-1);
    }
    if (positionComboBox.getEditor().getEditorComponent().hasFocus() && !verifyPosition()) {
      positionComboBox.setSelectedIndex(-1);
    }
    if (modificationsComboBox.getEditor().getEditorComponent().hasFocus() && !verifyType(modificationsComboBox)) {
      modificationsComboBox.setSelectedIndex(-1);
    }
  }

  /**
   * Set inserted modifications into proteins.
   */
  public void applyModifications(boolean check) {
    if (inputTable.getRowCount() == 1 && inputTable.getValueAt(0, 0) == null) {
      return;
    }
    for (int i = 0; i < inputTable.getRowCount(); i++) if (!inputTable.getValueAt(i, 1).equals(MUTATION)) {
      ArrayList<String> names;
      if (proteins.containsKey(inputTable.getValueAt(i, 0))) {
        names = new ArrayList<>(1);
        names.add(inputTable.getValueAt(i, 0).toString());
      } else {
        names = new ArrayList<>(proteins.size());
        for (String name : proteins.keySet()) {
          names.add(name);
        }
      }
      int level;
      if (proteins.containsKey(inputTable.getValueAt(i, 0)) || TARGETS[0].equals(inputTable.getValueAt(i, 0))) {
        level = 0;
      } else if (targetComboBox.getItemCount() == names.size()+2) {
        level = 1;
      } else {
        level = Integer.parseInt(((String)inputTable.getValueAt(i, 0)).replaceFirst("^.*no\\.", ""));
      }
      String key = inputTable.getValueAt(i, 4).toString();
      String mod = key.substring(0, inputTable.getValueAt(i, 4).toString().lastIndexOf('(')-1);
      BigDecimal mass = backup.get(key).getModification();
      BigDecimal diff = check ? backup.get(key).getCheckDiff() : BigDecimal.ZERO;
      for (char aa : Monomer.resolveShortcut(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2)).toCharArray()) {
        for (String name : names) {
          if (inputTable.getValueAt(i, 2).equals(POSITIONS[0])) {
            ModificationReal mr = new ModificationReal(mod, mass, diff, name, aa, null);
            if (inputTable.getValueAt(i, 3).equals(PRESENCES[0])) {
              proteins.get(name).addFixedModification(mr, level);
            } else {
              proteins.get(name).addVariableModification(mr, level);
            }
          } else {
            int pos = Integer.parseInt(inputTable.getValueAt(i, 2).toString())-proteins.get(name).start();
            ModificationReal mr = new ModificationReal(mod, mass, diff, name, aa, pos);
            if (inputTable.getValueAt(i, 3).equals(PRESENCES[0])) {
              proteins.get(name).addFixedModification(mr, pos, level);
            } else {
              proteins.get(name).addVariableModification(mr, pos, level);
            }
          }
        }
      }
    }
  }

  private void loadMods() {
    modifications = new TreeMap();
    Properties file = Defaults.loadDefaults(ModificationAbstract.FILE);
    for (String key : file.stringPropertyNames()) {
      modifications.put(key, new ModificationAbstract(key, file.getProperty(key)));
    }
  }

  public void reloadMods() {
    loadMods();
    modificationsComboBox.setModel(new DefaultComboBoxModel<>(modifications.keySet().toArray()));
    if (specificCheckBox.isSelected()) {
      filterModifications();
    }
  }

  private void loadTemplates() {
    templates = new TreeMap();
    Properties file = Defaults.loadDefaults(TEMPLATES_FILE);
    for (String key : file.stringPropertyNames()) {
      String[] parts = file.getProperty(key).split(MODS_SEPARATOR);
      ArrayList<String[]> template = new ArrayList(parts.length);
      for (String string : parts) {
        String[] mod = string.split(MOD_SEPARATOR);
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

  protected boolean typeActionListener() {
    if (typeComboBox.getSelectedItem() == null) {
      return true;
    }
    // Validace vstupu, případná úprava InputVerifier je problém použít, kvůli případnému formátování
    if (typeComboBox.getSelectedIndex() < 0 && !formatType(typeComboBox)) {
      return true;
    }
    Object oldMod = modificationsComboBox.getSelectedItem();
    Object oldPosition = MUTATION.equals(typeComboBox.getSelectedItem()) ^ (positionComboBox.getSelectedItem() != null &&
                                                                             positionComboBox.getSelectedItem().toString().endsWith(")"))
                         ? POSITIONS[0] : positionComboBox.getSelectedItem();
    if (typeComboBox.getSelectedItem().equals(MUTATION)) { // Mutation
      positionComboBox.setEnabled(true);
      positionComboBox.setEditable(false);
      positionComboBox.setModel(new DefaultComboBoxModel<>());
      Protein protein = proteins.get(targetComboBox.getSelectedItem());
      for (int i = 0; i < protein.length(); i++) {
        positionComboBox.addItem((i+protein.start()) + " (" + protein.charsAt(i).toString().substring(1, protein.charsAt(i).toString().length()-1) + ")");
      }
      if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == null) {
        positionComboBox.requestFocusInWindow();
      }

      presenceComboBox.setSelectedIndex(1);
      presenceComboBox.setEnabled(false);

      if (modificationsComboBox.isEditable() == false) {
        oldMod = null;
      }
      modificationsComboBox.setEditable(true);
      modificationsComboBox.setModel(new DefaultComboBoxModel<>(TYPES));
      modificationsComboBox.removeItemAt(TYPES.length-1);
      modificationsComboBox.removeItemAt(1);
      modificationsComboBox.removeItemAt(0);
      specificCheckBox.setEnabled(false);
    } else {
      positionComboBox.setEditable(true);
      positionComboBox.setModel(new DefaultComboBoxModel<>(POSITIONS));
      String type = (String)typeComboBox.getSelectedItem();
      char c;
      if (proteins.containsKey(targetComboBox.getSelectedItem()) && (type).matches(".*\\([A-Z]\\)") && Monomer.isMonomer(c = type.charAt(type.length()-2))) {
        positionComboBox.setEnabled(true); // Jsou možné i lokální modifikace
        for (Integer integer : proteins.get(targetComboBox.getSelectedItem()).positions(c)) {
          positionComboBox.addItem(integer+proteins.get(targetComboBox.getSelectedItem()).start());
        }

        if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == null) {
          positionComboBox.requestFocusInWindow();
        }
      } else { // Jsou možné jen globální pozice
        positionComboBox.setEnabled(false);
      }

      presenceComboBox.setEnabled(true);

      modificationsComboBox.setEditable(false);
      modificationsComboBox.setModel(new DefaultComboBoxModel<>(modifications.keySet().toArray()));
      specificCheckBox.setEnabled(true);
      if (specificCheckBox.isSelected()) {
        filterModifications();
      }
    }
    modificationsComboBox.setSelectedIndex(modificationsComboBox.getItemCount() > 0 ? 0 : -1);
    if (!(oldMod == null || oldMod.toString().isEmpty())) {
      modificationsComboBox.setSelectedItem(oldMod);
    }
    positionComboBox.setSelectedItem(oldPosition);
    if (!(typeComboBox.getSelectedItem().equals(MUTATION) || verifyPosition())) {
      positionComboBox.setSelectedIndex(positionComboBox.getItemCount() > 0 ? 0 : -1);
    }
    // Hack kvůli minimální velikosti, neumí aktualizovat samo...
    if (SwingUtilities.getWindowAncestor(typeComboBox) != null) {
      java.awt.Window parent = SwingUtilities.getWindowAncestor(typeComboBox);
      parent.setMinimumSize(null);
      parent.setMinimumSize(parent.getMinimumSize());
    }
    return false;
  }

  private void filterModifications() {
    // Profiltrování podle specificity
    HashSet<Character> selected = new HashSet<>();
    if (((String)typeComboBox.getSelectedItem()).matches(".*\\(.\\)")) {
      selected.add(((String)typeComboBox.getSelectedItem()).charAt(((String)typeComboBox.getSelectedItem()).length()-2));
    } else {
      for (char c : ((String)typeComboBox.getSelectedItem()).replaceAll("[ ,]", "").toCharArray()) {
        selected.add(c);
      }
    }
    HashSet<Character> resolved = new HashSet();
    for (Character character : selected) {
      if (Monomer.isShortcut(character)) {
        for (char ch : Monomer.resolveShortcut(character).toCharArray()) {
          resolved.add(ch);
        }
      } else {
        resolved.add(character);
      }
    }
    for (String string : modifications.keySet()) {
      for (Character character : resolved) {
        if (!(modifications.get(string).specific(character))) {
          modificationsComboBox.removeItem(string);
          break;
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
    targetComboBox.setSelectedIndex(0);
    typeComboBox.setSelectedIndex(0);
    positionComboBox.setSelectedIndex(positionComboBox.getItemCount() == 0 ? -1 : 0);
    presenceComboBox.setSelectedIndex(1);
    modificationsComboBox.setSelectedIndex(modificationsComboBox.getItemCount() == 0 ? -1 : 0);
    targetComboBox.requestFocus();
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

  public void deleteSelected() {
    if (inputTable.getSelectedRowCount() == 0) {
      return;
    }
    for (int i : inputTable.getSelectedRows()) {
      if (MUTATION.equals(inputTable.getValueAt(i, 1))) {
        deleteMutation();
      }
    }
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

  private void deleteMutation() {
    if (inputTable.getSelectedRowCount() == 0) {
      return;
    }

    // Musí být před filtrováním, protože jeden symbol zůstane
    proteins.get(inputTable.getValueAt(inputTable.getSelectedRow(), 0)).deleteMutations(Integer.parseInt((String)inputTable.getValueAt(inputTable.getSelectedRow(), 2))
                                                                                        -proteins.get(inputTable.getValueAt(inputTable.getSelectedRow(), 0)).start());

    // Je-li vybrán ten protein, nebo peptidy
    HashSet<Character> aas = new HashSet<>(20);
    if (proteins.containsKey(inputTable.getValueAt(inputTable.getSelectedRow(), 0))) {
      aas = proteins.get(inputTable.getValueAt(inputTable.getSelectedRow(), 0)).domain();
    } else {
      for (Protein protein : proteins.values()) {
        if (protein.getName().equals(inputTable.getValueAt(inputTable.getSelectedRow(), 0))) {
          // Je vybrán jiný protein, tedy není nutné měnit
          return;
        }
        aas.addAll(protein.domain());
      }
    }
    for (String string : ((String)inputTable.getValueAt(inputTable.getSelectedRow(), 4)).split(", ")) {
      if (!aas.contains(string.charAt(0))) {
        typeComboBox.removeItem(longer(string));
      }
    }
    if (!MUTATION.equals(typeComboBox.getSelectedItem())) {
      typeActionListener();
      return;
    }
    Protein protein = proteins.get(inputTable.getValueAt(inputTable.getSelectedRow(), 0));
    int position = Integer.parseInt((String)inputTable.getValueAt(inputTable.getSelectedRow(), 2)) - protein.start();
    positionComboBox.insertItemAt(inputTable.getValueAt(inputTable.getSelectedRow(), 2) + " (" +
                                  protein.charsAt(position).toString().substring(1, protein.charsAt(position).toString().length()-1) + ")", position + 1);
    if (positionComboBox.getSelectedIndex() == position) {
      // Už tímto by se mělo upravit
      positionComboBox.setSelectedIndex(position + 1);
    }
    positionComboBox.removeItemAt(position);
  }

  private boolean canEdit() {
    switch (inputTable.getSelectedRowCount()) {
      case 1:
        return true;
      case 0:
        return false;
    }

    int[] selected = inputTable.getSelectedRows();
    for (int j : new int[]{ 4, 0, 3 }) {
      for (int i = 1; i < selected.length; i++) {
        if (!inputTable.getValueAt(selected[i-1], j).equals(inputTable.getValueAt(selected[i], j))) {
          return false;
        }
      }
    }
    if (MUTATION.equals(inputTable.getValueAt(selected[0], 1))) {
      return false;
    }
    boolean same = true;
    for (int j : new int[]{ 1, 2 }) {
      for (int i = 1; i < selected.length; i++) {
        if (!inputTable.getValueAt(selected[i-1], j).equals(inputTable.getValueAt(selected[i], j))) {
          if (same) {
            same = false;
            break;
          } else {
            return false;
          }
        }
      }
    }

    return true;
  }

  public void editSelected() {
    if (inputTable.getSelectedRowCount() == 0) {
      JOptionPane.showMessageDialog(this, "Some row must be selected.", "No item", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    if (inputTable.getValueAt(0, 0) == null || inputTable.getValueAt(0, 0).equals("")) {
      JOptionPane.showMessageDialog(this, "No item is inserted.", "No item", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    if (!canEdit()) {
      JOptionPane.showMessageDialog(this, "Incompatible items selected.", "Incompatible items", JOptionPane.INFORMATION_MESSAGE);
      return;
    }


    if (!edited.isEmpty()) {
      if (edited.get(0)[1].equals(MUTATION)) {
        for (int i = 0; i < inputTable.getRowCount(); i++) {
          boolean match = true; // Mohla být mezi tím smazána
          for (int j = 0; j < inputTable.getColumnCount(); j++) {
            if (!edited.get(0)[j].equals(inputTable.getValueAt(i, j))) {
              match = false;
              break;
            }
          }
          if (match) {
            String[] aas = edited.get(0)[4].toString().split(", ");
            Protein protein = proteins.get(edited.get(0)[0]);
            int pos = Integer.parseInt(edited.get(0)[2].toString()) - protein.start();
            for (String aa : aas) {
              protein.addMutation(pos, aa.charAt(0));
            }
            break;
          }
        }
      }
      edited.clear();
    }

    int[] selected = inputTable.getSelectedRows();
    targetComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 0));
    if (MUTATION.equals(inputTable.getValueAt(inputTable.getSelectedRow(), 1))) {
      deleteMutation();
      typeComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 1));
      positionComboBox.setSelectedIndex(Integer.parseInt((String)inputTable.getValueAt(inputTable.getSelectedRow(), 2))
                                        -proteins.get(inputTable.getValueAt(inputTable.getSelectedRow(), 0)).start());
      presenceComboBox.setSelectedIndex(1);
      modificationsComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 4));
    } else {
      if (inputTable.getSelectedRowCount() == 1) {
        typeComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 1));
        positionComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 2));
      } else {
        String same = "";
        for (int i = 1; i < selected.length; i++) {
          if (!inputTable.getValueAt(selected[i-1], 1).equals(inputTable.getValueAt(selected[i], 1))) {
            same += inputTable.getValueAt(selected[i], 1).toString().charAt(inputTable.getValueAt(selected[i], 1).toString().length()-2);
          }
        }
        if (same.isEmpty()) {
          typeComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 1));
        } else {
          typeComboBox.setSelectedItem(inputTable.getValueAt(selected[0], 1).toString().charAt(inputTable.getValueAt(selected[0], 1).toString().length()-2) + same);
        }
        same = "";
        for (int i = 1; i < selected.length; i++) {
          if (!inputTable.getValueAt(selected[i-1], 2).equals(inputTable.getValueAt(selected[i], 2))) {
            same += " " + inputTable.getValueAt(selected[i], 2);
          }
        }
        if (same.isEmpty()) {
          positionComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 2));
        } else {
          positionComboBox.setSelectedItem(inputTable.getValueAt(selected[0], 2) + same);
        }
      }
      presenceComboBox.setSelectedItem(inputTable.getValueAt(inputTable.getSelectedRow(), 3));
      String mod = ((String)inputTable.getValueAt(inputTable.getSelectedRow(), 4)).split(" \\(")[0];
      modificationsComboBox.setSelectedItem(mod);
      if (!modificationsComboBox.getSelectedItem().equals(mod)) {
        if (specificCheckBox.isSelected()) {
          specificCheckBox.doClick();
          modificationsComboBox.setSelectedItem(mod);
        }
        if (!modificationsComboBox.getSelectedItem().equals(mod)) {
          JOptionPane.showMessageDialog(inputTable, "Modification wasn't found in the list of modifications!"  + System.lineSeparator() +
                                        "It was probably removed or renamed.", "Modification not found", JOptionPane.ERROR_MESSAGE);
          inputTable.setRowSelectionInterval(inputTable.getSelectedRow(), inputTable.getSelectedRow());
          return;
        }
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
    targetComboBox.requestFocus();
  }

  private boolean canTemplate() {
    if (inputTable.getSelectedRowCount() == 0 || inputTable.getValueAt(inputTable.getSelectedRow(), 0) == null) {
      return false;
    }

    int[] selected = inputTable.getSelectedRows();
    for (int i = 0; i < selected.length; i++) {
      if (MUTATION.equals(inputTable.getValueAt(selected[i], 1))) {
        return false;
      }
    }

    return true;
  }

  public void templateSelected() {
    if (!canTemplate()) {
      return;
    }

    LinkedHashSet<String> lhs = new LinkedHashSet(inputTable.getSelectedRowCount());
    for (int i : inputTable.getSelectedRows()) {
      if (!lhs.contains(inputTable.getValueAt(i, 1).toString() + MOD_SEPARATOR + PRESENCES[1] + MOD_SEPARATOR + inputTable.getValueAt(i, 4))) {
        lhs.add(inputTable.getValueAt(i, 1).toString() + MOD_SEPARATOR + inputTable.getValueAt(i, 3) + MOD_SEPARATOR + inputTable.getValueAt(i, 4));
        if (PRESENCES[1].equals(inputTable.getValueAt(i, 3))) {
          lhs.remove(inputTable.getValueAt(i, 1).toString() + MOD_SEPARATOR + PRESENCES[0] + MOD_SEPARATOR + inputTable.getValueAt(i, 4));
        }
      }
    }
    StringBuilder sb = new StringBuilder();
    for (String s : lhs) {
      sb.append(MODS_SEPARATOR).append(s);
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
          sbs.append(i == 0 ? MODS_SEPARATOR : MOD_SEPARATOR).append(mod[i]);
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
    if (templatesComboBox.getSelectedIndex() == 0 && ((typeComboBox.getEditor().getEditorComponent().hasFocus() && !verifyType(typeComboBox)) ||
                                                      (positionComboBox.getEditor().getEditorComponent().hasFocus() && !verifyPosition()) ||
                                                      (modificationsComboBox.getEditor().getEditorComponent().hasFocus() && !verifyType(modificationsComboBox)))) {
      return;
    }
    // Zřejmě zatrhnuto filtrování dle specificity a tam žádná možnost není.
    if (templatesComboBox.getSelectedIndex() == 0 && modificationsComboBox.getSelectedItem() == null) {
      if (alert) {
        JOptionPane.showMessageDialog(this, "No modification is selected!", "Cannot insert", JOptionPane.WARNING_MESSAGE);
        modificationsComboBox.requestFocus();
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
      positionComboBox.setSelectedIndex(0);
      specificCheckBox.setSelected(false);
      templatesComboBox.setSelectedIndex(0);
      int ins = inputTable.getRowCount();
      for (String[] mod : templates.get(selected)) {
        typeComboBox.setSelectedItem(mod[0]);
        if (!mod[0].equals(typeComboBox.getSelectedItem())) {
          continue;
        }
        presenceComboBox.setSelectedItem(mod[1]);
        if (!mod[1].equals(presenceComboBox.getSelectedItem())) {
          continue;
        }
        String m = mod[2].replaceFirst(" \\(.*\\)$", "");
        modificationsComboBox.setSelectedItem(m);
        if (!m.equals(modificationsComboBox.getSelectedItem())) {
          continue;
        }
        ins = insertModification(replace, ins, true);
      }
      specificCheckBox.setSelected(specificity);
    // Jedná se o mutaci
    } else if (typeComboBox.getSelectedItem().equals(MUTATION)) {
      int ins = inputTable.getRowCount();
      if (!edited.isEmpty()) {
        int[] rem = new int[replace.size()];
        for (int i = 0; i < replace.size(); i++) {
          rem[i] = replace.get(i);
        }
        ins = delete(rem);
        edited.clear();
      }
      // Když je tabulka prázdná, netřeba nic testovat
      if (inputTable.getRowCount() == 1 && inputTable.getValueAt(0, 0) == null) {
        String[] parts = positionComboBox.getSelectedItem().toString().split("( \\()|(\\))");
        inputTable.setValueAt(targetComboBox.getSelectedItem().toString(), 0, 0);
        inputTable.setValueAt(typeComboBox.getSelectedItem().toString(), 0, 1);
        inputTable.setValueAt(parts[0], 0, 2);
        inputTable.setValueAt("", 0, 3);
        inputTable.setValueAt(joinTypes(parts[1], modificationsComboBox.getSelectedItem().toString()), 0, 4);
        insertMutation(parts[0]);
      } else {
        String[] parts = positionComboBox.getSelectedItem().toString().split("( \\()|(\\))");
        int pos = -1;
        for (int i = 0; i < inputTable.getRowCount(); i++) {
          if (inputTable.getValueAt(i, 0).equals(targetComboBox.getSelectedItem()) && inputTable.getValueAt(i, 1).equals(typeComboBox.getSelectedItem()) &&
              inputTable.getValueAt(i, 2).equals(parts[0])) {
            pos = i;
            break;
          }
        }
        if (pos >= 0) {
          inputTable.setValueAt(joinTypes((String)inputTable.getValueAt(pos, 4), (String)modificationsComboBox.getSelectedItem()), pos, 4);
        } else {
          ((DefaultTableModel)inputTable.getModel()).insertRow(ins, new Object[] { targetComboBox.getSelectedItem().toString(),
                                                                           typeComboBox.getSelectedItem().toString(), parts[0], "",
                                                                           joinTypes(parts[1], modificationsComboBox.getSelectedItem().toString())});
        }
        insertMutation(parts[0]);
      }
      modificationsComboBox.setSelectedIndex(modificationsComboBox.getItemCount() == 0 ? -1 : 0);
    // Jedná se o modifikaci
    } else {
      insertModification(replace, inputTable.getRowCount(), alert);
    }
  }

  private int insertModification(ArrayList<Integer> replace, int ins, boolean alert) {
    // Je jednodušší smazat, než řešit multivklady
    if (inputTable.getRowCount() == 1 && inputTable.getValueAt(0, 0) == null) {
      replace.add(0);
    }
    String[] aas;
    if (typeComboBox.getSelectedIndex() < 0) {
      aas = ((String)typeComboBox.getSelectedItem()).split(", ");
      for (int i = 0; i < aas.length; i++) {
        aas[i] = longer(aas[i]);
      }
    } else {
      aas = new String[] { (String)typeComboBox.getSelectedItem() };
    }
    String[] positions = positionComboBox.getSelectedIndex() < 0 ? positionComboBox.getSelectedItem().toString().split(", ")
            : new String[] { positionComboBox.getSelectedItem().toString() };
    TreeMap<String, TreeSet<String>> comb = new TreeMap();
    boolean empty = true;
    for (String aa : aas) {
      comb.put(aa, new TreeSet<String>());
      for (String position : positions) {
        if (verifyInput(replace, aa, position, alert)) {
          comb.get(aa).add(position);
          empty = false;
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
    }else if (ins > inputTable.getRowCount()) {
      ins = inputTable.getRowCount();
    }
    
    String name = modificationsComboBox.getSelectedItem().toString();
    String mod = name + " (" + Defaults.sMassShortFormat.format(modifications.get(name).getModification());
    if (modifications.get(name).getCheckDiff().compareTo(BigDecimal.ZERO) != 0) {
      mod += "/ " + Defaults.sMassShortFormat.format(modifications.get(name).getCheckDiff());
    }
    mod += ")";
    for (String aa : comb.keySet()) {
      for (String position : comb.get(aa)) {
        ((DefaultTableModel)inputTable.getModel()).insertRow(ins++, new Object[] { targetComboBox.getSelectedItem().toString(), aa, position,
                                                                                   presenceComboBox.getSelectedItem().toString(), mod});
      }
    }
    backup.put(mod, modifications.get(modificationsComboBox.getSelectedItem()));
    return ins;
  }

  private void insertMutation(String position) {
    String[] aas = ((String)modificationsComboBox.getSelectedItem()).replaceFirst("^.*\\(", "").replaceFirst("\\).*$", "").split(", ");
    Protein protein = proteins.get(targetComboBox.getSelectedItem());
    int pos = Integer.parseInt(position) - protein.start();
    for (String aa : aas) {
      for (char c : Monomer.resolveShortcut(aa.charAt(0)).toCharArray()) {
        protein.addMutation(pos, c);
      }
    }
    int j = 0;
    for (int i = 0; i < TYPES.length; i++) {
      if (TYPES[i].equals(typeComboBox.getItemAt(j))) {
        j++;
      } else {
        for (String aa : aas) {
          if (aa.charAt(0) == TYPES[i].charAt(TYPES[i].length()-2)) {
            typeComboBox.insertItemAt(TYPES[i], j++);
          }
        }
      }
    }
    int index = positionComboBox.getSelectedIndex();
    positionComboBox.insertItemAt(((String)positionComboBox.getSelectedItem()).replaceFirst("\\(.*\\)",
        "(" + joinTypes((String)modificationsComboBox.getSelectedItem(), ((String)positionComboBox.getSelectedItem()).replaceAll("(^.*\\()|(\\)$)", "")) + ")"),
        index);
    positionComboBox.setSelectedIndex(index);
    positionComboBox.removeItemAt(index+1);
  }

  private boolean verifyInput(ArrayList<Integer> skip, String type, String position, boolean alert) {
    // TODO: Zjistit úroveň, ale zatím se tam nezohledňuje...
    if (!verifyProteases(0, type, position, alert)) {
      return false;
    }

    if (type.matches(".*\\(.\\)")) {
      type = type.substring(type.length()-2, type.length()-1);
    }
    for (int i = 0; i < inputTable.getRowCount(); i++) {
      if (skip.contains(i)) {
        continue;
      }
      
      // Když se jedná o různé proteiny, nemůže nastat kolize
      if (!targetComboBox.getSelectedItem().equals(inputTable.getValueAt(i, 0)) &&
          proteins.containsKey(targetComboBox.getSelectedItem()) && proteins.containsKey(inputTable.getValueAt(i, 0))) {
        continue;
      }
      // Když jde o odlišné pozice, tak nás to netrábí
      if (!(position.equals(POSITIONS[0]) || inputTable.getValueAt(i, 2).equals(POSITIONS[0]) ||
            ((String)inputTable.getValueAt(i, 2)).matches("(^|[^0-9])" + position + "($|[^0-9])"))) {
        continue;
      }
      // Když se ptáme na jiné písmeno, nemůže nastat kolize
      if (type.equals(MUTATION) || inputTable.getValueAt(i, 1).equals(MUTATION)) {
        if (!(type.equals(MUTATION) && inputTable.getValueAt(i, 1).equals(MUTATION))) {
          continue;
        }
      } else {
        boolean intersect = false;
        for (char t1 : Monomer.resolveShortcut(type.charAt(0)).toCharArray()) {
          for (char t2 : Monomer.resolveShortcut(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2)).toCharArray()) {
            if (t1 == t2) {
              intersect = true;
              break;
            }
          }
        }
        if (!intersect) {
          continue;
        }
      }

      // Opakované vkládání modifikace
      if (((String)inputTable.getValueAt(i, 4)).split(" \\(")[0].equals(modificationsComboBox.getSelectedItem())) {
        if (alert) { alertCollision("This modification has already been inserted on this position", i, type, position); }
        return false;
      }
//      int index = 0;
//      for (index = 0; index < targetComboBox.getItemCount(); index++) {
//        if (inputTable.getValueAt(i, 0).equals(targetComboBox.getItemAt(index))) {
//          break;
//        }
//      }
//      // Zároveň modifikace před štěpením i po štěpení
//      if ((index < proteins.size() && targetComboBox.getSelectedIndex() >= proteins.size()) ||
//          (index >= proteins.size() && targetComboBox.getSelectedIndex() < proteins.size())) {
//        // TODO Možné možnosti nějak rozdrobit a některé kombinace umožnit
//        if (alert) { alertCollision(parent, "It isn't allowed to have modifications at the position before and after digest too", i, type, position); }
//        return false;
//      }
//      // Modifikace v různých úrovních štěpení
//      if (index >= proteins.size() && targetComboBox.getSelectedIndex() >= proteins.size() && index != targetComboBox.getSelectedIndex()) {
//        // TODO Možná možnosti nějak rozdrobit a některé kombinace umožnit
//        if (alert) { alertCollision(parent, "It isn't allowed to have modifications at different levels", i, type, position); }
//        return false;
//      }
//      if (presenceComboBox.getSelectedItem().equals(presences[0]) && inputTable.getValueAt(i, 3).equals(presences[0]) &&
//          ((String)inputTable.getValueAt(i, 2)).matches(position)) {
//        if (alert) { alertCollision(parent, "Another fixed modification has already been inserted for this postion", i, type, position); }
//        return false;
//      }
    }
    return true;
  }

  public boolean verifyInput(Component parent, String target, String type, String position, int alertLevel) {
    if (!verifyProteases(0, type, position, alertLevel != JOptionPane.ERROR_MESSAGE)) {
      return false;
    }

    if (inputTable.getRowCount() == 1 && (inputTable.getValueAt(0, 0) == null || inputTable.getValueAt(0, 0).toString().isEmpty())) {
      return true;
    }

    String presence = PRESENCES[1];

    if (type.matches(".*\\(.\\)")) {
      type = type.substring(type.length()-2, type.length()-1);
    }
    for (int i = 0; i < inputTable.getRowCount(); i++) {
      // Když se jedná o různé proteiny, nemůže nastat kolize
      if (proteins.containsKey(target) && !target.equals(inputTable.getValueAt(i, 0)) && proteins.containsKey(inputTable.getValueAt(i, 0))) {
        continue;
      }
      // Když jde o odlišné pozice, tak nás to netrápí
      if (!(position.equals(POSITIONS[0]) || inputTable.getValueAt(i, 2).equals(POSITIONS[0]) ||
            ((String)inputTable.getValueAt(i, 2)).matches("(^|[^0-9])" + position + "($|[^0-9])"))) {
        continue;
      }
      // Když se ptáme na jiné písmeno, nemůže nastat kolize
      if (inputTable.getValueAt(i, 1).equals(MUTATION)) {
        continue;
      } else {
        boolean intersect = false;
        for (char t1 : Monomer.resolveShortcut(type.charAt(0)).toCharArray()) {
          for (char t2 : Monomer.resolveShortcut(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2)).toCharArray()) {
            if (t1 == t2) {
              intersect = true;
              break;
            }
          }
        }
        if (!intersect) {
          continue;
        }
      }
//      // Zároveň modifikace před štěpením i po štěpení
//      if (!(target.equals(inputTable.getValueAt(i, 0)) || (proteins.containsKey(target) && TARGETS[0].equals(inputTable.getValueAt(i, 0))) ||
//                                                          (proteins.containsKey(inputTable.getValueAt(i, 0)) && TARGETS[0].equals(target)))) {
//        // TODO Možná možnosti nějak rozdrobit a některé kombinace umožnit
//        if (alert) {
//          alertCollision(parent, "There will be modification on this position after digest", i, target, type, position, presence);
//        }
//        return false;
//      }
      // Fixní modifikace
      if (inputTable.getValueAt(i, 3).equals(PRESENCES[0])) {
        boolean strict = true;
        char[] bondAAs = Monomer.resolveShortcut(type.charAt(0)).toCharArray();
        Collection<Protein> bondProteins = proteins.containsKey(target) ? Arrays.asList(proteins.get(target)) : proteins.values();
        String modAAs = Monomer.resolveShortcut(inputTable.getValueAt(i, 1).toString().charAt(inputTable.getValueAt(i, 1).toString().length()-2));
        Collection<Protein> modProteins = proteins.containsKey(inputTable.getValueAt(i, 0)) ? Arrays.asList(proteins.get(inputTable.getValueAt(i, 0))) : proteins.values();
        for (Protein protein : bondProteins) {
          if (modProteins.contains(protein)) {
            for (char aa : bondAAs) {
              if (modAAs.indexOf(aa) < 0) {
                if (!(position.equals(POSITIONS[0]) && protein.positions(aa).isEmpty())) {
                  strict = false;
                  break;
                }
              } else {
                if (!POSITIONS[0].equals(inputTable.getValueAt(i, 2)) && ((!position.equals(POSITIONS[0]) && !position.equals(inputTable.getValueAt(i, 2))) ||
                                                                          (position.equals(POSITIONS[0]) && protein.positions(aa).size() > 1))) {
                                                                                                            // Předpoklad, že pozice modifikace tam opravdu je
                  strict = false;
                  break;
                }
              }
            }
          } else {
            for (char aa : bondAAs) {
              if (!(position.equals(POSITIONS[0]) && protein.positions(aa).isEmpty())) {
                strict = false;
                break;
              }
            }
          }
          if (!strict) {
            break;
          }
        }
        if (strict) {
          if (alertLevel != JOptionPane.ERROR_MESSAGE) {
            alertCollision(parent, "If some modification and some bond are on the same position, they must be both variable",
                           i, target, type, position, presence, JOptionPane.WARNING_MESSAGE);
          }
          return false;
        } else if (alertLevel == JOptionPane.INFORMATION_MESSAGE) {
          if (confirmCollision(parent, "The bond has interesection with some fixed modification, common positions will be skipped",
                         i, target, type, position, presence, JOptionPane.INFORMATION_MESSAGE)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean verifyProteases(int level, String type, String position, boolean alert) {
//    if (type.matches(".*\\(.\\)")) {
//      type = type.substring(type.length()-2, type.length()-1);
//    }
//    for (Collection<Protease> niveau : proteases) {
//      for (Protease protease : niveau) {
//        if (protease.getModificationLeft() != 0.0) {
//          String last = protease.getRuleLeft();
//          if (last.charAt(last.length()-1) == ']') {
//            last = last.substring(last.lastIndexOf('['));
//          } else {
//            last = last.substring(last.length()-1);
//          }
//          // TODO: Ještě v závislosti na volbě neštěpení na modifikacích a na úrovni modifikace a proteázy
//          if (type.matches(last)) {
//            JOptionPane.showMessageDialog(this, "Collision with modification on protease.", "Conflict", JOptionPane.WARNING_MESSAGE);
//            return false;
//          }
//        }
//        if (protease.getModificationRight() != 0.0) {
//          String first = protease.getRuleRight();
//          if (first.charAt(0) == '[') {
//            first = first.substring(0, first.indexOf(']')+1);
//          } else {
//            first = first.substring(0, 1);
//          }
//          // TODO: Ještě v závislosti na volbě neštěpení na modifikacích a na úrovni modifikace a proteázy
//          if (type.matches(first)) {
//            JOptionPane.showMessageDialog(this, "Collision with modification on protease.", "Conflict", JOptionPane.WARNING_MESSAGE);
//            return false;
//          }
//        }
//      }
//    }
    return true;
  }

  private boolean verifyType(JComboBox caller) {
    String inp = ((JTextField)caller.getEditor().getEditorComponent()).getText().toUpperCase();
    for (int i = 0; i < caller.getItemCount(); i++) {
      String string = ((String)caller.getItemAt(i));
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
      for (int i = 0; i < caller.getItemCount(); i++) {
        if (((String)caller.getItemAt(i)).matches(".*\\(.\\)") &&
            inp.charAt(j) == ((String)caller.getItemAt(i)).charAt(((String)caller.getItemAt(i)).length()-2)) {
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

  private boolean verifyPosition() {
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

  private boolean formatType(JComboBox caller) {
    String inp = ((JTextField)caller.getEditor().getEditorComponent()).getText().toUpperCase();
    for (int i = 0; i < caller.getItemCount(); i++) {
      String string = ((String)caller.getItemAt(i));
      if (string.toUpperCase().equals(inp)) {
        if (i != caller.getSelectedIndex()) {
          caller.setSelectedIndex(i);
          return false;
        }
        return true;
      }
      if (inp.startsWith(string.toUpperCase().replaceFirst(" \\(.\\)", "")) || (string.toUpperCase().startsWith(inp) && inp.length() > 1)) {
        caller.setSelectedItem(string);
        return true;
      }
    }
    TreeSet<Character> chars = new TreeSet<>(new Comparator() {
      public int compare(Object o1, Object o2) {
        return compareTypes(o1.toString(), o2.toString());
      }
    });
    for (int j = 0; j < inp.length(); j++) {
      for (int i = 0; i < caller.getItemCount(); i++) {
        if (((String)caller.getItemAt(i)).matches(".*\\(.\\)") &&
            inp.charAt(j) == ((String)caller.getItemAt(i)).charAt(((String)caller.getItemAt(i)).length()-2)) {
          chars.add(inp.charAt(j));
          break;
        }
      }
    }
    if (chars.isEmpty()) {
      ((JTextField)caller.getEditor().getEditorComponent()).requestFocus();
      return false;
    }
    if (chars.size() == 1) {
      char c = chars.iterator().next();
      for (String string : TYPES) {
        if (string.contains("(" + c + ")")) {
          caller.setSelectedItem(string);
        }
      }
    } else {
      String tmp = chars.toString().substring(1, chars.toString().length()-1);
      if (tmp.equals(caller.getSelectedItem())) {
        return true;
      }
      caller.setSelectedItem(tmp);
      return true;
    }
    ((JTextField)caller.getEditor().getEditorComponent()).requestFocus();
    return false;
  }

  private void formatPosition() {
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

  private int compareTypes(String s1, String s2) {
    int v1 = -1;
    int v2 = -1;
    for (int i = 0; i < TYPES.length-1; i++) {
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
    s1 = s1.replace('^', '<').replace('$', '>');
    s2 = s2.replace('^', '<').replace('$', '>');
    return s1.compareTo(s2);
  }

  private String longer(String str) {
    for (int i = 0; i < TYPES.length; i++) {
      if (TYPES[i].contains("(" + str + ")")) {
        return TYPES[i];
      }
    }
    return str;
  }

  private String joinTypes(String s1, String s2) {
    if (s1.matches(".*\\(.\\)")) {
      s1 = s1.substring(s1.length()-2, s1.length()-1);
    }
    if (s2.matches(".*\\(.\\)")) {
      s2 = s2.substring(s2.length()-2, s2.length()-1);
    }
    TreeSet<Character> chars = new TreeSet<>();
    for (String string : s1.split(", ")) {
      for (char c : Monomer.resolveShortcut(string.charAt(0)).toCharArray()) {
        chars.add(c);
      }
    }
    for (String string : s2.split(", ")) {
      for (char c : Monomer.resolveShortcut(string.charAt(0)).toCharArray()) {
        chars.add(c);
      }
    }
    return chars.toString().substring(1, chars.toString().length()-1);
  }

  private void alertCollision(String message, int i, String type, String position) {
    int j = 0;
    for (j = 0; j < TYPES.length; j++) {
      if (TYPES[j].contains("(" + type + ")")) {
        break;
      }
    }
    JOptionPane.showMessageDialog(this, message + ":" + System.lineSeparator() + "(" + (i+1) + ") " + inputTable.getValueAt(i, 0) + " - " +
            inputTable.getValueAt(i, 1) + " - " + inputTable.getValueAt(i, 2) + " - "+ inputTable.getValueAt(i, 3) + " - " + inputTable.getValueAt(i, 4) +
            System.lineSeparator() + "vs. " + targetComboBox.getSelectedItem() + " - " + (j < TYPES.length ? TYPES[j] : type) + " - " + position + " - " +
            presenceComboBox.getSelectedItem() + " - " + modificationsComboBox.getSelectedItem(), "Conflict", JOptionPane.WARNING_MESSAGE);
  }

  private void alertCollision(Component parent, String message, int i, String target, String type, String position, String presence, int messageType) {
    int j = 0;
    for (j = 0; j < TYPES.length; j++) {
      if (TYPES[j].contains("(" + type + ")")) {
        break;
      }
    }
    JOptionPane.showMessageDialog(parent, message + ":" + System.lineSeparator() + inputTable.getValueAt(i, 0) + " - " + inputTable.getValueAt(i, 1) + " - " +
            inputTable.getValueAt(i, 2) + " - "+ inputTable.getValueAt(i, 3) + " (Modification no." + (i+1) + ") vs." + System.lineSeparator() +
            target + " - " + (j < TYPES.length ? TYPES[j] : type) + " - " + position + " - " + presence, "Conflict", messageType);
  }

  private boolean confirmCollision(Component parent, String message, int i, String target, String type, String position, String presence, int messageType) {
    int j = 0;
    for (j = 0; j < TYPES.length; j++) {
      if (TYPES[j].contains("(" + type + ")")) {
        break;
      }
    }
    return JOptionPane.showConfirmDialog(parent, message + ":" + System.lineSeparator() + inputTable.getValueAt(i, 0) + " - " + inputTable.getValueAt(i, 1) +
                                                 " - " + inputTable.getValueAt(i, 2) + " - "+ inputTable.getValueAt(i, 3) + " (Modification no." + (i+1) + ") vs." +
                                                 System.lineSeparator() + target + " - " + (j < TYPES.length ? TYPES[j] : type) + " - " + position + " - " + presence,
                                         "Conflict", JOptionPane.OK_CANCEL_OPTION, messageType) != JOptionPane.OK_OPTION;
  }
}
