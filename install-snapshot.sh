#!/usr/bin/env bash
#
# This script just eases the installation process of the JJava for the development purposes.
# Essentially it's just an automated `maven package && unzip && jupyter kernelspec install` cycle.
#
# Usage: to install SNAPSHOT version of the Kernel just run `./install-snapshot.sh`
#

BUILD_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
BUILD_DIR="jjava/target/jjava-${BUILD_VERSION}"
KERNEL_DIR="$(pwd)/${BUILD_DIR}"

mvn clean package || exit 1
unzip -u "${BUILD_DIR}".zip -d "${BUILD_DIR}" || exit 1
jupyter kernelspec install "${KERNEL_DIR}" --name=java --user || exit 1
