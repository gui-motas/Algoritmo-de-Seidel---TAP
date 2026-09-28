package com.tap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GaussSeidel {

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            sc.useLocale(Locale.US);

            System.out.println();
            System.out.println("Olá, esse é o Método de Gauss-Seidel para resolução de sistemas lineares");
            System.out.println("Regras de entrada:");
            System.out.println("1. O sistema linear deve ter o mesmo número de equações e variáveis.");
            System.out.println("2. As equações devem ser separadas por vírgulas e cada equação deve ter o formato 'ax+by+cz=d'."); 
            System.out.println("Exemplo de formato: { 10x-y+2z=6, -x+11y-z=22, 2x-y+10z=-10 }");
            System.out.println("Digite agora o sistema linear que quer resolver:");

            SistemaLinear sistema;
            try {
                sistema = extrairSistema(sc.nextLine());
            } catch (IllegalArgumentException e) {
                System.out.println("Formato inválido: " + e.getMessage());
                return;
            }
            double[][] matrizCoeficientes = sistema.matrizCoeficientes();
            double[] termosIndependentes = sistema.termosIndependentes();
            double[] aproximacoes = new double[termosIndependentes.length];

            System.out.print("\nDefina a tolerância (ex: 0.001): ");
            double tolerancia = sc.nextDouble();

            System.out.print("Defina o número máximo de iterações: ");
            int maxIteracoes = sc.nextInt();

            System.out.println("Análise de Convergência:");

            ResultadoCriterio resultadoLinhas = calcularCriterioLinhas(matrizCoeficientes);
            exibirResultadoCriterio("Critério das Linhas", resultadoLinhas);

            ResultadoCriterio resultadoColunas = calcularCriterioColunas(matrizCoeficientes);
            exibirResultadoCriterio("Critério das Colunas", resultadoColunas);

            ResultadoCriterio resultadoSassenfeld = calcularCriterioSassenfeld(matrizCoeficientes);
            exibirResultadoCriterio("Critério de Sassenfeld", resultadoSassenfeld);

            boolean critLinhas = resultadoLinhas.satisfeito();
            boolean critColunas = resultadoColunas.satisfeito();
            boolean sassenfeldSatisfeito = resultadoSassenfeld.satisfeito();

            System.out.println();
            if (!critLinhas && !critColunas && !sassenfeldSatisfeito) {
                System.out.println("Aviso: O método pode não convergir (matiz não é estritamente diagonal dominante e critério de Sassenfeld não satisfeito).");
                return;
            } else {
                System.out.println("Convergência garantida!");
            }

            gaussSeidel(matrizCoeficientes, termosIndependentes, aproximacoes,
                    sistema.variaveis(), tolerancia, maxIteracoes);
        }
    }
    private static SistemaLinear extrairSistema(String entrada) {
        String[] equacoes = entrada.replaceAll("[{}\\s]", "").split(",");
        Pattern padraoTermo = Pattern.compile("(?<coeficiente>[+-]?\\d*\\.?\\d*)(?<variavel>[a-zA-Z])");
        String variaveis = "";

        for (String equacao : equacoes) {
            String[] lados = equacao.split("=");
            if (lados.length != 2) {
                throw new IllegalArgumentException("Separe as equações por vírgulas e use '='.");
            }
            Matcher matcher = padraoTermo.matcher(lados[0]);
            while (matcher.find()) {
                String variavel = matcher.group("variavel");
                if (!variaveis.contains(variavel)) variaveis += variavel;
            }
        }

        int n = equacoes.length;
        if (variaveis.length() != n) {
            throw new IllegalArgumentException("O número de equações deve ser igual ao de variáveis.");
        }

        double[][] matrizCoeficientes = new double[n][n];
        double[] termosIndependentes = new double[n];

        for (int i = 0; i < n; i++) {
            String[] lados = equacoes[i].split("=");
            Matcher matcher = padraoTermo.matcher(lados[0]);
            while (matcher.find()) {
                String coeficiente = matcher.group("coeficiente");
                double valor = coeficiente.isEmpty() || coeficiente.equals("+") ? 1
                        : coeficiente.equals("-") ? -1 : Double.parseDouble(coeficiente);
                matrizCoeficientes[i][variaveis.indexOf(matcher.group("variavel"))] += valor;
            }
            termosIndependentes[i] = Double.parseDouble(lados[1]);
        }

        String[] nomesVariaveis = new String[variaveis.length()];
        for (int i = 0; i < variaveis.length(); i++) {
            nomesVariaveis[i] = String.valueOf(variaveis.charAt(i));
        }

        return new SistemaLinear(nomesVariaveis, matrizCoeficientes, termosIndependentes);
    }

    private record SistemaLinear(String[] variaveis, double[][] matrizCoeficientes,
            double[] termosIndependentes) {
    }

    private record ResultadoCriterio(boolean satisfeito, List<String> calculos) {
    }

    public static boolean verificarCriterioLinhas(double[][] matrizCoeficientes) {
        return calcularCriterioLinhas(matrizCoeficientes).satisfeito();
    }

    private static ResultadoCriterio calcularCriterioLinhas(double[][] matrizCoeficientes) {
        List<String> calculos = new ArrayList<>();
        boolean satisfeito = true;

        for (int linha = 0; linha < matrizCoeficientes.length; linha++) {
            double diagonal = Math.abs(matrizCoeficientes[linha][linha]);
            double soma = 0;
            for (int coluna = 0; coluna < matrizCoeficientes.length; coluna++) {
                if (coluna != linha) soma += Math.abs(matrizCoeficientes[linha][coluna]);
            }

            boolean linhaSatisfeita = diagonal > soma;
            calculos.add(String.format("Equação %d: |a%d%d| = %.6f; soma dos demais = %s = %.6f; %.6f > %.6f -> %s",
                    linha + 1, linha + 1, linha + 1, diagonal, formatarSomaLinha(matrizCoeficientes, linha),
                    soma, diagonal, soma, linhaSatisfeita ? "sim" : "não"));
            if (!linhaSatisfeita) satisfeito = false;
        }

        return new ResultadoCriterio(satisfeito, calculos);
    }

    public static boolean verificarCriterioColunas(double[][] matrizCoeficientes) {
        return calcularCriterioColunas(matrizCoeficientes).satisfeito();
    }

    private static ResultadoCriterio calcularCriterioColunas(double[][] matrizCoeficientes) {
        List<String> calculos = new ArrayList<>();
        boolean satisfeito = true;

        for (int coluna = 0; coluna < matrizCoeficientes.length; coluna++) {
            double diagonal = Math.abs(matrizCoeficientes[coluna][coluna]);
            double soma = 0;
            for (int linha = 0; linha < matrizCoeficientes.length; linha++) {
                if (linha != coluna) soma += Math.abs(matrizCoeficientes[linha][coluna]);
            }

            boolean colunaSatisfeita = diagonal > soma;
            calculos.add(String.format("Coluna %d: |a%d%d| = %.6f; soma dos demais = %s = %.6f; %.6f > %.6f -> %s",
                    coluna + 1, coluna + 1, coluna + 1, diagonal, formatarSomaColuna(matrizCoeficientes, coluna),
                    soma, diagonal, soma, colunaSatisfeita ? "sim" : "não"));
            if (!colunaSatisfeita) satisfeito = false;
        }

        return new ResultadoCriterio(satisfeito, calculos);
    }

    private static String formatarSomaLinha(double[][] matrizCoeficientes, int linha) {
        StringBuilder soma = new StringBuilder();
        for (int coluna = 0; coluna < matrizCoeficientes.length; coluna++) {
            if (coluna == linha) continue;
            if (soma.length() > 0) soma.append(" + ");
            soma.append(String.format("|a%d%d| (%.6f)", linha + 1, coluna + 1,
                    Math.abs(matrizCoeficientes[linha][coluna])));
        }
        return soma.length() == 0 ? "0" : soma.toString();
    }

    private static String formatarSomaColuna(double[][] matrizCoeficientes, int coluna) {
        StringBuilder soma = new StringBuilder();
        for (int linha = 0; linha < matrizCoeficientes.length; linha++) {
            if (linha == coluna) continue;
            if (soma.length() > 0) soma.append(" + ");
            soma.append(String.format("|a%d%d| (%.6f)", linha + 1, coluna + 1,
                    Math.abs(matrizCoeficientes[linha][coluna])));
        }
        return soma.length() == 0 ? "0" : soma.toString();
    }

    public static boolean critSassenfeld(double[][] matrizCoeficientes) {
        return calcularCriterioSassenfeld(matrizCoeficientes).satisfeito();
    }

    private static ResultadoCriterio calcularCriterioSassenfeld(double[][] matrizCoeficientes) {
        int n = matrizCoeficientes.length;
        List<String> calculos = new ArrayList<>();
        if (n == 0) {
            return new ResultadoCriterio(false, List.of("Matriz vazia: não há valores β para calcular."));
        }

        double[] beta = new double[n];
        boolean satisfeito = true;
        for (int linha = 0; linha < n; linha++) {
            double diagonal = Math.abs(matrizCoeficientes[linha][linha]);
            if (diagonal == 0) {
                calculos.add(String.format("β%d: diagonal nula; não é possível calcular o quociente.", linha + 1));
                return new ResultadoCriterio(false, calculos);
            }

            double soma = 0;
            for (int coluna = 0; coluna < linha; coluna++) {
                soma += Math.abs(matrizCoeficientes[linha][coluna]) * beta[coluna];
            }
            for (int coluna = linha + 1; coluna < n; coluna++) {
                soma += Math.abs(matrizCoeficientes[linha][coluna]);
            }

            beta[linha] = soma / diagonal;
            calculos.add(formatarCalculoBeta(matrizCoeficientes, beta, linha, diagonal));
            if (!Double.isFinite(beta[linha]) || beta[linha] >= 1) satisfeito = false;
        }

        return new ResultadoCriterio(satisfeito, calculos);
    }

    private static String formatarCalculoBeta(double[][] matrizCoeficientes, double[] beta,
            int linha, double diagonal) {
        StringBuilder calculo = new StringBuilder(String.format("β%d = (", linha + 1));
        boolean primeiroTermo = true;
        for (int coluna = 0; coluna < linha; coluna++) {
            if (!primeiroTermo) calculo.append(" + ");
            calculo.append(String.format("|a%d%d| * β%d", linha + 1, coluna + 1, coluna + 1));
            primeiroTermo = false;
        }
        for (int coluna = linha + 1; coluna < beta.length; coluna++) {
            if (!primeiroTermo) calculo.append(" + ");
            calculo.append(String.format("|a%d%d|", linha + 1, coluna + 1));
            primeiroTermo = false;
        }
        if (primeiroTermo) calculo.append("0");

        calculo.append(String.format(") / |a%d%d| = (", linha + 1, linha + 1));
        primeiroTermo = true;
        for (int coluna = 0; coluna < linha; coluna++) {
            if (!primeiroTermo) calculo.append(" + ");
            calculo.append(String.format("%.6f * %.6f", Math.abs(matrizCoeficientes[linha][coluna]),
                    beta[coluna]));
            primeiroTermo = false;
        }
        for (int coluna = linha + 1; coluna < beta.length; coluna++) {
            if (!primeiroTermo) calculo.append(" + ");
            calculo.append(String.format("%.6f", Math.abs(matrizCoeficientes[linha][coluna])));
            primeiroTermo = false;
        }
        if (primeiroTermo) calculo.append("0");

        calculo.append(String.format(") / %.6f = %.6f; β%d < 1 -> %s", diagonal,
                beta[linha], linha + 1, beta[linha] < 1 ? "sim" : "não"));
        return calculo.toString();
    }

    private static void exibirResultadoCriterio(String nome, ResultadoCriterio resultado) {
        System.out.println("\n" + nome + ":");
        for (String calculo : resultado.calculos()) {
            System.out.println(calculo);
        }
        System.out.println("Resultado: " + (resultado.satisfeito() ? "Satisfeito" : "Não satisfeito"));
    }

    public static void gaussSeidel(double[][] matrizCoeficientes, double[] termosIndependentes,
            double[] aproximacoes, double tolerancia, int maxIteracoes) {
        String[] variaveis = new String[aproximacoes.length];
        for (int i = 0; i < variaveis.length; i++) {
            variaveis[i] = "x" + (i + 1);
        }
        gaussSeidel(matrizCoeficientes, termosIndependentes, aproximacoes, variaveis,
                tolerancia, maxIteracoes);
    }

    private static void gaussSeidel(double[][] matrizCoeficientes, double[] termosIndependentes,
            double[] aproximacoes, String[] variaveis, double tolerancia, int maxIteracoes) {
        int n = termosIndependentes.length;
        System.out.println();

        for (int iteracao = 1; iteracao <= maxIteracoes; iteracao++) {
            double maiorErro = 0;
            System.out.println("Iteração " + iteracao + ":");

            for (int i = 0; i < n; i++) {
                double soma = 0;
                for (int j = 0; j < n; j++) {
                    if (j != i) soma += matrizCoeficientes[i][j] * aproximacoes[j];
                }

                double novoX = (termosIndependentes[i] - soma) / matrizCoeficientes[i][i];
                maiorErro = Math.max(maiorErro, Math.abs(novoX - aproximacoes[i]));
                exibirCalculoIteracao(i, matrizCoeficientes, termosIndependentes,
                        aproximacoes, variaveis, novoX);
                aproximacoes[i] = novoX;
            }

            System.out.printf("Maior variação nesta iteração = %.8g%n", maiorErro);
            System.out.println();

            if (maiorErro < tolerancia) {
                System.out.println("Convergiu");
                exibirSolucao(aproximacoes, variaveis);
                return;
            }
        }

        System.out.println("Número máximo de iterações atingido sem convergência completa.");
        exibirSolucao(aproximacoes, variaveis);
    }

    private static void exibirCalculoIteracao(int i, double[][] matrizCoeficientes,
            double[] termosIndependentes, double[] aproximacoes, String[] variaveis, double novoX) {
        System.out.printf("Equação %d: %s = (%.6f - [", i + 1, variaveis[i], termosIndependentes[i]);

        boolean primeiroTermo = true;
        for (int j = 0; j < aproximacoes.length; j++) {
            if (j == i) continue;
            if (!primeiroTermo) System.out.print(" + ");
            System.out.printf("(%.6f * %s[%.6f])", matrizCoeficientes[i][j], variaveis[j],
                    aproximacoes[j]);
            primeiroTermo = false;
        }

        System.out.printf("]) / %.6f = %.6f%n", matrizCoeficientes[i][i], novoX);
    }

    private static void exibirSolucao(double[] aproximacoes, String[] variaveis) {
        System.out.println("Solução aproximada:");
        for (int i = 0; i < aproximacoes.length; i++) {
            System.out.printf("%s = %.6f%n", variaveis[i], aproximacoes[i]);
        }
    }
}
