package biochemie.linx;

/**
 *
 * @author Janek
 */
public class Measurement implements IMeasurement {
  FileName fileName;
  double intensity;
  String retentionTime;
  String rest;

  public Measurement(FileName fileName, double intensity, String retentionTime, String rest) {
    this.fileName = fileName;
    this.intensity = intensity;
    this.retentionTime = retentionTime;
    this.rest = rest;
  }

  public FileName getFileName() {
    return fileName;
  }

  public double getIntensity() {
    return intensity;
  }

  public String getRetentionTime() {
    return retentionTime;
  }

  public String getRest() {
    return rest;
  }

  public int hashCode() {
    return (fileName == null ? -255 : fileName.hashCode()) + (retentionTime == null ? -65280 : retentionTime.hashCode()) +
           (rest == null ? -16711680 : rest.hashCode()) + ((Double)intensity).hashCode();
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Measurement)) {
      return false;
    }
    Measurement meas = (Measurement)obj;
    return fileName.equals(meas.fileName) && intensity == meas.intensity && retentionTime.equals(meas.retentionTime) && rest.equals(meas.rest);
  }
}
