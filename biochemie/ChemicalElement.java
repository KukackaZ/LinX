package biochemie;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Janek
 */
public class ChemicalElement {
  // http://physics.nist.gov/cgi-bin/cuu/Value?mpu
  // http://www.iupac.org/publications/pac/2003/pdf/7506x0683.pdf
  public static final String FILE = "elements";
  private static HashMap<String, BigDecimal> masses;
  private static ArrayList<ChangeListener> listeners;

  static {
    listeners = new ArrayList();
    load();
  }

  public static void reload() {
    masses.clear();
    load();
  }

  public static void addChangeListener(ChangeListener listener) {
    listeners.add(listener);
  }
  
  private static void load() {
    Properties p = Defaults.loadDefaults(FILE);
    masses = new HashMap(p.size());
    masses.put("+", new BigDecimal("1.007276466812"));
    for (String key : p.stringPropertyNames()) {
      try {
        masses.put(key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase(), new BigDecimal(p.getProperty(key)));
      } catch (Exception e) { }
    }
    if (p.stringPropertyNames().isEmpty()) {
      masses.put("H",  new BigDecimal("1.0078250319"));
      masses.put("D",  new BigDecimal("2.014101779"));
      masses.put("C", new BigDecimal("12.0000000000"));
      masses.put("N", new BigDecimal("14.0030740074"));
      masses.put("O", new BigDecimal("15.9949146223"));
      masses.put("P", new BigDecimal("30.97376149"));
      masses.put("S", new BigDecimal("31.97207073"));
      masses.put("Se", new BigDecimal("79.9165221"));
    }
    
    for (ChangeListener changeListener : listeners) {
      changeListener.stateChanged(new ChangeEvent(ChemicalElement.class));
    }
  }

  public static BigDecimal mass(String c) {
    return masses.get(c);
  }

  public static BigDecimal evaluate(String s) {
    if (s == null || s.isEmpty()) {
      return BigDecimal.ZERO;
    }
    BigDecimal w = BigDecimal.ZERO;
    for (String string : s.split("(?<=.)(?=[+-])")) {
      if (string.charAt(0) == '-') {
        w = w.subtract(compute(string.substring(1)));
      } else {
        w = w.add(compute(string.charAt(0) == '+' ? string.substring(1) : string));
      }
    }
    return w;
  }

  private static BigDecimal compute(String s) {
    if (!s.matches("[A-Za-z0-9()\\[\\]{}]*")) {
      Matcher m = Pattern.compile("[^A-Za-z0-9()\\[\\]{}]").matcher(s);
      m.find();
      throw new java.util.regex.PatternSyntaxException("Symbol '" + m.group() + "' isn't allowed.", s, m.start());
    }
    if (s.contains("()") || s.contains("[]") || s.contains("{}")) {
      throw new java.util.regex.PatternSyntaxException("Empty group isn't allowed.", s, s.replaceAll("((\\(\\))|(\\[\\])|(\\{\\})).*$", "").length());
    }
    ArrayList<Character> tc = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      switch (s.charAt(i)) {
        case '(':
        case '[':
        case '{':
          tc.add(s.charAt(i));
          break;
        case ')':
        case ']':
        case '}':
          if (tc.size() == 0) {
            throw new java.util.regex.PatternSyntaxException("Closing bracket without opening bracket.", s, i);
          }
          if (s.charAt(i) - tc.get(tc.size()-1) >= 0 && s.charAt(i) - tc.get(tc.size()-1) <= 2) {
            tc.remove(tc.size()-1);
          } else {
            throw new java.util.regex.PatternSyntaxException("Unexpected closing bracket.", s, i);
          }
          break;
      }
    }

    String[] parts = s.split("(?<!^)(?=[A-Z()\\[\\]{}])");
    ArrayList<BigDecimal> stack = new ArrayList<>();
    stack.add(BigDecimal.ZERO);
    int i = 0;
    for (String string : parts) {
      if (string.matches("[A-Z][a-z]*[0-9]*")) {
        String el = string.replaceAll("[0-9]", "");
        if (masses.containsKey(el)) {
          stack.add(stack.remove(stack.size()-1).add(masses.get(el).multiply(string.length() == el.length() ? BigDecimal.ONE : new BigDecimal(string.substring(el.length())))));
        } else {
          throw new java.util.regex.PatternSyntaxException("Unknown element '" + el + "'.", s, i);
        }
      } else if (string.matches("[)\\]}][0-9]*")) {
        stack.add(stack.remove(stack.size() - 1).multiply(string.matches(".*[0-9]") ? new BigDecimal(string.replaceAll("[^0-9]", "")) : BigDecimal.ONE).add(stack.remove(stack.size() - 1)));
      } else if (string.matches("[(\\[{]")) {
        stack.add(BigDecimal.ZERO);
      } else {
        throw new java.util.regex.PatternSyntaxException("Unexpected element '" + string + "'.", s, i);
      }
      i += string.length();
    }
    return stack.get(0);
  }
}
