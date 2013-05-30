/**
 * $Id: SkinManagerImpl.java,v 1.00 2012/06/05 15:41:04 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Iterator;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SkinManager;
import com.untangle.uvm.SkinInfo;
import com.untangle.uvm.SkinSettings;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.node.License;
import com.untangle.uvm.servlet.UploadHandler;

/**
 * Implementation of SkinManager.
 */
public class SkinManagerImpl implements SkinManager
{
    private static final String SKINS_DIR = System.getProperty("uvm.skins.dir");;
    private static final String DEFAULT_SKIN = "default";
    private static final String DEFAULT_ADMIN_SKIN = DEFAULT_SKIN;
    private static final String DEFAULT_USER_SKIN = DEFAULT_SKIN;
    private static final int BUFFER = 2048; 

    private final Logger logger = Logger.getLogger(getClass());

    private SkinSettings settings;

    public SkinManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        SkinSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "skin";
    	
        try {
            readSettings = settingsManager.load( SkinSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            SkinSettings skinSettings = new SkinSettings();
            skinSettings.setSkinName(DEFAULT_ADMIN_SKIN);

            this.setSettings(skinSettings);
        }
        else {
            this.settings = readSettings;
            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }


        /**
         * If the skin is out of date, revert to default
         */
        String skin = this.settings.getSkinName();
        File skinXML = new File( SKINS_DIR + File.separator + skin + File.separator + "skin.xml" );
        SkinInfo skinInfo = getSkinInfo( skinXML );
        if ( skinInfo == null || skinInfo.isAdminSkinOutOfDate() ) {
            this.settings.setSkinName( DEFAULT_ADMIN_SKIN );
            this.setSettings( this.settings );
        }

        this.reconfigure();
    }

    // public methods ---------------------------------------------------------

    public SkinSettings getSettings()
    {
        return settings;
    }

    public void setSettings(SkinSettings newSettings)
    {
        this._setSettings( newSettings );
    }
	
    public void uploadSkin(FileItem item) throws UvmException
    {
        try {
            BufferedOutputStream dest = null;
            ZipEntry entry = null;
            File defaultSkinDir = new File(SKINS_DIR + File.separator + DEFAULT_SKIN);
            File skinDir = new File(SKINS_DIR);
            List<File> processedSkinFolders = new ArrayList<File>();
			
            //validate skin
            if (!item.getName().endsWith(".zip")) {
                throw new UvmException("Invalid Skin");
            }
			
            // Open the ZIP file
            InputStream uploadedStream = item.getInputStream();
            ZipInputStream zis = new ZipInputStream(uploadedStream);
            while ((entry = zis.getNextEntry()) != null) {
                //validate default skin
                String tokens[] = entry.getName().split(File.separator);
                if (tokens.length >= 1) {
                    File dir = new File(SKINS_DIR + File.separator + tokens[0]);
                    if (dir.equals(defaultSkinDir)) {
                        throw new UvmException("The default skin can not be overwritten");
                    }
                }
			    
                if (entry.isDirectory()) {
                    File dir = new File(SKINS_DIR + File.separator + entry.getName());
                    processSkinFolder(dir, processedSkinFolders);
                } else {
                    File file = new File(SKINS_DIR + File.separator + entry.getName());
                    File parentDir = file.getParentFile();
                    if (parentDir.equals(skinDir)) {
                        // invalid entry; skip it
                        continue;
                    } else {
                        processSkinFolder(parentDir, processedSkinFolders);
                    }
                    
                    int count;
                    byte data[] = new byte[BUFFER];
                    // write the files to the disk
                    FileOutputStream fos = new FileOutputStream(SKINS_DIR + File.separator + entry.getName());
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    if (entry.getName().contains("skin.xml")) {
                        File skinXML = new File( SKINS_DIR + File.separator + entry.getName() );
                        SkinInfo skinInfo = getSkinInfo( skinXML );
                        if ( skinInfo == null || skinInfo.isAdminSkinOutOfDate() ) {
                            logger.error("Upload Skin Failed, Out of Date");
                            throw new UvmException("Upload Skin Failed, Out of Date");
                        }
                    }
                }
            }
            zis.close();		    	
            uploadedStream.close();
        } catch (IOException e) {
            logger.error(e);
            throw new UvmException("Upload Skin Failed");
        }
    }

    public List<SkinInfo> getSkinsList( )
    {
    	
    	List<SkinInfo> skins = new ArrayList<SkinInfo>();
    	File dir = new File(SKINS_DIR);
    	
        File[] children = dir.listFiles();
        if (children == null) {
            logger.warn("Skin dir \""+SKINS_DIR+"\" does not exist");
        } else {
            for (int i=0; i<children.length; i++) {
                File file = children[i];
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    File[] skinFiles = file.listFiles(new FilenameFilter(){
                            public boolean accept(File dir, String name) {
                                return name.equals("skin.xml");
                            }
                        });
                    if (skinFiles.length < 1) {
                    	logger.warn("Skin folder \""+file.getName()+"\" does not have skin info file - skin.xml");
                    } else {
                    	SkinInfo skinInfo;
                        skinInfo = getSkinInfo( skinFiles[0] );
                        if (skinInfo != null) {
                            skins.add(skinInfo);
                        }
                    }
                }
            }
        }    	
        return skins;    	
    }

    public SkinInfo getSkinInfo( File skinXML  )
    {
        XStream xstream = new XStream();
        xstream.alias("skin", SkinInfo.class);
        SkinInfo skinInfo;
        try {
            skinInfo = (SkinInfo)xstream.fromXML(new FileInputStream(skinXML));
            if(!skinInfo.isAdminSkinOutOfDate()) {
                return skinInfo;
            } 
        } catch (FileNotFoundException e) {
            logger.error("Error reading skin info from skin foder \"" + skinXML.getName() + "\"");
        }
        return null;
    }
    
    // private methods --------------------------------------------------------

    private void _setSettings( SkinSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(SkinSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "skin", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.reconfigure();
    }
    
    private void reconfigure() 
    {
        /* Register a handler to upload skins */
        UvmContextImpl.context().uploadManager().registerHandler(new SkinUploadHandler());
    }
    
    private void processSkinFolder(File dir, List<File> processedSkinFolders)
        throws IOException, UvmException
    {
        if (processedSkinFolders.contains(dir)){
            return;
        }
        if (dir.exists()) {
            FileUtils.cleanDirectory(dir);
        } else {
            if (!dir.mkdirs()) {
                logger.error("Error creating skin folder: " + dir );
                throw new UvmException("Error creating skin folder");
            }
        }
        processedSkinFolders.add(dir);
    }
        
    private class SkinUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "skin";
        }
        
        @Override
        public String handleFile(FileItem fileItem, String argument) throws Exception
        {
            uploadSkin(fileItem);
            return "Successfully updated a skin";
        }
    }
}
