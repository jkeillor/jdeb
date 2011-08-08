package org.vafer.jdeb.control;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.descriptors.InvalidDescriptorException;
import org.vafer.jdeb.descriptors.PackageDescriptor;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

/**
 * Build the control section of the debian package.
 *
 * @author Elliot West <elliot@last.fm>
 */
public class ControlBuilder {

    private static final Set<String> CONFIGURATION_FILENAMES
        = new HashSet<String>(Arrays.asList(new String[] { "conffiles", "preinst", "postinst", "prerm", "postrm" } ));

    private final VariableResolver resolver;
    private final Console console;
    private PackageDescriptor packageDescriptor;
    private List<PropertyPlaceHolderFile> configurationFiles = new ArrayList<PropertyPlaceHolderFile>();
    
    public ControlBuilder(VariableResolver resolver, Console console) {
        this.resolver = resolver;
        this.console = console;
    }

    /**
     * Build control archive of the deb
     * @param pControlFiles
     * @param pDataSize
     * @param pChecksums
     * @param pOutput
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ParseException
     * @throws InvalidDescriptorException 
     */
    public void build( final File[] pControlFiles, final BigInteger pDataSize, final StringBuilder pChecksums, final File pOutput ) throws IOException, ParseException, InvalidDescriptorException {
        final TarOutputStream outputStream = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(pOutput)));
        outputStream.setLongFileMode(TarOutputStream.LONGFILE_GNU);

        for (File file : pControlFiles) {
            if (file.isFile()) {
                final String name = file.getName();
                if (CONFIGURATION_FILENAMES.contains(name)) {
                    addConfigurationFile(configurationFiles, file);
                } else if ("control".equals(name)) {
                    addPackageDescriptorEntry(file);
                } else {
                    addControlEntry(file, outputStream);
                }
            }
        }

        if (packageDescriptor == null) {
            throw new FileNotFoundException("No control file in " + Arrays.toString(pControlFiles));
        }
        packageDescriptor.set("Installed-Size", pDataSize.divide(BigInteger.valueOf(1024)).toString());

        for (PropertyPlaceHolderFile configurationFile : configurationFiles) {
            addControlEntry(configurationFile.getName(), configurationFile.toString(), outputStream);
        }
        addControlEntry("control", packageDescriptor.toString(), outputStream);
        addControlEntry("md5sums", pChecksums.toString(), outputStream);

        outputStream.close();
        
        if (!packageDescriptor.isValid()) {
            throw new InvalidDescriptorException(packageDescriptor);
        }
    }
    

    public PackageDescriptor getPackageDescriptor() {
        return packageDescriptor; 
    }
    
    
    private void addConfigurationFile( List<PropertyPlaceHolderFile> configurationFiles, final File file ) throws IOException, ParseException, FileNotFoundException {
        PropertyPlaceHolderFile configurationFile = new PropertyPlaceHolderFile(file.getName(), new FileInputStream(file), resolver);
        configurationFiles.add(configurationFile);
    }

    private void addPackageDescriptorEntry( final File file ) throws IOException, ParseException, FileNotFoundException {
        packageDescriptor = new PackageDescriptor(new FileInputStream(file), resolver);

        if (packageDescriptor.get("Date") == null) {
            SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH); // Mon, 26 Mar 2007 11:44:04 +0200 (RFC 2822)
            // FIXME Is this field allowed in package descriptors ?
            packageDescriptor.set("Date", fmt.format(new Date()));
        }

        if (packageDescriptor.get("Distribution") == null) {
            packageDescriptor.set("Distribution", "unknown");
        }

        if (packageDescriptor.get("Urgency") == null) {
            packageDescriptor.set("Urgency", "low");
        }

        final String debFullName = System.getenv("DEBFULLNAME");
        final String debEmail = System.getenv("DEBEMAIL");

        if (debFullName != null && debEmail != null) {
            packageDescriptor.set("Maintainer", debFullName + " <" + debEmail + ">");
            console.println("Using maintainer from the environment variables.");
        }
    }
    
    private void addControlEntry( final String pName, final String pContent, final TarOutputStream pOutput ) throws IOException {
        final byte[] data = pContent.getBytes("UTF-8");
        final TarEntry entry = new TarEntry("./" + pName);
        entry.setSize(data.length);
        entry.setNames("root", "root");
        entry.setMode(PermMapper.toMode("755"));

        pOutput.putNextEntry(entry);
        pOutput.write(data);
        pOutput.closeEntry();
    }
    
    private void addControlEntry( final File pFile, final TarOutputStream pOutput ) throws IOException {
        final TarEntry entry = new TarEntry(pFile);
        entry.setName("./" + pFile.getName());
        entry.setNames("root", "root");
        entry.setMode(PermMapper.toMode("755"));

        final InputStream inputStream = new FileInputStream(pFile);
        pOutput.putNextEntry(entry);
        Utils.copy(inputStream, pOutput);
        pOutput.closeEntry();
        inputStream.close();
    }

}
