/*
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package com.aelitis.azureus.plugins.startstoprules.defaultplugin.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

/** Seeding Automation Specific options
 * @author TuxPaper
 * @created Jan 12, 2004
 * 
 * TODO: StartStopManager_fAddForSeedingULCopyCount
 */
public class ConfigSectionSeeding implements UISWTConfigSection {
  public String configSectionGetParentSection() {
    return "queue";
  }

  public String configSectionGetName() {
    return "queue.seeding";
  }

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }

  public Composite configSectionCreate(Composite parent) {
    // Seeding Automation Setup
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite cSeeding = new Composite(parent, SWT.NULL);

    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    cSeeding.setLayout(layout);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cSeeding.setLayoutData(gridData);

    // General Seeding Options
    label = new Label(cSeeding, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.minSeedingTime");
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(cSeeding, "StartStopManager_iMinSeedingTime").setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cSeeding, "Disconnect Seed", true,
                         "ConfigView.label.disconnetseed").setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cSeeding, "Use Super Seeding", false,
                         "ConfigView.label.userSuperSeeding").setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cSeeding, "StartStopManager_bAutoReposition",
                         "ConfigView.label.seeding.autoReposition").setLayoutData(gridData);

    label = new Label(cSeeding, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.addForSeedingDLCopyCount");
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(cSeeding, "StartStopManager_iAddForSeedingDLCopyCount").setLayoutData(gridData);

    label = new Label(cSeeding, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.numPeersAsFullCopy");

    Composite cArea = new Composite(cSeeding, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 2;
    cArea.setLayout(layout);
    gridData = new GridData();
    cArea.setLayoutData(gridData);

    gridData = new GridData();
    gridData.widthHint = 20;
    IntParameter paramFakeFullCopy = new IntParameter(cArea, "StartStopManager_iNumPeersAsFullCopy");
    paramFakeFullCopy.setLayoutData(gridData);
    final Text txtFakeFullCopy = (Text)paramFakeFullCopy.getControl();

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.peers");


    final Composite cFullCopyOptionsArea = new Composite(cSeeding, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 4;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    cFullCopyOptionsArea.setLayout(layout);
    gridData = new GridData();
    gridData.horizontalIndent = 15;
    gridData.horizontalSpan = 2;
    cFullCopyOptionsArea.setLayoutData(gridData);
    
    label = new Label(cFullCopyOptionsArea, SWT.NULL);
    Image img = ImageRepository.getImage("subitem");
    img.setBackground(label.getBackground());
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    label.setLayoutData(gridData);
    label.setImage(img);
    
    label = new Label(cFullCopyOptionsArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.fakeFullCopySeedStart");

    gridData = new GridData();
    gridData.widthHint = 20;
    new IntParameter(cFullCopyOptionsArea, "StartStopManager_iFakeFullCopySeedStart").setLayoutData(gridData);
    label = new Label(cFullCopyOptionsArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeds");
    

    final int iNumPeersAsFullCopy = COConfigurationManager.getIntParameter("StartStopManager_iNumPeersAsFullCopy");
    controlsSetEnabled(cFullCopyOptionsArea.getChildren(), iNumPeersAsFullCopy != 0);

    paramFakeFullCopy.getControl().addListener(SWT.Modify, new Listener() {
        public void handleEvent(Event event) {
          try {
            Text control = (Text)event.widget;
            if (control.getEnabled()) {
              int value = Integer.parseInt(control.getText());
              boolean enabled = (value != 0);
              if (cFullCopyOptionsArea.getEnabled() != enabled) {
                cFullCopyOptionsArea.setEnabled(enabled);
                controlsSetEnabled(cFullCopyOptionsArea.getChildren(), enabled);
              }
            }
          }
          catch (Exception e) {}
        }
    });

    return cSeeding;
  }

  private void controlsSetEnabled(Control[] controls, boolean bEnabled) {
    for(int i = 0 ; i < controls.length ; i++) {
      if (controls[i] instanceof Composite)
        controlsSetEnabled(((Composite)controls[i]).getChildren(), bEnabled);
      controls[i].setEnabled(bEnabled);
    }
  }
}

