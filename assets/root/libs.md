# OpenTTY Apps Docs

## Starting 

To create Packages to OpenTTY, you need understand how it works. Here I will explain about this.  

Lets start choosing a envirronment to write codes, I use Sublime Text to do it, but you can use any editor or Nano inside MIDlet.  

Is important to know that the Packages is writed in [INI format](https://cheatsheets.zip/ini).

## First Package

The basic data that need to be write at file is 

```ini
[ Config ]

name=My Project
version=1.0
description=My Cool OpenTTY Package

api.version=1.15
api.error=execute echo Incorrect OpenTTY Version;

config=execute echo Is working; 
command=cmd,cmd2,othercmd

cmd=execute echo Command 1; 
cmd2=execute echo Command 2;
othercmd=execute echo Command 3;
```

You can see that:

1. `name` is the name of your project
2. `version` is the current version 
3. `description` is notes about your project

4. `api.version` what the version of OpenTTY your project is focused
5. `api.error` what will be executed if user try run your project in a diferrent version

6. `config` this code will be run when user import your project
7. `command` here you will list the commands that you will create in package, sepair with comma and create other keys with command contents.

