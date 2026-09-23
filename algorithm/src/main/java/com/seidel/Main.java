package com.seidel;

import java.util.Locale;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            sc.useLocale(Locale.US);

            System.out.print("Defina a quantidade de equações do sistema: ");
            int n = sc.nextInt();

            double[][] A = new double[n][n];
            double[] b = new double[n];
            double[] x = new double[n];

            for (int i = 0; i < n; i++) {

                System.out.println("Equação " + (i + 1));

                for (int j = 0; j < n; j++) {

                    System.out.print("Coeficiente de x" + (j + 1) + ": ");
                    A[i][j] = sc.nextDouble();
                }

                System.out.print("Termo independente: ");

                b[i] = sc.nextDouble();
            }

            System.out.print("\nDefina a tolerância (ex: 0.001): ");
            double tolerancia = sc.nextDouble();

            System.out.print("Defina o número máximo de iterações: ");
            int maxIteracoes = sc.nextInt();

            boolean critLinhas = verificarCriterioLinhas(A);
            boolean critColunas = verificarCriterioColunas(A);

            System.out.println("Análise de Convergência (Diagonal Dominante):");

            System.out.println("Critério das Linhas:  " + (critLinhas ? "Satisfeito" : "Não satisfeito"));
            
            System.out.println("Critério das Colunas: " + (critColunas ? "Satisfeito" : "Não satisfeito"));

            if (!critLinhas && !critColunas) {
                System.out.println("Aviso: O método pode não convergir (matriz não é estritamente diagonal dominante).");
            } else {
                System.out.println("Convergência garantida!");
            }

            gaussSeidel(A, b, x, tolerancia, maxIteracoes);
        }
    }

    // Critério das Linhas: |A[i][i]| > soma(|A[i][j]| para j != i)
    public static boolean verificarCriterioLinhas(double[][] A) {
        int n = A.length;
        for (int i = 0; i < n; i++) {
            double soma = 0;
            for (int j = 0; j < n; j++) {
                if (j != i) soma += Math.abs(A[i][j]);
            }
            if (Math.abs(A[i][i]) <= soma) return false;
        }
        return true;
    }

    // Critério das Colunas: |A[j][j]| > soma(|A[i][j]| para i != j)
    public static boolean verificarCriterioColunas(double[][] A) {
        int n = A.length;
        for (int j = 0; j < n; j++) {
            double soma = 0;
            for (int i = 0; i < n; i++) {
                if (i != j) soma += Math.abs(A[i][j]);
            }
            if (Math.abs(A[j][j]) <= soma) return false;
        }
        return true;
    }

    public static void gaussSeidel(double[][] A, double[] b, double[] x, double tolerancia, int maxIteracoes) {

        int n = b.length;

        for (int iteracao = 1; iteracao <= maxIteracoes; iteracao++) {
            double maiorErro = 0;

            for (int i = 0; i < n; i++) {
                double soma = 0;
                for (int j = 0; j < n; j++) {
                    if (j != i) soma += A[i][j] * x[j];
                }

                double novoX = (b[i] - soma) / A[i][i];
                maiorErro = Math.max(maiorErro, Math.abs(novoX - x[i]));
                x[i] = novoX;
            }

            System.out.println("Iteração " + iteracao +":");
            for (int i = 0; i < n; i++) {
                System.out.printf("x%d = %.6f  ", i + 1, x[i]);
            }

            System.out.println("Erro = " + maiorErro);

            if (maiorErro < tolerancia) {
                System.out.println("Convergiu");
                exibirSolucao(x);
                return;
            }
        }

        System.out.println("Número máximo de iterações atingido sem convergência completa.");
        exibirSolucao(x);
    }

    private static void exibirSolucao(double[] x) {
        System.out.println("Solução aproximada:");
        for (int i = 0; i < x.length; i++) {
            System.out.printf("x%d = %.6f%n", i + 1, x[i]);
        }
    }
}