package de.craftsblock.craftsnet.builder;

public interface MappableState<E extends Enum<E> & MappableState<E>> {

    E getMapTo();

}
