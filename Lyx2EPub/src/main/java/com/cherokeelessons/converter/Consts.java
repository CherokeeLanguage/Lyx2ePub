package com.cherokeelessons.converter;

public class Consts {

	public static final String HtmlSoftHyphen = "&shy;";
	public static final String[] SYLLABARY_arr;
	static {
		SYLLABARY_arr = new String[85];
		for (char s = 'Ꭰ'; s <= 'Ᏼ'; s++) {
			SYLLABARY_arr[s - 'Ꭰ'] = String.valueOf(s);
		}
	}

	public static final String copy = "©"; // copyright symbol
	public static final String registered = "®"; // registered mark symbol
	public static final String STOCK_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n"
			+ "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\"\n"
			+ "            \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n"
			+ "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
			+ "<head>\n<meta http-equiv=\"content-type\"\n"
			+ "	content=\"application/xhtml+xml; charset=UTF-8\" />\n"
			+ "<title>.</title>\n"
			+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"../Styles/stylesheet.css\" />\n"
			+ "</head>" + "\n<body>";
	public static final String STOCK_FOOTER = "\n</body>" + "\n</html>";

	// sigil derived locations for common assets
	public static final String TEXT = "Text/";
	public static final String STYLES = "Styles/";
	public static final String IMAGES = "Images/";
	public static final String FONTS = "Fonts/";
	public static final String AUDIO = "Audio/";
	public static final String VIDEO = "Video/";
	public static final String MISC = "Misc/";
	public static final String META_INF = "/META-INF/";

	public static final String StyleSheet = "/data/epub/epub-default.css";
	public static final String KindleStyleSheet = "/data/epub/epub-kindle.css";
	public static final String FreeSerifSheet = "/data/epub/freeserif.css";
	public static final String KF8 = "/data/epub/kf8.css";
}
