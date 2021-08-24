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

import org.apache.commons.io.FileUtils;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the plugin's functionality
 * @author Jan Berkel
 * @author Michel Kraemer
 */
@RunWith(value = Parameterized.class)
public class FunctionalDownloadTest extends FunctionalTestBase {
    private String singleSrc;
    private String multipleSrc;
    private String dest;
    private File destFile;

    private static String lastVersion;
    private static Set<String> gradleProcesses;

    /**
     * @return the Gradle versions to test against
     */
    @Parameterized.Parameters(name = "Gradle {0}")
    public static List<String> versionsToTest() {
        if ("true".equals(System.getenv("CI"))) {
            // on CI server, limit to major versions to avoid running
            // out of open file descriptors (happens when we load the
            // jar files of too many Gradle distributions into memory)
            return Arrays.asList(
                    "2.14.1",
                    "3.5.1",
                    "4.10.3",
                    "5.6.4",
                    "6.9",
                    "7.1.1"
            );
        } else {
            return Arrays.asList(
                    "2.14.1", "3.0", "3.1", "3.2.1", "3.3", "3.4.1", "3.5.1",
                    "4.0.2", "4.1", "4.2.1", "4.3.1", "4.4.1", "4.5.1",
                    "4.6", "4.7", "4.8.1", "4.9", "4.10.3",
                    "5.0", "5.1", "5.1.1", "5.2", "5.2.1", "5.3", "5.3.1",
                    "5.4", "5.4.1", "5.5", "5.5.1",
                    "5.6", "5.6.1", "5.6.2", "5.6.3", "5.6.4",
                    "6.0", "6.0.1", "6.1", "6.1.1", "6.2", "6.2.1", "6.2.2",
                    "6.3", "6.4", "6.4.1", "6.5", "6.5.1", "6.6", "6.6.1",
                    "6.7", "6.7.1", "6.8", "6.8.1", "6.8.2", "6.8.3", "6.9",
                    "7.0", "7.0.1", "7.0.2", "7.1", "7.1.1"
            );
        }
    }

    /**
     * Constructs a new functional test
     * @param gradleVersion the Gradle version to test against (null for default)
     */
    public FunctionalDownloadTest(String gradleVersion) {
        this.gradleVersion = gradleVersion;

        // On CI server, kill all Gradle daemons that we started earlier.
        // Otherwise, the daemon processes will pile up and use too much main
        // memory, which will eventually cause the build to fail.
        if ("true".equals(System.getenv("CI"))) {
            if (!Objects.equals(gradleVersion, lastVersion)) {
                try {
                    if (gradleProcesses == null) {
                        gradleProcesses = getGradleProcesses();
                    }
                    Set<String> currentGradleProcesses = getGradleProcesses();
                    currentGradleProcesses.removeAll(gradleProcesses);
                    for (String pid : currentGradleProcesses) {
                        System.out.println("Killing Gradle process " + pid);
                        killProcess(pid);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            lastVersion = gradleVersion;
        }
    }

    /**
     * List all Gradle processes in the system
     * @return the PIDs of the Gradle processes
     * @throws IOException if the `ps` command could not be executed
     */
    private static Set<String> getGradleProcesses() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("ps", "x");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Set<String> result = new HashSet<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("gradle-launcher")) {
                String[] t = line.trim().split("\\s+");
                String pid = t[0];
                result.add(pid);
            }
        }
        return result;
    }

    /**
     * Kill a process by its PID
     * @param pid the PID
     * @throws IOException if the `kill` command could not be executed
     */
    private static void killProcess(String pid) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("kill", pid);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        IOUtils.skip(reader, Long.MAX_VALUE);
    }

    /**
     * Set up the functional tests
     * @throws Exception if anything went wrong
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        singleSrc = "'" + wireMockRule.url(TEST_FILE_NAME) + "'";
        multipleSrc = "['" +  wireMockRule.url(TEST_FILE_NAME) +
                "', '" + wireMockRule.url(TEST_FILE_NAME2) + "']";
        destFile = new File(testProjectDir.getRoot(), "someFile");
        dest = "file('" + destFile.getName() + "')";
    }

    /**
     * Test if a single file can be downloaded successfully
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFile() throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
        assertTrue(destFile.isFile());
        assertEquals(CONTENTS, FileUtils.readFileToString(destFile));
    }

    /**
     * Test if a single file can be downloaded successfully when destination is
     * a RegularFileProperty
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileUsingRegularFileProperty() throws Exception {
        Assume.assumeTrue(GradleVersion.version("5.0").compareTo(
                GradleVersion.version(gradleVersion)) < 0);
        configureDefaultStub();
        String setup = "RegularFileProperty fp = project.objects.fileProperty();\n" +
                "fp.set(" + dest + ")\n";
        assertTaskSuccess(download(new Parameters(singleSrc, "fp", setup, true, false)));
        assertTrue(destFile.isFile());
        assertEquals(CONTENTS, FileUtils.readFileToString(destFile));
    }

    /**
     * Test if a single file can be downloaded successfully when destination
     * is a basic Property provider
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileUsingFileProperty() throws Exception {
        Assume.assumeTrue(GradleVersion.version("4.3").compareTo(
                GradleVersion.version(gradleVersion)) < 0);
        configureDefaultStub();
        String setup = "Property fp = project.objects.property(File.class);\n" +
                "fp.set(" + dest + ")\n";
        assertTaskSuccess(download(new Parameters(singleSrc, "fp", setup, true, false)));
        assertTrue(destFile.isFile());
        assertEquals(CONTENTS, FileUtils.readFileToString(destFile));
    }


    /**
     * Test if a single file can be downloaded successfully when destination is
     * a file inside the buildDirectory
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileUsingBuildDirectoryFile() throws Exception {
        Assume.assumeTrue(GradleVersion.version("4.3").compareTo(
                GradleVersion.version(gradleVersion)) < 0);
        configureDefaultStub();
        String dest = "layout.buildDirectory.file('download/outputfile')";
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
        File destFile = new File(testProjectDir.getRoot(), "build/download/outputfile");
        assertTrue(destFile.isFile());
        assertEquals(CONTENTS, FileUtils.readFileToString(destFile));
    }


    /**
     * Test if a single file can be downloaded successfully when destination
     * is a directory inside the buildDirectory
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileUsingBuildDirectoryDir() throws Exception {
        Assume.assumeTrue(GradleVersion.version("4.3").compareTo(
                GradleVersion.version(gradleVersion)) < 0);
        configureDefaultStub();
        String dest = "layout.buildDirectory.dir('download/')";
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
        File[] destFiles = new File(testProjectDir.getRoot(), "build/download/").listFiles();
        assertNotNull(destFiles);
        File destFile = destFiles[0];
        assertTrue(destFile.isFile());
        assertEquals(CONTENTS, FileUtils.readFileToString(destFile));
    }

    /**
     * Test if a single file can be downloaded successfully with quiet mode
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileWithQuietMode() throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true,
                false, true, false, true)));
        assertTrue(destFile.isFile());
        assertEquals(CONTENTS, FileUtils.readFileToString(destFile));
    }

    /**
     * Test if a single file can be downloaded successfully with quiet mode
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileWithoutCompress() throws Exception {
        configureDefaultStub();
        configureDefaultStub2();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true,
                false, false, false, false)));
        assertTrue(destFile.isFile());
        assertEquals(CONTENTS, FileUtils.readFileToString(destFile));
    }

    /**
     * Test if multiple files can be downloaded successfully
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadMultipleFiles() throws Exception {
        configureDefaultStub();
        configureDefaultStub2();
        assertTaskSuccess(download(new Parameters(multipleSrc, dest, true, false)));
        assertTrue(destFile.isDirectory());
        assertEquals(CONTENTS, FileUtils.readFileToString(
                new File(destFile, TEST_FILE_NAME)));
        assertEquals(CONTENTS2, FileUtils.readFileToString(
                new File(destFile, TEST_FILE_NAME2)));
    }

    /**
     * Download a file twice and check if the second attempt is skipped
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileTwiceMarksTaskAsUpToDate() throws Exception {
        configureDefaultStub();
        final Parameters parameters = new Parameters(singleSrc, dest, false, false);
        assertTaskSuccess(download(parameters));
        assertTaskUpToDate(download(parameters));
    }

    /**
     * Download a file with 'overwrite' flag and check if the second attempt succeeds
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileTwiceWithOverwriteExecutesTwice() throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, false)));
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
    }

    /**
     * Download a file twice in offline mode and check if the second attempt is
     * skipped even if the 'overwrite' flag is set
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadSingleFileTwiceWithOfflineMode() throws Exception {
        configureDefaultStub();
        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, false)));
        assertTaskSkipped(download(new Parameters(singleSrc, dest, true, false,
                true, true, false)));
    }

    /**
     * Download a file once, then download again with 'onlyIfModified'
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadOnlyIfNewer() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "Sat, 21 Jun 2019 11:54:15 GMT")
                        .withBody(CONTENTS)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, true)));
        assertTaskUpToDate(download(new Parameters(singleSrc, dest, true, true)));
    }

    /**
     * Download a file once, then download again with 'onlyIfModified'.
     * File changed between downloads.
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadOnlyIfNewerRedownloadsIfFileHasBeenUpdated() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "Sat, 21 Jun 2019 11:54:15 GMT")
                        .withBody(CONTENTS)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, false, true)));

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", "Sat, 21 Jun 2019 11:55:15 GMT")
                        .withBody(CONTENTS)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, true)));
    }
    
    /**
     * Download a file once, then download again with 'useETag'
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadUseETag() throws Exception {
        String etag = "\"foobar\"";

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", absent())
                .willReturn(aResponse()
                        .withHeader("ETag", etag)
                        .withBody(CONTENTS)));

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .withHeader("If-None-Match", equalTo(etag))
                .willReturn(aResponse()
                        .withStatus(304)));

        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, true,
                false, false, false, true)));
        assertTaskUpToDate(download(new Parameters(singleSrc, dest, true, true,
                false, false, false, true)));
    }

    /**
     * Create destination file locally, then run download.
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadOnlyIfNewerReDownloadIfFileExists() throws Exception {
        String lm = "Sat, 21 Jun 2019 11:54:15 GMT";
        long expectedlmlong = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .parse(lm)
                .getTime();

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_FILE_NAME))
                .willReturn(aResponse()
                        .withHeader("Last-Modified", lm)
                        .withBody(CONTENTS)));

        FileUtils.writeStringToFile(destFile, CONTENTS, StandardCharsets.UTF_8);
        assertTrue(destFile.setLastModified(expectedlmlong));
        assertTaskSuccess(download(new Parameters(singleSrc, dest, true, false)));
    }
    
    /**
     * Copy a file from a file:// URL once, then download again with 'onlyIfModified'
     * @throws Exception if anything went wrong
     */
    @Test
    public void downloadFileURLOnlyIfNewer() throws Exception {
        File srcFile = folder.newFile();
        FileUtils.writeStringToFile(srcFile, CONTENTS, StandardCharsets.UTF_8);
        String srcFileUri = "'" + srcFile.toURI().toString() + "'";
        assertTaskSuccess(download(new Parameters(srcFileUri, dest, true, true)));
        assertTrue(destFile.setLastModified(srcFile.lastModified()));
        assertTaskUpToDate(download(new Parameters(srcFileUri, dest, true, true)));
    }

    /**
     * Test if the download task is triggered if another task depends on its
     * output file
     * @throws Exception if anything went wrong
     */
    @Test
    public void fileDependenciesTriggersDownloadTask() throws Exception {
        configureDefaultStub();
        assertTaskSuccess(runTask(":processTask", new Parameters(singleSrc, dest, true, false)));
        assertTrue(destFile.isFile());
    }

    /**
     * Test if the download task is triggered if another tasks depends on its
     * output files
     * @throws Exception if anything went wrong
     */
    @Test
    public void fileDependenciesWithMultipleSourcesTriggersDownloadTask() throws Exception {
        configureDefaultStub();
        configureDefaultStub2();
        assertTrue(destFile.mkdirs());
        assertTaskSuccess(runTask(":processTask", new Parameters(multipleSrc, dest, true, false)));
        assertTrue(destFile.isDirectory());
        assertEquals(CONTENTS, FileUtils.readFileToString(
                new File(destFile, TEST_FILE_NAME)));
        assertEquals(CONTENTS2, FileUtils.readFileToString(
                new File(destFile, TEST_FILE_NAME2)));
    }

    /**
     * Create a download task
     * @param parameters the download parameters
     * @return the download task
     * @throws Exception if anything went wrong
     */
    protected BuildTask download(Parameters parameters) throws Exception {
        return runTask(":downloadTask", parameters);
    }

    /**
     * Create a task
     * @param taskName the task's name
     * @param parameters the download parameters
     * @return the task
     * @throws Exception if anything went wrong
     */
    protected BuildTask runTask(String taskName, Parameters parameters) throws Exception {
        return createRunner(parameters)
                .withArguments(parameters.offline ? asList("--offline", taskName) :
                    singletonList(taskName))
                .build()
                .task(taskName);
    }

    /**
     * Create a gradle runner to test against
     * @param parameters the download parameters
     * @return the runner
     * @throws IOException if the build file could not be created
     */
    protected GradleRunner createRunner(Parameters parameters) throws IOException {
        return createRunnerWithBuildFile(
            "plugins { id 'de.undercouch.download' }\n" +
            parameters.setup +
            "task downloadTask(type: Download) {\n" +
                "src(" + parameters.src + ")\n" +
                "dest " + parameters.dest + "\n" +
                "overwrite " + parameters.overwrite + "\n" +
                "onlyIfModified " + parameters.onlyIfModified + "\n" +
                "compress " + parameters.compress + "\n" +
                "quiet " + parameters.quiet + "\n" +
                "useETag " + parameters.useETag + "\n" +
            "}\n" +
            "task processTask {\n" +
                "inputs.files files(downloadTask)\n" +
                "doLast {\n" +
                    "assert !inputs.files.isEmpty()\n" +
                    "inputs.files.each { f -> assert f.isFile() }\n" +
                "}\n" +
            "}\n");
    }

    private static class Parameters {
        final String src;
        final String dest;
        final String setup;
        final boolean overwrite;
        final boolean onlyIfModified;
        final boolean compress;
        final boolean quiet;
        final boolean offline;
        final boolean useETag;

        Parameters(String src, String dest, String setup, boolean overwrite, boolean onlyIfModified) {
            this(src, dest, setup, overwrite, onlyIfModified, true, false, false, false);
        }

        Parameters(String src, String dest, boolean overwrite, boolean onlyIfModified) {
            this(src, dest, overwrite, onlyIfModified, true, false, false);
        }
        
        Parameters(String src, String dest, boolean overwrite, boolean onlyIfModified,
                boolean compress, boolean offline, boolean quiet) {
            this(src, dest, overwrite, onlyIfModified, compress, offline, quiet, false);
        }

        Parameters(String src, String dest, boolean overwrite, boolean onlyIfModified,
                boolean compress, boolean offline, boolean quiet, boolean useETag) {
            this(src, dest, "", overwrite, onlyIfModified, compress, offline, quiet, useETag);
        }

        Parameters(String src, String dest, String setup, boolean overwrite, boolean onlyIfModified,
                boolean compress, boolean offline, boolean quiet, boolean useETag) {
            this.src = src;
            this.dest = dest;
            this.setup = setup;
            this.overwrite = overwrite;
            this.onlyIfModified = onlyIfModified;
            this.compress = compress;
            this.offline = offline;
            this.quiet = quiet;
            this.useETag = useETag;
        }
    }
}
