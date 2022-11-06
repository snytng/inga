package snytng.astah.plugin.inga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IAssociation;
import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IDependency;
import com.change_vision.jude.api.inf.model.IUseCaseDiagram;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;

/**
 * ユースケース図を因果ループとして解析する
 */
public class UseCaseDiagramReader {

	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(UseCaseDiagramReader.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	private IUseCaseDiagram diagram = null;

	public UseCaseDiagramReader(IUseCaseDiagram d) {
		this.diagram = d;
	}

	static int ingaSupplierIndex = 0;
	public static void setIngaSupplierIndex(int n) {
		ingaSupplierIndex = n % IngaSuppliers.length;
	}

	static String[] IngaSuppliers = new String[] {
		"Default（実線、破線）",
		"SO",
		"＋－",
		"同逆"
	};


	/**
	 * ユースケース図に含まれる正リンクを取得する
	 * @return IngaのSet
	 */
	public Set<Inga> getPositiveIngas(){
		switch(ingaSupplierIndex){
		case 0:
			return getNavigableAssociations();
		case 1:
			return getAssociations("S", true);
		case 2:
			return getAssociations("+", true);
		case 3:
			return getAssociations("同", true);
		default:
			return new HashSet<>();
		}
	}

	/**
	 * ユースケース図に含まれる負リンクを取得する
	 * @return IngaのSet
	 */
	public Set<Inga> getNegativeIngas(){
		switch(ingaSupplierIndex){
		case 0:
			return getDependencies();
		case 1:
			return getAssociations("O", false);
		case 2:
			return getAssociations("-", false);
		case 3:
			return getAssociations("逆", false);
		default:
			return new HashSet<>();
		}
	}

	/**
	 * ユースケース図に含まれる方向を持った関連を取得する
	 * @return IngaのSet
	 */
	Set<Inga> getNavigableAssociations(){
		Set<Inga> ret = new HashSet<>();

		try {
			Arrays.stream(diagram.getPresentations())
			.filter(p -> p.getModel() instanceof IAssociation)
			.map(ILinkPresentation.class::cast)
			.forEach(lp -> {
				IAssociation a = (IAssociation)lp.getModel();
				INodePresentation source = lp.getSource();
				INodePresentation target = lp.getTarget();
				IAttribute[] attrs = a.getMemberEnds();
				IAttribute sourceAttr = attrs[0];
				IAttribute targetAttr = attrs[1];
				if(sourceAttr.getNavigability().equals("Unspecified") &&
						targetAttr.getNavigability().equals("Navigable")
						){
					ret.add(new Inga(lp, source, target, true));
				}
				else if(sourceAttr.getNavigability().equals("Navigable") &&
						targetAttr.getNavigability().equals("Unspecified")
						){
					ret.add(new Inga(lp, target, source, true));
				}
			});
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage());
		}

		return ret;
	}

	/**
	 * ユースケース図に含まれる依存リンクを取得する
	 * @return IngaのSet
	 */
	Set<Inga> getDependencies(){

		Set<Inga> ret = new HashSet<>();

		try {
			Stream.of(diagram.getPresentations())
			.filter(p -> p.getModel() instanceof IDependency)
			.map(ILinkPresentation.class::cast)
			.forEach(lp -> ret.add(new Inga(lp, lp.getTarget(), lp.getSource(), false)));
		} catch (InvalidUsingException e) {
			logger.log(Level.WARNING, e.getMessage());
		}

		return ret;
	}

	/**
	 * ユースケース図に含まれる特定の関連名が入っているリンクを取得する
	 * @return IngaのSet
	 */
	Set<Inga> getAssociations(String relationName, boolean positive){

		Set<Inga> ret = new HashSet<>();

		try {
			Arrays.stream(diagram.getPresentations())
			.filter(p -> p.getModel() instanceof IAssociation)
			.map(ILinkPresentation.class::cast)
			.filter(lp -> lp.getLabel().equals(relationName))
			.forEach(lp -> {
				IAssociation a = (IAssociation)lp.getModel();
				INodePresentation source = lp.getSource();
				INodePresentation target = lp.getTarget();
				IAttribute[] attrs = a.getMemberEnds();
				IAttribute sourceAttr = attrs[0];
				IAttribute targetAttr = attrs[1];
				if(sourceAttr.getNavigability().equals("Unspecified") &&
						targetAttr.getNavigability().equals("Navigable")
						){
					// no action
				}
				else if(sourceAttr.getNavigability().equals("Navigable") &&
						targetAttr.getNavigability().equals("Unspecified")
						){
					INodePresentation temp = target;
					target = source;
					source = temp;
				}
				ret.add(new Inga(lp, source, target, positive));
			});
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage());
		}

		return ret;
	}



	/**
	 * ユースケース図に含まれる線の数を取得する
	 * @return メッセージ数
	 */
	public int getNumberOfMessages(){
		return getPositiveIngas().size() + getNegativeIngas().size();
	}

	private static Set<Inga> ingaSet = new HashSet<>();
	private static Map<INodePresentation, List<Inga>> ingaMap = new HashMap<>();
	private static List<Loop> loops = new ArrayList<>();
	private static List<LoopElement<INodePresentation>> nodes = new ArrayList<>();
	private static List<LoopElement<Inga>>links = new ArrayList<>();

	private static void updateIngas(
			IUseCaseDiagram diagram,
			boolean showLoopOnly,
			boolean showPNOnly
			) {
		UseCaseDiagramReader udr = new UseCaseDiagramReader(diagram);

		Set<Inga> positiveIngas = udr.getPositiveIngas();
		Set<Inga> negativeIngas = udr.getNegativeIngas();

		ingaSet = new HashSet<>();
		ingaSet.addAll(positiveIngas);
		ingaSet.addAll(negativeIngas);

		loops = new ArrayList<>();
		ingaSet.stream().forEach(inga -> {
			INodePresentation node = inga.p.getSource();
			logger.log(Level.FINE, () -> "##### p " + node.getLabel());
			getLoops(node, ingaSet, new Loop(), loops);
		});

		ingaMap = ingaSet.stream()
		.collect(Collectors.groupingBy(inga -> inga.source));

		nodes = ingaSet.stream()
				.map(inga -> inga.source)
				.distinct()
				.map(node -> new LoopElement<>(
						node,
						loops.stream()
						.filter(ingas -> ingas.stream()
								.map(inga -> inga.source)
								.anyMatch(s -> s.equals(node)))
						.collect(Collectors.toList()))
						)
				.filter(node ->  ! showLoopOnly || node.hasLoop())
				.filter(node ->  ! showPNOnly   || node.hasPostiveNegativeLoop())
				.sorted(Comparator.comparing(LoopElement<INodePresentation>::numOfLoops).reversed())
				.collect(Collectors.toList());

		links = ingaSet.stream()
				.map(inga -> new LoopElement<>(
							inga,
							loops.stream()
							.filter(loop -> loop.contains(inga))
							.collect(Collectors.toList()))
					)
				.filter(link -> ! showLoopOnly || link.hasLoop())
				.filter(link -> ! showPNOnly   || link.hasPostiveNegativeLoop())
				.sorted(Comparator.comparing(LoopElement<Inga>::numOfLoops).reversed())
				.collect(Collectors.toList());

	}

	private static boolean showPNOnly = false;

	public List<MessagePresentation> getMessagePresentation(
			List<IPresentation> selectedPresentations,
			boolean showLoopOnly,
			boolean showPNOnly) {

		UseCaseDiagramReader.showPNOnly = showPNOnly;

		updateIngas(diagram, showLoopOnly, showPNOnly);

		List<MessagePresentation> mps = new ArrayList<>();

		recordBar(mps, "ループ");
		// 選択されている要素を先頭にループ表示
		IPresentation startPresentation = selectedPresentations.isEmpty() ? null : selectedPresentations.get(0);
		recordLoop(mps, startPresentation);

		recordBar(mps, "リンク");
		recordLink(mps);
		recordBar(mps, "ノード");
		recordNode(mps);

		// 選択されている要素があれば含まれているものだけを表示する
		if (! selectedPresentations.isEmpty()) {
			List<MessagePresentation> selectedMessagePresentation = new ArrayList<>();

			for(int i = 0; i < mps.size(); i++) {
				MessagePresentation mp = mps.get(i);

				if(
						// nullだったら表示
						mp.presentations == null

						// 選択しているIPresentationと同じだったら表示
						||
						selectedPresentations.stream()
						.anyMatch(p -> Stream.of(mp.presentations)
								.anyMatch(x -> x == p))

						// ノードを選択していたら、つながっているILinkPresentationを表示
						||
						selectedPresentations.stream()
						.filter(INodePresentation.class::isInstance)
						.map(INodePresentation.class::cast)
						.anyMatch(np -> Stream.of(mp.presentations)
								.filter(ILinkPresentation.class::isInstance)
								.map(ILinkPresentation.class::cast)
								.anyMatch(l -> l.getTarget().equals(np) || l.getSource().equals(np)))

						// リンクを選択していたら、つながっているINodePresentationを表示
						||
						selectedPresentations.stream()
						.filter(ILinkPresentation.class::isInstance)
						.map(ILinkPresentation.class::cast)
						.anyMatch(lp -> Stream.of(mp.presentations)
								.filter(INodePresentation.class::isInstance)
								.map(INodePresentation.class::cast)
								.anyMatch(np -> np.equals(lp.getTarget()) || np.equals(lp.getSource())))

						) {

					selectedMessagePresentation.add(mp);
				}
			}
			mps = selectedMessagePresentation;

			// シミュレーション結果は常に表示する
			recordBar(mps, "シミュレーション");
			recordSimulation(mps, startPresentation);

		}

		return mps;
	}

	// 仕切りを追加
	private static void recordBar(List<MessagePresentation> mps, String label) {
		mps.add(new MessagePresentation("===== " + label, null));
	}

	// 増減シミュレーションを表示
	static class SimulationResult {
		int plus;
		int minus;
		public SimulationResult(int plus, int minus) {
			this.plus = plus;
			this.minus = minus;
		}
	}

	static boolean simPositive = true;

	private static void recordSimulation(List<MessagePresentation> mps, IPresentation startPresentation) {
		if(! (startPresentation instanceof INodePresentation)) {
			return;
		}

		INodePresentation np = (INodePresentation)startPresentation;
		Map<INodePresentation, SimulationResult> simulationResults = new LinkedHashMap<>();

		// ループを探索
		simPositive = true;
		simulate(np, simPositive, new HashSet<>(ingaSet), simulationResults);

		// ノード増減を表示
		simulationResults.keySet().stream()
		.forEach(key -> {
			SimulationResult rs = simulationResults.get(key);
			int pm = rs.plus + rs.minus;
			if(pm > 0
					&&
					! (showPNOnly && (rs.plus == 0 || rs.minus == 0))
					) {
				mps.add(new MessagePresentation(
						key.getLabel() + " " + pm + " (増加=" + rs.plus + ", 減少=" + rs.minus + ")",
						new IPresentation[] {key}));
			}
		});
	}

	private static void simulate(
			INodePresentation from,
			boolean simPositive,
			Set<Inga> checkedIngaSet,
			Map<INodePresentation, SimulationResult> simulationResults) {
		SimulationResult sr = simulationResults.getOrDefault(from,  new SimulationResult(0, 0));
		if(simPositive) {
			sr.plus  += 1;
		} else {
			sr.minus += 1;
		}
		simulationResults.put(from, sr);

		ingaMap.get(from).stream()
		.forEach(inga -> {
			if(checkedIngaSet.contains(inga)) {
				checkedIngaSet.remove(inga);
				simulate(inga.target, ! (simPositive ^ inga.isPositive()), checkedIngaSet, simulationResults);
			}
		});
	}

	// ノードを表示
	private static void recordNode(List<MessagePresentation> mps) {
		nodes.stream().forEach(node ->
		mps.add(new MessagePresentation(
				String.format(
						"%s: %d (自己強化=%d, バランス=%d)",
						node.getElement().getLabel(),
						node.numOfLoops(),
						node.numOfPositiveLoops(),
						node.numOfNegativeLoops()
						),
				new IPresentation[]{node.getElement()}))
				);
	}

	// リンクを表示
	private static void recordLink(List<MessagePresentation> mps) {
		links.stream().forEach(link ->
		mps.add(new MessagePresentation(
				String.format(
						"%s: %d (自己強化=%d, バランス=%d)",
						link.getElement().toString(),
						link.numOfLoops(),
						link.numOfPositiveLoops(),
						link.numOfNegativeLoops()
						),
				new IPresentation[]{link.getElement().p}))
				);
	}

	// ループを表示
	private static void recordLoop(List<MessagePresentation> mps, IPresentation startPresentation) {
		// 自己強化、バランスの順番かつリンク数の小さい順にする
		Stream.concat(
				loops.stream().filter(Loop::isReinforcingLoop).sorted(Comparator.comparing(Loop::size)),
				loops.stream().filter(Loop::isBalancedLoop).sorted(Comparator.comparing(Loop::size))
				)
		.forEach(loop ->
		mps.add(new MessagePresentation(
				loop.getDescription(startPresentation),
				loop.getAllPresentations())
				)
				);
	}

	private static void getLoops(
			INodePresentation source,
			Set<Inga> ingas,
			Loop loop,
			List<Loop> loops
			){

		ingas.stream()
		.filter(inga -> inga.source == source)
		.forEach(inga -> {
			logger.log(Level.FINE, () -> "##### inga " + inga.from + "->" + inga.to);

			// 最初のノードに戻ってループした
			if(! loop.isEmpty() && loop.get(0).from == inga.to){
				Loop nl = new Loop(loop, inga);

				if(loops.stream()
						.anyMatch(l -> new HashSet<Inga>(l).equals(new HashSet<Inga>(nl)))){
					logger.log(Level.FINE, () -> "existed loop " + nl);
				} else {
					logger.log(Level.FINE, () -> "new loop " + nl);
					loops.add(nl);
				}

			}
			// ループしたが途中にぶつかった
			else if(loop.stream().anyMatch(l -> l.from == inga.to)) {
				logger.log(Level.FINE, "loop but not back to start point");

			}
			// ループしていないので、次のリンクへ進む
			else {
				logger.log(Level.FINE, "not loop go next");
				Loop nl = new Loop(loop, inga);
				getLoops(inga.target, ingas, nl, loops);
			}

		});
	}

}
