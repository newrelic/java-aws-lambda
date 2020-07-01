# AWS Lambda OpenTracing Java SDK Release Process

## Release
1. Run the [ReleaseLambdaSDK](https://javaagent-build.pdx.vm.datanerd.us/view/Java%20Agent/job/ReleaseLambdaSDK/) jenkins job by selecting `Build with Parameters`, specifying the release version, and selecting `Build`. This job will handle the following tasks: 
   * Run all tests
   * Build artifacts
   * Upload versioned artifacts to sonatype
   * Tag release version
2. When the `ReleaseLambdaSDK` job finishes there should be an artifact in the [Sonatype staging repository](https://oss.sonatype.org/index.html#stagingRepositories). Verify that the artifact is staged by searching for `comnewrelic`, there should be a numbered artifact named similar to `comnewrelic-1234`.
3. Select the artifact in the Sonatype staging repository, choose the `Content` tab, and verify that the files and version looks correct.
4. Click "Close" (add a simple comment like "releasing 1.1.0") and wait until the "Release" option is available.
5. Click "Release" (add a simple comment like "releasing 1.1.0") [note: keep the "Automatically Drop" checkbox checked]
6. Once this is complete, verify that it shows up in [Maven Central](https://repo1.maven.org/maven2/com/newrelic/opentracing/java-aws-lambda/) or the [public facing version of the site](https://search.maven.org/search?q=a:java-aws-lambda). Note: This may take awhile, potentially up to a couple of hours, and it's likely that it will show up in Maven Central first. Additionally, https://mvnrepository.com/ is not directly associated with sonatype, and it may take days to show up there, so don't worry if it doesn't.
7. A release should automatically be created on the [project's repo](https://github.com/newrelic/java-aws-lambda/releases). Select the latest release by clicking on the version number and update it to include any relevant release notes (bug fixes, improvements, notes).

## Post Release
The newly released artifact should be incorporated into an existing project to validate that it functions as expected.

Submit and merge a PR with the following:
- Update the `Usage` example in the README with the newly released version (e.g. `compile com.newrelic.opentracing:java-aws-lambda:2.0.0`). 
- Update the [gradle.properties](gradle.properties) file with a snapshot of the next version to be released (e.g. `version = 2.1.0-SNAPSHOT`).
- Update the Github release with details of the new release.
