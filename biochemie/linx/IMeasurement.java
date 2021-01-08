package biochemie.linx;

/**
 *
 * @author Janek
 */
public interface IMeasurement {
  public FileName getFileName();
  public double getIntensity();
  public String getRetentionTime();
  public String getRest();
}
