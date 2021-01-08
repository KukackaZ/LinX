package biochemie;

import java.math.BigDecimal;

/**
 *
 * @author Janek
 */
public class BondReal implements Comparable<BondReal> {
  private String name;
  private BigDecimal bond;
  private BigDecimal check;
  private String[] proteins;
  private char[] aas;
  private Integer[] positions;

  public BondReal(String name, BigDecimal bond, BigDecimal check, String proteinLeft, char aaLeft, Integer positionLeft, String proteinRight, char aaRight, Integer positionRight) {
    this.name = name;
    this.bond = bond;
    this.check = check;
    if (proteinRight == null) {
      this.proteins = new String[] { proteinLeft };
    } else {
      this.proteins = new String[] { proteinLeft, proteinRight };
    }
    this.aas = new char[] { aaLeft, aaRight };
    this.positions = new Integer[] { positionLeft, positionRight };
  }

  public BondReal reverse() {
    if (proteins.length == 1) {
      return new BondReal(name, bond, check, proteins[0], aas[1], positions[1], null, aas[0], positions[0]);
    } else {
      return new BondReal(name, bond, check, proteins[1], aas[1], positions[1], proteins[0], aas[0], positions[0]);
    }
  }

  public String getName() {
    return name;
  }

  public BigDecimal getBond() {
    return bond;
  }

  public BigDecimal getCheckDiff() {
    return check;
  }

  public String getProteinLeft() {
    return proteins[0];
  }

  public String getProteinRight() {
    return proteins.length == 1 ? proteins[0] : proteins[1];
  }

  public boolean withinOneMolecule() {
    return proteins.length == 1;
  }

  public char getAaLeft() {
    return aas[0];
  }

  public char getAaRight() {
    return aas[1];
  }

  public Integer getPositionLeft() {
    return positions[0];
  }

  public Integer getPositionRight() {
    return positions[1];
  }

  public int hashCode(){
    return name.hashCode() + proteins[0].hashCode()*3 + (proteins.length == 1 ? 0 : proteins[1].hashCode()*5) +
           (positions[0] == null ? -1 : positions[0]) + (positions[1] == null ? -1 : positions[1])*4 + aas[0]*16 + aas[1]*64;
  }

  public boolean equals(Object o) {
    if (!(o instanceof BondReal)) {
      return false;
    }
    BondReal br = (BondReal)o;
    if (!(name.equals(br.name) && bond.compareTo(br.bond) == 0 && check.compareTo(br.check) == 0 && proteins.length == br.proteins.length)) {
      return false;
    }
    for (int i = 0; i < proteins.length; i++) {
      if (!proteins[i].equals(br.proteins[i])) {
        return false;
      }
    }
    for (int i = 0; i < 2; i++) {
      if (!(aas[i] == br.aas[i] && (positions[i] == br.positions[i] || (positions[i] != null && positions[i].equals(br.positions[i]))))) {
        return false;
      }
    }
    return true;
  }

  public int compareTo(BondReal o) {
    if (name.compareTo(o.name) != 0) {
      return name.compareTo(o.name);
    }
    if (bond.compareTo(o.bond) != 0) {
      return bond.compareTo(o.bond);
    }
    if (check.compareTo(o.check) != 0) {
      return check.compareTo(o.check);
    }
    for (int i = 0; i < 2; i++) {
      if (i == 1 && (proteins.length == 1 || o.proteins.length == 1)) {
        if (proteins.length != o.proteins.length) {
          return Integer.compare(proteins.length, o.proteins.length);
        }
      } else {
        if (proteins[i].compareTo(o.proteins[i]) != 0) {
          return proteins[i].compareTo(o.proteins[i]);
        }
      }
      if (aas[i] != o.aas[i]) {
        return Character.compare(aas[i], o.aas[i]);
      }
      if (positions[i] == null) {
        if (o.positions[i] != null) {
          return -1;
        }
      } else if (o.positions[i] == null) {
        return 1;
      } else if (positions[i].compareTo(o.positions[i]) != 0) {
        return positions[i].compareTo(o.positions[i]);
      }
    }
    return 0;
  }
}
