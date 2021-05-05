package snytng.astah.plugin.inga;

import com.change_vision.jude.api.inf.presentation.IPresentation;

public class MessagePresentation {
	String message;
	IPresentation[] presentations;

	public MessagePresentation(String m, IPresentation[] ps){
		this.message = m;
		this.presentations = ps;
	}
}