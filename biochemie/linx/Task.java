package biochemie.linx;

import biochemie.Protease;
import java.util.*;
import javax.swing.SwingWorker;

/**
 *
 * @author Janek
 */
public class Task extends SwingWorker<Void, Void> {
  LinX parent;
  ResultsCard resultsCard;
  ArrayList<Collection<Protease>> proteases;
  int MCLimit;

  public Task(LinX parent, ResultsCard resultsCard) {
    this.parent = parent;
    this.resultsCard = resultsCard;
    this.proteases = null;
  }

  public Task(LinX parent, ResultsCard resultsCard, ArrayList<Collection<Protease>> proteases, int MCLimit) {
    this(parent, resultsCard);
    this.proteases = proteases;
    this.MCLimit = MCLimit;
  }

  protected Void doInBackground() throws Exception {
    if (proteases == null) {
      resultsCard.nonspecificDigest();
    } else {
      resultsCard.specificDigest(proteases, MCLimit);
    }
    parent.showResults();
    System.out.println("Finished");
    return null;
  }

  public void done() { }
}
