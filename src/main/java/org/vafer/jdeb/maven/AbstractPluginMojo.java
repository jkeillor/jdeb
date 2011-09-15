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
package org.vafer.jdeb.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

public abstract class AbstractPluginMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    protected File buildDirectory;

    /**
     * @parameter expession="$[build.timestamp}"
     * @return
     */
    protected String fragment;

    /**
     * @parameter default-value=false
     * @return
     */
    protected boolean uniqueVersion;

    protected MavenProject getProject() {
        if ( project.getExecutionProject() != null ) {
            return project.getExecutionProject();
        }

        return project;
    }

}
