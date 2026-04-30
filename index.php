<?php
$ua = $_SERVER['HTTP_USER_AGENT'] ?? '';
$oldDevices = ["Nokia", "Series40", "MIDP", "CLDC", "SonyEricsson", "S40", "S60", "Opera Mini", "UCWEB", "MicroMessenger"];

$isOld = false;
foreach ($oldDevices as $keyword) {
    if (stripos($ua, $keyword) !== false) {
        $isOld = true;
        break;
    }
}
header('Content-Type: text/html; charset=UTF-8');

if ($isOld) {
    ?>
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <title>OpenTTY</title>
        <style>
            body, html { background: #000; color: #fff; font-family: monospace; padding: 20px; }
            .card, .header, .links, .meta { all: unset; }
            a { color: #6cf; text-decoration: none; display: block; margin: 6px 0; }
        </style>
    </head>
    <body>
        <h1>OpenTTY</h1>
        <p>Terminal Emulator for J2ME Phones</p><br>
        <a href="/assets/">Mirror</a>
        <a href="/dist/">Downloads</a>
        <a href="https://github.com/mrlima4095/OpenTTY-J2ME">Repository</a>
    </body>
    </html>
    <?php
} 
else {
    ?>
    <!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>OpenTTY · Modern Terminal for J2ME</title>
  <!-- Bootstrap 5 + Icons + Fonts -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:opsz,wght@14..32,400;14..32,500;14..32,600;14..32,700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }

    body {
      font-family: 'Inter', sans-serif;
      background: #0b0e14;
      background-image: radial-gradient(circle at 10% 20%, rgba(30, 40, 60, 0.4) 0%, rgba(5, 8, 12, 0.95) 90%);
      color: #eef3ff;
      scroll-behavior: smooth;
    }

    /* terminal-like accent */
    :root {
      --glow-cyan: #2dd4bf;
      --glow-blue: #3b82f6;
      --dark-card: #11161f;
      --border-dim: #2a3342;
    }

    /* navbar glassmorphism */
    .navbar-glass {
      backdrop-filter: blur(12px);
      background: rgba(10, 14, 23, 0.75);
      border-bottom: 1px solid rgba(45, 212, 191, 0.25);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
    }

    .btn-outline-terminal {
      border: 1px solid var(--glow-cyan);
      color: var(--glow-cyan);
      background: transparent;
      transition: all 0.2s ease;
      font-weight: 500;
      letter-spacing: 0.3px;
    }

    .btn-outline-terminal:hover {
      background: var(--glow-cyan);
      color: #0a0f1a;
      box-shadow: 0 0 12px rgba(45, 212, 191, 0.6);
      border-color: var(--glow-cyan);
    }

    .btn-solid-cyan {
      background: linear-gradient(95deg, #2dd4bf, #1e8f8a);
      border: none;
      color: #0b0f17;
      font-weight: 600;
      transition: 0.2s;
      box-shadow: 0 4px 12px rgba(45, 212, 191, 0.3);
    }

    .btn-solid-cyan:hover {
      transform: translateY(-2px);
      background: linear-gradient(95deg, #22c5b0, #1a7d78);
      box-shadow: 0 8px 20px rgba(45, 212, 191, 0.5);
      color: #000;
    }

    .hero-section {
      padding: 6rem 0 5rem 0;
      position: relative;
      overflow: hidden;
    }

    .hero-section::before {
      content: "⚡";
      font-size: 380px;
      opacity: 0.03;
      position: absolute;
      bottom: -80px;
      right: -60px;
      pointer-events: none;
      font-family: monospace;
    }

    .code-badge {
      background: #0f121b;
      border-left: 4px solid var(--glow-cyan);
      font-family: 'JetBrains Mono', monospace;
      font-size: 0.85rem;
      padding: 0.6rem 1rem;
      border-radius: 12px;
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.4);
    }

    .feature-card {
      background: rgba(18, 24, 34, 0.7);
      backdrop-filter: blur(4px);
      border: 1px solid #2a3442;
      border-radius: 28px;
      transition: transform 0.2s, border-color 0.2s;
      height: 100%;
    }

    .feature-card:hover {
      border-color: #2dd4bf80;
      transform: translateY(-5px);
      background: rgba(28, 36, 48, 0.85);
    }

    .btn-outline-dim {
      border: 1px solid #3e4a5d;
      color: #cbd5f0;
      background: transparent;
      transition: 0.2s;
    }

    .btn-outline-dim:hover {
      border-color: var(--glow-cyan);
      color: var(--glow-cyan);
      background: rgba(45, 212, 191, 0.1);
    }

    .terminal-window {
      background: #03060c;
      border-radius: 18px;
      border: 1px solid #2b3548;
      box-shadow: 0 20px 35px -12px black;
      font-family: 'JetBrains Mono', monospace;
    }

    .terminal-header {
      background: #121722;
      padding: 10px 16px;
      border-top-left-radius: 18px;
      border-top-right-radius: 18px;
      border-bottom: 1px solid #2b3548;
    }

    .window-dot {
      height: 12px;
      width: 12px;
      border-radius: 50%;
      display: inline-block;
      margin-right: 6px;
    }

    .footer-links a {
      color: #a3b3d6;
      text-decoration: none;
      transition: 0.2s;
    }

    .footer-links a:hover {
      color: var(--glow-cyan);
    }

    pre, .terminal-body {
      font-family: 'JetBrains Mono', monospace;
    }

    .btn-icon-group {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      justify-content: center;
    }

    .btn {
      border-radius: 60px;
      padding: 0.6rem 1.4rem;
      font-weight: 500;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .btn i {
      font-size: 1.2rem;
    }

    @media (max-width: 640px) {
      .hero-section {
        padding: 3.5rem 0 3rem;
      }
      .btn {
        padding: 0.5rem 1rem;
        font-size: 0.85rem;
      }
    }
  </style>
</head>
<body>

<!-- Navigation -->
<nav class="navbar navbar-expand-lg navbar-dark navbar-glass fixed-top">
  <div class="container">
    <a class="navbar-brand fw-bold fs-3" href="#">
      <span class="text-cyan" style="color:#2dd4bf;">◢</span> OpenTTY
    </a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarMain" aria-controls="navbarMain" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarMain">
      <ul class="navbar-nav ms-auto mb-2 mb-lg-0 gap-2">
        <li class="nav-item"><a class="nav-link" href="#features">Features</a></li>
        <li class="nav-item"><a class="nav-link" href="#tech">Tech stack</a></li>
        <li class="nav-item"><a class="nav-link" href="#about">About</a></li>
        <li class="nav-item"><a class="nav-link" href="#downloads">Get started</a></li>
      </ul>
    </div>
  </div>
</nav>

<!-- Hero -->
<section class="hero-section">
  <div class="container">
    <div class="row align-items-center g-5">
      <div class="col-lg-6">
        <div class="mb-3">
          <span class="badge bg-dark text-cyan border border-cyan px-3 py-2 rounded-pill" style="border-color:#2dd4bf80 !important;">
            <i class="bi bi-terminal-fill me-1"></i> J2ME · ARM32 Emulation
          </span>
        </div>
        <h1 class="display-4 fw-bold mb-3" style="letter-spacing: -0.02em;">
          Run ELF & Lua<br> inside <span class="text-gradient" style="color:#2dd4bf;">retro terminals</span>
        </h1>
        <p class="lead text-light-emphasis opacity-75 mb-4">
          OpenTTY brings a full Unix-like environment to J2ME feature phones. Execute ARM32 binaries, Lua scripts, manage processes, and experience modern CLI on old devices.
        </p>
        <div class="btn-icon-group mb-4">
          <a class="btn btn-outline-terminal" href="/dist/"><i class="bi bi-download"></i><span>Downloads</span></a>
          <a class="btn btn-outline-terminal" href="http://git.opentty.xyz"><i class="bi bi-code-square"></i><span>Gitea</span></a>
          <a class="btn btn-outline-terminal" href="https://github.com/mrlima4095/OpenTTY-J2ME"><i class="bi bi-github"></i><span>Repository</span></a>
          <a class="btn btn-outline-terminal" href="/cli"><i class="bi bi-globe2"></i><span>WebProxy</span></a>
        </div>
        <div class="code-badge d-inline-flex mt-2">
          <i class="bi bi-cpu me-2 text-cyan"></i> 
          <span>Lua J2ME · ELF loader · Dynamic linking</span>
        </div>
      </div>
      <div class="col-lg-6">
        <div class="terminal-window">
          <div class="terminal-header d-flex align-items-center">
            <span class="window-dot" style="background:#ff5f56;"></span>
            <span class="window-dot" style="background:#ffbd2e;"></span>
            <span class="window-dot" style="background:#27c93f;"></span>
            <span class="ms-2 small text-secondary">opentty@j2me:~/sandbox</span>
          </div>
          <div class="terminal-body p-3" style="min-height: 260px; background:#0b0e12;">
            <pre style="background: transparent; border: none; color:#bbd4ff; margin:0; font-size:0.8rem;">
<span style="color:#2dd4bf;">$</span> uname -a
Linux opentty 3.2.0 #1 armv5tejl OpenTTY

<span style="color:#2dd4bf;">$</span> ./hello.elf
Hello from ARM ELF on J2ME!

<span style="color:#2dd4bf;">$</span> lua -e 'print(os.date())'
Tue Apr 30 16:20:01 2026

<span style="color:#2dd4bf;">$</span> ps
PID  PROCESS
1    init
142  lua script.lua
<span style="color:#9cdcfe;">status: running</span></pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- feature grid -->
<section id="features" class="py-5">
  <div class="container py-4">
    <div class="text-center mb-5">
      <h2 class="fw-bold">Designed for constrained devices,<br> <span style="color:#2dd4bf;">powerful emulation</span></h2>
      <p class="mx-auto col-lg-7 text-secondary">OpenTTY provides an unprecedented experience on J2ME phones: ELF loader, dynamic linking, POSIX syscalls, and full Lua environment.</p>
    </div>
    <div class="row g-4">
      <div class="col-md-6 col-lg-4">
        <div class="feature-card p-4">
          <i class="bi bi-file-binary fs-1 text-cyan" style="color:#2dd4bf;"></i>
          <h4 class="mt-3 fw-semibold">ARM32 ELF Loader</h4>
          <p class="text-secondary">Full ELF interpreter for ARM executables. Support for PT_LOAD, dynamic sections, GOT/PLT lazy binding, and syscall translation layer.</p>
        </div>
      </div>
      <div class="col-md-6 col-lg-4">
        <div class="feature-card p-4">
          <i class="bi bi-lua fs-1 text-cyan" style="color:#2dd4bf;"></i>
          <h4 class="mt-3 fw-semibold">Lua 5.x Runtime</h4>
          <p class="text-secondary">Complete Lua interpreter with coroutines, tables, metatables, and custom modules: `os`, `io`, `socket`, `graphics`, `audio`, `push` API.</p>
        </div>
      </div>
      <div class="col-md-6 col-lg-4">
        <div class="feature-card p-4">
          <i class="bi bi-hdd-stack fs-1 text-cyan" style="color:#2dd4bf;"></i>
          <h4 class="mt-3 fw-semibold">Virtual FS & RMS</h4>
          <p class="text-secondary">In-memory filesystem, persistent RMS storage, `/bin`, `/etc`, `/home`, and real FileConnection for memory cards. Full Unix-like paths.</p>
        </div>
      </div>
      <div class="col-md-6 col-lg-4">
        <div class="feature-card p-4">
          <i class="bi bi-wifi fs-1 text-cyan" style="color:#2dd4bf;"></i>
          <h4 class="mt-3 fw-semibold">Network Sockets</h4>
          <p class="text-secondary">TCP/UDP socket support, HTTP client, socket server API. Connect, bind, send/receive syscalls mapped to J2ME GCF.</p>
        </div>
      </div>
      <div class="col-md-6 col-lg-4">
        <div class="feature-card p-4">
          <i class="bi bi-display fs-1 text-cyan" style="color:#2dd4bf;"></i>
          <h4 class="mt-3 fw-semibold">GUI & Graphics</h4>
          <p class="text-secondary">Interactive UI via `graphics` module: Forms, Lists, TextFields, Images, Commands with callback handlers, LCDUI integration.</p>
        </div>
      </div>
      <div class="col-md-6 col-lg-4">
        <div class="feature-card p-4">
          <i class="bi bi-shield-lock fs-1 text-cyan" style="color:#2dd4bf;"></i>
          <h4 class="mt-3 fw-semibold">Process isolation</h4>
          <p class="text-secondary">Multi-process model with UIDs, signalling, priority handling, and kernel IPC via request() mechanism. Sandboxed execution.</p>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- Technical deep dive (ELF + dynamic linking) -->
<section id="tech" class="py-5 bg-dark bg-opacity-25">
  <div class="container">
    <div class="row align-items-center">
      <div class="col-lg-6 mb-4 mb-lg-0">
        <h2 class="fw-bold mb-3"><i class="bi bi-layers me-2" style="color:#2dd4bf;"></i> Hybrid execution engine</h2>
        <p>The core of OpenTTY includes a lightweight ARM32 interpreter that reads ELF binaries, relocates symbols, resolves dynamic libraries, and traps syscalls. Combined with the Lua runtime, it offers both compiled C applications and scripting.</p>
        <ul class="list-unstyled mt-3">
          <li class="mb-2"><i class="bi bi-check-lg text-cyan me-2"></i> <strong>Dynamic Linking:</strong> DT_NEEDED, GOT/PLT, lazy resolution, R_ARM_JUMP_SLOT</li>
          <li class="mb-2"><i class="bi bi-check-lg text-cyan me-2"></i> <strong>Full ARM Thumb/ARM mode:</strong> Data processing, load/store, branch, SWI syscalls</li>
          <li class="mb-2"><i class="bi bi-check-lg text-cyan me-2"></i> <strong>Signal handling & setjmp/longjmp</strong></li>
          <li class="mb-2"><i class="bi bi-check-lg text-cyan me-2"></i> <strong>POSIX emulation:</strong> open, read, write, mmap, fork, execve, socket, futex</li>
        </ul>
        <a href="https://github.com/mrlima4095/OpenTTY-J2ME" class="btn btn-outline-terminal mt-2"><i class="bi bi-github"></i> Explore source code</a>
      </div>
      <div class="col-lg-6">
        <div class="bg-black rounded-4 p-4" style="border: 1px solid #2a3342;">
          <h5 class="mb-3"><i class="bi bi-cpu"></i> Dynamic relocation demo</h5>
          <pre style="font-size: 0.75rem; background:#0c0f15; padding: 1rem; border-radius: 16px; overflow-x: auto;"><span style="color:#6aa9ff;">// ELF relocation handling (Java)</span>
<span style="color:#2dd4bf;">switch</span>(type) {
  <span style="color:#2dd4bf;">case</span> R_ARM_JUMP_SLOT:
    setupLazyBinding(gotOffset, symIndex);
    <span style="color:#6aa9ff;">break;</span>
  <span style="color:#2dd4bf;">case</span> R_ARM_GLOB_DAT:
    symAddr = resolveSymbol(symName);
    writeIntLE(memory, offset, symAddr);
    <span style="color:#6aa9ff;">break;</span>
}
<span style="color:#2dd4bf;">// PLT resolver stub</span>
<span style="color:#d4d4d4;">0xe51ff004 : ldr pc, [pc, #-4]</span></pre>
          <div class="small text-secondary mt-2">OpenTTY implements both REL and RELA relocations, dynamic symbol table, and a generic syscall dispatcher with 70+ Linux EABI syscalls.</div>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- about + architecture quotes -->
<section id="about" class="py-5">
  <div class="container">
    <div class="row g-5">
      <div class="col-md-6">
        <div class="p-4 rounded-4 bg-dark bg-opacity-30 border border-secondary border-opacity-25 h-100">
          <i class="bi bi-quote fs-1 text-cyan opacity-50"></i>
          <p class="mt-2 fs-5 fst-italic">"OpenTTY brings the Unix philosophy to feature phones. It's more than a terminal — it's a full development and runtime environment for ARM binaries and Lua, all running inside the JVM."</p>
          <hr class="my-3 opacity-25">
          <div class="d-flex align-items-center gap-3">
            <div style="width:48px;height:48px;background:#1e293b; border-radius: 99px; display:flex; align-items:center; justify-content:center;"><i class="bi bi-code-slash fs-4"></i></div>
            <div><strong>J2ME ecosystem</strong><br>MIDP 2.0 / CLDC 1.1 compatible</div>
          </div>
        </div>
      </div>
      <div class="col-md-6">
        <div class="p-4 rounded-4 bg-dark bg-opacity-30 border border-secondary border-opacity-25 h-100">
          <i class="bi bi-braces fs-1 text-cyan opacity-50"></i>
          <h5 class="mt-2">Modular design</h5>
          <p>ELF loader – independent bytecode interpreter – posix file abstraction – Lua FFI – graphics toolkit. Everything structured for extensions and lightweight memory usage (dynamic heap, 1MB base memory).</p>
          <div class="mt-3 small">
            <span class="badge bg-dark me-1">#ARM32</span>
            <span class="badge bg-dark me-1">#LuaJIT-like</span>
            <span class="badge bg-dark me-1">#RecordStoreFS</span>
            <span class="badge bg-dark">#Socket API</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- download & resources -->
<section id="downloads" class="py-5" style="background: linear-gradient(115deg, #0b1118 0%, #0f1622 100%);">
  <div class="container text-center">
    <h2 class="fw-bold mb-3">Start building with OpenTTY</h2>
    <p class="mb-4 col-lg-7 mx-auto">Get the latest release, browse the repository, or test the web proxy. All resources are open-source and community-driven.</p>
    <div class="d-flex flex-wrap justify-content-center gap-3 mb-5">
      <a href="/dist/" class="btn btn-solid-cyan px-4 py-2"><i class="bi bi-box-seam"></i> Download stable .jar</a>
      <a href="https://github.com/mrlima4095/OpenTTY-J2ME/releases" class="btn btn-outline-terminal"><i class="bi bi-tag"></i> Releases</a>
      <a href="/cli" class="btn btn-outline-dim"><i class="bi bi-terminal"></i> Live WebProxy (demo)</a>
    </div>
    <div class="row justify-content-center">
      <div class="col-md-8">
        <div class="terminal-window mb-4">
          <div class="terminal-header ps-3 py-2"><span class="text-white-50">Quick setup</span></div>
          <div class="terminal-body p-3 text-start">
            <pre style="margin:0; font-size: 0.8rem;"># compile & deploy via Ant / J2ME SDK
$ ant jar
$ openttty install /dist/OpenTTY.jar

# or upload directly to J2ME phone via OTA
# browse internal RMS programs:</pre>
          </div>
        </div>
        <div class="mt-3">
          <a href="http://git.opentty.xyz" class="text-decoration-none me-3"><i class="bi bi-git"></i> Git repository (Gitea)</a>
          <a href="/assets/" class="text-decoration-none"><i class="bi bi-archive"></i> Assets & mirrors</a>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- footer -->
<footer class="pt-5 pb-4 border-top border-secondary border-opacity-25">
  <div class="container">
    <div class="row gy-4">
      <div class="col-md-5">
        <a class="navbar-brand fw-bold fs-3" href="#"><span style="color:#2dd4bf;">❯❯</span> OpenTTY</a>
        <p class="small text-secondary mt-2">Modern UNIX-like environment for J2ME phones. ARM ELF loader, Lua scripting, process isolation, networking, and GUI.</p>
        <div class="d-flex gap-3 footer-links">
          <a href="https://github.com/mrlima4095/OpenTTY-J2ME"><i class="bi bi-github"></i> GitHub</a>
          <a href="http://git.opentty.xyz"><i class="bi bi-gitlab"></i> Gitea</a>
          <a href="/cli"><i class="bi bi-globe"></i> Proxy</a>
        </div>
      </div>
      <div class="col-md-3 offset-md-1">
        <h6 class="fw-semibold">Resources</h6>
        <ul class="list-unstyled footer-links small">
          <li class="mb-1"><a href="/assets/">🔁 Mirror repository</a></li>
          <li class="mb-1"><a href="/dist/">⬇️ Downloads / JAR</a></li>
          <li class="mb-1"><a href="https://github.com/mrlima4095/OpenTTY-J2ME/issues">Report issues</a></li>
          <li><a href="/docs">Documentation (wiki)</a></li>
        </ul>
      </div>
      <div class="col-md-3">
        <h6 class="fw-semibold">Community</h6>
        <ul class="list-unstyled footer-links small">
          <li class="mb-1"><i class="bi bi-discord me-1"></i> Discord / IRC</li>
          <li class="mb-1"><i class="bi bi-envelope"></i> open@syst3m.xyz</li>
          <li><span class="text-muted">Build: 2026-1.18.1-03x27</span></li>
        </ul>
      </div>
    </div>
    <div class="text-center text-secondary small pt-4 mt-3 border-top border-secondary border-opacity-25">
      &copy; 2026 OpenTTY contributors — made for J2ME, inspired by Unix & retro computing.
    </div>
  </div>
</footer>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
    <?php
}
