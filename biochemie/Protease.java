package biochemie;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.regex.*;

/**
 *
 * @author Janek
 */
public class Protease {
  //<editor-fold desc=" Non-static section ">
  private String name;
  private String proteaseLeft;
  private String proteaseRight;
  private int lengthLeft;
  private int lengthRight;
  private boolean anchoredLeft;
  private boolean anchoredRight;
  private BigDecimal modificationLeft;
  private BigDecimal modificationRight;
  private boolean lockLeft;
  private boolean lockRight;
  private boolean ignoreMods;

  /**
   * Initialize newly created <code>ProteaseOld</code>.
   * @param protease Specification of cleavage.
   */
  public Protease(String name, String protease) {
    this(name, protease, "", true);
  }

  public Protease(String name, String protease, String modifications, boolean ignoreMods) {
    this.name = name;
    String[] sides = checkRules(splitRule(protease));
    this.lengthLeft = lengthOfRule(sides[0]);
    this.anchoredLeft = classifySide(sides[1]) == ProteaseClass.ANCHORED;
    this.lengthRight = lengthOfRule(sides[1]);
    this.anchoredRight = classifySide(sides[0]) == ProteaseClass.ANCHORED;
    this.proteaseLeft  = sides[0];
    this.proteaseRight = sides[1];
    this.modificationLeft = BigDecimal.ZERO;
    this.modificationRight = BigDecimal.ZERO;
    this.lockLeft = false;
    this.lockRight = false;
    this.ignoreMods = ignoreMods;

    if (!modifications.isEmpty()) {
      String[] parts = modifications.split(SEPARATOR, 2);
      if (!parts[0].isEmpty()) {
        try {
          modificationLeft = ChemicalElement.evaluate(parts[0].replace(LOCK, ""));
        } catch (Exception e) {
          try {
            modificationLeft = new BigDecimal(parts[0].replace(LOCK, ""));
          } catch (Exception f) { }
        }
        lockLeft = parts[0].contains(LOCK);
      }
      if (!parts[1].isEmpty()) {
        try {
          modificationRight = ChemicalElement.evaluate(parts[1].replace(LOCK, ""));
        } catch (Exception e) {
          try {
            modificationRight = new BigDecimal(parts[1].replace(LOCK, ""));
          } catch (Exception f) { }
        }
        lockRight = parts[1].contains(LOCK);
      }
    }
  }

  public String getName() {
    return name;
  }

  /**
   * Returns specification of the neighbourhood of the cleavage.
   * @return Specification of cleavage.
   */
  public String getRuleWithoutSeparator() {
    return proteaseLeft + proteaseRight;
  }

  /**
   * Returns specification of cleavage.
   * @return Specification of cleavage.
   */
  public String getRule() {
    return proteaseLeft + SEPARATOR + proteaseRight;
  }

  /**
   * Returns specification of left bound of cleavage.
   * @return Specification of left bound of cleavage.
   */
  public String getRuleLeft() {
    return proteaseLeft;
  }

  /**
   * Returns specification of right bound of cleavage.
   * @return Specification of right bound of cleavage.
   */
  public String getRuleRight() {
    return proteaseRight;
  }

  public BigDecimal getModificationLeft() {
    return modificationLeft;
  }

  public BigDecimal getModificationRight() {
    return modificationRight;
  }

  public boolean isLockedLeft() {
    return lockLeft;
  }

  public boolean isLockedRight() {
    return lockRight;
  }

  public int getLengthLeft() {
    return lengthLeft;
  }

  public int getLengthRight() {
    return lengthRight;
  }

  public boolean isFlexibleLeft() {
    return !isLockedLeft() && (ignoreMods || lengthLeft == 0 || proteaseLeft.endsWith(".") || proteaseLeft.matches(".*\\[\\^[^\\[\\]]+\\]$"));
  }

  public boolean isFlexibleRight() {
    return !isLockedRight() && (ignoreMods || lengthRight == 0 || proteaseRight.startsWith(".") || proteaseRight.startsWith("[^"));
  }

  /**
   * Returns complexity class of left bound of cleavage.
   * @return Complexity class of left bound of cleavage.
   */
  public ProteaseClass getRuleClass() {
    if (anchoredLeft || anchoredRight) {
      return ProteaseClass.ANCHORED;
    } else if (lengthLeft > 1 || lengthRight > 1) {
      return ProteaseClass.CONSTANT_LENGTH;
    } else if (lengthLeft == 0 && lengthRight == 0) {
      return ProteaseClass.EMPTY;
    } else {
      return ProteaseClass.ONE_CHAR;
    }
  }

  /**
   * Returns complexity class of left bound of cleavage.
   * @return Complexity class of left bound of cleavage.
   */
  public ProteaseClass getLeftClass() {
    if (anchoredLeft) {
      return ProteaseClass.ANCHORED;
    } else if (lengthLeft == 1) {
      return ProteaseClass.ONE_CHAR;
    } else if (lengthLeft == 0) {
      return ProteaseClass.EMPTY;
    } else {
      return ProteaseClass.CONSTANT_LENGTH;
    }
  }

  /**
   * Returns complexity class of right bound of cleavage.
   * @return Complexity class of right bound of cleavage.
   */
  public ProteaseClass getRightClass() {
    if (anchoredRight) {
      return ProteaseClass.ANCHORED;
    } else if (lengthRight == 1) {
      return ProteaseClass.ONE_CHAR;
    } else if (lengthRight == 0) {
      return ProteaseClass.EMPTY;
    } else {
      return ProteaseClass.CONSTANT_LENGTH;
    }
  }
  //</editor-fold>

  //<editor-fold desc=" STATIC section ">
  public static final String SEPARATOR = ";";
  public static final String LOCK = "&";

  /**
   * Classes for selection of best algorithm for cutting.
   */
  public enum ProteaseClass { EMPTY, ONE_CHAR, CONSTANT_LENGTH, ANCHORED }

  /**
   * Tests complexity of protease's regexp.
   * @param protease Regexp for classification.
   * @return Return class of regexp.
   */
  public static ProteaseClass classifySide(String protease) {
    // Odfiltrování nejběnžjších případů, ať u nich detekce proběhne rychle a není potřeba se zdržovat těmi teoretickými
    if (protease.length() == 0 || protease.compareTo(".") == 0) {
      return ProteaseClass.EMPTY;
    }
    // Ukotvení pravidla
    if (protease.charAt(0) == '^' || protease.charAt(protease.length()-1) == '$') {
      return ProteaseClass.ANCHORED;
    }
    // Jeden znak; [A-Z&&[^BJOXZ]]
    if (protease.length() == 1 || protease.matches("\\[[\\^A-Z\\-\\&\\[\\]]+\\]")) {
      return ProteaseClass.ONE_CHAR;
    }
    // Zbytek musí být delší
    return ProteaseClass.CONSTANT_LENGTH;
  }

  /**
   * Finds the worst class in proteases collection.
   * @param proteases Proteases.
   * @return Worst class or null if collection is empty.
   */
  public static ProteaseClass worstProteaseClass(Collection<Collection<Protease>> proteases) {
    ProteaseClass worst = null;
    for (Collection<Protease> level : proteases) {
      for (Protease rule : level) {
        // Speciální případ, snad by mělo platit i v aktuální verzi počítání m-c.
        ProteaseClass cl;
        if (rule.anchoredLeft || rule.anchoredRight) {
          cl = ProteaseClass.ANCHORED;
        } else if (rule.lengthLeft > 1 || rule.lengthRight > 1) {
          cl = ProteaseClass.CONSTANT_LENGTH;
        } else if (rule.lengthLeft == 0 && rule.lengthRight == 0 && !rule.lockLeft && !rule.lockRight && rule.modificationLeft.compareTo(BigDecimal.ZERO) == 0 && rule.modificationRight.compareTo(BigDecimal.ZERO) == 0) {
          cl = ProteaseClass.EMPTY;
        } else {
          cl = ProteaseClass.ONE_CHAR;
        }
        if (worst == null || worst.compareTo(cl) < 0) {
          worst = cl;
        }
      }
    }
    return worst;
  }

  /**
   * Tests complexity of protease's regexp.
   * @param protease Regexp for classification.
   * @return Return class of regexp.
   */
  public static int lengthOfRule(String protease) {
    // Odfiltrování nejběžnějších případů, ať u nich detekce proběhne rychle a není potřeba se zdržovat těmi teoretickými
    if (protease.length() == 0 || protease.compareTo(".") == 0) {
      return 0;
    }
    // Jeden znak; [A-Z&&[^BJOXZ]]; G|L|I
    if (protease.length() == 1 || protease.matches("\\[[\\^A-Z\\-\\&\\[\\]]+\\]")) {
      return 1;
    }

    //# Zachycení ^, $ a \b
    if (protease.contains("$")) {
      protease = protease.replaceAll("\\$", "");
    }
    if (protease.charAt(0) == '^') {
      protease = protease.substring(1);
    }

    // Odfiltrování [..], neboť není důležité, co konkrétně chytí, jen že to je jeden znak.
    while (protease.matches(".*\\[[^\\[\\]]+\\].*")) {
      protease = protease.replaceAll("\\[[^\\[\\]]+\\]", "A");
    }
    // Nahrazení .{n} n znaky.
    Pattern pattern = Pattern.compile("([A-Z.])\\{(\\d+)\\}");
    Matcher matcher = pattern.matcher(protease);
    while (matcher.find()) {
      protease = protease.replace(matcher.group(), repeat(matcher.group(1), Integer.parseInt(matcher.group(2))));
      matcher = pattern.matcher(protease);
    }

    return protease.length();
  }

  /**
   * Repeat input string <i>n</i>-times.
   * @param string String for multiplication.
   * @param repeats Number of repeats.
   * @return Return String string+string+...+string (<i>n</i>-times).
   */
  private static String repeat(String string, int repeats) {
    StringBuilder ret = new StringBuilder(string.length() * repeats);
    for (int i = 0; i < repeats; i++) {
      ret.append(string);
    }
    return ret.toString();
  }

  /**
   * Clean rule.
   * @param rule Rule for clean.
   * @return Cleaned rule.
   * @throws java.util.regex.PatternSyntaxException
   */
  private static String[] checkRules(String[] rule) throws java.util.regex.PatternSyntaxException {
    String position = "(\\.)|([A-Z])|(\\[\\^?[A-Z-]{1," + (Byte.MAX_VALUE-1) + "}\\])";
    for (int i = 0; i < rule.length; i++) {
      if (!rule[i].matches("(" + position + ")*")) {
        int end = 0;
        Matcher m = Pattern.compile(position).matcher(rule[i]);
        while (m.find()) {
          end = m.end();
        }
        throw new java.util.regex.PatternSyntaxException("Invalid definition of position", rule[i], end);
      }
    }

//    // Kdyžtak později umožnit víc, například opakování, závorky (ale pro reprezentaci je rovinout)...
//    rule[0] = rule[0].replace("\\b", "^");
//    rule[1] = rule[1].replace("\\b", "$");
//    for (int i = 0; i < rule.length; i++) {
//      // ..{n,n} -> ..{n}
//      rule[i] = rule[i].replaceAll("\\{([0-9]+),\\1\\}", "$1");
//      // ..{1} -> ..
//      rule[i] = rule[i].replace("{1}", "");
//      // ^c -> [^c]
//      rule[i] = rule[i].replaceAll("(?<!\\[)(\\^[A-Z])", "[$1]");
//      if (rule[i].equals("")) {
//        rule[i] = ".";
//      }
//    }
//
//    // TODO: Zduplikování {n}, odstaranění závorek
//
//    for (String part : rule) {
//      if (!part.matches("^[.A-Z\\-\\[\\]^${}0-9]*$")) {
//        Matcher m = Pattern.compile("[^.A-Z\\-\\[\\]^${}0-9]").matcher(part);
//        m.find();
//        throw new java.util.regex.PatternSyntaxException("Rule contains forbidden character.", part, m.start());
//      }
//      // Že '^' je jen na začátku vzoru nebo jako [^..]
//      if (part.matches(".*[^\\[]^.*")) {
//        throw new java.util.regex.PatternSyntaxException("'^' isn't allowed on this position.", part, part.indexOf('^', 1));
//      }
//      // $ je jen na konci vzoru
//      if (part.contains("$") && part.indexOf('$') < part.length()-1) {
//        throw new java.util.regex.PatternSyntaxException("'$' isn't allowed on this position.", part, part.indexOf('$'));
//      }
//      // Že čísla jsou jen v konstrukci {n}
//      if (part.matches(".*((?![{0-9])[0-9])|([0-9](?![}0-9])).*")) {
//        Matcher m = Pattern.compile("((?![{0-9])[0-9])|([0-9](?![}0-9]))").matcher(part);
//        m.find();
//        throw new java.util.regex.PatternSyntaxException("This usage of numbers isn't allowed.", part, m.start());
//      }
//
//      if (part.matches("(^|(.*\\]))[^\\[]*(-)[^\\]]*((\\[.*)|$)")) {
//        Matcher m = Pattern.compile("(^|(.*\\]))[^\\[]*(-)[^\\]]*((\\[.*)|$)").matcher(part);
//        m.find();
//        throw new java.util.regex.PatternSyntaxException("'-' cannot be used instead of amino acid.", part, m.start(3));
//      }
//    }

    return rule;
  }

  /**
   * Split rule with seperator.
   * @param rule Rule for split.
   * @return Sides of rule.
   * @throws java.util.regex.PatternSyntaxException
   */
  private static String[] splitRule(String rule) throws java.util.regex.PatternSyntaxException {
    String[] ret = rule.split(SEPARATOR, -1);
    switch (ret.length) {
      case 2:
        try {
          Pattern.compile(ret[0]);
          Pattern.compile(ret[1]);
        } catch (java.util.regex.PatternSyntaxException e) {
          throw new java.util.regex.PatternSyntaxException("Incorrect usage of cleavage symbol.", rule, ret[0].length());
        }
        return ret;
      case 0:
      case 1:
        throw new java.util.regex.PatternSyntaxException("Missing cleavage symbol.", rule, -1);
      default:
        throw new java.util.regex.PatternSyntaxException("Too many cleavage symbols in the top level of rule.", rule, ret[0].length() + 1 + ret[1].length());
    }
  }

  /**
   * Test if is the rule valid definition of protease.
   * @param rule Rule for testing.
   * @throws java.util.regex.PatternSyntaxException Throws if rule isn't valid.
   */
  public static void checkProtease(String rule) throws java.util.regex.PatternSyntaxException {
    checkRules(splitRule(rule));
  }

  public static String formatMods(String mods, boolean full) {
    if (!(mods == null || mods.isEmpty())) {
      String[] parts = mods.split(SEPARATOR, 2);
      for (int i = 0; i < 2; i++) {
        String part = parts[i].replace(LOCK, "");
        if (!part.isEmpty()) {
          try {
            parts[i] = parts[i].replace(part, full ? Defaults.sMassFullFormat.format(new BigDecimal(part)) : Defaults.sMassShortFormat.format(new BigDecimal(part)));
          } catch (Exception e) {
            try {
              parts[i] = parts[i].replace(part, full ? Defaults.sMassFullFormat.format(new BigDecimal(Defaults.sMassFullFormat.getPositivePrefix() + part)) :
                                                       Defaults.sMassShortFormat.format(new BigDecimal(Defaults.sMassShortFormat.getPositivePrefix() + part)));
            } catch (Exception f) {
              try {
                if (!full) {
                  parts[i] = parts[i].replace(part, Defaults.sMassShortFormat.format(ChemicalElement.evaluate(part)));
                }
              } catch (Exception g) { }
            }
          }
        }
      }
      mods = parts[0] + SEPARATOR + parts[1];
    }
    return mods;
  }
  //</editor-fold>
}
