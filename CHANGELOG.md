OpenTTY Java Edition 1.18.1
Copyright (C) 2026 - Mr. Lima

---

## HEAD (unreleased) — work since tag 1.18

Novidades implementadas entre a tag `1.18` e o commit mais recente (`HEAD`). (Build atual ainda rotulado `2026-1.18.1-03x28`.)

### multi-tasking / process management

- `Process` agora tem seu próprio `Displayable screen` (registrado via `os.setproc("screen", ...)`) e **removidos os globais `midlet.stdout`/`midlet.stdin`** — cada processo tem seu próprio buffer de saída (per-process `StringBuffer`) e seu próprio `TextField` de entrada
- Novo rastreamento de sockets **por processo** (`Process.net`) substituindo o global `midlet.network`
- Novo **Task Manager** `graphics.taskmngr()` (List `"Running"` com `List.IMPLICIT`), enumerando todos os processos como `"title/name [pid]"`:
  - **Back**: se não houver mais processos, chama `destroyApp` (fecha o MIDlet); senão volta pra tela anterior
  - **Interrupt (SIGTERM)**: envia sinal 15 pro `sighandler` do processo selecionado e o remove da lista
  - **SELECT**: troca `display.setCurrent()` pra tela do processo (switching de processos)
- `os.setproc("title", ...)` define o título mostrado no Task Manager (`title [pid]` em vez de `name [pid]`)
- Escopo do processo clonado por `cloneScope()` em `os.popen`/`serve`, de modo que `su`/`cd` em um terminal não afetam os outros
- **`/bin/xterm`** (novo): o terminal emulador foi extraído do `init` — cria seu próprio stdout/stdin/tela, prompt `[USER@HOST PWD] #/$`, comando "Run" e botão "Switch to..." (task manager); exibe o `/etc/motd` ao abrir
- **`/bin/init`** simplificado: só monta fstab, configura env, `os.su`, roda `/home/.initrc` e chama `os.execute("xterm")`; também aceita `--serve=<program>` para spawnar daemons
- `graphics.display()` zera `kill` ao exibir uma `Displayable`, mantendo o runtime vivo com tela em foreground

### elf / rede

- **API de sockets completa** restaurada no emulador ELF:
  - `bind`: abre `StreamConnectionNotifier`/`DatagramConnection` na porta informada, retorna `-EADDRINUSE` em conflito
  - `listen`: marca socket como listening, com fallback de porta efêmera
  - `accept`: `acceptAndOpen`, novo fd com streams + peer `sockaddr_in` devolvido ao chamador
  - `sendto`/`recvfrom`: DGRAM via `DatagramConnection`, TCP via streams, com writeback do peer
  - `shutdown` e `nanosleep` restaurados (no-op de sucesso)
- **Socket options**: `setsockopt`/`getsockopt` com tabela de opções (`SO_REUSEADDR`, `SO_KEEPALIVE`, `SO_OOBINLINE`, `SO_BROADCAST`, `SO_DONTROUTE`, `SO_LINGER`, `SO_SNDBUF`, `SO_RCVBUF`, `TCP_NODELAY`); `SO_ERROR` e `SO_TYPE`; `-ENOPROTOOPT` p/ opções não suportadas
- Novas constantes: `SOL_SOCKET`, `SOL_IP`, `TCP_NODELAY`, `SO_*`; novos erros `ENOTSOCK`, `ENOPROTOOPT`, `EADDRINUSE`, `EADDRNOTAVAIL`, `EISCONN`
- Corrigido `writeSockAddr` (evita cast para `short` que quebrava o codegen do SDK)
- `connect` falho seta `socketInfo["error"] = 111`
- ELF `listdir` agora enxerga `/proc/` virtual (entradas de pid, `cpuinfo`, `meminfo`, `uptime`, `version`) via `midlet.procEntries()`
- Cleanup em shutdown também fecha conexões `datagram`

### filesystem / vfs / proc

- `/proc/` virtual: `uptime`, `version`, `meminfo` (agora usando `Runtime.totalMemory`, pois CLDC não tem `maxMemory`), `cpuinfo` e diretórios `/proc/<pid>/` com `status`, `cmdline`, `comm`, `stat`; usuários comuns só veem os próprios processos (root vê todos)
- `/root/` (índice 6 do `OpenRMS`) — diretório protegido, só root lê/escreve/entra; `rms` segue root-only
- Subdiretórios VFS sob `/bin/`, `/etc/`, `/lib/` com hashing estável (índices `>= 9`, `VFS_HASH_MOD=97`) e persistência em `/etc/vfs.conf` (restaurados no mount)
- `fstab` atualizado: inclui `root/` no mount raiz, e `mkdir`, `pkg`, `xterm` em `/bin/`

### shell / commands

- **`/bin/sh`** reduzido de 284 → 32 linhas: os builtins agora são tratados pelo kernel/`os.execute`; preserva `-c`, execução de arquivo e modo interativo
- **`/bin/pkg`** (reescrito, v1.6.0): usa `socket.http.get`/`rget` em vez de TCP cru; servidor `http://opentty.fun` (override `REPO`); comandos `install`, `remove`, `update`, `list`, `info`, `download`, `run`; mirror com 50+ pacotes
- **`/bin/yang`** enxuto: wrapper que repassa os args para `pkg`
- **`/bin/mkdir`** (novo): cria diretórios VFS; root-only sob `/bin`, `/etc`, `/lib`, `/root`
- **`/bin/lua`** agora executa arquivos diretamente (`lua <file>`)
- **`/bin/cp`** suporta modo de um argumento (`cp file` copia para `file-copy`)
- **`/bin/rm`** aceita `-r`/`-rf`/`-fr` e múltiplos arquivos
- **`/bin/nano`**: botão "Add new line" (p/ J2EMU) e "Switch to..." (task manager)
- **`/bin/curl`**: corrigido o parsing de URL (`sub(1,5)` para `"http:"`)

### novos apps /bin

- **`xterm`** — terminal emulador (ver multi-tasking)
- **`irc`** (`apps/net/irc.lua`) — cliente IRC com modo CLI (`connect`/`send`) e GUI, join/part/nick/PRIVMSG, PING/PONG, MOTD
- **`play`** (`apps/file/play.lua`) — player de áudio (play/stop/pause/resume/status/volume/list) acionando o daemon `audio-codec`; modo GUI
- **`tree`** (`apps/file/tree.lua`) — visualizador de árvore de diretórios (`-d`, `-L N`, `-a`, contagem)
- **`nginx`** (`apps/net/nginx/main.lua`) — servidor HTTP estilo nginx: config `/etc/nginx/nginx.conf`, `mime.types`, `sites-enabled/`, serve estáticos, `proxy_pass`, alias por location, logs de acesso/erro
- **`dns`** (`apps/net/dns/main.lua`) — daemon de DNS: lê `/etc/hosts` e zonas `/etc/dns/*.zone`, responde A/AAAA/MX/CNAME, estatísticas, `lookup`/`add`/`remove`/`reload`/`list`
- **`head`, `tail`, `netstat`** — implementados (head/tail imprimem as N primeiras/últimas linhas; netstat testa conectividade via HTTP GET)

### apps atualizados

- **`jdb`** (`apps/sys/benchmark/main.lua`, +388 linhas) — ponte de debug estilo adb: `ps`, `getproc`, `dumpsys`, `logcat`, `users`, `crash`, `stack`, `meminfo`, `shell`, `connect` (cliente TCP/UDP); modo servidor na porta 5555
- **`sudo`** — lê todos os args (`arg[1]` é o comando, o restante é repassado)
- **`docker`** — gerenciamento expandido de containers, imagens e scripts de init
- **`httpd`** (`res/lua/modules/httpd.lua`) — nova função `httpd.static(root_dir)` para servir arquivos estáticos com detecção de MIME

### lua runtime

- **otimizações de performance**:
  - `SMALL_NUMBERS[-128..1023]` — `Double`s pequenos em cache para loops/índices
  - `ScopeTable` (scope encadeado) em vez de clonar a tabela de globais a cada chamada de função
  - concatenação via `StringBuffer`; `Boolean` `TRUE/FALSE` estáticos; caching do tokenizador por fonte; early-returns em `getpattern`/`replace`/`escape`
- **erros / traceback**: novo `getTraceback(Throwable)` gera stack trace de `Frame` (nome, fonte, linha), `pointerBlock()` com `^---` e `(near '<token>')`, injetado em `run()`, `pcall`, handlers de `os.request`, threads de fundo e callbacks de UI
- **funções novas**: `string.startswith`, `string.endswith`, `table.pack`, `graphics.taskmngr`; `os.setproc` com atributos `"screen"`, `"title"`, `"stdout"`
- `os.scope()` sem args retorna o escopo atual; com tabela, troca o escopo (`father`)
- `os.execute` refatorado: suporte a `>`, `&&`, e `&` (background via classe nomeada `BGRunner` para evitar `NoClassDefFoundError` do preverifier)

### bug fixes

- `IMPLICT` → `IMPLICIT` no construtor de `List`
- `init` usa nome de comando puro para `exec` resolver `/bin/` corretamente
- `socket.http.rget` enviava POST em vez de GET (405 em downloads de pacote) — corrigido para GET
- `/proc/meminfo` usa `Runtime.totalMemory` em vez de `maxMemory`
- redirecionamento `>` não descartava mais os args antes do operador
- `rm` em subdiretórios VFS (exit 0 correto, `-r`)
- `os.mkdir`/`os.exit` silencioso; correção de cast em `deleteFile`; OOM handler com uso de memória
- `pkg`/`fetch_file` com prefixo `/apps/` no URL
- `_G` como global padrão em vez de `_ENV`

### build / toolchain

- **`build-elf.sh`** (novo) — monta `.s`/`.c` em ELF32 ARM para o emulador (opções `-o`, `-T`, `-lib`, `-entry`, `-keep`; validação via Python)
- Novos ELFs de teste: `netsock`, `netudp`, `whoami`, `cat` em `res/apps/dist/`
- `res/lib/lib32.s` expandido (+602 linhas); novos fontes `netsock.s`, `netudp.s`, `server.s`, `whoami.s`
- Removida a árvore `j2me-lib/` (stubs Android)

### deployment / infra

- **`Dockerfile`** (novo): PHP 8.3 FPM Alpine + nginx + supervisord; serviços php-fpm, nginx, mirror Python (`:31522`), pproxy (`:4096` + Flask web `:10141`)
- `docker/`: `nginx.conf`, `php.ini`, `supervisord.conf`; submodule `pproxy` em `.gitmodules`
- `krnl/`: kernel Python desktop reimplementando o runtime (`kernel.py`, `main.py`, `tkgui.py`, `lua/`)
- `index.php` expandido

### documentação / config

- `AGENTS.md`; novos `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `SECURITY.md`
- `docs/BUILD.md`, `docs/FILESYS.md`, `docs/USERS.md`, `docs/lua/README.md` expandidos; 5 exemplos novos em `docs/lua/examples/`
- Proxy/HOME_URL: `opentty.xyz` → `opentty.fun`; `RELEASE` muda de `"stable"` para `"mod"`
- `res/template.ini` novo

---

## Build 2026-1.18.1-03x28

filesystem / vfs

- Subdirectories inside `/bin/`, `/etc/`, `/lib/` are now supported as RMS mounts
- Each subdir (e.g. `/bin/tools/`) maps to its own page (index) of the `OpenRMS` store via a stable path hash (indices >= 6), keeping the every-store-in-OpenRMS model (no new RecordStores cluttering `/home/`)
- Read, write and delete now resolve nested paths at any depth (`/bin/tools/sub/file.lua`)
- Directory listing (`io.dirs` and ELF `getdents`) works for any `/bin|etc|lib/...` folder
- Writing into a subdir auto-registers it in the VFS (`cd` + `ls` detect it)
- `rms` handler can clear subdir stores (`rm -r /bin/tools`)
- Declare subdirs in `/etc/fstab` under their parent line using a trailing `/` (e.g. `tools/`)
- Refactored `addFile` to operate on a store index instead of a base string
- Root's home is now `/root/` (OpenRMS index 6, same top-level style as `/bin`→3, `/lib`→4, `/etc`→5); regular users cannot read, write or enter it, and `rms` stays root-only
- VFS hash subdirectories now use indices `>= 9` (`VFS_RESERVED` moved from 6 to 9), leaving indices 7 and 8 free for future top-level mounts without remapping
- `/root/` is declared in `/etc/fstab` root line, `su`, `chdir` and directory listing all enforce the root-home rule
- Fixed `rm` in `/bin|etc|lib/` subdirs: it no longer errors with exit code 5 (read-only) — `deleteFile` now removes a registered VFS subdirectory (clears its OpenRMS store and drops it from the `fs` table), and `rm` accepts `-r`/`-rf`/`-fr`
- Entering `/root/` without permission now returns exit code 13 and the shell prints `cd: <dir>: permission denied`
- Default shell home is now `/home/` for every user (including root after `su`); `/root/` remains as a protected directory but login/boot does not jump to it
- `/proc/` virtual filesystem: `cpuinfo`, `meminfo`, `uptime`, `version` plus per-process dirs `/proc/<pid>/` with `cmdline`, `comm`, `stat`, `status`; regular users only see/read their own processes (root sees all), enforced in file reads, directory listing and `cd`
- VFS subdirs under `/bin/`, `/etc/`, `/lib/` now persist across restarts into `/etc/vfs.conf` (written on mkdir, removed on delete/`rms`, restored on mount) so created folders survive closing the MIDlet

runtime / exit

- `os.exit()` no longer prints `java.lang.Error` on the terminal when it kills a process
- Process death via `os.exit` (any exit code), status abort and `Process killed` is now silent
- Real Lua errors and resource errors are still reported

shell / commands

- New `mkdir` command (`/bin/mkdir`) to create directories, including VFS subdirectories under `/bin/`, `/etc/`, `/lib/`
- `os.mkdir` now creates VFS subdirectory mounts (via `registerVfsDir`) in addition to `/mnt/`
- `mkdir` under `/bin/`, `/etc/`, `/lib/`, `/root/` is now root-only (exit 13 for regular users), consistent with write/delete rules

---

bug fixes

- fixed a bug in id, it can't retrieve id from root and another system virtual users

general

- Added Kernel request `netsh` to get openned objects
- New syscall added `nice` to change process priority
- New log manager `sys/smile/logs.lua`, install with `yang install log`
- Limited Lua cached tokens to 100 files
- Native Shell `os.execute(cmd)` still wrote in Java
- SheBang `#!/bin/sh` on `. [file]` run file with shell
- Added the **Add new line** button in Nano Editor
- Config. file `OpenRMS` doesnt appear in file listings
- New sh label
- Allowed multiple terminals

lua

- Added functions `string.startswith(s, pattern)` and `string.endswith(s, pattern)`
- Read a file or stream with chunck size `-1` will read until end of file/ connection end
- fixed `tonumber` invalid or missing value message

elf

- 

yang - package manager

- Updated to `1.5.1`
- New command `download [pkg] [file]` to download a package without install it
- New command `run [file]` to run installation scripts
- Fixed **not found** message that disappears from stdout
- Command `yang install *` install all available packages
- Indexed new package `du` 
- `yang` linked to `pkg` 
