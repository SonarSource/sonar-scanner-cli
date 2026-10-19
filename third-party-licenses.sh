#!/bin/sh
./gradlew generateLicenseReport

cat build/reports/dependency-license/THIRD-PARTY-NOTICES.txt
