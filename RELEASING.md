Releasing
=========

 1. Create a new branch called `release/X.Y.Z`
 2. `git checkout -b release/X.Y.Z`
 3. Change the version in `gradle.properties` to your desired release version
 4. Update the `CHANGELOG.md` for the impending release.
 5. `git commit -am "Create release X.Y.Z."` (where X.Y.Z is the new version)
 6. `git push`
 7. Merge release/X.Y.Z back to main branch

Example (stable release)
========
 1. Current VERSION_NAME in `gradle.properties` = 0.10.0
 2. `git checkout -b release/0.10.1`
 3. Change VERSION_NAME = 0.10.1 (next higher version)
 4. Update CHANGELOG.md
 5. `git commit -am "Create release 0.10.1"`
 6. `git push`
 7. Merge release/0.10.1 back to main branch