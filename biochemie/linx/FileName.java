package biochemie.linx;

/**
 *
 * @author Janek
 */
public class FileName {
  String fileName;
  String format;

  public FileName(String fileName, String format) {
    this.fileName = fileName;
    this.format = format;
  }

  public FileName(String fileName) {
    this.fileName = fileName;
    this.format = "";
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFormat() {
    return format;
  }

  public String toString() {
    if (fileName == null) {
      return null;
    }
    if (format == null) {
      return fileName;
    }
    return fileName + ' ' + format;
  }

  public int hashCode() {
    return (fileName == null ? -255 : fileName.hashCode()) + (format == null ? -65280 : format.hashCode());
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FileName)) {
      return false;
    }
    FileName fn = (FileName)obj;
    return (fileName == fn.fileName || (fileName != null && fileName.equals(fn.fileName))) &&
           (format == fn.format || (format != null && format.equals(fn.format)));
  }
}
