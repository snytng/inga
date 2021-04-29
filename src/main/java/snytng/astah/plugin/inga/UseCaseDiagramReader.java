package snytng.astah.plugin.inga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
		INamedElement from;
		INamedElement to;
		boolean positive;

		public Link(ILinkPresentation p, INamedElement from, INamedElement to, boolean positive){
			this.p = p;
			this.from = from;
			this.to = to;
			this.positive = positive;
		}

		public String toString(){
			if(positive){
				return "➚「" + from.getName() + "」が増えれば、「" + to.getName() + "」が増える";
			} else {
				return "➘「" + from.getName() + "」が増えれば、「" + to.getName() + "」が減る";
			}
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
	 * CyclicPath
	 */
	static class CyclicPath extends ArrayList<Link> implements List<Link> {

		public CyclicPath() {
			super();
		}

		public CyclicPath(CyclicPath cp) {
			super();
			cp.stream().forEach(this::add);
		}

		public CyclicPath(CyclicPath cp, Link link) {
			super();
			cp.stream().forEach(this::add);
			this.add(link);
		}

		public String getDescription(){
			StringBuilder builder = new StringBuilder();

			builder.append(isPositive() ? "自己強化: " : "バランス: ");

			for(Link link : this){
				builder.append(link.from);

				if(! this.isEmpty() && this.get(0).from != link.to){
					if(link.positive){
						builder.append(" -(+)-> ");
					} else {
						builder.append(" -(-)-> ");
					}
				}
			}
			return builder.toString();
		}

		public boolean isPositive() {
			return this.stream()
					.reduce(true,
							(ret,  link) -> ret && link.positive,
							(ret1, ret2) -> ret1 && ret2);
		}

		public String toString() {
			return this.stream().map(cp -> cp.from.getName()).collect(Collectors.joining("->"));
		}

	}


	/**
	 * ユースケース図に含まれるユースケースを取得する
	 * @return ユースケース配列
	 */
	public Set<IPresentation> getUseCases(){

		Set<IPresentation> us = new HashSet<>();

		try {
			for(IPresentation p : diagram.getPresentations()){
				if(p.getModel() instanceof IUseCase){
					us.add(p);
				}
			}
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

		Set<Link> links = new HashSet<>();

		try {
			Arrays.stream(diagram.getPresentations())
			.filter(p -> p.getModel() instanceof IAssociation)
			.forEach(p -> {
				IAssociation a = (IAssociation)p.getModel();
				IAttribute[] attrs = a.getMemberEnds();
				INamedElement from = null;
				INamedElement to = null;
				if(attrs[0].getNavigability().equals("Navigable") && attrs[1].getNavigability().equals("Unspecified")){
					from = attrs[1].getType();
					to   = attrs[0].getType();
				}
				else if(attrs[1].getNavigability().equals("Navigable") && attrs[0].getNavigability().equals("Unspecified")){
					from = attrs[0].getType();
					to   = attrs[1].getType();
				}
				if(from != null && to != null){
					links.add(new Link((ILinkPresentation) p, from, to, true));
				}
			});
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage());
		}

		return links;
	}

	/**
	 * ユースケース図に含まれる負リンクを取得する
	 * @return ユースケース配列
	 */
	public Set<Link> getNegativeLinks(){

		Set<Link> links = new HashSet<>();

		try {
			Arrays.stream(diagram.getPresentations())
			.filter(p -> p.getModel() instanceof IDependency)
			.forEach(p -> {
				IDependency d = (IDependency)p.getModel();
				links.add(new Link((ILinkPresentation) p, d.getClient(), d.getSupplier(), false));
			});
		} catch (InvalidUsingException e) {
			logger.log(Level.WARNING, e.getMessage());
		}

		return links;
	}

	/**
	 * ユースケース図に含まれる線の数を取得する
	 * @return メッセージ数
	 */
	public int getNumberOfMessages(){
		return getPositiveLinks().size() + getNegativeLinks().size();
	}

	public static MessagePresentation getMessagePresentation(IUseCaseDiagram diagram) {
		MessagePresentation mps = new MessagePresentation();

		UseCaseDiagramReader udr = new UseCaseDiagramReader(diagram);
		Set<Link> positiveLinks = udr.getPositiveLinks();
		Set<Link> negativeLinks = udr.getNegativeLinks();

		Set<Link> links = new HashSet<>();
		links.addAll(positiveLinks);
		links.addAll(negativeLinks);

		recordCyclicPath(mps, links);
		mps.add("=====", null);
		recordLink(mps, links);

		return mps;
	}

	public static MessagePresentation getCyclicPathMessagePresentation(IUseCaseDiagram diagram) {
		MessagePresentation mps = new MessagePresentation();

		UseCaseDiagramReader udr = new UseCaseDiagramReader(diagram);
		Set<Link> positiveLinks = udr.getPositiveLinks();
		Set<Link> negativeLinks = udr.getNegativeLinks();

		Set<Link> links = new HashSet<>();
		links.addAll(positiveLinks);
		links.addAll(negativeLinks);

		recordCyclicPath(mps, links);

		return mps;
	}

	private static void recordLink(MessagePresentation mps, Set<Link> links){
		// リンクを表示
		for(Link link : links){
			mps.add(link.toString(), new IPresentation[]{link.p});
		}
	}

	private static void recordCyclicPath(MessagePresentation mps, Set<Link> links) {
		List<CyclicPath> cps = new ArrayList<>();
		for(Link link : links) {
			INodePresentation node = link.p.getSource();
			if(node.getType().equals("UseCase")) {
				logger.log(Level.FINE, () -> "##### p " + node.getLabel());
				IUseCase uc = (IUseCase)node.getModel();
				getCyclicPaths(uc, links, new CyclicPath(), cps);
			}
		}

		for(CyclicPath cp : cps){
			mps.add(cp.getDescription(), cp.stream().map(link -> (IPresentation)link.p).toArray(IPresentation[]::new));
		}
	}

	private static void getCyclicPaths(
			IUseCase currentUC,
			Set<Link> links,
			CyclicPath cyclicPath,
			List<CyclicPath> cyclicPaths
			){

		links.stream()
		.filter(link -> link.from == currentUC)
		.forEach(link -> {
			logger.log(Level.FINE, () -> "##### link " + link.from + "->" + link.to);

			// 最初のノードに戻ってループした
			if(! cyclicPath.isEmpty() && cyclicPath.get(0).from == link.to){
				CyclicPath newcp = new CyclicPath(cyclicPath, link);

				if(cyclicPaths.stream()
						.anyMatch(cp -> new HashSet<Link>(cp).equals(new HashSet<Link>(newcp)))){
					logger.log(Level.FINE, () -> "existed cycle " + newcp);
				} else {
					logger.log(Level.FINE, () -> "new cycle " + newcp);
					cyclicPaths.add(newcp);
				}

			}
			// ループしたが途中にぶつかった
			else if(cyclicPath.stream().anyMatch(cp -> cp.from == link.to)) {
				logger.log(Level.FINE, "cycle but not back to start point");

			}
			// ループしていないので、次のリンクへ進む
			else {
				logger.log(Level.FINE, "not cycle go next");
				CyclicPath cp = new CyclicPath(cyclicPath, link);
				getCyclicPaths((IUseCase)link.to, links, cp, cyclicPaths);
			}

		});
	}

}
