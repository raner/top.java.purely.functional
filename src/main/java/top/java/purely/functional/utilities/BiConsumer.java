package top.java.purely.functional.utilities;

import java.util.function.Function;

public interface BiConsumer<_First_, _Second_> extends java.util.function.BiConsumer<_First_, _Second_>, io.reactivex.functions.BiConsumer<_First_, _Second_>
{
  default <_Return_> BiFunction<_First_, _Second_, _Return_> andReturnFirst(Function<_First_, _Return_> x)
  {
    return (a, b) -> {accept(a,b); return x.apply(a);};
  }
//  default <_Return_> BiConsumer<_First_, _Second_> andReturnFirst(Function<_First_, _Return_> x)
//  {
//    return (a, b) -> x.apply(a);
//  }
}
