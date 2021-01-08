package biochemie.linx;

import biochemie.Defaults;

/**
 *
 * @author Janek
 */
public class MeasurementAlternatives implements IMeasurement {
  FileName fileName;
  double[] masses;
  double[] errors;
  String[] proteins;
  String[] chains;
  String[][] mods;
  String[][] bonds;

  public MeasurementAlternatives(FileName fileName, double[] masses, double[] errors, String[] proteins, String[] chains, String[][] mods, String[][] bonds) {
    this.fileName = fileName;
    this.masses = masses;
    this.errors = errors;
    this.proteins = proteins;
    this.chains = chains;
    this.mods = mods;
    this.bonds = bonds;
  }

  public FileName getFileName() {
    return fileName;
  }

  public void setFileName(FileName fileName) {
    this.fileName = fileName;
  }

  public double getIntensity() {
    return -1;
  }

  public String getRetentionTime() {
    return "";
  }

  public String getRest() {
    StringBuilder sb = new StringBuilder();
    for (String rest : getRests()) {
      sb.append(System.lineSeparator()).append(rest);
    }
    return sb.substring(System.lineSeparator().length());
  }

  public String[] getRests() {
    String[] ret = new String[masses.length];
    for (int i = 0; i < ret.length; i++) {
      if (Double.isNaN(masses[i])) {
        ret[i] = "";
      } else {
        ret[i] = Defaults.uMassShortFormat.format(masses[i]) + "\t";
        if (!Double.isNaN(errors[i])) {
          ret[i] += Defaults.sPpmFormat.format(errors[i]) + "\t";
        }
        ret[i] += proteins[i] + "\t" + chains[i] + "\t" + mods[i][0];
        for (int j = 1; j < mods[i].length; j++) {
          ret[i] += " | " + mods[i][j];
        }
        ret[i] += "\t" + bonds[i][0];
        for (int j = 1; j < bonds[i].length; j++) {
          ret[i] += " | " + bonds[i][j];
        }
      }
    }
    return ret;
  }

  public boolean contains(Double mass, Double error, String protein, String chain, String mod, String bond) {
    for (int i = 0; i < masses.length; i++) {
//      if (Defaults.massShortFormat.format(masses[i]) == null ? Defaults.massShortFormat.format(mass) != null
//                                                             : !Defaults.massShortFormat.format(masses[i]).equals(Defaults.massShortFormat.format(mass))) {
//        continue;
//      }
//      if (Defaults.errorFormat.format(errors[i]) == null ? Defaults.errorFormat.format(error) != null
//                                                         : !Defaults.errorFormat.format(errors[i]).equals(Defaults.errorFormat.format(error))) {
//        continue;
//      }
      if (chains[i] == null ? chain != null : !chains[i].equals(chain)) {
        continue;
      }
      if (proteins[i] == null ? protein != null : !proteins[i].equals(protein)) {
        continue;
      }
      boolean skip = true;
      for (String modi : mods[i]) {
        if (modi == null ? mod == null : modi.equals(mod)) {
          skip = false;
          break;
        }
      }
      if (skip) {
        continue;
      }
      for (String bondi : bonds[i]) {
        if (bondi == null ? bond == null : bondi.equals(bond)) {
          return true;
        }
      }
    }
    return false;
  }

  public int hashCode() {
    return fileName.hashCode() + ((Double)masses[0]).hashCode();
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MeasurementAlternatives)) {
      return false;
    }
    MeasurementAlternatives meas = (MeasurementAlternatives)obj;
    if (!fileName.equals(meas.fileName) || masses.length != meas.masses.length || errors.length != meas.errors.length ||
        proteins.length != meas.proteins.length || chains.length != meas.chains.length || mods.length != meas.mods.length || bonds.length != meas.bonds.length) {
      return false;
    }
    // TODO Nezávislé na přeuspořádání
    for (int i = 0; i < masses.length; i++) {
      if (masses[i] != meas.masses[i] || errors[i] != meas.errors[i] || (proteins[i] == null ? meas.proteins[i] != null : !proteins[i].equals(meas.proteins[i])) ||
          (chains[i] == null ? meas.chains[i] != null : !chains[i].equals(meas.chains[i])) || mods[i].length != meas.mods[i].length || bonds[i].length != meas.bonds[i].length) {
        return false;
      }
      for (int j = 0; j < mods[i].length; j++) {
        if (mods[i][j] == null ? meas.mods[i][j] != null : !mods[i][j].equals(meas.mods[i][j])) {
          return false;
        }
      }
      for (int j = 0; j < bonds[i].length; j++) {
        if (bonds[i][j] == null ? meas.bonds[i][j] != null : !bonds[i][j].equals(meas.bonds[i][j])) {
          return false;
        }
      }
    }
    return true;
  }
}
