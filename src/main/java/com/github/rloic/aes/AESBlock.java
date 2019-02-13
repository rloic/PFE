package com.github.rloic.aes;

import com.github.rloic.collections.Coordinates;
import com.github.rloic.collections.Pair;

public abstract class AESBlock {

    public final int keyRows;
    public final int keyColumns;
    public final int nbRounds;

    private AESBlock(int keyRows, int keyColumns, int nbRounds) {
        this.keyRows = keyRows;
        this.keyColumns = keyColumns;
        this.nbRounds = nbRounds;
    }

    abstract public boolean isInitialKey(int i, int k);
    abstract public Pair<Coordinates, Coordinates> xorKeySchedulePi(int i, int j, int k);
    abstract public boolean isSBox(int i, int k);
    abstract public int nbColumnsThroughSBox(int i);
    final public boolean isSBRound(int i) {
        return nbColumnsThroughSBox(i) == 1;
    }

    public static class AESBlock128 extends AESBlock {

        public final static AESBlock128 AES_BLOCK_128 = new AESBlock128();

        private AESBlock128() {
            super(4, 4, 10);
        }

        /*
            Picat:
            isInitialKey(_,1,_) => true.
         */
        @Override
        public boolean isInitialKey(int i, int k) {
            return i == 0;
        }

        /*
            Picat:
            xorKS(4,I,J,1) = L => L = sort([[I-1,J,1],[I-1,J,5]]).
            xorKS(4,I,J,K) = L => L = [[I-1,J,K],[I,J,K-1]].
         */
        @Override
        public Pair<Coordinates, Coordinates> xorKeySchedulePi(int i, int j, int k) {
            if (k == 0) {
                return new Pair<>(new Coordinates(i - 1, j, 0), new Coordinates(i - 1, j, 4));
            } else {
                // xorKeySchedulePi(4,I,J,K) = L => L = [[I-1,J,K],[I,J,K-1]].
                return new Pair<>(new Coordinates(i - 1, j, k), new Coordinates(i, j, k - 1));
            }
        }

        /*
            Picat: isSB(4,_,4) => true.
         */
        @Override
        public boolean isSBox(int i, int k) {
            return k == 3;
        }

        /*
            Picat: nbSCol(4, _) = N => N=1.
         */
        @Override
        public int nbColumnsThroughSBox(int i) {
            return 1;
        }

        @Override
        public String toString() {
            return "AES-128";
        }
    }

    public static class AESBlock192 extends AESBlock {

        public final static AESBlock192 AES_BLOCK_192 = new AESBlock192();

        public AESBlock192() {
            super(4, 6, 12);
        }

        /*
            Picat:
            isInitialKey(_,1,_) => true.
            isInitialKey(6,2,K), K <= 2 => true.
         */
        @Override
        public boolean isInitialKey(int i, int k) {
            if (i == 0) return true;
            if (i == 1 && k <= 1) return true;
            return false;
        }

        /*
            Picat:
            xorKS(6,I,J,2) = L => L=[[I-2,J,4],[I,J,1]].
            xorKS(6,I,J,4) = L => L=[[I-1,J,2],[I,J,3]].
            xorKS(6,I,J,1) = L, I mod 3 == 1 => L = [[I-2,J,3],[I-1,J,5]].
            xorKS(6,I,J,1) = L => L = [[I-2,J,3],[I-1,J,4]].
            xorKS(6,I,J,3) = L, I mod 3 == 2 => L = [[I-1,J,1],[I,J,5]].
            xorKS(6,I,J,3) = L => L = [[I-1,J,1],[I,J,2]].
         */
        @Override
        public Pair<Coordinates, Coordinates> xorKeySchedulePi(int i, int j, int k) {
            if (k == 1) {
                // xorKeySchedulePi(6,I,J,2) = L => L=[[I-2,J,4],[I,J,1]].
                return new Pair<>(new Coordinates(i - 2, j, 3), new Coordinates(i, j, 0));
            } else if (k == 3) {
                // xorKeySchedulePi(6,I,J,4) = L => L=[[I-1,J,2],[I,J,3]].
                return new Pair<>(new Coordinates(i - 1, j, 1), new Coordinates(i, j, 2));
            } else if (k == 0 && i % 3 == 0) {
                // xorKeySchedulePi(6,I,J,1) = L, I mod 3 == 1 => L = [[I-2,J,3],[I-1,J,5]].
                return new Pair<>(new Coordinates(i - 2, j, 2), new Coordinates(i - 1, j, 4));
            } else if (k == 0) {
                // xorKeySchedulePi(6,I,J,1) = L => L = [[I-2,J,3],[I-1,J,4]].
                return new Pair<>(new Coordinates(i - 2, j, 2), new Coordinates(i - 1, j, 3));
            } else if (k == 2 && i % 3 == 1) {
                // xorKeySchedulePi(6,I,J,3) = L, I mod 3 == 2 => L = [[I-1,J,1],[I,J,5]].
                return new Pair<>(new Coordinates(i - 1, j, 0), new Coordinates(i, j, 4));
            } else if (k == 2) {
                // xorKeySchedulePi(6,I,J,3) = L => L = [[I-1,J,1],[I,J,2]].
                return new Pair<>(new Coordinates(i - 1, j, 0), new Coordinates(i, j, 1));
            }
            throw new IllegalStateException();
        }

        /*
            Picat: isSB(6,I,K), (4*(I-1)+K-1) mod 6 == 5 => true.
         */
        @Override
        public boolean isSBox(int i, int k) {
            return (4 * i + k) % 6 == 5;
        }

        /*
            Picat:
            nbSCol(6, I) = N, (I mod 3) == 0 => N=1.
            nbSCol(6, I) = N, (I mod 3) == 2 => N=1.
            nbSCol(6, _) = N => N=0.
         */
        @Override
        public int nbColumnsThroughSBox(int i) {
            return (i % 3 != 0) ? 1 : 0;
        }

        @Override
        public String toString() {
            return "AES-192";
        }
    }

    public static class AESBlock256 extends AESBlock {

        public final static AESBlock256 AES_BLOCK_256 = new AESBlock256();

        private AESBlock256() {
            super(4, 8, 14);
        }

        /*
            Picat:
            isInitialKey(_,1,_) => true.
            isInitialKey(8,2,_) => true. % AES-256
         */
        @Override
        public boolean isInitialKey(int i, int k) {

            if (i == 0) return true;
            if (i == 1) return true;
            return false;
        }

        /*
            Picat:
            xorKS(8,I,J,1) = L, I mod 2 == 1 => L = [[I-2,J,1],[I-1,J,5]].
            xorKS(8,I,J,1) = L => L = [[I-2,J,1],[I-1,J,5]].
            xorKS(8,I,J,K) = L => L = [[I-2,J,K],[I,J,K-1]].

         */
        @Override
        public Pair<Coordinates, Coordinates> xorKeySchedulePi(int i, int j, int k) {
            if (k == 0 && i % 2 == 0) {
                // xorKeySchedulePi(8,I,J,1) = L, I mod 2 == 1 => L = [[I-2,J,1],[I-1,J,5]].
                return new Pair<>(new Coordinates(i - 2, j, 0), new Coordinates(i - 1, j, 4));
            } else if (k == 0) {
                // xorKeySchedulePi(8,I,J,1) = L => L = [[I-2,J,1],[I-1,J,5]].
                return new Pair<>(new Coordinates(i - 2, j, 0), new Coordinates(i - 1, j, 4));
            } else {
                // xorKeySchedulePi(8,I,J,K) = L => L = [[I-2,J,K],[I,J,K-1]].
                return new Pair<>(new Coordinates(i - 2, j, k), new Coordinates(i, j, k - 1));
            }
        }

        /*
            Picat: isSB(8,I,4), I > 1 => true.
         */
        @Override
        public boolean isSBox(int i, int k) {
            return i > 0 && k == 3;
        }

        /*
            Picat:
            nbSCol(8, 1) = N => N=0.
            nbSCol(8, _) = N => N=1.
         */
        @Override
        public int nbColumnsThroughSBox(int i) {
            return (i > 0) ? 1 : 0;
        }

        @Override
        public String toString() {
            return "AES-256";
        }
    }

}