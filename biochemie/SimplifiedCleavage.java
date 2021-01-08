package biochemie;

import java.util.Map;

/**
 * Information about cleavage: &lt;position, left bound, right bound, order in cleavaging of protein&gt;.
 * @author Janek
 */
public class SimplifiedCleavage {
  protected int position;
  protected Map<Integer, Character> leftMutations;
  protected Map<Integer, Character> rightMutations;
  protected Protease protease;
  protected int level;

  /**
   * Initialize newly created <code>Cleavage</code>.
   * @param position Position of cleavage in peptide.
   * @param leftMutations Enforced mutations on the left side of cleavage.
   * @param rightMutations Enforced mutations on the right side of cleavage.
   */
  public SimplifiedCleavage(int position, Map<Integer, Character> leftMutations, Map<Integer, Character> rightMutations, int level, Protease protease) {
    this.position = position;
    this.leftMutations = leftMutations;
    this.rightMutations = rightMutations;
    this.level = level;
    this.protease = protease;
  }

  /**
   * Returns position of cleavage.
   * @return Position of cleavage.
   */
  public int getPosition() {
    return position;
  }

  /**
   * Returns enforced mutations on the left side of cleavage.
   * @return Enforced mutations on the left side of cleavage.
   */
  public Map<Integer, Character> getLeftMutations() {
    return leftMutations;
  }

  /**
   * Returns enforced mutations on the right side of cleavage.
   * @return Enforced mutations on the right side of cleavage.
   */
  public Map<Integer, Character> getRightMutations() {
    return rightMutations;
  }

  /**
   * Returns protease that cleaves on this position.
   * @return Protease that cleaves on this position.
   */
  public Protease getProtease() {
    return protease;
  }

  /**
   * Returns index of level, when it was applicable.
   * @return Level of cleaving.
   */
  public int getLevel() {
    return level;
  }

  @Override
  public String toString() {
    return "(" + position + ")";
  }
}
