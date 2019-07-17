package top.java.purely.functional.utilities;

import java.util.function.BiConsumer;

public interface Utilities
{
  default <_First_, _Second_> top.java.purely.functional.utilities.BiConsumer<_First_, _Second_> reverse(java.util.function.BiConsumer<_Second_, _First_> consumer)
  {
    return (first, second) -> consumer.accept(second, first);
  }

  default <_First_, _Second_> java.util.function.BiFunction<_First_, _Second_, _First_> first(BiConsumer<_First_, _Second_> consumer)
  {
    return (first, second) ->
    {
      consumer.accept(first, second);
      return first;
    };
  }
  
  default <_First_, _Second_> java.util.function.BiFunction<_First_, _Second_, _First_> flippedFirst(BiConsumer<_Second_, _First_> consumer)
  {
    return (first, second) ->
    {
      consumer.accept(second, first);
      return first;
    };
  }
  
  default <_First_, _Second_> java.util.function.BiFunction<_First_, _Second_, _Second_> second(BiConsumer<_First_, _Second_> consumer)
  {
    return (first, second) ->
    {
      consumer.accept(first, second);
      return second;
    };
  }
}
