package biochemie.linx;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class CheckBoxBorder extends AbstractBorder {
  static private final int SPACE = 5;

  private Component parent;
  private Border border;
  private JCheckBox jCheckBox;

  public CheckBoxBorder(Component component, Border border, String title) {
    this.parent = component;
    this.border = border == null ? UIManager.getBorder("TitledBorder.border") : border;
    this.jCheckBox = new JCheckBox(title == null ? "" : title);
    this.jCheckBox.setFont(UIManager.getFont("TitledBorder.font"));
    this.jCheckBox.setForeground(UIManager.getColor("TitledBorder.titleColor"));
    this.jCheckBox.setOpaque(false);
    this.jCheckBox.setSelected(true);
    this.jCheckBox.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        parent.repaint();
      }
    });
    component.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        propagate(e);
      }

      public void mousePressed(MouseEvent e) {
        propagate(e);
      }

      public void mouseReleased(MouseEvent e) {
        propagate(e);
      }

      public void mouseEntered(MouseEvent e) {
        propagate(e);
      }

      public void mouseExited(MouseEvent e) {
        propagate(e);
      }

      private void propagate(MouseEvent e) {
        Dimension size = jCheckBox.getPreferredSize();
        jCheckBox.setBounds(new Rectangle(SPACE, 0, size.width, size.height));
        jCheckBox.dispatchEvent(new MouseEvent(jCheckBox, e.getID(), e.getWhen(), e.getModifiers(), e.getPoint().x-SPACE, e.getPoint().y, e.getClickCount(), e.isPopupTrigger(), e.getButton()));
      }
    });
    component.addMouseMotionListener(new MouseMotionListener() {
      private boolean in = false;

      public void mouseDragged(MouseEvent e) {
        Dimension size = jCheckBox.getPreferredSize();
        Rectangle rect = new Rectangle(SPACE, 0, size.width, size.height);
        if (rect.contains(e.getPoint())) {
          if (!in) {
            jCheckBox.dispatchEvent(new MouseEvent(jCheckBox, MouseEvent.MOUSE_ENTERED, e.getWhen(), e.getModifiers(), e.getPoint().x-SPACE, e.getPoint().y, e.getClickCount(), e.isPopupTrigger(), e.getButton()));
            in = true;
          }
        } else {
          if (in) {
            jCheckBox.dispatchEvent(new MouseEvent(jCheckBox, MouseEvent.MOUSE_EXITED, e.getWhen(), e.getModifiers(), e.getPoint().x-SPACE, e.getPoint().y, e.getClickCount(), e.isPopupTrigger(), e.getButton()));
            in = false;
          }
        }
      }

      public void mouseMoved(MouseEvent e) {
        Dimension size = jCheckBox.getPreferredSize();
        Rectangle rect = new Rectangle(SPACE, 0, size.width, size.height);
        if (rect.contains(e.getPoint())) {
          in = true;
        } else {
          in = false;
        }
      }
    });
    component.setFocusable(true);
  }

  public String getTitle() {
    return jCheckBox.getText();
  }

  public boolean isSelected() {
    return jCheckBox.isSelected();
  }

  public void addItemListener(ItemListener il) {
    jCheckBox.addItemListener(il);
  }

  public void doClick() {
    jCheckBox.doClick();
  }

  public int getBaseline(Component c, int width, int height) {
    if (c == null) { throw new NullPointerException("Null component isn't allowed."); }
    if (width < 0) { throw new IllegalArgumentException("Width cannot be negative."); }
    if (height < 0) { throw new IllegalArgumentException("Height cannot be negative."); }

    Dimension size = jCheckBox.getPreferredSize();
    int baseline = jCheckBox.getBaseline(size.width, size.height);
    int top = (border.getBorderInsets(c).top - size.height) / 2;
    return baseline + ((top < 0) ? 0 : top);
  }

  public Insets getBorderInsets(Component c, Insets insets) {
    insets = defaultBorderInsets(c, insets);
    Dimension size = jCheckBox.getPreferredSize();
    if (insets.top < size.height) {
      insets.top = size.height;
    }
    return insets;
  }

  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    jCheckBox.setComponentOrientation(c.getComponentOrientation());
    Dimension size = jCheckBox.getPreferredSize();
    Insets insets = defaultBorderInsets(c, new Insets(0, 0, 0, 0));
    insets.left += SPACE;
    insets.right += SPACE;
    insets.top = (insets.top - size.height)/2;

    if (insets.top < 0) {
      y -= insets.top;
      height += insets.top;
    }

    int tx = x + insets.left;
    int ty = y + insets.top;
    int th = size.height;
    int tw = width - insets.left - insets.right;
    if (tw > size.width) {
      tw = size.width;
    }

    Graphics g2 = g.create();
    if (g2 instanceof Graphics2D) {
      Graphics2D g2d = (Graphics2D) g2;
      Path2D path = new Path2D.Float();
      path.append(new Rectangle(x, y, width, insets.top), false);
      path.append(new Rectangle(x, ty, insets.left, th), false);
      path.append(new Rectangle(tx + tw, ty, width - tw - insets.left, th), false);
      path.append(new Rectangle(x, ty + th, width, height - th - insets.top), false);
      g2d.clip(path);
    }
    border.paintBorder(c, g2, x, y, width, height);
    g2.dispose();
    g.translate(tx, ty);
    jCheckBox.setSize(tw, th);
    jCheckBox.paint(g);
    g.translate(-tx, -ty);
  }

  private Insets defaultBorderInsets(Component c, Insets insets) {
    if (border instanceof AbstractBorder) {
      AbstractBorder ab = (AbstractBorder) border;
      insets = ab.getBorderInsets(c, insets);
    } else {
      Insets i = border.getBorderInsets(c);
      insets.set(i.top, i.left, i.bottom, i.right);
    }
    return insets;
  }
}
