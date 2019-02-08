package com.github.rloic;

public enum AESBlock {

    AES128(4, 4, 10),
    AES192(6, 6, 12),
    AES256(8, 8, 14);

    public final int keyRows;
    public final int keyColumns;
    public final int nbRounds;

    AESBlock(int keyRows, int keyColumns, int nbRounds) {
        this.keyRows = keyRows;
        this.keyColumns = keyColumns;
        this.nbRounds = nbRounds;
    }


}
