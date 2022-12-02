Releasing
=========

1. Create a new branch called `release/X.Y.Z`
    
    `git checkout -b release/X.Y.Z`
1. Change the version in `gradle.properties` to your desired release version
1. Commit the changes with a "Create release X.Y.Z" message
    
    `git commit -am "Create release X.Y.Z."` (where X.Y.Z is the new version)
1. Create a tag, which will also be used for the GitHub release
    
    `git tag -a vX.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)

    `git push && git push --tags`
1. Merge release/X.Y.Z back to main branch

Example (stable release)
========
1. Current VERSION_NAME in `gradle.properties` = 0.10.0
    
    `git checkout -b release/0.10.1`
1. Change VERSION_NAME = 0.10.1 (next higher version)
1. `git commit -am "Create release 0.10.1"`
1. `git tag -a v0.10.1 -m "Version 0.10.1"`
    
    `git push && git push --tags`
1. Merge release/0.10.1 back to main branch