package com.github.rloic.paper;

import java.util.function.Function;

public abstract class Result<T, E extends Exception> {

   static class Ok<T, E extends Exception> extends Result<T, E> {

      public final T value;
      private Ok(T value) {
         this.value = value;
      }

      @Override
      public boolean isOk() {
         return true;
      }

      @Override
      public boolean isErr() {
         return false;
      }

      @Override
      public <U> Result<U, E> map(Function<T, U> transform) {
         return new Ok<>(transform.apply(value));
      }

      @Override
      public <U> Result<U, E> flatMap(Function<T, Result<U, E>> transform) {
         return transform.apply(value);
      }

      @Override
      public T orFail() {
         return value;
      }
   }

   static class Err<T, E extends Exception> extends Result<T, E> {

      public final E err;
      private Err(E err) {
         this.err = err;
      }

      @Override
      public boolean isOk() {
         return false;
      }

      @Override
      public boolean isErr() {
         return true;
      }

      @Override
      public <U> Result<U, E> map(Function<T, U> transform) {
         return new Err<>(err);
      }

      @Override
      public <U> Result<U, E> flatMap(Function<T, Result<U, E>> transform) {
         return (Result<U, E>) this;
      }

      @Override
      public T orFail() throws E {
         throw err;
      }
   }

   public abstract boolean isOk();
   public abstract boolean isErr();

   public abstract <U> Result<U, E> map(Function<T, U> transform);
   public abstract <U> Result<U, E> flatMap(Function<T, Result<U, E>> transform);

   public abstract T orFail() throws E;

   public static <T, E extends Exception> Result<T, E> Err(E err) {
      return new Err<>(err);
   }
   public static <T, E extends Exception> Result<T, E> Ok(T value) {
      return new Ok<>(value);
   }
   public static <Void, E extends Exception> Result<Void, E> Ok() {
      return new Ok<>(null);
   }

}
