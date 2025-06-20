import java.util.*;

// Simulador de um compilador J2ME que utiliza a cx.java como base
public class J2MEStringCompiler {
    private final cx compilador;

    public J2MEStringCompiler(String javaCode, String className, ha symbolTable) throws Exception {
        // Simula um projeto fv contendo uma "unidade de compilacao"
        fv projetoFake = new fv("MemProject");

        // Cria um fp falso que retorna o conteudo da string como se fosse um .java
        fp arquivoFake = new fp(className, javaCode);

        // Adiciona ao projeto
        projetoFake.a.addElement(arquivoFake);

        // Inicializa o compilador real com a estrutura montada
        this.compilador = new cx(projetoFake, symbolTable);
    }

    public byte[][] compilar() throws Exception {
        nb[] classesCompiladas = this.compilador.a();
        byte[][] bytes = new byte[classesCompiladas.length][];

        for (int i = 0; i < classesCompiladas.length; i++) {
            bytes[i] = classesCompiladas[i].toByteArray(); // ou classe.getBytes(), conforme implementação de nb
        }

        return bytes;
    }
}
