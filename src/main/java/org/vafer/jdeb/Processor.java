/*
 * Copyright 2010 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vafer.jdeb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.vafer.jdeb.changes.ChangeSet;
import org.vafer.jdeb.changes.ChangesProvider;
import org.vafer.jdeb.control.ControlBuilder;
import org.vafer.jdeb.data.DataBuilder;
import org.vafer.jdeb.descriptors.ChangesDescriptor;
import org.vafer.jdeb.descriptors.InvalidDescriptorException;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.signing.SigningUtils;
import org.vafer.jdeb.utils.InformationOutputStream;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * The processor does the actual work of building the deb related files.
 * It is been used by the ant task and (later) the maven plugin.
 *
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public class Processor {

    private final Console console;
    private final ControlBuilder controlBuilder;
    private final DataBuilder dataBuilder;

    public Processor( final Console pConsole, final VariableResolver pResolver ) {
        console = pConsole;
        dataBuilder = new DataBuilder(console);
        controlBuilder = new ControlBuilder(pResolver, pConsole);
    }

    private void addTo( final ArArchiveOutputStream pOutput, final String pName, final String pContent ) throws IOException {
        final byte[] content = pContent.getBytes();
        pOutput.putArchiveEntry(new ArArchiveEntry(pName, content.length));
        pOutput.write(content);
        pOutput.closeArchiveEntry();
    }

    private void addTo( final ArArchiveOutputStream pOutput, final String pName, final File pContent ) throws IOException {
        pOutput.putArchiveEntry(new ArArchiveEntry(pName, pContent.length()));

        final InputStream input = new FileInputStream(pContent);
        try {
            Utils.copy(input, pOutput);
        } finally {
            input.close();
        }

        pOutput.closeArchiveEntry();
    }

    /**
     * Create the debian archive with from the provided control files and data producers.
     *
     * @param pControlFiles
     * @param pData
     * @param pOutput
     * @param compression the compression method used for the data file (gzip, bzip2 or anything else for no compression)
     * @return PackageDescriptor
     * @throws PackagingException
     */
    public PackageDescriptor createDeb( final File[] pControlFiles, final DataProducer[] pData, final File pOutput, String compression ) throws PackagingException, InvalidDescriptorException {

        File tempData = null;
        File tempControl = null;

        try {
            tempData = File.createTempFile("deb", "data");
            tempControl = File.createTempFile("deb", "control");
            
            dataBuilder.build(pData, tempData, compression);
            final BigInteger size = dataBuilder.getSize();
            final String checkSums = dataBuilder.getMD5s();
            
            controlBuilder.build(pControlFiles, size, checkSums, tempControl);
            PackageDescriptor packageDescriptor = controlBuilder.getPackageDescriptor();
             
            pOutput.getParentFile().mkdirs();
            final InformationOutputStream md5output = new InformationOutputStream(new FileOutputStream(pOutput), MessageDigest.getInstance("MD5"));
            //Add chain of filters in order to calculate sha1 and sha256 for 1.8 format
            final InformationOutputStream sha1output = new InformationOutputStream(md5output, MessageDigest.getInstance("SHA1"));
            final InformationOutputStream sha256output = new InformationOutputStream(sha1output, MessageDigest.getInstance("SHA-256"));

            final ArArchiveOutputStream ar = new ArArchiveOutputStream(sha256output);

            addTo(ar, "debian-binary", "2.0\n");
            addTo(ar, "control.tar.gz", tempControl);
            addTo(ar, "data.tar" + getExtension(compression), tempData);

            ar.close();

            // intermediate values
            packageDescriptor.set("MD5", md5output.getHexDigest());
            packageDescriptor.set("SHA1", sha1output.getHexDigest());
            packageDescriptor.set("SHA256", sha256output.getHexDigest());
            packageDescriptor.set("Size", "" + md5output.getSize());
            packageDescriptor.set("File", pOutput.getName());

            return packageDescriptor;

        } catch(InvalidDescriptorException e) {
            throw e;
        } catch(Exception e) {
            throw new PackagingException("Could not create deb package", e);
        } finally {
            if (tempData != null) {
                if (!tempData.delete()) {
                    throw new PackagingException("Could not delete " + tempData);
                }
            }
            if (tempControl != null) {
                if (!tempControl.delete()) {
                    throw new PackagingException("Could not delete " + tempControl);
                }
            }
        }
    }

    /**
     * Return the extension of a file compressed with the specified method.
     *
     * @param pCompression the compression method used
     * @return
     */
    private String getExtension( final String pCompression ) {
        if ("gzip".equals(pCompression)) {
            return ".gz";
        } else if ("bzip2".equals(pCompression)) {
            return ".bz2";
        } else {
            return "";
        }
    }

    /**
     * Create changes file based on the provided PackageDescriptor.
     * If pRing, pKey and pPassphrase are provided the changes file will also be signed.
     * It returns a ChangesDescriptor reflecting the changes
     * @param pPackageDescriptor
     * @param pChangesProvider
     * @param pRing
     * @param pKey
     * @param pPassphrase
     * @param pOutput
     * @return ChangesDescriptor
     * @throws IOException
     */
    public ChangesDescriptor createChanges( final PackageDescriptor pPackageDescriptor, final ChangesProvider pChangesProvider, final InputStream pRing, final String pKey, final String pPassphrase, final OutputStream pOutput ) throws IOException, InvalidDescriptorException {

        final ChangeSet[] changeSets = pChangesProvider.getChangesSets();
        final ChangesDescriptor changesDescriptor = new ChangesDescriptor(pPackageDescriptor, changeSets);

        changesDescriptor.set("Format", "1.8");

        if (changesDescriptor.get("Binary") == null) {
            changesDescriptor.set("Binary", changesDescriptor.get("Package"));
        }

        if (changesDescriptor.get("Source") == null) {
            changesDescriptor.set("Source", changesDescriptor.get("Package"));
        }

        if (changesDescriptor.get("Description") == null) {
            changesDescriptor.set("Description", "update to " + changesDescriptor.get("Version"));
        }

        final StringBuilder checksumsSha1 = new StringBuilder("\n");
        // Checksums-Sha1:
        // 56ef4c6249dc3567fd2967f809c42d1f9b61adf7 45964 jdeb.deb
        checksumsSha1.append(' ').append(changesDescriptor.get("SHA1"));
        checksumsSha1.append(' ').append(changesDescriptor.get("Size"));
        checksumsSha1.append(' ').append(changesDescriptor.get("File"));
        changesDescriptor.set("Checksums-Sha1", checksumsSha1.toString());

        final StringBuilder checksumsSha256 = new StringBuilder("\n");
        // Checksums-Sha256:
        // 38c6fa274eb9299a69b739bcbdbd05c7ffd1d8d6472f4245ed732a25c0e5d616 45964 jdeb.deb
        checksumsSha256.append(' ').append(changesDescriptor.get("SHA256"));
        checksumsSha256.append(' ').append(changesDescriptor.get("Size"));
        checksumsSha256.append(' ').append(changesDescriptor.get("File"));
        changesDescriptor.set("Checksums-Sha256", checksumsSha256.toString());


        final StringBuilder files = new StringBuilder("\n");
        files.append(' ').append(changesDescriptor.get("MD5"));
        files.append(' ').append(changesDescriptor.get("Size"));
        files.append(' ').append(changesDescriptor.get("Section"));
        files.append(' ').append(changesDescriptor.get("Priority"));
        files.append(' ').append(changesDescriptor.get("File"));
        changesDescriptor.set("Files", files.toString());

        if (!changesDescriptor.isValid()) {
            throw new InvalidDescriptorException(changesDescriptor);
        }

        final String changes = changesDescriptor.toString();
        //console.println(changes);

        final byte[] changesBytes = changes.getBytes("UTF-8");

        if (pRing == null || pKey == null || pPassphrase == null) {
            pOutput.write(changesBytes);
            pOutput.close();
            return changesDescriptor;
        }

        console.println("Signing changes with key " + pKey);

        final InputStream input = new ByteArrayInputStream(changesBytes);

        try {
            SigningUtils.clearSign(input, pRing, pKey, pPassphrase, pOutput);
        } catch (Exception e) {
            e.printStackTrace();
        }

        pOutput.close();

        return changesDescriptor;
    }

}
