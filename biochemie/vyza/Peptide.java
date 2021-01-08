package biochemie.vyza;

/**
 *
 * @author Janek
 */
public class Peptide {
  private int begin;
  private int end;
  private String[] info;

  public Peptide(int begin, int end, String[] info) {
    this.begin = begin;
    this.end = end;
    this.info = info;
  }

  public int getBegin() {
    return begin;
  }

  public int getEnd() {
    return end;
  }
  
  public int length() {
    return end-begin;
  }

  public String[] getInfo() {
    return info;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof Peptide)) {
      return false;
    }
    if (hashCode() != obj.hashCode()) {
      return false;
    }
    Peptide o = (Peptide)obj;
    if (begin != o.begin || end != o.end || info.length != o.info.length) {
      return false;
    }
    for (int i = 0; i < info.length; i++) {
      if (!info[i].equals(o.info[i])) {
        return false;
      }
    }
    return true;
  }

  public int hashCode() {
    int code = begin << 16 + end;
    if (info != null) for (String string : info) {
      code ^= string.hashCode();
    }
    return code;
  }
  
  
}
