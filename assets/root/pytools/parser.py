#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
#	NOTE: This file is a Python library
#
#		Parse INI files to JSON and Python dictionaries
#	You can use it to parse OpenTTY Packages to JSON if 
#	by a fuck you need

import json

def ini2json(filename): 
	config_dict = {}

	with open(filename, "r") as file:
		for line in file:
			line = line.strip()
			if line.startswith("[") and line.endswith("]"):
				current_section = line[1:-1]
				config_dict[current_section] = {}
			elif "=" in line and current_section is not None:
				key, value = line.split("=", 1)
				config_dict[current_section][key.strip()] = value.strip()

def parse2file(config_dict, filename):
	json.dump(config_dict, filename, ensure_ascii=False, indent=4)