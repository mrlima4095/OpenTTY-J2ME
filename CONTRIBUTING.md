# 🤝 Contributing to OpenTTY

First off, thanks for taking the time to contribute! 🎉

## 📋 Table of Contents

- [🤝 Contributing to OpenTTY](#-contributing-to-opentty)
  - [📋 Table of Contents](#-table-of-contents)
  - [📜 Code of Conduct](#-code-of-conduct)
  - [💡 How Can I Contribute?](#-how-can-i-contribute)
    - [🐛 Reporting Bugs](#-reporting-bugs)
  - [Bug Report](#bug-report)

## 📜 Code of Conduct

This project and everyone participating in it is governed by the
[OpenTTY Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are
expected to uphold this code. Please report unacceptable behavior to
**opentty@proton.me**.

## 💡 How Can I Contribute?

### 🐛 Reporting Bugs

Before creating bug reports, please check the issue tracker as you might find
out that you don't need to create one. When you are creating a bug report,
please include as many details as possible:

* 🔍 **Use a clear and descriptive title**
* 📝 **Describe the exact steps to reproduce the problem**
* 🎯 **Describe the behavior you observed after following the steps**
* ✅ **Explain which behavior you expected to see instead and why**
* 📸 **Include screenshots/error messages if possible**
* 💻 **Tell us which JVM/MIDP environment you're using**
* 🔄 **Tell us what version of OpenTTY you're running**

**Example:**
```markdown
## Bug Report

**OpenTTY Version:** 2026-1.18.1

**Environment:** Nokia S40, J2ME Loader 1.7.2

**Description:**
When I run `/bin/init`, I get a Kernel Panic with error:
`java.lang.ArrayIndexOutOfBoundsException at ELF.java:2456`

**Steps to Reproduce:**
1. Install OpenTTY
2. Launch the MIDlet
3. See error immediately

**Expected Behavior:**
OpenTTY should boot normally to the console.