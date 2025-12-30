package org.openstreetmap.josm.plugins.movablesidebuttons;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.HashMap;
import java.util.Objects;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.widgets.MultiSplitPane;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

public class MoveableSideButtonsPlugin extends Plugin {
	private static MoveableSideButtonsPlugin instance;
	private final HashMap<ToggleDialog, MoveableButtonPanel> moveableMouseButtonsMap = new HashMap<>();
	private final ContainerAdapter listener;

	public MoveableSideButtonsPlugin(PluginInformation info) {
		super(info);
		
		listener = new ContainerAdapter() {
			@Override
			public void componentAdded(ContainerEvent e) {
				if(e.getChild() instanceof ToggleDialog) {
					addToggleDialog((ToggleDialog) e.getChild());
				}
			}
		};

		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
			@Override
			public void eventDispatched(AWTEvent event) {
				if(Objects.equals("org.openstreetmap.josm.gui.dialogs.ToggleDialog.DetachedDialog",
						event.getSource().getClass().getCanonicalName())) {
					Container c = ((JDialog) event.getSource()).getContentPane();

					if(c.getComponentCount() == 1) {
						ToggleDialog d = (ToggleDialog) c.getComponent(0);
						MoveableButtonPanel p = moveableMouseButtonsMap.get(d);
						if(p == null && d.getComponentCount() == 3) {
							moveableMouseButtonsMap.put(d, new MoveableButtonPanel(d));
							SwingUtilities.invokeLater(() -> d.revalidate());
						}
						else if(p != null) {
						  p.apply(d);
						}
					}
				}
			}
		}, AWTEvent.WINDOW_EVENT_MASK);
	}

	public static MoveableSideButtonsPlugin getInstance() {
		return instance;
	}

	private void addToggleDialog(ToggleDialog d) {
		if(moveableMouseButtonsMap.get(d) == null && d.getComponentCount() == 3) {
			moveableMouseButtonsMap.put(d, new MoveableButtonPanel(d));
		}
	}
	
	@Override
	public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
		if(newFrame != null) {
			for(int k = 0; k < newFrame.getComponentCount(); k++) {
				if(newFrame.getComponent(k) instanceof JSplitPane) {
					JSplitPane sp = (JSplitPane) newFrame.getComponent(k);
					for(int i = 0; i < sp.getComponentCount(); i++) {
						if(sp.getComponent(i) instanceof DialogsPanel) {
							DialogsPanel p = (DialogsPanel) sp.getComponent(i);

							for(int j = 0; j < p.getComponentCount(); j++) {
								if(p.getComponent(j) instanceof MultiSplitPane) {
									MultiSplitPane mp = (MultiSplitPane) p.getComponent(j);
									mp.addContainerListener(new ContainerAdapter() {
										@Override
										public void componentAdded(ContainerEvent e) {
											if(Objects.equals("org.openstreetmap.josm.gui.dialogs.DialogsPanel.MinSizePanel",
													e.getChild().getClass().getCanonicalName())) {
												((JPanel) e.getChild()).addContainerListener(listener);
											}
										}
									});

									for(int i1 = 0; i1 < mp.getComponentCount(); i1++) {
										if(((JPanel) mp.getComponent(i1)).getComponentCount() > 0) {
											JPanel p2 = (JPanel) mp.getComponent(i1);
											p2.addContainerListener(listener);
											
											for(int i2 = 0; i2 < p2.getComponentCount(); i2++) {
												if(p2.getComponent(i2) instanceof ToggleDialog) {
													addToggleDialog((ToggleDialog) p2.getComponent(i2));
												}
											}
										}
									}
								}
							}
						}
					}
				}

				Window[] wins = JFrame.getWindows();

				for(Window w : wins) {
					if(Objects.equals("org.openstreetmap.josm.gui.dialogs.ToggleDialog.DetachedDialog",
							w.getClass().getCanonicalName())) {
						Container c = ((JDialog) w).getContentPane();

						if(c.getComponentCount() == 1) {
							addToggleDialog((ToggleDialog) c.getComponent(0));
						}
					}
				}
			}
		}

		super.mapFrameInitialized(oldFrame, newFrame);
	}
}
