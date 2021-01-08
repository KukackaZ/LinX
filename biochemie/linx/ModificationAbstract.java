package biochemie.linx;

import biochemie.ChemicalElement;
import biochemie.Monomer;
import java.math.BigDecimal;
import java.util.HashSet;

/**
 *
 * @author Janek
 */
public class ModificationAbstract {
  public final static String FILE = "modifications";
  public final static String SEPARATOR = "\t";

  private String name;
  private BigDecimal modification;
  private BigDecimal diff;
  private HashSet<Character> specificity;

  public ModificationAbstract(String name, String characteristic) {
    this.name = name;
    String[] parts = characteristic.split(SEPARATOR);
    try {
      modification = new BigDecimal(parts[0]);
    } catch (Exception e) {
      modification = ChemicalElement.evaluate(parts[0]);
    }
    if (parts.length == 1 || parts[1].isEmpty()) {
      diff = BigDecimal.ZERO;
    } else {
      try {
        diff = new BigDecimal(parts[1]);
      } catch (Exception e) {
        diff = ChemicalElement.evaluate(parts[1]);
      }
    }
    if (parts.length <= 2 || parts[2].isEmpty()) {
      specificity = null;
    } else {
      specificity = new HashSet();
      for (char c : parts[2].toCharArray()) {
        if (Monomer.isMonomer(c) || Monomer.isTerminus(c)) {
          specificity.add(c);
        } else if (Monomer.isShortcut(c)) {
          for (char r : Monomer.resolveShortcut(c).toCharArray()) {
            specificity.add(r);
          }
        }
      }
    }
  }

  public String getName() {
    return name;
  }

  public BigDecimal getModification() {
    return modification;
  }

  public BigDecimal getCheckDiff() {
    return diff;
  }

  public boolean specific(char ch) {
    return specificity == null || specificity.contains(ch);
  }
}
