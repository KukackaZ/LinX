package biochemie;

import java.math.BigDecimal;
import java.util.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * AminoAcidChain created directly from chain of aminoacids.
 * @author Janek
 */
public class Protein extends Polymer {
  // NON-OPT: Možná by pro globální modifikace stačilo iterovat počty výskytů a ne přiřadit každému výskytu... zase by bylo potřeba si počítat počty výskytů
  //          takže pokud je opravdu modifikujících míst tak málo, tak to není výhodné... ale pokud by to v dalších testech nestíhalo...
  // NON-OPT: Asi by to chtělo zámek, pokud se vloží mutaci do iterovaného okna uprostřed iterování. Nebo zámek když se začne iterovat, aby se už neupravovalo.
  //          A také obecně aby si více požadavků nekradlo nastavení, ale to se při současném použití neočekává, ať se kdyžtak použijí zámky/ monitory.
  //          A také testovat, zda se ptá jen na uzamknutou (resetovanou, iterovanou) oblast.
  //          Ve všech případech stačí hlídat pouze požadované okno nebo lépe požadované okno vyjma těch fixovaných.

  private static final ModificationReal EMPTY_MODIFICATION;
  // Sice bychom mohli protein nastavit na správný protein, ale pak bychom museli ošetřit, aby při klonování byla použita správná prázdná modifikace...
  static {
    EMPTY_MODIFICATION = new ModificationReal("", BigDecimal.ZERO, BigDecimal.ZERO, "", ' ', null);
  }

  private String name;
  private String prefix;
  private String protein;
  private String suffix;
  private Map<Integer, ArrayList<Character>> mutations;
  private TreeMap<Integer, Integer> mutationPointers;
  private java.util.Map<Integer, Character> mutationSkip;
  private java.util.Map<Integer, Character> modificationSkip;
  private Map<Integer, Map<Character, TreeMap<Integer, TreeSet<ModificationReal>>>> modifications;
  private ArrayList<TreeMap<Integer, TreeSet<ModificationReal>>> terminusModifications;
  private TreeMap<Integer, Map<Character, Pointer>> modificationPointers;
  private Pointer[] terminusModificationPointers;
  private TreeMap<Integer, Map<Character, TreeMap<Integer, Map<Character, TreeSet<BondReal>>>>> bonds;
  private TreeMap<Integer, Integer> bondsSelected;
  private TreeMap<Integer, BondReal> bondsPointers;
  private SimplifiedCleavage[] bounds;
  private int begin;
  private int end;
  private BigDecimal[] mins;
  private BigDecimal[] maxs;
  private BigDecimal[] minTerminus;
  private BigDecimal[] maxTerminus;
  private BigDecimal diff = BigDecimal.ZERO;
  private int start;

  /**
   * Initialize newly created <code>Protein</code>. Mutation could be defined by '/' after corresponding amino acid.
   * @param name Name of aminoacids.
   * @param protein Chain of aminoacids and their mutations.
   */
  public Protein(String name, String protein) { this(name, protein, 1); }

  public Protein(String name, String protein, int start) {
    this(name, protein.contains("^") ? protein.split("\\^", 2)[0] : "",
              (protein.contains("^") ? protein.split("\\^", 2)[1] : protein).split("\\$", 2)[0],
               protein.contains("$") ? protein.split("\\$", 2)[1] : "",
               start + (protein.contains("^") ? protein.split("\\^", 2)[0].replaceAll("/.", "") : "").length());
  }
  
  public Protein(String name, String prefix, String protein, String suffix, int start) {
    check(prefix, "prefix");
    check(protein, "protein");
    check(suffix, "suffix");

    this.name = name;
    this.start = start;
    for (Character shortcut : Monomer.getShortcuts()) {
      String value = Monomer.resolveShortcut(shortcut);
      StringBuilder buf = new StringBuilder(value.length()*2-1);
      buf.append(value.charAt(0));
      for (int i = 1; i < value.length(); i++) {
        buf.append(Polymer.MUTATION_SPLITTER).append(value.charAt(i));
      }
      protein = protein.replace(shortcut.toString(), buf.toString());
    }
    this.prefix = prefix;
    this.protein = protein.replaceAll("/.", "");
    this.suffix = suffix;
    this.mutations = new HashMap(0);
    this.mutationPointers = new TreeMap();
    this.mutationSkip = new HashMap(0);
    this.modifications = new HashMap(0);
    this.terminusModifications = new ArrayList(2) {{ add(null); add(null); }};
    this.modificationPointers = new TreeMap<>();
    this.terminusModificationPointers = new Pointer[] { null, null };
    this.modificationSkip = new HashMap(0);
    this.bonds = new TreeMap<>();
    this.bondsSelected = new TreeMap<>();
    this.bondsPointers = new TreeMap<>();
    mins = new BigDecimal[this.protein.length()];
    maxs = new BigDecimal[this.protein.length()];
    for (int i = 0; i < this.protein.length(); i++) {
      mins[i] = Monomer.mass(this.protein.charAt(i));
      maxs[i] = Monomer.mass(this.protein.charAt(i));
    }
    minTerminus = new BigDecimal[]{ BigDecimal.ZERO, BigDecimal.ZERO };
    maxTerminus = new BigDecimal[]{ massNdefault(), massCdefault() };

    if (protein.contains(Character.toString(MUTATION_SPLITTER))) {
      while (protein.contains(Character.toString(MUTATION_SPLITTER))) {
        int position = protein.indexOf(MUTATION_SPLITTER);
        addMutation(position-1, protein.charAt(position+1));
        protein = protein.replaceFirst("" + MUTATION_SPLITTER + protein.charAt(position+1), "");
      }

      if (!Polymer.isPolymer(this.protein)) {
        throw new UnknownError("Isn't chain of aminoacids.");
      }
    }

    this.bounds = new SimplifiedCleavage[2];
    this.begin = -1;
    this.end = this.protein.length();
  }

  private void check(String protein, String name) throws UnknownError {
    if (protein.isEmpty()) {
      return;
    }
    if (protein.charAt(0) == MUTATION_SPLITTER) {
      throw new UnknownError("Mutation symbol isn't allowed on the begin of the " + name + ".");
    }
    if (protein.charAt(protein.length()-1) == MUTATION_SPLITTER) {
      throw new UnknownError("Mutation symbol isn't allowed at the end of the " + name + ".");
    }
    if (protein.contains("" + MUTATION_SPLITTER + MUTATION_SPLITTER)) {
      throw new UnknownError("Two mutation symbols together in the " + name + ".");
    }
    if (!Polymer.isPolymer(protein, true)) {
      throw new UnknownError("The " + name + " isn't chain of aminoacids.");
    }
  }

  @Override
  public Protein clone() {
    // Nen nutné tvořit vše znovu, jen pointery, ale do budoucna hlídat, kdyby náhodou
    Protein clone = new Protein(name, protein, start);
    clone.mutations = this.mutations;
    clone.mutationPointers.putAll(this.mutationPointers);
    clone.mutationSkip.putAll(this.mutationSkip);
    clone.modifications = this.modifications;
    clone.terminusModifications = this.terminusModifications;
    for (Integer pos : this.modificationPointers.keySet()) {
      clone.modificationPointers.put(pos, new HashMap(this.modificationPointers.get(pos).size()));
      for (Character aa : this.modificationPointers.get(pos).keySet()) {
        clone.modificationPointers.get(pos).put(aa, this.modificationPointers.get(pos).get(aa).clone());
      }
    }
    clone.modificationSkip.putAll(this.modificationSkip);
    clone.bonds = this.bonds;
    clone.bondsSelected.putAll(this.bondsSelected);
    clone.bondsPointers.putAll(this.bondsPointers);
    clone.bounds = new SimplifiedCleavage[this.bounds.length];
    for (int i = 0; i < clone.bounds.length; i++) {
      clone.bounds[i] = this.bounds[i];
    }
    clone.begin = this.begin;
    clone.end = this.end;
    clone.mins = this.mins;
    clone.maxs = this.maxs;
    clone.minTerminus = this.minTerminus;
    clone.maxTerminus = this.maxTerminus;
    clone.diff = this.diff;

    return clone;
  }

  //<editor-fold defaultstate="collapsed" desc=" Configuration settings ">
  public boolean reset(SimplifiedCleavage begin, SimplifiedCleavage end, java.util.Map<Integer, Character> fixed) {
    diff = BigDecimal.ZERO;
    return resetMutations(begin, end, fixed);
  }

  public boolean nextConfiguration() {
    diff = BigDecimal.ZERO;
    return nextConfigurationModifications() || nextConfigurationMutations();
  }

  //<editor-fold defaultstate="collapsed" desc=" Mutations settings ">
  /**
   * Set mutation.
   * @param index Position of mutation in the protein.
   * @param m Mutation.
   */
  public final void addMutation(int index, char m) {
    if (index < 0 || index >= protein.length()) {
      throw new java.lang.IndexOutOfBoundsException("Position is out of protein.");
    }
    if (charsAt(index).contains(m)) {
      return;
    }
    if (!mutations.containsKey(index)) {
      mutations.put(index, new ArrayList<Character>(1));
      mutationPointers.put(index, 0);
    }
    mutations.get(index).add(m);
    mins[index] = mins[index].min(Monomer.mass(m));
    maxs[index] = maxs[index].max(Monomer.mass(m));
  }

  public final void deleteMutations(int index) {
    mutations.remove(index);
    mutationPointers.remove(index);
    // TODO: Mělo by se aktualizovat mins[index] a maxs[index] ale tak to zatím jen zkrátka bude probírat víc možností...
    // TODO: Měly by se aktualizovat seznamy modifikací a můstků, ale v současnosti se přidávají až později, takže jsou prázdné
  }

  public boolean resetMutations(int begin, SimplifiedCleavage end) {
    return resetMutations(new SimplifiedCleavage(begin, new HashMap<Integer, Character>(0), new HashMap<Integer, Character>(0), 0, new Protease("", ";")), end);
  }

  public boolean resetMutations(SimplifiedCleavage begin, int end) {
    return resetMutations(begin, new SimplifiedCleavage(end, new HashMap<Integer, Character>(0), new HashMap<Integer, Character>(0), 0, new Protease("", ";")));
  }

  public boolean resetMutations(SimplifiedCleavage begin, SimplifiedCleavage end) {
    return resetMutations(begin, end, new java.util.HashMap<Integer, Character>(0));
  }

  // TODO: Zbytečné tvoření SimplifiedCleavage, ale tak mělo by jich být jen lineárně vzhledem k počtu volání metody, tak to snad není až takový problém...
  //       Byl by to problém, pokud by bylo hodně mutací, z nichž by bylo jen málo přípustných
  public boolean resetMutations(SimplifiedCleavage begin, SimplifiedCleavage end, java.util.Map<Integer, Character> fixed) {
    bounds[0] = begin;
    bounds[1] = end;
    this.begin = begin.getPosition() == 0 ? -1 : begin.getPosition();
    this.end = end.getPosition() == length() ? end.getPosition() : end.getPosition()-1;
    this.mutationSkip = fixed;
    for (Integer key : mutationPointers.subMap(this.begin, true, this.end, true).keySet()) {
      if (!(modificationSkip.containsKey(key) || mutationSkip.containsKey(key))) {
        // V těch dvou případech snad není potřeba resetovat, takhle se ušetří čas zbytečným nastavováním
        mutationPointers.put(key, 0);
      }
    }
    for (Integer mod : modificationSkip.keySet()) {
      if (mutationSkip.containsKey(mod) && !modificationSkip.get(mod).equals(mutationSkip.get(mod))) {
        return false;
      }
    }
    // Možná by bylo lepší před kvůli blokaci pozic můstky, ale tak to stačí použít opačný pořadí.
    // Alternativní přístup je vyresetovat i modificationSkip - možné i je zadávat současně s resetem
    return resetModifications() || nextConfigurationMutations();
  }

  public boolean nextConfigurationMutations() {
    do {
      Iterator<Integer> iterator = mutationPointers.subMap(begin, true, end, true).keySet().iterator();
      boolean out = true;
      while (iterator.hasNext()) {
        int index = iterator.next();
        if (!(modificationSkip.containsKey(index) || mutationSkip.containsKey(index))) {
          diff = diff.subtract(Monomer.mass(charAt(index))); // NON-OPT: Opakovaně se bude ptát, zda není v těch slovnících
          if (mutationPointers.get(index) < mutations.get(index).size()) {
            diff = diff.add(Monomer.mass(mutations.get(index).get(mutationPointers.get(index))));
            mutationPointers.put(index, mutationPointers.get(index)+1);
            out = false;
            break;
          }
          mutationPointers.put(index, 0);
          diff = diff.add(Monomer.mass(protein.charAt(index)));
        }
      }
      if (out) {
        return false;
      }
    } while (!resetModifications());
    return true;
  }

  public Map<Integer, Character> getConfigurationMutations() {
    Map<Integer, Character> sm = new HashMap<>();
    Iterator<Integer> iterator = mutationPointers.subMap(begin, true, end, true).keySet().iterator();
    while (iterator.hasNext()) {
      int index = iterator.next();
      if (mutationSkip.containsKey(index)) {
        sm.put(index, mutationSkip.get(index));
      } else if (modificationSkip.containsKey(index)) {
        // Předpoklad konzistence mutationSkip a modificationSkip
        sm.put(index, modificationSkip.get(index));
      } else {
        if (mutations.containsKey(index) && mutationPointers.get(index) > 0) {
          sm.put(index, mutations.get(index).get(mutationPointers.get(index)-1));
        } else {
          sm.put(index, protein.charAt(index));
        }
      }
    }
    return sm;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc=" Modifications settings ">
  public void removeModifications() {
    modifications.clear();
    modificationPointers.clear();
    terminusModifications.add(0, null);
    terminusModifications.add(1, null);
    bonds.clear();
  }

  public void addFixedModification(ModificationReal modification, int position, int level) {
    if (!modifications.containsKey(position)) {
      modifications.put(position, new HashMap(1));
    }
    if (!modifications.get(position).containsKey(modification.getAa())) {
      modifications.get(position).put(modification.getAa(), new TreeMap());
    } else if (modifications.get(position).get(modification.getAa()).remove(Integer.MAX_VALUE) == null &&
               modifications.get(position).get(modification.getAa()).lastKey() < level) {
      // Nemá smysl modifikaci přidávat, protože v některá z předchozích úrovní je fixní.
      return;
    }
    while (modifications.get(position).get(modification.getAa()).higherKey(level) != null) {
      // Modifikace ve vyšších úrovních nemají šanci se prosadit
      modifications.get(position).get(modification.getAa()).remove(modifications.get(position).get(modification.getAa()).higherKey(level));
    }
    if (!modifications.get(position).get(modification.getAa()).containsKey(level)) {
      modifications.get(position).get(modification.getAa()).put(level, new TreeSet());
    }
    modifications.get(position).get(modification.getAa()).get(level).add(modification);
    // TODO: Měly by se aktualizovat mins[index] a maxs[index] ale tak to zatím jen zkrátka bude probírat víc možností...
    addPointer(position, modification.getAa());
    updateMinMax(position, modification.getAa(), modification.getModification());
  }

  public void addFixedModification(ModificationReal modification, int level) {
    if (modification.getAa() == '^') {
      if (terminusModifications.get(0) == null) {
        terminusModifications.set(0, new TreeMap());
        terminusModificationPointers[0] = new Pointer(level, modification);
      }
      if (!terminusModifications.get(0).containsKey(level)) {
        terminusModifications.get(0).put(level, new TreeSet());
      }
      terminusModifications.get(0).get(level).add(modification);
      terminusModifications.get(0).get(level).remove(EMPTY_MODIFICATION);
      updateMinMax(modification.getAa(), modification.getModification());
    } else if (modification.getAa() == '$') {
      if (terminusModifications.get(1) == null) {
        terminusModifications.set(1, new TreeMap());
        terminusModificationPointers[1] = new Pointer(level, modification);
      }
      if (!terminusModifications.get(1).containsKey(level)) {
        terminusModifications.get(1).put(level, new TreeSet());
      }
      terminusModifications.get(1).get(level).add(modification);
      terminusModifications.get(1).get(level).remove(EMPTY_MODIFICATION);
      updateMinMax(modification.getAa(), modification.getModification());
    } else {
      for (Integer pos : positions(modification.getAa())) {
        addFixedModification(modification, pos, level);
      }
    }
  }

  public void addVariableModification(ModificationReal modification, int position, int level) {
    if (!modifications.containsKey(position)) {
      modifications.put(position, new HashMap(1));
    }
    if (!modifications.get(position).containsKey(modification.getAa())) {
      modifications.get(position).put(modification.getAa(), new TreeMap());
      modifications.get(position).get(modification.getAa()).put(Integer.MAX_VALUE, new TreeSet() {{ add(EMPTY_MODIFICATION); }});
    } else if (!modifications.get(position).get(modification.getAa()).containsKey(Integer.MAX_VALUE) &&
               modifications.get(position).get(modification.getAa()).lastKey() < level) {
      // Nemá smysl modifikaci přidávat, protože v některé z předchozích úrovní je fixní
      return;
    }
    if (!modifications.get(position).get(modification.getAa()).containsKey(level)) {
      modifications.get(position).get(modification.getAa()).put(level, new TreeSet());
    }
    modifications.get(position).get(modification.getAa()).get(level).add(modification);

    addPointer(position, modification.getAa());
    updateMinMax(position, modification.getAa(), modification.getModification());
  }

  public void addVariableModification(ModificationReal modification, int level) {
    if (modification.getAa() == '^') {
      if (terminusModifications.get(0) == null) {
        terminusModifications.set(0, new TreeMap());
        terminusModificationPointers[0] = new Pointer(level, modification);
      }
      if (!terminusModifications.get(0).containsKey(level)) {
        terminusModifications.get(0).put(level, new TreeSet());
        terminusModifications.get(0).get(level).add(EMPTY_MODIFICATION);
      }
      terminusModifications.get(0).get(level).add(modification);
      updateMinMax(modification.getAa(), modification.getModification());
    } else if (modification.getAa() == '$') {
      if (terminusModifications.get(1) == null) {
        terminusModifications.set(1, new TreeMap());
        terminusModificationPointers[1] = new Pointer(level, modification);
      }
      if (!terminusModifications.get(1).containsKey(level)) {
        terminusModifications.get(1).put(level, new TreeSet());
        terminusModifications.get(1).get(level).add(EMPTY_MODIFICATION);
      }
      terminusModifications.get(1).get(level).add(modification);
      updateMinMax(modification.getAa(), modification.getModification());
    } else {
      for (Integer pos : positions(modification.getAa())) {
        addVariableModification(modification, pos, level);
      }
    }
  }

  private void addPointer(int position, char aminoacid) {
    if (!modificationPointers.containsKey(position)) {
      modificationPointers.put(position, new HashMap<Character, Pointer>(1));
      for (Character c : modifications.get(position).keySet()) {
        int level = modifications.get(position).get(c).firstKey();
        modificationPointers.get(position).put(c, new Pointer(level, modifications.get(position).get(c).get(level).first()));
      }
    } else {
      int level = modifications.get(position).get(aminoacid).firstKey();
      modificationPointers.get(position).put(aminoacid, new Pointer(level, modifications.get(position).get(aminoacid).get(level).first()));
    }
  }

  /**
   * It is recommended to call firstly this and then reset() or resetMutation(), because blocked position will affect whether and what positions will be reset and how it will be done.
   * @param position
   * @param aminoacid
   * @return
   */
  public boolean blockPosition(int position, char aminoacid) {
    // Musí být splněno, aby vůbec šlo nastavit
    if (aminoacid == '^' && position == -1) {
      if (terminusModifications.get(0) != null) {
        for (TreeSet<ModificationReal> treeSet : terminusModifications.get(0).values()) {
          if (!treeSet.contains(EMPTY_MODIFICATION)) {
            return false;
          }
        }
      }
    } else if (aminoacid == '$' && position == length()) {
      if (terminusModifications.get(1) != null) {
        for (TreeSet<ModificationReal> treeSet : terminusModifications.get(1).values()) {
          if (!treeSet.contains(EMPTY_MODIFICATION)) {
            return false;
          }
        }
      }
    } else if (!(position >= 0 && position < protein.length() && charsAt(position).contains(aminoacid) &&
           (!modifications.containsKey(position) || !modifications.get(position).containsKey(aminoacid) ||
            modifications.get(position).get(aminoacid).containsKey(Integer.MAX_VALUE)))) {
      return false;
    }
    modificationSkip.clear();
    modificationSkip.put(position, aminoacid);
    // Testuje se až po nastavení kvůli tomu, aby bylo jedno, zda se mutationSkip nastavuje předtím, nebo potom.
    if (aminoacid != '^' && aminoacid != '$' && mutationSkip.containsKey(position) && !mutationSkip.get(position).equals(aminoacid)) {
      return false;
    }
    return true;
  }

  public void unblockPositions() {
    modificationSkip.clear();
  }

  private boolean resetModifications() { // Resetování jen pro aktuální nastavení mutací
    // U toho je jisté, že vždy lze.
    resetBonds();

    // Proteáza by chtěla zamknout oba konce, ale délka peptidu je 1
    if (begin == end && bounds[0].getProtease().isLockedRight() && bounds[1].getProtease().isLockedLeft()) {
      return false;
    }

    if (bounds[0].getProtease().isLockedRight() || bounds[0].getProtease().getModificationRight().compareTo(BigDecimal.ZERO) != 0 ||
        terminusModifications.get(0) == null || modificationSkip.containsKey(-1) || terminusModifications.get(0).ceilingKey(bounds[0].getLevel()) == null) {
      if (terminusModificationPointers[0] != null) {
        diff = diff.subtract(terminusModificationPointers[0].getModification().getModification());
        terminusModificationPointers[0] = null;
      }
    } else {
      if (terminusModificationPointers[0] == null) {
        terminusModificationPointers[0] = new Pointer(terminusModifications.get(0).ceilingKey(bounds[0].getLevel()),
                                                      terminusModifications.get(0).ceilingEntry(bounds[0].getLevel()).getValue().first());
      } else {
        diff = diff.subtract(terminusModificationPointers[0].getModification().getModification());
        terminusModificationPointers[0].setLevel(terminusModifications.get(0).ceilingKey(bounds[0].getLevel()));
        terminusModificationPointers[0].setModification(terminusModifications.get(0).ceilingEntry(bounds[0].getLevel()).getValue().first());
      }
      if (terminusModificationPointers[0].getModification() == EMPTY_MODIFICATION) { // Znalost, že "" není v žádné úrovni samo.
        terminusModificationPointers[0].setModification(terminusModifications.get(0).ceilingEntry(bounds[0].getLevel()).getValue().higher(EMPTY_MODIFICATION));
      }
      diff = diff.add(terminusModificationPointers[0].getModification().getModification());
    }
    if (bounds[1].getProtease().isLockedLeft() || bounds[1].getProtease().getModificationLeft().compareTo(BigDecimal.ZERO) != 0 ||
        terminusModifications.get(1) == null || modificationSkip.containsKey(length()) || terminusModifications.get(1).ceilingKey(bounds[1].getLevel()) == null) {
      if (terminusModificationPointers[1] != null) {
        diff = diff.subtract(terminusModificationPointers[1].getModification().getModification());
        terminusModificationPointers[1] = null;
      }
    } else {
      if (terminusModificationPointers[1] == null) {
        terminusModificationPointers[1] = new Pointer(terminusModifications.get(1).ceilingKey(bounds[1].getLevel()),
                                                      terminusModifications.get(1).ceilingEntry(bounds[1].getLevel()).getValue().first());
      } else {
        diff = diff.subtract(terminusModificationPointers[1].getModification().getModification());
        terminusModificationPointers[1].setLevel(terminusModifications.get(1).ceilingKey(bounds[1].getLevel()));
        terminusModificationPointers[1].setModification(terminusModifications.get(1).ceilingEntry(bounds[1].getLevel()).getValue().first());
      }
      if (terminusModificationPointers[1].getModification() == EMPTY_MODIFICATION) { // Znalost, že "" není v žádné úrovni samo.
        terminusModificationPointers[1].setModification(terminusModifications.get(1).ceilingEntry(bounds[1].getLevel()).getValue().higher(EMPTY_MODIFICATION));
      }
      diff = diff.add(terminusModificationPointers[1].getModification().getModification());
    }

    char aa;
    if (begin == end && !bounds[0].getProtease().isFlexibleRight() && !bounds[1].getProtease().isFlexibleLeft()) {
      if (modificationPointers.containsKey(begin) && modificationPointers.get(begin).containsKey(charAt(begin))) {
        aa = charAt(begin);
        if (modificationSkip.containsKey(begin) || bounds[0].getProtease().isLockedRight() || bounds[1].getProtease().isLockedLeft()) {
          // Když je pozice uzamknutá můstkem, nebo tam uzamyká proteáza
          if (!modifications.get(begin).get(aa).lastEntry().getValue().contains(EMPTY_MODIFICATION)) {
            return false;
          }
          diff = diff.subtract(modificationPointers.get(begin).get(aa).getModification().getModification());
          modificationPointers.get(begin).get(aa).setLevel(modifications.get(begin).get(aa).lastKey());
          modificationPointers.get(begin).get(aa).setModification(EMPTY_MODIFICATION);
        } else {
          int level = Math.max(bounds[0].getLevel(), bounds[1].getLevel());
          if (modifications.get(begin).get(aa).ceilingKey(level) == null) {
            return false;
          }
          diff = diff.subtract(modificationPointers.get(begin).get(aa).getModification().getModification());
          modificationPointers.get(begin).get(aa).setLevel(modifications.get(begin).get(aa).ceilingKey(level));
          modificationPointers.get(begin).get(aa).setModification(modifications.get(begin).get(aa).ceilingEntry(level).getValue().first());
          diff = diff.add(modificationPointers.get(begin).get(aa).getModification().getModification());
        }
      }
    } else {
      if (modificationPointers.containsKey(begin) && modificationPointers.get(begin).containsKey(charAt(begin)) && !bounds[0].getProtease().isFlexibleRight()) {
        aa = charAt(begin);
        if (modificationSkip.containsKey(begin) || bounds[0].getProtease().isLockedRight()) {
          // Když je pozice uzamknutá můstkem, nebo tam uzamyká proteáza
          if (!modifications.get(begin).get(aa).lastEntry().getValue().contains(EMPTY_MODIFICATION)) {
            return false;
          }
          diff = diff.subtract(modificationPointers.get(begin).get(aa).getModification().getModification());
          modificationPointers.get(begin).get(aa).setLevel(modifications.get(begin).get(aa).lastKey());
          modificationPointers.get(begin).get(aa).setModification(EMPTY_MODIFICATION);
        } else {
          if (modifications.get(begin).get(aa).ceilingKey(bounds[0].getLevel()) == null) {
            return false;
          }
          diff = diff.subtract(modificationPointers.get(begin).get(aa).getModification().getModification());
          modificationPointers.get(begin).get(aa).setLevel(modifications.get(begin).get(aa).ceilingKey(bounds[0].getLevel()));
          modificationPointers.get(begin).get(aa).setModification(modifications.get(begin).get(aa).ceilingEntry(bounds[0].getLevel()).getValue().first());
          diff = diff.add(modificationPointers.get(begin).get(aa).getModification().getModification());
        }
      }
      // TODO: Pointer včetně začátku/ konce zohlednit IsLockedLeft/Right - musí se zohlednit i při next & můstky & odmazat z resetování terminů
      for (Integer pos : modificationPointers.subMap(begin, bounds[0].getProtease().isFlexibleRight(), end, bounds[1].getProtease().isFlexibleLeft()).keySet()) {
        aa = charAt(pos);
        if (modificationPointers.get(pos).containsKey(aa)) {
          if (modificationSkip.containsKey(pos)) {
            if (!modifications.get(pos).get(aa).lastEntry().getValue().contains(EMPTY_MODIFICATION)) {
              return false;
            }
            diff = diff.subtract(modificationPointers.get(pos).get(aa).getModification().getModification());
            modificationPointers.get(pos).get(aa).setLevel(modifications.get(pos).get(aa).lastKey());
            modificationPointers.get(pos).get(aa).setModification(EMPTY_MODIFICATION);
          } else {
            diff = diff.subtract(modificationPointers.get(pos).get(aa).getModification().getModification());
            modificationPointers.get(pos).get(aa).setLevel(modifications.get(pos).get(aa).firstKey());
            modificationPointers.get(pos).get(aa).setModification(modifications.get(pos).get(aa).firstEntry().getValue().first());
            diff = diff.add(modificationPointers.get(pos).get(aa).getModification().getModification());
          }
        }
      }
      if (modificationPointers.containsKey(end) && modificationPointers.get(end).containsKey(charAt(end)) && !bounds[1].getProtease().isFlexibleLeft()) {
        aa = charAt(end);
        if (modificationSkip.containsKey(end) || bounds[1].getProtease().isLockedLeft()) {
          // Když je pozice uzamknutá můstkem, nebo tam uzamyká proteáza
          if (!modifications.get(end).get(aa).lastEntry().getValue().contains(EMPTY_MODIFICATION)) {
            return false;
          }
          diff = diff.subtract(modificationPointers.get(end).get(aa).getModification().getModification());
          modificationPointers.get(end).get(aa).setLevel(modifications.get(end).get(aa).lastKey());
          modificationPointers.get(end).get(aa).setModification(EMPTY_MODIFICATION);
        } else {
          if (modifications.get(end).get(aa).ceilingKey(bounds[1].getLevel()) == null) {
            return false;
          }
          diff = diff.subtract(modificationPointers.get(end).get(aa).getModification().getModification());
          modificationPointers.get(end).get(aa).setLevel(modifications.get(end).get(aa).ceilingKey(bounds[1].getLevel()));
          modificationPointers.get(end).get(aa).setModification(modifications.get(end).get(aa).ceilingEntry(bounds[1].getLevel()).getValue().first());
          diff = diff.add(modificationPointers.get(end).get(aa).getModification().getModification());
        }
      }
    }

    return true;
  }

  private boolean nextConfigurationModifications() {
    if (nextConfigurationBonds()) {
      return true;
    }
    resetBonds();

    // Kvůli defaultnímu setřídění generováno "odzadu"
    if (nextConfigurationModificationTerminus(1)) return true;
    
    char aa;
    if (begin == end && !bounds[0].getProtease().isFlexibleRight() && !bounds[1].getProtease().isFlexibleLeft()) {
      if (modificationPointers.containsKey(begin) && modificationPointers.get(begin).containsKey(charAt(begin)) &&
          !modificationSkip.containsKey(begin) && !bounds[0].getProtease().isLockedRight() && !bounds[1].getProtease().isLockedLeft()) {
        aa = charAt(begin);
        Pointer p = modificationPointers.get(begin).get(aa);
        diff = diff.subtract(p.getModification().getModification());
        ModificationReal next = modifications.get(begin).get(aa).get(p.getLevel()).higher(p.getModification());
        if (next == null) {
          if (modifications.get(begin).get(aa).higherKey(p.getLevel()) == null) {
            // Vynulování
            int level = Math.max(bounds[0].getLevel(), bounds[1].getLevel());
            modificationPointers.get(begin).get(aa).setLevel(modifications.get(begin).get(aa).ceilingKey(level));
            next = modifications.get(begin).get(aa).ceilingEntry(level).getValue().first();
            modificationPointers.get(begin).get(aa).setModification(next);
            diff = diff.add(next.getModification());
          } else {
            // Další úroveň
            modificationPointers.get(begin).get(aa).setLevel(modifications.get(begin).get(aa).higherKey(p.getLevel()));
            next = modifications.get(begin).get(aa).get(p.getLevel()).first();
            modificationPointers.get(begin).get(aa).setModification(next);
            diff = diff.add(next.getModification());
            return true;
          }
        } else {
          // Další modifikace
          modificationPointers.get(begin).get(aa).setModification(next);
          diff = diff.add(next.getModification());
          return true;
        }
      }
    } else {
      if (modificationPointers.containsKey(end) && modificationPointers.get(end).containsKey(charAt(end)) && !bounds[1].getProtease().isFlexibleLeft() &&
          !modificationSkip.containsKey(end) && !bounds[1].getProtease().isLockedLeft()) {
        aa = charAt(end);
        Pointer p = modificationPointers.get(end).get(aa);
        diff = diff.subtract(p.getModification().getModification());
        ModificationReal next = modifications.get(end).get(aa).get(p.getLevel()).higher(p.getModification());
        if (next == null) {
          if (modifications.get(end).get(aa).higherKey(p.getLevel()) == null) {
            // Vynulování
            modificationPointers.get(end).get(aa).setLevel(modifications.get(end).get(aa).ceilingKey(bounds[1].getLevel()));
            next = modifications.get(end).get(aa).ceilingEntry(bounds[1].getLevel()).getValue().first();
            modificationPointers.get(end).get(aa).setModification(next);
            diff = diff.add(next.getModification());
          } else {
            // Další úroveň
            modificationPointers.get(end).get(aa).setLevel(modifications.get(end).get(aa).higherKey(p.getLevel()));
            next = modifications.get(end).get(aa).get(p.getLevel()).first();
            modificationPointers.get(end).get(aa).setModification(next);
            diff = diff.add(next.getModification());
            return true;
          }
        } else {
          // Další modifikace
          modificationPointers.get(end).get(aa).setModification(next);
          diff = diff.add(next.getModification());
          return true;
        }
      }
      for (Integer pos : modificationPointers.subMap(begin, bounds[0].getProtease().isFlexibleRight(), end, bounds[1].getProtease().isFlexibleLeft()).descendingKeySet()) {
        aa = charAt(pos);
        if (!modificationSkip.containsKey(pos) && modificationPointers.get(pos).containsKey(aa)) {
          Pointer p = modificationPointers.get(pos).get(aa);
          diff = diff.subtract(p.getModification().getModification());
          ModificationReal next = modifications.get(pos).get(aa).get(p.getLevel()).higher(p.getModification());
          if (next == null) {
            if (modifications.get(pos).get(aa).higherKey(p.getLevel()) == null) {
              // Vynulování
              modificationPointers.get(pos).get(aa).setLevel(modifications.get(pos).get(aa).firstKey());
              next = modifications.get(pos).get(aa).firstEntry().getValue().first();
              modificationPointers.get(pos).get(aa).setModification(next);
              diff = diff.add(next.getModification());
            } else {
              // Další úroveň
              modificationPointers.get(pos).get(aa).setLevel(modifications.get(pos).get(aa).higherKey(p.getLevel()));
              next = modifications.get(pos).get(aa).get(p.getLevel()).first();
              modificationPointers.get(pos).get(aa).setModification(next);
              diff = diff.add(next.getModification());
              return true;
            }
          } else {
            // Další modifikace
            modificationPointers.get(pos).get(aa).setModification(next);
            diff = diff.add(next.getModification());
            return true;
          }
        }
      }
      if (modificationPointers.containsKey(begin) && modificationPointers.get(begin).containsKey(charAt(begin)) && !bounds[0].getProtease().isFlexibleRight() &&
          !modificationSkip.containsKey(begin) && !bounds[0].getProtease().isLockedRight()) {
        aa = charAt(begin);
        Pointer p = modificationPointers.get(begin).get(aa);
        diff = diff.subtract(p.getModification().getModification());
        ModificationReal next = modifications.get(begin).get(aa).get(p.getLevel()).higher(p.getModification());
        if (next == null) {
          if (modifications.get(begin).get(aa).higherKey(p.getLevel()) == null) {
            // Vynulování
            modificationPointers.get(begin).get(aa).setLevel(modifications.get(begin).get(aa).ceilingKey(bounds[0].getLevel()));
            next = modifications.get(begin).get(aa).ceilingEntry(bounds[0].getLevel()).getValue().first();
            modificationPointers.get(begin).get(aa).setModification(next);
            diff = diff.add(next.getModification());
          } else {
            // Další úroveň
            modificationPointers.get(begin).get(aa).setLevel(modifications.get(begin).get(aa).higherKey(p.getLevel()));
            next = modifications.get(begin).get(aa).get(p.getLevel()).first();
            modificationPointers.get(begin).get(aa).setModification(next);
            diff = diff.add(next.getModification());
            return true;
          }
        } else {
          // Další modifikace
          modificationPointers.get(begin).get(aa).setModification(next);
          diff = diff.add(next.getModification());
          return true;
        }
      }
    }
    
    if (nextConfigurationModificationTerminus(0)) return true;
    return false;
  }

  private boolean nextConfigurationModificationTerminus(int side) {
    // Při iteraci přes modifikace okrajů jít do vyšší úrovně jen pokud aktuální obsahuje ""
    // Ve všech úrovních kromě poslední přeskakovat ""
    if (terminusModificationPointers[side] != null) {
      diff = diff.subtract(terminusModificationPointers[side].getModification().getModification());
      if (terminusModificationPointers[side].getModification() == EMPTY_MODIFICATION) {
        // znamená poslední ukazatel
        terminusModificationPointers[side].setLevel(terminusModifications.get(side).ceilingKey(bounds[side].getLevel()));
        terminusModificationPointers[side].setModification(terminusModifications.get(side).get(terminusModificationPointers[side].getLevel()).first());
        if (terminusModificationPointers[side].getModification() == EMPTY_MODIFICATION) {
          terminusModificationPointers[side].setModification(terminusModifications.get(side).get(terminusModificationPointers[side].getLevel()).higher(EMPTY_MODIFICATION));
        }
        diff = diff.add(terminusModificationPointers[side].getModification().getModification());
      } else {
        ModificationReal next = terminusModifications.get(side).get(terminusModificationPointers[side].getLevel()).higher(terminusModificationPointers[side].getModification());
        if (next == EMPTY_MODIFICATION) {
          // Pokud by v některé z dalších úrovní byla fixní, tak se "" neprosadí
          next = terminusModifications.get(side).get(terminusModificationPointers[side].getLevel()).higher(next);
        }
        if (next == null) {
          if (terminusModifications.get(side).get(terminusModificationPointers[side].getLevel()).contains(EMPTY_MODIFICATION)) {
            if (terminusModifications.get(side).higherKey(terminusModificationPointers[side].getLevel()) == null) {
              // Je možné být bez modifikace
              terminusModificationPointers[side].setModification(EMPTY_MODIFICATION);
              diff = diff.add(EMPTY_MODIFICATION.getModification());
              return true;
            } else {
              // Přejití do další úrovně
              terminusModificationPointers[side].setLevel(terminusModifications.get(side).higherKey(terminusModificationPointers[side].getLevel()));
              terminusModificationPointers[side].setModification(terminusModifications.get(side).get(terminusModificationPointers[side].getLevel()).first());
              if (terminusModificationPointers[side].getModification() == EMPTY_MODIFICATION) {
                terminusModificationPointers[side].setModification(terminusModifications.get(side).get(terminusModificationPointers[side].getLevel()).higher(EMPTY_MODIFICATION));
              }
              diff = diff.add(terminusModificationPointers[side].getModification().getModification());
              return true;
            }
          } else {
            // Když tato neobsahuje "", tak vynulovat
            terminusModificationPointers[side].setLevel(terminusModifications.get(side).ceilingKey(bounds[side].getLevel()));
            terminusModificationPointers[side].setModification(terminusModifications.get(side).get(terminusModificationPointers[side].getLevel()).first());
            if (terminusModificationPointers[side].getModification() == EMPTY_MODIFICATION) {
              terminusModificationPointers[side].setModification(terminusModifications.get(side).get(terminusModificationPointers[side].getLevel()).higher(EMPTY_MODIFICATION));
            }
            diff = diff.add(terminusModificationPointers[side].getModification().getModification());
          }
        } else {
          // Další modifikace
          terminusModificationPointers[side].setModification(next);
          diff = diff.add(next.getModification());
          return true;
        }
      }
    }
    return false;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc=" Bonds settings ">
  public void addVariableBond(BondReal bond) {
    if (bond.getPositionLeft() == null) {
      for (Integer pos : positions(bond.getAaLeft())) {
        addVariableBond(bond, pos);
      }
    } else {
      addVariableBond(bond, bond.getPositionLeft());
    }
  }

  private void addVariableBond(BondReal bond, int positionLeft) {
    if (bond.getPositionRight() == null) {
      for (Integer pos : positions(bond.getAaRight())) {
        addVariableBond(bond, positionLeft, bond.getAaLeft(), pos, bond.getAaRight());
        addVariableBond(bond.reverse(), pos, bond.getAaRight(), positionLeft, bond.getAaLeft());
      }
    } else {
      addVariableBond(bond, positionLeft, bond.getAaLeft(), bond.getPositionRight(), bond.getAaRight());
      addVariableBond(bond.reverse(), bond.getPositionRight(), bond.getAaRight(), positionLeft, bond.getAaLeft());
    }
  }

  private void addVariableBond(BondReal bond, int position1, char aa1, int position2, char aa2) {
    if (position1 == position2) {
      return;
    }
    if (!bonds.containsKey(position1)) {
      bonds.put(position1, new HashMap<Character, TreeMap<Integer, Map<Character, TreeSet<BondReal>>>>(1));
    }
    if (!bonds.get(position1).containsKey(aa1)) {
      bonds.get(position1).put(aa1, new TreeMap<Integer, Map<Character, TreeSet<BondReal>>>());
    }
    if (!bonds.get(position1).get(aa1).containsKey(position2)) {
      bonds.get(position1).get(aa1).put(position2, new HashMap<Character, TreeSet<BondReal>>(1));
    }
    if (!bonds.get(position1).get(aa1).get(position2).containsKey(aa2)) {
      bonds.get(position1).get(aa1).get(position2).put(aa2, new TreeSet<BondReal>());
    }
    bonds.get(position1).get(aa1).get(position2).get(aa2).add(bond);
    // NON-OPT: Prakticky se kontroluje dvakrát, ale logicky patří spíš sem než do předchozí metody.
    if (position1 == -1 || position1 == length()) {
      updateMinMax(aa1, bond.getBond().divide(new BigDecimal(2)));
    } else {
      updateMinMax(position1, aa1, bond.getBond().divide(new BigDecimal(2)));
    }
    if (position2 == -1 || position2 == length()) {
      updateMinMax(aa2, bond.getBond().divide(new BigDecimal(2)));
    } else {
      updateMinMax(position2, aa2, bond.getBond().divide(new BigDecimal(2)));
    }
  }

  private void resetBonds() {
    for (BondReal bond : bondsPointers.values()) { // Sice by melo být zbytečné (Navíc zda zohlednit interval?), neboť NEXT vrátí FALSE, když je prázdné, ale...
      diff = diff.subtract(bond.getBond());
    }
    bondsPointers.clear();
    bondsSelected.clear();
//    for (Integer index : bondsSelected.subMap(begin, true, end, true).keySet()) {
//      if (bondsPointers.containsKey(index)) {
//        bondsPointers.remove(index);
//      } else {
//        bondsPointers.remove(bondsSelected.get(index));
//      }
//      bondsSelected.remove(bondsSelected.get(index));
//      bondsSelected.remove(index);
//    }
  }

  private boolean nextConfigurationBonds() {
    for (Integer i : bonds.subMap(begin, bounds[0].getProtease().isFlexibleRight(), end, false).keySet()) {
      if (!modificationSkip.containsKey(i) && bonds.get(i).containsKey(charAt(i)) &&
          (!modificationPointers.containsKey(i) || !modificationPointers.get(i).containsKey(charAt(i)) ||
           modificationPointers.get(i).get(charAt(i)).getModification() == EMPTY_MODIFICATION) &&
          (i >= 0 || terminusModificationPointers[0] == null || terminusModificationPointers[0].getModification() == EMPTY_MODIFICATION)) {
        for (Integer j : bonds.get(i).get(charAt(i)).subMap(i, false, end, bounds[1].getProtease().isFlexibleLeft()).keySet()) {
          if (!modificationSkip.containsKey(j) && bonds.get(i).get(charAt(i)).get(j).containsKey(charAt(j)) &&
              (!modificationPointers.containsKey(j) || !modificationPointers.get(j).containsKey(charAt(j)) ||
               modificationPointers.get(j).get(charAt(j)).getModification() == EMPTY_MODIFICATION) &&
              (i < length() || terminusModificationPointers[1] == null || terminusModificationPointers[1].getModification() == EMPTY_MODIFICATION)) {
            TreeSet<BondReal> poss = bonds.get(i).get(charAt(i)).get(j).get(charAt(j));
            if (bondsPointers.containsKey(i) && bondsSelected.get(i) == j) {
              // Když už mezi těmi pozicemi můstek je, tak se zkusí, zda je další, jinak se odstraní
              BondReal curr = bondsPointers.get(i);
              BondReal next = poss.higher(curr);
              diff = diff.subtract(curr.getBond());
              if (next != null) {
                bondsPointers.put(i, next);
                diff = diff.add(next.getBond());
                return true;
              }
              bondsSelected.remove(i);
              bondsSelected.remove(j);
              bondsPointers.remove(i);
            } else if (!(bondsSelected.containsKey(i) || bondsSelected.containsKey(j))) {
              // Když jsou obě pozice volné
              bondsSelected.put(i, j);
              bondsSelected.put(j, i);
              bondsPointers.put(i, poss.first());
              diff = diff.add(poss.first().getBond());
              return true;
            }
          }
        }
      }
    }
    return false;
  }
  //</editor-fold>
  //</editor-fold>

  @Override
  public int length() {
    return protein.length();
  }

  public int start() {
    return start;
  }

  @Override
  public char charAt(int index) {
    if (index == -1) {
      return '^';
    }
    if (index == length()) {
      return '$';
    }
    if (mutationSkip.containsKey(index)) {
      return mutationSkip.get(index);
    }
    if (modificationSkip.containsKey(index)) {
      return modificationSkip.get(index);
    }
    if (mutations.containsKey(index) && mutationPointers.get(index) > 0) {
      return mutations.get(index).get(mutationPointers.get(index)-1);
    }
    return protein.charAt(index);
  }

  //<editor-fold defaultstate="collapsed" desc=" Masses ">
  public void updateMinMax(int position, char aminoacid, BigDecimal modification) {
    mins[position] = mins[position].min(Monomer.mass(aminoacid).add(modification));
    maxs[position] = maxs[position].max(Monomer.mass(aminoacid).add(modification));
  }

  public void updateMinMax(char aminoacid, BigDecimal modification) {
    switch (aminoacid) {
      case '^':
        minTerminus[0] = minTerminus[0].min(massNdefault().add(modification));
        maxTerminus[0] = maxTerminus[0].max(massNdefault().add(modification));
        break;
      case '$':
        minTerminus[1] = minTerminus[1].min(massCdefault().add(modification));
        maxTerminus[1] = maxTerminus[1].max(massCdefault().add(modification));
        break;
      default:
        for (Integer pos : positions(aminoacid)) {
          updateMinMax(pos, aminoacid, modification);
        }
    }
  }

  public BigDecimal massDiff() {
    return diff;
  }

  public HashSet<BigDecimal> checkDiff() {
    HashSet<BigDecimal> cds = new HashSet(1);
    cds.add(BigDecimal.ZERO);
    if (terminusModificationPointers[0] != null && terminusModificationPointers[0].getModification() != EMPTY_MODIFICATION) {
      HashSet<BigDecimal> tmp = new HashSet(cds.size());
      for (BigDecimal cd : cds) {
        tmp.add(cd.add(terminusModificationPointers[0].getModification().getCheckDiff()));
      }
      cds.addAll(tmp);
    }
    if (terminusModificationPointers[1] != null && terminusModificationPointers[1].getModification() != EMPTY_MODIFICATION) {
      HashSet<BigDecimal> tmp = new HashSet(cds.size());
      for (BigDecimal cd : cds) {
        tmp.add(cd.add(terminusModificationPointers[1].getModification().getCheckDiff()));
      }
      cds.addAll(tmp);
    }
    for (Integer pos : modificationPointers.subMap(begin, !bounds[0].getProtease().isLockedRight(), end, !bounds[1].getProtease().isLockedLeft()).keySet()) {
      if (modificationPointers.containsKey(pos) && modificationPointers.get(pos).containsKey(charAt(pos))) {
        HashSet<BigDecimal> tmp = new HashSet(cds.size());
        for (BigDecimal cd : cds) {
          tmp.add(cd.add(modificationPointers.get(pos).get(charAt(pos)).getModification().getCheckDiff()));
        }
        cds.addAll(tmp);
      }
    }
    for (Integer pos : bondsPointers.subMap(begin, bounds[0].getProtease().isFlexibleRight(), end, bounds[1].getProtease().isFlexibleLeft()).keySet()) {
      HashSet<BigDecimal> tmp = new HashSet(cds.size());
      for (BigDecimal cd : cds) {
        tmp.add(cd.add(bondsPointers.get(pos).getCheckDiff()));
      }
      cds.addAll(tmp);
    }
    return cds;
  }

  public BigDecimal massAt(int index) {
    if (bondsSelected.containsKey(index) && (index != begin || bounds[0].getProtease().isFlexibleRight()) && (index != end || bounds[1].getProtease().isFlexibleLeft())) {
      int j = bondsSelected.get(index);
      if (bondsPointers.containsKey(index)) {
        return Monomer.mass(charAt(index)).add(bondsPointers.get(index).getBond().divide(new BigDecimal(2)));
      } else {
        return Monomer.mass(charAt(index)).add(bondsPointers.get(j).getBond().divide(new BigDecimal(2)));
      }
    }
    // Předpoklad, že v případě flexibleLeft/Right, byly příslušné okraje správně nastaveny, aby se nic nepřičítalo
    if (modificationPointers.containsKey(index) && modificationPointers.get(index).containsKey(charAt(index)) && !modificationSkip.containsKey(index) &&
        modificationPointers.get(index).get(charAt(index)).getModification() != EMPTY_MODIFICATION) {
      return Monomer.mass(charAt(index)).add(modificationPointers.get(index).get(charAt(index)).getModification().getModification());
    }
    return Monomer.mass(charAt(index));
  }

  public BigDecimal minMassAt(int index) {
    return mins[index];
  }

  public BigDecimal maxMassAt(int index) {
    return maxs[index];
  }

  public BigDecimal massN(int position) {
    if (terminusModificationPointers[0] != null && terminusModificationPointers[0].getModification() != EMPTY_MODIFICATION) {
      return massNdefault().add(terminusModificationPointers[0].getModification().getModification());
    }
    if (position == 0 && bondsSelected.containsKey(-1)) {
      return massNdefault().add(bondsPointers.get(-1).getBond().divide(new BigDecimal(2)));
    }
    return massNdefault();
  }

  public BigDecimal massNdefault() {
    return ChemicalElement.mass("H");
  }

  public BigDecimal minMassN() {
    return minMassN(false);
  }

  public BigDecimal minMassN(boolean locked) {
    return locked ? minTerminus[0].min(BigDecimal.ZERO) : minTerminus[0];
  }

  public BigDecimal maxMassN() {
    return maxMassN(false);
  }

  public BigDecimal maxMassN(boolean locked) {
    return locked ? maxTerminus[0].min(BigDecimal.ZERO) : maxTerminus[0];
  }

  public BigDecimal massC(int position) {
    if (terminusModificationPointers[1] != null && terminusModificationPointers[1].getModification() != EMPTY_MODIFICATION) {
      return massCdefault().add(terminusModificationPointers[1].getModification().getModification());
    }
    if (position == length()-1 && bondsSelected.containsKey(length())) {
      return massCdefault().add(bondsPointers.get(bondsSelected.get(length())).getBond().divide(new BigDecimal(2)));
    }
    return massCdefault();
  }

  public BigDecimal massCdefault() {
    return ChemicalElement.mass("O").add(ChemicalElement.mass("H"));
  }

  public BigDecimal minMassC() {
    return minMassC(false);
  }

  public BigDecimal minMassC(boolean locked) {
    return locked ? minTerminus[1].min(BigDecimal.ZERO) : minTerminus[1];
  }

  public BigDecimal maxMassC() {
    return maxMassC(false);
  }

  public BigDecimal maxMassC(boolean locked) {
    return locked ? maxTerminus[1].min(BigDecimal.ZERO) : maxTerminus[1];
  }
  //</editor-fold>

  public TreeSet<Character> charsAt(int index) {
    TreeSet<Character> ret = new TreeSet<>();
    ret.add(protein.charAt(index));
    if (mutations.containsKey(index)) {
      ret.addAll(mutations.get(index));
    }
    return ret;
  }

  public HashSet<Character> domain() {
    HashSet<Character> ret = new HashSet<>(Monomer.getNames().size());
    for (char c : protein.toCharArray()) {
      ret.add(c);
    }
    for (ArrayList<Character> arrayList : mutations.values()) {
      ret.addAll(arrayList);
    }
    return ret;
  }

  public TreeSet<Integer> positions(char c) {
    TreeSet<Integer> ret = new TreeSet<>();
    if (c == '^') {
      ret.add(-1);
      return ret;
    }
    if (c == '$') {
      ret.add(length());
      return ret;
    }
    for (int i = 0; i < protein.length(); i++) {
      if (protein.charAt(i) == c) {
        ret.add(i);
      }
    }
    for (Integer integer : mutations.keySet()) {
      for (Character character : mutations.get(integer)) {
        if (character == c) {
          ret.add(integer);
          break;
        }
      }
    }
    return ret;
  }

  @Override
  public CharSequence subSequence(int begin, int end) {
    if (!checkBound(begin)) {
      throw new StringIndexOutOfBoundsException(begin);
    }
    if (!checkBound(end)) {
      throw new StringIndexOutOfBoundsException(end);
    }
    throw new NotImplementedException();
    //return new Peptide(this, begin, end, fixed, bounds[0], bounds[1], dopočítat min/maxMasses);
  }

  @Override
  public String substringConfiguration(int begin, int end) {
    NavigableMap<Integer, Integer> subMap = mutationPointers.subMap(begin, true, end, false);
    if (subMap.isEmpty()) {
      return protein.substring(begin, end);
    }
    StringBuilder buffer = new StringBuilder(end-begin);
    int prev = begin;
    for (Integer integer : subMap.keySet()) {
      buffer.append(protein.substring(prev, integer));
      if (modificationSkip.containsKey(integer)) {
        buffer.append(modificationSkip.get(integer));
      } else if (mutationSkip.containsKey(integer)) {
        buffer.append(mutationSkip.get(integer));
      } else if (mutationPointers.get(integer) == 0) {
        buffer.append(protein.charAt(integer));
      } else {
        buffer.append(mutations.get(integer).get(mutationPointers.get(integer) - 1));
      }
      prev = integer+1;
    }
    if (prev < end) {
      buffer.append(protein.substring(prev, end));
    }
    return buffer.toString();
  }

  public String getName() {
    return name;
  }

  public int getShift() {
    return start() - prefix.replaceAll("/.", "").length();
  }

  public String getProtein() {
    return prefix+protein+suffix;
  }
  @Override
  public String toString() {
    if (prefix.isEmpty() && mutationPointers.isEmpty() && suffix.isEmpty()) {
      return protein;
    }
    
    StringBuilder buffer = new StringBuilder(prefix.length() + 1 + protein.length() + (mutationPointers.size()) * 2 + suffix.length() + 1);
    if (!prefix.isEmpty()) {
      buffer.append(prefix).append("^");
    }
    int prev = 0;
    for (Integer integer : mutationPointers.keySet()) {
      buffer.append(protein.substring(prev, integer + 1));
      for (char aa : mutations.get(integer)) {
        buffer.append(MUTATION_SPLITTER).append(aa);
      }
      prev = integer+1;
    }
    buffer.append(protein.substring(prev));
    if (!suffix.isEmpty()) {
      buffer.append("$").append(suffix);
    }
    return buffer.toString();
  }
  @Override
  public String toStringConfiguration() {
    return substringConfiguration(0, protein.length());
  }
  @Override
  public String toStringModifications(String prefix) {
    StringBuilder buf = new StringBuilder();
    if (terminusModificationPointers[0] != null && terminusModificationPointers[0].getModification() != EMPTY_MODIFICATION) {
      buf.append(terminusModificationPointers[0].getModification().getName()).append(" (").append(prefix).append("N-terminus), ");
    }
    for (Integer pos : modificationPointers.subMap(begin, !bounds[0].getProtease().isLockedRight(), end, !bounds[1].getProtease().isLockedLeft()).keySet()) {
      if (modificationPointers.containsKey(pos) && modificationPointers.get(pos).containsKey(charAt(pos)) &&
          modificationPointers.get(pos).get(charAt(pos)).getModification() != EMPTY_MODIFICATION) {
        buf.append(modificationPointers.get(pos).get(charAt(pos)).getModification().getName()).append(" (").append(prefix).append(pos+start).append("), ");
      }
    }
    if (terminusModificationPointers[1] != null && terminusModificationPointers[1].getModification() != EMPTY_MODIFICATION) {
      buf.append(terminusModificationPointers[1].getModification().getName()).append(" (").append(prefix).append("C-terminus), ");
    }
    if (buf.length() == 0) {
      return "";
    }
    return buf.substring(0, buf.length()-2);
  }
  @Override
  public String toStringBonds(String prefix) {
    StringBuilder buf = new StringBuilder();
    for (Integer pos : bondsPointers.subMap(begin, bounds[0].getProtease().isFlexibleRight(), end, bounds[1].getProtease().isFlexibleLeft()).keySet()) {
      buf.append(bondsPointers.get(pos).getName()).append(" (").append(prefix).append(pos == -1 ? "N-terminus" : pos+start).append("; ")
                                                               .append(prefix).append(bondsSelected.get(pos) == length() ? "C-terminus" : bondsSelected.get(pos)+start).append("), ");
    }
    if (buf.length() == 0) {
      return "";
    }
    return buf.substring(0, buf.length()-2);
  }

  private class Pointer implements Cloneable {
    private int level;
    private ModificationReal modification;

    public Pointer(int level, ModificationReal modification) {
      this.level = level;
      this.modification = modification;
    }

    public Pointer clone() {
      return new Pointer(level, modification);
    }

    public int getLevel() {
      return level;
    }

    public ModificationReal getModification() {
      return modification;
    }

    public void setLevel(int level) {
      this.level = level;
    }

    public void setModification(ModificationReal modification) {
      this.modification = modification;
    }
  }
}
