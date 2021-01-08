package biochemie;

/**
 * Abstract class, ensuring behavior of peptide.
 * @author Janek
 */
public abstract class Polymer implements CharSequence {
  static final char MUTATION_SPLITTER = '/';

  /**
   * Test if string is chain of Amino Acids. The same as <code>IsAminoAcidChain(chain, false)</code>.
   * @param chain Tested chain.
   * @return True if chain is only from Amino Acids.
   */
  public static boolean isPolymer(String chain) {
    return isPolymer(chain, false);
  }
  /**
   * Test if string is chain of Amino Acids.
   * @param chain Tested chain.
   * @param mutations Whether mutations are allowed.
   * @return True if chain is only from Amino Acids.
   */
  public static boolean isPolymer(String chain, boolean mutations) {
    if (chain.isEmpty()) {
      return true;
    }
    for (int i = 0; i < chain.length(); i++) {
      if (!(Monomer.isMonomer(chain.charAt(i)) || (mutations && Monomer.isShortcut(chain.charAt(i))))) {
        if (!mutations || i == 0 || i == chain.length()-1 || (chain.charAt(i) == MUTATION_SPLITTER && chain.charAt(i+1) == MUTATION_SPLITTER)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Test whether the position could be bound of this peptide.
   * @param i Tested position.
   * @return True if the position could be bound of this peptide.
   */
  protected boolean checkBound(int i) {
    return i >= 0 && i <= length();
  }

  /**
   * Reset pointers on mutations in the specified windowexcept specified positions.
   * @param begin Where the window begins (inclusive).
   * @param end Where the window ends (exclusive).
   * @param fixed Positions that can be skipped.
   */
  abstract boolean reset(SimplifiedCleavage begin, SimplifiedCleavage end, java.util.Map<Integer, Character> fixed);
  /**
   * Overturn mutation configuration on next configuration.
   * @return <code>True</code> if next configuration exists, <code>false</code> otherwise.
   */
  abstract boolean nextConfiguration();
  /**
   * Return current configuration of mutations.
   * @return Current configuration.
   */
  abstract java.util.Map<Integer, Character> getConfigurationMutations();
  /**
   * Returns a new aminoacids chain that is a substring of this chain with current mutations.
   * The substring begins at the specified <ode>begin</code> and extends to the character at
   * index <code>end-1</code>. Thus the length of the substring is <code>end-begin</code>.
   * @param begin The beginning index, inclusice.
   * @param end The ending index, exclusive.
   * @return The specified substring.
   */
  abstract String substringConfiguration(int begin, int end);
  /**
   * Returns a string representation of the object with current configuration of mutations.
   * @return Returns a string representation of the object.
   */
  abstract String toStringConfiguration();
  /**
   * Returns modifications set in the current configuration.
   * @return Modifications set in the current configuration.
   */
  abstract String toStringModifications(String prefix);
  /**
   * Returns bonds set in the current configuration.
   * @return Bonds set in the current configuration.
   */
  abstract String toStringBonds(String prefix);
}
