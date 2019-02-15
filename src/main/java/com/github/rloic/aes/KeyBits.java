package com.github.rloic.aes;

import com.github.rloic.collections.BytePosition;
import com.github.rloic.collections.Pair;

public abstract class KeyBits {

    public final int keyRows;
    public final int keyColumns;
    public final int nbRounds;

    private KeyBits(int keyRows, int keyColumns, int nbRounds) {
        this.keyRows = keyRows;
        this.keyColumns = keyColumns;
        this.nbRounds = nbRounds;
    }

    abstract public boolean isInitialKey(int i, int k);

    abstract public Pair<BytePosition, BytePosition> xorKeySchedulePi(int i, int j, int k);

    abstract boolean isSBRound(int i);

    abstract int getNbCol(int i);

    public static class AES128 extends KeyBits {

        public final static AES128 AES_128 = new AES128();

        private AES128() {
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
        public Pair<BytePosition, BytePosition> xorKeySchedulePi(int i, int j, int k) {
            if (k == 0) {
                return new Pair<>(new BytePosition(i - 1, j, 0), new BytePosition(i - 1, j, 4));
            } else {
                // xorKeySchedulePi(4,I,J,K) = L => L = [[I-1,J,K],[I,J,K-1]].
                return new Pair<>(new BytePosition(i - 1, j, k), new BytePosition(i, j, k - 1));
            }
        }

        @Override
        int getNbCol(int i) {
            return 4;
        }

        @Override
        boolean isSBRound(int i) {
            return true;
        }

        @Override
        public String toString() {
            return "AES-128";
        }
    }

    public static class AES192 extends KeyBits {

        public final static AES192 AES_192 = new AES192();

        private AES192() {
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

        @Override
        public Pair<BytePosition, BytePosition> xorKeySchedulePi(int i, int j, int k) {
            if (k == 1) {
                // xorKeySchedulePi(6,I,J,2) = L => L=[[I-2,J,4],[I,J,1]].
                return new Pair<>(new BytePosition(i - 2, j, 3), new BytePosition(i, j, 0));
            } else if (k == 3) {
                // xorKeySchedulePi(6,I,J,4) = L => L=[[I-1,J,2],[I,J,3]].
                return new Pair<>(new BytePosition(i - 1, j, 1), new BytePosition(i, j, 2));
            } else if (k == 0 && i % 3 == 0) {
                // xorKeySchedulePi(6,I,J,1) = L, I mod 3 == 1 => L = [[I-2,J,3],[I-1,J,5]].
                return new Pair<>(new BytePosition(i - 2, j, 2), new BytePosition(i - 1, j, 4));
            } else if (k == 0) {
                // xorKeySchedulePi(6,I,J,1) = L => L = [[I-2,J,3],[I-1,J,4]].
                return new Pair<>(new BytePosition(i - 2, j, 2), new BytePosition(i - 1, j, 3));
            } else if (k == 2 && i % 3 == 1) {
                // xorKeySchedulePi(6,I,J,3) = L, I mod 3 == 2 => L = [[I-1,J,1],[I,J,5]].
                return new Pair<>(new BytePosition(i - 1, j, 0), new BytePosition(i, j, 4));
            } else if (k == 2) {
                // xorKeySchedulePi(6,I,J,3) = L => L = [[I-1,J,1],[I,J,2]].
                return new Pair<>(new BytePosition(i - 1, j, 0), new BytePosition(i, j, 1));
            }
            throw new IllegalStateException();
        }

        @Override
        boolean isSBRound(int i) {
            return i % 3 != 0;
        }

        @Override
        int getNbCol(int i) {
            return (i % 3 == 1) ? 1 : 3;
        }

        @Override
        public String toString() {
            return "AES-192";
        }
    }

    public static class AES256 extends KeyBits {

        public final static AES256 AES_256 = new AES256();

        private AES256() {
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

        @Override
        public Pair<BytePosition, BytePosition> xorKeySchedulePi(int i, int j, int k) {
            if (k == 0 && i % 2 == 0) {
                // xorKeySchedulePi(8,I,J,1) = L, I mod 2 == 1 => L = [[I-2,J,1],[I-1,J,5]].
                return new Pair<>(new BytePosition(i - 2, j, 0), new BytePosition(i - 1, j, 4));
            } else if (k == 0) {
                // xorKeySchedulePi(8,I,J,1) = L => L = [[I-2,J,1],[I-1,J,5]].
                return new Pair<>(new BytePosition(i - 2, j, 0), new BytePosition(i - 1, j, 4));
            } else {
                // xorKeySchedulePi(8,I,J,K) = L => L = [[I-2,J,K],[I,J,K-1]].
                return new Pair<>(new BytePosition(i - 2, j, k), new BytePosition(i, j, k - 1));
            }
        }

        @Override
        boolean isSBRound(int i) {
            return i > 0;
        }

        @Override
        int getNbCol(int i) {
            return 3;
        }

        @Override
        public String toString() {
            return "AES-256";
        }
    }

}