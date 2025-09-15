import threading
import socket
import uuid
from flask import Flask, request, session, redirect, render_template, url_for, jsonify
from flask_cors import CORS

app = Flask(__name__)
app.secret_key = 'segredo_super_seguro'
CORS(app)

# Conexões ativas
connections = {}  
# {conn_id: {'conn': socket, 'addr': (ip, port), 'password': str, 'buffer': str, 'in_use': bool, 'disconnected': bool}}

def handle_client(conn, addr):
    try:
        print(f'[TCP] Nova conexão de {addr}')
        conn.sendall(b'Password: ')
        password = conn.recv(1024).decode().strip()

        conn_id = str(uuid.uuid4())[:8]

        connections[conn_id] = {
            'conn': conn,
            'addr': addr,
            'password': password,
            'buffer': '',
            'in_use': False,
            'disconnected': False
        }

        print(f'[TCP] Cliente autenticado. ID: {conn_id} | IP: {addr} | Senha: {password}')
        conn.sendall(f'Connected. Your ID is {conn_id}\n'.encode())

        conn.settimeout(0.5)  # evita bloqueio eterno no recv()

        while True:
            try:
                data = conn.recv(1024)
                if not data:
                    break
                msg = data.decode()
                print(f'[TCP] Recebido de {conn_id}: {msg.strip()}')
                connections[conn_id]['buffer'] += msg
            except socket.timeout:
                continue  # não trava, volta pro loop
    except Exception as e:
        print(f'[TCP] Erro com {addr}: {e}')
    finally:
        print(f'[TCP] Conexão encerrada com {addr}')
        for k, v in connections.items():
            if v['conn'] == conn:
                print(f'[TCP] Marcando conexão {k} como desconectada')
                v['disconnected'] = True
        conn.close()

def start_tcp_server(host='0.0.0.0', port=4096):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((host, port))
    server.listen(50)
    print(f'[TCP] Servidor escutando em {host}:{port}')

    while True:
        conn, addr = server.accept()
        threading.Thread(target=handle_client, args=(conn, addr), daemon=True).start()

# --- Flask endpoints ---

@app.route('/cli/')
def index():
    return render_template('login.html')

@app.route('/cli/login', methods=['POST'])
def login():
    conn_id = request.form['conn_id']
    password = request.form['password']

    conn_data = connections.get(conn_id)

    if not conn_data:
        return 'ID inválido', 403
    if conn_data['password'] != password:
        return 'Senha incorreta', 403
    if conn_data['in_use']:
        return 'Sessão já está em uso', 403
    if conn_data.get('disconnected', False):
        return 'Conexão encerrada', 403

    conn_data['in_use'] = True
    session['conn_id'] = conn_id

    return redirect(url_for('terminal'))

@app.route('/cli/terminal')
def terminal():
    if 'conn_id' not in session:
        return redirect('/cli/')
    return render_template('terminal.html')

@app.route('/cli/send', methods=['POST'])
def send_command():
    if 'conn_id' not in session:
        return 'Não autorizado', 401

    conn_id = session['conn_id']
    data = request.json
    command = data.get('command', '')

    conn_data = connections.get(conn_id)
    if not conn_data:
        return 'Sessão inválida', 400
    if conn_data.get('disconnected', False):
        return 'Conexão encerrada', 400

    try:
        conn_data['conn'].sendall((command + '\n').encode())
        print(f'[FLASK] Comando enviado para {conn_id}: {command}')
        return 'Enviado', 200
    except Exception as e:
        print(f'[FLASK] Erro ao enviar comando: {e}')
        return f'Erro: {e}', 500

@app.route('/cli/receive')
def receive_data():
    if 'conn_id' not in session:
        return 'Não autorizado', 401

    conn_id = session['conn_id']
    conn_data = connections.get(conn_id)

    if not conn_data:
        return 'Sessão inválida', 400

    output = conn_data['buffer']
    conn_data['buffer'] = ''
    return jsonify({'output': output})

if __name__ == '__main__':
    threading.Thread(target=start_tcp_server, daemon=True).start()
    app.run(host='0.0.0.0', port=10142, debug=True, use_reloader=False)
