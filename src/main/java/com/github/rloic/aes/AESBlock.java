package com.github.rloic.aes;

import com.github.rloic.collections.IntTuple;

import static com.github.rloic.collections.ArrayExtensions.arrayOf;

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
    abstract public IntTuple[] xorKeySchedule(int i, int j, int k);

    public static class AESBlock128 extends AESBlock {

        public final static AESBlock128 AES_BLOCK_128 = new AESBlock128();

        private AESBlock128() {
            super(4, 4, 10);
        }

        @Override
        public boolean isInitialKey(int i, int k) {
            return i == 1;
        }

        @Override
        public IntTuple[] xorKeySchedule(int i, int j, int k) {
            if (k == 1) {
                // xorKeySchedule(4,I,J,1) = L => L = sort([[I-1,J,1],[I-1,J,5]]).
                // Do not sort because the calling function resort the results
                return arrayOf(new IntTuple(i - 1, j, 1), new IntTuple(i - 1, j, 5));
            } else {
                // xorKeySchedule(4,I,J,K) = L => L = [[I-1,J,K],[I,J,K-1]].
                return arrayOf(new IntTuple(i - 1, j, k), new IntTuple(i, j, k - 1));
            }
        }
    }

    public static class AESBlock192 extends AESBlock {

        public final static AESBlock192 AES_BLOCK_192 = new AESBlock192();

        public AESBlock192() {
            super(6, 6, 12);
        }

        @Override
        public boolean isInitialKey(int i, int k) {
            // isInitialKey(_,1,_) => true.
            // isInitialKey(6,2,K), K <= 2 => true. % AES-192
            if (i == 1) return true;
            if (i == 2 && k <= 2) return true;
            return false;
        }

        @Override
        public IntTuple[] xorKeySchedule(int i, int j, int k) {
            if (k == 2) {
                // xorKeySchedule(6,I,J,2) = L => L=[[I-2,J,4],[I,J,1]].
                return arrayOf(new IntTuple(i - 2, j, 4), new IntTuple(i, j, 1));
            } else if (k == 4) {
                // xorKeySchedule(6,I,J,4) = L => L=[[I-1,J,2],[I,J,3]].
                return arrayOf(new IntTuple(i - 1, j, 2), new IntTuple(i, j, 3));
            } else if (k == 1 && i % 3 == 1) {
                // xorKeySchedule(6,I,J,1) = L, I mod 3 == 1 => L = [[I-2,J,3],[I-1,J,5]].
                return arrayOf(new IntTuple(i - 2, j, 3), new IntTuple(i - 1, j, 5));
            } else if (k == 1) {
                // xorKeySchedule(6,I,J,1) = L => L = [[I-2,J,3],[I-1,J,4]].
                return arrayOf(new IntTuple(i - 2, j, 3), new IntTuple(i - 1, j, 4));
            } else if (k == 3 && i % 3 == 2) {
                // xorKeySchedule(6,I,J,3) = L, I mod 3 == 2 => L = [[I-1,J,1],[I,J,5]].
                return arrayOf(new IntTuple(i - 1, j, 1), new IntTuple(i, j, 5));
            } else if (k == 3) {
                // xorKeySchedule(6,I,J,3) = L => L = [[I-1,J,1],[I,J,2]].
                return arrayOf(new IntTuple(i - 1, j, 1), new IntTuple(i, j, 2));
            }
            throw new IllegalStateException();
        }
    }

    public static class AESBlock256 extends AESBlock {

        public final static AESBlock256 AES_BLOCK_256 = new AESBlock256();

        private AESBlock256() {
            super(8, 8, 14);
        }

        @Override
        public boolean isInitialKey(int i, int k) {
            // isInitialKey(_,1,_) => true.
            // isInitialKey(8,2,_) => true. % AES-256
            if (i == 1) return true;
            if (i == 2) return true;
            return false;
        }

        @Override
        public IntTuple[] xorKeySchedule(int i, int j, int k) {
            if (k == 1 && i % 2 == 1) {
                // xorKeySchedule(8,I,J,1) = L, I mod 2 == 1 => L = [[I-2,J,1],[I-1,J,5]].
                return arrayOf(new IntTuple(i - 2, j, 1), new IntTuple(i - 1, j, 5));
            } else if (k == 1) {
                // xorKeySchedule(8,I,J,1) = L => L = [[I-2,J,1],[I-1,J,5]].
                return arrayOf(new IntTuple(i - 2, j, 1), new IntTuple(i - 1, j, 5));
            } else {
                // xorKeySchedule(8,I,J,K) = L => L = [[I-2,J,K],[I,J,K-1]].
                return arrayOf(new IntTuple(i - 2, j, k), new IntTuple(i, j, k - 1));
            }
        }
    }

}