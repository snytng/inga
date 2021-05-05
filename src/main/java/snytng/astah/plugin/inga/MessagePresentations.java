package snytng.astah.plugin.inga;

import java.util.ArrayList;
import java.util.List;

import com.change_vision.jude.api.inf.presentation.IPresentation;

//読み上げ結果の構造体
public class MessagePresentation {

	class MP {
		String message;
		IPresentation[] presentations;

		MP(String m, IPresentation[] ps){
			this.message = m;
			this.presentations = ps;
		}
	}

	List<MP> mps= new ArrayList<>();

	public MessagePresentation(){
		clear();
	}

	public void clear(){
		this.mps = new ArrayList<>();
	}

	public void add(String m, IPresentation[] p){
		this.add(new MP(m,p));
	}

	public void add(MP mp){
		this.mps.add(mp);
	}

	public void addAll(MessagePresentation mp){
		this.mps.addAll(mp.mps);
	}

	public String[] getMessagesArray(){
		return this.mps.stream().map(mp -> mp.message).toArray(String[]::new);
	}

	public IPresentation[][] getPresentationsArray(){
		return this.mps.stream().map(mp -> mp.presentations).toArray(IPresentation[][]::new);
	}

	public int size() {
		return this.mps.size();
	}

}