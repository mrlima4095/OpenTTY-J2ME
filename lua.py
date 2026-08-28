#!/usr/bin/env python3
import sys

sys.path.insert(0, __import__('os').path.dirname((__import__('os').path.abspath(__file__))))

from lua.run import main

if __name__ == "__main__":
    sys.exit(main())