package org.vafer.jdeb.data;

import java.math.BigInteger;

/**
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 * @author Elliot West <elliot@last.fm>
 */
public class DataSize {
    
    BigInteger count = BigInteger.valueOf(0);

    public void add( long size ) {
        count = count.add(BigInteger.valueOf(size));
    }

    public String toString() {
        return "" + count;
    }

}