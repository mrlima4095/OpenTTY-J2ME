def dividir_arquivo_em_partes(caminho_arquivo, prefixo_saida="PARTES/cx-parte", linhas_por_parte=1000):
    with open(caminho_arquivo, "r", encoding="utf-8") as f:
        linhas = f.readlines()

    total_linhas = len(linhas)
    partes = (total_linhas + linhas_por_parte - 1) // linhas_por_parte  # número total de partes

    for i in range(partes):
        inicio = i * linhas_por_parte
        fim = inicio + linhas_por_parte
        trecho = linhas[inicio:fim]

        nome_saida = f"{prefixo_saida}{i+1}.java"
        with open(nome_saida, "w", encoding="utf-8") as out:
            out.writelines(trecho)

        print(f"Parte {i+1} salva como {nome_saida}")

dividir_arquivo_em_partes("cx.java")
