package biochemie;

import biochemie.dbedit.SettingsPanel;
import java.awt.Window;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author Janek
 */
public abstract class Defaults {
  private static final String DEFAULTS = "General";
  private static final String FOLDER = "Properties";
  private static final String FILE = "settings";
  private static final String EXTENSION = ".prs";
  public static final String SEPARATOR = "\t";
  public static Properties PROPERTIES;
  public static Locale LOCALE;
  public static final Charset CHARSET = Charset.forName("UTF-8");
  public static final DecimalFormat uMassFullFormat;
  public static final DecimalFormat uMassShortFormat;
  public static final DecimalFormat sMassFullFormat;
  public static final DecimalFormat sMassShortFormat;
  public static final DecimalFormat uPpmFormat;
  public static final DecimalFormat sPpmFormat;
  public static final DecimalFormat uDaFormat;
  public static final DecimalFormat sDaFormat;
  public static final DecimalFormat intensityFormat;

  static {
    PROPERTIES = getDefaults(DEFAULTS);
    try {
      LOCALE = Locale.forLanguageTag(PROPERTIES.getProperty("Locale"));
    } catch (Exception e) {
      PROPERTIES.setProperty("Locale", Locale.ENGLISH.getLanguage());
      LOCALE = Locale.ENGLISH;
    }
    
    DecimalFormat df = (DecimalFormat)DecimalFormat.getNumberInstance(LOCALE);
    if (df == null) {
      df = (DecimalFormat)DecimalFormat.getNumberInstance();
    }

    df = (DecimalFormat)df.clone();
    df.setMinimumFractionDigits(0);
    df.setMaximumFractionDigits(Double.MAX_EXPONENT);
    df.setParseBigDecimal(true);
    uMassFullFormat = df;

    if (df.getPositivePrefix().isEmpty()) {
      df = (DecimalFormat)df.clone();
      df.setPositivePrefix("+");
      sMassFullFormat = df;
    } else {
      sMassFullFormat = uMassFullFormat;
    }

    df = (DecimalFormat)uMassFullFormat.clone();
    if (PROPERTIES.getProperty("MassDigits") == null) {
      PROPERTIES.setProperty("MassDigits", "4");
    }
    try {
      df.setMinimumFractionDigits(Integer.parseInt(PROPERTIES.getProperty("MassDigits")));
      df.setMaximumFractionDigits(Integer.parseInt(PROPERTIES.getProperty("MassDigits")));
    } catch (Exception e) {
      PROPERTIES.setProperty("MassDigits", "4");
      df.setMinimumFractionDigits(4);
      df.setMaximumFractionDigits(4);
    }
    uMassShortFormat = df;

    if (df.getPositivePrefix().isEmpty()) {
      df = (DecimalFormat)df.clone();
      df.setPositivePrefix("+");
      sMassShortFormat = df;
    } else {
      sMassShortFormat = uMassShortFormat;
    }

    df = (DecimalFormat)uMassShortFormat.clone();
    if (PROPERTIES.getProperty("PpmDigits") == null) {
      PROPERTIES.setProperty("PpmDigits", "2");
    }
    try {
      df.setMinimumFractionDigits(Integer.parseInt(PROPERTIES.getProperty("PpmDigits")));
      df.setMaximumFractionDigits(Integer.parseInt(PROPERTIES.getProperty("PpmDigits")));
    } catch (Exception e) {
      PROPERTIES.setProperty("PpmDigits", "2");
      df.setMinimumFractionDigits(2);
      df.setMaximumFractionDigits(2);
    }
    uPpmFormat = df;

    if (df.getPositivePrefix().isEmpty()) {
      df = (DecimalFormat)df.clone();
      df.setPositivePrefix("+");
      sPpmFormat = df;
    } else {
      sPpmFormat = uPpmFormat;
    }

    df = (DecimalFormat)uPpmFormat.clone();
    if (PROPERTIES.getProperty("DaDigits") == null) {
      PROPERTIES.setProperty("DaDigits", "4");
    }
    try {
      df.setMinimumFractionDigits(Integer.parseInt(PROPERTIES.getProperty("DaDigits")));
      df.setMaximumFractionDigits(Integer.parseInt(PROPERTIES.getProperty("DaDigits")));
    } catch (Exception e) {
      PROPERTIES.setProperty("DaDigits", "4");
      df.setMinimumFractionDigits(4);
      df.setMaximumFractionDigits(4);
    }
    uDaFormat = df;

    if (df.getPositivePrefix().isEmpty()) {
      df = (DecimalFormat)df.clone();
      df.setPositivePrefix("+");
      sDaFormat = df;
    } else {
      sDaFormat = uDaFormat;
    }

    df = (DecimalFormat)uDaFormat.clone();
    if (PROPERTIES.getProperty("IntensityDigits") == null) {
      PROPERTIES.setProperty("IntensityDigits", "2");
    }
    int digits = 2;
    try {
      digits = Integer.parseInt(PROPERTIES.getProperty("IntensityDigits"));
      if (digits < 0) {
        digits = 0;
      }
    } catch (Exception e) {
      PROPERTIES.setProperty("DaDigits", "2");
    }
    df.applyPattern((digits == 0 ? "0" : ("0." + new String(new char[digits]).replace('\0', '0'))) + "E0");
    intensityFormat = df;

    addDefaults(DEFAULTS, PROPERTIES);
  }
  
  public static SettingsPanel getSettingsPanel() {
    SettingsPanel ret = new SettingsPanel("Numbers") {
      private JLabel localeLabel;
      private JComboBox localeComboBox;
      private JLabel massLabel;
      private JSpinner massSpinner;
      private JLabel ppmLabel;
      private JSpinner ppmSpinner;
      private JLabel daLabel;
      private JSpinner daSpinner;
      private JLabel intensityLabel;
      private JSpinner intensitySpinner;

      {
        localeLabel = new JLabel("Locale:");
        localeComboBox = new JComboBox();
        massLabel = new JLabel("Mass digits:");
        massSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(Defaults.PROPERTIES.getProperty("MassDigits")), Integer.valueOf(0), null, Integer.valueOf(1)));
        ppmLabel = new JLabel("Relative error (ppm) digits:");
        ppmSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(Defaults.PROPERTIES.getProperty("PpmDigits")), Integer.valueOf(0), null, Integer.valueOf(1)));
        daLabel = new JLabel("Absolute error (Da) digits:");
        daSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(Defaults.PROPERTIES.getProperty("DaDigits")), Integer.valueOf(0), null, Integer.valueOf(1)));
        intensityLabel = new JLabel("Intinsity digits:");
        intensitySpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(Defaults.PROPERTIES.getProperty("IntensityDigits")), Integer.valueOf(0), null, Integer.valueOf(1)));

        //localeComboBox.setEditable(true);
        TreeSet<String> locales = new TreeSet();
        for (Locale locale : Locale.getAvailableLocales()) {
          locales.add(locale.toLanguageTag());
        }
        localeComboBox.setModel(new javax.swing.DefaultComboBoxModel(locales.toArray()));
        localeComboBox.setSelectedItem(Defaults.PROPERTIES.getProperty("Locale"));
        localeComboBox.setName(Defaults.PROPERTIES.getProperty("Locale"));
//        ((JTextField)localeComboBox.getEditor().getEditorComponent()).setInputVerifier(new InputVerifier() {
//          public boolean verify(JComponent input) {
//            if (!(input instanceof JTextField)) {
//              throw new IllegalArgumentException("Unexpected class. Expected: JTextField; Found: " + input.getClass().getCanonicalName());
//            }
//            JTextField jtf = (JTextField)input;
//            if (jtf.getText() == null || jtf.getText().isEmpty()) {
//              return true;
//            }
//            try {
//              return jtf.getText().equals(Locale.forLanguageTag(jtf.getText()).toLanguageTag());
//            } catch (Exception e) {
//              return false;
//            }
//          }
//        });

        ((JSpinner.DefaultEditor)massSpinner.getEditor()).getTextField().setColumns(3);
        ((JSpinner.DefaultEditor)ppmSpinner.getEditor()).getTextField().setColumns(3);
        ((JSpinner.DefaultEditor)daSpinner.getEditor()).getTextField().setColumns(3);
        ((JSpinner.DefaultEditor)intensitySpinner.getEditor()).getTextField().setColumns(3);
        massSpinner.setName(Defaults.PROPERTIES.getProperty("MassDigits"));
        ppmSpinner.setName(Defaults.PROPERTIES.getProperty("PpmDigits"));
        daSpinner.setName(Defaults.PROPERTIES.getProperty("DaDigits"));
        intensitySpinner.setName(Defaults.PROPERTIES.getProperty("IntensityDigits"));

        GroupLayout generalLayout = new GroupLayout(this);
        setLayout(generalLayout);
        generalLayout.setHorizontalGroup(
          generalLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(generalLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(generalLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(localeLabel)
              .addComponent(massLabel)
              .addComponent(ppmLabel)
              .addComponent(daLabel)
              .addComponent(intensityLabel))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(generalLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(localeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(massSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(ppmSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(daSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(intensitySpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        generalLayout.setVerticalGroup(
          generalLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(generalLayout.createSequentialGroup()
            .addGroup(generalLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
              .addComponent(localeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(localeLabel))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(generalLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
              .addComponent(massSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(massLabel))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(generalLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
              .addComponent(ppmSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(ppmLabel))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(generalLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
              .addComponent(daSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(daLabel))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(generalLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
              .addComponent(intensitySpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(intensityLabel)))
        );
      }

      public boolean isChanged() {
        return !localeComboBox.getName().equals(localeComboBox.getSelectedItem()) ||
               !massSpinner.getName().equals(massSpinner.getValue().toString()) || 
               !ppmSpinner.getName().equals(ppmSpinner.getValue().toString()) || 
               !daSpinner.getName().equals(daSpinner.getValue().toString()) || 
               !intensitySpinner.getName().equals(intensitySpinner.getValue().toString());
      }

      public void save() {
        if (localeComboBox.getSelectedItem() != null) {
          Defaults.PROPERTIES.setProperty("Locale", localeComboBox.getSelectedItem().toString());
        }
        Defaults.PROPERTIES.setProperty("MassDigits", massSpinner.getValue().toString());
        Defaults.PROPERTIES.setProperty("PpmDigits", ppmSpinner.getValue().toString());
        Defaults.PROPERTIES.setProperty("DaDigits", daSpinner.getValue().toString());
        Defaults.PROPERTIES.setProperty("IntensityDigits", intensitySpinner.getValue().toString());
        addDefaults(DEFAULTS, Defaults.PROPERTIES);
      }
    };
    return ret;
  }


  public static boolean addDefaults(String id, Properties table) {
    Properties p = new Properties();
    try {
      p.load(new FileReader(FOLDER + File.separator + FILE + EXTENSION));
    } catch (Exception e) { }
    for (String key : table.stringPropertyNames()) {
      p.setProperty(id + SEPARATOR + key, table.getProperty(key));
    }
    return saveDefaults(FILE, p);
  }

  public static boolean saveDefaults(String file, Properties properties) {
    if (!(new File(FOLDER).exists())) {
      new File(FOLDER).mkdir();
    }
    try (FileWriter fw = new FileWriter(FOLDER + File.separator + file + EXTENSION, false)) {
      properties.store(fw, null);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, "It isn't possible to save the file." + System.lineSeparator() + "================================" +
                                    System.lineSeparator() + e.getMessage(), "IO Exception", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    return true;
  }

  public static Properties getDefaults(String id) {
    Properties ret = new Properties();
    Properties p = loadDefaults(FILE);
    for (String key : p.stringPropertyNames()) {
      if (key.startsWith(id + SEPARATOR)) {
        ret.setProperty(key.substring(id.length()+SEPARATOR.length()), p.getProperty(key));
      }
    }
    return ret;
  }

  public static Properties loadDefaults(String file) {
    Properties p = new Properties();
    try {
      p.load(new FileReader(FOLDER + File.separator + file + EXTENSION));
    } catch (Exception e) { }
    return p;
  }

  public static Properties putWindowDefaults(Window window, Properties properties) {
    properties.setProperty("Size", window.getWidth() + "," + window.getHeight());
    properties.setProperty("Location", window.getLocationOnScreen().x + "," + window.getLocationOnScreen().y);
    return properties;
  }

  public static Properties putTableDefaults(JTable table, Properties properties) {
    for (int i = 0; i < table.getColumnCount(); i++) {
      properties.setProperty(table.getColumnName(i), String.valueOf(table.getColumn(table.getColumnName(i)).getWidth()));
    }
    return properties;
  }

  public static void setWindowDefaults(Window window, Properties properties) {
    if (properties.containsKey("Size")) {
      try {
        String[] p = properties.getProperty("Size").split(",");
        window.setSize(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
      } catch (Exception e) { }
    }
    if (properties.containsKey("Location")) {
      try {
        String[] p = properties.getProperty("Location").split(",");
        window.setLocation(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
      } catch (Exception e) { }
    }
  }

  public static void setTableDefaults(JTable table, Properties properties) {
    HashMap<String, Integer> sizes = new HashMap<>(table.getColumnCount());
    for (int i = 0; i < table.getColumnCount(); i++) {
      sizes.put(table.getColumnName(i), table.getColumn(table.getColumnName(i)).getWidth());
    }
    for (String name : properties.stringPropertyNames()) {
      try {
        if (sizes.containsKey(name)) {
          sizes.put(name, Integer.parseInt(properties.getProperty(name)));
        }
      } catch (Exception e) { }
    }
    if (table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF) {
      int sum = 0;
      for (Integer i : sizes.values()) {
        sum += i;
      }
      for (String key : sizes.keySet()) {
        sizes.put(key, (int)(1.0 * sizes.get(key) * table.getWidth() / sum));
      }
    }
    for (String key : sizes.keySet()) {
      table.getColumn(key).setPreferredWidth(sizes.get(key));
    }
  }
}
