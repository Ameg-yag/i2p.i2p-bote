/**
 * Copyright (C) 2009  HungryHobo@mail.i2p
 * 
 * The GPG fingerprint for HungryHobo@mail.i2p is:
 * 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 * 
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.packet;

import i2p.bote.EmailDestination;
import i2p.bote.EmailIdentity;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import net.i2p.I2PAppContext;
import net.i2p.crypto.ElGamalAESEngine;
import net.i2p.crypto.SessionKeyManager;
import net.i2p.data.DataFormatException;
import net.i2p.data.Hash;
import net.i2p.data.PrivateKey;
import net.i2p.data.PublicKey;
import net.i2p.data.SessionKey;
import net.i2p.util.Log;
import net.i2p.util.RandomSource;

@TypeCode('E')
public class EncryptedEmailPacket extends DhtStorablePacket {
    private static final int PADDED_SIZE = PrivateKey.KEYSIZE_BYTES;   // TODO is this a good choice?
    
    private Log log = new Log(EncryptedEmailPacket.class);
    private ElGamalAESEngine cryptoEngine = I2PAppContext.getGlobalContext().elGamalAESEngine();
    private SessionKeyManager sessionKeyManager = I2PAppContext.getGlobalContext().sessionKeyManager();
    private Hash dhtKey;
    private UniqueId deletionKeyPlain;
    private byte[] encryptedData;   // the encrypted fields of an I2PBote email packet: Encrypted Deletion Key, Message ID, Fragment Index, Number of Fragments, and Content.

	/**
	 * Creates an <code>EncryptedEmailPacket</code> from raw datagram data.
	 * To read the encrypted parts of the packet, <code>decrypt</code> must be called first.
     * @param data
	 */
	public EncryptedEmailPacket(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        if (buffer.get() != getPacketTypeCode())
            log.error("Wrong type code for EncryptedEmailPacket. Expected <" + getPacketTypeCode() + ">, got <" + (char)data[0] + ">");
        
        dhtKey = readHash(buffer);
        deletionKeyPlain = new UniqueId(buffer);
        
        int encryptedLength = buffer.getShort();   // length of the encrypted part of the packet
        encryptedData = new byte[encryptedLength];
        buffer.get(encryptedData);
	}
	
	/**
	 * Creates an <code>EncryptedEmailPacket</code> from an <code>UnencryptedEmailPacket</code>.
	 * The public key of <code>emailDestination</code> is used for encryption.
	 * @param unencryptedPacket
	 * @param emailDestination
	 */
	public EncryptedEmailPacket(UnencryptedEmailPacket unencryptedPacket, EmailDestination emailDestination) {
        dhtKey = generateRandomHash();
        deletionKeyPlain = unencryptedPacket.getPlaintextDeletionKey();
        
        // put all the encrypted fields into an array
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        try {
            unencryptedPacket.getVerificationDeletionKey().writeTo(dataStream);
            unencryptedPacket.getMessageId().writeTo(dataStream);
            dataStream.writeShort(unencryptedPacket.getFragmentIndex());
            dataStream.writeShort(unencryptedPacket.getNumFragments());
            
            byte[] content = unencryptedPacket.getContent();
            dataStream.writeShort(content.length);
            dataStream.write(content);
            
            encryptedData = encrypt(byteStream.toByteArray(), emailDestination);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream/DataOutputStream.", e);
        }
	}
	
	private Hash generateRandomHash() {
	    RandomSource randomSource = RandomSource.getInstance();
	    
	    byte[] bytes = new byte[Hash.HASH_LENGTH];
	    for (int i=0; i<bytes.length; i++)
	        bytes[i] = (byte)randomSource.nextInt(256);
	    
	    return new Hash(bytes);
	}
    
	/**
	 * Decrypts the encrypted part of the packet with the private key of <code>identity</code>.
	 * @param identity
	 * @throws DataFormatException 
	 */
	public UnencryptedEmailPacket decrypt(EmailIdentity identity) throws DataFormatException {
	    byte[] decryptedData = decrypt(encryptedData, identity);
	    ByteBuffer buffer = ByteBuffer.wrap(decryptedData);
	    
	    UniqueId deletionKeyVerify = new UniqueId(buffer);
	    UniqueId messageId = new UniqueId(buffer);
        int fragmentIndex = buffer.getShort();
        int numFragments = buffer.getShort();
        
        int contentLength = buffer.getShort();
        byte[] content = new byte[contentLength];
        buffer.get(content);
        
	    return new UnencryptedEmailPacket(deletionKeyPlain, deletionKeyVerify, messageId, fragmentIndex, numFragments, content);
	}
	
    /**
     * Decrypts data with an email identity's private key.
     * @param data
     * @param identity
     * @return The decrypted data
     */
	private byte[] decrypt(byte[] data, EmailIdentity identity) throws DataFormatException {
        PrivateKey privateKey = identity.getPrivateEncryptionKey();
        return cryptoEngine.decrypt(data, privateKey);
	}
	
    /**
     * Encrypts data with an email destination's public key.
     * @param data
     * @param emailDestination
     * @return The encrypted data
     */
    public byte[] encrypt(byte[] data, EmailDestination emailDestination) {
        PublicKey publicKey = emailDestination.getPublicEncryptionKey();
        SessionKey sessionKey = sessionKeyManager.createSession(publicKey);
        return cryptoEngine.encrypt(data, publicKey, sessionKey, PADDED_SIZE);
    }
    
    public static Collection<EncryptedEmailPacket> encrypt(Collection<UnencryptedEmailPacket> packets, EmailDestination destination) throws DataFormatException {
    	Collection<EncryptedEmailPacket> encryptedPackets = new ArrayList<EncryptedEmailPacket>();
    	for (UnencryptedEmailPacket unencryptedPacket: packets)
    		encryptedPackets.add(new EncryptedEmailPacket(unencryptedPacket, destination));
    	return encryptedPackets;
    }
    
    @Override
    public Hash getDhtKey() {
        return dhtKey;
    }
	
    /**
     * Returns the value of the "plain-text deletion key" field.
     * Storage nodes set this to all zero bytes when the packet is retrieved.
     * @return
     */
    public UniqueId getPlaintextDeletionKey() {
        return deletionKeyPlain;
    }
    
	@Override
	public byte[] toByteArray() {
	    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
	    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);

        try {
            dataStream.write((byte)getPacketTypeCode());
            dataStream.write(dhtKey.toByteArray());
            dataStream.write(deletionKeyPlain.toByteArray());
            dataStream.writeShort(encryptedData.length);
            dataStream.write(encryptedData);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
		return byteArrayStream.toByteArray();
	}

	// Returns the number of bytes in the packet.
	// TODO just return content.length+CONST so we don't call toByteArray every time
	public int getSize() {
	    return toByteArray().length;
	}
	
    @Override
    public String toString() {
    	return super.toString() + ", DHTkey=" + dhtKey + ", delKeyPln=" + deletionKeyPlain + ", encrLen=" + encryptedData.length;
    }
}