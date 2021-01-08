package biochemie;

import java.math.BigDecimal;
import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Testing of aminoacids
 * @author Janek
 */
public class Monomer {
  // Podle http://proteomicsresource.washington.edu/tools/masses.php
  public static final String FILE = "monomers";
  public static final String SEPARATOR = "\t";
  private static Properties properties;
  private static HashMap<Character, AbstractMap.SimpleImmutableEntry<String, BigDecimal>> termini;
  private static HashMap<Character, AbstractMap.SimpleImmutableEntry<String, BigDecimal>> masses;
  private static HashMap<Character, AbstractMap.SimpleImmutableEntry<String, String>> shortcuts;

  static {
    properties = Defaults.loadDefaults(FILE);
    ChemicalElement.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        load();
      }
    });
    load();
  }
  
  private static void load() {
    if (properties.stringPropertyNames().isEmpty()) {
      termini = new HashMap(2);
      masses = new HashMap(22);
      shortcuts = new HashMap(4);
      termini.put('^', new AbstractMap.SimpleImmutableEntry("N-terminus", ChemicalElement.mass("H")));
      termini.put('$', new AbstractMap.SimpleImmutableEntry("C-terminus", ChemicalElement.evaluate("OH")));
      masses.put('A', new AbstractMap.SimpleImmutableEntry("Alanin", ChemicalElement.evaluate("C3H5NO")));
      shortcuts.put('B', new AbstractMap.SimpleImmutableEntry("Aspar*", "DN"));
      masses.put('C', new AbstractMap.SimpleImmutableEntry("Cysteine", ChemicalElement.evaluate("C3H5NOS")));
      masses.put('D', new AbstractMap.SimpleImmutableEntry("Aspartic acid", ChemicalElement.evaluate("C4H5NO3")));
      masses.put('E', new AbstractMap.SimpleImmutableEntry("Glutamic acid", ChemicalElement.evaluate("C5H7NO3")));
      masses.put('F', new AbstractMap.SimpleImmutableEntry("Phenylalanine", ChemicalElement.evaluate("C9H9NO")));
      masses.put('G', new AbstractMap.SimpleImmutableEntry("Glycine", ChemicalElement.evaluate("C2H3NO")));
      masses.put('H', new AbstractMap.SimpleImmutableEntry("Histidine", ChemicalElement.evaluate("C6H7N3O")));
      masses.put('I', new AbstractMap.SimpleImmutableEntry("Isoleucine", ChemicalElement.evaluate("C6H11NO")));
      shortcuts.put('J', new AbstractMap.SimpleImmutableEntry("*leucine", "IL"));
      masses.put('K', new AbstractMap.SimpleImmutableEntry("Lysine", ChemicalElement.evaluate("C6H12N2O")));
      masses.put('L', new AbstractMap.SimpleImmutableEntry("Leucine", ChemicalElement.evaluate("C6H11NO")));
      masses.put('M', new AbstractMap.SimpleImmutableEntry("Methionine", ChemicalElement.evaluate("C5H9NOS")));
      masses.put('N', new AbstractMap.SimpleImmutableEntry("Asparagine", ChemicalElement.evaluate("C4H6N2O2")));
      masses.put('O', new AbstractMap.SimpleImmutableEntry("Pyrrolysine", ChemicalElement.evaluate("C12H19N3O2")));
      masses.put('P', new AbstractMap.SimpleImmutableEntry("Proline", ChemicalElement.evaluate("C5H7NO")));
      masses.put('Q', new AbstractMap.SimpleImmutableEntry("Glutamine", ChemicalElement.evaluate("C5H8N2O2")));
      masses.put('R', new AbstractMap.SimpleImmutableEntry("Arginine", ChemicalElement.evaluate("C6H12N4O")));
      masses.put('S', new AbstractMap.SimpleImmutableEntry("Serine", ChemicalElement.evaluate("C3H5NO2")));
      masses.put('T', new AbstractMap.SimpleImmutableEntry("Threonine", ChemicalElement.evaluate("C4H7NO2")));
      masses.put('U', new AbstractMap.SimpleImmutableEntry("Selenocysteine", ChemicalElement.evaluate("C3H5NOSe")));
      masses.put('V', new AbstractMap.SimpleImmutableEntry("Valine", ChemicalElement.evaluate("C5H9NO")));
      masses.put('W', new AbstractMap.SimpleImmutableEntry("Tryptophan", ChemicalElement.evaluate("C11H10N2O")));
      shortcuts.put('X', new AbstractMap.SimpleImmutableEntry("any", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
      masses.put('Y', new AbstractMap.SimpleImmutableEntry("Tyrosine", ChemicalElement.evaluate("C9H9NO2")));
      shortcuts.put('Z', new AbstractMap.SimpleImmutableEntry("Glutami*", "EQ"));
    } else {
      termini = new HashMap(2);
      termini.put('^', new AbstractMap.SimpleImmutableEntry("N-terminus", BigDecimal.ZERO));
      termini.put('$', new AbstractMap.SimpleImmutableEntry("C-terminus", BigDecimal.ZERO));
      masses = new HashMap(16);
      shortcuts = new HashMap();
      for (String key : properties.stringPropertyNames()) {
        if (key.length() == 1) {
          String[] parts = properties.getProperty(key).split("\t");
          if (parts.length == 2) {
            BigDecimal value = null;
            try {
              value = new BigDecimal(parts[0]);
            } catch (Exception e) {
              try {
                value = ChemicalElement.evaluate(parts[0]);
              } catch (Exception f) { }
            }
            if (value != null) {
              char k = key.toUpperCase().charAt(0);
              switch (k) {
                case '^':
                  termini.put('^', new AbstractMap.SimpleImmutableEntry("N-terminus", value));
                  break;
                case '$':
                  termini.put('$', new AbstractMap.SimpleImmutableEntry("C-terminus", value));
                  break;
                default:
                  masses.put(k, new AbstractMap.SimpleImmutableEntry(parts[1], value));
              }
            }
          } else if (parts.length == 3) {
            shortcuts.put(key.toUpperCase().charAt(0), new AbstractMap.SimpleImmutableEntry(parts[1], parts[2].toUpperCase()));
          }
        }
      }
    }

    for (Character key : shortcuts.keySet()) {
      // Duplicitní definice
      if (masses.containsKey(key)) {
        shortcuts.remove(key);
      }
      // Odstranění rekurzivních definic, protože nic nepřidávají
      String value = shortcuts.get(key).getValue().replace(key.toString(), "");
      // Odstraní ty znaky, které vůbec nejsou definované
      for (int i = 0; i < value.length(); i++) {
        if (!(masses.containsKey(value.charAt(i)) || shortcuts.containsKey(value.charAt(i)))) {
          value = value.replace(""+value.charAt(i), "");
        }
      }
      // Pokud je prázdný, tak je to zbytečné, ale asi by byl problém ve for-cyklu
      shortcuts.put(key, new AbstractMap.SimpleImmutableEntry(shortcuts.get(key).getKey(), value));
      // Odstraníme písmeno ze všech substitucí
      for (Character ch : shortcuts.keySet()) {
        shortcuts.put(ch, new AbstractMap.SimpleImmutableEntry(shortcuts.get(ch).getKey(), shortcuts.get(ch).getValue().replace(key.toString(), value)));
      }
    }
    HashMap<Character, AbstractMap.SimpleImmutableEntry<String, String>> tmp = shortcuts;
    shortcuts = new HashMap(shortcuts.size());
    for (Character key : tmp.keySet()) {
      TreeSet<Character> list = new TreeSet<>();
      for (char ch : tmp.get(key).getValue().toCharArray()) {
        list.add(ch);
      }
      list.remove(key);
      StringBuilder buf = new StringBuilder(list.size());
      for (Character val : list) {
        buf.append(val);
      }
      shortcuts.put(key, new AbstractMap.SimpleImmutableEntry(tmp.get(key).getKey(), buf.toString()));
    }
  }
  
  public static boolean isTerminus(char ch) {
    return termini.containsKey(ch);
  }

  /**
   * Test if character is Amino Acid (A, C..I, K..N, P..W, Y).
   * @param ch Tested character.
   * @return True if character is Amino Acid.
   */
  public static boolean isMonomer(char ch) {
    return masses.containsKey(Character.toUpperCase(ch));
  }

  public static boolean isShortcut(char ch) {
    return shortcuts.containsKey(Character.toUpperCase(ch));
  }

  public static ArrayList<String> getNames() {
    ArrayList<String> ret = new ArrayList(termini.size() + masses.size() + shortcuts.size());
    ret.add(termini.get('^').getKey() + " (^)");
    ret.add(termini.get('$').getKey() + " ($)");
    TreeSet<String> sort = new TreeSet();
    for (Map.Entry<Character, AbstractMap.SimpleImmutableEntry<String, BigDecimal>> pair : masses.entrySet()) {
      sort.add(pair.getValue().getKey() + " (" + pair.getKey() + ")");
    }
    ret.addAll(sort);
    sort.clear();
    for (Map.Entry<Character, AbstractMap.SimpleImmutableEntry<String, String>> pair : shortcuts.entrySet()) {
      sort.add(pair.getValue().getKey() + " (" + pair.getKey() + ")");
    }
    ret.addAll(sort);
    return ret;
  }

  public static Set<Character> getShortcuts() {
    return shortcuts.keySet();
  }

  public static String resolveShortcut(char shortcut) {
    return shortcuts.containsKey(shortcut) ? shortcuts.get(shortcut).getValue() : ""+shortcut;
  }

  public static BigDecimal mass(char c) {
    return masses.get(c).getValue();
  }
}
