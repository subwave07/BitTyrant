/*
 * File    : SavePathItem.java
 * Created : 01 febv. 2004
 * By      : TuxPaper
 *
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


public class FilesDoneItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  public FilesDoneItem(String sTableID) {
	  super("filesdone", ALIGN_CENTER, POSITION_INVISIBLE, 50, sTableID);
	  setRefreshInterval(5);
  }

  public void refresh(TableCell cell) {
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    
    String	text = "";
    
    if ( dm != null ){
    	int	complete 			= 0;
    	int	skipped				= 0;
    	int	skipped_complete	= 0;
    	
    	DiskManagerFileInfo[]	files = dm.getDiskManagerFileInfo();
    	
    	int	total	= files.length;
    	
    	for (int i=0;i<files.length;i++){
    		DiskManagerFileInfo	file = files[i];
    		
    		if ( file.getLength() == file.getDownloaded()){
    			complete++;
    			if ( file.isSkipped()){
    				skipped++;
    				skipped_complete++;
    			}
    		}else if ( file.isSkipped()){
    			skipped++;
    		}
    	}
    	
    	if ( skipped == 0 ){
    		text = complete + "/" + total;
    	}else{
    		text = (complete-skipped_complete) + "(" + complete + ")/" + (total-skipped) + "(" + total + ")"; 
    	}
    }
    
    cell.setText( text );
  }
}
