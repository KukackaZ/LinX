package biochemie;

import java.math.BigDecimal;

/**
 *
 * @author Janek
 */
public class ModificationReal implements Comparable<ModificationReal> {
  private String name;
  private BigDecimal modification;
  private BigDecimal check;
  private String protein;
  private char aa;
  private Integer position;

  public ModificationReal(String name, BigDecimal modification, String protein, char aa, Integer position) {
    this(name, modification, BigDecimal.ZERO, protein, aa, position);
  }

  public ModificationReal(String name, BigDecimal modification, BigDecimal check, String protein, char aa, Integer position) {
    this.name = name;
    this.modification = modification;
    this.check = check;
    this.protein = protein;
    this.aa = aa;
    this.position = position;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getModification() {
    return modification;
  }

  public BigDecimal getCheckDiff() {
    return check;
  }

  public String getProtein() {
    return protein;
  }

  public char getAa() {
    return aa;
  }

  public Integer getPosition() {
    return position;
  }

  public boolean equals(Object o) {
    if (!(o instanceof ModificationReal)) {
      return false;
    }
    ModificationReal mr = (ModificationReal)o;
    return (name.equals(mr.name) && modification.compareTo(mr.modification) == 0 && check.compareTo(mr.check) == 0 && protein.equals(mr.protein) && aa == mr.aa &&
            (position == mr.position || (position != null && position.equals(mr.position))));
  }

  public int compareTo(ModificationReal o) {
    if (name.compareTo(o.name) != 0) {
      return name.compareTo(o.name);
    }
    if (modification.compareTo(o.modification) != 0) {
      return modification.compareTo(o.modification);
    }
    if (check.compareTo(o.check) != 0) {
      return check.compareTo(o.check);
    }
    if (protein.compareTo(o.protein) != 0) {
      protein.compareTo(o.protein);
    }
    if (aa != o.aa) {
      return Character.compare(aa, o.aa);
    }
    if (position == null) {
      if (o.position != null) {
        return -1;
      }
    } else if (o.position == null) {
      return 1;
    } else if (position.compareTo(o.position) != 0) {
      return position.compareTo(o.position);
    }
    return 0;
  }
}
