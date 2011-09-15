package org.vafer.jdeb.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.utils.Utils;

/**
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 * @author Elliot West <elliot@last.fm>
 */
public class DefaultDataConsumer implements DataConsumer {
    
    private final Console console;
    private final TarOutputStream outputStream;
    private final List<String> addedDirectories;
    private StringBuilder checkSums;
    private MessageDigest digest;
    private DataSize dataSize;

    DefaultDataConsumer(Console console, TarOutputStream outputStream, StringBuilder checkSums, DataSize dataSize, List<String> addedDirectories) {
        this.console = console;
        this.outputStream = outputStream;
        this.checkSums = checkSums;
        this.dataSize = dataSize;
        this.addedDirectories = addedDirectories;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void onEachDir( String dirname, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
        dirname = fixPath(dirname);

        createParentDirectories(new File(dirname).getParent(), user, uid, group, gid);

        // The directory passed in explicitly by the caller also gets the passed-in mode.  (Unlike
        // the parent directories for now.  See related comments at "int mode =" in
        // createParentDirectories, including about a possible bug.)
        createDirectory(dirname, user, uid, group, gid, mode, 0);

        console.println("dir: " + dirname);
    }

    public void onEachFile( InputStream inputStream, String filename, String linkname, String user, int uid, String group, int gid, int mode, long size ) throws IOException {
        filename = fixPath(filename);

        createParentDirectories(new File(filename).getParent(), user, uid, group, gid);

        TarEntry entry = new TarEntry(filename);

        // FIXME: link is in the constructor
        entry.setUserName(user);
        entry.setUserId(uid);
        entry.setGroupName(group);
        entry.setGroupId(gid);
        entry.setMode(mode);
        entry.setSize(size);

        outputStream.putNextEntry(entry);

        dataSize.add(size);

        digest.reset();

        Utils.copy(inputStream, new DigestOutputStream(outputStream, digest));

        final String md5 = Utils.toHex(digest.digest());

        outputStream.closeEntry();

        console.println(
                "file:" + entry.getName() +
                " size:" + entry.getSize() +
                " mode:" + entry.getMode() +
                " linkname:" + entry.getLinkName() +
                " username:" + entry.getUserName() +
                " userid:" + entry.getUserId() +
                " groupname:" + entry.getGroupName() +
                " groupid:" + entry.getGroupId() +
                " modtime:" + entry.getModTime() +
                " md5: " + md5
        );

        checkSums.append(md5).append(" ").append(entry.getName()).append('\n');

    }

    private String fixPath(String path) {
        // If we're receiving directory names from Windows, then we'll convert to use slash
        // This does eliminate the ability to use of a backslash in a directory name on *NIX, but in practice, this is a non-issue
        if (path.indexOf('\\') > -1) {
            path = path.replace('\\', '/');
        }
        // ensure the path is like : ./foo/bar
        if (path.startsWith("/")) {
            path = "." + path;
        } else if (!path.startsWith("./")) {
            path = "./" + path;
        }
        return path;
    }

    private void createDirectory(String directory, String user, int uid, String group, int gid, int mode, long size) throws IOException {
        // All dirs should end with "/" when created, or the test DebAndTaskTestCase.testTarFileSet() thinks its a file
        // and so thinks it has the wrong permission.
        // This consistency also helps when checking if a directory already exists in addedDirectories.

        if (!directory.endsWith("/")) {
            directory += "/";
        }

        if (!addedDirectories.contains(directory)) {
            TarEntry entry = new TarEntry(directory);
            // FIXME: link is in the constructor
            entry.setUserName(user);
            entry.setUserId(uid);
            entry.setGroupName(group);
            entry.setGroupId(gid);
            entry.setMode(mode);
            entry.setSize(size);

            outputStream.putNextEntry(entry);
            outputStream.closeEntry();
            addedDirectories.add(directory); // so addedDirectories consistently have "/" for finding duplicates.
        }
    }

    private void createParentDirectories(String dirname, String user, int uid, String group, int gid) throws IOException {
        // Debian packages must have parent directories created
        // before sub-directories or files can be installed.
        // For example, if an entry of ./usr/lib/foo/bar existed
        // in a .deb package, but the ./usr/lib/foo directory didn't
        // exist, the package installation would fail.  The .deb must
        // then have an entry for ./usr/lib/foo and then ./usr/lib/foo/bar

        if (dirname == null) {
          return;
        }

        // The loop below will create entries for all parent directories
        // to ensure that .deb packages will install correctly.
        String[] pathParts = dirname.split("\\/");
        String parentDir = "./";
        for (int i = 1; i < pathParts.length; i++) {
            parentDir += pathParts[i] + "/";
            // Make it so the dirs can be traversed by users.
            // We could instead try something more granular, like setting the directory
            // permission to 'rx' for each of the 3 user/group/other read permissions
            // found on the file being added (ie, only if "other" has read
            // permission on the main node, then add o+rx permission on all the containing
            // directories, same w/ user & group), and then also we'd have to
            // check the parentDirs collection of those already added to
            // see if those permissions need to be similarly updated.  (Note, it hasn't
            // been demonstrated, but there might be a bug if a user specifically
            // requests a directory with certain permissions,
            // that has already been auto-created because it was a parent, and if so, go set
            // the user-requested mode on that directory instead of this automatic one.)
            // But for now, keeping it simple by making every dir a+rx.   Examples are:
            // drw-r----- fs/fs   # what you get with setMode(mode)
            // drwxr-xr-x fs/fs   # Usable. Too loose?
            int mode = TarEntry.DEFAULT_DIR_MODE;

            createDirectory(parentDir, user, uid, group, gid, mode, 0);
        }
    }

}
