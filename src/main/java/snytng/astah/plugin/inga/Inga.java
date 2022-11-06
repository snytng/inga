package snytng.astah.plugin.inga;

import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.INodePresentation;

/**
 * Inga - cause and effect
 */
public class Inga {
	ILinkPresentation p;
	INodePresentation source;
	INodePresentation target;
	INamedElement from;
	INamedElement to;
	private boolean positive;

	public Inga(ILinkPresentation p, INodePresentation source, INodePresentation target, boolean positive){
		this.p        = p;
		this.source   = source;
		this.target   = target;
		this.from     = (INamedElement)this.source.getModel();
		this.to       = (INamedElement)this.target.getModel();
		this.positive = positive;
	}

	public boolean isPositive() {
		return positive;
	}

	public boolean isNegative() {
		return ! positive;
	}

	private static int nPNString = 0;
	public static void setPNStringIndex(int n) {
		nPNString = n % PNStrings.length;
	}

	static final int POSITIVE_INDEX = 0;
	static final int NEGATIVE_INDEX = 1;
	static final String[][] PNStrings = new String[][] {
		{"➚", "➘"},
		{"S", "O"},
		{"＋", "－"},
		{"同", "逆"}
	};

	public static String getPositiveString() {
	    return PNStrings[nPNString][POSITIVE_INDEX];
	}

    public static String getNegativeString() {
        return PNStrings[nPNString][NEGATIVE_INDEX];
    }

	public String getArrowString() {
	    if(positive){
	        return "-(" + getPositiveString() + ")->";
	    } else {
	        return "-(" + getNegativeString() + ")->";
	    }
	}

	public String toString(){
		if(positive){
			return String.format("%s「%s」が増えれば、「%s」が増える",
			        PNStrings[nPNString][POSITIVE_INDEX],
			        from.getName(),
			        to.getName());
		} else {
            return String.format("%s「%s」が増えれば、「%s」が減る",
                    PNStrings[nPNString][NEGATIVE_INDEX],
                    from.getName(),
                    to.getName());
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

		Inga a = (Inga)obj;
		return p.equals(a.p);
	}
}

