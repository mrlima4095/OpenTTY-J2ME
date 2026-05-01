<?php
session_start();

// Configurações
$SSH_HOST = 'localhost';
$SSH_PORT = 22;

// Função para conectar SSH
function ssh_connect($host, $port, $user, $pass) {
    // Verificar se extensão está carregada
    if (!function_exists('ssh2_connect')) {
        return array('error' => 'Extensão SSH2 não está instalada no servidor');
    }
    
    $connection = @ssh2_connect($host, $port);
    if (!$connection) {
        return array('error' => "Não foi possível conectar a $host:$port");
    }
    
    if (@ssh2_auth_password($connection, $user, $pass)) {
        return array('conn' => $connection);
    } else {
        return array('error' => 'Falha na autenticação. Verifique usuário/senha');
    }
}

// Processar login
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action'])) {
    if ($_POST['action'] === 'login') {
        $ssh_user = trim($_POST['ssh_user'] ?? '');
        $ssh_pass = $_POST['ssh_pass'] ?? '';
        $host = trim($_POST['ssh_host'] ?? $SSH_HOST);
        $port = intval($_POST['ssh_port'] ?? $SSH_PORT);
        
        if (empty($ssh_user) || empty($ssh_pass)) {
            $error = "Usuário e senha são obrigatórios";
        } else {
            $result = ssh_connect($host, $port, $ssh_user, $ssh_pass);
            
            if (isset($result['conn'])) {
                $_SESSION['ssh_user'] = $ssh_user;
                $_SESSION['ssh_host'] = $host;
                $_SESSION['ssh_port'] = $port;
                $_SESSION['ssh_conn'] = $result['conn'];
                
                // Criar stream da shell
                $shell = @ssh2_shell($_SESSION['ssh_conn'], 'xterm', null, 0, 0, 0, 0);
                if (!$shell) {
                    $shell = @ssh2_shell($_SESSION['ssh_conn'], 'vt100');
                }
                
                if ($shell) {
                    stream_set_blocking($shell, true);
                    $_SESSION['ssh_shell'] = $shell;
                    
                    // Limpar output inicial
                    $output = '';
                    $start = time();
                    while (time() - $start < 2) {
                        $read = array($shell);
                        $write = NULL;
                        $except = NULL;
                        if (stream_select($read, $write, $except, 0, 200000)) {
                            $data = fread($shell, 4096);
                            if ($data !== false && strlen($data) > 0) {
                                $output .= $data;
                            }
                        } else {
                            break;
                        }
                    }
                    
                    $_SESSION['last_output'] = htmlspecialchars($output);
                    header('Location: ' . $_SERVER['PHP_SELF']);
                    exit;
                } else {
                    $error = "Não foi possível criar a shell no servidor remoto";
                }
            } else {
                $error = $result['error'];
            }
        }
    }
    
    elseif ($_POST['action'] === 'comando') {
        if (isset($_SESSION['ssh_shell'])) {
            $comando = $_POST['comando'] . "\n";
            fwrite($_SESSION['ssh_shell'], $comando);
            fflush($_SESSION['ssh_shell']);
            
            // Aguardar resposta
            usleep(500000); // 500ms
            
            $output = '';
            $timeout = time() + 5; // Timeout de 5 segundos
            
            while (time() < $timeout) {
                $read = array($_SESSION['ssh_shell']);
                $write = NULL;
                $except = NULL;
                
                $streams = stream_select($read, $write, $except, 0, 200000);
                if ($streams === false || $streams === 0) {
                    break;
                }
                
                if ($streams > 0) {
                    $data = fread($_SESSION['ssh_shell'], 4096);
                    if ($data !== false && strlen($data) > 0) {
                        $output .= $data;
                    } else {
                        break;
                    }
                }
            }
            
            // Se não houve output, tenta ler mais uma vez
            if (empty($output)) {
                usleep(200000);
                $output = fread($_SESSION['ssh_shell'], 8192);
                if ($output === false) $output = '';
            }
            
            $_SESSION['last_output'] = htmlspecialchars($output);
            header('Location: ' . $_SERVER['PHP_SELF']);
            exit;
        }
    }
    
    elseif ($_POST['action'] === 'logout') {
        if (isset($_SESSION['ssh_shell'])) {
            fclose($_SESSION['ssh_shell']);
        }
        if (isset($_SESSION['ssh_conn'])) {
            ssh2_disconnect($_SESSION['ssh_conn']);
        }
        session_destroy();
        header('Location: ' . $_SERVER['PHP_SELF']);
        exit;
    }
}

// Verificar se está logado
$is_logged = isset($_SESSION['ssh_shell']);
?>

<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Terminal Web SSH</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            background: #000000;
            color: #00ff00;
            font-family: 'Courier New', 'Lucida Console', monospace;
            padding: 15px;
            font-size: 14px;
        }
        
        .container {
            max-width: 100%;
            margin: 0 auto;
        }
        
        h1 {
            color: #00ff00;
            border-bottom: 2px solid #00ff00;
            padding-bottom: 10px;
            margin-bottom: 20px;
            font-size: 20px;
        }
        
        .terminal {
            background: #000000;
            border: 2px solid #00ff00;
            padding: 15px;
            margin-bottom: 20px;
            min-height: 400px;
            height: auto;
            overflow-y: auto;
            overflow-x: auto;
        }
        
        .output {
            font-family: 'Courier New', monospace;
            white-space: pre-wrap;
            word-wrap: break-word;
            font-size: 13px;
            line-height: 1.4;
        }
        
        .output pre {
            margin: 0;
            padding: 0;
            background: transparent;
            color: #00ff00;
            font-family: inherit;
        }
        
        .input-area {
            background: #000000;
            border-top: 2px solid #00ff00;
            padding: 15px 0;
        }
        
        .prompt {
            color: #00ff00;
            display: inline;
            font-weight: bold;
            font-size: 13px;
        }
        
        form {
            display: inline;
            width: 100%;
        }
        
        input, button {
            background: #000000;
            color: #00ff00;
            border: 1px solid #00ff00;
            padding: 8px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
        }
        
        input {
            width: 70%;
            background: #000000;
        }
        
        button {
            cursor: pointer;
            margin-left: 5px;
            padding: 8px 15px;
        }
        
        button:hover {
            background: #00ff00;
            color: #000000;
        }
        
        .login-box {
            background: #000000;
            border: 2px solid #00ff00;
            padding: 20px;
            max-width: 400px;
            margin: 40px auto;
        }
        
        .login-box h2 {
            color: #00ff00;
            margin-bottom: 20px;
            font-size: 18px;
        }
        
        .login-box input {
            width: 100%;
            margin-bottom: 12px;
            padding: 8px;
        }
        
        .info {
            color: #ffff00;
            margin-bottom: 15px;
            padding: 10px;
            border: 1px solid #ffff00;
            font-size: 12px;
        }
        
        .error {
            color: #ff0000;
            border-color: #ff0000;
        }
        
        .status-bar {
            background: #001100;
            padding: 8px;
            margin-bottom: 15px;
            border: 1px solid #00ff00;
            font-size: 12px;
        }
        
        @media (max-width: 768px) {
            body {
                padding: 8px;
                font-size: 12px;
            }
            
            input {
                width: 65%;
                font-size: 11px;
            }
            
            button {
                padding: 6px 10px;
                font-size: 11px;
            }
            
            .terminal {
                padding: 10px;
                min-height: 300px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🌐 Terminal Web SSH</h1>
        
        <?php if (!$is_logged): ?>
            <!-- Formulário de Login -->
            <div class="login-box">
                <h2>🔐 Conexão SSH</h2>
                
                <?php if (isset($error)): ?>
                    <div class="info error">❌ <?php echo htmlspecialchars($error); ?></div>
                <?php endif; ?>
                
                <form method="POST" action="">
                    <input type="hidden" name="action" value="login">
                    <input type="text" name="ssh_host" placeholder="Servidor (IP ou hostname)" value="<?php echo $SSH_HOST; ?>" required autocomplete="off">
                    <input type="text" name="ssh_port" placeholder="Porta SSH" value="<?php echo $SSH_PORT; ?>" required>
                    <input type="text" name="ssh_user" placeholder="Usuário" required autocomplete="off">
                    <input type="password" name="ssh_pass" placeholder="Senha" required autocomplete="off">
                    <button type="submit" style="width: 100%; margin-top: 10px;">🔌 Conectar</button>
                </form>
                
                <div class="info" style="margin-top: 15px; font-size: 11px;">
                    💡 Dica: Use IP local ou remoto com permissão SSH
                </div>
            </div>
        
        <?php else: ?>
            <!-- Barra de status -->
            <div class="status-bar">
                ✅ Conectado: <strong><?php echo htmlspecialchars($_SESSION['ssh_user']); ?>@<?php echo htmlspecialchars($_SESSION['ssh_host']); ?></strong>
            </div>
            
            <!-- Terminal -->
            <div class="terminal">
                <div class="output">
                    <?php
                    if (isset($_SESSION['last_output'])) {
                        echo "<pre>" . $_SESSION['last_output'] . "</pre>";
                        unset($_SESSION['last_output']);
                    } else {
                        echo "<pre>Terminal pronto. Digite um comando abaixo.</pre>";
                    }
                    ?>
                </div>
            </div>
            
            <div class="input-area">
                <form method="POST" action="" style="display: flex; flex-wrap: wrap; gap: 8px; align-items: center;">
                    <input type="hidden" name="action" value="comando">
                    <span class="prompt">[<?php echo htmlspecialchars($_SESSION['ssh_user']); ?>@<?php echo htmlspecialchars($_SESSION['ssh_host']); ?> ~]$ </span>
                    <input type="text" name="comando" id="comando" value="<?php echo isset($_POST['comando']) ? htmlspecialchars($_POST['comando']) : ''; ?>" autocomplete="off" style="flex: 1; min-width: 150px;">
                    <button type="submit">▶ Executar</button>
                    <button type="button" onclick="clearInput()">🗑 Limpar</button>
                </form>
            </div>
            
            <div style="margin-top: 20px; text-align: center;">
                <form method="POST" action="" style="display: inline;">
                    <input type="hidden" name="action" value="logout">
                    <button type="submit" style="background: #330000; color: #ff0000;">🔌 Desconectar</button>
                </form>
            </div>
            
            <script>
                // Script simples para limpar input - só isso, não afeta funcionalidade principal
                function clearInput() {
                    document.getElementById('comando').value = '';
                    document.getElementById('comando').focus();
                }
                
                // Manter foco no input
                if (document.getElementById('comando')) {
                    document.getElementById('comando').focus();
                }
            </script>
        <?php endif; ?>
    </div>
</body>
</html>