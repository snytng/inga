package snytng.astah.plugin.inga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IUseCase;
import com.change_vision.jude.api.inf.model.IUseCaseDiagram;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;

/**
 * ユースケース図をインがループとして解析するる
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

	public UseCaseDiagramReader(IUseCaseDiagram diagram) {
		this.diagram = diagram;
	}

	/**
	 * Link
	 */
	static class Link {
		ILinkPresentation p;
		INodePresentation source;
		INodePresentation target;
		INamedElement from;
		INamedElement to;
		boolean positive;

		public Link(ILinkPresentation p, INodePresentation source, INodePresentation target, boolean positive){
			this.p        = p;
			this.source   = source;
			this.target   = target;
			this.from     = (INamedElement)this.source.getModel();
			this.to       = (INamedElement)this.target.getModel();
			this.positive = positive;
		}

		public String toString(){
			if(positive){
				return "➚「" + from.getName() + "」が増えれば、「" + to.getName() + "」が増える";
			} else {
				return "➘「" + from.getName() + "」が増えれば、「" + to.getName() + "」が減る";
			}
		}

		public String getLinkString() {
			return this.positive ? "-(+)->" : "-(-)->";
		}

		public int hashCode(){
			return p.hashCode();
		}

		public boolean equals(Object obj){
			if (obj == null)
				return false;

			if (this.getClass() != obj.getClass())
				return false;

			Link a = (Link)obj;
			return p.equals(a.p);
		}
	}

	/**
	 * Loop the cyclic path of links
	 */
	static class Loop extends ArrayList<Link> implements List<Link> {

		public Loop() {
			super();
		}

		public Loop(Loop cp) {
			super();
			cp.stream().forEach(this::add);
		}

		public Loop(Loop cp, Link link) {
			super();
			cp.stream().forEach(this::add);
			this.add(link);
		}

		private String getType() {
			return isPositive() ? "自己強化" : "バランス";
		}

		public String getDescription(){
			return getType() + ": " +
					this.stream().map(link -> link.from + " " + link.getLinkString() + " ")
					.collect(Collectors.joining());
		}

		public boolean isPositive() {
			return this.stream().filter(l -> ! l.positive).count() % 2 == 0;
		}

		public boolean isNegative() {
			return ! isPositive();
		}

		public String toString() {
			return this.stream().map(cp -> cp.from.getName()).collect(Collectors.joining("->"));
		}

	}

	static class Node {
		INodePresentation p;
		List<Loop> loops = new ArrayList<>();

		public Node(INodePresentation p) {
			this.p = p;
		}

		public int numOfLoops() {
			return loops.size();
		}

		public int numOfPositiveLoops() {
			return (int)loops.stream().filter(Loop::isPositive).count();
		}

		public int numOfNegativeLoops() {
			return (int)loops.stream().filter(Loop::isNegative).count();
		}

		public boolean hasPostiveNegativeLoop() {
			return (numOfLoops() > 0) && (numOfNegativeLoops() > 0);
		}
	}


	/**
	 * ユースケース図に含まれるユースケースを取得する
	 * @return ユースケース配列
	 */
	public Set<IPresentation> getUseCases(){

		Set<IPresentation> us = new HashSet<>();

		try {
			Stream.of(diagram.getPresentations())
			.filter(p -> p.getModel() instanceof IUseCase)
			.forEach(us::add);
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage());
		}

		return us;
	}


	/**
	 * ユースケース図に含まれる正リンクを取得する
	 * @return ユースケース配列
	 */
	public Set<Link> getPositiveLinks(){

		Set<Link> ret = new HashSet<>();

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
					ret.add(new Link(lp, source, target, true));
				}
				else if(sourceAttr.getNavigability().equals("Navigable") &&
						targetAttr.getNavigability().equals("Unspecified")
						){
					ret.add(new Link(lp, target, source, true));
				}
			});
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage());
		}

		return ret;
	}

	/**
	 * ユースケース図に含まれる負リンクを取得する
	 * @return ユースケース配列
	 */
	public Set<Link> getNegativeLinks(){

		Set<Link> ret = new HashSet<>();

		try {
			Stream.of(diagram.getPresentations())
			.filter(p -> p.getModel() instanceof IDependency)
			.map(ILinkPresentation.class::cast)
			.forEach(lp -> ret.add(new Link(lp, lp.getTarget(), lp.getSource(), false)));
		} catch (InvalidUsingException e) {
			logger.log(Level.WARNING, e.getMessage());
		}

		return ret;
	}

	/**
	 * ユースケース図に含まれる線の数を取得する
	 * @return メッセージ数
	 */
	public int getNumberOfMessages(){
		return getPositiveLinks().size() + getNegativeLinks().size();
	}

	private static Set<Link> links = new HashSet<>();
	private static List<Node> nodes = new ArrayList<>();
	private static List<Loop> loops = new ArrayList<>();

	private static void updateLinks(IUseCaseDiagram diagram) {
		UseCaseDiagramReader udr = new UseCaseDiagramReader(diagram);

		Set<Link> positiveLinks = udr.getPositiveLinks();
		Set<Link> negativeLinks = udr.getNegativeLinks();

		links = new HashSet<>();
		links.addAll(positiveLinks);
		links.addAll(negativeLinks);

		loops = new ArrayList<>();
		links.stream().forEach(link -> {
			INodePresentation node = link.p.getSource();
			if(node.getType().equals("UseCase")) {
				logger.log(Level.FINE, () -> "##### p " + node.getLabel());
				IUseCase uc = (IUseCase)node.getModel();
				getLoops(uc, links, new Loop(), loops);
			}
		});

		nodes = links.stream()
				.map(l -> l.source)
				.distinct()
				.map(n -> {
					Node node = new Node(n);
					node.loops = loops.stream()
							.filter(link -> link.stream()
									.map(l -> l.source)
									.anyMatch(s -> s.equals(n)))
							.collect(Collectors.toList());
					return node;
					})
				.filter(node -> node.numOfLoops() > 0)
				.sorted(Comparator.comparing(Node::numOfLoops).reversed())
				.collect(Collectors.toList());

	}

	public static List<MessagePresentation> getMessagePresentation(IUseCaseDiagram diagram) {
		updateLinks(diagram);

		List<MessagePresentation> mps = new ArrayList<>();
		recordLoop(mps);
		recordBar(mps);
		recordLink(mps);
		recordBar(mps);
		recordNode(mps);

		return mps;
	}

	// 仕切りを追加
	private static void recordBar(List<MessagePresentation> mps) {
		mps.add(new MessagePresentation("=====", null));
	}

	// リンクを表示
	private static void recordLink(List<MessagePresentation> mps) {
		links.stream()
		.forEach(link -> mps.add(new MessagePresentation(link.toString(), new IPresentation[]{link.p})));
	}

	// ノードを表示
	private static void recordNode(List<MessagePresentation> mps) {
		nodes.stream().forEach(node ->
		mps.add(new MessagePresentation(
				String.format(
						"%s: %d (自己強化=%d, バランス=%d)",
						node.p.getLabel(),
						node.numOfLoops(),
						node.numOfPositiveLoops(),
						node.numOfNegativeLoops()
						),
				new IPresentation[]{node.p}))
				);
	}

	// ループを表示
	private static void recordLoop(List<MessagePresentation> mps) {
		// 自己強化、バランスの順番かつリンク数の小さい順にする
		Stream.concat(
				loops.stream().filter(Loop::isPositive).sorted(Comparator.comparing(Loop::size)),
				loops.stream().filter(Loop::isNegative).sorted(Comparator.comparing(Loop::size))
				)
		.forEach(loop ->
		mps.add(new MessagePresentation(
				loop.getDescription(),
				loop.stream()
				.map(link -> (IPresentation)link.p)
				.toArray(IPresentation[]::new))
				)
				);
	}

	private static void getLoops(
			IUseCase currentUC,
			Set<Link> links,
			Loop loop,
			List<Loop> loops
			){

		links.stream()
		.filter(link -> link.from == currentUC)
		.forEach(link -> {
			logger.log(Level.FINE, () -> "##### link " + link.from + "->" + link.to);

			// 最初のノードに戻ってループした
			if(! loop.isEmpty() && loop.get(0).from == link.to){
				Loop nl = new Loop(loop, link);

				if(loops.stream()
						.anyMatch(l -> new HashSet<Link>(l).equals(new HashSet<Link>(nl)))){
					logger.log(Level.FINE, () -> "existed loop " + nl);
				} else {
					logger.log(Level.FINE, () -> "new loop " + nl);
					loops.add(nl);
				}

			}
			// ループしたが途中にぶつかった
			else if(loop.stream().anyMatch(l -> l.from == link.to)) {
				logger.log(Level.FINE, "loop but not back to start point");

			}
			// ループしていないので、次のリンクへ進む
			else {
				logger.log(Level.FINE, "not loop go next");
				Loop nl = new Loop(loop, link);
				getLoops((IUseCase)link.to, links, nl, loops);
			}

		});
	}

}
