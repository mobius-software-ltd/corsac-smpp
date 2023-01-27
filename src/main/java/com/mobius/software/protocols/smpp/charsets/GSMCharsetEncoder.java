package com.mobius.software.protocols.smpp.charsets;
/*
 * Mobius Software LTD
 * Copyright 2019 - 2023, Mobius Software LTD and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.BitSet;

public class GSMCharsetEncoder extends CharsetEncoder {

    private int bitpos = 0;
    private int carryOver;
    private GSMCharset cs;
    private GSMCharsetEncodingData encodingData;

    // The mask to check if corresponding bit in read byte is 1 or 0 and hence
    // store it i BitSet accordingly
    byte[] mask = new byte[] { 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40 };

    // BitSet to hold the bits of passed char to be encoded
    BitSet bitSet = new BitSet();

    static final byte ESCAPE = 0x1B;

    protected GSMCharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar) {
        super(cs, averageBytesPerChar, maxBytesPerChar);
        implReset();
        this.cs = (GSMCharset) cs;
    }

    public void setGSMCharsetEncodingData(GSMCharsetEncodingData encodingData) {
        this.encodingData = encodingData;
    }

    public GSMCharsetEncodingData getGSMCharsetEncodingData() {
        return this.encodingData;
    }

    @Override
    protected void implReset() {
        bitpos = 0;
        carryOver = 0;
        bitSet.clear();

        if (encodingData != null) {
            encodingData.totalSeptetCount = 0;
            encodingData.leadingBufferIsEncoded = false;
        }
    }

    /**
     *
     */
    @Override
    protected CoderResult implFlush(ByteBuffer out) {

        if (!out.hasRemaining()) {
            return CoderResult.OVERFLOW;
        }
        return CoderResult.UNDERFLOW;
    }

    byte rawData = 0;

    @Override
    protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {

        if (this.encodingData != null && this.encodingData.leadingBuffer != null && !this.encodingData.leadingBufferIsEncoded) {
            if (out.limit() - out.position() >= this.encodingData.leadingBuffer.length) {
                if (this.encodingData.encodingStyle != Gsm7EncodingStyle.bit8_smpp_style) {
                    int septetCount = (this.encodingData.leadingBuffer.length * 8 + 6) / 7;
                    bitpos = septetCount % 8;
                    this.encodingData.totalSeptetCount = septetCount;
                }
                for (int ind = 0; ind < this.encodingData.leadingBuffer.length; ind++) {
                    out.put(this.encodingData.leadingBuffer[ind]);
                }
                this.encodingData.leadingBufferIsEncoded = true;
            } else {
                // not enough size in the target buffer - return CoderResult.OVERFLOW for out buffer encreasing
                return CoderResult.OVERFLOW;
            }
        }

        Integer lastChar = (int)' ';
        while (in.hasRemaining()) {
            if (out.limit() - out.position() < 3) {
                // not enough size in the target buffer - return CoderResult.OVERFLOW for out buffer encreasing
                return CoderResult.OVERFLOW;
            }

            // Read the first char
            Integer c = (int)in.get();
            lastChar = c;

            Integer value=this.cs.reverseMainTable.get(c);
            if(value!=null)
                this.putByte(value, out);                
            
            if(value==null)
            {
            	value=this.cs.reverseExtensionTable.get(c);
            	if(value!=null)
            	{
            		this.putByte(GSMCharsetEncoder.ESCAPE, out);
            		this.putByte(value, out);                
            	}
            }
            
            if (value==null) 
            {
                // found no suitable symbol - encode a space char
                this.putByte(0x20, out);
            }
        }

        if (out.limit() - out.position() < 1) {
            // not enough size in the target buffer - return CoderResult.OVERFLOW for out buffer encreasing
            return CoderResult.OVERFLOW;
        }

        if (this.encodingData == null || this.encodingData.encodingStyle != Gsm7EncodingStyle.bit8_smpp_style) {
            if (bitpos != 0) {
                // USSD: replace 7-bit pad with <CR>
                if (this.encodingData != null && this.encodingData.encodingStyle == Gsm7EncodingStyle.bit7_ussd_style
                        && bitpos == 7)
                    carryOver |= 0x1A;

                // writing a carryOver data
                out.put((byte) carryOver);
            } else {
                // USSD: adding extra <CR> if the last symbol is <CR> and no padding
                if (this.encodingData != null && this.encodingData.encodingStyle == Gsm7EncodingStyle.bit7_ussd_style
                        && lastChar == (int)'\r')
                    out.put((byte) 0x0D);
            }
        }

        return CoderResult.UNDERFLOW;
    }

    private void putByte(int data, ByteBuffer out) {
        if (this.encodingData != null && this.encodingData.encodingStyle == Gsm7EncodingStyle.bit8_smpp_style) {
            out.put((byte) data);
        } else {

            if (bitpos == 0) {
                carryOver = data;
            } else {
                int i1 = data << (8 - bitpos);
                out.put((byte) (i1 | carryOver));
                carryOver = data >>> bitpos;
            }

            bitpos++;
            if (bitpos == 8) {
                bitpos = 0;
            }

            if (this.encodingData != null)
                this.encodingData.totalSeptetCount++;
        }
    }
}
