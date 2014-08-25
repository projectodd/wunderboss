Since I forget this every time, I'm documenting it here.

First, commit and push all changes.

## Versioned Release

* Set up the release repo:

    git remote add release git@github.com:projectodd/wunderboss-release.git

* Make sure the release repo is up-to-date and doesn't have the release tag:

    git push release master -f
    git push release :0.2.0

* `mvn release:clean`
* `mvn release:prepare -DautoVersionSubmodules` -
   Use the version number (ex: '0.3.0') as the tag, and use the next
   minor version + SNAPSHOT for the next dev version (ex:
   '0.4.0-SNAPSHOT' instead of '0.3.1-SNAPSHOT')
* `mvn release:perform`
* Log into <http://oss.sonatype.org>
* Browse to 'Staging Repositories', find the correct 'org.projectodd' repo,
  and select it
* Close the repo, then release it (you may need to refresh the list
  for the release button to become active).
* Wait for several hours until sonatype syncs to central. You can
  check <http://search.maven.org> to know when this has completed.
* `git push origin master && git push origin master --tags`
For reference:

<https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide>
<http://wickedsource.org/2013/09/23/releasing-your-project-to-maven-central-guide/>
