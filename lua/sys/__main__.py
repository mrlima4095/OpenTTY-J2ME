#!/usr/bin/env python3
"""Allow `python -m lua.sys` to run the desktop launcher."""

import sys

from lua.sys.main import main

if __name__ == "__main__":
    sys.exit(main())