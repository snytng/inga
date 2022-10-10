package snytng.astah.plugin.inga;

import java.util.ArrayList;
import java.util.List;

public class LoopElement<T> {
    private T element;
    private List<Loop> loops = new ArrayList<>();

    public LoopElement(T element, List<Loop> loops) {
        this.element = element;
        this.loops = loops;
    }

    public T getElement() {
        return element;
    }

    public int numOfLoops() {
        return loops.size();
    }

    public boolean hasLoop() {
        return numOfLoops() > 0;
    }

    public int numOfPositiveLoops() {
        return (int)loops.stream().filter(Loop::isReinforcingLoop).count();
    }

    public int numOfNegativeLoops() {
        return (int)loops.stream().filter(Loop::isBalancedLoop).count();
    }

    public boolean hasPostiveNegativeLoop() {
        return (numOfPositiveLoops() > 0) && (numOfNegativeLoops() > 0);
    }
}

