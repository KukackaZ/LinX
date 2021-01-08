package biochemie;

import java.io.IOException;
import java.util.*;
import javax.swing.JOptionPane;
import biochemie.dbedit.ProteaseDialog;

/**
 *
 * @author Janek
 */
public abstract class ProteasesFile {
  public static final String FILE = "proteases";
  public static final String RULE_SEPARATOR = " ";
  public static final String PARAM_SEPARATOR = "\t";
  static final String PROTEASES_FILE = "PROpertieS\\proteases.pres";

  /**
   * Shows dialog for insert rule in proteases file.
   * @param parent Parent dialog.
   * @return Returns <code>RET_OK</code> if update was succesfull, else returns <code>RET_CANCEL</code>.
   */
  public static String addProtease(java.awt.Window parent) {
    return editProtease(parent, "", "", "");
  }

  /**
   * Shows dialog for update rule in proteases file.
   * @param parent Parent dialog.
   * @param name Name of updated protease.
   * @param rules Rules of updated protease.
   * @return Returns <code>RET_OK</code> if update was succesfull, else returns <code>RET_CANCEL</code>.
   */
  public static String editProtease(java.awt.Window parent, String name, String rules, String modifications) {
    ProteaseDialog pd = new ProteaseDialog(parent, name, rules, modifications);
    pd.setVisible(true);
    return pd.getAdded();
  }

  public static boolean removeProtease(java.awt.Component parent, String name) {
    try {
      LinkedHashMap<String, String[]> file = getProteases();
      if (file.containsKey(name)) {
        file.remove(name);
        saveProteases(file);
        return true;
      }
    } catch (IOException e) {
      JOptionPane.showMessageDialog(parent, "Change couldn't be saved.\n" + e.getMessage(), "IO Exception", JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }

  public static LinkedHashMap<String, String[]> getProteases() throws IOException {
    Properties proteases = Defaults.loadDefaults(FILE);
    TreeMap<Integer, String> ordered = new TreeMap<>();
    for (String key : proteases.stringPropertyNames()) {
      try {
        ordered.put(Integer.parseInt(key), proteases.getProperty(key));
      } catch (Exception e) { }
    }
    LinkedHashMap<String, String[]> ret = new LinkedHashMap<>(ordered.size());
    for (String value : ordered.values()) {
      int i = value.indexOf(PARAM_SEPARATOR);
      if (i > 0) {
        String[] parts = value.substring(i+PARAM_SEPARATOR.length()).split(PARAM_SEPARATOR, 2);
        if (parts.length == 2) {
          ret.put(value.substring(0, i), parts);
        }
      }
    }
    return ret;
  }

  public static void saveProteases(LinkedHashMap<String, String[]> proteases) throws IOException {
    Properties file = new Properties();
    int i = 1;
    for (Map.Entry<String, String[]> entry : proteases.entrySet()) {
      StringBuilder sb = new StringBuilder(entry.getKey());
      for (String string : entry.getValue()) {
        sb.append(PARAM_SEPARATOR).append(string);
      }
      file.setProperty(String.valueOf(i++), sb.toString());
    }
    Defaults.saveDefaults(FILE, file);
  }

  public static void moveUp(String name) throws IOException {
    LinkedHashMap<String, String[]> proteases = getProteases();
    int i = 0;
    for (String item : proteases.keySet()) {
      if (item.compareToIgnoreCase(name) == 0) {
        move(proteases, i, i-1);
        break;
      }
      i++;
    }
  }

  public static void moveDown(String name) throws IOException {
    LinkedHashMap<String, String[]> proteases = getProteases();
    int i = 0;
    for (String item : proteases.keySet()) {
      if (item.compareToIgnoreCase(name) == 0) {
        move(proteases, i, i+1);
        break;
      }
      i++;
    }
  }

  public static void moveTop(String name) throws IOException {
    LinkedHashMap<String, String[]> proteases = getProteases();
    int i = 0;
    for (String item : proteases.keySet()) {
      if (item.compareToIgnoreCase(name) == 0) {
        move(proteases, i, 0);
        break;
      }
      i++;
    }
  }

  public static void moveEnd(String name) throws IOException {
    LinkedHashMap<String, String[]> proteases = getProteases();
    int i = 0;
    for (String item : proteases.keySet()) {
      if (item.compareToIgnoreCase(name) == 0) {
        move(proteases, i, proteases.size() - 1);
        break;
      }
      i++;
    }
  }

  public static void moveOn(String name, int position) throws IOException {
    LinkedHashMap<String, String[]> proteases = getProteases();
    int i = 0;
    for (String item : proteases.keySet()) {
      if (item.compareToIgnoreCase(name) == 0) {
        move(proteases, i, position);
        break;
      }
      i++;
    }
  }

  private static int position(TreeMap<String, String> proteases, String name) {
    for (Map.Entry<String, String> entry : proteases.entrySet()) {
      if (entry.getValue().equalsIgnoreCase(name)) {
        return Integer.parseInt(entry.getKey());
      }
    }
    return -1;
  }

  private static void move(LinkedHashMap<String, String[]> proteases, int from, int on) throws IOException {
    if (proteases.size() == 0) { return; }
    if (from < 0) { from = 0; }
    if (from >= proteases.size()) { from = proteases.size()-1; }
    if (on < 0) { on = 0; }
    if (on >= proteases.size()) { on = proteases.size()-1; }
    if (from == on) { return; }

    LinkedHashMap<String, String[]> file = new LinkedHashMap<>(proteases.size());
    Iterator<String> it = proteases.keySet().iterator();
    for (int i = 0; i < Math.min(from, on); i++) {
      String index = it.next();
      file.put(index, proteases.get(index));
    }
    if (from < on) {
      String moved = it.next();
      for (int i = Math.min(from, on); i < Math.max(from, on); i++) {
        String index = it.next();
        file.put(index, proteases.get(index));
      }
      file.put(moved, proteases.get(moved));
    } else {
      LinkedHashMap<String, String[]> middle = new LinkedHashMap<>(proteases.size());
      for (int i = Math.min(from, on); i < Math.max(from, on); i++) {
        String index = it.next();
        middle.put(index, proteases.get(index));
      }
      String moved = it.next();
      file.put(moved, proteases.get(moved));
      file.putAll(middle);
    }
    while (it.hasNext()) {
      String index = it.next();
      file.put(index, proteases.get(index));
    }
    saveProteases(file);
  }
}
