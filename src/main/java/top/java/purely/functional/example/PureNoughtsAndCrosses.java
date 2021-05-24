package top.java.purely.functional.example;

import java.awt.Point;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import pro.projo.singles.Factory;
import top.java.purely.functional.example.PureNoughtsAndCrosses.Mark;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static pro.projo.Projo.creates;
import static top.java.purely.functional.example.PureNoughtsAndCrosses.Column.COLUMNS;
import static top.java.purely.functional.example.PureNoughtsAndCrosses.Diagonal.DIAGONALS;
import static top.java.purely.functional.example.PureNoughtsAndCrosses.Row.ROWS;

public interface PureNoughtsAndCrosses extends List<List<Optional<Mark>>>
{
    enum GameState {X_WINS, O_WINS, DRAW, UNFINISHED, INVALID}

    enum Mark {X, O}

    interface Series
    {
        Integer index();
    }

    interface Row extends Series
    {
        Factory<Row, Integer> ROWS = creates(Row.class).with(Row::index);

        @Override
        public boolean equals(Object other);

        @Override
        public int hashCode();
    }

    interface Column extends Series
    {
        Factory<Column, Integer> COLUMNS = creates(Column.class).with(Column::index);

        @Override
        public boolean equals(Object other);

        @Override
        public int hashCode();
    }

    interface Diagonal extends Series
    {
        Factory<Diagonal, Integer> DIAGONALS = creates(Diagonal.class).with(Diagonal::index);

        @Override
        public boolean equals(Object other);

        @Override
        public int hashCode();
    }
    
    interface Counter extends Series {}

    Optional<Counter> counter = of(() -> 0);

    default Optional<Row> row(int row, int column)
    {
        return of(ROWS.create(row));
    }

    default Optional<Column> column(int row, int column)
    {
        return of(COLUMNS.create(column));
    }

    default Optional<Diagonal> mainDiagonal(int row, int column)
    {
        return ofNullable(row == column? DIAGONALS.create(0):null);
    }

    default Optional<Diagonal> antiDiagonal(int row, int column)
    {
        return ofNullable(row == size()-1-column? DIAGONALS.create(1):null);
    }

    default Optional<Counter> counter(int row, int column)
    {
        return counter;
    }

    default Stream<Entry<Point, Mark>> enumerate() // skips marks that are absent
    {
        List<Entry<Point, Mark>> entries = new ArrayList<>();
        for (int row = 0; row < size(); row++)
        {
            List<Optional<Mark>> nested = get(row);
            for (int column = 0; column < nested.size(); column++)
            {
                Optional<Mark> mark = nested.get(column);
                if (mark.isPresent())
                {
                    entries.add(new SimpleEntry<>(new Point(column, row), mark.get()));
                }
            }
        }
        return entries.stream();
    }

    default Stream<BiFunction<Integer, Integer, Optional<? extends Series>>> winningSeries()
    {
        return Stream.of
        (
            this::row,
            this::column,
            this::mainDiagonal,
            this::antiDiagonal,
            this::counter
        );
    }

    default Stream<Entry<Mark, Series>> winningSeries(Entry<Point, Mark> entry)
    {
        int row = entry.getKey().y;
        int column = entry.getKey().x;
        Mark mark = entry.getValue();
        return winningSeries()
            .map(series -> series.apply(row, column))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(series -> new SimpleEntry<>(mark, series));
    }

    default Table<Mark, Series, Integer> count(Table<Mark, Series, Integer> counts, Entry<Mark, Series> entry)
    {
        Optional<Integer> count = ofNullable(counts.get(entry.getKey(), entry.getValue()));
        Integer increased = count.orElse(0)+1;
        Table<Mark, Series, Integer> table = HashBasedTable.create();
        table.putAll(counts);
        table.put(entry.getKey(), entry.getValue(), increased);
        return table;
    }

    default GameState evaluate()
    {
        Stream<Entry<Mark, Series>> series = enumerate().flatMap(this::winningSeries);
        Table<Mark, Series, Integer> counts = series.reduce(HashBasedTable.create(), this::count, (a, b) -> a);
        int xTotal = Optional.ofNullable(counts.remove(Mark.X, counter.get())).orElse(0); // TODO
        int oTotal = Optional.ofNullable(counts.remove(Mark.O, counter.get())).orElse(0); // TODO
        Stream<Integer> xCounts = counts.row(Mark.X).values().stream();
        Stream<Integer> oCounts = counts.row(Mark.O).values().stream();
        boolean xWins = xCounts.anyMatch(count -> count >= size());
        boolean oWins = oCounts.anyMatch(count -> count >= size());
        if ((xWins && oWins) || Math.abs(xTotal-oTotal) > 1)
        {
            return GameState.INVALID;
        }
        if (xWins)
        {
            return GameState.X_WINS;
        }
        if (oWins)
        {
            return GameState.O_WINS;
        }
        if (xTotal + oTotal == size()*size())
        {
            return GameState.DRAW;
        }
        return GameState.UNFINISHED;
    }

    static PureNoughtsAndCrosses create3x3
    (
        Optional<Mark> a1, Optional<Mark> a2, Optional<Mark> a3,
        Optional<Mark> b1, Optional<Mark> b2, Optional<Mark> b3,
        Optional<Mark> c1, Optional<Mark> c2, Optional<Mark> c3
    )
    {
        class ArrayTicTacToe extends ArrayList<List<Optional<Mark>>> implements PureNoughtsAndCrosses
        {
            private final static long serialVersionUID = 1065463647453633226L;
        }
        ArrayTicTacToe ttt = new ArrayTicTacToe();
        ttt.add(Arrays.asList(a1, a2, a3));
        ttt.add(Arrays.asList(b1, b2, b3));
        ttt.add(Arrays.asList(c1, c2, c3));
        return ttt;
    }

    public static void main(String[] arguments)
    {
        Optional<Mark> x = Optional.of(Mark.X);
        Optional<Mark> o = Optional.of(Mark.O);
        Optional<Mark> __ = Optional.empty();

        PureNoughtsAndCrosses xWinsByRow = create3x3( x, x, x,
        /**/                                          o, o,__,
        /**/                                         __,__,__);
        System.out.println(xWinsByRow.evaluate());
        
        PureNoughtsAndCrosses oWinsByColumn = create3x3( x, o, x,
        /**/                                            __, o,__,
        /**/                                            __, o,__);
        System.out.println(oWinsByColumn.evaluate());
        
        PureNoughtsAndCrosses xWinsByDiagonal = create3x3( x, o, o,
        /**/                                              __, x,__,
        /**/                                              __, o, x);
        System.out.println(xWinsByDiagonal.evaluate());
        
        PureNoughtsAndCrosses oWinsByDiagonal = create3x3( x, o, o,
        /**/                                              __, o,__,
        /**/                                               o, x, x);
        System.out.println(oWinsByDiagonal.evaluate());
        
        PureNoughtsAndCrosses xWinsByRowAndColumn = create3x3( o, o, x,
        /**/                                                   o, o, x,
        /**/                                                   x, x, x);
        System.out.println(xWinsByRowAndColumn.evaluate());
        
        PureNoughtsAndCrosses invalidDueToDoubleWin = create3x3( o, o, o,
        /**/                                                    __,__,__,
        /**/                                                     x, x, x);
        System.out.println(invalidDueToDoubleWin.evaluate());
        
        PureNoughtsAndCrosses invalidDueToMissedTurns = create3x3( o, x, x,
        /**/                                                       o, x, o,
        /**/                                                       x,__, x);
        System.out.println(invalidDueToMissedTurns.evaluate());
        
        PureNoughtsAndCrosses draw = create3x3( o, x, o,
        /**/                                    o, x, o,
        /**/                                    x, o, x);
        System.out.println(draw.evaluate());
        
        PureNoughtsAndCrosses unfinished = create3x3( o, x, o,
        /**/                                         __, x, o,
        /**/                                         __, o, x);
        System.out.println(unfinished.evaluate());
        
        PureNoughtsAndCrosses emptyBoard = create3x3(__,__,__,
        /**/                                         __,__,__,
        /**/                                         __,__,__);
        System.out.println(emptyBoard.evaluate());
    }
}
