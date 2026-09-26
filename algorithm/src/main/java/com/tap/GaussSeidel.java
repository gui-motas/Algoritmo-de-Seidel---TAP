package com.tap;

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
            double[] b = sistema.termosIndependentes();
            double[] x = new double[b.length];

            System.out.print("\nDefina a tolerância (ex: 0.001): ");
            double tolerancia = sc.nextDouble();

            System.out.print("Defina o número máximo de iterações: ");
            int maxIteracoes = sc.nextInt();

            System.out.println("Análise de Convergência:");

            System.out.println("\nCritério das Linhas:");
            boolean critLinhas = verificarCriterioLinhas(matrizCoeficientes);
            System.out.println("Resultado: " + (critLinhas ? "Satisfeito" : "Não satisfeito"));

            System.out.println("\nCritério das Colunas:");
            boolean critColunas = verificarCriterioColunas(matrizCoeficientes);
            System.out.println("Resultado: " + (critColunas ? "Satisfeito" : "Não satisfeito"));

            System.out.println("\nCritério de Sassenfeld:");
            boolean sassenfeldSatisfeito = critSassenfeld(matrizCoeficientes);
            System.out.println("Resultado: " + (sassenfeldSatisfeito ? "Satisfeito" : "Não satisfeito"));

            System.out.println();
            if (!critLinhas && !critColunas && !sassenfeldSatisfeito) {
                System.out.println("Aviso: O método pode não convergir (matriz não é estritamente diagonal dominante).");
            } else {
                System.out.println("Convergência garantida!");
            }

            gaussSeidel(matrizCoeficientes, b, x, tolerancia, maxIteracoes);
        }
    }
    private static SistemaLinear extrairSistema(String entrada) {
        String[] equacoes = entrada.replaceAll("[{}\\s]", "").split(",");
        Pattern padraoTermo = Pattern.compile("([+-]?\\d*\\.?\\d*)([a-zA-Z])");
        String variaveis = "";

        for (String equacao : equacoes) {
            String[] lados = equacao.split("=");
            if (lados.length != 2) {
                throw new IllegalArgumentException("Separe as equações por vírgulas e use '='.");
            }
            Matcher matcher = padraoTermo.matcher(lados[0]);
            while (matcher.find()) {
                String variavel = matcher.group(2);
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
                String coeficiente = matcher.group(1);
                double valor = coeficiente.isEmpty() || coeficiente.equals("+") ? 1
                        : coeficiente.equals("-") ? -1 : Double.parseDouble(coeficiente);
                matrizCoeficientes[i][variaveis.indexOf(matcher.group(2))] += valor;
            }
            termosIndependentes[i] = Double.parseDouble(lados[1]);
        }

        return new SistemaLinear(matrizCoeficientes, termosIndependentes);
    }

    private record SistemaLinear(double[][] matrizCoeficientes, double[] termosIndependentes) {
    }

    public static boolean verificarCriterioLinhas(double[][] matrizCoeficientes) {
        int n = matrizCoeficientes.length;
        boolean satisfeito = true;
        for (int i = 0; i < n; i++) {
            double soma = 0;
            for (int j = 0; j < n; j++) {
                if (j != i) soma += Math.abs(matrizCoeficientes[i][j]);
            }

            System.out.printf("Equação %d: |a%d%d| = %.6f; soma dos demais = ", i + 1, i + 1, i + 1,
                    Math.abs(matrizCoeficientes[i][i]));
            imprimirSomaLinha(matrizCoeficientes, i);
            System.out.printf(" = %.6f; %.6f > %.6f -> %s%n", soma,
                    Math.abs(matrizCoeficientes[i][i]), soma,
                    Math.abs(matrizCoeficientes[i][i]) > soma ? "sim" : "não");

            if (Math.abs(matrizCoeficientes[i][i]) <= soma) satisfeito = false;
        }
        return satisfeito;
    }

    public static boolean verificarCriterioColunas(double[][] matrizCoeficientes) {
        int n = matrizCoeficientes.length;
        boolean satisfeito = true;
        for (int j = 0; j < n; j++) {
            double soma = 0;
            for (int i = 0; i < n; i++) {
                if (i != j) soma += Math.abs(matrizCoeficientes[i][j]);
            }

            System.out.printf("Coluna %d: |a%d%d| = %.6f; soma dos demais = ", j + 1, j + 1, j + 1,
                    Math.abs(matrizCoeficientes[j][j]));
            imprimirSomaColuna(matrizCoeficientes, j);
            System.out.printf(" = %.6f; %.6f > %.6f -> %s%n", soma,
                    Math.abs(matrizCoeficientes[j][j]), soma,
                    Math.abs(matrizCoeficientes[j][j]) > soma ? "sim" : "não");

            if (Math.abs(matrizCoeficientes[j][j]) <= soma) satisfeito = false;
        }
        return satisfeito;
    }

    private static void imprimirSomaLinha(double[][] matrizCoeficientes, int linha) {
        boolean primeiroTermo = true;
        for (int j = 0; j < matrizCoeficientes.length; j++) {
            if (j == linha) continue;
            if (!primeiroTermo) System.out.print(" + ");
            System.out.printf("|a%d%d| (%.6f)", linha + 1, j + 1,
                    Math.abs(matrizCoeficientes[linha][j]));
            primeiroTermo = false;
        }
        if (primeiroTermo) System.out.print("0");
    }

    private static void imprimirSomaColuna(double[][] matrizCoeficientes, int coluna) {
        boolean primeiroTermo = true;
        for (int i = 0; i < matrizCoeficientes.length; i++) {
            if (i == coluna) continue;
            if (!primeiroTermo) System.out.print(" + ");
            System.out.printf("|a%d%d| (%.6f)", i + 1, coluna + 1,
                    Math.abs(matrizCoeficientes[i][coluna]));
            primeiroTermo = false;
        }
        if (primeiroTermo) System.out.print("0");
    }

    public static boolean critSassenfeld(double[][] matrizCoeficientes) {
        int n = matrizCoeficientes.length;
        if (n == 0) return false;

        double[] beta = new double[n];
        boolean satisfeito = true;
        for (int i = 0; i < n; i++) {
            double diagonal = Math.abs(matrizCoeficientes[i][i]);
            if (diagonal == 0) {
                System.out.printf("β%d: diagonal nula; não é possível calcular o quociente.%n", i + 1);
                return false;
            }

            double soma = 0;
            for (int j = 0; j < i; j++) {
                soma += Math.abs(matrizCoeficientes[i][j]) * beta[j];
            }
            for (int j = i + 1; j < n; j++) {
                soma += Math.abs(matrizCoeficientes[i][j]);
            }

            beta[i] = soma / diagonal;
            imprimirCalculoBeta(matrizCoeficientes, beta, i, diagonal, beta[i]);
            if (!Double.isFinite(beta[i]) || beta[i] >= 1) satisfeito = false;
        }
        return satisfeito;
    }

    private static void imprimirCalculoBeta(double[][] matrizCoeficientes, double[] beta,
            int linha, double diagonal, double resultado) {
        System.out.printf("β%d = (", linha + 1);
        boolean primeiroTermo = true;
        for (int j = 0; j < linha; j++) {
            if (!primeiroTermo) System.out.print(" + ");
            System.out.printf("|a%d%d| * β%d", linha + 1, j + 1, j + 1);
            primeiroTermo = false;
        }
        for (int j = linha + 1; j < beta.length; j++) {
            if (!primeiroTermo) System.out.print(" + ");
            System.out.printf("|a%d%d|", linha + 1, j + 1);
            primeiroTermo = false;
        }
        if (primeiroTermo) System.out.print("0");

        System.out.print(") / |a" + (linha + 1) + (linha + 1) + "| = (");
        primeiroTermo = true;
        for (int j = 0; j < linha; j++) {
            if (!primeiroTermo) System.out.print(" + ");
            System.out.printf("%.6f * %.6f", Math.abs(matrizCoeficientes[linha][j]), beta[j]);
            primeiroTermo = false;
        }
        for (int j = linha + 1; j < beta.length; j++) {
            if (!primeiroTermo) System.out.print(" + ");
            System.out.printf("%.6f", Math.abs(matrizCoeficientes[linha][j]));
            primeiroTermo = false;
        }
        if (primeiroTermo) System.out.print("0");
        System.out.printf(") / %.6f = %.6f; β%d < 1 -> %s%n", diagonal, resultado, linha + 1,
                resultado < 1 ? "sim" : "não");
    }

    public static void gaussSeidel(double[][] matrizCoeficientes, double[] b, double[] x, double tolerancia, int maxIteracoes) {

        int n = b.length;
        System.out.println();

        for (int iteracao = 1; iteracao <= maxIteracoes; iteracao++) {
            double maiorErro = 0;
            System.out.println("Iteração " + iteracao + ":");

            for (int i = 0; i < n; i++) {
                double soma = 0;
                for (int j = 0; j < n; j++) {
                    if (j != i) soma += matrizCoeficientes[i][j] * x[j];
                }

                double novoX = (b[i] - soma) / matrizCoeficientes[i][i];
                maiorErro = Math.max(maiorErro, Math.abs(novoX - x[i]));
                exibirCalculoIteracao(i, matrizCoeficientes, b, x, novoX);
                x[i] = novoX;
            }

            System.out.printf("Maior variação nesta iteração = %.8g%n", maiorErro);
            System.out.println();

            if (maiorErro < tolerancia) {
                System.out.println("Convergiu");
                exibirSolucao(x);
                return;
            }
        }

        System.out.println("Número máximo de iterações atingido sem convergência completa.");
        exibirSolucao(x);
    }

    private static void exibirCalculoIteracao(int i, double[][] matrizCoeficientes,
    double[] b, double[] x, double novoX) {
        
        System.out.printf("Equação %d: x%d = (%.6f - [", i + 1, i + 1, b[i]);

        boolean primeiroTermo = true;
        for (int j = 0; j < x.length; j++) {
            if (j == i) continue;
            if (!primeiroTermo) System.out.print(" + ");
            System.out.printf("(%.6f * x%d[%.6f])", matrizCoeficientes[i][j], j + 1, x[j]);
            primeiroTermo = false;
        }

        System.out.printf("]) / %.6f = %.6f%n", matrizCoeficientes[i][i], novoX);
    }

    private static void exibirSolucao(double[] x) {
        System.out.println("Solução aproximada:");
        for (int i = 0; i < x.length; i++) {
            System.out.printf("x%d = %.6f%n", i + 1, x[i]);
        }
    }
}
