// Copyright 2015-2019 Michel Kraemer
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

import de.undercouch.gradle.tasks.download.internal.ProjectApiHelper;
import groovy.lang.Closure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import static de.undercouch.gradle.tasks.download.internal.ProviderHelper.tryGetProvider;

/**
 * Verifies a file's integrity by calculating its checksum.
 * @author Michel Kraemer
 */
public class VerifyAction implements VerifySpec {
    private final ProjectApiHelper projectApi;
    private File src;
    private String algorithm = "MD5";
    private String checksum;
    
    /**
     * Creates a new verify action
     * @param project the project to be built
     */
    public VerifyAction(Project project) {
        this.projectApi = ProjectApiHelper.newInstance(project);
    }

    private String toHex(byte[] barr) {
        StringBuilder result = new StringBuilder();
        for (byte b : barr) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
    
    /**
     * Starts verifying
     * @throws IOException if the file could not verified
     * @throws NoSuchAlgorithmException if the given algorithm is not available
     */
    public void execute() throws IOException, NoSuchAlgorithmException {
        if (src == null) {
            throw new IllegalArgumentException("Please provide a file to verify");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Please provide the algorithm to "
                    + "use to calculate the checksum");
        }
        if (checksum == null) {
            throw new IllegalArgumentException("Please provide a checksum to verify against");
        }
        
        // calculate file's checksum
        MessageDigest md = MessageDigest.getInstance(algorithm);
        String calculatedChecksum;
        try (FileInputStream fis = new FileInputStream(src)) {
            byte[] buf = new byte[1024];
            int read;
            while ((read = fis.read(buf)) != -1) {
                md.update(buf, 0, read);
            }
            calculatedChecksum = toHex(md.digest());
        }
        
        // verify checksum
        if (!calculatedChecksum.equalsIgnoreCase(checksum)) {
            throw new GradleException("Invalid checksum for file '" +
                    src.getName() + "'. Expected " + checksum.toLowerCase() + 
                    " but got " + calculatedChecksum.toLowerCase() + ".");
        }
    }
    
    @Override
    public void src(Object src) {
        if (src instanceof Closure) {
            //lazily evaluate closure
            Closure<?> closure = (Closure<?>)src;
            src = closure.call();
        }

        src = tryGetProvider(src);
        
        if (src instanceof CharSequence) {
            src = projectApi.file(src.toString());
        }
        if (src instanceof File) {
            this.src = (File)src;
        } else {
            throw new IllegalArgumentException("Verification source must "
                    + "either be a CharSequence or a File");
        }
    }
    
    @Override
    public void algorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public void checksum(String checksum) {
        this.checksum = checksum;
    }
    
    public File getSrc() {
        return src;
    }

    public String getAlgorithm() {
        return algorithm;
    }
    
    public String getChecksum() {
        return checksum;
    }
}
