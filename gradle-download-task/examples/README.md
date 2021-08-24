Gradle Download Task Examples
=============================

This directory contains a number of examples for gradle-download-task.

The examples work with Gradle 2.1 and higher. If you need instructions
for older Gradle versions, please have a look at the project's [readme file](../README.md).

A more detailed description of the examples here can be found in the blog post
[10 recipes for gradle-download-task](http://www.michel-kraemer.com/recipes-for-gradle-download).

[simpleTask.gradle](simpleTask.gradle)
--------------------------------------

Download a single file.

    gradle -b simpleTask.gradle

[simpleExtension.gradle](simpleExtension.gradle)
------------------------------------------------

Download a single file using the DSL extension.

    gradle -b simpleExtension.gradle

[multipleFiles.gradle](multipleFiles.gradle)
--------------------------------------------

Download multiple files.

    gradle -b multipleFiles.gradle

[multipleFilesRename.gradle](multipleFilesRename.gradle)
--------------------------------------------------------

Download multiple files and specify destination file names for each of them.

    gradle -b multipleFilesRename.gradle

[customHeader.gradle](customHeader.gradle)
------------------------------------------

Download single file and specify a custom HTTP header.

    gradle -b customHeader.gradle

[directory.gradle](directory.gradle)
------------------------------------

Download all files from a directory.

    gradle -b directory.gradle

[directoryGitHub.gradle](directoryGitHub.gradle)
------------------------------------

Download all files from a directory in GitHub. Use the GitHub API to get the
directory's contents. Parse the result and download the files.

    gradle -b directoryGitHub.gradle

[mirrors.gradle](mirrors.gradle)
--------------------------------

Download a single file from a mirror server. Configure multiple mirror servers
and use the first one that is working.

    gradle -b mirrors.gradle

[srcAndDestClosure.gradle](srcAndDestClosure.gradle)
----------------------------------------------------

Download a single file to a directory. Use closures for the `src` and `dest`
property.

    gradle -b srcAndDestClosure.gradle

[tempRename.gradle](tempRename.gradle)
--------------------------------------

Conditionally download a single file using a temporary name (`<filename>.part`).
Rename the file afterwards if the download was successful.

    gradle -b tempRename.gradle

[unzip.gradle](unzip.gradle)
----------------------------

Download a ZIP file and extract its contents.

    gradle -b unzip.gradle

[useETag.gradle](useETag.gradle)
--------------------------------

Download a file conditionally using its entity tag (ETag).

    gradle -b useETag.gradle

[verify.gradle](verify.gradle)
------------------------------

Download a file and verify its contents by calculating its checksum and
comparing it to a given value.

    gradle -b verify.gradle

[verifyExtension.gradle](verifyExtension.gradle)
------------------------------------------------

Same as [verify.gradle](verify.gradle) but uses the `verifyChecksum` extension
instead of the `Verify` task.

    gradle -b verifyExtension.gradle

Run all examples
----------------

Useful for testing.

Bash:

    find . -maxdepth 1 -name '*.gradle' -type f -exec echo {} \; -exec gradle -b {} \;

Windows:

    FORFILES /M *.gradle /C "cmd /c if @isdir equ FALSE echo @file && gradle -b @file"
