package com.cherokeelessons.converter;

public class Consts {

	public static String description = "One of the keys to acquiring a new language is to learn the patterns that make up the language. Simply learning phrases so you can speak “pidgin” Cherokee is not learning Cherokee. You need to learn the fundamentals of the language on how words are put together to be able to understand and communicate in the language. There are many degrees of meaning that different word parts provide and if you don't learn these shades of meaning up front and how they are expressed you will never progress beyond simple memorized phrases and never obtain satisfaction with the language. While each person's skill will differ, one should strive to gain enough understanding of the mechanics of language to be able to comprehend and communicate effectively. The goal of this material is to provide you a solid structural foundation on how Cherokee works. You will learn how words are put together in basic sentences and how to form new words for ideas not listed in the dictionary. You will be able to parse out the most important meanings from more complex writings and sayings found in the real world. Eventually you will be able to think and dream in Cherokee as well. None of these things are guaranteed, but with enough effort, and a good path to follow in learning the fundamental patterns hidden within the language, there is a good chance you will master the Cherokee language and possibly much more.";
	public static final String ISBN_EPUB_FORMATTED = "ISBN: 978-1-311-16595-4";
	public static final String ISBN_EPUB_META = ISBN_EPUB_FORMATTED.replaceAll("[^0-9]", "");
	
	public static final String sourcedir = "/home/mjoyner/Documents/ᏣᎳᎩ/Lessons/Cherokee Language Lessons-Volume 1";
	public static final String sourcelyx = "Cherokee Language Lessons.lyx";
	public static final String EPUB = "epub/Cherokee Language Lessons.epub";
	public static final String EPUB_Kindle = "epub/Cherokee Language Lessons-Kindle.epub";
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
	public static final String TITLE = "Cherokee Language Lessons";
}
