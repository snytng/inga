package snytng.astah.plugin.inga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.change_vision.jude.api.inf.presentation.IPresentation;

/**
 * Loop - the cyclic path of Ingas
 */
public class Loop extends ArrayList<Inga> {
	public static final String REINFORCING_NAME = "自己強化";
	public static final String BALANCING_NAME = "バランス";

	public Loop() {
		super();
	}

	public Loop(Loop cp) {
		super();
		cp.stream().forEach(this::add);
	}

	public Loop(Loop cp, Inga inga) {
		super();
		cp.stream().forEach(this::add);
		this.add(inga);
	}

	private String getType() {
		return isReinforcingLoop() ? REINFORCING_NAME : BALANCING_NAME;
	}

	private List<Inga> getIngaLoop(IPresentation startPresentation) {
		int index = 0;
		for(int i = 0; i < this.size(); i++) {
			if(get(i).source == startPresentation || get(i).p == startPresentation) {
				index = i;
				break;
			}
		}
		return Stream.concat(this.stream().skip(index), this.stream().limit(index))
				.collect(Collectors.toList());
	}

	public String getDescription(IPresentation startPresentation){
		List<Inga> ingaLoop = getIngaLoop(startPresentation);
		return getType() + ": " +
				ingaLoop.stream()
		.map(inga -> inga.from + " " + inga.getArrowString() + " ")
		.collect(Collectors.joining())
		+ ingaLoop.get(0).from;
	}

	public boolean isReinforcingLoop() {
		// ネガティブループが偶数個含まれていれば自己強化ループ
		return this.stream().filter(Inga::isNegative).count() % 2 == 0;
	}

	public boolean isBalancedLoop() {
		return ! isReinforcingLoop();
	}

	public IPresentation[] getAllPresentations() {
		return this.stream()
		.map(inga -> new IPresentation[] {(IPresentation)inga.p, (IPresentation)inga.source, (IPresentation)inga.target})
		.flatMap(l -> Arrays.asList(l).stream())
		.toArray(IPresentation[]::new);
	}

	@Override
	public String toString() {
		return this.stream().map(cp -> cp.from.getName()).collect(Collectors.joining("->"));
	}

}