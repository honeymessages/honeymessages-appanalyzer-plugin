# Honeymessage Framework Appanalyzer Plugin

Welcome to the Honeymessages [AppAnalyzer](https://github.com/simkoc/scala-appanalyzer) plugin for the [Honeymessages framework](https://github.com/honeymessages/honeymessages-framework).

## Description
This plugin is designed to be used with the AppAnalyzer to instrument Android applications to send honeymessages.

## Requirements
You will first need to follow the instructions to set up the [AppAnalyzer](https://github.com/simkoc/scala-appanalyzer) and can then proceed to use this plugin.
Please make sure that you run `sbt publishLocal` in the AppAnalyzer
home folder after building it using the instructions.

```bash
# in AppAnalyzer home folder
sbt publishLocal
```

## Compiling the Plugin

Compiling the plugin is straight forward you only need to run

```bash
sbt package
```

you can then copy the plugin into the plugin folder of the AppAnalyzer.

## Configuration

Prior to compilation you need to change the return value of `getAuthenticateToken` in `Chatinstrumentation.scala`
to your API token for your user. For further information refer to the [honeymessages framework](https://github.com/honeymessages/honeymessages-framework/#4-acquire-an-authentication-token).

## Usage

You have configured your token, compiled, and copied the plugin into the AppAnalyzer folder.
Now you are read to go. Copy the apps you want to test into a folder and run the following command

```bash
run android_device /path/to/apps/ plugin ChatInstrumentation -p "domain=<HONEYFRAMEWORKDOMAIN>"
```

Now you can follow the on screen instructions and get going.
Follow the on screen instructions.
Your first step is usually to log into the application and set up the communication channel you want to use to send the
honey URLs.
