/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn.gui;

import com.metavize.mvvm.security.*;
import com.metavize.gui.widgets.wizard.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.Util;
import javax.swing.*;
import java.awt.Dialog;


import java.util.*;

import com.metavize.tran.openvpn.*;
import com.metavize.mvvm.tran.*;

public class ServerRoutingWizardSitesJPanel extends MWizardPageJPanel {


    private VpnTransform vpnTransform;
	
    public ServerRoutingWizardSitesJPanel(VpnTransform vpnTransform) {
	this.vpnTransform = vpnTransform;
	initComponents();
	((MEditTableJPanel)configSiteToSiteJPanel).setShowDetailJPanelEnabled(false);
	((MEditTableJPanel)configSiteToSiteJPanel).setInstantRemove(true);
	((MEditTableJPanel)configSiteToSiteJPanel).setFillJButtonEnabled(false);
    }

    protected boolean enteringForwards(){
	try{
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run() {
		try{
		    GroupList groupList = vpnTransform.getAddressGroups();
		    ((TableModelSiteToSite)((MEditTableJPanel)configSiteToSiteJPanel).getTableModel()).updateGroupModel( (List<VpnGroup>) groupList.getGroupList() );
		    ((MEditTableJPanel)configSiteToSiteJPanel).getTableModel().clearAllRows();
		}
		catch(Exception e){ Util.handleExceptionNoRestart("Error updating group list", e);}
	    }});
	}
	catch(Exception e){ Util.handleExceptionNoRestart("Error updating group list", e);}
	return true;
    }
    
    Vector<Vector> filteredDataVector;
    List<VpnSite> elemList;
    Exception exception;
	MProgressJDialog mProgressJDialog;
    JProgressBar jProgressBar;
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {

	SwingUtilities.invokeAndWait( new Runnable(){ public void run() {
	    ((MEditTableJPanel)configSiteToSiteJPanel).getJTable().getCellEditor().stopCellEditing();
	    ((MEditTableJPanel)configSiteToSiteJPanel).getJTable().clearSelection();
	    filteredDataVector = ((MEditTableJPanel)configSiteToSiteJPanel).getTableModel().getFilteredDataVector();
	    
	    exception = null;

	    elemList = new ArrayList<VpnSite>(filteredDataVector.size());
	    VpnSite newElem = null;
	    int rowIndex = 0;

	    for( Vector rowVector : filteredDataVector ){
		rowIndex++;
		newElem = new VpnSite();
		newElem.setDistributeClient(false);
		newElem.setLive( (Boolean) rowVector.elementAt(2) );
                newElem.setIsEdgeGuard( (Boolean) rowVector.elementAt(3) );
		newElem.setName( (String) rowVector.elementAt(4) );
		newElem.setGroup( (VpnGroup) ((ComboBoxModel) rowVector.elementAt(5)).getSelectedItem() );
		IPaddr network;
		try{ network = IPaddr.parse((String) rowVector.elementAt(6)); }
		catch(Exception e){ exception = new Exception("Invalid \"network address\" in row: " + rowIndex); return; }
		IPaddr netmask;
		try{ netmask = IPaddr.parse((String) rowVector.elementAt(7)); }
		catch(Exception e){ exception = new Exception("Invalid \"network netmask\" in row: " + rowIndex); return; }
		newElem.setSiteNetwork(network, netmask);
		newElem.setDescription( (String) rowVector.elementAt(9) );
		elemList.add(newElem);
	    }       		
	}});

        if( exception != null)
            throw exception;
	        
        if( !validateOnly ){
	    SiteList siteList = new SiteList(elemList);
	    vpnTransform.setSites(siteList);
		
		// BRING UP SAVING DIALOG
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    mProgressJDialog = new MProgressJDialog("Saving Configuration",
							    "<html><center>Please wait a moment while your configuration is being saved." + 
							    "<br>This may take up to one minute.</center></html>",
							    (Dialog)ServerRoutingWizardSitesJPanel.this.getTopLevelAncestor(), false);
		    jProgressBar = mProgressJDialog.getJProgressBar();
		    jProgressBar.setValue(0);
		    jProgressBar.setString("Saving...");
		    jProgressBar.setIndeterminate(true);
		    mProgressJDialog.setVisible(true);
		}});
		try{
		    vpnTransform.completeConfig();

		    // SHOW RESULTS AND REMOVE SAVING DIALOG
		    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
			jProgressBar.setValue(100);
			jProgressBar.setString("Finished Saving");
			jProgressBar.setIndeterminate(false);
		    }});
		    try{Thread.currentThread().sleep(2000);} catch(Exception e){e.printStackTrace();}
		    SwingUtilities.invokeLater( new Runnable(){ public void run(){
			mProgressJDialog.setVisible(false);
		    }});
		    
		}
		catch(Exception e){
		    SwingUtilities.invokeLater( new Runnable(){ public void run(){
			mProgressJDialog.setVisible(false);
		    }});
		    throw new Exception("Your VPN Routing Server configuration could not be saved.  Please try again.");
		}
        }
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                jLabel2 = new javax.swing.JLabel();
                configSiteToSiteJPanel = new ConfigSiteToSiteJPanel();
                jLabel3 = new javax.swing.JLabel();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html><b>Optionally add VPN Sites.</b><br>VPN Sites are remote networks that can access any exported hosts and networks on the VPN, and visa versa.</html>");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 410, -1));

                add(configSiteToSiteJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 90, 465, 210));

                jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/tran/openvpn/gui/ProductShot.png")));
                jLabel3.setEnabled(false);
                add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

        }//GEN-END:initComponents
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel configSiteToSiteJPanel;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        // End of variables declaration//GEN-END:variables
    
}
