<?php
session_start();

// Configurações
$SSH_HOST = 'localhost'; // Altere para seu servidor
$SSH_PORT = 22;
$SSH_USER = ''; // Será solicitado no login

// Função para conectar SSH
function ssh_connect($host, $port, $user, $pass) {
    $connection = ssh2_connect($host, $port);
    if (!$connection) {
        return false;
    }
    
    if (ssh2_auth_password($connection, $user, $pass)) {
        return $connection;
    }
    return false;
}

// Processar login
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action'])) {
    if ($_POST['action'] === 'login') {
        $ssh_user = $_POST['ssh_user'] ?? '';
        $ssh_pass = $_POST['ssh_pass'] ?? '';
        $host = $_POST['ssh_host'] ?? $SSH_HOST;
        $port = $_POST['ssh_port'] ?? $SSH_PORT;
        
        $conn = ssh_connect($host, $port, $ssh_user, $ssh_pass);
        
        if ($conn) {
            $_SESSION['ssh_conn'] = true;
            $_SESSION['ssh_user'] = $ssh_user;
            $_SESSION['ssh_host'] = $host;
            $_SESSION['ssh_port'] = $port;
            
            // Iniciar sessão shell
            $_SESSION['ssh_stream'] = ssh2_shell($conn, 'xterm');
            stream_set_blocking($_SESSION['ssh_stream'], true);
            
            header('Location: ' . $_SERVER['PHP_SELF']);
            exit;
        } else {
            $error = "Falha na conexão SSH. Verifique suas credenciais.";
        }
    }
    
    elseif ($_POST['action'] === 'comando') {
        if (isset($_SESSION['ssh_stream'])) {
            $comando = $_POST['comando'] . "\n";
            fwrite($_SESSION['ssh_stream'], $comando);
            
            // Aguardar resposta
            usleep(100000); // 100ms
            $output = '';
            while ($line = fgets($_SESSION['ssh_stream'])) {
                $output .= $line;
                if (strpos($line, '$ ') !== false || strpos($line, '# ') !== false) {
                    break;
                }
            }
            
            $_SESSION['last_output'] = htmlspecialchars($output);
            header('Location: ' . $_SERVER['PHP_SELF']);
            exit;
        }
    }
    
    elseif ($_POST['action'] === 'logout') {
        if (isset($_SESSION['ssh_stream'])) {
            fclose($_SESSION['ssh_stream']);
        }
        session_destroy();
        header('Location: ' . $_SERVER['PHP_SELF']);
        exit;
    }
}

// Verificar se está logado
$is_logged = isset($_SESSION['ssh_stream']);
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
            background: #000;
            color: #0f0;
            font-family: 'Courier New', 'Lucida Console', monospace;
            padding: 20px;
            font-size: 14px;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        
        h1 {
            color: #0f0;
            border-bottom: 2px solid #0f0;
            padding-bottom: 10px;
            margin-bottom: 20px;
            font-size: 24px;
        }
        
        .terminal {
            background: #000;
            border: 2px solid #0f0;
            padding: 20px;
            margin-bottom: 20px;
            min-height: 400px;
            max-height: 500px;
            overflow-y: auto;
        }
        
        .output {
            font-family: 'Courier New', monospace;
            white-space: pre-wrap;
            word-wrap: break-word;
            margin-bottom: 10px;
        }
        
        .input-area {
            background: #000;
            border-top: 2px solid #0f0;
            padding: 15px 0;
        }
        
        .prompt {
            color: #0f0;
            display: inline;
            font-weight: bold;
        }
        
        input, button {
            background: #000;
            color: #0f0;
            border: 1px solid #0f0;
            padding: 10px;
            font-family: 'Courier New', monospace;
            font-size: 14px;
        }
        
        input {
            width: 80%;
            background: #000;
        }
        
        button {
            cursor: pointer;
            margin-left: 10px;
            padding: 10px 20px;
        }
        
        button:hover {
            background: #0f0;
            color: #000;
        }
        
        .login-box {
            background: #000;
            border: 2px solid #0f0;
            padding: 30px;
            max-width: 400px;
            margin: 50px auto;
        }
        
        .login-box input {
            width: 100%;
            margin-bottom: 15px;
        }
        
        .info {
            color: #ff0;
            margin-bottom: 20px;
            padding: 10px;
            border: 1px solid #ff0;
        }
        
        .error {
            color: #f00;
            border-color: #f00;
        }
        
        @media (max-width: 768px) {
            body {
                padding: 10px;
                font-size: 12px;
            }
            
            input {
                width: 70%;
            }
            
            button {
                padding: 8px 15px;
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
                    <div class="info error"><?php echo htmlspecialchars($error); ?></div>
                <?php endif; ?>
                
                <form method="POST" action="">
                    <input type="hidden" name="action" value="login">
                    <input type="text" name="ssh_host" placeholder="Servidor (ex: 192.168.1.1)" value="<?php echo $SSH_HOST; ?>" required>
                    <input type="text" name="ssh_port" placeholder="Porta" value="<?php echo $SSH_PORT; ?>" required>
                    <input type="text" name="ssh_user" placeholder="Usuário" required autocomplete="username">
                    <input type="password" name="ssh_pass" placeholder="Senha" required autocomplete="current-password">
                    <button type="submit">🔌 Conectar</button>
                </form>
                
                <div class="info" style="margin-top: 20px;">
                    💡 Dica: Certifique-se que o servidor SSH está acessível
                </div>
            </div>
        
        <?php else: ?>
            <!-- Terminal -->
            <div class="terminal">
                <div class="output">
                    <?php
                    if (isset($_SESSION['last_output'])) {
                        echo "<pre>" . $_SESSION['last_output'] . "</pre>";
                        unset($_SESSION['last_output']);
                    } else {
                        // Buscar prompt inicial
                        if (isset($_SESSION['ssh_stream'])) {
                            $initial = '';
                            for ($i = 0; $i < 10; $i++) {
                                $line = fgets($_SESSION['ssh_stream']);
                                if ($line === false) break;
                                $initial .= htmlspecialchars($line);
                                if (strpos($line, '$ ') !== false || strpos($line, '# ') !== false) {
                                    break;
                                }
                            }
                            echo "<pre>" . $initial . "</pre>";
                        }
                    }
                    ?>
                </div>
            </div>
            
            <div class="input-area">
                <form method="POST" action="">
                    <input type="hidden" name="action" value="comando">
                    <span class="prompt"><?php echo htmlspecialchars($_SESSION['ssh_user']); ?>@<?php echo htmlspecialchars($_SESSION['ssh_host']); ?>:~$ </span>
                    <input type="text" name="comando" id="comando" autocomplete="off" autofocus>
                    <button type="submit">▶ Executar</button>
                </form>
            </div>
            
            <div style="margin-top: 20px; text-align: center;">
                <form method="POST" action="" style="display: inline;">
                    <input type="hidden" name="action" value="logout">
                    <button type="submit" style="background: #300; color: #f00;">🔌 Desconectar</button>
                </form>
            </div>
        <?php endif; ?>
    </div>
</body>
</html>