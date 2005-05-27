/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#include "netcap_tcp.h"

#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#define __FAVOR_BSD
#include <netinet/tcp.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/hash.h>
#include <mvutil/unet.h>
#include <mvutil/mailbox.h>
#include <linux/netfilter_ipv4.h>
#include "libnetcap.h"
#include "netcap_hook.h"
#include "netcap_session.h"
#include "netcap_pkt.h"
#include "netcap_queue.h"
#include "netcap_globals.h"
#include "netcap_interface.h"
#include "netcap_sesstable.h"
#include "netcap_shield.h"
#include "netcap_icmp.h"

/* When completing to the server, this is essentially one plus number of ICMP messages to receive
 * before giving up.  On the last attempt, incoming ICMP messages are ignored */
#define TCP_SRV_COMPLETE_ATTEMPTS 3

/* Delay of the first connection attempt */
#define TCP_SRV_COMPLETE_TIMEOUT_SEC  30
#define TCP_SRV_COMPLETE_TIMEOUT_USEC  0

/* Delay after receiving at least one ICMP message before giving up. */
#define TCP_SRV_COMPLETE_IGN_TIMEOUT_SEC   2
#define TCP_SRV_COMPLETE_IGN_TIMEOUT_USEC  0

static int  _netcap_tcp_setsockopt_srv ( int sock );

static int _srv_complete_connection( netcap_session_t* netcap_sess, int flags );
static int _srv_start_connection( netcap_session_t* netcap_sess, struct sockaddr_in* dst_addr, int flags );

static int _icmp_mailbox_init    ( netcap_session_t* netcap_sess );
static int _icmp_mailbox_destroy ( netcap_session_t* netcap_sess );

int  _netcap_tcp_callback_srv_complete ( netcap_session_t* netcap_sess, netcap_callback_action_t action, 
                                         netcap_callback_flag_t flags )
{
    int ret = 0;

    switch( netcap_sess->srv_state ) {
    case CONN_STATE_INCOMPLETE:
        break;

    case CONN_STATE_COMPLETE:
        errlog( ERR_WARNING, "TCP: (%10u) SRV_COMPLETE %s connection already completed\n", 
                netcap_sess->session_id, netcap_session_srv_tuple_print( netcap_sess ));
        return 0;
        
    default:
        return errlog( ERR_WARNING, "TCP: (%10u) SRV_COMPLETE %s unknown state %d\n", 
                       netcap_sess->session_id, netcap_session_srv_tuple_print( netcap_sess ),
                       netcap_sess->srv_state );
    }
    
    ret = 0;
    
    /* Grab the session table lock */
    SESSTABLE_WRLOCK();
    ret = _icmp_mailbox_init( netcap_sess );
    SESSTABLE_UNLOCK();
    
    if ( ret < 0 ) return errlog( ERR_CRITICAL, "_icmp_mailbox_init\n" );    

    if ( _srv_complete_connection( netcap_sess, flags ) < 0 ) {
        ret = -1;
    }
    
    SESSTABLE_WRLOCK();
    if ( _icmp_mailbox_destroy( netcap_sess ) < 0 ) errlog( ERR_CRITICAL, "_destroy_icmp_mailbox\n" );
    SESSTABLE_UNLOCK();

    return ret;
}

static int  _netcap_tcp_setsockopt_srv ( int sock )
{
    int one        = 1;
    int thirty     = 30;
    int threehundo = 300;
    int twohours   = 7200;
    
    struct ip_sendnfmark_opts nfmark = {
        .on 1,
        .mark MARK_NOTRACK
    };
    
    if (setsockopt(sock,SOL_IP,IP_NONLOCAL,&one,sizeof(one))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_NODELAY,&one,sizeof(one))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_LINGER2,&thirty,sizeof(thirty))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_SOCKET,SO_KEEPALIVE,&one,sizeof(one))<0)
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPINTVL,&threehundo,sizeof(threehundo))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPIDLE,&twohours, sizeof(twohours)) < 0 )
        perrlog("setsockopt");

    if (setsockopt(sock,SOL_IP,IP_SENDNFMARK,&nfmark,sizeof(nfmark))<0)
        return perrlog( "setsockopt" );

    return 0;
}

static int _srv_complete_connection( netcap_session_t* netcap_sess, int flags )
{
    struct sockaddr_in dst_addr;
    int max;
    fd_set read_fds, write_fds;
    struct timeval tv;
    int ret = 0;
    int c;
    int mb_fd, sock;
    netcap_pkt_t* pkt = NULL;
    struct icmp* icmp_hdr = NULL;

    if ( _srv_start_connection( netcap_sess, &dst_addr, flags ) < 0 ) {
        netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_RESET;
        return errlog( ERR_CRITICAL, "_srv_start_connection\n" );
    }
    
    if (( mb_fd = mailbox_get_pollable_event( &netcap_sess->srv_mb )) < 0 ) {
        return errlog( ERR_CRITICAL, "mailbox_get_pollable_event\n" );
    }
    
    sock = netcap_sess->server_sock;
    
    debug( 8, "TCP: (%10u) Completing connection %i to %s\n", netcap_sess->session_id, sock );

    max = ( sock > mb_fd ) ? sock : mb_fd;
    max++;
    
    /* Number of attempts to complete the connection */
    for ( c = 0 ; c < TCP_SRV_COMPLETE_ATTEMPTS ; c++ ) {
        FD_ZERO( &read_fds );
        FD_ZERO( &write_fds );
        FD_SET( sock, &write_fds );

        if ( c == 0 ) {
            tv.tv_sec  = TCP_SRV_COMPLETE_TIMEOUT_SEC;
            tv.tv_usec = TCP_SRV_COMPLETE_TIMEOUT_USEC;
        } else {
            /* This is the timeout for when ICMP messages may come but the connection
             * !might! complete anyway(many implementations ignore ICMP messages).
             */
            tv.tv_sec  = TCP_SRV_COMPLETE_IGN_TIMEOUT_SEC;
            tv.tv_usec = TCP_SRV_COMPLETE_IGN_TIMEOUT_USEC;
        }
        
        /* Look for ICMP messages on the first few attempts, but on the last attempt
         * just try to connect. */
        if ( c < ( TCP_SRV_COMPLETE_ATTEMPTS - 1 ))  { 
            FD_SET( mb_fd, &read_fds );
        } else {
            max = sock + 1;
        }
        
        if (( ret = select( max, &read_fds, &write_fds, NULL, &tv )) < 0 ) {
            perror( "select" );
            break;
        } else if ( ret == 0 ) {
            if ( c == 0 ) {
                /* Connection timeout only if this is the first iteration. 
                   (Otherwise, could have been ICMP). */
                netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_DROP;
            }
            ret = -1;
            break;
        } else {
            if ( FD_ISSET( mb_fd, &read_fds )) {
                /* Attempt to read out the ICMP packet */
                /* A packet should definitely have been available */
                if (( pkt = mailbox_try_get( &netcap_sess->srv_mb )) == NULL ) {
                    ret = errlog( ERR_CRITICAL, "mailbox_try_get\n" );
                    break;
                }
                
                if (( pkt->data == NULL ) || ( pkt->data_len < ICMP_ADVLENMIN ) || 
                    ( pkt->proto != IPPROTO_ICMP )) {
                    netcap_pkt_raze( pkt );
                    pkt = NULL;
                    ret = errlog( ERR_CRITICAL, "Invalid ICMP packet\n" );
                    break;
                }
                
                icmp_hdr = (struct icmp*)pkt->data;
                
                if (( netcap_icmp_verify_type_and_code( icmp_hdr->icmp_type, icmp_hdr->icmp_code ) < 0 ) || 
                    ICMP_INFOTYPE( icmp_hdr->icmp_type )) {
                    /* Invalid ICMP type or code, just drop the packet */
                    netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_DROP;
                    netcap_pkt_raze( pkt );
                    icmp_hdr = NULL;
                    pkt = NULL;
                    continue;
                }

                /* XX May want to do some more validation here */
                if ( netcap_sess->dead_tcp.exit_type == TCP_CLI_DEAD_ICMP ) {
                    if (( netcap_sess->dead_tcp.type != icmp_hdr->icmp_type ) ||
                        ( netcap_sess->dead_tcp.code != icmp_hdr->icmp_code )) {
                        debug( 8, "TCP: (%10u) ICMP modification (%d/%d) -> (%d/%d)\n",
                               netcap_sess->dead_tcp.type, netcap_sess->dead_tcp.code,
                               icmp_hdr->icmp_type, icmp_hdr->icmp_code );
                    }
                }

                /* Check to see if this packet is from a different source address */
                if ( icmp_hdr->icmp_ip.ip_dst.s_addr != pkt->src.host.s_addr ) {
                    netcap_sess->dead_tcp.use_src = 1;
                    netcap_sess->dead_tcp.src     = pkt->src.host.s_addr;
                } else {
                    netcap_sess->dead_tcp.use_src = 0;
                    netcap_sess->dead_tcp.src     = (in_addr_t)0;
                }
                
                netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_ICMP;
                netcap_sess->dead_tcp.type = icmp_hdr->icmp_type;
                netcap_sess->dead_tcp.code = icmp_hdr->icmp_code;

                debug( 10, "TCP: (%10u) ICMP message type %d code %d\n", netcap_sess->session_id, 
                       netcap_sess->dead_tcp.type, netcap_sess->dead_tcp.code );
                
                if ( icmp_hdr->icmp_type == ICMP_REDIRECT ) {
                    debug( 10, "TCP: (%10u) ICMP message redirect: %s\n", netcap_sess->session_id, 
                           unet_next_inet_ntoa( icmp_hdr->icmp_gwaddr.s_addr ));
                    
                    netcap_sess->dead_tcp.redirect = icmp_hdr->icmp_gwaddr.s_addr;
                }
                
                netcap_pkt_raze( pkt );
                icmp_hdr = NULL;
                pkt = NULL;
            }
            
            if ( FD_ISSET( sock, &write_fds )) {
                /* Check if the connection was established */
                if ( connect( sock, (struct sockaddr*)&dst_addr, sizeof( dst_addr )) < 0 ) {
                    debug( 4, "TCP: (%10u) Server connection failed '%s'\n", netcap_sess->session_id, 
                           strerror( errno ));
                    ret = -1;
                    netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_RESET;
                    break;
                } else {
                    debug( 10, "TCP: (%10u) Server connection complete\n", netcap_sess->session_id );
                    /* Reenable blocking io */
                    if ( unet_blocking_enable( sock ) < 0 ) {
                        errlog( ERR_CRITICAL, "unet_blocking_enable\n" );
                    }
                    ret = 0;
                    break;
                }
            }
        }
    }
    
    if ( ret < 0 ) {
        netcap_shield_rep_add_srv_fail( netcap_sess->cli.cli.host.s_addr );
    } else {
        netcap_shield_rep_add_srv_conn( netcap_sess->cli.cli.host.s_addr );
    }
    
    return ret;
}

static int _srv_start_connection( netcap_session_t* netcap_sess, struct sockaddr_in* dst_addr, int flags )
{
    int newsocket;
    struct sockaddr_in src_addr;
    int ret = 0;
    netcap_endpoint_t* src;
    netcap_endpoint_t* dst;
        
    src = &netcap_sess->srv.cli;
    dst = &netcap_sess->srv.srv;
    
    if (( unet_sockaddr_in_init( &src_addr, src->host.s_addr, src->port ) < 0 ) ||
        ( unet_sockaddr_in_init(  dst_addr, dst->host.s_addr, dst->port ) < 0 )) {
        return errlog( ERR_CRITICAL, "unet_sockaddr_in_init\n" );
    }
        
    debug( 8, "TCP: (%10u) Completing connection to %s\n", netcap_sess->session_id,
           netcap_session_srv_endp_print( netcap_sess ));
    
    if (( newsocket = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) < 0 ) return perrlog("socket");

    if ( _netcap_tcp_setsockopt_srv( newsocket ) < 0 ) return perrlog("_netcap_tcp_setsockopt_srv");
    
    do {
        if ( flags & SRV_COMPLETE_NONLOCAL_BIND ) {
            debug( 8,"TCP: (%10u) Binding %i to %s:%i\n", netcap_sess->session_id, newsocket,
                   unet_inet_ntoa( src_addr.sin_addr.s_addr ), ntohs( src_addr.sin_port ));
            if ( bind( newsocket, (struct sockaddr*)&src_addr, sizeof(src_addr)) < 0 ) {
                ret = perrlog( "bind" ); 
                break;
            }
        }
        else {
            debug( 8, "TCP: (%10u) Skipping binding\n", netcap_sess->session_id );
        }
        
        /**
         * set non-blocking
         */
        if ( unet_blocking_disable( newsocket ) < 0 ) {
            ret = errlog( ERR_CRITICAL, "unet_blocking_disable\n" );
            break;
        }
        
        debug( 6, "TCP: (%10u) Connect %i to %s:%i\n", netcap_sess->session_id, newsocket,
               unet_inet_ntoa( dst_addr->sin_addr.s_addr ), ntohs( dst_addr->sin_port ));
    
        if ( connect( newsocket, (struct sockaddr*)dst_addr, sizeof(struct sockaddr_in)) < 0 ) {
            if ( errno != EINPROGRESS ) {
                ret = perrlog( "connect" );
                break;
            }
        }
    } while ( 0 );
    
    if ( ret < 0 ) { 
        if ( close( newsocket ) < 0 ) perrlog( "close" );
    } else {
        netcap_sess->server_sock = newsocket;
    }
    
    return ret;
}

static int _icmp_mailbox_init    ( netcap_session_t* netcap_sess )
{
    netcap_session_t* current_sess;
    
    netcap_endpoint_t* src;
    netcap_endpoint_t* dst;
    
    /* Insert the reverse session (matching an incoming packet from the server side ) */
    dst = &netcap_sess->srv.cli;
    src = &netcap_sess->srv.srv;

    /* Lookup the tuple */
    // First check to see if the session already exists.
    current_sess = netcap_nc_sesstable_get_tuple ( !NC_SESSTABLE_LOCK, IPPROTO_TCP,
                                                   src->host.s_addr, dst->host.s_addr,
                                                   src->port, dst->port, 0 );
    
    if ( current_sess == NULL ) {
        debug( 10, "TCP: (%10u) Creating server mailbox\n", netcap_sess->session_id );
                 
        /* Create the mailbox */
        if ( mailbox_init( &netcap_sess->srv_mb ) < 0 ) return errlog ( ERR_CRITICAL, "mailbox_init\n" );

        /* Insert the tuple */
        if ( netcap_nc_sesstable_add_tuple( !NC_SESSTABLE_LOCK, netcap_sess, IPPROTO_TCP,
                                            src->host.s_addr, dst->host.s_addr,
                                            src->port, dst->port, 0 ) < 0 ) {
            if ( mailbox_destroy( &netcap_sess->srv_mb ) < 0 ) errlog( ERR_CRITICAL, "mailbox_destroy\n" );
            return errlog( ERR_CRITICAL, "netcap_nc_sesstable_add_tuple\n" );
        }
    } else {
        return errlog( ERR_WARNING, "TCP: (%10u) Server TCP session already exists %10u\n", 
                       netcap_sess->session_id, current_sess->session_id );
    }

    return 0;
}

static int _icmp_mailbox_destroy ( netcap_session_t* netcap_sess )
{
    netcap_endpoint_t* src;
    netcap_endpoint_t* dst;
    
    netcap_pkt_t* pkt = NULL;
    int c;

    debug( 10, "TCP: (%10u) Removing tuples and destroying server mailbox\n", netcap_sess->session_id );
    
    /* Remove the reverse session (matching an incoming packet from the server side ) */
    dst = &netcap_sess->srv.cli;
    src = &netcap_sess->srv.srv;

    /* Remove the tuple */
    if ( netcap_sesstable_remove_tuple( !NC_SESSTABLE_LOCK, IPPROTO_TCP,
                                           src->host.s_addr, dst->host.s_addr,
                                           src->port, dst->port, 0 ) < 0 ) {
        errlog( ERR_WARNING, "netcap_nc_sesstable_remove_tuple\n" );
    }
    
    /* Empty the mailbox */
    for ( c = 0 ; ( pkt = (netcap_pkt_t*)mailbox_try_get( &netcap_sess->srv_mb )) != NULL ; c++ ) {
        netcap_pkt_raze( pkt );
    }
    
    debug( 10, "TCP: (%10u) Deleted %d packets from mailbox\n", netcap_sess->session_id, c );
    
    /* Destroy the mailbox */
    if ( mailbox_destroy( &netcap_sess->srv_mb ) < 0 ) {
        errlog( ERR_WARNING, "mailbox_destroy\n" );
    }
    
    return 0;
}

