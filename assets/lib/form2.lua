graphics.display(graphics.BuildScreen({
    title = "Tela de Teste",
    back = { label = "Voltar", root = os.exit },
    button = { label = "OK" },
    fields = {
        [1] = { type = "text", label = "Mensagem:", value = "Olá mundo no J2ME!" },
        [2] = { type = "textfield", label = "Digite algo", value = "" },
        [3] = { type = "spacer", width = 1, heigth = 20 },
        [4] = { type = "gauge", label = "Progresso", interactive = false, max = 100, value = 40 },
        [5] = { type = "choice", label = "Opções", mode = "exclusive",
                options = { [1] = "A", [2] = "B", [3] = "C" } },
        [6] = { type = "item", label = "Executar", root = "echo Você clicou em Executar" }
    }
}))
