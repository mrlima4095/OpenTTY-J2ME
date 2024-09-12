# OpenTTY J2ME
![License](https://img.shields.io/badge/License-MIT-blue.svg) ![GitHub top language](https://img.shields.io/github/languages/top/mrlima4095/OpenTTY-J2ME) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/mrlima4095/OpenTTY-J2ME)


OpenTTY is a terminal emulator for mobile phones that support J2ME (Java 2 Micro Edition). It allows users to execute a variety of commands on J2ME-supported devices, providing a lightweight command-line experience.

## Features
- Terminal emulator for J2ME-supported devices
- Basic command execution capabilities
- LCDUI-based terminal interface
- Auto-update functionality and package management system
- Full support for shell scripts (in development)
- Built-in text editor (`nano`) for simple file editing
- Network utilities such as `curl` and `wget`

## Requirements
- A J2ME-supported mobile device
- Minimum memory: 512KB

## Installation

1. Download the latest `.jar` file from the OpenTTY repository.
2. Transfer the file to your mobile phone.
3. Launch the file to install OpenTTY on your device.

## Usage

1. Start OpenTTY from your phone’s menu.
2. A terminal interface will appear where you can execute commands.

### Basic Commands

- `ls`: Lists files in the current directory.
- `cd [directory]`: Changes the current directory.
- `exit`: Exits the terminal.
- `echo [text]`: Displays a line of text or variables.
- `curl [url]`: Transfers data from a server using supported protocols.
- `wget [url]`: Downloads files from the web.
- `nano [filename]`: Opens a simple text editor to create or edit files.
- `install [filename]`: Save the content of nano into a RMS File.
- `touch [file]` Clear file content, if not file specified clear content of nano
- `help`: Displays a list of available commands and usage information.
- `execute [command]`: Execute many command in a time, sepair it with _;_


