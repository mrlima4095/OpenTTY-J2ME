#!/usr/bin/env python3
"""Allow `python -m krnl` to run the desktop launcher."""

import sys

from krnl.main import main

if __name__ == "__main__":
    sys.exit(main())