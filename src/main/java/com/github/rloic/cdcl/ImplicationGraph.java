package com.github.rloic.cdcl;

import java.util.ArrayList;
import java.util.List;

public class ImplicationGraph<T> {
   private final List<Node<T>> nodes;

   public ImplicationGraph() {
      this.nodes = new ArrayList<>();
   }

   public ImplicationGraph(List<Node<T>> nodes) {
      this.nodes = nodes;
   }
}
