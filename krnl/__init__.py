# OpenTTY Python 1.18 — desktop kernel (process / filesystem / services / users).
# Rebuild of the src/ (J2ME) OpenTTY kernel on top of the Python Lua runtime in lua/.

__version__ = "1.18.1"
__build__ = "2026-1.18.1-python"

from .kernel import OpenTTYKernel, OpenTTYRuntime, KernelProcess, KernelHandler  # noqa: E402, F401

__all__ = ["OpenTTYKernel", "OpenTTYRuntime", "KernelProcess", "KernelHandler", "__version__", "__build__"]