package com.github.rloic.cdcl;

import java.util.ArrayList;
import java.util.List;

public class Node<T> {
   public final T value;
   public final List<T> parents = new ArrayList<>();
   public final List<T> children = new ArrayList<>();

   public Node(T value) {
      this.value = value;
   }
}
