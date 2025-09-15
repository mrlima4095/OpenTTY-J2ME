function sendCommand() {
    const cmd = document.getElementById('cmd').value;
    fetch('/cli/send', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({command: cmd})
    }).then(() => {
        document.getElementById('cmd').value = '';
    });
}

function pollOutput() {
    fetch('/cli/receive')
        .then(res => res.json())
        .then(data => {
            if (data.output) {
                const outputDiv = document.getElementById('output');
                outputDiv.textContent += data.output + "\n";
                outputDiv.scrollTop = outputDiv.scrollHeight;
            }
        });
}

setInterval(pollOutput, 1000);
