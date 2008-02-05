#!/bin/sh

#Update the target pom
sed -e "s/<version>X<\/version>/<version>$VER<\/version>/" redirection.pom > target.pom

#Deploy the redirection pom
mvn deploy:deploy-file -DgroupId=org.apache.maven.plugins -DartifactId=maven-clover-plugin -Dversion=$VER -Dfile=target.pom -DpomFile=target.pom -DgeneratePom=false -Dpackaging=pom -Durl=dav:https://maven.atlassian.com/public -DrepositoryId=atlassian-m2-repository
