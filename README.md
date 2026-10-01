<!-- Sonar Marketing hosts these approved brand assets on its Kentico Kontent CDN (assets-eu-01.kc-usercontent.com). Shared URLs are intentional; consult Marketing before replacing them. -->
<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/a23fc7ba-23f0-489a-829d-ed88c0748521/Sonar_Logo_Dark%20Backgrounds.svg">
    <img src="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/82c13eba-d95c-4bb8-8007-7ce77c14e043/Sonar_Logo_Light%20Backgrounds.svg" alt="Sonar logo" width="400">
  </picture>
</p>

[![Build](https://github.com/SonarSource/sonar-scanner-cli/actions/workflows/build.yml/badge.svg)](https://github.com/SonarSource/sonar-scanner-cli/actions/workflows/build.yml)
[![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.scanner.cli%3Asonar-scanner-cli&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.scanner.cli%3Asonar-scanner-cli)

<!-- sonar-marketing:start -->
<!-- Marketing maintains this section. For wording changes, consult the relevant Product Marketing Manager (PMM). Repository CODEOWNERS review accuracy and merge changes. -->

# SonarScanner CLI

SonarScanner CLI is the official scanner for projects that do not use a scanner integrated with their build system. It runs code analysis and sends the results to SonarQube Server or SonarQube Cloud.

Read the [SonarScanner CLI documentation](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/scanners/sonarscanner) to install and run the scanner.

To learn more about the products this scanner works with, visit the [SonarQube product page](https://www.sonarsource.com/products/sonarqube/).

<!-- sonar-marketing:end -->

## Configuration

Project configuration is read from file `sonar-project.properties` or passed on the command line.

## Documentation

https://redirect.sonarsource.com/doc/install-configure-scanner.html

## Have Questions or Feedback?

For support questions ("How do I?", "I got this error, why?", ...), please head to the [SonarSource forum](https://community.sonarsource.com/c/help). There are chances that a question similar to yours has already been answered.

Be aware that this forum is a community, so the standard pleasantries ("Hi", "Thanks", ...) are expected. And if you don't get an answer to your thread, you should sit on your hands for at least three days before bumping it. Operators are not standing by. :-)

## Contributing

If you would like to see a new feature, please create a new thread in the forum ["Suggest new features"](https://community.sonarsource.com/c/suggestions/features).

Please be aware that we are not actively looking for feature contributions. The truth is that it's extremely difficult for someone outside SonarSource to comply with our roadmap and expectations. Therefore, we typically only accept minor cosmetic changes and typo fixes.

With that in mind, if you would like to submit a code contribution, please create a pull request for this repository. Please explain your motives to contribute this change: what problem you are trying to fix, what improvement you are trying to make.

Make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all tests are passing (Travis build is executed for each pull request).

## License

Copyright SonarSource Sàrl.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
