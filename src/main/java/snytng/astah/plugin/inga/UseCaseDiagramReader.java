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

import com.change_vision.jude.api.inf.editor.UseCaseDiagramEditor;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IAssociation;
import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IDependency;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IUseCase;
import com.change_vision.jude.api.inf.model.IUseCaseDiagram;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;

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
	@SuppressWarnings("serial")
	static class CyclicPath extends ArrayList<Link> implements List<Link> {


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
			boolean positive = true;
			for(Link link : this){
				positive = positive == link.positive;
			}
			return positive;
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

		Set<Link> us = new HashSet<>();

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
					us.add(new Link((ILinkPresentation) p, from, to, true));
				}
			});
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage());
		}

		return us;
	}

	/**
	 * ユースケース図に含まれる負リンクを取得する
	 * @return ユースケース配列
	 */
	public Set<Link> getNegativeLinks(){

		Set<Link> path = new HashSet<>();

		try {
			Arrays.stream(diagram.getPresentations())
			.filter(p -> p.getModel() instanceof IDependency)
			.forEach(p -> {
				IDependency d = (IDependency)p.getModel();
				path.add(new Link((ILinkPresentation) p, d.getClient(), d.getSupplier(), false));
			});
		} catch (InvalidUsingException e) {
			logger.log(Level.WARNING, e.getMessage());
		}

		return path;
	}

	/**
	 * ユースケース図に含まれる線の数を取得する
	 * @return メッセージ数
	 */
	public int getNumberOfMessages(){
		return getPositiveLinks().size() + getNegativeLinks().size();
	}

	private static List<IPresentation> linePresentations = new ArrayList<>();

	public static MessagePresentation getMessagePresentation(IUseCaseDiagram diagram, IDiagramViewManager dvm, UseCaseDiagramEditor usecaseModelEditor) {
		MessagePresentation mps = new MessagePresentation();

		UseCaseDiagramReader udr = new UseCaseDiagramReader(diagram);

		Set<IPresentation> pusecases = udr.getUseCases();
		Set<Link> positiveLinks = udr.getPositiveLinks();	
		Set<Link> negativeLinks = udr.getNegativeLinks();		

		// ユースケース図の情報表示
		/*
		mps.add("[" + diagram.getName() + "]ユースケース図には、「" + pusecases.size() + "個」のユースケースがあります", null);
		mps.add("[" + diagram.getName() + "]ユースケース図には、「" + positiveLinks.size() + "個」の正リンクがあります", null);
		mps.add("[" + diagram.getName() + "]ユースケース図には、「" + negativeLinks.size() + "個」の負リンクがあります", null);
		mps.add("=====", null);
		 */
		
		Set<Link> links = new HashSet<>();
		links.addAll(positiveLinks);
		links.addAll(negativeLinks);

		recordLink(mps, pusecases, links);
		recordCyclicPath(mps, pusecases, links);

		return mps;
	}
	
	private static void recordLink(MessagePresentation mps, Set<IPresentation> pusecases, Set<Link> links){
		// リンクを表示
		for(Link link : links){
			mps.add(link.toString(), new IPresentation[]{link.p});	
		}
	}

	private static void recordCyclicPath(MessagePresentation mps, Set<IPresentation> pusecases, Set<Link> links) {
		List<CyclicPath> cyclicPaths = new ArrayList<>();
		for(IPresentation p : pusecases){
			logger.log(Level.FINE, "##### p " + p.getLabel());

			IUseCase uc = (IUseCase)p.getModel();
			CyclicPath cyclicPath = new CyclicPath();
			getCyclicPaths(uc, links, cyclicPath, cyclicPaths);
		}

		for(CyclicPath path : cyclicPaths){
			mps.add(path.getDescription(), path.stream().map(link -> (IPresentation)link.p).toArray(IPresentation[]::new));
		}			
	}

	private static void getCyclicPaths(
			IUseCase currentUC, 
			Set<Link> links, 
			CyclicPath cyclicPath, 
			List<CyclicPath> cyclicPaths
			){

		if(cyclicPaths.stream().anyMatch(cp -> cp.contains(currentUC))){
			return;
		}

		links.stream()
		.filter(a -> a.from == currentUC)
		.forEach(a -> {
			logger.log(Level.FINE, "##### link " + a.from + "->" + a.to);

			if(! cyclicPath.isEmpty() && cyclicPath.get(0).from == a.to){
				CyclicPath as = new CyclicPath();
				boolean start = false;
				for(Link p : cyclicPath){
					if(start || p.from == a.to){
						as.add(p);
						start = true;
					}
				}
				as.add(a);
				if(cyclicPaths.stream().anyMatch(path -> new HashSet<Link>(path).equals(new HashSet<Link>(as)))){
					logger.log(Level.FINE, "duplicated cycle " + as.stream().map(cp -> cp.from.getName()).collect(Collectors.joining("->")));					
				} else {
					logger.log(Level.FINE, "cycle " + as.stream().map(cp -> cp.from.getName()).collect(Collectors.joining("->")));
					cyclicPaths.add(as);
				}

			} else if(cyclicPath.stream().anyMatch(cp -> cp.from == a.to)) {
				logger.log(Level.FINE, "cycle but not back to start point");

			} else {
				logger.log(Level.FINE, "not cycle go next");
				CyclicPath path = new CyclicPath();
				for(Link p : cyclicPath){
					path.add(p);
				}
				path.add(a);
				getCyclicPaths((IUseCase)a.to, links, path, cyclicPaths);
			}

		});
	}

}
