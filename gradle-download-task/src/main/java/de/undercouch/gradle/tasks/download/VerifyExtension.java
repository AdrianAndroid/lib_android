// Copyright 2013-2019 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.undercouch.gradle.tasks.download;

import groovy.lang.Closure;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.gradle.api.Project;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

/**
 * An extension that configures and executes a {@link VerifyAction}
 * @author Michel Kraemer
 */
public class VerifyExtension implements Configurable<VerifyExtension> {
    private Project project;
    
    /**
     * Creates a new extension
     * @param project the project to be built
     */
    public VerifyExtension(Project project) {
        this.project = project;
    }
    
    @Override
    public VerifyExtension configure(@SuppressWarnings("rawtypes") Closure cl) {
        VerifyAction va = ConfigureUtil.configure(cl, new VerifyAction(project));
        try {
            va.execute();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not verify file checksum", e);
        }
        return this;
    }
}
