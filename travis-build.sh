#! /bin/bash

set -e

echo "TRAVIS_PULL_REQUEST $TRAVIS_PULL_REQUEST"
echo "TRAVIS_TAG $TRAVIS_TAG"

export TAG=`if [ "$TRAVIS_PULL_REQUEST" = "false" -a -n "$TRAVIS_TAG" ] ; then echo "$TRAVIS_TAG" ; fi`

if [ "$TAG" ]; then
  echo "Build is tagged. Uploading Javadoc to https://commercetools.github.io/commercetools-sync-java/javadoc/v/$TAG"
  ./gradlew --info -Dbuild.version=$TRAVIS_TAG gitPublishPush

  echo "Uploading artifact $TAG to Bintray."
  ./gradlew --info -Dbuild.version=$TRAVIS_TAG bintrayUpload
else
  echo "This build doesn't publish the library since it is not tagged."
fi