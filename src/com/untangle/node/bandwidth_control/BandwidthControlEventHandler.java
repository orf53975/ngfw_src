/**
 * $Id$
 */
package com.untangle.node.bandwidth_control;

import org.apache.log4j.Logger;
import java.util.List;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

public class BandwidthControlEventHandler extends AbstractEventHandler
{
    private static final Logger logger = Logger.getLogger( BandwidthControlEventHandler.class );

    private static final int TCP_HEADER_SIZE_ESTIMATE = 32;
    private static final int IP_HEADER_SIZE = 20;
    private static final int UDP_HEADER_SIZE = 8;
    
    private final int MAX_CHUNK_COUNT = 10;
    
    private BandwidthControlApp node;

    public BandwidthControlEventHandler(BandwidthControlApp node)
    {
        super(node);

        this.node = node;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        _handleNewSessionRequest( sessionRequest, Protocol.TCP );
    }

    public void handleUDPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        _handleNewSessionRequest( sessionRequest, Protocol.UDP );
    }

    public void handleTCPComplete( NodeTCPSession session )
    {
        try {
            _handleSession( null, session, Protocol.TCP );
        }
        catch (Exception e) {
            logger.warn("Exception: ",e);
        }
    }

    public void handleUDPComplete( NodeUDPSession session )
    {
        try {
            _handleSession( null, session, Protocol.UDP );
        }
        catch (Exception e) {
            logger.warn("Exception: ",e);
        }
    }

    public void handleUDPClientPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        _handleSession( data, session, Protocol.UDP );
        session.sendServerPacket( data, header );
    }

    public void handleUDPServerPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        _handleSession( data, session, Protocol.UDP );
        session.sendClientPacket( data, header );
    }

    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        _handleSession( data, session, Protocol.TCP );
        session.sendDataToServer( data );
        return;
    }

    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        _handleSession( data, session, Protocol.TCP );
        session.sendDataToClient( data );
        return;
    }

    protected void reprioritizeHostSessions(InetAddress addr, String reason)
    {
        if ( addr == null )
            return;
        
        logger.info("Reprioritizing Sessions for " + addr.getHostAddress() + " because \"" + reason + "\"");

        for (NodeSession sess : this.node.liveNodeSessions()) {
            if (addr.equals(sess.getClientAddr()) || addr.equals(sess.getServerAddr())) {
                logger.debug( "Reevaluating NodeSession : " + sess.getProtocol() + " " +
                              sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                              sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());


                BandwidthControlRule rule = _findFirstMatch(sess, true);
        
                if (rule != null) {
                    try {
                        rule.getAction().apply( sess );
                    } catch (Exception e) {
                        logger.warn("Failed to reprioritize session: " + sess.getProtocol() + " " +
                                    sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                                    sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort(), e);
                    }
                }
            }
        }
    }

    private void _doQuotaAccounting( int chunkSize, NodeSession sess, Protocol protocol )
    {
        if (chunkSize == 0) 
            return; /* no data with this event. return */


        /**
         * Account for packet header overhead
         */
        if ( protocol.getId() == 6 ) {
            chunkSize = chunkSize + IP_HEADER_SIZE + TCP_HEADER_SIZE_ESTIMATE;
        }
        if ( protocol.getId() == 17 ) {
            chunkSize = chunkSize + IP_HEADER_SIZE + UDP_HEADER_SIZE;
        }
        
        /**
         * Do quota accounting
         */
        if (UvmContextFactory.context().hostTable().decrementQuota(sess.getClientAddr(),chunkSize)) {
            this.node.incrementMetric( BandwidthControlApp.STAT_QUOTA_EXCEEDED );
            reprioritizeHostSessions(sess.getClientAddr(),"quota exceeded");
        }
        if (UvmContextFactory.context().hostTable().decrementQuota(sess.getServerAddr(),chunkSize)) {
            this.node.incrementMetric( BandwidthControlApp.STAT_QUOTA_EXCEEDED );
            reprioritizeHostSessions(sess.getServerAddr(),"quota exceeded");
        }
    }

    private void _handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
        logger.debug( "New NodeSession Request: " + protocol + " " +
                     request.getOrigClientAddr().getHostAddress() + ":" + request.getOrigClientPort() + " -> " +
                     request.getNewServerAddr().getHostAddress() + ":" + request.getNewServerPort());

        BandwidthControlSessionState sessInfo = new BandwidthControlSessionState();
        request.attach(sessInfo);
    }

    private void _handleSession( ByteBuffer data, NodeSession sess, Protocol protocol )
    {
        BandwidthControlSessionState sessInfo = (BandwidthControlSessionState)sess.attachment();
        if (sessInfo == null) {
            sessInfo = new BandwidthControlSessionState();
            sess.attach(sessInfo);
        }

        if (! this.node.isLicenseValid()) 
            return;
        
        if ( data != null )
            _doQuotaAccounting( data.remaining(), sess, protocol );
        
        /**
         * If we are too deep in the session - stop running the rules
         */
        sessInfo.chunkCount++;
        if (sessInfo.chunkCount > MAX_CHUNK_COUNT) {
            sess.release();
            return;
        }
        
        logger.debug( "Session Event  : " + protocol + " " +
                     sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                     sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());

        /**
         * Check for a matching rule and apply it
         */
        BandwidthControlRule rule = _findFirstMatch(sess);
        
        if (rule != null)
            rule.getAction().apply( sess );

    }
    
    private BandwidthControlRule _findFirstMatch(NodeSession sess)
    {
        return _findFirstMatch(sess, false);
    }
    
    private BandwidthControlRule _findFirstMatch(NodeSession sess, boolean onlyPrioritizeRules)
    {
        List<BandwidthControlRule> rules = this.node.getRules();

        logger.debug( "Checking Rules against NodeSession : " + sess.getProtocol() + " " +
                      sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                      sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());

        for ( BandwidthControlRule rule : rules ) {
            boolean evalRule = true;

            if (onlyPrioritizeRules) {
                /**
                 * Only check rules that set priority for reprioritization if allRules is false
                 * This is used for reprioritizing sessions and we only want to match certain rules
                 */
                evalRule = ( rule.getAction().getActionType() == BandwidthControlRuleAction.ActionType.SET_PRIORITY ||
                             rule.getAction().getActionType() == BandwidthControlRuleAction.ActionType.APPLY_PENALTY_PRIORITY );
            }
            
            if (rule.getEnabled() && evalRule && rule.matches( sess )) {
                logger.debug( "Matched NodeSession : " + sess.getProtocol() + " " +
                              sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                              sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort() + " matches " + rule.getDescription());
                return rule; /* check no further */
            } else {
                logger.debug( "Checking Rule \"" + rule.getDescription() + "\" against NodeSession : " + sess.getProtocol() + " " +
                              sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                              sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());
            }
        }

        return null;
    }

}
