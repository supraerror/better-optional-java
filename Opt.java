import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * A container object which may or may not contain a non-null value.
 *
 * @param <T> the type of the value
 * @author <a href="https://github.com/supraerror">supraerror</a>
 * @version 1.1
 */
public sealed interface Opt<T> {

    String THE_VALUE_CAN_T_BE_NULL = "The value can't be null";
    String NO_VALUE_PRESENT = "No value present";

    /**
     * Returns an {@code Opt} with the specified non-null value.
     *
     * @param value the value to be present, must not be null
     * @param <T>   the type of the value
     * @return an {@code Opt} with the value present
     * @throws NullPointerException if the value is null
     * @since 1.1
     */
    static <T> Opt<T> of(T value) {
        return new Some<>(requireNonNull(value, THE_VALUE_CAN_T_BE_NULL));
    }

    /**
     * Flattens a stream of {@code Opt} objects to a stream of values.
     *
     * @param opts a stream of {@code Opt} objects
     * @param <T>  the type of the values
     * @return a stream containing all the present values
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    static <T> Stream<T> flatten(Stream<Opt<T>> opts) {
        return requireNonNull(opts, THE_VALUE_CAN_T_BE_NULL).flatMap(Opt::stream);
    }

    /**
     * Map the provided {@code Optional} to an {@code Opt} with the specified non-null value.<br>
     * <p>
     * If the provided {@code Optional} is non-null, it will be mapped to an {@code Opt}
     * with the contained value (if present), or {@code none} if the {@code Optional} is empty.
     *
     * @param value the {@code Optional} to be mapped, must not be null
     * @param <T>   the type of the value inside the {@code Optional}
     * @return an {@code Opt} containing the value if present, or {@code Opt.none()} if the {@code Optional} is empty
     * @throws NullPointerException if the {@code Optional} is null
     * @since 1.1
     */
    static <T> Opt<T> of(Optional<T> value) {
        return requireNonNull(value, THE_VALUE_CAN_T_BE_NULL)
                .map(Opt::of)
                .orElseGet(Opt::none);
    }

    /**
     * Returns an {@code Opt} describing the specified value, if non-null,
     * otherwise returns an empty {@code Opt}.
     *
     * @param value the possibly-null value to describe
     * @param <T>   the type of the value
     * @return an {@code Opt} with a present value if the specified value is non-null,
     * otherwise an empty {@code Opt}
     * @since 1.1
     */
    static <T> Opt<T> ofNullable(T value) {
        return (value == null) ? none() : new Some<>(value);
    }

    /**
     * Returns an empty {@code Opt} instance.
     *
     * @param <T> the type of the non-existent value
     * @return an empty {@code Opt}
     * @since 1.1
     */
    @SuppressWarnings("unchecked")
    static <T> Opt<T> none() {
        return (Opt<T>) None.INSTANCE;
    }


    /**
     * Sequences a stream of {@code Opt} objects into an {@code Opt} containing a stream of values.
     * If any {@code Opt} in the stream is empty, returns an empty {@code Opt}.
     *
     * @param opts a stream of {@code Opt} objects
     * @param <T>  the type of the values
     * @return an {@code Opt} containing a stream of values if all are present, otherwise empty
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    static <T> Opt<Stream<T>> sequence(Stream<Opt<T>> opts) {
        var list = requireNonNull(opts, THE_VALUE_CAN_T_BE_NULL).toList();
        return (list.stream().allMatch(Opt::isPresent)) ? Opt.of(list.stream().map(Opt::get)) : none();
    }

    /**
     * Flattens a nested {@code Opt} into a single level {@code Opt}.
     *
     * @param nested the nested {@code Opt}
     * @param <T>    the type of the inner value
     * @return the flattened {@code Opt}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    static <T> Opt<T> flatten(Opt<Opt<T>> nested) {
        return requireNonNull(nested, THE_VALUE_CAN_T_BE_NULL).flatMap(Function.identity());
    }


    /**
     * Returns a sequential {@code Iterator} containing the value if present,
     * otherwise an empty {@code Iterator}.
     *
     * @return a {@code Iterator} of the value, or empty if not present
     * @since 1.1
     */
    Iterator<T> iterator();

    /**
     * Accepts a visitor to process this {@code Opt} instance.
     *
     * @param visitor the visitor instance
     * @param <R>     the type of the result produced by the visitor
     * @return the result produced by the visitor
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    <R> R accept(OptVisitor<T, R> visitor);

    /**
     * Applies the function contained in {@code optFn} to this value if both are present.
     *
     * @param optFn an {@code Opt} containing a function to apply
     * @param <U>   the type of the result after applying the function
     * @return an {@code Opt} containing the result, or empty if either is empty
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default <U> Opt<U> ap(Opt<Function<? super T, ? extends U>> optFn) {
        return (this.isPresent() && requireNonNull(optFn, THE_VALUE_CAN_T_BE_NULL).isPresent()) ? Opt.ofNullable(optFn.get().apply(this.get())) : Opt.none();
    }

    /**
     * Combines this {@code Opt} with another {@code Opt} using the provided function.
     * If both values are present, the function is applied to produce a new result.
     * Otherwise, an empty {@code Opt} is returned.
     *
     * @param other  the other {@code Opt} to combine with
     * @param zipper the function that combines both values
     * @param <U>    the type of the other value
     * @param <R>    the type of the result
     * @return an {@code Opt} containing the combined result if both values are present, otherwise an empty {@code Opt}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default <U, R> Opt<R> zip(Opt<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return (this.isPresent() && requireNonNull(other, THE_VALUE_CAN_T_BE_NULL).isPresent()) ? Opt.ofNullable(zipper.apply(this.get(), other.get())) : Opt.none();
    }

    /**
     * If no value is present, executes the provided {@code action}.
     *
     * @param action the action to be executed if no value is present
     * @return this {@code Opt}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default Opt<T> ifEmpty(Runnable action) {
        if (isEmpty()) {
            requireNonNull(action).run();
        }
        return this;
    }

    /**
     * Converts this {@code Opt} to a {@code List}.
     * If a value is present, returns a single-element list containing the value.
     * Otherwise, returns an empty list.
     *
     * @return a list containing the value if present, otherwise an empty list.
     * @since 1.1
     */
    default List<T> toList() {
        return isPresent() ? List.of(get()) : List.of();
    }

    /**
     * Converts this {@code Opt} to a {@code Set}.
     * If a value is present, returns a single-element set containing the value.
     * Otherwise, returns an empty set.
     *
     * @return a set containing the value if present, otherwise an empty set.
     * @since 1.1
     */
    default Set<T> toSet() {
        return isPresent() ? Set.of(get()) : Set.of();
    }

    /**
     * Converts this {@code Opt} to a {@code Map} with a single key-value pair.
     * If a value is present, applies the given key and value mapping functions to the value.
     * Otherwise, returns an empty map.
     *
     * @param keyMapper   a function to generate a key from the value.
     * @param valueMapper a function to generate a value from the value.
     * @param <K>         the key type.
     * @param <V>         the value type.
     * @return a map containing a single entry if a value is present, otherwise an empty map.
     * @throws NullPointerException if the mapping functions return {@code null}.
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default <K, V> Map<K, V> toMap(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return isPresent() ? Map.of(requireNonNull(keyMapper).apply(get()), valueMapper.apply(get())) : Map.of();
    }

    /**
     * Transforms this {@code Opt} using a provided function.
     * The function receives this {@code Opt} as input and returns a new value.
     *
     * @param transformer the transformation function.
     * @param <U>         the type of the transformed value.
     * @return an {@code Opt} containing the transformed value, or an empty {@code Opt} if the transformation returns {@code null}.
     * @throws NullPointerException if the transformer function is {@code null}.
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default <U> Opt<U> transform(Function<? super Opt<T>, ? extends U> transformer) {
        return Opt.ofNullable(requireNonNull(transformer).apply(this));
    }

    /**
     * If a value is present, applies the given function and returns this {@code Opt}.
     * Otherwise, does nothing.
     *
     * @param action the action to apply if a value is present
     * @return this {@code Opt}
     * @throws NullPointerException if input is null
     * @since 1.3
     */
    default Opt<T> andThen(Consumer<? super T> action) {
        if (isPresent()) {
            requireNonNull(action).accept(get());
        }
        return this;
    }


    /**
     * Applies the given function if a value is present, otherwise applies the fallback function.
     * This is similar to {@code flatMap()}, but it provides an alternative in case of {@code None}.
     *
     * @param mapper   the function to apply if a value is present.
     * @param fallback the function to apply if no value is present.
     * @param <U>      the type of the result.
     * @return the result of applying either the mapper function or the fallback function.
     * @throws NullPointerException if either function is {@code null}.
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default <U> Opt<U> flatMapOrElse(Function<? super T, Opt<U>> mapper, Supplier<Opt<U>> fallback) {
        return isPresent() ? requireNonNull(mapper).apply(get()) : requireNonNull(fallback).get();
    }


    /**
     * Returns a sequential {@code Stream} containing the value if present,
     * otherwise an empty {@code Stream}.
     *
     * @return a {@code Stream} of the value, or empty if not present
     * @since 1.1
     */
    Stream<T> stream();

    /**
     * Returns {@code true} if there is a value present, otherwise {@code false}.
     *
     * @return {@code true} if a value is present, otherwise {@code false}
     * @since 1.1
     */
    boolean isPresent();

    /**
     * Returns {@code true} if there is no value present.
     *
     * @return {@code true} if a value is not present, otherwise {@code false}
     * @since 1.1
     */
    default boolean isEmpty() {
        return !isPresent();
    }

    /**
     * Returns the contained value if present.
     *
     * @return the non-null value held by this {@code Opt}
     * @throws NoSuchElementException if there is no value present
     * @since 1.1
     */
    T get();

    /**
     * Returns the value if present, otherwise returns {@code other}.
     *
     * @param other the value to be returned if there is no value present
     * @return the value, if present, otherwise {@code other}
     * @since 1.1
     */
    T orElse(T other);

    /**
     * Returns the value if present, otherwise invokes {@code supplier} and returns the result.
     *
     * @param supplier the supplier whose result is returned if no value is present
     * @return the value if present, otherwise the result from {@code supplier}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    T orElseGet(Supplier<? extends T> supplier);


    /**
     * Returns the result of applying {@code mapper} to the contained value if present,
     * otherwise returns the result produced by {@code defaultSupplier}.
     *
     * @param defaultSupplier the supplier providing the default value
     * @param mapper          the function to apply to the value if present
     * @param <U>             the type of the result
     * @return the mapped value if present, otherwise the default value
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default <U> U mapOrElse(Supplier<? extends U> defaultSupplier, Function<? super T, ? extends U> mapper) {
        return isPresent() ? requireNonNull(mapper).apply(get()) : requireNonNull(defaultSupplier).get();
    }

    /**
     * Returns {@code other} if a value is present, otherwise returns an empty {@code Opt}.
     *
     * @param other the alternative {@code Opt} to return if this is present
     * @param <U>   the type of the value in the alternative {@code Opt}
     * @return {@code other} if a value is present, otherwise an empty {@code Opt}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default <U> Opt<U> and(Opt<U> other) {
        return isPresent() ? requireNonNull(other) : Opt.none();
    }

    /**
     * Returns {@code this} if a value is present and {@code other} is empty,
     * returns {@code other} if this is empty and a value is present in {@code other},
     * otherwise returns an empty {@code Opt}.
     *
     * @param other the other {@code Opt} to compare with
     * @return the exclusive value between {@code this} and {@code other}, or empty if both are present or both are empty
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default Opt<T> xor(Opt<T> other) {
        if (isPresent() && requireNonNull(other).isEmpty()) {
            return this;
        } else if (isEmpty() && other.isPresent()) {
            return other;
        } else {
            return Opt.none();
        }
    }

    /**
     * Returns the contained value if present, otherwise returns {@code null}.
     *
     * @return the value if present, otherwise {@code null}
     * @since 1.1
     */
    default T orNull() {
        return isPresent() ? get() : null;
    }

    /**
     * Returns an {@code Optional} describing the value if present, otherwise an empty {@code Optional}.
     *
     * @return an {@code Optional} with a present value if this {@code Opt} is non-empty, otherwise an empty {@code Optional}
     * @since 1.1
     */
    default Optional<T> toOptional() {
        return isPresent() ? Optional.of(get()) : Optional.empty();
    }

    /**
     * Returns {@code true} if a value is present and the predicate returns {@code true} for it.
     *
     * @param predicate the predicate to apply to the value
     * @return {@code true} if the value is present and matches the predicate, otherwise {@code false}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default boolean exists(Predicate<? super T> predicate) {
        return isPresent() && requireNonNull(predicate).test(get());
    }

    /**
     * Returns the contained value if present, otherwise throws a {@code NoSuchElementException}
     * with the provided message.
     *
     * @param message the exception message to use if no value is present
     * @return the value if present
     * @throws NoSuchElementException if no value is present
     * @throws NullPointerException   if message is null
     * @since 1.1
     */
    default T expect(String message) {
        if (isPresent()) {
            return get();
        }
        throw new NoSuchElementException(requireNonNull(message));
    }

    /**
     * Returns {@code true} if the contained value is equal to {@code value}.
     *
     * @param value the value to compare with the contained value
     * @return {@code true} if the contained value equals {@code value}, otherwise {@code false}
     * @since 1.1
     */
    default boolean contains(T value) {
        return isPresent() && Objects.equals(get(), value);
    }

    /**
     * Returns the contained value if present, otherwise throws an exception produced by {@code exceptionSupplier}.
     *
     * @param exceptionSupplier the supplier of the exception to be thrown
     * @param <X>               the type of the exception to be thrown
     * @return the value if present
     * @throws X if no value is present
     * @since 1.1
     */
    <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X;

    /**
     * Applies the provided {@code mapper} function to the contained value if present,
     * and returns an {@code Opt} containing the result.
     *
     * @param mapper the function to apply to the value if present
     * @param <U>    the type of the result of the mapping
     * @return an {@code Opt} containing the result of applying the mapper, or an empty {@code Opt} if no value is present
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    <U> Opt<U> map(Function<? super T, ? extends U> mapper);

    /**
     * Applies the provided {@code mapper} function to the contained value if present,
     * and returns the result directly.
     *
     * @param mapper the function to apply to the value if present
     * @param <U>    the type of the result
     * @return the result of applying the mapper, or an empty {@code Opt} if no value is present
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    <U> Opt<U> flatMap(Function<? super T, Opt<U>> mapper);

    /**
     * Returns an {@code Opt} describing the value if it matches the given predicate,
     * otherwise returns an empty {@code Opt}.
     *
     * @param predicate the predicate to apply to the value, if present
     * @return {@code this} if the value matches the predicate, otherwise an empty {@code Opt}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    Opt<T> filter(Predicate<? super T> predicate);

    /**
     * If a value is present and matches the predicate, return this {@code Opt}.
     * Otherwise, return the alternative {@code Opt}.
     *
     * @param predicate the predicate to test the value
     * @param fallback  the alternative {@code Opt} if predicate fails
     * @return this {@code Opt} if predicate passes, otherwise {@code fallback}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default Opt<T> filterOrElse(Predicate<? super T> predicate, Supplier<Opt<T>> fallback) {
        return isPresent() && requireNonNull(predicate).test(get()) ? this : requireNonNull(fallback).get();
    }

    /**
     * Returns an {@code Opt} with the value filtered out if it satisfies the given predicate.
     *
     * @param predicate the predicate to test the value against
     * @return this {@code Opt} if the value does not satisfy the predicate,
     * otherwise an empty {@code Opt}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default Opt<T> filterNot(Predicate<? super T> predicate) {
        return filter(requireNonNull(predicate).negate());
    }

    /**
     * Reduces this {@code Opt} to a single value by applying the {@code mapper} function to the contained value
     * if present, or returns {@code defaultValue} if not.
     *
     * @param defaultValue the value to return if no value is present
     * @param mapper       the mapping function to apply to the value, if present
     * @param <U>          the type of the result
     * @return the result of the mapping function if a value is present, otherwise {@code defaultValue}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default <U> U fold(U defaultValue, Function<? super T, ? extends U> mapper) {
        return isPresent() ? requireNonNull(mapper).apply(get()) : requireNonNull(defaultValue);
    }

    /**
     * Returns this {@code Opt} if a value is present, otherwise returns {@code alternative}.
     *
     * @param alternative the alternative {@code Opt} to return if no value is present
     * @return this {@code Opt} if a value is present, otherwise {@code alternative}
     * @since 1.1
     */
    Opt<T> or(Opt<T> alternative);

    /**
     * If a value is present, performs the given action with the value,
     * otherwise does nothing.
     *
     * @param consumer the action to be performed, if a value is present
     * @since 1.1
     */
    void ifPresent(Consumer<? super T> consumer);

    /**
     * If a value is present, performs the given action with the value,
     * otherwise runs the provided alternative.
     *
     * @param consumer    the action to be performed if a value is present
     * @param alternative the runnable to run if no value is present
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    void ifPresentOrElse(Consumer<? super T> consumer, Runnable alternative);

    // Inner classes

    /**
     * Functional pattern matching.
     * Executes {@code some} if a value is present, otherwise executes {@code none}.
     *
     * @param some a function to apply if a value is present
     * @param none a supplier t o invoke if no value is present
     * @param <R>  the type of the result
     * @return the result of applying {@code some} or {@code none}
     * @throws NullPointerException if input is null
     * @since 1.1
     */
    default <R> R match(Function<? super T, ? extends R> some, Supplier<? extends R> none) {
        return switch (this) {
            case Some<T> s -> requireNonNull(some).apply(s.value());
            case None<T> n -> requireNonNull(none).get();
        };
    }

    /**
     * Represents the presence of a value.
     *
     * @param <T> the type of the contained value
     * @since 1.1
     */
    record Some<T>(T value) implements Opt<T> {

        public Some(T value) {
            this.value = requireNonNull(value, THE_VALUE_CAN_T_BE_NULL);
        }

        @Override
        public Iterator<T> iterator() {
            return List.of(value).iterator();
        }

        @Override
        public <R> R accept(OptVisitor<T, R> visitor) {
            return requireNonNull(visitor).visit(this);
        }

        @Override
        public Stream<T> stream() {
            return Stream.of(value);
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public T orElse(T other) {
            return value;
        }

        @Override
        public T orElseGet(Supplier<? extends T> supplier) {
            return value;
        }

        @Override
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            return value;
        }

        @Override
        public <U> Opt<U> map(Function<? super T, ? extends U> mapper) {
            return Opt.ofNullable(requireNonNull(mapper).apply(value));
        }

        @Override
        public <U> Opt<U> flatMap(Function<? super T, Opt<U>> mapper) {
            return requireNonNull(mapper).apply(value);
        }

        @Override
        public Opt<T> filter(Predicate<? super T> predicate) {
            return requireNonNull(predicate).test(value) ? this : Opt.none();
        }

        @Override
        public Opt<T> or(Opt<T> alternative) {
            return this;
        }

        @Override
        public void ifPresent(Consumer<? super T> consumer) {
            requireNonNull(consumer).accept(value);
        }

        @Override
        public void ifPresentOrElse(Consumer<? super T> consumer, Runnable alternative) {
            requireNonNull(consumer).accept(value);
        }

        @Override
        public String toString() {
            return "Some(%s)".formatted(value);
        }
    }

    /**
     * Represents the absence of a value.
     *
     * @param <T> the type of the non-existent value
     * @since 1.1
     */
    record None<T>() implements Opt<T> {
        private static final None<?> INSTANCE = new None<>();

        @Override
        public Iterator<T> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public <R> R accept(OptVisitor<T, R> visitor) {
            return requireNonNull(visitor).visit(this);
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public T get() {
            throw new NoSuchElementException(NO_VALUE_PRESENT);
        }

        @Override
        public T orElse(T other) {
            return other;
        }

        @Override
        public T orElseGet(Supplier<? extends T> supplier) {
            return requireNonNull(supplier).get();
        }

        @Override
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            throw requireNonNull(exceptionSupplier).get();
        }

        @Override
        public <U> Opt<U> map(Function<? super T, ? extends U> mapper) {
            return Opt.none();
        }

        @Override
        public <U> Opt<U> flatMap(Function<? super T, Opt<U>> mapper) {
            return Opt.none();
        }

        @Override
        public Opt<T> filter(Predicate<? super T> predicate) {
            return this;
        }

        @Override
        public Opt<T> or(Opt<T> alternative) {
            return alternative;
        }

        @Override
        public void ifPresent(Consumer<? super T> consumer) {
            // Does nothing, following the behavior of Optional
        }

        @Override
        public void ifPresentOrElse(Consumer<? super T> consumer, Runnable alternative) {
            requireNonNull(alternative).run();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof None;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "None";
        }
    }
}
