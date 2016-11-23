package com.github.forax.pro.api.helper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public interface OptionAction<C> {
  public Optional<UnaryOperator<CmdLine>> apply(C config);
  
  public static <C> OptionAction<C> exists(String optionName, Predicate<? super C> predicate) {
    return config -> Optional.of(config).filter(predicate).map(__ -> line -> line.add(optionName));
  }
  public static <C> OptionAction<C> action(String optionName, Function<? super C, ?> mapper) {
    return actionMaybe(optionName, mapper.andThen(Optional::of));
  }
  public static <C> OptionAction<C> action(String optionName, Function<? super C, ? extends Collection<?>> mapper, String separator) {
    return actionMaybe(optionName, mapper.andThen(Optional::of), separator);
  }
  public static <C> OptionAction<C> actionMaybe(String optionName, Function<? super C, ? extends Optional<?>> mapper) {
    return config -> mapper.apply(config).map(value -> line -> line.add(optionName).add(value));
  }
  public static <C> OptionAction<C> actionMaybe(String optionName, Function<? super C, ? extends Optional<? extends Collection<?>>> mapper, String separator) {
    return config -> mapper.apply(config).map(value -> line -> line.add(optionName).add(value, separator));
  }
  
  public static <C, O> BiFunction<C, CmdLine, CmdLine> gatherAll(Class<O> optionType, Function<? super O, ? extends OptionAction<C>> mapper) {
    return gatherAll(Arrays.stream(optionType.getEnumConstants()).map(mapper));
  }
  public static <C> BiFunction<C, CmdLine, CmdLine> gatherAll(Stream<OptionAction<? super C>> actions) {
    return (config, cmdLine) -> actions.flatMap(action -> action.apply(config).stream())
                                       .reduce(line -> line, (op1, op2) -> line -> op2.apply(op1.apply(line)))
                                       .apply(cmdLine);
  }
}