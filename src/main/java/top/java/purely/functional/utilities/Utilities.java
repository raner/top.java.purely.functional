package top.java.purely.functional.utilities;

import java.util.function.BiConsumer;

public interface Utilities
{
  default <_First_, _Second_> top.java.purely.functional.utilities.BiConsumer<_First_, _Second_> reverse(java.util.function.BiConsumer<_Second_, _First_> consumer)
  {
    return (first, second) -> consumer.accept(second, first);
  }

}
