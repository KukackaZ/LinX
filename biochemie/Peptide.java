package biochemie;

import java.math.BigDecimal;
import java.util.Map;

/**
 *
 * @author Janek
 */
public class Peptide {
  private Protein protein;
  private Map<Integer, Character> fixed;
  private SimplifiedCleavage left;
  private SimplifiedCleavage right;
  private BigDecimal minMass;
  private BigDecimal maxMass;
  private BigDecimal mass;

  /**
   *
   * @param protein
   * @param begin Left bound, include.
   * @param end Right bound, exclude.
   * @param fixed
   * @param left
   * @param right
   */
  public Peptide(Protein protein, SimplifiedCleavage left, SimplifiedCleavage right, Map<Integer, Character> fixed, BigDecimal minMass, BigDecimal maxMass) {
    this.protein = protein;
    this.fixed = fixed;
    this.left = left;
    this.right = right;
    this.minMass = minMass;
    this.maxMass = maxMass;
  }

  public Peptide clone(Protein protein) {
    // Dokud má SimplifiedCleavage pouze get metody.
    return new Peptide(protein, left, right, new java.util.HashMap<>(fixed), minMass, maxMass);
  }

  public int getBegin() {
    return left.getPosition()+protein.start();
  }

  public int getEnd() {
    return right.getPosition()+protein.start()-1;
  }

  public int start() {
    return protein.start();
  }

  public int length() {
    return right.getPosition()-left.getPosition();
  }

  public boolean reset() {
    if (protein.reset(left, right, fixed)) {
      recomputeMass();
      return true;
    } else {
      mass = null;
      return false;
    }
  }

  public boolean reset(int position, char aminoacid) {
    if ((position < left.getPosition() && (aminoacid != '^' || position >= 0 || left.getPosition() != 0)) ||
        (right.getPosition() <= position && (aminoacid != '$' || position < protein.length() || right.getPosition() != protein.length())) ||
        (fixed.containsKey(position) && !fixed.get(position).equals(aminoacid)) ||
        (position == left.getPosition() && !left.getProtease().isFlexibleRight()) ||
        (position + 1 == right.getPosition() && !right.getProtease().isFlexibleLeft())) {
      return false;
    }
    protein.blockPosition(position, aminoacid);
    return reset();
  }

  public boolean nextConfiguration() {
    if (protein.nextConfiguration()) {
      mass = mass.add(protein.massDiff());
      return true;
    } else {
      mass = null;
      return false;
    }
  }

  public BigDecimal minMass() {
    return minMass;
  }

  public BigDecimal maxMass() {
    return maxMass;
  }

  public BigDecimal getMass() {
    return mass;
  }

  public java.util.HashSet<BigDecimal> getCheckDiff() {
    return protein.checkDiff();
  }

  private void recomputeMass() {
    mass = protein.massN(left.getPosition()).add(protein.massC(right.getPosition())).add(left.getProtease().getModificationRight()).add(right.getProtease().getModificationLeft());
    for (int i = 0; i < length(); i++) {
      mass = mass.add(protein.massAt(left.getPosition()+i));
    }
  }

  public String proteinName() {
    return protein.getName();
  }

  public Protein getProtein() {
    return protein;
  }

  public String toStringConfiguration() {
    return protein.substringConfiguration(left.getPosition(), right.getPosition());
  }

  public String toStringModifications() {
    return toStringModifications("");
  }

  public String toStringModifications(String prefix) {
    return protein.toStringModifications(prefix);
  }

  public String toStringBonds() {
    return toStringBonds("");
  }

  public String toStringBonds(String prefix) {
    return protein.toStringBonds(prefix);
  }

  @Override
  public int hashCode() {
    return protein.hashCode() + (left.getPosition()) + (left.getLevel() << 4) + (right.getPosition() << 8) + (right.getLevel() << 12);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof Protein && obj == protein && fixed.isEmpty() && left.getPosition() == 0 && right.getPosition() == protein.length()) {
      return true;
    }

    if (obj instanceof Peptide) {
      Peptide p2 = (Peptide)obj;
      if (protein.equals(p2.protein) && fixed.equals(p2.fixed) &&
             left.getPosition() == p2.left.getPosition() && left.getLevel() == p2.left.getLevel() && left.getProtease().equals(p2.left.getProtease()) &&
          right.getPosition() == p2.right.getPosition() && right.getLevel() == p2.right.getLevel() && right.getProtease().equals(p2.right.getProtease())) {
        return true;
      }
    }

    return false;
  }
}
