package snytng.astah.plugin.inga;

import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IAssociation;
import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IGeneralization;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IRealization;
import com.change_vision.jude.api.inf.presentation.IPresentation;

public class RelationReader {
	
	private RelationReader(){}
	
	/**
	 * 読み上げをサポートされている関連かどうかを返却する
	 * @param e モデル要素
	 * @return サポートされている場合はtrue、サポートされていない場合にはfalse
	 */
	public static boolean isSupportedRelation(IElement e){
		if(e == null) return false;
		
		return 
				(e instanceof IAssociation) ||
				(e instanceof IRealization) ||
				(e instanceof IGeneralization);
	}
	
	/**
	 * 関連を読み上げる
	 * @param e モデル要素
	 * @return 関連を読み上げた文字列、読み上げられない場合にはnull
	 */
	public static String printRelation(IElement e){
		// 関連
		if(e instanceof IAssociation){
			IAssociation ia = (IAssociation)e;
			return printAssotication(ia);
		}
		// 継承
		else if(e instanceof IGeneralization){
			IGeneralization ig = (IGeneralization)e;
			return printGeneralization(ig); 
		} 
		// 実現
		else if(e instanceof IRealization){
			IRealization ir = (IRealization)e;
			return printRealization(ir);
		}
		// それ以外
		else {
			return null;
		}
	}
	
	public static String printGeneralization(IGeneralization ig) {
		IClass sub = ig.getSubType();
		IClass sup = ig.getSuperType();
		return "○「" + sub + "」は、「" + sup + "」の一種である。 or すべての「" + sub + "」は、「" + sup + "」である。";
	}

	public static String printRealization(IRealization iRealization) {
		INamedElement client = iRealization.getClient();
		INamedElement supplier = iRealization.getSupplier();
		return "○「" + client + "」は、「" + supplier + "」を実現する。";
	}
	
	public static String printAssotication(IAssociation iAssociation) {
		String text = "";

		// 関連名の読み方の方向＝▲の方向  
		// IPresentationのname_direction_reverseが0なら関連の方向と同じ、1ながら関連の方向と反対
		boolean direction = true;
		try {
			IPresentation[] ips = iAssociation.getPresentations();
			direction = ips[0].getProperty("name_direction_reverse").equals("0");
		}catch(InvalidUsingException e){
			direction = false;
		}

		IAttribute[] iAttributes = iAssociation.getMemberEnds();
		if(iAttributes != null){
			IAttribute fromAttribute;
			IAttribute toAttribute;
			// 順方向
			if(direction){
				fromAttribute = iAttributes[0];
				toAttribute = iAttributes[1];
			} 
			// 逆方向
			else {
				fromAttribute = iAttributes[1];				
				toAttribute = iAttributes[0];
			}
			
			IClass fromClass = fromAttribute.getType();
			IClass toClass = toAttribute.getType();

			// 関連名
			String verb = iAssociation.getName();
			
			// 関連名がない場合
			if(verb.equals("")){
				// 集約 
				if (toAttribute.isAggregate() || toAttribute.isComposite()) {
					text += "○";
					verb = "の一部である";

				} 
				// 集約　fromとto
				else if(fromAttribute.isAggregate() || fromAttribute.isComposite()){
					fromClass = toAttribute.getType();
					toClass = fromAttribute.getType();
					text += "○";
					verb = "の一部である";

				} else {
					text += "×";
				}
			} else {
				text += "○";
			}
			
			// 関連端のロールを取得する
			String fromRole = fromAttribute.getName();
			String toRole = toAttribute.getName();
			
			// 読み上げの名前を決める
			String fromName = fromClass.toString();
			if(fromRole != null && fromRole.length() != 0){
				fromName += "(" + fromRole + ")";
			}
			String toName = toClass.toString();
			if(toRole != null && toRole.length() != 0){
				toName += "(" + toRole + ")";
			}

			// 読み上げ文章を作成
			text += "「" + fromName + "」は、「" + toName + "」" +  verb + "。";
		}
		return text;
	}
}
