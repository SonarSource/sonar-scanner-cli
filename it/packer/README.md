Custom Windows VM image for sonar-scanner-cli Windows ITs
=========================================================

This defines a custom Windows image necessary for the ITs. It contains all [build tools helpers](https://github.com/SonarSource/buildTools/blob/docker/bin/), as well as Node JS, which is needed to scan the example projects.

How to build this VM image
--------------------------

*This isn't supposed to be built by hand.* We have a special image on our Google Cloud project, called *packer-builder-v1*. This image gets started up by Cirrus CI in the `create_win_vm_task` (see [`../../.cirrus.yml`](../../.cirrus.yml)), and will use [Packer](https://packer.io/) to create our custom VM image. The Packer instructions are contained in the `sonar-scanner-cli-qa.json` file.

Note that this image is rebuilt by Cirrus CI every time the `sonar-scanner-cli-qa.json` or `setup.ps1` files change (see the `create_win_vm_task`'s `skip` instruction in [`../../.cirrus.yml`](../../.cirrus.yml)). If no changes are detected, the build will be skipped, and the previously existing image will be used.

How to debug this VM image
--------------------------

1. Log on to [Google Cloud](http://console.cloud.google.com/)
2. Go to our SonarQube project (ci-cd-215716)
3. Under *Compute Engine > Images*, you should see *packer-builder-v1*. Start a new VM with this image.
   This image is pre-configured for using Packer, as well as pushing new VM images to our SonarQube project.
4. Once started, SSH into this VM (you can do this directly via the browser).
5. `sudo su` to use the root user (which is configured to use the GCE service account).
   You can now add packer JSON files, and run the `packer build` command to test your new images. **Make sure you remove any test images from GCE.**

