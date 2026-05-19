package com.cadac.stone_inscription.report.specification;

public interface Specification<T> {

    boolean isSatisfiedBy(T candidate);

    String errorMessage();
}
