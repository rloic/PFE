package com.github.rloic.common.collections;

import java.util.Arrays;

public class UnionFind {

    private final int[] parentOf;

    public UnionFind(int nbVariables) {
        this.parentOf = new int[nbVariables];
        for(int variable = 0; variable < nbVariables; variable++) {
            parentOf[variable] = variable;
        }
    }

    public UnionFind(UnionFind other) {
        this.parentOf = ArrayExtensions.deepCopy(other.parentOf);
    }

    public boolean sameSet(int lhs, int rhs) {
        return find(lhs) == find(rhs);
    }

    public int find(int variable) {
        int node = variable;
        int parent = parentOf[node];
        while (parent != node) {
            node = parent;
            parent = parentOf[node];
        }
        return parent;
    }

    public void union(int lhs, int rhs) {
        int lhsRoot = find(lhs);
        int rhsRoot = find(rhs);
        if(lhsRoot != rhsRoot) {
            parentOf[lhsRoot] = rhsRoot;
        }
    }

    public void detach(int lhs) {
        parentOf[lhs] = lhs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnionFind)) return false;
        UnionFind unionFind = (UnionFind) o;
        return Arrays.equals(parentOf, unionFind.parentOf);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(parentOf);
    }
}
