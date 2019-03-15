package com.github.rloic.benchmark;

import com.github.rloic.aes.KeyBits;

import java.util.concurrent.TimeUnit;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;

public class Experiment {

   public final int round;
   public final int objStep1;
   public final KeyBits key;
   public final long timeout;
   public final TimeUnit unit;

   Experiment(int round, int objStep1, KeyBits key, long timeout, TimeUnit unit) {
      this.round = round;
      this.objStep1 = objStep1;
      this.key = key;
      this.timeout = timeout;
      this.unit = unit;
   }

   @Override
   public String toString() {
      String keyStr = "";
      if (key == AES_128) {
         keyStr = "AES-128";
      } else if (key == AES_192) {
         keyStr = "AES-192";
      } else if (key == AES_256) {
         keyStr = "AES-256";
      }
      return keyStr + "-" + round + "_" + objStep1;
   }
}