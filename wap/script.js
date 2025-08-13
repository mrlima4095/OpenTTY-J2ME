// Script mínimo externo para o mirror
(function(){
    // Atualiza data e ano do rodapé
    var elU = document.getElementById('lastUpdate');
    var elY = document.getElementById('y');
    var now = new Date();
    if(elU) {
        elU.textContent = now.toLocaleDateString('pt-BR', { year:'numeric', month:'2-digit', day:'2-digit'});
        elU.setAttribute('datetime', now.toISOString());
    }
    if(elY) elY.textContent = now.getFullYear();

    // Busca simples: esconde/mostra seções
    var q = document.getElementById('q');
    if(q) {
        q.addEventListener('keydown', function(e){
            if(e.key !== 'Enter') return;
            var term = this.value.trim().toLowerCase();
            var sections = document.querySelectorAll('main section, .sidebar .group');
            sections.forEach(function(s){
                if(!term){ s.style.display=''; return; }
                var txt = s.innerText.toLowerCase();
                s.style.display = txt.indexOf(term) !== -1 ? '' : 'none';
            });
        });
    }

    // Toggle checksums
    window.toggleChecksums = function(){
        var el = document.getElementById('checksumsBlock');
        if(!el) return;
        el.style.display = (el.style.display==='none' || !el.style.display) ? 'block' : 'none';
    };
})();
