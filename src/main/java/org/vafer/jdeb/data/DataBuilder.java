package org.vafer.jdeb.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.apache.tools.tar.TarOutputStream;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;

/**
 * Build the control section of the debian package.
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 * @author Elliot West <elliot@last.fm>
 */
public class DataBuilder {

    private final Console console;
    private DataSize dataSize;
    private StringBuilder checkSums;
    
    public DataBuilder(Console console) {
        this.console = console;
    }

    /**
     * Build the data archive of the deb from the provided DataProducers
     * @param pData
     * @param pOutput
     * @param pChecksums
     * @param pCompression the compression method used for the data file (gzip, bzip2 or anything else for no compression)
     * @return
     * @throws IOException
     */
    public void build( final DataProducer[] pData, final File pOutput, String pCompression ) throws IOException {
        console.println("Building data");
        
        checkSums = new StringBuilder();
        dataSize = new DataSize();
        
        final TarOutputStream outputStream = createOutputStream(pOutput, pCompression);
        outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

        final List<String> addedDirectories = new ArrayList<String>();
        final DataConsumer receiver = new DefaultDataConsumer(console, outputStream, checkSums, dataSize, addedDirectories);

        for (int i = 0; i < pData.length; i++) {
            final DataProducer data = pData[i];
            data.produce(receiver);
        }

        outputStream.close();
        console.println("Total size: " + dataSize);
    }

    private TarOutputStream createOutputStream( final File pOutput, String pCompression ) throws FileNotFoundException, IOException {
        OutputStream out = new FileOutputStream(pOutput);
        if ("gzip".equals(pCompression)) {
            out = new GZIPOutputStream(out);
        } else if ("bzip2".equals(pCompression)) {
            out.write("BZ".getBytes());
            out = new CBZip2OutputStream(out);
        }
        return new TarOutputStream(out);
    }
    
    public BigInteger getSize() {
        return dataSize.count;
    }
    
    public String getMD5s() {
        return checkSums.toString();
    }
    
}
