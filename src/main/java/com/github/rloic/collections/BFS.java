package com.github.rloic.collections;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BFS {

   public static void main(String[] args) {

      int[][] equations = new int[][]{
            new int[]{7, 32, 62},
            new int[]{0, 22, 27},
            new int[]{93, 41, 103},
            new int[]{25, 11, 105},
            new int[]{24, 9, 94},
            new int[]{16, 3, 75},
            new int[]{17, 42, 39},
            new int[]{74, 56, 58},
            new int[]{69, 8, 5},
            new int[]{110, 31, 18},
            new int[]{40, 37, 49},
            new int[]{57, 61, 60},
            new int[]{101, 35, 109},
            new int[]{89, 65, 73},
            new int[]{83, 12, 46},
            new int[]{91, 80, 21},
            new int[]{47, 100, 59},
            new int[]{34, 2, 79},
            new int[]{53, 54, 92},
            new int[]{67, 70, 43},
            new int[]{1, 106, 38},
            new int[]{97, 85, 20},
            new int[]{96, 66, 95},
            new int[]{23, 84, 44},
            new int[]{50, 28, 82},
            new int[]{86, 26, 81},
            new int[]{15, 4, 14},
            new int[]{30, 102, 29},
            new int[]{19, 51, 108},
            new int[]{48, 36, 55},
            new int[]{77, 111, 13},
            new int[]{52, 45, 107},
            new int[]{100, 32, 56},
            new int[]{106, 9, 61},
            new int[]{28, 8, 80},
            new int[]{51, 35, 11},
            new int[]{100, 2, 22},
            new int[]{106, 85, 3},
            new int[]{28, 26, 31},
            new int[]{51, 36, 65},
            new int[]{32, 64, 104},
            new int[]{9, 6, 76},
            new int[]{8, 68, 78},
            new int[]{35, 99, 63},
            new int[]{2, 54, 41},
            new int[]{85, 66, 42},
            new int[]{26, 4, 37},
            new int[]{36, 111, 12},
            new int[]{54, 70, 11},
            new int[]{66, 84, 56},
            new int[]{4, 102, 61},
            new int[]{111, 45, 80},
            new int[]{32, 22, 98},
            new int[]{9, 3, 90},
            new int[]{8, 31, 88},
            new int[]{35, 65, 33},
            new int[]{22, 41, 87},
            new int[]{3, 42, 71},
            new int[]{31, 37, 72},
            new int[]{65, 12, 10},
            new int[]{41, 11, 63},
            new int[]{42, 56, 104},
            new int[]{37, 61, 76},
            new int[]{12, 80, 78},
      };

      int len = 112;
      boolean[][] graph = new boolean[112][];
      for (int i = 0; i < 112; i++) {
         graph[i] = new boolean[112];
      }

      for (int[] equation : equations) {
         for (int j = 0; j < equation.length; j++) {
            for (int k = j + 1; k < equation.length; k++) {
               graph[equation[j]][equation[k]] = true;
               graph[equation[k]][equation[j]] = true;
            }
         }
      }


      for (int i = 0; i < graph.length; i++) {
         for (int j = 0; j < graph[i].length; j++) {
            if (graph[i][j]) System.out.print('1');
            else System.out.print('0');
         }
         System.out.println();
      }

      System.out.println(isolatedSubGraphs(graph).get(0).size());


   }

   private static int firstUnmarked(boolean[] marked) {
      int i = 0;
      while (i < marked.length && marked[i]) {
         i += 1;
      }
      if (i == marked.length) {
         return -1;
      } else {
         return i;
      }
   }

   private static boolean all(boolean[] marked) {
      int i = 0;
      while (i < marked.length && marked[i]) {
         i += 1;
      }
      return i == marked.length;
   }

   private static IntList nei(boolean[][] graph, int n) {
      IntList nei = new IntArrayList();
      for (int j = 0; j < graph[n].length; j++) {
         if (graph[n][j]) nei.add(j);
      }
      return nei;
   }

   public static List<IntList> isolatedSubGraphs(boolean[][] graph) {
      IntList f = new IntArrayList();
      boolean[] isMarked = new boolean[graph.length];
      List<IntList> components = new ArrayList<>();
      while (!all(isMarked)) {
         int n = firstUnmarked(isMarked);
         f.add(n);
         isMarked[n] = true;
         IntList component = new IntArrayList();

         while (!f.isEmpty()) {
            int s = f.removeInt(0);
            component.add(s);
            if (s < graph.length) {
               for (int nei : nei(graph, s)) {
                  if (!isMarked[nei]) {
                     f.add(nei);
                     isMarked[nei] = true;
                  }
               }
            }
         }
         components.add(component);
      }
      return components;
   }

}
