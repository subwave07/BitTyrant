/*
 * Created on Apr 26, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.peerdb;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.gudy.azureus2.core3.peer.PEPeerSource;



/**
 * Represents a peer item, unique by ip address + port combo.
 * NOTE: Overrides equals().
 */
public class PeerItem {
  private final byte[] address;
  private final short udp_port;
  private final short tcp_port;
  private final byte source;
  private final int hashcode;
  private final byte handshake;
  
  protected PeerItem( String _address, int _tcp_port, byte _source, byte _handshake, int _udp_port ) {
    byte[] raw;
    try{
      //see if we can resolve the address into a compact raw IPv4/6 byte array (4 or 16 bytes)
      InetAddress ip = InetAddress.getByName( _address );
      raw = ip.getAddress();
    }
    catch( UnknownHostException e ) {
      //not a standard IPv4/6 address, so just use the full string bytes
      raw = _address.getBytes();
    }

    address = raw;
    tcp_port = (short)_tcp_port;
    udp_port = (short)_udp_port;
    source = _source;
    hashcode = new String( address ).hashCode() + tcp_port;
    handshake = _handshake;
    
    if( address.length != 4 ) {
      System.out.println( "PeerItem OUT: address byte size=" +address.length);
    }
  }
  

  protected PeerItem( byte[] _serialization, byte _source, byte _handshake, int _udp_port ) throws Exception{
	if ( _serialization.length < 6 || _serialization.length > 32){
		throw( new Exception( "PeerItem: invalid serialisation length - " + _serialization.length ));
	}
    //extract address and port
    address = new byte[ _serialization.length -2 ];
    System.arraycopy( _serialization, 0, address, 0, _serialization.length -2 );
    
    byte p0 = _serialization[ _serialization.length -2 ];
    byte p1 = _serialization[ _serialization.length -1 ];  
    tcp_port = (short)((p1 & 0xFF) + ((p0 & 0xFF) << 8));
    
    source = _source;
    hashcode = new String( address ).hashCode() + tcp_port;
    handshake = _handshake;
    udp_port = (short)_udp_port;
    
    if( address.length > 15 ) {
      System.out.println( "PeerItem IN: address byte size=" +address.length);
    }
  }
    
  
  
  public byte[] getSerialization() {
    //combine address and port bytes into one
    byte[] full_address = new byte[ address.length +2 ];
    System.arraycopy( address, 0, full_address, 0, address.length );
    full_address[ address.length ] = (byte)(tcp_port >> 8);
    full_address[ address.length +1 ] = (byte)(tcp_port & 0xff);
    return full_address;
  }
  
  
  public String getAddressString() {
    try{
      //see if it's an IPv4/6 address (4 or 16 bytes)
      return InetAddress.getByAddress( address ).getHostAddress();
    }
    catch( UnknownHostException e ) {
      //not a standard IPv4/6 address, so just return as full string
      return new String( address );
    }
  }
  
  
  public int getTCPPort() {  return tcp_port&0xffff;  }
  
  public int getUDPPort() {  return udp_port&0xffff;  }
   
  public byte getSource() {  return source;  }

  public byte getHandshakeType() {  return handshake;  }
  

  public boolean equals( Object obj ) {
    if( this == obj )  return true;
    if( obj != null && obj instanceof PeerItem ) {
      PeerItem other = (PeerItem)obj;
      if( 	this.tcp_port == other.tcp_port &&
    		this.udp_port == other.udp_port &&
    		this.handshake == other.handshake &&
    		Arrays.equals( this.address, other.address ) )  return true;
    }
    return false;
  }
  
  public int hashCode() {  return hashcode;  }
  
  
  
  public static String convertSourceString( byte source_id ) {
    //we use an int to store the source text string as this class is supposed to be lightweight
    switch( source_id ) {
      case PeerItemFactory.PEER_SOURCE_TRACKER:        return PEPeerSource.PS_BT_TRACKER;
      case PeerItemFactory.PEER_SOURCE_DHT:            return PEPeerSource.PS_DHT;
      case PeerItemFactory.PEER_SOURCE_PEER_EXCHANGE:  return PEPeerSource.PS_OTHER_PEER;
      case PeerItemFactory.PEER_SOURCE_PLUGIN:         return PEPeerSource.PS_PLUGIN;
      case PeerItemFactory.PEER_SOURCE_INCOMING:       return PEPeerSource.PS_INCOMING;
      default:                                         return "<unknown>";
    }
  }
  
  
  public static byte convertSourceID( String source ) {
    if( source.equals( PEPeerSource.PS_BT_TRACKER ) )  return PeerItemFactory.PEER_SOURCE_TRACKER;
    if( source.equals( PEPeerSource.PS_DHT ) )         return PeerItemFactory.PEER_SOURCE_DHT;
    if( source.equals( PEPeerSource.PS_OTHER_PEER ) )  return PeerItemFactory.PEER_SOURCE_PEER_EXCHANGE;
    if( source.equals( PEPeerSource.PS_PLUGIN ) )      return PeerItemFactory.PEER_SOURCE_PLUGIN;
    if( source.equals( PEPeerSource.PS_INCOMING ) )    return PeerItemFactory.PEER_SOURCE_INCOMING;
    return -1;
  }

}
