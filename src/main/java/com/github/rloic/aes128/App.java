package com.github.rloic.aes128;

public class App {

    public static void main(String[] args) {
            System.out.println("r="+3+" and objStep1="+2);
            new BasicAESSolver(null, 3, 2);
            System.out.println("r="+3+" and objStep1="+3);
            new BasicAESSolver(null, 3, 3);
            System.out.println("r="+3+" and objStep1="+4);
            new BasicAESSolver(null, 3, 4);
    }

}
