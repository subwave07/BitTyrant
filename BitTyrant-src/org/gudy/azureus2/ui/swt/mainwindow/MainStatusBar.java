/*
 * Created on Mar 20, 2006 6:40:14 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.text.NumberFormat;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.BlockedIpsWindow;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.update.UpdateProgressWindow;
import org.gudy.azureus2.ui.swt.update.UpdateWindow;
import org.gudy.azureus2.ui.swt.views.ConfigView;
import org.gudy.azureus2.ui.swt.views.configsections.ConfigSectionConnection;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.update.*;

/**
 * Moved from MainWindow and GUIUpdater
 */
public class MainStatusBar {
	/**
	 * Warning status icon identifier
	 */
	private static final String STATUS_ICON_WARN = "sb_warning";

	private AEMonitor this_mon = new AEMonitor("MainStatusBar");

	private ArrayList update_stack = new ArrayList();

	private UpdateWindow updateWindow;

	private Composite statusBar;

	private Composite statusArea;

	private StackLayout layoutStatusArea;

	private CLabel statusText;

	private String statusTextKey = "";

	private String statusImageKey = null;

	private Composite statusUpdate;

	private Label statusUpdateLabel;

	private ProgressBar statusUpdateProgressBar;

	private CLabel ipBlocked;

	private CLabel srStatus;

	private CLabel natStatus;

	private CLabel dhtStatus;

	private CLabel statusDown;

	private CLabel statusUp;

	private Display display;

	// For Refresh..
	private long last_sr_ratio = -1;

	private int last_sr_status = -1;

	private int lastNATstatus = -1;

	private int lastDHTstatus = -1;

	private long lastDHTcount = -1;

	private NumberFormat numberFormat;

	private OverallStats overall_stats;

	private ConnectionManager connection_manager;

	private DHTPlugin dhtPlugin;

	private GlobalManager globalManager;

	private AzureusCore azureusCore;

	private UIFunctions uiFunctions;

	/**
	 * 
	 */
	public MainStatusBar() {
		numberFormat = NumberFormat.getInstance();
		overall_stats = StatsFactory.getStats();
		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		connection_manager = pm.getDefaultPluginInterface().getConnectionManager();
		PluginInterface dht_pi = pm.getPluginInterfaceByClass(DHTPlugin.class);
		if (dht_pi != null) {
			dhtPlugin = (DHTPlugin) dht_pi.getPlugin();
		}
	}

	/**
	 * 
	 * @return composite holiding the statusbar
	 */
	public Composite initStatusBar(final AzureusCore core, final GlobalManager globalManager,
			Display display, final Composite parent)
	{
		this.display = display;
		this.globalManager = globalManager;
		this.azureusCore = core;
		this.uiFunctions = UIFunctionsManager.getUIFunctions();

		FormData formData;

		//final int borderFlag = (Constants.isOSX) ? SWT.SHADOW_NONE : SWT.SHADOW_IN;
		final int borderFlag = SWT.SHADOW_NONE;

		statusBar = new Composite(parent, SWT.NONE);

		GridLayout layout_status = new GridLayout();
		layout_status.numColumns = 7;
		layout_status.horizontalSpacing = 0;
		layout_status.verticalSpacing = 0;
		layout_status.marginHeight = 0;
		if (Constants.isOSX) {
			// OSX has a resize widget on the bottom right.  It's about 15px wide.
			try {
				layout_status.marginRight = 15;
			} catch (NoSuchFieldError e) {
				// Pre SWT 3.1 
				layout_status.marginWidth = 15;
			}
		} else {
			layout_status.marginWidth = 0;
		}
		statusBar.setLayout(layout_status);

		GridData gridData;

		//Composite with StackLayout
		statusArea = new Composite(statusBar, SWT.NONE);
		gridData = new GridData(GridData.FILL_BOTH);
		statusArea.setLayoutData(gridData);

		layoutStatusArea = new StackLayout();
		statusArea.setLayout(layoutStatusArea);

		//Either the Status Text
		statusText = new CLabel(statusArea, borderFlag);
		gridData = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_FILL);
		statusText.setLayoutData(gridData);

		// This is the highest image displayed on the statusbar
		Image image = ImageRepository.getImage(STATUS_ICON_WARN);
		int imageHeight = (image == null) ? 20 : image.getBounds().height;

		GC gc = new GC(statusText);
		// add 6, because CLabel forces a 3 pixel indent
		int height = Math.max(imageHeight, gc.getFontMetrics().getHeight()) + 6;
		gc.dispose();

		formData = new FormData();
		formData.height = height;
		formData.bottom = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
		formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
		formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
		statusBar.setLayoutData(formData);

		Listener listener = new Listener() {
			public void handleEvent(Event e) {
				if (updateWindow != null) {
					updateWindow.show();
				}
			}
		};

		statusText.addListener(SWT.MouseUp, listener);
		statusText.addListener(SWT.MouseDoubleClick, listener);

		//Or a composite with a label, a progressBar and a button
		statusUpdate = new Composite(statusArea, SWT.NULL);
		gridData = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_FILL);
		statusUpdate.setLayoutData(gridData);
		GridLayout layoutStatusUpdate = new GridLayout(2, false);
		layoutStatusUpdate.marginHeight = 0;
		layoutStatusUpdate.marginWidth = 0;
		statusUpdate.setLayout(layoutStatusUpdate);

		statusUpdateLabel = new Label(statusUpdate, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		statusUpdateLabel.setLayoutData(gridData);
		Messages.setLanguageText(statusUpdateLabel,
				"MainWindow.statusText.checking");
		Messages.setLanguageText(statusUpdateLabel,
				"MainWindow.status.update.tooltip");
		statusUpdateLabel.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent arg0) {
				showUpdateProgressWindow();
			}
		});

		final int progressFlag = (Constants.isOSX) ? SWT.INDETERMINATE
				: SWT.HORIZONTAL;
		statusUpdateProgressBar = new ProgressBar(statusUpdate, progressFlag);
		gridData = new GridData(GridData.FILL_BOTH);
		statusUpdateProgressBar.setLayoutData(gridData);
		Messages.setLanguageText(statusUpdateProgressBar,
				"MainWindow.status.update.tooltip");
		statusUpdateProgressBar.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent arg0) {
				showUpdateProgressWindow();
			}
		});

		layoutStatusArea.topControl = statusText;
		statusBar.layout();

		srStatus = new CLabelPadding(statusBar, borderFlag);
		srStatus.setText(MessageText.getString("SpeedView.stats.ratio"));

		COConfigurationManager.addAndFireParameterListener("Status Area Show SR",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						srStatus.setVisible(COConfigurationManager.getBooleanParameter(
								parameterName, true));
						statusBar.layout();
					}
				});

		natStatus = new CLabelPadding(statusBar, borderFlag);
		natStatus.setText("");

		COConfigurationManager.addAndFireParameterListener("Status Area Show NAT",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						natStatus.setVisible(COConfigurationManager.getBooleanParameter(
								parameterName, true));
						statusBar.layout();
					}
				});

		dhtStatus = new CLabelPadding(statusBar, borderFlag);
		dhtStatus.setText("");
		dhtStatus.setToolTipText(MessageText
				.getString("MainWindow.dht.status.tooltip"));

		COConfigurationManager.addAndFireParameterListener("Status Area Show DDB",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						dhtStatus.setVisible(COConfigurationManager.getBooleanParameter(
								parameterName, true));
						statusBar.layout();
					}
				});
		ipBlocked = new CLabelPadding(statusBar, borderFlag);
		ipBlocked.setText("{} IPs:"); //$NON-NLS-1$
		Messages.setLanguageText(ipBlocked, "MainWindow.IPs.tooltip");
		ipBlocked.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent arg0) {
				BlockedIpsWindow.showBlockedIps(azureusCore, parent.getShell());
			}
		});

		statusDown = new CLabelPadding(statusBar, borderFlag);
		statusDown.setImage(ImageRepository.getImage("down"));
		statusDown
				.setText(/*MessageText.getString("ConfigView.download.abbreviated") +*/"n/a");
		Messages.setLanguageText(statusDown,
				"MainWindow.status.updowndetails.tooltip");

		Listener lStats = new Listener() {
			public void handleEvent(Event e) {
				uiFunctions.showStats();
			}
		};

		statusUp = new CLabelPadding(statusBar, borderFlag);
		statusUp.setImage(ImageRepository.getImage("up"));
		statusUp
				.setText(/*MessageText.getString("ConfigView.upload.abbreviated") +*/"n/a");
		Messages.setLanguageText(statusUp,
				"MainWindow.status.updowndetails.tooltip");

		statusDown.addListener(SWT.MouseDoubleClick, lStats);
		statusUp.addListener(SWT.MouseDoubleClick, lStats);

		Listener lDHT = new Listener() {
			public void handleEvent(Event e) {
				uiFunctions.showStatsDHT();
			}
		};

		dhtStatus.addListener(SWT.MouseDoubleClick, lDHT);

		Listener lSR = new Listener() {
			public void handleEvent(Event e) {

				uiFunctions.showStatsTransfers();

				OverallStats stats = StatsFactory.getStats();

				long ratio = (1000 * stats.getUploadedBytes() / (stats
						.getDownloadedBytes() + 1));

				if (ratio < 900) {

					Utils
							.openURL("http://azureus.aelitis.com/wiki/index.php/Share_Ratio");
				}
			}
		};

		srStatus.addListener(SWT.MouseDoubleClick, lSR);

		Listener lNAT = new Listener() {
			public void handleEvent(Event e) {
				uiFunctions.showConfig(ConfigSection.SECTION_CONNECTION);

				if (azureusCore.getPluginManager().getDefaultPluginInterface()
						.getConnectionManager().getNATStatus() != ConnectionManager.NAT_OK) {
					Utils
							.openURL("http://azureus.aelitis.com/wiki/index.php/NAT_problem");
				}
			}
		};

		natStatus.addListener(SWT.MouseDoubleClick, lNAT);

		// Status Bar Menu construction
		final Menu menuUpSpeed = new Menu(statusBar.getShell(), SWT.POP_UP);
		menuUpSpeed.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event e) {
				SelectableSpeedMenu.generateMenuItems(menuUpSpeed, core, globalManager,
						true);
			}
		});
		statusUp.setMenu(menuUpSpeed);

		final Menu menuDownSpeed = new Menu(statusBar.getShell(), SWT.POP_UP);
		menuDownSpeed.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event e) {
				SelectableSpeedMenu.generateMenuItems(menuDownSpeed, core,
						globalManager, false);
			}
		});
		statusDown.setMenu(menuDownSpeed);

		addUpdateListener();

		return statusBar;
	}

	/**
	 * 
	 * @param keyedSentence
	 */
	public void setStatusText(String keyedSentence) {
		this.statusTextKey = keyedSentence == null ? "" : keyedSentence;
		statusImageKey = null;
		if (statusTextKey.length() == 0) { // reset
			if (Constants.isCVSVersion()) {
				statusTextKey = "MainWindow.status.unofficialversion ("
						+ Constants.AZUREUS_VERSION + ")";
				statusImageKey = STATUS_ICON_WARN;
			} else if (!Constants.isOSX) { //don't show official version numbers for OSX L&F
				statusTextKey = Constants.AZUREUS_NAME + " "
						+ Constants.AZUREUS_VERSION;
			}
		}

		updateStatusText();
	}

	/**
	 * 
	 *
	 */
	public void updateStatusText() {
		if (display == null || display.isDisposed())
			return;
		final String text;
		if (updateWindow != null) {
			text = "MainWindow.updateavail";
		} else {
			text = this.statusTextKey;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (statusText != null && !statusText.isDisposed()) {
					statusText.setText(MessageText.getStringForSentence(text));
					statusText.setImage((statusImageKey == null) ? null : ImageRepository
							.getImage(statusImageKey));
				}
			}
		});
	}

	/**
	 * 
	 *
	 */
	public void refreshStatusText() {
		if (statusText != null && !statusText.isDisposed())
			statusText.update();
	}

	/**
	 * 
	 * @param updateWindow
	 */
	public void setUpdateNeeded(UpdateWindow updateWindow) {
		this.updateWindow = updateWindow;
		if (updateWindow != null) {
			statusText.setCursor(Cursors.handCursor);
			statusText.setForeground(Colors.colorWarning);
			updateStatusText();
		} else {
			statusText.setCursor(null);
			statusText.setForeground(null);
			updateStatusText();
		}
	}

	private void addUpdateListener() {
		azureusCore.getPluginManager().getDefaultPluginInterface()
				.getUpdateManager().addListener(new UpdateManagerListener() {
					public void checkInstanceCreated(UpdateCheckInstance instance) {

						new updateStatusChanger(instance);
					}
				});
	}

	protected class updateStatusChanger {
		UpdateCheckInstance instance;

		int check_num = 0;

		boolean active;

		protected updateStatusChanger(UpdateCheckInstance _instance) {
			instance = _instance;

			try {
				this_mon.enter();

				update_stack.add(this);

				instance.addListener(new UpdateCheckInstanceListener() {
					public void cancelled(UpdateCheckInstance instance) {
						deactivate();
					}

					public void complete(UpdateCheckInstance instance) {
						deactivate();
					}
				});

				UpdateChecker[] checkers = instance.getCheckers();

				UpdateCheckerListener listener = new UpdateCheckerListener() {
					public void cancelled(UpdateChecker checker) {
						// we don't count a cancellation as progress step
					}

					public void completed(UpdateChecker checker) {
						setNextCheck();
					}

					public void failed(UpdateChecker checker) {
						setNextCheck();
					}

				};
				for (int i = 0; i < checkers.length; i++) {
					checkers[i].addListener(listener);
				}

				activate();

			} finally {

				this_mon.exit();
			}

		}

		protected UpdateCheckInstance getInstance() {
			return (instance);
		}

		private void activate() {
			try {
				this_mon.enter();

				active = true;

				switchStatusToUpdate();

				setNbChecks(instance.getCheckers().length);

			} finally {

				this_mon.exit();
			}
		}

		private void deactivate() {
			try {
				this_mon.enter();

				active = false;

				for (int i = 0; i < update_stack.size(); i++) {

					if (update_stack.get(i) == this) {

						update_stack.remove(i);

						break;
					}
				}
				if (update_stack.size() == 0) {

					switchStatusToText();

				} else {

					((updateStatusChanger) update_stack.get(update_stack.size() - 1))
							.activate();
				}

			} finally {

				this_mon.exit();
			}
		}

		private void setNbChecks(final int nbChecks) {
			if (display != null && !display.isDisposed())
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (statusUpdateProgressBar == null
								|| statusUpdateProgressBar.isDisposed())
							return;
						statusUpdateProgressBar.setMinimum(0);
						statusUpdateProgressBar.setMaximum(nbChecks);
						statusUpdateProgressBar.setSelection(check_num);
					}
				});
		}

		private void setNextCheck() {
			if (display != null && !display.isDisposed())
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (statusUpdateProgressBar == null
								|| statusUpdateProgressBar.isDisposed())
							return;

						check_num++;

						if (active) {
							statusUpdateProgressBar.setSelection(check_num);
						}
					}
				});
		}

		private void switchStatusToUpdate() {
			if (display != null && !display.isDisposed())
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (statusArea == null || statusArea.isDisposed()) {
							return;
						}

						String name = instance.getName();

						if (MessageText.keyExists(name)) {

							name = MessageText.getString(name);
						}

						statusUpdateLabel.setText(name);

						layoutStatusArea.topControl = statusUpdate;
						statusArea.layout();
					}
				});
		}

		private void switchStatusToText() {
			if (display != null && !display.isDisposed())
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (statusArea == null || statusArea.isDisposed()) {
							return;
						}
						layoutStatusArea.topControl = statusText;
						statusArea.layout();
					}
				});
		}
	}

	protected void showUpdateProgressWindow() {
		try {
			this_mon.enter();

			UpdateCheckInstance[] instances = new UpdateCheckInstance[update_stack
					.size()];

			for (int i = 0; i < instances.length; i++) {

				instances[i] = ((updateStatusChanger) update_stack.get(i))
						.getInstance();
			}

			UpdateProgressWindow.show(instances, statusBar.getShell());

		} finally {

			this_mon.exit();
		}
	}

	/**
	 */
	public void refreshStatusBar() {
		if (ipBlocked.isDisposed()) {
			return;
		}
		
		// IP Filter Status Section
		IpFilter ip_filter = azureusCore.getIpFilterManager().getIPFilter();

		ipBlocked.setText("IPs: "
				+ numberFormat.format(ip_filter.getNbRanges())
				+ " - "
				+ numberFormat.format(ip_filter.getNbIpsBlockedAndLoggable())
				+ "/"
				+ numberFormat.format(ip_filter.getNbBannedIps())
				+ "/"
				+ numberFormat.format(azureusCore.getIpFilterManager().getBadIps()
						.getNbBadIps()));
		ipBlocked.setToolTipText(DisplayFormatters.formatDateShort(ip_filter
				.getLastUpdateTime()));

		// SR status section

		long ratio = (1000 * overall_stats.getUploadedBytes() / (overall_stats
				.getDownloadedBytes() + 1));

		int sr_status;

		if (ratio < 500) {

			sr_status = 0;

		} else if (ratio < 900) {

			sr_status = 1;

		} else {

			sr_status = 2;
		}

		if (sr_status != last_sr_status) {

			String imgID;

			switch (sr_status) {
				case 2:
					imgID = "greenled";
					break;

				case 1:
					imgID = "yellowled";
					break;

				default:
					imgID = "redled";
					break;
			}

			srStatus.setImage(ImageRepository.getImage(imgID));

			last_sr_status = sr_status;
		}

		if (ratio != last_sr_ratio) {

			String tooltipID;

			switch (sr_status) {
				case 2:
					tooltipID = "MainWindow.sr.status.tooltip.ok";
					break;

				case 1:
					tooltipID = "MainWindow.sr.status.tooltip.poor";
					break;

				default:
					tooltipID = "MainWindow.sr.status.tooltip.bad";
					break;
			}

			String ratio_str = "";

			String partial = "" + ratio % 1000;

			while (partial.length() < 3) {

				partial = "0" + partial;
			}

			ratio_str = (ratio / 1000) + "." + partial;

			srStatus.setToolTipText(MessageText.getString(tooltipID,
					new String[] { ratio_str }));

			last_sr_ratio = ratio;
		}

		// NAT status Section

		int nat_status = connection_manager.getNATStatus();

		if (lastNATstatus != nat_status) {
			String imgID;
			String tooltipID;
			String statusID;

			switch (nat_status) {
				case ConnectionManager.NAT_UNKNOWN:
					imgID = "grayled";
					tooltipID = "MainWindow.nat.status.tooltip.unknown";
					statusID = "MainWindow.nat.status.unknown";
					break;

				case ConnectionManager.NAT_OK:
					imgID = "greenled";
					tooltipID = "MainWindow.nat.status.tooltip.ok";
					statusID = "MainWindow.nat.status.ok";
					break;

				case ConnectionManager.NAT_PROBABLY_OK:
					imgID = "yellowled";
					tooltipID = "MainWindow.nat.status.tooltip.probok";
					statusID = "MainWindow.nat.status.probok";
					break;

				default:
					imgID = "redled";
					tooltipID = "MainWindow.nat.status.tooltip.bad";
					statusID = "MainWindow.nat.status.bad";
					break;
			}

			natStatus.setImage(ImageRepository.getImage(imgID));
			natStatus.setToolTipText(MessageText.getString(tooltipID));
			natStatus.setText(MessageText.getString(statusID));
			lastNATstatus = nat_status;
		}

		// DHT Status Section
		int dht_status = (dhtPlugin == null) ? DHTPlugin.STATUS_DISABLED
				: dhtPlugin.getStatus();
		long dht_count = -1;
		if (dht_status == DHTPlugin.STATUS_RUNNING) {
			DHT[] dhts = dhtPlugin.getDHTs();

			if (dhts.length > 0 && dhts[0].getTransport().isReachable()) {
				dht_count = dhts[0].getControl().getStats().getEstimatedDHTSize();
			}
		}

		if (lastDHTstatus != dht_status || lastDHTcount != dht_count) {
			switch (dht_status) {
				case DHTPlugin.STATUS_RUNNING:
					if (dht_count > 100 * 1000) { //release dht has at least a half million users
						dhtStatus.setImage(ImageRepository.getImage("greenled"));
						dhtStatus.setToolTipText(MessageText
								.getString("MainWindow.dht.status.tooltip"));
						dhtStatus.setText(MessageText.getString("MainWindow.dht.status.users").replaceAll("%1", numberFormat.format(dht_count)));
					} else {
						dhtStatus.setImage(ImageRepository.getImage("yellowled"));
						dhtStatus.setToolTipText(MessageText
								.getString("MainWindow.dht.status.unreachabletooltip"));
						dhtStatus.setText(MessageText
								.getString("MainWindow.dht.status.unreachable"));
					}
					break;

				case DHTPlugin.STATUS_DISABLED:
					dhtStatus.setImage(ImageRepository.getImage("grayled"));
					dhtStatus.setText(MessageText
							.getString("MainWindow.dht.status.disabled"));
					break;

				case DHTPlugin.STATUS_INITALISING:
					dhtStatus.setImage(ImageRepository.getImage("yellowled"));
					dhtStatus.setText(MessageText
							.getString("MainWindow.dht.status.initializing"));
					break;

				case DHTPlugin.STATUS_FAILED:
					dhtStatus.setImage(ImageRepository.getImage("redled"));
					dhtStatus.setText(MessageText
							.getString("MainWindow.dht.status.failed"));
					break;

				default:
					dhtStatus.setImage(null);
					break;
			}

			lastDHTstatus = dht_status;
			lastDHTcount = dht_count;
		}

		// UL/DL Status Sections

		int dl_limit = NetworkManager.getMaxDownloadRateBPS() / 1024;

		GlobalManagerStats stats = globalManager.getStats();

		statusDown.setText((dl_limit == 0 ? "" : "[" + dl_limit + "K] ")
				+ DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(stats
						.getDataReceiveRate(), stats.getProtocolReceiveRate()));

		boolean auto_up = COConfigurationManager
				.getBooleanParameter(TransferSpeedValidator
						.getActiveAutoUploadParameter(globalManager))
				&& TransferSpeedValidator.isAutoUploadAvailable(azureusCore);

		int ul_limit_norm = NetworkManager.getMaxUploadRateBPSNormal() / 1024;

		String seeding_only;
		if (NetworkManager.isSeedingOnlyUploadRate()) {
			int ul_limit_seed = NetworkManager.getMaxUploadRateBPSSeedingOnly() / 1024;
			if (ul_limit_seed == 0) {
				seeding_only = "+" + Constants.INFINITY_STRING + "K";
			} else {
				int diff = ul_limit_seed - ul_limit_norm;
				seeding_only = (diff >= 0 ? "+" : "") + diff + "K";
			}
		} else {
			seeding_only = "";
		}

		statusUp.setText((ul_limit_norm == 0 ? "" : "[" + ul_limit_norm + "K"
				+ seeding_only + "]")
				+ (auto_up ? "* " : " ")
				+ DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(stats
						.getDataSendRate(), stats.getProtocolSendRate()));

		// End of Status Sections
		statusBar.layout();
	}

	/**
	 * @param string
	 */
	public void setDebugInfo(String string) {
		if (!statusText.isDisposed())
			statusText.setToolTipText(string);
	}

	/**
	 * CLabel that shrinks to fit text after a specific period of time.
	 * Makes textual changes less jumpy
	 * 
	 * @author TuxPaper
	 * @created Mar 21, 2006
	 *
	 */
	private class CLabelPadding extends CLabel {
		private int lastWidth = 0;

		private long widthSetOn = 0;

		private static final int KEEPWIDTHFOR_MS = 30 * 1000;

		/**
		 * Default Constructor
		 * 
		 * @param parent
		 * @param style
		 */
		public CLabelPadding(Composite parent, int style) {
			super(parent, style | SWT.CENTER);

			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER
					| GridData.VERTICAL_ALIGN_FILL);
			setLayoutData(gridData);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.custom.CLabel#computeSize(int, int, boolean)
		 */
		public Point computeSize(int wHint, int hHint, boolean changed) {
			if (!isVisible()) {
				return (new Point(0, 0));
			}
			Point pt = super.computeSize(wHint, hHint, changed);
			pt.x += 4;

			long now = System.currentTimeMillis();
			if (lastWidth > pt.x && now - widthSetOn < KEEPWIDTHFOR_MS) {
				pt.x = lastWidth;
			} else {
				if (lastWidth != pt.x)
					lastWidth = pt.x;
				widthSetOn = now;
			}

			return pt;
		}
	}

}
