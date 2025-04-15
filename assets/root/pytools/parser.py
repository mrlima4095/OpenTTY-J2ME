def loadconfig(self, filename): # Load a configuration from a file
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