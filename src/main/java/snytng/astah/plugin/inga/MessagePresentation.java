package snytng.astah.plugin.inga;

import java.util.ArrayList;
import java.util.List;

import com.change_vision.jude.api.inf.presentation.IPresentation;

//読み上げ結果の構造体
public class MessagePresentation {

	// 読み上げられたメッセージ
	List<String> messages = new ArrayList<>();

	// 読み上げられた要素のIPresentation[]
	List<IPresentation[]> presentations = new ArrayList<>();

	public MessagePresentation(){
		clear();
	}

	public void clear(){
		this.messages      = new ArrayList<>();
		this.presentations = new ArrayList<>();
	}

	public void add(String m, IPresentation[] p){
		this.messages.add(m);
		this.presentations.add(p);
	}

	public void addAll(MessagePresentation mp){
		this.messages.addAll(mp.messages);
		this.presentations.addAll(mp.presentations);
	}

	public String[] getMessagesArray(){
		return this.messages.toArray(new String[messages.size()]);
	}

	public String[] getPresentationsArray(){
		return this.presentations.toArray(new String[presentations.size()]);
	}

}