<?php
$ua = $_SERVER['HTTP_USER_AGENT'] ?? '';
$oldDevices = ["Nokia", "Series40", "MIDP", "CLDC", "SonyEricsson", "S40", "Opera Mini", "UCWEB", "MicroMessenger"];

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
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>OpenTTY</title>

        <style>
            :root { --bg1:#0f172a; --bg2:#071033; --card:#0b1220; --accent:#7c3aed; --muted:#9aa4b2; --glass: rgba(255,255,255,0.03); --winbar:#1e293b; }

            * { box-sizing: border-box }
            html, body { height: 100%; margin: 0; font-family: Inter, ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, 'Helvetica Neue', Arial; }
            body { background: radial-gradient(1200px 600px at 10% 10%, rgba(124,58,237,0.12), transparent), linear-gradient(180deg,var(--bg1),var(--bg2)); color: #e6eef8; display: flex; align-items: center; justify-content: center; padding: 32px; }

            .card { width: min(880px,95%); background: linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01)); border: 1px solid rgba(255,255,255,0.04); backdrop-filter: blur(6px); padding: 28px 32px; border-radius: 18px; box-shadow: 0 30px 60px rgba(2,6,23,0.6), 0 6px 18px rgba(3,7,20,0.5); position: relative; overflow: hidden; }
            .header { display: flex; align-items: center; gap: 16px; margin-bottom: 18px } 
            .logo { width: 64px; height: 64px; border-radius: 12px; background: linear-gradient(135deg,var(--accent), #3b82f6); display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 22px; color: white; box-shadow: 0 8px 24px rgba(12,8,32,0.6) }
            
            h1 { margin: 0; font-size: 28px; letter-spacing: -0.5px }
            p.lead{ margin: 6px 0 0; color: var(--muted); font-size: 13px }

            .links { margin-top: 20px; display: grid; grid-template-columns: repeat(auto-fit, minmax(160px,1fr)); gap: 12px; }
            a.btn { display: inline-flex; align-items:center; gap:10px; padding:12px 14px; border-radius:12px; text-decoration:none; color:inherit; border:1px solid rgba(255,255,255,0.03); background: linear-gradient(180deg, rgba(255,255,255,0.015), rgba(255,255,255,0.006)); transition: transform .15s ease, box-shadow .15s ease, background .15s ease; box-shadow: 0 6px 18px rgba(2,6,23,0.45); font-weight: 600; font-size: 14px; }
            a.btn:hover { transform: translateY(-4px); box-shadow: 0 18px 40px rgba(12,8,32,0.6) }

            .meta { margin-top: 18px; color: var(--muted); font-size: 13px; display: flex; justify-content: space-between; gap: 16px; }

            .modal { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; z-index: 999; backdrop-filter: blur(4px); }
            .window { width: min(460px, 95%); background: var(--card); border-radius: 10px; box-shadow: 0 16px 40px rgba(0,0,0,0.6); border: 1px solid rgba(255,255,255,0.05); overflow: hidden; animation: fadeIn .2s ease-out; }
            .window-bar { background: var(--winbar); display: flex; align-items: center; justify-content: space-between; padding: 6px 10px; font-size: 13px; color: var(--muted); border-bottom: 1px solid rgba(255,255,255,0.05); }
            .window-bar .title { display: flex; align-items: center; gap: 6px; color: #cbd5e1; font-weight: 500; }
            .window-bar button { background: var(--accent); border: none; color: white; font-weight: bold; border-radius: 4px; padding: 0 8px; cursor: pointer; transition: background .15s; }
            .window-bar button:hover { background: #6d28d9; }
            .window-body { padding: 14px 16px; display: flex; flex-direction: column; gap: 8px; }

            .folder { display: flex; align-items: center; gap: 10px; padding: 8px 10px; border-radius: 8px; text-decoration: none; color: #e2e8f0; transition: background .15s; }
            .folder:hover { background: rgba(124,58,237,0.15); }
            .folder img { width: 22px; height: 22px; filter: brightness(0.9); }

            @keyframes fadeIn { from { opacity: 0; transform: scale(0.96); } to { opacity: 1; transform: scale(1); } }
            @media (max-width:420px){ 
                .header{ gap: 12px }
                .logo{ width: 52px; height: 52px; font-size: 18px }
                
                h1{ font-size: 20px }
            }
        </style>

        <script>
            function openProxy() { document.getElementById('proxyModal').style.display = 'flex'; document.getElementById('proxyURL').focus(); }
            function closeProxy() { document.getElementById('proxyModal').style.display = 'none'; } 

            function goProxy() { const url = document.getElementById('proxyURL').value.trim(); closeProxy(); if (url) { window.location.href = "/proxy.php?" + encodeURIComponent(url); }  }
        </script>
    </head>
    <body>
        <main class="card" role="main" aria-labelledby="title">
            <header class="header">
                <div class="logo" aria-hidden><img src="src/res/img/sh.png" height="32" width="32"></div>
                <div>
                    <h1 id="title">OpenTTY</h1>
                    <p class="lead">Services Center</p>
                </div>
            </header>

            <nav class="links" aria-label="Tools">
                <a class="btn" href="/assets/"><span>🔁</span><span>Mirror</span></a>
                <a class="btn" href="/dist/"><span>⬇️</span><span>Downloads</span></a>
                <a class="btn" href="http://gitea.opentty.xyz"><span>📝</span><span>Gitea</span></a>
                <a class="btn" href="https://github.com/mrlima4095/OpenTTY-J2ME"><span>📦</span><span>Repository</span></a>
                <a class="btn" href="/cli"><span>🌐</span><span>WebProxy</span></a>
                <a class="btn" href="javascript:proxy()"><span>🔐</span><span>HTTPS Proxy</span></a>
                <a class="btn" href="https://github.com/mrlima4095/OpenTTY-J2ME/wiki"><span>📚</span><span>Documentation</span></a>
                <a class="btn" href="/lts/"><span>⏱️</span><span>LTS Source</span></a>
            </nav>

            <div class="meta">
                <div>OpenTTY • J2ME</div>
                <div>© <span id="year"></span></div>
            </div>
        </main>

        <div id="proxyModal" class="modal" style="display:none;">
            <div class="window">
                <div class="window-bar">
                    <span class="title">🔐 HTTPS Proxy</span>
                    <button onclick="closeProxy()">✕</button>
                </div>
                <div class="window-body">
                    <label for="proxyURL" style="font-size:14px; color:#cbd5e1;">Enter URL:</label>
                    <input id="proxyURL" type="text" placeholder="https://example.com" style="padding:8px 10px; border-radius:8px; border:1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.02); color:#e6eef8; width:100%; margin-bottom:12px;">
                    <button onclick="goProxy()" style="background:var(--accent); color:white; padding:8px 12px; border-radius:8px; border:none; font-weight:600; cursor:pointer;">Connect</button>
                </div>
            </div>
        </div>

        <script>document.getElementById('year').textContent = new Date().getFullYear();</script>
    </body>
    </html>
    <?php
}
