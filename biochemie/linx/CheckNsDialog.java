package biochemie.linx;

import java.awt.Color;
import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ProgressMonitor;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author kuan
 */
public class CheckNsDialog extends javax.swing.JDialog {

	LinX parent;
	HashMap<FileName, ResultsPanel> resultsPanels;
	HashMap<FileName, TreeMap<BigDecimal, IMeasurement>> massLists;
	HashMap<FileName, ClTableModel> tableModels;
	HashMap<FileName, HashMap<String, List<Object[]>>> bgConstDataLists;
	FileName actResPanel;
	int scanTol = SCAN_TOL;
	long minInt = MIN_INT_FOR_BG;
	int outTol = MED_DIF_FILTER;

	static final int MIN_INT_FOR_BG = 10000000;
	static final int MED_DIF_FILTER = 20;
	static final int SCAN_TOL = 3;
	private static final int MAX_CHARGE = 10;
	private static final Map<String, Integer> NOOF_N;
	private static final double N_ISO_DIFF = 15.0001088989 - 14.00307400446; //0.99703489
	private static final double CHARGE_MASS = 1.007276466879;

	static {
		Map<String, Integer> m = new HashMap<>();
		m.put("R", 4);
		m.put("H", 3);
		m.put("K", 2);
		m.put("N", 2);
		m.put("Q", 2);
		m.put("W", 2);
		NOOF_N = Collections.unmodifiableMap(m);
	}

	/**
	 * Creates new form CheckNsDialog
	 *
	 * @param parent	Parent LinX Window
	 * @param resultsPanels	Map of ResultsPanel containing results in tables (Jan
	 * Jelínek)
	 * @param masses	Map of Measurements - input MS data (Jan Jelínek)
	 * @param monitor Attached ProgressMonitor or null if not used
	 * @param scanTol	Number of scans distance to consider N-mass belongs to rec
	 * @param minInt	Minimal intensity for corr. fact calculation
	 * @param outTol	Maximal difference from median for corr. fact. calculation
	 */
	public CheckNsDialog(LinX parent, HashMap<FileName, ResultsPanel> resultsPanels, TreeMap<BigDecimal, HashSet<IMeasurement>> masses, ProgressMonitor monitor, int scanTol, long minInt, int outTol) {
		super(parent, false);
		this.parent = parent;
		this.scanTol = scanTol;
		this.minInt = minInt;
		this.scanTol = scanTol;
		this.resultsPanels = resultsPanels;
		massLists = new HashMap<>(resultsPanels.size());
		tableModels = new HashMap<>(resultsPanels.size());
		bgConstDataLists = new HashMap<>(resultsPanels.size());
		int numRows = 0;
		for (FileName fileName : resultsPanels.keySet()) {
			numRows += resultsPanels.get(fileName).getNoofTableRows();
		}

		float progress = 0.0f;
		float progInc = 60.0f / numRows;
		int lastReported = 0;

		if (monitor != null) {
			monitor.setNote("Preparing tables...");
		}
		for (FileName fileName : resultsPanels.keySet()) {
			massLists.put(fileName, new TreeMap<>());
			tableModels.put(fileName, new ClTableModel());
			bgConstDataLists.put(fileName, new HashMap<>());
			for (String protName : resultsPanels.get(fileName).getProteins()) {
				bgConstDataLists.get(fileName).put(protName, new ArrayList<>());
			}
			for (int i = 0; i < resultsPanels.get(fileName).getNoofTableRows(); i++) {
				/*
0 - "Exp. Mass" ~ Double[].class
1 - "Thr. Mass" ~ Double[].class
2 - "Error" ~ Double[].class
3 - "Protein (from, to)" ~ String[].class
4 - "Chain" ~ String[].class ------------------------------
5 - "Modifications" ~ String[].class
6 - "Bonds" ~ String[].class ------------------------------
7 - "Intensity" ~ Long[].class
8 - "Retention time" ~ String[].class
9 - "Other"	 ~ String[].class
				 */
				Object[] rowData = resultsPanels.get(fileName).getTableRowData(i);
				if (rowData[4] != null && rowData[6] != null && !((String) rowData[6]).isEmpty()) {
					//if bond found(not empty
					tableModels.get(fileName).addRow(new ClTableRow((String) rowData[4], (String) rowData[3], (String) rowData[5], (String) rowData[6], (Double) rowData[1], (Long) rowData[7], (String) rowData[8], (String) rowData[9]));
				} else if (rowData[4] != null && !((String) rowData[4]).isEmpty() && rowData[5] != null && ((String) rowData[5]).isEmpty() && rowData[6] != null && ((String) rowData[6]).isEmpty()) {
					//else if peptide found and neither mod nor bond found
					String protRec = (String) rowData[3];
					String protKey = protRec.substring(0, protRec.indexOf(" ("));
					bgConstDataLists.get(fileName).get(protKey).add(rowData);
				}
				progress += progInc;
				if (monitor != null && ((int) progress > lastReported)) {
					lastReported = (int) progress;
					monitor.setProgress(lastReported);
					monitor.setNote("Preparing table data...");
				}
			}
			tableModels.get(fileName).setTolerance(resultsPanels.get(fileName).getTolerance());
		}
		progress = 60.0f;
		if (monitor != null && ((int) progress > lastReported)) {
			lastReported = (int) progress;
			monitor.setProgress(lastReported);
			monitor.setNote("Extracting masslists...");
		}
//5.3
		progInc = 30.0f / masses.size();
		for (BigDecimal mass : masses.keySet()) {
			for (IMeasurement iMeasurement : masses.get(mass)) {
				if (massLists.get(iMeasurement.getFileName()) == null) {
					System.err.println(String.format("Mass %.4f cannot be assigned to unknown file %s.", mass, iMeasurement.getFileName().toString()));
				} else {
					massLists.get(iMeasurement.getFileName()).put(mass, iMeasurement);
				}
			}
			progress += progInc;
			if (monitor != null && ((int) progress > lastReported)) {
				lastReported = (int) progress;
				monitor.setProgress(lastReported);
				monitor.setNote("Extracting masslists...");
			}
		}
//7.0
		if (monitor != null) {
			monitor.setProgress(90);
			monitor.setNote("Calculating...");
		}
		for (FileName fileName : tableModels.keySet()) {
			tableModels.get(fileName).findMasses(massLists.get(fileName));
		}
		initComponents();
		fileChooser.setCurrentDirectory(new File(actResPanel.getFileName()));
		setTable();
		if (monitor != null) {
			monitor.setProgress(100);
			monitor.setNote("Done...");
		}
		//7.5

		HashMap<String, double[]> bgConsts;
		for (FileName fileName : bgConstDataLists.keySet()) {
			bgConsts = new HashMap<>();
			TreeMap<BigDecimal, IMeasurement> mList = massLists.get(fileName);
			System.out.println(fileName.toString() + "\n----------------------\n");
			for (String prot : bgConstDataLists.get(fileName).keySet()) {
				System.out.println(prot + ":\n");
				List<Double> ratios = new ArrayList<>();
				List<Long> ints = new ArrayList<>();

				for (Object[] rowData : bgConstDataLists.get(fileName).get(prot)) {
					double mass = (double) rowData[0];
					String seq = (String) rowData[4];
					long intensity = (long) rowData[7];
					String scan = (String) rowData[8];
					double findNMass = mass + calculateNs(seq) * N_ISO_DIFF;
					double massDiff = mass - tableModels.get(fileName).tolerance.lower(new BigDecimal(mass)).doubleValue();
					Set<BigDecimal> candidates = getValidMassesSet(findNMass, massDiff, scan, mList);
					System.out.print(String.format("%s: %s (%s) - %.4f (%d): %d candidates",
							  rowData[3], seq, scan, mass, intensity, candidates.size()));
					if (candidates.size() == 1 && intensity >= minInt) {
						BigDecimal heavyMass = candidates.iterator().next();
						double heavyIntensity = mList.get(heavyMass).getIntensity();
						double ratio = heavyIntensity / intensity * 100;
						System.out.print(String.format(" %.2f%% (%.2f)",
								  ratio, Math.log10(intensity)));
						ratios.add(ratio);
						ints.add(intensity);

					}
					System.out.println("");
				}
				System.out.println("Summary (ratio intensity):");
				for (int i = 0; i < ratios.size(); i++) {
					System.out.println(String.format("%.2f\t%d", ratios.get(i), ints.get(i)));
				}
				List<Integer> badInds = indicesToDiscard(ratios);

				for (int i = badInds.size() - 1; i >= 0; i--) {
					System.out.println(String.format("Discarding value %.2f with intensity %d (%.2f).", ratios.get(i), ints.get(i), Math.log10(ints.get(i))));
					ints.remove((int) badInds.get(i));
					ratios.remove((int) badInds.get(i));
				}

				double[] bgs = getBgStats(ratios, ints);
				System.out.println(String.format("=> Avg: %.2f (SD: %.2f); W Avg: %.2f", bgs[0], bgs[1], bgs[2]));
				System.out.println("\n\n\n");
				bgConsts.put(prot, bgs);
			}
			tableModels.get(fileName).setBgConsts(bgConsts);
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      fileChooser = new javax.swing.JFileChooser();
      inputFileCB = new javax.swing.JComboBox<>();
      xLinkSP = new javax.swing.JScrollPane();
      xLinkTbl = new javax.swing.JTable();
      exportBt = new javax.swing.JButton();
      closeBt = new javax.swing.JButton();
      loadMasslistBt = new javax.swing.JButton();

      fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setPreferredSize(new java.awt.Dimension(1200, 512));

      inputFileCB.setModel(new javax.swing.DefaultComboBoxModel<>(extractKeyArr(resultsPanels)));
      inputFileCB.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            inputFileCBActionPerformed(evt);
         }
      });

      inputFileCBActionPerformed(null);
      xLinkTbl.setAutoCreateRowSorter(true);
      xLinkTbl.setModel(tableModels.get(actResPanel));
      xLinkSP.setViewportView(xLinkTbl);

      exportBt.setText("Export");
      exportBt.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            exportBtActionPerformed(evt);
         }
      });

      closeBt.setText("Close");
      closeBt.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            closeBtActionPerformed(evt);
         }
      });

      loadMasslistBt.setText("Load masslist");
      loadMasslistBt.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadMasslistBtActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(xLinkSP, javax.swing.GroupLayout.DEFAULT_SIZE, 1390, Short.MAX_VALUE)
               .addComponent(inputFileCB, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(exportBt)
                  .addGap(18, 18, 18)
                  .addComponent(loadMasslistBt)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(closeBt)))
            .addContainerGap())
      );

      layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {exportBt, loadMasslistBt});

      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(inputFileCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(xLinkSP, javax.swing.GroupLayout.DEFAULT_SIZE, 404, Short.MAX_VALUE)
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(exportBt)
               .addComponent(closeBt)
               .addComponent(loadMasslistBt))
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void inputFileCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputFileCBActionPerformed
		for (FileName fileName : resultsPanels.keySet()) {
			if (inputFileCB.getSelectedItem().toString().equals(fileName.toString())) {
				actResPanel = fileName;
			}
		}
		xLinkTbl.setModel(tableModels.get(actResPanel));
		setTable();
   }//GEN-LAST:event_inputFileCBActionPerformed

   private void closeBtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeBtActionPerformed
		dispose();
   }//GEN-LAST:event_closeBtActionPerformed

   private void exportBtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportBtActionPerformed
		fileChooser.setDialogTitle("Export");
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.resetChoosableFileFilters();
		fileChooser.setSelectedFile(new File(new File(actResPanel.fileName).getName() + "-export.txt"));
		int returnVal = fileChooser.showDialog(this, "Export");
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File outFile;
			if (fileChooser.getSelectedFile().getName().toLowerCase().endsWith(".txt")) {
				outFile = fileChooser.getSelectedFile();
			} else {
				outFile = new File(fileChooser.getSelectedFile().getAbsolutePath() + ".txt");
			}
			try (BufferedWriter out = new BufferedWriter(new FileWriter(outFile))) {
				out.write("Cross-link\tProtein\tModification\tBonds\tCharge\tScan\tForms found\t"
						  + "Theoretical LL m/z (1+)\tTheoretical LL m/z (X+)\t"
						  + "Theoretical LH m/z (1+)\tTheoretical LH m/z (X+)\t"
						  + "Theoretical HL m/z (1+)\tTheoretical HL m/z (X+)\t"
						  + "Theoretical HH m/z (1+)\tTheoretical HH m/z (X+)\t"
						  + "Experimental LL m/z (1+)\tExperimental LL m/z (X+)\tLL Intensity\t"
						  + "Experimental LH m/z (1+)\tExperimental LH m/z (X+)\tLH Intensity\t"
						  + "Experimental HL m/z (1+)\tExperimental HL m/z (X+)\tHL Intensity\t"
						  + "Experimental HH m/z (1+)\tExperimental HH m/z (X+)\tHH Intensity\t"
						  + "Inter CL (%)\tIntra CL (%)\tCorrection ratios\tLL\tLH\tHL\tHH\n");
				for (ClTableRow row : tableModels.get(actResPanel).data) {
					out.write(String.format("%s\t%s\t%s\t%s\t%s\t%s\t", row.xLink, row.protein, row.mod, row.bonds, (row.chrgGuessed) ? "(" + row.charge + ")" : "" + row.charge, row.scan));
					int noofForms = row.getNoofForms();
					if (row.isInternal()) {
						noofForms /= 2;
					}
					out.write(String.format("%d\t", noofForms));
					out.write(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t",
							  row.llMass, convertMassToCharged(row.llMass, row.charge),
							  (row.isInternal()) ? Double.NaN : row.lhMass, (row.isInternal()) ? Double.NaN : convertMassToCharged(row.lhMass, row.charge),
							  (row.isInternal()) ? Double.NaN : row.hlMass, (row.isInternal()) ? Double.NaN : convertMassToCharged(row.hlMass, row.charge),
							  row.hhMass, convertMassToCharged(row.hhMass, row.charge)));
					out.write(row.getExpDataStringForExportLL());
					out.write(row.getExpDataStringForExportLH());
					out.write(row.getExpDataStringForExportHL());
					out.write(row.getExpDataStringForExportHH());
					if (row.hasValidRatio()) {
						double mixedInt = (row.massList.get(row.lhMassesSet.iterator().next()).getIntensity() + row.massList.get(row.hlMassesSet.iterator().next()).getIntensity());
						double sameIsoInt = (row.massList.get(row.llMassesSet.iterator().next()).getIntensity() + row.massList.get(row.hhMassesSet.iterator().next()).getIntensity());
						if (row.sameHlLh) {
							mixedInt /= 2;
						}
						int num = (int) Math.round(mixedInt / sameIsoInt * 100);
						if (num > 100) {
							out.write(String.format("%d\t0\t", num));
						} else {
							out.write(String.format("%d\t%d\t", num, 100 - num));
						}
					} else if (row.isInternal()) {
						out.write("0\t100\t");
					} else {
						out.write("---\t----\t");
					}

					Set<String> uniqueProts = new HashSet<>(row.prots);
					StringBuilder retVal = new StringBuilder();
					for (String protName : uniqueProts) {
						retVal.append(String.format("%s: %.1f%%; ", protName, tableModels.get(actResPanel).bgConsts.get(protName)[2]));
					}
					out.write(retVal.toString().substring(0, retVal.length() - 2));

					if (row.llMassesSet.size() > 1) {
						for (BigDecimal mass : row.llMassesSet) {
							out.write(String.format("%.4f (%.4f) ~ %d;", mass, convertMassToCharged(mass.doubleValue(), row.charge), Math.round(row.massList.get(mass).getIntensity())));
						}
					}
					out.write("\t");
					if (row.lhMassesSet.size() > 1 && !row.isInternal()) {
						for (BigDecimal mass : row.lhMassesSet) {
							out.write(String.format("%.4f (%.4f) ~ %d;", mass, convertMassToCharged(mass.doubleValue(), row.charge), Math.round(row.massList.get(mass).getIntensity())));
						}
					}
					out.write("\t");
					if (row.hlMassesSet.size() > 1 && !row.isInternal()) {
						for (BigDecimal mass : row.hlMassesSet) {
							out.write(String.format("%.4f (%.4f) ~ %d;", mass, convertMassToCharged(mass.doubleValue(), row.charge), Math.round(row.massList.get(mass).getIntensity())));
						}
					}
					out.write("\t");
					if (row.hhMassesSet.size() > 1) {
						for (BigDecimal mass : row.hhMassesSet) {
							out.write(String.format("%.4f (%.4f) ~ %d;", mass, convertMassToCharged(mass.doubleValue(), row.charge), Math.round(row.massList.get(mass).getIntensity())));
						}
					}
					out.write("\n");
				}
				out.close();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Error writing export file.\n" + ex.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
			}
		}
   }//GEN-LAST:event_exportBtActionPerformed

   private void loadMasslistBtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadMasslistBtActionPerformed
		throw (new UnsupportedOperationException("Not implemented yet."));
//		fileChooser.setDialogTitle("Load masslist");
//		fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
//		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//		fileChooser.setMultiSelectionEnabled(false);
//		fileChooser.resetChoosableFileFilters();
//		fileChooser.setSelectedFile(new File(""));
//		int returnVal = fileChooser.showDialog(this, "Export");
//		if (returnVal == JFileChooser.APPROVE_OPTION) {
//			loadNewMasslist(fileChooser.getSelectedFile());
//		}
   }//GEN-LAST:event_loadMasslistBtActionPerformed

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton closeBt;
   private javax.swing.JButton exportBt;
   private javax.swing.JFileChooser fileChooser;
   private javax.swing.JComboBox<String> inputFileCB;
   private javax.swing.JButton loadMasslistBt;
   private javax.swing.JScrollPane xLinkSP;
   private javax.swing.JTable xLinkTbl;
   // End of variables declaration//GEN-END:variables

	private static double convertMassToCharged(double mhMass, int charge) {
		double retVal = mhMass - CHARGE_MASS;
		return (retVal + charge * CHARGE_MASS) / charge;
	}

	private String[] extractKeyArr(Map input) {
		String[] retVal = new String[input.keySet().size()];
		int i = 0;
		for (Object key : input.keySet()) {
			retVal[i++] = key.toString();
		}
		return retVal;
	}

	private void setTable() {
		NMassCellRenderer nMassCellRenderer = new NMassCellRenderer();
		for (int i = 0; i < xLinkTbl.getColumnCount(); i++) {
			TableColumn column = xLinkTbl.getColumnModel().getColumn(i);
			switch (i) {
				case 0: //seq
					column.setPreferredWidth(300);
					break;
				case 1: //prot
					column.setPreferredWidth(220);
					break;
				case 2: //mod
					column.setPreferredWidth(140);
					break;
				case 3: //bonds
					column.setPreferredWidth(150);
					break;
				case 4: //mass
					column.setCellRenderer(new MassCellRenderer());
					column.setPreferredWidth(85);
					break;
				case 5: //scan
					column.setPreferredWidth(60);
					break;
				case 6: //charge
					column.setCellRenderer(new ChargeCellRenderer());
					column.setPreferredWidth(20);
					break;
				case 7: //label masses
				case 8:
				case 9:
				case 10:
					column.setCellRenderer(nMassCellRenderer);
					column.setPreferredWidth(120);
					break;
				case 11:	//ratio
					column.setCellRenderer(new RatioCellRenderer());
					column.setPreferredWidth(60);
					break;
				case 12:	//bgConsts
					column.setPreferredWidth(200);
					break;
				default:
					throw new AssertionError();
			}
		}
	}

	private void loadNewMasslist(File selectedFile) {
		throw new UnsupportedOperationException("Not supported yet."); //TODO: method body
	}

	static int calculateNs(String seq) {
		int retVal = seq.length();
		for (int i = 0; i < seq.length(); i++) {
			if (NOOF_N.containsKey(seq.substring(i, i + 1).toUpperCase())) {
				retVal += NOOF_N.get(seq.substring(i, i + 1).toUpperCase()) - 1;
			}

		}
		return retVal;
	}

	private Set<BigDecimal> getValidMassesSet(double mass, double massDiff, String scan, Map<BigDecimal, IMeasurement> massList) {
		Set<BigDecimal> retVal = new HashSet<>();
		List<BigDecimal> masses = new ArrayList<>(massList.keySet());
		Collections.sort(masses);
		int lower = 0;
		int upper = masses.size() - 1;
		while (lower <= upper) {
			int middle = lower + (upper - lower) / 2;
			if (Math.abs(masses.get(middle).doubleValue() - mass) <= massDiff) {
				int index = middle;
				while (index > 0 && Math.abs(masses.get(index - 1).doubleValue() - mass) <= massDiff) {
					index--;
				}
				int minInd = index;
				index = middle;
				while (index < masses.size() - 1 && Math.abs(masses.get(index + 1).doubleValue() - mass) <= massDiff) {
					index++;
				}
				int maxInd = index;

				for (int i = minInd; i <= maxInd; i++) {
					if (scansInTol(scan, massList.get(masses.get(i)).getRetentionTime())) {
						retVal.add(masses.get(i));
					}
				}
				return retVal;

			}
			if (masses.get(middle).doubleValue() < mass) {
				lower = middle + 1;
			} else {
				upper = middle - 1;
			}
		}
		return retVal;
	}

	private boolean scansInTol(String retentionTime1, String retentionTime2) {
		ScanRec sc1 = new ScanRec(retentionTime1);
		ScanRec sc2 = new ScanRec(retentionTime2);
		return sc1.getGap(sc2) <= scanTol;
	}

	private List<String> parseProteins(String protein) {
		List<String> retVal = new ArrayList<>();
		String[] parts = protein.split("[.+] - ");
		for (String part : parts) {
			part = part.trim();
			retVal.add(part.substring(0, part.indexOf(" (")));
		}
		return retVal;
	}

	private double[] getBgStats(List<Double> ratios, List<Long> ints) {
		double ratSum = 0;
		double wRatSum = 0;
		double weight = 0;

		for (int i = 0; i < ratios.size(); i++) {
			ratSum += ratios.get(i);
			wRatSum += ratios.get(i) * ints.get(i);
			weight += ints.get(i);
		}

		double avg = ratSum / ratios.size();
		double wAvg = wRatSum / weight;
		double sum = 0;
		for (Double value : ratios) {
			sum += (value - avg) * (value - avg);
		}

		double[] retVal = new double[3];
		retVal[0] = avg;
		retVal[1] = Math.sqrt(sum / (ratios.size() - 1));
		retVal[2] = wAvg;
		return retVal;
	}

	private List<Integer> indicesToDiscard(List<Double> ratios) {
		List<Integer> retVal = new ArrayList<>();
		if (ratios == null || ratios.size() < 3) {
			return retVal;
		}
		List<Double> temp = new ArrayList<>(ratios);
		Collections.sort(temp);
		double med;
		if (temp.size() % 2 == 0) {
			int midIndex = temp.size() / 2;
			med = (temp.get(midIndex - 1) + temp.get(midIndex)) / 2;
		} else {
			med = temp.get((temp.size() - 1) / 2);
		}
		System.out.println(String.format("Median: %.1f", med));
		for (int i = 0; i < ratios.size(); i++) {
			if (ratios.get(i) < med - outTol || ratios.get(i) > med + outTol) {
				retVal.add(i);
			}
		}
		return retVal;
	}

	private class ClTableModel extends DefaultTableModel {

		String[] colNames = {"Cross-links", "Protein", "Modification", "Bonds", "Mass", "Scan", "Charge", "14N/14N", "14N/15N", "15N/14N", "15N/15N", "Inter/Intra Ratio", "Corr. factors"};
		ArrayList<ClTableRow> data;
		boolean processed = false;
		MeasurementCard.Tolerance tolerance;
		Map<String, double[]> bgConsts;
		Map<Double, Set<String>> massesScans;
		TreeMap<BigDecimal, IMeasurement> massList;

		public ClTableModel() {
			data = new ArrayList<>();
			massesScans = new HashMap<>();
		}

		public void addRow(ClTableRow newRow) {
			if (massesScans.containsKey(newRow.mhMass)) {
				if (massesScans.get(newRow.mhMass).contains(newRow.scan)) {
					return;
				}
			} else {
				massesScans.put(newRow.mhMass, new HashSet<>());
			}
			massesScans.get(newRow.mhMass).add(newRow.scan);
			data.add(newRow);
		}

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
				case 0:
					return data.get(row).xLink;
				case 1:
					return data.get(row).protein;
				case 2:
					return data.get(row).mod;
				case 3:
					return data.get(row).bonds;
				case 4:
					return data.get(row).mhMass;
				case 5:
					return new ScanRec(data.get(row).scan);
				case 6:
					return new ChargeRec(data.get(row).charge, data.get(row).chrgGuessed);
				case 7:
					return new LabeledMassesRec(data.get(row).llMass, convertMassToCharged(data.get(row).llMass, data.get(row).charge), data.get(row).llMassesSet, massList, false, data.get(row).isInternal());
				case 8:
					return new LabeledMassesRec(data.get(row).lhMass, convertMassToCharged(data.get(row).lhMass, data.get(row).charge), data.get(row).lhMassesSet, massList, data.get(row).sameHlLh, data.get(row).isInternal());
				case 9:
					return new LabeledMassesRec(data.get(row).hlMass, convertMassToCharged(data.get(row).hlMass, data.get(row).charge), data.get(row).hlMassesSet, massList, data.get(row).sameHlLh, data.get(row).isInternal());
				case 10:
					return new LabeledMassesRec(data.get(row).hhMass, convertMassToCharged(data.get(row).hhMass, data.get(row).charge), data.get(row).hhMassesSet, massList, false, data.get(row).isInternal());
				case 11:
					if (data.get(row).isInternal()) {
						return -100.0;
					}
					if (data.get(row).hasValidRatio()) {
						double mixedInt = ((LabeledMassesRec) getValueAt(row, 8)).getIntensity() + ((LabeledMassesRec) getValueAt(row, 9)).getIntensity();
						double sameIsoInt = ((LabeledMassesRec) getValueAt(row, 7)).getIntensity() + ((LabeledMassesRec) getValueAt(row, 10)).getIntensity();
						double retVal = (double) (mixedInt / sameIsoInt);
						return retVal;
					} else {
						return Double.NaN;
					}
				case 12:
					Set<String> uniqueProts = new HashSet<>(data.get(row).prots);
					StringBuilder retVal = new StringBuilder();
					for (String protName : uniqueProts) {
						retVal.append(String.format("%s: %.1f%%; ", protName, bgConsts.get(protName)[2]));
					}
					return retVal.toString().substring(0, retVal.length() - 2);
				default:
					return "Unknown column";
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0:
				case 1:
				case 2:
				case 3:
					return String.class;
				case 4:
					return Double.class;
				case 5:
					return ScanRec.class;
				case 6:
					return ChargeRec.class;
				case 7:
				case 8:
				case 9:
				case 10:
					return LabeledMassesRec.class;
				case 11:
					return Double.class;
				case 12:
					return String.class;
				default:
					throw new AssertionError();
			}
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public String getColumnName(int column) {
			return colNames[column];
		}

		@Override
		public int getColumnCount() {
			return colNames.length;
		}

		@Override
		public int getRowCount() {
			if (data == null) {
				return 0;
			}
			return data.size();
		}

		private void findMasses(TreeMap<BigDecimal, IMeasurement> massList) {
			this.massList = massList;
			for (ClTableRow clTableRow : data) {
				clTableRow.checkMasses(massList, tolerance);
			}
		}

		private void setTolerance(MeasurementCard.Tolerance tolerance) {
			this.tolerance = tolerance;
		}

		private void setBgConsts(Map<String, double[]> bgs) {
			this.bgConsts = bgs;
		}
	}

	private class ClTableRow {

		String xLink;
		String protein;
		String mod;
		String bonds;
		int charge;
		long intensity;
		String scan;
		double mhMass;
		Set<BigDecimal> llMassesSet, hlMassesSet, lhMassesSet, hhMassesSet;
		double llMass, hlMass, lhMass, hhMass;
		boolean sameHlLh = false;
		boolean chrgGuessed = false;
		TreeMap<BigDecimal, IMeasurement> massList;
		List<String> prots;

		public ClTableRow(String xLink, String protein, String mod, String bonds, double mhMass, long intensity, String scan, String othInfo) {
			this.xLink = xLink;
			this.protein = protein;
			this.mod = mod;
			this.bonds = bonds;
			this.intensity = intensity;
			this.scan = scan;
			this.mhMass = mhMass;
			this.charge = extractChargeFromOth(othInfo);
			if (this.charge < 1) {
				guessCharge();
			}
			String[] chains = new String[2];
			if (xLink.contains("-")) {
				chains = xLink.split("-");
			} else {
				chains[0] = xLink;
				chains[1] = "";
			}
			int noofChain1Ns = calculateNs(chains[0].trim());
			int noofChain2Ns = calculateNs(chains[1].trim());
			this.llMass = mhMass;
			this.lhMass = mhMass + noofChain2Ns * N_ISO_DIFF;
			this.hlMass = mhMass + noofChain1Ns * N_ISO_DIFF;
			this.hhMass = mhMass + (noofChain1Ns + noofChain2Ns) * N_ISO_DIFF;
			llMassesSet = new HashSet<>();
			lhMassesSet = new HashSet<>();
			hlMassesSet = new HashSet<>();
			hhMassesSet = new HashSet<>();
			massList = new TreeMap<>();
			this.prots = parseProteins(protein);
		}

		private boolean hasValidRatio() {
			if (isInternal()) {
				return false;
			}
			return (llMassesSet.size() == 1 && lhMassesSet.size() == 1 && hlMassesSet.size() == 1 && hhMassesSet.size() == 1);
		}

		private int extractChargeFromOth(String othInfo) {
			String[] parts = othInfo.split("\\h+");
			int retVal;
			for (int i = 0; i < parts.length; i++) {
				try {
					retVal = Integer.parseInt(parts[i].trim());
				} catch (NumberFormatException ex) {
					continue;
				}
				if (retVal >= 1 && retVal <= MAX_CHARGE) {
					return retVal;
				}
			}
			return 0;
		}

		private void guessCharge() {
			/*
			Guesses a charge according to a*ln(mass)+b equation (empirical)
			min: a=1; b=-5.9
			max: a=3.2; b=-17.5
			most int: a=2.3; b=-14
			 */
			charge = Math.round((float) (2.3 * Math.log(this.mhMass) - 14));
			if (charge < 1) {
				charge = 1;
			}
			chrgGuessed = true;
		}

		private void checkMasses(TreeMap<BigDecimal, IMeasurement> massList, MeasurementCard.Tolerance tolerance) {
			double massDiff = llMass - tolerance.lower(new BigDecimal(llMass)).doubleValue();
			this.massList = massList;
			llMassesSet = getValidMassesSet(llMass, massDiff, this.scan, massList);
			lhMassesSet = getValidMassesSet(lhMass, massDiff, this.scan, massList);
			hlMassesSet = getValidMassesSet(hlMass, massDiff, this.scan, massList);
			hhMassesSet = getValidMassesSet(hhMass, massDiff, this.scan, massList);
			sameHlLh = (Math.abs(hlMass - lhMass) < massDiff);
		}

		private int getNoofForms() {
			int retVal = 0;
			if (llMassesSet.size() > 0) {
				retVal++;
			}
			if (lhMassesSet.size() > 0) {
				retVal++;
			}
			if (hlMassesSet.size() > 0) {
				retVal++;
			}
			if (hhMassesSet.size() > 0) {
				retVal++;
			}
			return retVal;
		}

		private boolean isInternal() {
			return !xLink.contains("-");
		}

		private String getExpDataStringForExport(String form) {
			if (isInternal() && (form.equals("lh") || form.equals("hl"))) {
				return ("---\t---\t---\t");
			}
			Set<BigDecimal> massesSet;
			switch (form) {
				case "ll":
					massesSet = llMassesSet;
					break;
				case "lh":
					massesSet = lhMassesSet;
					break;
				case "hl":
					massesSet = hlMassesSet;
					break;
				case "hh":
					massesSet = hhMassesSet;
					break;
				default:
					throw new AssertionError();
			}

			switch (massesSet.size()) {
				case 0:
					return ("---\t---\t---\t");
				case 1:
					BigDecimal m = massesSet.iterator().next();
					return (String.format("%.4f\t%.4f\t%d\t",
							  m, convertMassToCharged(m.doubleValue(), charge), Math.round(massList.get(m).getIntensity())));
				default:
					return (String.format("~~~%1$d~~~\t~~~%1$d~~~\t~~~%1$d~~~\t", massesSet.size()));
			}
		}

		private String getExpDataStringForExportLL() {
			return getExpDataStringForExport("ll");
		}

		private String getExpDataStringForExportLH() {
			return getExpDataStringForExport("lh");
		}

		private String getExpDataStringForExportHL() {
			return getExpDataStringForExport("hl");
		}

		private String getExpDataStringForExportHH() {
			return getExpDataStringForExport("hh");
		}
	}

	private class NMassCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel retVal = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			LabeledMassesRec record = (LabeledMassesRec) value;
			retVal.setText(String.format("%.4f (%.4f)", record.mass, record.chargedMass));
			retVal.setToolTipText("");
			int col = table.convertColumnIndexToModel(column);
			if (record.internalLink && (col == 8 || col == 9)) {
				retVal.setText("N/A");
				retVal.setBackground(Color.YELLOW);
				return retVal;
			}
			if (record.massIndices.size() == 1) {
				double intensity = record.getIntensity();
				int gVal = (int) (255 - (Math.log10(intensity) - 6) * 100);
				if (gVal < 0) {
					gVal = 0;
				}
				if (gVal > 255) {
					gVal = 255;
				}
				retVal.setBackground(new Color(gVal, 255, gVal));
				retVal.setToolTipText(String.format("Log intensity: %.4f", Math.log10(intensity)));
			} else if (record.massIndices.size() > 1) {
				retVal.setBackground(new Color(128, 192, 255));
				StringBuilder tttSB = new StringBuilder();
				ArrayList<BigDecimal> tttMasses = new ArrayList<>(record.massIndices);
				Collections.sort(tttMasses);
				for (BigDecimal tttMass : tttMasses) {
					tttSB.append(String.format("; %.4f", tttMass));
				}
				String ttt = tttSB.toString().substring(2);
				retVal.setToolTipText(ttt);
				retVal.setText("" + (record.massIndices.size()) + " ~ " + ttt);
			} else {
				retVal.setBackground(new Color(255, 224, 224));
			}
			retVal.setForeground(Color.BLACK);
			return retVal;
		}

	}

	private class MassCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel retVal = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			retVal.setText(String.format("%.4f", value));
			return retVal;
		}

	}

	private class ChargeCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel retVal = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			ChargeRec chrg = (ChargeRec) value;
			if (chrg.guessed) {
				retVal.setForeground(Color.RED);
			} else {
				retVal.setForeground(Color.BLACK);
			}
			retVal.setText("" + chrg.charge);
			return retVal;
		}
	}

	private class RatioCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel retVal = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			double ratio = (double) value;
			retVal.setForeground(Color.BLACK);
			if (Double.isNaN(ratio)) {
				retVal.setText("---");
			} else if (ratio < 0) {
				retVal.setText("0 / 100");
			} else if (ratio <= 1) {
				int num = (int) Math.round(ratio * 100);
				retVal.setText(String.format("%d / %d", num, 100 - num));
			} else {
				int num = (int) Math.round(ratio * 100);
				retVal.setForeground(Color.RED);
				retVal.setText(num + " / 0");
			}
			return retVal;
		}

	}

	private class LabeledMassesRec implements Comparable<LabeledMassesRec> {

		double mass, chargedMass;
		Set<BigDecimal> massIndices;
		TreeMap<BigDecimal, IMeasurement> massList;
		boolean sameHlLh, internalLink;

		public LabeledMassesRec(double mass, double chargedMass, Set<BigDecimal> masses, TreeMap<BigDecimal, IMeasurement> massList, boolean sameHlLh, boolean internalLink) {
			this.mass = mass;
			this.chargedMass = chargedMass;
			this.massIndices = masses;
			this.massList = massList;
			this.sameHlLh = sameHlLh;
			this.internalLink = internalLink;
		}

		@Override
		public int compareTo(LabeledMassesRec o) {
			return (int) Math.signum(this.mass - o.mass);
		}

		private double getIntensity() {
			if (massIndices.size() != 1) {
				throw new IllegalStateException("More than one mass in tolerance, cannot state the right intensity.");
			} else {
				double retVal = massList.get(massIndices.iterator().next()).getIntensity();
				if (sameHlLh) {
					retVal /= 2;
				}
				return retVal;
			}
		}
	}

	private class ChargeRec implements Comparable<ChargeRec> {

		int charge;
		boolean guessed;

		public ChargeRec(int charge, boolean guessed) {
			this.charge = charge;
			this.guessed = guessed;
		}

		@Override
		public int compareTo(ChargeRec o) {
			return charge - o.charge;
		}
	}

	private class ScanRec implements Comparable<ScanRec> {

		String scans;
		int from = 0;
		int to = 0;

		public ScanRec(String scans) {
			this.scans = scans;
			try {
				if (scans.contains("-")) {
					String[] parts = scans.split("-");
					from = Integer.parseInt(parts[0].trim());
					to = Integer.parseInt(parts[1].trim());
				} else {
					from = Integer.parseInt(scans.trim());
					to = from;
				}
			} catch (NumberFormatException ex) {
				//no action, just leave 0 as from/to
			}
		}

		public int getGap(ScanRec compRec) {
			if (this.from >= compRec.from && this.from <= compRec.to) {
				return 0;
			}
			if (compRec.from >= this.from && compRec.from <= this.to) {
				return 0;
			}
			if (this.from < compRec.from) {
				return compRec.from - this.to;
			}
			return this.from - compRec.to;
		}

		@Override
		public int compareTo(ScanRec o) {
			if (from != o.from) {
				return (from - o.from);
			}
			return (to - o.to);
		}

		@Override
		public String toString() {
			return scans;
		}
	}
}
