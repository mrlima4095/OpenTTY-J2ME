<?php
session_start();

// Configurações do SSH (movido para arquivo separado por segurança)
$config = [
    'host' => '192.168.1.100',
    'port' => 22,
    'user' => 'usuario',
    'password' => 'senha'
];

class WebSSHTerminal {
    private $connection;
    private $current_dir;
    private $history_file;
    
    public function __construct($config) {
        $this->current_dir = $_SESSION['current_dir'] ?? '/home/' . $config['user'];
        $this->history_file = sys_get_temp_dir() . '/ssh_history_' . session_id();
        
        // Conectar e autenticar
        $this->connection = ssh2_connect($config['host'], $config['port']);
        if (!$this->connection || !ssh2_auth_password($this->connection, $config['user'], $config['password'])) {
            die("ERRO: Não foi possível conectar ao servidor SSH\n");
        }
    }
    
    public function executeCommand($command) {
        if (empty($command)) return '';
        
        // Comandos internos
        if ($command === 'clear' || $command === 'cls') {
            return 'CLEAR_SCREEN';
        }
        
        if ($command === 'exit' || $command === 'quit') {
            session_destroy();
            header('Location: ' . $_SERVER['PHP_SELF']);
            exit;
        }
        
        // Comando cd (change directory)
        if (preg_match('/^cd\s+(.+)$/', $command, $matches)) {
            return $this->changeDirectory($matches[1]);
        }
        
        // Comando pwd interno
        if ($command === 'pwd') {
            return $this->current_dir . "\n";
        }
        
        // Executar comando via SSH
        $full_command = "cd " . escapeshellarg($this->current_dir) . " 2>&1 && ";
        $full_command .= $command . " 2>&1; echo 'EXIT_CODE:'$?";
        
        $stream = ssh2_exec($this->connection, $full_command);
        stream_set_blocking($stream, true);
        $output = stream_get_contents($stream);
        fclose($stream);
        
        // Verificar exit code
        if (preg_match('/EXIT_CODE:(\d+)$/', $output, $matches)) {
            $exit_code = $matches[1];
            $output = preg_replace('/EXIT_CODE:\d+$/', '', $output);
            if ($exit_code != 0) {
                $output .= "\n[Erro: Código de saída $exit_code]";
            }
        }
        
        return rtrim($output);
    }
    
    private function changeDirectory($path) {
        $new_dir = $path;
        
        if ($new_dir === '..') {
            $new_dir = dirname($this->current_dir);
        } elseif ($new_dir === '~') {
            $new_dir = '/home/' . explode('@', $config['user'])[0];
        } elseif ($new_dir[0] !== '/') {
            $new_dir = $this->current_dir . '/' . $new_dir;
        }
        
        // Verificar se o diretório existe via SSH
        $test_command = "cd " . escapeshellarg($new_dir) . " 2>&1 && echo 'OK'";
        $stream = ssh2_exec($this->connection, $test_command);
        stream_set_blocking($stream, true);
        $result = stream_get_contents($stream);
        fclose($stream);
        
        if (trim($result) === 'OK') {
            $this->current_dir = realpath($new_dir) ?: $new_dir;
            $_SESSION['current_dir'] = $this->current_dir;
            return "Diretório alterado para: " . $this->current_dir . "\n";
        } else {
            return "Erro: Diretório não encontrado\n";
        }
    }
    
    public function getCurrentDir() {
        return $this->current_dir;
    }
    
    public function __destruct() {
        if ($this->connection) {
            ssh2_disconnect($this->connection);
        }
    }
}

// Processar comando
$output = '';
$command = $_POST['command'] ?? '';

if ($command !== '') {
    $terminal = new WebSSHTerminal($config);
    $output = $terminal->executeCommand($command);
    
    if ($output === 'CLEAR_SCREEN') {
        $_SESSION['history'] = [];
        $output = '';
    } else {
        $_SESSION['history'][] = ['command' => $command, 'output' => $output, 'time' => time()];
    }
    
    // Limitar histórico
    if (count($_SESSION['history']) > 200) {
        array_shift($_SESSION['history']);
    }
}

$current_dir = $_SESSION['current_dir'] ?? '/home/' . $config['user'];
$current_user = $config['user'];
$hostname = gethostname();

?>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Web Terminal SSH</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            background: #0a0e0a;
            color: #33ff33;
            font-family: 'Courier New', 'Lucida Console', monospace;
            font-size: 14px;
            padding: 15px;
        }
        
        .container {
            max-width: 100%;
            background: #000000;
            border: 2px solid #33ff33;
            border-radius: 5px;
            padding: 15px;
            box-shadow: 0 0 10px rgba(51, 255, 51, 0.3);
        }
        
        .terminal-header {
            border-bottom: 1px solid #33ff33;
            padding-bottom: 8px;
            margin-bottom: 15px;
            font-weight: bold;
        }
        
        .terminal-output {
            white-space: pre-wrap;
            word-wrap: break-word;
            margin-bottom: 15px;
            max-height: 70vh;
            overflow-y: auto;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            line-height: 1.4;
        }
        
        .command-line {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            margin-top: 10px;
            border-top: 1px solid #33ff33;
            padding-top: 10px;
        }
        
        .prompt {
            color: #33ff33;
            font-weight: bold;
            margin-right: 8px;
            white-space: nowrap;
        }
        
        .prompt-user {
            color: #00cc00;
        }
        
        .prompt-path {
            color: #33ff33;
        }
        
        form {
            flex: 1;
            min-width: 150px;
        }
        
        input {
            background: transparent;
            border: none;
            color: #33ff33;
            font-family: 'Courier New', monospace;
            font-size: 14px;
            width: 100%;
            outline: none;
            padding: 5px 0;
        }
        
        input:focus {
            border-bottom: 1px solid #33ff33;
        }
        
        .button-group {
            margin-top: 15px;
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
        }
        
        button {
            background: #1a1f1a;
            color: #33ff33;
            border: 1px solid #33ff33;
            padding: 5px 12px;
            cursor: pointer;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            transition: all 0.2s;
        }
        
        button:hover {
            background: #33ff33;
            color: #000000;
        }
        
        .status-bar {
            font-size: 11px;
            color: #669966;
            margin-top: 10px;
            padding-top: 5px;
            border-top: 1px solid #1a3f1a;
        }
        
        @media (max-width: 600px) {
            body {
                padding: 5px;
            }
            .container {
                padding: 10px;
            }
            .terminal-output {
                font-size: 11px;
            }
            input {
                font-size: 12px;
            }
        }
        
        ::-webkit-scrollbar {
            width: 8px;
            background: #000;
        }
        
        ::-webkit-scrollbar-thumb {
            background: #33ff33;
            border-radius: 4px;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="terminal-header">
        🔌 Web Terminal SSH | <?php echo htmlspecialchars($current_user); ?>@<?php echo htmlspecialchars($hostname); ?>
    </div>
    
    <div class="terminal-output" id="terminal-output">
        <?php 
        if (isset($_SESSION['history']) && is_array($_SESSION['history'])) {
            foreach ($_SESSION['history'] as $entry) {
                $prompt_display = $current_user . '@' . $hostname . ':' . $current_dir . '$ ';
                echo '<span style="color:#00cc00">' . htmlspecialchars($prompt_display) . '</span>';
                echo htmlspecialchars($entry['command']) . "\n";
                
                $output_lines = explode("\n", $entry['output']);
                foreach ($output_lines as $line) {
                    if (trim($line) !== '') {
                        echo htmlspecialchars($line) . "\n";
                    }
                }
                echo "\n";
            }
        }
        ?>
    </div>
    
    <div class="command-line">
        <span class="prompt">
            <span class="prompt-user"><?php echo htmlspecialchars($current_user); ?></span>@
            <span class="prompt-user"><?php echo htmlspecialchars($hostname); ?></span>:<span class="prompt-path"><?php echo htmlspecialchars($current_dir); ?></span>$
        </span>
        <form method="POST" action="">
            <input type="text" name="command" id="command-input" autocomplete="off" autofocus>
        </form>
    </div>
    
    <div class="button-group">
        <form method="POST" action="" style="flex:0">
            <input type="hidden" name="command" value="clear">
            <button type="submit">🧹 Limpar</button>
        </form>
        <form method="POST" action="" style="flex:0">
            <input type="hidden" name="command" value="ls -la">
            <button type="submit">📁 Listar arquivos</button>
        </form>
        <form method="POST" action="" style="flex:0">
            <input type="hidden" name="command" value="pwd">
            <button type="submit">📂 Mostrar caminho</button>
        </form>
        <form method="POST" action="" style="flex:0">
            <input type="hidden" name="command" value="df -h">
            <button type="submit">💾 Disco</button>
        </form>
        <form method="POST" action="" style="flex:0">
            <input type="hidden" name="command" value="free -m">
            <button type="submit">🧠 Memória</button>
        </form>
    </div>
    
    <div class="status-bar">
        ℹ️ Comandos básicos: ls, cd, pwd, mkdir, rm, cat, clear, exit
    </div>
</div>

<?php
// Scroll automático via meta refresh (sem JS)
if ($command !== '') {
    echo '<meta http-equiv="refresh" content="0.1">';
}
?>
</body>
</html>