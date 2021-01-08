package biochemie.linx;

import biochemie.ChemicalElement;
import biochemie.Monomer;
import java.math.BigDecimal;
import java.util.HashSet;

/**
 *
 * @author Janek
 */
public class BondAbstract {
  public final static String FILE = "bonds";
  public final static String SEPARATOR = "\t";
  public final static String SYMETRIC = "---";

  private String name;
  private BigDecimal bond;
  private BigDecimal diff;
  private BigDecimal[] mods;
  private BigDecimal[] diffs;
  private HashSet<Character>[] specificity;

  public BondAbstract(String name, String characteristic) {
    this.name = name;

    String[] parts = characteristic.split(SEPARATOR,9);

    try {
      bond = new BigDecimal(parts[0]);
    } catch (Exception e) {
      bond = ChemicalElement.evaluate(parts[0]);
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

    mods = new BigDecimal[2];
    for (int i = 0; i < mods.length; i++) {
      if (parts.length < 2*i+5 || parts[2*i+4].isEmpty()) {
        mods[i] = null;
      } else {
        try {
          mods[i] = new BigDecimal(parts[2*i+4]);
        } catch (Exception e) {
          mods[i] = ChemicalElement.evaluate(parts[2*i+4]);
        }
      }
    }

    diffs = new BigDecimal[2];
    for (int i = 0; i < diffs.length; i++) {
      if (parts.length < 2*i+6 || parts[2*i+5].isEmpty()) {
        diffs[i] = BigDecimal.ZERO;
      } else {
        try {
          diffs[i] = new BigDecimal(parts[2*i+5]);
        } catch (Exception e) {
          diffs[i] = ChemicalElement.evaluate(parts[2*i+5]);
        }
      }
    }

    specificity = new HashSet[2];
    for (int i = 0; i < 2; i++) {
      int j = i+2;
      if (parts.length <= j || parts[j].isEmpty()) {
        specificity[i] = null;
      } else {
        specificity[i] = new HashSet();
        for (char c : parts[j].toCharArray()) {
          if (Monomer.isMonomer(c) || Monomer.isTerminus(c)) {
            specificity[i].add(c);
          } else if (Monomer.isShortcut(c)) {
            for (char r : Monomer.resolveShortcut(c).toCharArray()) {
              specificity[i].add(r);
            }
          }
        }
      }
    }
    //specificity = new String[] { parts.length > 2 ? parts[2] : "", parts.length > 3 ? parts[3] : "" };

    if (parts[3].equals(SYMETRIC)) {
      specificity[1] = specificity[0];
      mods[1] = mods[0];
      diffs[1] = diffs[0];
    }
  }

  public String getName() {
    return name;
  }

  public BigDecimal getBond() {
    return bond;
  }

  public BigDecimal getCheckDiff() {
    return diff;
  }

  public BigDecimal[] getMods() {
    return mods;
  }

  public BigDecimal getMod(int i) {
    return mods[i];
  }

  public BigDecimal getCheckDiff(int i) {
    return diffs[i];
  }

  public boolean specific(int i, char ch) {
    return specificity[i] == null || specificity[i].contains(ch);
  }
}
