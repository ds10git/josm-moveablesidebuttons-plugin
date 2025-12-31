package org.openstreetmap.josm.plugins.movablesidebuttons;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.spi.preferences.Config;

public class MoveableButtonPanel extends JPanel {
	private String id;
	private JPanel buttonsParent;
	private List<JPanel> buttonRows;
	private HashMap<JPanel, List<String>> buttonText;
	private ButtonsLayout layout;
	private ComponentAdapter adapter;
	private ToggleDialog d;
	
	public MoveableButtonPanel(ToggleDialog d) {
		super(new BorderLayout());
		id = getClass().getSimpleName() + "." + d.getClass().getCanonicalName();
		this.d = d;
		
		if(d.isDialogInCollapsedView()) {
			Component c = d.getComponent(1);
			adapter = new ComponentAdapter() {
				@Override
				public synchronized void componentResized(ComponentEvent e) {
					if(!d.isDialogInCollapsedView() && d.isDialogInDefaultView()) {
						SwingUtilities.invokeLater(() -> {
							apply(d);
							SwingUtilities.invokeLater(() -> revalidate());
						});

						c.removeComponentListener(this);
						adapter = null;
					}
				}
			};
			
			c.addComponentListener(adapter);
		} else {
			apply(d);
		}
	}
	
	public synchronized void reset() {
		if(adapter != null) {
			d.getComponent(1).removeComponentListener(adapter);
			adapter = null;
		}
		else {
			Component data = getComponent(0);
			JPanel buttonsParent = (JPanel)getComponent(1);
			
			remove(1);
			remove(0);
			
			for(JPanel buttonRow : buttonRows) {
				List<String> textList = buttonText.get(buttonRow);
				List<SideButton> newList = new LinkedList<>();
				
				for(int i = buttonRow.getComponentCount() - 1; i >= 0; i--) {
					SideButton button = ((ExtendedSideButton)buttonRow.getComponent(i)).b;
					button.setText(textList.get(i));
					
					newList.add(0,button);
					buttonRow.remove(i);
				}
				
				buttonRow.setLayout(Config.getPref().getBoolean("dialog.align.left", false) ? new FlowLayout(FlowLayout.LEFT) : new GridLayout(1, newList.size()));
				
				for(SideButton b : newList) {
					buttonRow.add(b);
				}
			}
			
  		MouseListener[] listeners = d.getComponent(0).getMouseListeners();
  		
  		for(int i = 0; i < listeners.length; i++) {
  			if(listeners[i] instanceof PopupMenuLauncher) {
  				JPopupMenu popup = ((PopupMenuLauncher) listeners[i]).getMenu();
  				if(popup.getComponentCount() > 0 && popup.getComponent(0) instanceof JMenu) {
  					JMenu menu = (JMenu) popup.getComponent(0);
  					menu.remove(menu.getMenuComponentCount()-1);
  					menu.remove(menu.getMenuComponentCount()-1);
  					menu.remove(menu.getMenuComponentCount()-1);
  				}
  			}
  		}
			
			d.remove(1);
			d.add(data, BorderLayout.CENTER);
			d.add(buttonsParent, BorderLayout.SOUTH);
			d.doLayout();
			buttonsParent = null;
			buttonRows.clear();
			buttonRows = null;
			buttonText.clear();
			buttonText = null;
			layout.destroy();
			layout = null;
		}
		
		id = null;
		d = null;
	}
	
	public synchronized void apply(ToggleDialog d) {
	  if(buttonsParent == null) {
  		String pos = Config.getPref().get(id + ".position", BorderLayout.EAST);
  		MouseListener[] listeners = d.getComponent(0).getMouseListeners();
  		JCheckBoxMenuItem showTextMenuItem = null;
  
  		for(int i = 0; i < listeners.length; i++) {
  			if(listeners[i] instanceof PopupMenuLauncher) {
  				JPopupMenu popup = ((PopupMenuLauncher) listeners[i]).getMenu();
  				if(popup.getComponentCount() > 0 && popup.getComponent(0) instanceof JMenu) {
  					showTextMenuItem = new JCheckBoxMenuItem(tr("Show text"),
  							Config.getPref().getBoolean(id + ".showText", false));
  					showTextMenuItem.setEnabled(false);
  					final JCheckBoxMenuItem iten = showTextMenuItem;
  					showTextMenuItem.addActionListener(e -> {
  						showText(iten.isSelected());
  					});
  
  					JRadioButtonMenuItem left = new JRadioButtonMenuItem(tr("Left"), Objects.equals(BorderLayout.WEST, pos));
  					left.addActionListener(e -> setPosition(BorderLayout.WEST));
  					JRadioButtonMenuItem bottom = new JRadioButtonMenuItem(tr("Bottom"), Objects.equals(BorderLayout.SOUTH, pos));
  					bottom.addActionListener(e -> setPosition(BorderLayout.SOUTH));
  					JRadioButtonMenuItem right = new JRadioButtonMenuItem(tr("Right"), Objects.equals(BorderLayout.EAST, pos));
  					right.addActionListener(e -> setPosition(BorderLayout.EAST));
  
  					ButtonGroup bg = new ButtonGroup();
  					bg.add(left);
  					bg.add(bottom);
  					bg.add(right);
  
  					JMenu config = new JMenu(tr("Position"));
  					config.add(left);
  					config.add(bottom);
  					config.add(right);
  
  					JMenu menu = (JMenu) popup.getComponent(0);
  					menu.addSeparator();
  					menu.add(config);
  					menu.add(showTextMenuItem);
  				}
  			}
  		}
  
  		Component data = d.getComponent(1);
  
  		if(data instanceof JScrollPane) {
  			Border borderOriginal = ((JScrollPane) data).getBorder();
  
  			((JScrollPane) data).setBorder(new MatteBorder(1, 0, 0, 0, Color.gray) {
  				public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
  					borderOriginal.paintBorder(c, g, x, y, width, height);
  				}
  			});
  		}
  
  		buttonsParent = (JPanel) d.getComponent(2);
  		buttonRows = new LinkedList<>();
  
  		for(int i = 0; i < buttonsParent.getComponentCount(); i++) {
  			buttonRows.add((JPanel) buttonsParent.getComponent(i));
  		}
  
  		buttonText = new HashMap<>();
  
  		boolean showText = Config.getPref().getBoolean(id + ".showText", false);
  
  		for(JPanel buttonRow : buttonRows) {
  			List<String> textList = new LinkedList<>();
  			buttonText.put(buttonRow, textList);
  
  			List<ExtendedSideButton> newList = new LinkedList<>();
  
  			for(int i = buttonRow.getComponentCount() - 1; i >= 0; i--) {
  				ExtendedSideButton button = new ExtendedSideButton((SideButton) buttonRow.getComponent(i));
  
  				newList.add(0, button);
  
  				if(button.getText() != null && !button.getText().isBlank() && !showTextMenuItem.isEnabled()) {
  					showTextMenuItem.setEnabled(true);
  				}
  
  				textList.add(0, button.getText());
  
  				if(!showText) {
  					button.setText("");
  				}
  			}
  
  			for(ExtendedSideButton b : newList) {
  				buttonRow.add(b);
  			}
  		}
  
  		d.remove(2);
  		d.remove(1);
  
  		add(data, BorderLayout.CENTER);
  		add(buttonsParent, pos);
  
  		d.add(this, BorderLayout.CENTER);
  
  		layout = new ButtonsLayout(d, Objects.equals(BorderLayout.SOUTH, pos) ? ButtonsLayout.Orientation.HORIZONTAL
  				: ButtonsLayout.Orientation.VERTICAL);
  
  		for(JPanel buttonRow : buttonRows) {
  			buttonRow.setLayout(layout);
  		}
	  }
	  
	  revalidate();
	}

	private void showText(boolean show) {
		Config.getPref().putBoolean(id + ".showText", show);

		for(JPanel buttonRow : buttonRows) {
			List<String> textList = buttonText.get(buttonRow);

			for(int i = 0; i < buttonRow.getComponentCount(); i++) {
				if(!show) {
					((ExtendedSideButton) buttonRow.getComponent(i)).setText("");
				} else {
					((ExtendedSideButton) buttonRow.getComponent(i)).setText(textList.get(i));
				}
			}
		}

		SwingUtilities.invokeLater(() -> revalidate());

	}

	private void setPosition(String position) {
		Config.getPref().put(id + ".position", position);

		if(Objects.equals(BorderLayout.SOUTH, position)) {
			layout.setOrientation(ButtonsLayout.Orientation.HORIZONTAL);
		} else {
			layout.setOrientation(ButtonsLayout.Orientation.VERTICAL);
		}

		remove(buttonsParent);
		add(buttonsParent, position);
		revalidate();

		SwingUtilities.invokeLater(() -> revalidate());
	}

	private static final class ButtonsLayout implements LayoutManager {
		public static enum Orientation {
			HORIZONTAL, VERTICAL
		}

		private int width = 0;
		private int height = 0;
		private Orientation o;
		private ToggleDialog d;
		
		private void destroy() {
			o = null;
			d = null;
		}

		public ButtonsLayout(ToggleDialog d, Orientation o) {
			this.o = o;
			this.d = d;
		}

		public void setOrientation(Orientation o) {
			this.o = o;
		}

		@Override
		public void addLayoutComponent(String name, Component comp) {

		}

		@Override
		public void removeLayoutComponent(Component comp) {

		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			if(width == 0 && height == 0) {
				layoutContainer(parent);
			}
			return new Dimension(width, height);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return new Dimension(0, 0);
		}

		@Override
		public void layoutContainer(Container parent) {
			boolean repaint = false;

			if(d.isDialogInCollapsedView()) {
			  width = d.getParent().getWidth();
			  height = 0;
			  
	      d.setPreferredSize(new Dimension(width, 20));
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        d.setMinimumSize(new Dimension(Integer.MAX_VALUE, 20));
	      
        d.getParent().revalidate();
        
	      return;
			}
			
			if(Orientation.VERTICAL == o) {
				int height = parent.getHeight();

				int xEnd = 0;
				int maxWidth = 0;

				ArrayList<Component> column = new ArrayList<>();

				Component first = parent.getComponentCount() > 0 ? parent.getComponent(0) : null;

				if(first != null) {
					if(first.getPreferredSize() != null) {
						height -= first.getPreferredSize().height;
						maxWidth = first.getPreferredSize().width;
					}

					column.add(first);
				}

				for(int i = 1; i < parent.getComponentCount(); i++) {
					Component c = parent.getComponent(i);
					height -= c.getPreferredSize().height;

					if(height < 0) {
						int cHeight = parent.getHeight() / column.size();

						for(int j = 0; j < column.size(); j++) {
							column.get(j).setLocation(xEnd, j * cHeight);
							column.get(j).setSize(maxWidth, cHeight);
						}

						height = parent.getHeight() - c.getPreferredSize().height;
						xEnd += maxWidth;
						maxWidth = 0;

						column.clear();
					}

					maxWidth = Math.max(c.getPreferredSize().width, maxWidth);
					column.add(c);
				}

				if(!column.isEmpty()) {
					int cHeight = parent.getHeight() / column.size();
					for(int j = 0; j < column.size(); j++) {
						column.get(j).setLocation(xEnd, j * cHeight);
						column.get(j).setSize(maxWidth, cHeight);
					}

					xEnd += maxWidth;
				}

				repaint = this.width != xEnd;
				this.width = xEnd;
			} else {
				int width = Config.getPref().getBoolean("dialog.align.left", false) ? -1
						: parent.getParent().getWidth() / parent.getComponentCount();
				int height = 0;
				int xPos = 0;

				for(int i = 0; i < parent.getComponentCount(); i++) {
					height = Math.max(height, parent.getComponent(i).getPreferredSize().height);
				}

				for(int i = 0; i < parent.getComponentCount(); i++) {
					parent.getComponent(i).setVisible(true);
					parent.getComponent(i).setLocation(xPos, 0);

					if(width > 0) {
						xPos += width;
						parent.getComponent(i).setSize(width, height);
					} else {
						xPos += parent.getComponent(i).getPreferredSize().width;
						parent.getComponent(i).setSize(parent.getComponent(i).getPreferredSize().width, height);
					}
				}

				repaint = this.width != parent.getParent().getWidth();
				this.width = parent.getParent().getWidth();
				parent.setSize(this.width, height);
			}

			repaint |= this.height != parent.getHeight();

			this.height = parent.getHeight();

			parent.setSize(new Dimension(this.width, this.height));
			parent.getParent().setSize(new Dimension(this.width, this.height));

			if(repaint) {
				Container c = parent.getParent();

				if(c != null) {
					while(c.getParent() != null && c.getParent() instanceof JPanel) {
						c = c.getParent();
					}

					parent.getParent().doLayout();
				}
			}
		}
	}

	private static final class ExtendedSideButton extends JPanel {
		private BasicArrowButton button;
		SideButton b;

		public ExtendedSideButton(SideButton b) {
			this.b = b;
			setLayout(new BorderLayout());
			add(b, BorderLayout.CENTER);

			int width = 6;

			for(int i = 0; i < b.getComponentCount(); i++) {
				if(b.getComponent(i) instanceof BasicArrowButton) {
					button = (BasicArrowButton) b.getComponent(i);
					break;
				}
			}

			if(b.getIcon() instanceof ImageIcon) {
				ImageIcon icon = (ImageIcon) b.getIcon();

				BufferedImage bi = new BufferedImage(icon.getIconWidth() + width, icon.getIconHeight(),
						BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = bi.createGraphics();

				g.drawImage(icon.getImage(), 3, 0, null);
				g.dispose();
				b.setIcon(new ImageIcon(bi));
			}
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension size = super.getPreferredSize();

			if(getParent().getLayout() instanceof ButtonsLayout) {
				if(((ButtonsLayout) getParent().getLayout()).o == ButtonsLayout.Orientation.VERTICAL) {
					b.setHorizontalAlignment(button == null ? SwingConstants.CENTER : SwingConstants.LEADING);
				} else {
					b.setHorizontalAlignment(SwingConstants.CENTER);
					size.height += 4;
				}
			}

			if(button != null) {
				size.width += button.getPreferredSize().width;

				if(b.getText() != null && !b.getText().isBlank()) {
					size.width += (int) getFont()
							.getStringBounds(b.getText(), new FontRenderContext(getFont().getTransform(), true, true)).getWidth();
				}

				size.width += 12;
			}

			return size;
		}

		public String getText() {
			return b.getText();
		}

		public void setText(String text) {
			b.setText(text);
		}
	}
}
