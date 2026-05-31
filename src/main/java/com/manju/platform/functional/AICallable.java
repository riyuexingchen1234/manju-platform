package com.manju.platform.functional;

@FunctionalInterface
public interface AICallable<T> {
    T execute();
}

/* 泛型函数式接口 = 只有一个方法的接口 + 可以返回任意类型的数据
* 只有一个抽象方法的接口叫函数式接口。
* @FunctionalInterface 用来标记和强制检查这个接口是不是符合函数式接口的要求，
* 如果不小心加了第二个抽象方法，直接报错。
* <T> 就是泛型，意思是 “这个类型我现在不确定，用的时候再指定”。 */