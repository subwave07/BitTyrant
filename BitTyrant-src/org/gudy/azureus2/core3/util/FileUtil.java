 /*
 * Created on Oct 10, 2003
 * Modified Apr 14, 2004 by Alon Rohter
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * 
 */

package org.gudy.azureus2.core3.util;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationTask;

/**
 * File utility class.
 */
public class FileUtil {
	private static final LogIDs LOGID = LogIDs.CORE;
  public static final String DIR_SEP = System.getProperty("file.separator");
  
  private static final int	RESERVED_FILE_HANDLE_COUNT	= 4;
	
  private static List		reserved_file_handles 	= new ArrayList();
  private static AEMonitor	class_mon				= new AEMonitor( "FileUtil:class" );

  
  public static String getCanonicalFileName(String filename) {
    // Sometimes Windows use filename in 8.3 form and cannot
    // match .torrent extension. To solve this, canonical path
    // is used to get back the long form

    String canonicalFileName = filename;
    try {
      canonicalFileName = new File(filename).getCanonicalPath();
    }
    catch (IOException ignore) {}
    return canonicalFileName;
  }

  
  public static File getUserFile(String filename) {
    return new File(SystemProperties.getUserPath(), filename);
  }
  
  public static File getApplicationFile(String filename) {
      
    String path = SystemProperties.getApplicationPath();
      
      if(Constants.isOSX) {
        path = path + "/" + SystemProperties.getApplicationName() + ".app/Contents/";
      }
      
      return new File(path, filename);
  }
  
  
  
  /**
   * Deletes the given dir and all files/dirs underneath
   */
  public static void recursiveDelete(File f) {
    String defSaveDir = COConfigurationManager.getStringParameter("Default save path", "");
    String moveToDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");
    
    try{
  	  moveToDir = new File(moveToDir).getCanonicalPath();
    }catch( Throwable e ){
    }
    try{
    	defSaveDir = new File(defSaveDir).getCanonicalPath();
    }catch( Throwable e ){
    }
    
    try {

      if (f.getCanonicalPath().equals(moveToDir)) {
        System.out.println("FileUtil::recursiveDelete:: not allowed to delete the MoveTo dir !");
        return;
      }
      if (f.getCanonicalPath().equals(defSaveDir)) {
        System.out.println("FileUtil::recursiveDelete:: not allowed to delete the default data dir !");
        return;
      }
      
      if (f.isDirectory()) {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
          recursiveDelete(files[i]);
        }
        f.delete();
      }
      else {
        f.delete();
      }
    } catch (Exception ignore) {/*ignore*/}
  }
  
  public static long
  getFileOrDirectorySize(
  	File		file )
  {
  	if ( file.isFile()){
  		
  		return( file.length());
  		
  	}else{
  		
  		long	res = 0; 
  			
  		File[] files = file.listFiles();
  		
  		if ( files != null ){
  			
  			for (int i=0;i<files.length;i++){
  				
  				res += getFileOrDirectorySize( files[i] );
  			}
  		}
  		
  		return( res );
  	}
  }
  
  protected static void 
  recursiveEmptyDirDelete(
  	File	f,
	Set		ignore_set,
	boolean	log_warnings ) 
  {
     try {
      String defSaveDir 	= COConfigurationManager.getStringParameter("Default save path", "");
      String moveToDir 		= COConfigurationManager.getStringParameter("Completed Files Directory", "");
        
      if ( defSaveDir.trim().length() > 0 ){
      	
      	defSaveDir = new File(defSaveDir).getCanonicalPath();
      }

      if ( moveToDir.trim().length() > 0 ){
      	
      	moveToDir = new File(moveToDir).getCanonicalPath();
      }
      
      if ( f.isDirectory()){
      	
        File[] files = f.listFiles();
        
        if ( files == null ){
        	
        	if (log_warnings ){
        		Debug.out("Empty folder delete:  failed to list contents of directory " + f );
        	}
          	 
          	return;
        }
        
        for (int i = 0; i < files.length; i++) {
        	
        	File	x = files[i];
        	
        	if ( x.isDirectory()){
        		
        		recursiveEmptyDirDelete(files[i],ignore_set,log_warnings);
        		
        	}else{
        		
        		if ( ignore_set.contains( x.getName().toLowerCase())){
        			
        			if ( !x.delete()){
        				
        				if ( log_warnings ){
        					Debug.out("Empty folder delete: failed to delete file " + x );
        				}
        			}
        		}
        	}
        }

        if (f.getCanonicalPath().equals(moveToDir)) {
        	
        	if ( log_warnings ){
        		Debug.out("Empty folder delete:  not allowed to delete the MoveTo dir !");
        	}
          
          return;
        }
        
        if (f.getCanonicalPath().equals(defSaveDir)) {
        	
        	if ( log_warnings ){
        		Debug.out("Empty folder delete:  not allowed to delete the default data dir !");
        	}
          
          return;
        }

        if (f.listFiles().length == 0) {
        	
          if ( !f.delete()){
          	
        	  if ( log_warnings ){
        		  Debug.out("Empty folder delete:  failed to delete directory " + f );
        	  }
          }
        }else{
        	if ( log_warnings ){
        		Debug.out("Empty folder delete: "+f.listFiles().length+" file(s)/folder(s) still in " + f + ". Not removing.");
        	}
        }
      }

    } catch (Exception e) { Debug.out(e.toString()); }
  }
  
  public static String
  convertOSSpecificChars(
  	String	file_name_in )
  {
  		// this rule originally from DiskManager
 
  	char[]	chars = file_name_in.toCharArray();
  	
  	for (int i=0;i<chars.length;i++){
  		
  		if ( chars[i] == '"' ){
  			
  			chars[i] = '\'';
  		}
  	}
  	
  	if ( !Constants.isOSX ){
  		
  		if ( Constants.isWindows ){
  			
  				//  this rule originally from DiskManager

	 		// The definitive list of characters permitted for Windows is defined here:
	 		// http://support.microsoft.com/kb/q120138/
  			String not_allowed = "\\/:?*<>"; 
  		 	for (int i=0;i<chars.length;i++){
  		 		if (not_allowed.indexOf(chars[i]) != -1) {
  		  			chars[i] = '_';
  		  		}
  		  	}
  		}
  		
  			// '/' is valid in mac file names, replace with space
  			// so it seems are cr/lf
  		
	 	for (int i=0;i<chars.length;i++){
		 		
			char	c = chars[i];
				
			if ( c == '/' || c == '\r' || c == '\n'  ){
		  			
				chars[i] = ' ';
			}
		}
  	}

  	String	file_name_out = new String(chars);
  	
	try{
		
			// mac file names can end in space - fix this up by getting
			// the canonical form which removes this on Windows
		
			// however, for soem reason getCanonicalFile can generate high CPU usage on some user's systems
			// in  java.io.Win32FileSystem.canonicalize
			// so changing this to only be used on non-windows
		
		if ( Constants.isWindows ){
			
			while( file_name_out.endsWith( " " )){
				
				file_name_out = file_name_out.substring(0,file_name_out.length()-1);
			}
			
		}else{
			
			String str = new File(file_name_out).getCanonicalFile().toString();
		
			int	p = str.lastIndexOf( File.separator );
			
			file_name_out = str.substring(p+1);
		}
		
	}catch( Throwable e ){
		// ho hum, carry on, it'll fail later
		//e.printStackTrace();
	}
	
	//System.out.println( "convertOSSpecificChars: " + file_name_in + " ->" + file_name_out );
	
	return( file_name_out );
  }
  
  public static void
  writeResilientConfigFile(
  	String		file_name,
	Map			data )
  {
	  File parent_dir = new File(SystemProperties.getUserPath());
	  
	  boolean use_backups = COConfigurationManager.getBooleanParameter("Use Config File Backups" );

	  writeResilientFile( parent_dir, file_name, data, use_backups );
  }
  
  public static void
  writeResilientFile(
	File		file,
	Map			data )
  {
	  writeResilientFile( file.getParentFile(), file.getName(), data, false );
  }
  
  public static void
  writeResilientFile(
    File		parent_dir,
  	String		file_name,
	Map			data,
	boolean		use_backup )
  {	  
	  if ( use_backup ){
		  
		  File	originator = new File( parent_dir, file_name );
		  
		  if ( originator.exists()){
			  
			  backupFile( originator, true );
		  }
	  }
	  
	  writeResilientFile( parent_dir, file_name, data );
  }
  
  	// synchronise it to prevent concurrent attempts to write the same file
  
  private static void
  writeResilientFile(
	File		parent_dir,
  	String		file_name,
	Map			data )
  {
  	try{
  		class_mon.enter();
  	
	  	try{
	  		getReservedFileHandles();
	      File temp = new File(  parent_dir, file_name + ".saving");
		    BufferedOutputStream	baos = null;
		    
		    try{
		    	byte[] encoded_data = BEncoder.encode(data);
		    	baos = new BufferedOutputStream( new FileOutputStream( temp, false ), 8192 );
		    	baos.write( encoded_data );
		    	baos.flush();
	        baos.close();
	        baos = null;
	           
	        //only use newly saved file if it got this far, i.e. it saved successfully
	        if ( temp.length() > 1L ) {
	        	File file = new File( parent_dir, file_name );
	        	if ( file.exists() ){
	        		file.delete();
	        	}
	        	temp.renameTo( file );
	        }
	
		    }catch (Exception e) {
		    	Logger.log(new LogAlert(LogAlert.UNREPEATABLE, "Save of '"
							+ file_name + "' fails", e));
		    	
		    }finally{
		    	
		    	try {
		    		if (baos != null){
		    			
		    			baos.close();
		    		}
		    	}catch( Exception e){
		    		Logger.log(new LogAlert(LogAlert.UNREPEATABLE, "Save of '"
								+ file_name + "' fails", e)); 
		    	}
		    }
	  	}finally{
	  		
	  		releaseReservedFileHandles();
	  	}
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
	public static Map
	readResilientConfigFile(
		String		file_name )
	{
 		File parent_dir = new File(SystemProperties.getUserPath());
  		
 		boolean use_backups = COConfigurationManager.getBooleanParameter("Use Config File Backups" );

 		return( readResilientFile( parent_dir, file_name, use_backups ));
	}
	
	public static Map
	readResilientConfigFile(
		String		file_name,
		boolean		use_backups )
	{
 		File parent_dir = new File(SystemProperties.getUserPath());
  		
 		if ( !use_backups ){
 			
 				// override if a backup file exists. This is needed to cope with backups
 				// of the main config file itself as when boostrapping we can't get the
 				// "use backups" 
 			
 			if ( new File( parent_dir, file_name + ".bak").exists()){
 				
 				use_backups = true;
 			}
 		}
 		
 		return( readResilientFile( parent_dir, file_name, use_backups ));
	}
	
	public static Map
	readResilientFile(
		File		file )
	{
		return( readResilientFile( file.getParentFile(),file.getName(),false));
	}
	
 	public static Map
	readResilientFile(
		File		parent_dir,
		String		file_name,
		boolean		use_backup )
 	{
		File	backup_file = new File( parent_dir, file_name + ".bak" );
		 
 		if ( use_backup ){	
			
 			use_backup = backup_file.exists();
 		}
 			
 			// if we've got a backup, don't attempt recovery here as the .bak file may be
 			// fully OK
 		
 		Map	res = readResilientFileSupport( parent_dir, file_name, !use_backup );
 		
 		if ( res == null && use_backup ){
 				
 				// try backup without recovery
 			
 		 	res = readResilientFileSupport( parent_dir, file_name + ".bak", false );
 		 		
	 		if ( res != null ){
	 			
	 			Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_WARNING,
						"Backup file '" + backup_file
								+ "' has been used for recovery purposes"));
				
					// rewrite the good data, don't use backups here as we want to
					// leave the original backup in place for the moment
				
				writeResilientFile( parent_dir, file_name, res, false );
				
	 		}else{
	 			
	 				// neither main nor backup file ok, retry main file with recovery
	 			
	 			res = readResilientFileSupport( parent_dir, file_name, true );
	 		}
 		}
 		
 		if ( res == null ){
 			
 			res = new HashMap();
 		}
 		
 		return( res );
 	}

  		// synchronised against writes to make sure we get a consistent view
  
  	private static Map
	readResilientFileSupport(
		File		parent_dir,
		String		file_name,
		boolean		attempt_recovery )
	{
   		try{
  			class_mon.enter();
	  	
	  		try{
	  			getReservedFileHandles();
	  	
	  			Map	res = null;
	  			
	  			try{
	  				res = readResilientFile( parent_dir, file_name, 0, false );
	  			
	  			}catch( Throwable e ){
	  				
	  				// ignore, it'll be rethrown if we can't recover below
	  			}
	  			
	  			if ( res == null && attempt_recovery ){
	  				
	  				res = readResilientFile( parent_dir, file_name, 0, true );
	  				
	  				if ( res != null ){
	  					Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_WARNING,
								"File '" + file_name + "' has been partially recovered, "
										+ "information may have been lost!"));
	  				}
	  			}
	  			
	  			return( res );
	  			
	  		}catch( Throwable e ){
	  				  			
	  			Debug.printStackTrace( e );
	  			
	  			return( null );
	  			
	  		}finally{
	  			
	  			releaseReservedFileHandles();
	  		}
  		}finally{
  			
  			class_mon.exit();
  		}
  	}
  	
	private static Map
	readResilientFile(
		File		parent_dir,
		String		file_name,
		int			fail_count,
		boolean		recovery_mode )
	{	  
			// logging in here is only done during "non-recovery" mode to prevent subsequent recovery
			// attempts logging everything a second time.
			// recovery-mode allows the decoding process to "succeed" with a partially recovered file
		
  		boolean	using_backup	= file_name.endsWith(".saving");
  		
  		File file = new File(  parent_dir, file_name );
	    
	   		//make sure the file exists and isn't zero-length
	    
  		if ( (!file.exists()) || file.length() <= 1L ){

  			if ( using_backup ){
	     
  				if ( !recovery_mode ){
  					
	  				if ( fail_count == 1 ){
	  					
	  					// we only alert the user if at least one file was found and failed
	  					// otherwise it could be start of day when neither file exists yet
	  					Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
								"Load of '" + file_name + "' fails, no usable file or backup"));
	  				}else{
	  					if (Logger.isEnabled())
								Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR, "Load of '"
										+ file_name + "' fails, file not found"));
					
	  				}
  				}
	       
  				return( null );
  			}
        
  			if ( !recovery_mode ){
  				
  				// kinda confusing log this as we get it under "normal" circumstances (loading a config
  				// file that doesn't exist legitimately, e.g. shares or bad-ips
//  				if (Logger.isEnabled())
//						Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR, "Load of '"
//								+ file_name + "' failed, " + "file not found or 0-sized."));
  			}
  			
  			return( readResilientFile( parent_dir, file_name + ".saving", 0, recovery_mode ));
  		}

  		BufferedInputStream bin = null;
   
  		try{
  			int	retry_limit = 5;
  			
  			while(true){
  				
  				try{
  					bin = new BufferedInputStream( new FileInputStream(file), 16384 );
  				
  					break;
  					
  				}catch( IOException e ){
  				
  	 				if ( --retry_limit == 0 ){
  						
  						throw( e );
  					}
  	 				
  	 				if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, "Failed to open '" + file.toString()
								+ "', retrying", e));
  					
  					Thread.sleep(500);
  				}
  			}
  			
  			BDecoder	decoder = new BDecoder();
  			
  			if ( recovery_mode ){
  				
  				decoder.setRecoveryMode( true );
  			}
  			
	    	Map	res = decoder.decodeStream(bin);
	    	
	    	if ( using_backup && !recovery_mode ){
  		
	    		Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_WARNING,
						"Load of '" + file_name.substring(0, file_name.length() - 7)
								+ "' had to revert to backup file")); 
	    	}
	    	
	    	return( res );
	    	
	    }catch( Throwable e ){
	    	
	    	Debug.printStackTrace( e );
	    	
	    	try {  
	    		if (bin != null){
	    			
	    			bin.close();
	    			
	    			bin	= null;
	    		}
	    	} catch (Exception x) { 
	    		
	    		Debug.printStackTrace( x ); 
	    	}
	    	
	    		// if we're not recovering then backup the file
	    	
	    	if ( !recovery_mode ){
	    		
	   			// Occurs when file is there but b0rked
      			// copy it in case it actually contains useful data, so it won't be overwritten next save
    	
		    	File bad;
		    	
		    	int	bad_id = 0;
		    	
		    	while(true){
		    		
		    		File	test = new File( parent_dir, file.getName() + ".bad" + (bad_id==0?"":(""+bad_id)));
		    		
		    		if ( !test.exists()){
		    			
		    			bad	= test;
		    			
		    			break;
		    		}
		    		
		    		bad_id++;
		    	}

		    	if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING, "Read of '"
								+ file_name + "' failed, decoding error. " + "Renaming to "
								+ bad.getName()));
	
		    		// copy it so its left in place for possible recovery
		    	
		    	copyFile( file, bad );
	    	}
	    	
	    	if ( using_backup ){
		
	    		if ( !recovery_mode ){
	    			Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
							"Load of '" + file_name + "' fails, no usable file or backup")); 
	    		}
	    			
	    		return( null );
	    	}
	    	
	    	return( readResilientFile( parent_dir, file_name + ".saving", 1, recovery_mode ));
 			 
	    }finally{
	    	
	    	try {
	    		
	    		if (bin != null){
	    			
	    			bin.close();
	    		}
	    	}catch (Exception e) {
	    		
	    		Debug.printStackTrace( e );
	    	}
	    }
	}
  

	private static void
	getReservedFileHandles()
	{
		try{
			class_mon.enter();
		
			while( reserved_file_handles.size() > 0 ){
				
				// System.out.println( "releasing reserved file handle");
				
				InputStream	is = (InputStream)reserved_file_handles.remove(0);
				
				try{
					is.close();
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
		}finally{
			
			class_mon.exit();
		}
	}
			
	private static void
	releaseReservedFileHandles()
	{
		try{
			
			class_mon.enter();
			
			File	lock_file	= new File(SystemProperties.getUserPath() + ".lock");
							
			lock_file.createNewFile();
		
			while(  reserved_file_handles.size() < RESERVED_FILE_HANDLE_COUNT ){
				
				// System.out.println( "getting reserved file handle");
				
				InputStream	is = new FileInputStream( lock_file );
				
				reserved_file_handles.add(is);
			}
		}catch( Throwable e ){
		
			Debug.printStackTrace( e );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
    /**
     * Backup the given file to filename.bak, removing the old .bak file if necessary.
     * If _make_copy is true, the original file will copied to backup, rather than moved.
     * @param _filename name of file to backup
     * @param _make_copy copy instead of move
     */
    public static void backupFile( final String _filename, final boolean _make_copy ) {
      backupFile( new File( _filename ), _make_copy );
    }
      
    /**
     * Backup the given file to filename.bak, removing the old .bak file if necessary.
     * If _make_copy is true, the original file will copied to backup, rather than moved.
     * @param _file file to backup
     * @param _make_copy copy instead of move
     */
    public static void backupFile( final File _file, final boolean _make_copy ) {
      if ( _file.length() > 0L ) {
        File bakfile = new File( _file.getAbsolutePath() + ".bak" );
        if ( bakfile.exists() ) bakfile.delete();
        if ( _make_copy ) {
          copyFile( _file, bakfile );
        }
        else {
          _file.renameTo( bakfile );
        }
      }
    }
    
    
    /**
     * Copy the given source file to the given destination file.
     * Returns file copy success or not.
     * @param _source_name source file name
     * @param _dest_name destination file name
     * @return true if file copy successful, false if copy failed
     */
    public static boolean copyFile( final String _source_name, final String _dest_name ) {
      return copyFile( new File(_source_name), new File(_dest_name));
    }
    
    /**
     * Copy the given source file to the given destination file.
     * Returns file copy success or not.
     * @param _source source file
     * @param _dest destination file
     * @return true if file copy successful, false if copy failed
     */
    /*
    // FileChannel.transferTo() seems to fail under certain linux configurations.
    public static boolean copyFile( final File _source, final File _dest ) {
      FileChannel source = null;
      FileChannel dest = null;
      try {
        if( _source.length() < 1L ) {
          throw new IOException( _source.getAbsolutePath() + " does not exist or is 0-sized" );
        }
        source = new FileInputStream( _source ).getChannel();
        dest = new FileOutputStream( _dest ).getChannel();
      
        source.transferTo(0, source.size(), dest);
        return true;
      }
      catch (Exception e) {
        Debug.out( e );
        return false;
      }
      finally {
        try {
          if (source != null) source.close();
          if (dest != null) dest.close();
        }
        catch (Exception ignore) {}
      }
    }
    */
    
    public static boolean copyFile( final File _source, final File _dest ) {
      try {
        copyFile( new FileInputStream( _source ), new FileOutputStream( _dest ) );
        return true;
      }
      catch( Throwable e ) {
      	Debug.printStackTrace( e );
        return false;
      }
    }
    
    public static boolean copyFile( final File _source, final OutputStream _dest, boolean closeInputStream ) {
        try {
          copyFile( new FileInputStream( _source ), _dest, closeInputStream );
          return true;
        }
        catch( Throwable e ) {
        	Debug.printStackTrace( e );
          return false;
        }
      }
    
    public static void 
    copyFile( 
    	final InputStream _source, 
    	final File _dest )
    
    	throws IOException
    {
    	FileOutputStream	dest = null;
    	
    	try{
    		dest = new FileOutputStream(_dest);
   
    		copyFile( _source, dest, true );
    		
    	}finally{
    		
    		if ( dest != null ){
    			
    			dest.close();
    		}
    	}
    }
    
    public static void 
    copyFile( 
      InputStream   is,
      OutputStream  os ) 
    throws IOException {
      copyFile(is,os,true);
    }
    
    public static void 
	copyFile( 
		InputStream		is,
		OutputStream	os,
		boolean 		closeInputStream )
	
		throws IOException
	{
    	try{
    		
    		if ( !(is instanceof BufferedInputStream )){
    			
    			is = new BufferedInputStream(is);
    		}
    		
    		byte[]	buffer = new byte[65536*2];
			
    		while(true){
    			
    			int	len = is.read(buffer);
    			
    			if ( len == -1 ){
    				
    				break;
    			}
    			
    			os.write( buffer, 0, len );
    		}
    	}finally{
    		try{
    			if(closeInputStream){
    			  is.close();
    			}
    		}catch( IOException e ){
    			
    		}
    		
    		os.close();
    	}
	}
    
    public static void
    copyFileOrDirectory(
    	File	from_file_or_dir,
    	File	to_parent_dir )
    
    	throws IOException
    {
    	if ( !from_file_or_dir.exists()){
    		
    		throw( new IOException( "File '" + from_file_or_dir.toString() + "' doesn't exist" ));
    	}
    	
    	if ( !to_parent_dir.exists()){
    		
    		throw( new IOException( "File '" + to_parent_dir.toString() + "' doesn't exist" ));
    	}
    	
    	if ( !to_parent_dir.isDirectory()){
    		
    		throw( new IOException( "File '" + to_parent_dir.toString() + "' is not a directory" ));
    	}
    	
    	if ( from_file_or_dir.isDirectory()){
    		
    		File[]	files = from_file_or_dir.listFiles();
    		
    		File	new_parent = new File( to_parent_dir, from_file_or_dir.getName());
    		
    		new_parent.mkdirs();
    		
    		for (int i=0;i<files.length;i++){
    			
    			File	from_file	= files[i];
    			
    			copyFileOrDirectory( from_file, new_parent );
    		}
    	}else{
    		
    		copyFile(  from_file_or_dir, new File( to_parent_dir, from_file_or_dir.getName()));
    	}
    }
    
    /**
     * Returns the file handle for the given filename or it's
     * equivalent .bak backup file if the original doesn't exist
     * or is 0-sized.  If neither the original nor the backup are
     * available, a null handle is returned.
     * @param _filename root name of file
     * @return file if successful, null if failed
     */
    public static File getFileOrBackup( final String _filename ) {
      try {
        File file = new File( _filename );
        //make sure the file exists and isn't zero-length
        if ( file.length() <= 1L ) {
          //if so, try using the backup file
          File bakfile = new File( _filename + ".bak" );
          if ( bakfile.length() <= 1L ) {
            return null;
          }
          else return bakfile;
        }
        else return file;
      }
      catch (Exception e) {
        Debug.out( e );
        return null;
      }
    }

    
    public static File
	getJarFileFromURL(
		String		url_str )
    {
    	if (url_str.startsWith("jar:file:")) {
        	
        	// java web start returns a url like "jar:file:c:/sdsd" which then fails as the file
        	// part doesn't start with a "/". Add it in!
    		// here's an example 
    		// jar:file:C:/Documents%20and%20Settings/stuff/.javaws/cache/http/Dparg.homeip.net/P9090/DMazureus-jnlp/DMlib/XMAzureus2.jar1070487037531!/org/gudy/azureus2/internat/MessagesBundle.properties
    			
        	// also on Mac we don't get the spaces escaped
        	
    		url_str = url_str.replaceAll(" ", "%20" );
        	
        	if ( !url_str.startsWith("jar:file:/")){
        		
       
        		url_str = "jar:file:/".concat(url_str.substring(9));
        	}
        	
        	try{
        			// 	you can see that the '!' must be present and that we can safely use the last occurrence of it
          	
        		int posPling = url_str.lastIndexOf('!');
            
        		String jarName = url_str.substring(4, posPling);
        		
        			//        System.out.println("jarName: " + jarName);
        		
        		URI uri = URI.create(jarName);
        		
        		File jar = new File(uri);
        		
        		return( jar );
        		
        	}catch( Throwable e ){
        	
        		Debug.printStackTrace( e );
        	}
    	}
    	
    	return( null );
    }

    public static boolean
	renameFile(
		File		from_file,
		File		to_file )
    {
        return renameFile(from_file, to_file, true);
    	}
    	

    public static boolean
    renameFile(
        File        from_file,
        File        to_file,
        boolean     fail_on_existing_directory)
    {
    	return renameFile(from_file, to_file, fail_on_existing_directory, null);
    }
    
    public static boolean renameFile(
    	File        from_file,
    	File        to_file,
    	boolean     fail_on_existing_directory,
    	FileFilter  file_filter
    ) {
    
    	if ( !from_file.exists()){
    		Logger
					.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
							"renameFile: source file '" + from_file
									+ "' doesn't exist, failing"));
    		
    		return( false );
    	}
    	
        /**
         * If the destination exists, we only fail if requested.
         */
        if (to_file.exists() && (fail_on_existing_directory || from_file.isFile() || to_file.isFile())) {
            Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
                    "renameFile: target file '" + to_file + "' already exists, failing"));

            return( false );
        }
    	File to_file_parent = to_file.getParentFile();
    	if (!to_file_parent.exists()) {to_file_parent.mkdirs();}
    	
    	if ( from_file.isDirectory()){
    		
    		File[] files = null;
    		if (file_filter != null) {files = from_file.listFiles(file_filter);}
    		else {files = from_file.listFiles();}
    		
    		if ( files == null ){
    			
    				// empty dir
    			
    			return( true );
    		}
    		
    		int	last_ok = 0;
    		
    		if (!to_file.exists()) {to_file.mkdir();}
    		
    		for (int i=0;i<files.length;i++){
    			
  				File	ff = files[i];
				File	tf = new File( to_file, ff.getName());

    			try{
     				if ( renameFile( ff, tf, fail_on_existing_directory, file_filter )){
    					
    					last_ok++;
    					
    				}else{
    					
    					break;
    				}
    			}catch( Throwable e ){
    				
    				Logger.log(new LogAlert(LogAlert.REPEATABLE,
							"renameFile: failed to rename file '" + ff.toString() + "' to '"
									+ tf.toString() + "'", e));

    				break;
    			}
    		}
    		
    		if ( last_ok == files.length ){
    			
    			File[]	remaining = from_file.listFiles();
    			
    			if ( remaining != null && remaining.length > 0 ){
    				// This might be important or not. We'll make it a debug message if we had a filter,
    				// or log it normally otherwise.
    				if (file_filter == null) {
    					Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
							"renameFile: files remain in '" + from_file.toString()
									+ "', not deleting"));
    				}
    				else {
    					/* Should we log this? How should we log this? */
    					return true;
    				}
   				 
    			}else{
    				
    				if ( !from_file.delete()){
    					Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
								"renameFile: failed to delete '" + from_file.toString() + "'"));
    				}
    			}
    			
    			return( true );
    		}
    		
    			// recover by moving files back
    		
      		for (int i=0;i<last_ok;i++){
        		
				File	ff = files[i];
				File	tf = new File( to_file, ff.getName());

    			try{
    				// null - We don't want to use the file filter, it only refers to source paths.
                    if ( !renameFile( tf, ff, false, null )){
    					Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
								"renameFile: recovery - failed to move file '" + tf.toString()
										+ "' to '" + ff.toString() + "'"));
    				}
    			}catch( Throwable e ){
    				Logger.log(new LogAlert(LogAlert.REPEATABLE,
							"renameFile: recovery - failed to move file '" + tf.toString()
									+ "' to '" + ff.toString() + "'", e));
   	   			    				
    			}
      		}
      		
      		return( false );
      		
    	}else{
			if ( 	(!COConfigurationManager.getBooleanParameter("Copy And Delete Data Rather Than Move")) &&
					from_file.renameTo( to_file )){
		  					
				return( true );
	
			}else{
				boolean		success	= false;
				
					// can't rename across file systems under Linux - try copy+delete
	
				FileInputStream		fis = null;
				
				FileOutputStream	fos = null;
				
				try{
					fis = new FileInputStream( from_file );
					
					fos = new FileOutputStream( to_file );
				
					byte[]	buffer = new byte[65536];
					
					while( true ){
						
						int	len = fis.read( buffer );
						
						if ( len <= 0 ){
							
							break;
						}
						
						fos.write( buffer, 0, len );
					}
					
					fos.close();
					
					fos	= null;
					
					fis.close();
					
					fis = null;
					
					if ( !from_file.delete()){
						Logger.log(new LogAlert(LogAlert.REPEATABLE,
								LogAlert.AT_ERROR, "renameFile: failed to delete '"
										+ from_file.toString() + "'"));
						
						throw( new Exception( "Failed to delete '" + from_file.toString() + "'"));
					}
					
					success	= true;
					
					return( true );
					
				}catch( Throwable e ){		
	
					Logger.log(new LogAlert(LogAlert.REPEATABLE,
							"renameFile: failed to rename '" + from_file.toString()
									+ "' to '" + to_file.toString() + "'", e));
					
					return( false );
					
				}finally{
					
					if ( fis != null ){
						
						try{
							fis.close();
							
						}catch( Throwable e ){
						}
					}
					
					if ( fos != null ){
						
						try{
							fos.close();
							
						}catch( Throwable e ){
						}
					}
					
						// if we've failed then tidy up any partial copy that has been performed
					
					if ( !success ){
						
						if ( to_file.exists()){
							
							to_file.delete();
						}
					}
				}
			}
    	}
    }
    
    
    
    public static void writeBytesAsFile( String filename, byte[] file_data ) {
      try{
        File file = new File( filename );
        
        FileOutputStream out = new FileOutputStream( file );
        
        out.write( file_data );
        
        out.close();
      }
      catch( Throwable t ) {
        Debug.out( "writeBytesAsFile:: error: ", t );
      }

    }
    
	public static boolean
	deleteWithRecycle(
		File		file )
	{
		if ( COConfigurationManager.getBooleanParameter("Move Deleted Data To Recycle Bin" )){
			
			try{
			    final PlatformManager	platform  = PlatformManagerFactory.getPlatformManager();
			    
			    if (platform.hasCapability(PlatformManagerCapabilities.RecoverableFileDelete)){
			    	
			    	platform.performRecoverableFileDelete( file.getAbsolutePath());
			    
			    	return( true );
			    	
			    }else{
			    	
			    	return( file.delete());
			    }
			}catch( PlatformManagerException e ){
				
				return( file.delete());
			}
		}else{
			
			return( file.delete());
		}
	}
	
	public static String translateMoveFilePath(String old_root, String new_root, String file_to_move) {
		if (!file_to_move.startsWith(old_root)) {return null;}
		String file_suffix = file_to_move.substring(old_root.length());
		if (new_root.endsWith(File.separator)) {new_root = new_root.substring(0, new_root.length()-1);}
		if (file_suffix.startsWith(File.separator)) {file_suffix = file_suffix.substring(1);}
		return new_root + File.separator + file_suffix;
	}
	
	public static void
	runAsTask(
		AzureusCoreOperationTask	task )
	{
		AzureusCore	core = AzureusCoreFactory.getSingleton();
		
		core.createOperation( AzureusCoreOperation.OP_FILE_MOVE, task );
	}
}
