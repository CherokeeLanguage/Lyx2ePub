package com.cherokeelessons.converter;

public class Consts {

	public static String description = "... description ...";
	public static final String ISBN_EPUB_FORMATTED = "ISBN: 978-0-000-00000-0";
	public static final String ISBN_EPUB_META = ISBN_EPUB_FORMATTED.replaceAll("[^0-9]", "");
	
	public static final String sourcedir = "./";
	public static final String sourcelyx = "input.lyx";
	public static final String EPUB = "epub/ouput.epub";
	public static final String EPUB_Kindle = "epub/output-Kindle.epub";
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
			+ "</head>" + "\n<body>\n";
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

	public static final String CoverImage = "artwork/ebook-cover-784x1024.png";
	public static final String StyleSheet = "/data/epub/epub-default.css";
	public static final String KindleStyleSheet = "/data/epub/epub-kindle.css";
	public static final String TITLE = "... title ...";
}
