/**                                                                                                                                                                                
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved.                                                                                                                             
 *                                                                                                                                                                                 
 * Licensed under the Apache License, Version 2.0 (the "License"); you                                                                                                             
 * may not use this file except in compliance with the License. You                                                                                                                
 * may obtain a copy of the License at                                                                                                                                             
 *                                                                                                                                                                                 
 * http://www.apache.org/licenses/LICENSE-2.0                                                                                                                                      
 *                                                                                                                                                                                 
 * Unless required by applicable law or agreed to in writing, software                                                                                                             
 * distributed under the License is distributed on an "AS IS" BASIS,                                                                                                               
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or                                                                                                                 
 * implied. See the License for the specific language governing                                                                                                                    
 * permissions and limitations under the License. See accompanying                                                                                                                 
 * LICENSE file.                                                                                                                                                                   
 */
package com.yahoo.ycsb;

import java.util.Iterator;
import java.util.ArrayList;
/**
 * YCSB-specific buffer class.  ByteIterators are designed to support
 * efficient field generation, and to allow backend drivers that can stream
 * fields (instead of materializing them in RAM) to do so.
 * <p>
 * YCSB originially used String objects to represent field values.  This led to
 * two performance issues.
 * </p><p>
 * First, it leads to unnecessary conversions between UTF-16 and UTF-8, both
 * during field generation, and when passing data to byte-based backend
 * drivers.
 * </p><p>
 * Second, Java strings are represented internally using UTF-16, and are
 * built by appending to a growable array type (StringBuilder or
 * StringBuffer), then calling a toString() method.  This leads to a 4x memory
 * overhead as field values are being built, which prevented YCSB from
 * driving large object stores.
 * </p>
 * The StringByteIterator class contains a number of convenience methods for
 * backend drivers that convert between Map&lt;String,String&gt; and
 * Map&lt;String,ByteBuffer&gt;.
 *
 * @author sears
 */
public abstract class ByteIterator implements Iterator<Byte> {

    transient byte []b = new byte[6];
    transient short read = 0;   // number of bytes in b
    transient short consumed  = 0;   // number of bytes consumed from b
    
	@Override
	public abstract boolean hasNext();

	@Override
	public Byte next() {
		throw new UnsupportedOperationException();
		//return nextByte();
	}

	public final long getTimestamp() {
	    while( read < 6 && bytesLeft0() > 0) {
	        b[read++] = nextByte0();
	    }
	    if( read < 6)
	        return -1;
	    long t = (b[5] - ' ');
	    t = (t << 5) + (b[4] - ' ');;
        t = (t << 5) + (b[3] - ' ');;
        t = (t << 5) + (b[2] - ' ');;
        t = (t << 5) + (b[1] - ' ');;
        t = (t << 5) + (b[0] - ' ');;
        return t;
	}
	
    protected abstract byte nextByte0();

	public final byte nextByte() {
	    if( consumed >= 6)
	        return nextByte0();
	    if( consumed < read)
	        return b[consumed++];
	    b[read++] = nextByte0();
	    return b[consumed++];
	}
	
        /** @return byte offset immediately after the last valid byte */
	public int nextBuf(byte[] buf, int buf_off) {
		int sz = buf_off;
		while(sz < buf.length && hasNext()) {
			buf[sz] = nextByte();
			sz++;
		}
		return sz;
	}

	protected abstract long bytesLeft0();
	
    public final long bytesLeft() {
        return bytesLeft0() + read - consumed;
    }
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/** Consumes remaining contents of this object, and returns them as a string. */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		while(this.hasNext()) { sb.append((char)nextByte()); }
		return sb.toString();
	}
	/** Consumes remaining contents of this object, and returns them as a byte array. */
	public byte[] toArray() {
	    long left = bytesLeft();
	    if(left != (int)left) { throw new ArrayIndexOutOfBoundsException("Too much data to fit in one array!"); }
	    byte[] ret = new byte[(int)left];
	    int off = 0;
	    while(off < ret.length) {
		off = nextBuf(ret, off);
	    }
	    return ret;
	}

}
