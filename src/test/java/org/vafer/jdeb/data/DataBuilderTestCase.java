/*
 * Copyright 2009 The Apache Software Foundation.
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

package org.vafer.jdeb.data;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.data.DataBuilder;
import org.vafer.jdeb.producers.DataProducerFileSet;

public class DataBuilderTestCase extends TestCase {

    /**
     * Checks if the file paths in the md5sums file use only unix file separators
     * (this test can only fail on Windows)
     */
    public void testBuildDataWithFileSet() throws Exception {
        Console console = new Console() {
            public void println(String s) {
            }
        };
        
        Project project = new Project();
        project.setCoreLoader(getClass().getClassLoader());
        project.init();

        FileSet fileset = new FileSet();
        fileset.setDir(new File(getClass().getResource("/org/vafer/jdeb/deb/data").toURI()));
        fileset.setIncludes("**/*");
        fileset.setProject(project);

        DataBuilder dataBuilder = new DataBuilder(console);
        dataBuilder.build(new DataProducer[] { new DataProducerFileSet(fileset) }, new File("target/data.tar"), "tar");

        String md5s = dataBuilder.getMD5s();
        assertTrue("empty md5 file", md5s.length() > 0);
        assertFalse("windows path separator found", md5s.indexOf("\\") != -1);
    }
    
}
