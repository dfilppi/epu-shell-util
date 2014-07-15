epu-shell-util
==============

This repository contains a utility to enable deployment and scaling of elastic PUs from the command line

Overview
========

This utility consists of 3 scripts that together "wrap" the standard XAP gs.{sh,bat} script to add extra functionality.  In this case the extra functionality is the ability to run Elastic PUs, and spaces from the command line without coding, and scale the same.  There are 3 scripts:

* gs2.{sh,bat} - Executes the "gs.groovy" script, and if no action is taken, forwards the parameters to the standard gs.{sh,bat} script.  The goal is to provide a transparent wrapper/decorator that adds new commands to the standard gs script.

* gs.groovy - Holds the code to run additional commands. The commands added currently:
            - deploy-stateful-epu : deploys a stateful EPU
            - deploy-stateless-epu : deploys a web/stateless EPU
            - deploy-espace : deploys an elastic space
            - scale-epu : scales a running EPU

Usage
=====

To install, copy the scripts to the XAP bin directory.  Run gs2.{bat,sh} with one of the commands above and no arguments for details about the arguments.  The script itself does not validate arguments (much), and so silly combinations are allowed which the API will reject.

Most of the EPU functionality has been implemented.  Security is one that comes to mind that isn't.  Also missing is support for adding container configuration (via properties) and support for automatic machine provisioning.

