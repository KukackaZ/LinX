package biochemie;

/**
 * Information about cleavage: &lt;position, left bound, right bound, order in cleavaging of protein&gt;.
 * @author Janek
 */
public class Cleavage extends SimplifiedCleavage {
  private int left;
  private int right;

  /**
   * Initialize newly created <code>Cleavage</code>.
   * @param position Position of cleavage in peptide.
   * @param left Length of left bound, which is necessary for cleaving.
   * @param right Length of left bound, which is necessary for cleaving.
   * @param level Index of level, when it was applicable.
   */
  public Cleavage(int position, java.util.Map<Integer, Character> leftMutations, java.util.Map<Integer, Character> rightMutations, int left, int right,
                  int level, Protease protease) {
    super(position, leftMutations, rightMutations, level, protease);
    this.left = left;
    this.right = right;
  }

  public Cleavage (int position, int left, int right, int level, Protease protease) {
    this(position, new java.util.HashMap<Integer, Character>(0), new java.util.HashMap<Integer, Character>(0), left, right, level, protease);
  }

  /**
   * Returns left bound of cleavage.
   * @return Left bound of cleavage.
   */
  public int getLeft() {
    return left;
  }

  /**
   * Returns right bound of cleavage.
   * @return Right bound of cleavage.
   */
  public int getRight() {
    return right;
  }

  @Override
  public String toString() {
    return "(" + position + "-" + left + "; " + position + "+" + right + ") [" + level + "]";
  }
}
