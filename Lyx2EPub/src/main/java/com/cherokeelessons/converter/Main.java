package com.cherokeelessons.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Date;
import nl.siegmann.epublib.domain.Identifier;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.epub.EpubWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.apache.commons.lang3.text.translate.NumericEntityUnescaper;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.cherokeelessons.epub.Resource;

public class Main implements Runnable {

	/*
	 * 400P
	 */
	// private static final int IMG_HEIGHT = 711;
	// private static final int IMG_WIDTH = 400;

	/*
	 * 480P
	 */
//	 private static final int IMG_HEIGHT = 853;
//	 private static final int IMG_WIDTH = 480;
	/*
	 * 576P
	 */
//	private static final int IMG_HEIGHT = 1024;
//	private static final int IMG_WIDTH = 576;
	/*
	 * 720P
	 */
//	 private static final int IMG_HEIGHT = 1280;
//	 private static final int IMG_WIDTH = 720;
	/*
	 * 1080P
	 */
	 private static final int IMG_HEIGHT = 1920;
	 private static final int IMG_WIDTH = 1080;

	/*
	 * SQUARE: 1024
	 */
//	 private static final int IMG_HEIGHT = 1024;
//	 private static final int IMG_WIDTH = 1024;

	private static boolean skipimages = false;
	private static boolean dofontspans = true;

	private String minimalEscape(String text) {
		text = StringEscapeUtils.escapeHtml4(text);
		return UNESCAPE_EXTHTML4.translate(text);
	}

	private final CharSequenceTranslator UNESCAPE_EXTHTML4 = new AggregateTranslator(
			new LookupTranslator(EntityArrays.ISO8859_1_UNESCAPE()),
			new LookupTranslator(EntityArrays.HTML40_EXTENDED_UNESCAPE()),
			new NumericEntityUnescaper());

	public Main() {
		settings=new Settings();
	}

	private Settings settings;
	public static void main(String[] args) throws IOException {
		File settings_file = new File("settings.sample.json");
		JsonConverter json = new JsonConverter();
		json.toJson(settings_file, new Settings());
		settings_file = new File("settings.json");
		if (!settings_file.exists()) {
			json.toJson(settings_file, new Settings());
		}
		new Main().run();
		System.out.println("Done: " + new java.util.Date());
	}

	public int depth = 0;

	@Override
	public void run() {
		JsonConverter json = new JsonConverter();
		settings = json.fromJson(new File("settings.json"), Settings.class);
		File lyxfile = new File(settings.sourcedir, settings.sourcelyx);
		List<String> lines;
		try {
			lines = FileUtils.readLines(lyxfile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ListIterator<String> iline = lines.listIterator();

		while (iline.hasNext()) {
			if (iline.next().startsWith("\\begin_body")) {
				break;
			}
		}

		List<String> result = new ArrayList<>();

		final StateObject state = new StateObject();
		while (iline.hasNext()) {
			String line = iline.next();
			if (line.length() == 0) {
				continue;
			}
			if (line.startsWith("\\end_body")) {
				break;
			}
			String parsed = parse(line, iline, state);
			result.add(parsed);
		}
		String last = result.get(result.size() - 1);
		while (state.hasGroupsToClose()) {
			last += state.popGrouping();
		}
		result.set(result.size() - 1, last);

		List<String> sections = new ArrayList<>();
		ListIterator<String> rlines = result.listIterator();
		StringBuilder buffer = new StringBuilder();
		while (rlines.hasNext()) {
			String fragment = rlines.next();
			newpagecheck: {
				if (fragment.contains("<!-- epub:clear page -->")) {
					String before = StringUtils
							.substringBefore(fragment,
									"<div class=\"Standard\"><!-- epub:clear page --></div>");
					buffer.append(before);
					sections.add(buffer.toString());
					buffer.setLength(0);
					fragment = StringUtils
							.substringAfter(fragment,
									"<div class=\"Standard\"><!-- epub:clear page --></div>");
				}
				if (buffer.length() == 0) {
					break newpagecheck;
				}
				if (fragment.contains("<div class=\"Section")
						&& buffer.length() > 1024) {
					String before = StringUtils.substringBefore(fragment,
							"<div class=\"Section");
					String after = StringUtils.substringAfter(fragment, before);
					buffer.append(before);
					sections.add(buffer.toString());
					buffer.setLength(0);
					buffer.append(after);
					continue;
				}
				if (fragment.contains("<div class=\"Chapter\">")
						&& buffer.length() != 0) {
					String before = StringUtils.substringBefore(fragment,
							"<div class=\"Chapter\">");
					String after = StringUtils.substringAfter(fragment, before);
					buffer.append(before);
					sections.add(buffer.toString());
					buffer.setLength(0);
					buffer.append(after);
					continue;
				}
			}
			buffer.append(fragment);
		}
		if (buffer.length() != 0) {
			sections.add(buffer.toString());
			buffer.setLength(0);
		}

		Book epub;
		File file;
		System.out.println("[For Kindle] Processing " + sections.size()
				+ " sections.");
		epub = createEpub(Target.Kindle, sections);
		file = new File(settings.sourcedir, settings.dest_epub_kindle);
		saveEpub(file, epub);

		System.out.println("[For Smashwords] Processing " + sections.size()
				+ " sections.");
		epub = createEpub(Target.Smashwords, sections);
		file = new File(settings.sourcedir, settings.dest_epub);
		saveEpub(file, epub);
	}
	
	public void appleFy(File file) {
		try {
			ZipFile zip = new ZipFile(file);
			ZipParameters parameters=new ZipParameters();
			String fileNameInZip = Consts.META_INF+"com.apple.ibooks.display-options.xml";
			if (fileNameInZip.startsWith("/")){
				fileNameInZip=StringUtils.substring(fileNameInZip, 1);
			}
			parameters.setFileNameInZip(fileNameInZip);
			parameters.setSourceExternalStream(true);
			InputStream is=getClass().getResourceAsStream("/data/epub/com.apple.ibooks.display-options.xml");			
			zip.addStream(is, parameters);
			IOUtils.closeQuietly(is);
		} catch (ZipException e) {
			throw new RuntimeException(e);
		}
	}

	public void saveEpub(File file, Book epub) {
		try (FileOutputStream fos = FileUtils.openOutputStream(file)) {
			EpubWriter writer = new EpubWriter();
			writer.write(epub, fos);
			appleFy(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static enum Target {
		Kindle, Smashwords;
	}

	public Book createEpub(Target target, List<String> sections) {

		dofontspans = Target.Kindle.equals(target);
		System.out.println("ePub target: " + target.name()
				+ " [adding font spans: " + dofontspans + "]");

		Book epub = new Book();
		Resources res = epub.getResources();
		Spine spine = epub.getSpine();

		Metadata meta = epub.getMetadata();
		setMetadata(target, meta);

		Resource cover = getFrontCoverImage();
		epub.setCoverImage(cover);

		// res.add(getFont("FreeSerif.otf"));
		// res.add(getFont("FreeSerifBold.otf"));
		// res.add(getFont("FreeSerifBoldItalic.otf"));
		// res.add(getFont("FreeSerifItalic.otf"));

		res.add(getFont("FreeSerif.ttf"));
		res.add(getFont("FreeSerifBold.ttf"));
		res.add(getFont("FreeSerifBoldItalic.ttf"));
		res.add(getFont("FreeSerifItalic.ttf"));

		// res.add(getFont("chr.ttf"));
		// res.add(getFont("chrb.ttf"));
		// res.add(getFont("chrbi.ttf"));
		// res.add(getFont("chri.ttf"));

		// res.add(getFont("FreeSans.ttf"));
		// res.add(getFont("FreeSansBold.ttf"));
		// res.add(getFont("FreeSansBoldOblique.ttf"));
		// res.add(getFont("FreeSansOblique.ttf"));
		//
		// res.add(getFont("FreeMono.ttf"));
		// res.add(getFont("FreeMonoBold.ttf"));
		// res.add(getFont("FreeMonoBoldOblique.ttf"));
		// res.add(getFont("FreeMonoOblique.ttf"));

		Resource sheet = getDefaultStylesheet(target);
		res.add(sheet);

		Resource coverPage = getCover();
		spine.addResource(res.add(coverPage));
		epub.setCoverPage(coverPage);

		Resource titlePage = new Resource("", Consts.TEXT + "titlepage.xhtml");
		Resource copyPage = new Resource("", Consts.TEXT + "copyright.xhtml");
		Resource tocPage = new Resource("", Consts.TEXT + "toc.xhtml");
		StringBuilder toc = new StringBuilder();

		toc.append("<ul>");

		Map<String, String> syl2lat = translationMaps();
		ListIterator<String> lsections;
		int counter;

		// handle cross-references to labels (simplistic)
		Map<String, String> crossref_map = new LinkedHashMap<>();
		counter = 0;
		// find and record all labels
		lsections = sections.listIterator();
		while (lsections.hasNext()) {
			String section = lsections.next();
			counter++;
			String url = null;
			getUrl: {
				if (section.contains("class=\"Title")
						&& titlePage.getSize() == 0) {
					url = FilenameUtils.getName(titlePage.getHref());
					break getUrl;
				}
				if (section.contains(">Copyright ")
						&& section.contains(">ISBN: ")
						&& copyPage.getSize() == 0) {
					url = FilenameUtils.getName(copyPage.getHref());
					break getUrl;
				}
				if (section.contains("<!-- TOC -->") && tocPage.getSize() == 0) {
					url = FilenameUtils.getName(tocPage.getHref());
					break getUrl;
				}
				if (section.contains("class=\"Chapter\"")) {
					url = String.format("x_%03d_chapter.xhtml", counter);
					break getUrl;
				}
				url = String.format("x_%03d_section.xhtml", counter);
			}
			if (!section.contains("<a class=\"CommandInsetLabel\"")) {
				continue;
			}
			String[] labels = StringUtils.substringsBetween(section,
					"<a class=\"CommandInsetLabel\" id=\"", "\"></a>");
			for (String label : labels) {
				crossref_map.put("#" + label, url);
			}
		}
		// find and fixup all cross-references to labels
		lsections = sections.listIterator();
		while (lsections.hasNext()) {
			String section = lsections.next();
			String open = "<a class=\"CommandInsetReference\" href=\"";
			if (!section.contains(open)) {
				continue;
			}
			String close = "\">";
			String[] refs = StringUtils.substringsBetween(section, open, close);
			for (String ref : refs) {
				if (!ref.startsWith("#")) {
					continue;
				}
				String onPage = crossref_map.get(ref);
				if (StringUtils.isEmpty(onPage)) {
					System.err.println("\tBAD CROSS REFERENCE: " + ref);
					continue;
				}
				String newRef = onPage + ref;
				section = section.replace(open + ref + close, open + newRef
						+ close);
			}
			lsections.set(section);
		}

		counter = 0;
		lsections = sections.listIterator();
		while (lsections.hasNext()) {
			String section = lsections.next();
			counter++;
			// add images
			int img_counter = 0;
			if (section.contains("<!-- IMG: ")) {
				String[] images = StringUtils.substringsBetween(section,
						"<!-- IMG: ", " -->");
				if (images != null) {
					for (String image : images) {
						img_counter++;
						String image_name = FilenameUtils.getName(image);
						final File lfile = new File("output", image);
						try (InputStream imageis = FileUtils
								.openInputStream(lfile)) {
							Resource img = new Resource(imageis, Consts.IMAGES
									+ image_name);
							img.setId("IMG_" + img_counter);
							res.add(img);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			if (section.contains("class=\"Title") && titlePage.getSize() == 0) {
				section = targetedHtmlManipulation(section, target);
				section = Consts.STOCK_HEADER + section + Consts.STOCK_FOOTER;
				titlePage.setData(asBytes(section));
				epub.addSection("Title Page", titlePage);
				continue;
			}
			if (section.contains(">Copyright ") && section.contains(">ISBN: ")
					&& copyPage.getSize() == 0) {
				section = targetedHtmlManipulation(section, target);
				section = Consts.STOCK_HEADER + section + Consts.STOCK_FOOTER;
				copyPage.setData(asBytes(section));
				epub.addSection("Copyright Page", copyPage);
				continue;
			}
			if (section.contains("<!-- TOC -->") && tocPage.getSize() == 0) {
				section = targetedHtmlManipulation(section, target);
				section = Consts.STOCK_HEADER + section + Consts.STOCK_FOOTER;
				tocPage.setData(asBytes(section));
				epub.addSection("Table of Contents", tocPage);
				continue;
			}
			if (section.contains("class=\"Chapter\"")) {
				String url = String.format("x_%03d_chapter.xhtml", counter);
				String title = StringUtils.substringBetween(section,
						"class=\"Chapter\">", "<");
				section = targetedHtmlManipulation(section, target);
				section = Consts.STOCK_HEADER + section + Consts.STOCK_FOOTER;
				Resource sectionpage = new Resource(section, Consts.TEXT + url);
				sectionpage.setTitle(title);
				StringBuilder latin = new StringBuilder();
				for (char letter : title.toCharArray()) {
					if (letter < 'Ꭰ' || letter > 'Ᏼ') {
						latin.append(letter);
						continue;
					}
					latin.append(syl2lat.get(letter + ""));
				}
				epub.addSection(latin + "|" + title, sectionpage);
				toc.append("<li class=\"toc\">");
				toc.append("<a href=\"");
				toc.append(url);
				toc.append("\">");

				toc.append(title);

				toc.append("</a>");
				toc.append("</li>");
				continue;
			}
			if (StringUtils.isEmpty(StringUtils.strip(section))) {
				continue;
			}
			String url = String.format("x_%03d_section.xhtml", counter);
			section = targetedHtmlManipulation(section, target);
			section = Consts.STOCK_HEADER + section + Consts.STOCK_FOOTER;
			Resource sectionpage = new Resource(section, Consts.TEXT + url);
			sectionpage.setTitle("section");
			res.add(sectionpage);
			spine.addResource(sectionpage);
		}
		toc.append("</ul>");
		tocPage.setData(asBytes(Consts.STOCK_HEADER
				+ targetedHtmlManipulation(toc.toString(), target)
				+ Consts.STOCK_FOOTER));
		return epub;
	}

	private String specials_by_target(String section, Target target) {
		if (target.equals(Target.Kindle)) {
			final String q1 = Pattern.quote("<!-- smashwords only:begin -->");
			final String q2 = Pattern.quote("<!-- smashwords only:end -->");
			section = section.replaceAll(q1 + ".*?" + q2, "");
		}
		return section;
	}

	final String hyph = "\\-";
	final String punct = ".?!,:'\"+=\\-";
	final String lat_ur = "A-Z";
	final String lat_lr = "a-z";
	final String chr_r = "Ꭰ-Ᏼ";
	final String ud_l = "ạẹịọụṿ";
	final String ud_u = "ẠẸỊỌỤṾ";
	final String ud = "\u0323";
	final String udall = ud_l + ud_u + ud;
	final String gs = "ɂ";
	final String tm = "¹²³⁴";
	final String l_pm = ud_l + ud + ud_u + gs + tm;
	final String c_pm = ud + gs + tm;
	final String lat = lat_lr + lat_ur;
	final String space = "\\s";
	final String specialsymbols = "☞⚠⚀⚁⚂⚃⚄⚅☒☐☑₊";

	public String targetedHtmlManipulation(String section, Target target) {
		section = specials_by_target(section, target);
		section = addFontSpans(section);
		return section;
	}

	public String addFontSpans(String section) {
		if (!dofontspans) {
			return section;
		}
		String chr_lead = space + "*" + "[" + chr_r + tm + ud + hyph + gs
				+ "]*";
		String chr_mid = "[" + chr_r + "]";
		String chr_cont = "[" + chr_r + tm + ud + hyph + gs + "]*" + space
				+ "*";
		// hunt for chr, maybe with prononunciation markings
		String chr = chr_lead + chr_mid + chr_cont;

		// hunt for any words with tone marks, glottals, dotted inside, specials
		// ...
		String tn_lead = space + "*[" + lat + udall + tm + gs + hyph
				+ specialsymbols + "]*";
		String tn_mid = "[" + tm + gs + udall + specialsymbols + "]";
		String tn_cont = "[" + lat + udall + tm + gs + hyph + specialsymbols
				+ "]*" + space + "*";
		String tn = tn_lead + tn_mid + tn_cont;

		// hunt for any bracketed areas ...
		String br = "(?<=\\[)[" + lat + punct + space + hyph + specialsymbols
				+ "]*(?=\\])";

		// build alternating regex, longest to shortest, set to try and join
		// neighboring matched sections!
		String regex = "((" + chr + "|" + tn + "|" + br + ")+)";

		section = section.replaceAll(regex,
				"<span class=\"chrserif\">$1</span>");

		return section;
	}

	private Resource getCover() {
		Resource coverPage = null;
		try {
			String image = FilenameUtils.getName(settings.coverImage);
			InputStream is_coverXhtml = getClass().getResourceAsStream(
					"/data/epub/cover.xhtml");
			String xhtml = IOUtils.toString(is_coverXhtml);
			InputStream is_coverImg;
			is_coverImg = FileUtils.openInputStream(new File(settings.sourcedir,
					settings.coverImage));
			BufferedImage bimg = ImageIO.read(is_coverImg);
			int width = bimg.getWidth();
			int height = bimg.getHeight();
			xhtml = xhtml.replace("_WIDTH_", width + "").replace("_HEIGHT_",
					height + "");
			xhtml = xhtml.replace("_COVER_IMG_", image);
			InputStream is = IOUtils.toInputStream(xhtml);
			coverPage = new Resource(is, Consts.TEXT + "cover.xhtml");
			IOUtils.closeQuietly(is_coverXhtml);
			IOUtils.closeQuietly(is_coverImg);
			IOUtils.closeQuietly(is);
			coverPage.setTitle("Cover Page");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return coverPage;
	}

	private byte[] asBytes(String text) {
		try {
			return text.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return text.getBytes();
		}
	}

	private Resource getDefaultStylesheet(Target target) {
		Resource sheet = null;
		try {
			InputStream css;
			if (target.equals(Target.Kindle)) {
				css = getClass().getResourceAsStream(Consts.KindleStyleSheet);
			} else {
				css = getClass().getResourceAsStream(Consts.StyleSheet);
			}
			sheet = new Resource(css, Consts.STYLES + "stylesheet.css");
			IOUtils.closeQuietly(css);
			sheet.setTitle("StyleSheet");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return sheet;
	}

	private static final MediaType X_FONT_TTF = new MediaType(
			"application/x-font-ttf", ".ttf");

	private static final MediaType X_FONT_OTF = new MediaType(
			"application/x-font-otf", ".otf");

	private Resource getFont(String name) {
		Resource fontfile = null;
		try {
			InputStream cd;
			cd = getClass().getResourceAsStream("/data/epub/fonts/" + name);
			fontfile = new Resource(cd, Consts.FONTS + name);
			if (name.endsWith(".ttf")) {
				fontfile.setMediaType(X_FONT_TTF);
			}
			if (name.endsWith(".otf")) {
				fontfile.setMediaType(X_FONT_OTF);
			}
			IOUtils.closeQuietly(cd);
			fontfile.setTitle(name);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return fontfile;
	}

	private Resource getFrontCoverImage() {
		Resource cover = null;
		try {
			InputStream cd;
			cd = FileUtils.openInputStream(new File(settings.sourcedir,
					settings.coverImage));
			cover = new Resource(cd, Consts.IMAGES
					+ FilenameUtils.getName(settings.coverImage));
			IOUtils.closeQuietly(cd);
			cover.setTitle("Cover Image");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return cover;
	}

	private void setMetadata(Target target, Metadata meta) {

		List<Identifier> idList = meta.getIdentifiers();

		Identifier url = new Identifier();
		url.setScheme(Identifier.Scheme.URL);
		url.setValue(settings.url);
		idList.add(url);

		Identifier uid = new Identifier();
		uid.setScheme("uid");
		uid.setValue(settings.ISBN_EPUB_META());
		idList.add(uid);

		Identifier isbn = new Identifier();
		isbn.setScheme(Identifier.Scheme.ISBN);
		isbn.setValue(settings.ISBN_EPUB_META());
		isbn.setBookId(true);
		idList.add(isbn);

		for(String str_author: settings.authors) {
			Author author = new Author(str_author);
			meta.addAuthor(author);
		}

		Date date = new Date(Calendar.getInstance().getTime(),
				Date.Event.MODIFICATION);
		meta.addDate(date);

		meta.addDescription(settings.description);

		meta.setLanguage("en");

		ArrayList<String> rights = new ArrayList<String>();
		rights.add(Consts.copy
				+ settings.copyright);

		ArrayList<String> subjList = new ArrayList<String>();
		for (String subj: settings.subjects) {
			subjList.add(subj);
		}
		meta.setSubjects(subjList);

		meta.addTitle(settings.title);
		if (!Target.Kindle.equals(target)) {
			meta.addPublisher("Smashwords, Inc.");
		}
	}

	private int img_counter = 1;

	private String parse(String line, ListIterator<String> iline,
			StateObject state) {
		StringBuilder tmp = new StringBuilder();
		line = fixSpecials(line);
		whichparsing: {
			if (line.startsWith("\\family ")) {
				while (state.containsGroup("</div><!-- family -->")) {
					tmp.append(state.popGrouping());
				}
				state.pushGrouping("</div><!-- family -->");
				tmp.append("<div class=\"inline family_"
						+ StringUtils.substringAfter(line, " "));
				tmp.append("\">");
				break whichparsing;
			}
			if (line.startsWith("\\series ")) {
				while (state.containsGroup("</div><!-- series -->")) {
					tmp.append(state.popGrouping());
				}
				state.pushGrouping("</div><!-- series -->");
				tmp.append("<div class=\"inline series_"
						+ StringUtils.substringAfter(line, " "));
				tmp.append("\">");
				break whichparsing;
			}
			if (line.startsWith("\\shape ")) {
				while (state.containsGroup("</div><!-- shape -->")) {
					tmp.append(state.popGrouping());
				}
				state.pushGrouping("</div><!-- shape -->");
				tmp.append("<div class=\"inline shape_"
						+ StringUtils.substringAfter(line, " "));
				tmp.append("\">");
				break whichparsing;
			}
			if (line.startsWith("\\emph ")) {
				while (state.containsGroup("</div><!-- emph -->")) {
					tmp.append(state.popGrouping());
				}
				state.pushGrouping("</div><!-- emph -->");
				tmp.append("<div class=\"inline emph_"
						+ StringUtils.substringAfter(line, " "));
				tmp.append("\">");
				break whichparsing;
			}
			if (line.startsWith("\\bar ")) {
				while (state.containsGroup("</div><!-- bar -->")) {
					tmp.append(state.popGrouping());
				}
				state.pushGrouping("</div><!-- bar -->");
				tmp.append("<div class=\"inline bar_"
						+ StringUtils.substringAfter(line, " "));
				tmp.append("\">");
				break whichparsing;
			}
			if (line.startsWith("\\noun ")) {
				while (state.containsGroup("</div><!-- noun -->")) {
					tmp.append(state.popGrouping());
				}
				state.pushGrouping("</div><!-- noun -->");
				tmp.append("<div class=\"inline noun_"
						+ StringUtils.substringAfter(line, " "));
				tmp.append("\">");
				break whichparsing;
			}
			if (line.startsWith("\\color ")) {
				while (state.containsGroup("</div><!-- color -->")) {
					tmp.append(state.popGrouping());
				}
				state.pushGrouping("</div><!-- color -->");
				tmp.append("<div class=\"inline color_"
						+ StringUtils.substringAfter(line, " "));
				tmp.append("\">");
				break whichparsing;
			}
			if (line.startsWith("ISBN: ")) {
				line = settings.ISBN_formatted;
			}
			if (line.startsWith("<cell ")) {
				tmp.append(processCell(line, iline, state));
				break whichparsing;
			}
			if (line.startsWith("<row>")) {
				tmp.append(parseRow(iline, state));
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Tabular")) {
				tmp.append("<table>");
				// state.pushGrouping("</table>");
				final StateObject table_state = new StateObject();
				tmp.append(parseUntil("\\end_inset", iline, table_state));
				while (table_state.hasGroupsToClose()) {
					tmp.append(table_state.popGrouping());
				}
				tmp.append("</table>");
				break whichparsing;
			}
			if (line.startsWith("</lyxtabular>")) {
				tmp.append("<!-- "
						+ StringEscapeUtils.escapeHtml4(line).replace("--",
								"- -") + " -->\n");
				break whichparsing;
			}
			if (line.startsWith("<lyxtabular ")) {
				tmp.append("<!-- "
						+ StringEscapeUtils.escapeHtml4(line).replace("--",
								"- -") + " -->\n");
				break whichparsing;
			}
			if (line.startsWith("<features ")) {
				tmp.append("<!-- "
						+ StringEscapeUtils.escapeHtml4(line).replace("--",
								"- -") + " -->\n");
				break whichparsing;
			}
			if (line.startsWith("<column alignment")) {
				tmp.append("<!-- "
						+ StringEscapeUtils.escapeHtml4(line).replace("--",
								"- -") + " -->\n");
				break whichparsing;
			}
			if (line.startsWith("\\labelwidthstring")) {
				tmp.append("<!-- "
						+ StringEscapeUtils.escapeHtml4(line).replace("--",
								"- -") + " -->\n");
				break whichparsing;
			}

			if (line.startsWith("\\begin_inset Box Shadowbox")) {
				tmp.append("<div class=\"box shadowbox\">");
				int g = state.size();
				state.pushGrouping("</div>");
				tmp.append(parseUntil("\\end_inset", iline, state));
				while (state.size() > g) {
					tmp.append(state.popGrouping());
				}
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Float figure")) {
				tmp.append("<div class=\"box float\">");
				int g = state.size();
				state.pushGrouping("</div>");
				while (!StringUtils.isEmpty(iline.next()))
					;
				tmp.append(parseUntil("\\end_inset", iline, state));
				while (state.size() > g) {
					tmp.append(state.popGrouping());
				}
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Caption")) {
				tmp.append("<div class=\"box caption\">");
				int g = state.size();
				state.pushGrouping("</div>");
				tmp.append(parseUntil("\\end_inset", iline, state));
				while (state.size() > g) {
					tmp.append(state.popGrouping());
				}
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Note Note")) {
				tmp.append("<!-- SKIPPED COMMENT -->");
				discardUntil("\\end_inset", iline, new StateObject());
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset space \\quad{}")) {
				tmp.append("<!-- space-quad -->");
				discardUntil("\\end_inset", iline, new StateObject());
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset CommandInset href")) {
				tmp.append("<!-- HREF Link -->");
				discardUntil("\\end_inset", iline, new StateObject());
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset CommandInset label")) {
				tmp.append("<!-- Label Target -->");
				String ref = "";
				while (iline.hasNext()) {
					ref = iline.next();
					if (ref.startsWith("name "))
						break;
					if (StringUtils.isEmpty(ref))
						break;
				}
				tmp.append("<a class=\"CommandInsetLabel\" id=\"");
				tmp.append(StringUtils.substringBetween(ref, "\"").replaceAll(
						"[^a-zA-Z0-9]", "_"));
				tmp.append("\"></a>");
				discardUntil("\\end_inset", iline, new StateObject());
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset CommandInset ref")) {
				tmp.append("<!-- Cross Reference Link -->");
				String ref = "";
				while (iline.hasNext()) {
					ref = iline.next();
					if (ref.startsWith("reference "))
						break;
					if (StringUtils.isEmpty(ref))
						break;
				}
				String substringBetween = StringUtils.substringBetween(ref,
						"\"");
				tmp.append("<a class=\"CommandInsetReference\" href=\"#");
				tmp.append(substringBetween.replaceAll("[^a-zA-Z0-9]", "_"));
				tmp.append("\">");
				tmp.append("<span class=\"emph_on\">");
				tmp.append(substringBetween);
				tmp.append(")</span></a>");
				discardUntil("\\end_inset", iline, new StateObject());
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset CommandInset line")) {
				tmp.append("<hr />");
				discardUntil("\\end_inset", iline, new StateObject());
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Argument")) {
				tmp.append("<!-- Argument -->");
				discardUntil("\\end_inset", iline, new StateObject());
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset space \\hfill{}")) {
				tmp.append("<div class=\"hfill\">");
				state.pushGrouping("</div>");
				discardUntil("\\end_inset", iline, new StateObject());
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset space \\hspace*{\\fill}")) {
				tmp.append("<!-- protected hspace fill -->");
				discardUntil("\\end_inset", iline, new StateObject());
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Branch smashwords")) {
				discardUntil("", iline, new StateObject());
				tmp.append("<!-- smashwords only:begin -->");
				tmp.append(parseUntil("\\end_inset", iline, state));
				tmp.append("<!-- smashwords only:end -->");
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Foot")) {
				tmp.append("<div class=\"floating_foot\">");
				discardUntil("\\begin_layout Plain Layout", iline, state);
				tmp.append(parseUntil("\\end_layout", iline, state));
				tmp.append("</div>");
				discardUntil("\\end_inset", iline, state);
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Text")) {
				int g = state.size();
				state.pushGrouping("</div>");
				tmp.append("<div class=\"Text\">");
				tmp.append(parseUntil("\\end_inset", iline, state));
				while (state.size() > g) {
					tmp.append(state.popGrouping());
				}
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Flex URL")) {
				discardUntil("\\begin_layout Plain Layout", iline, state);
				String url;
				do {
					url = StringUtils.strip(iline.next());
				} while (iline.hasNext() && StringUtils.isEmpty(url));
				discardUntil("\\end_layout", iline, state);
				discardUntil("\\end_inset", iline, state);
				tmp.append("<a href=\"");
				tmp.append(url);
				tmp.append("\">");
				tmp.append(minimalEscape(url));
				tmp.append("</a>");
				break whichparsing;
			}

			if (line.startsWith("\\start_of_appendix")) {
				break whichparsing;
			}
			if (line.startsWith("\\size ")) {
				String fsizeclass = StringUtils.substringAfter(line, " ")
						.toLowerCase();
				tmp.append("<div class=\"inline fs_" + fsizeclass + "\">");
				state.pushGrouping("</div>");
				break whichparsing;
			}
			if (line.equals("\\align center")) {
				state.pushGrouping("</div>");
				tmp.append("<div class=\"align_center\">");
				break whichparsing;
			}
			if (line.equals("\\align right")) {
				state.pushGrouping("</div>");
				tmp.append("<div class=\"align_right\">");
				break whichparsing;
			}
			if (line.equals("\\noindent")) {
				tmp.append("<div class=\"noindent\">");
				state.pushGrouping("</div>");
				break whichparsing;
			}
			if (line.equals("\\begin_inset ERT")) {
				// discard until end of ERT
				tmp.append(parseErt(iline));
				break whichparsing;
			}
			if (line.equals("\\begin_deeper")) {
				StateObject substate = new StateObject();
				// shove in tag based on what inside of ...
				String close = "<!-- LOST -->";
				String astate = state.lastGrouping();
				whichstate: {
					if (astate.contains("</ul>")) {
						tmp.append("\n<!-- NESTED --><li class=\"nested\"><div>\n");
						close = "\n</div></li><!-- ul nested -->\n";
						break whichstate;
					}
					if (astate.contains("</ol>")) {
						tmp.append("\n<!-- NESTED --><li class=\"nested\"><div>\n");
						close = "\n</div></li><!-- ol nested -->\n";
						break whichstate;
					}
					tmp.append("\n<!-- NESTED --><div class=\"nested\">\n");
					close = "\n</div><!-- div nested -->\n";
				}
				tmp.append(parseUntil("\\end_deeper", iline, substate));
				while (substate.hasGroupsToClose()) {
					tmp.append(substate.popGrouping());
				}
				tmp.append(close);
				break whichparsing;
			}
			String group_itemize = "</ul><!-- itemize -->";
			String group_description = "</ul><!-- description -->";
			String group_enumerate = "</ol><!-- enumerate -->";
			if (line.startsWith("\\begin_layout Enumerate")) {
				if (!state.isActiveGroup(group_enumerate)) {
					while (state.containsGroup(group_itemize)) {
						tmp.append(state.popGrouping());
					}
					while (state.containsGroup(group_description)) {
						tmp.append(state.popGrouping());
					}
					state.pushGrouping(group_enumerate);
					tmp.append("\n<!-- enumerate --><ol>");
				}
				int g = state.size();
				tmp.append(begin_itemize(iline, state));
				while (state.size() > g) {
					tmp.append(state.popGrouping());
				}
				break whichparsing;
			}
			if (line.startsWith("\\begin_layout Itemize")) {
				if (!state.isActiveGroup(group_itemize)) {
					while (state.containsGroup(group_enumerate)) {
						tmp.append(state.popGrouping());
					}
					while (state.containsGroup(group_description)) {
						tmp.append(state.popGrouping());
					}
					state.pushGrouping(group_itemize);
					tmp.append("\n<!-- itemize --><ul>");
				}
				int g = state.size();
				tmp.append(begin_itemize(iline, state));
				while (state.size() > g) {
					tmp.append(state.popGrouping());
				}
				break whichparsing;
			}

			if (line.startsWith("\\begin_layout Description")) {
				if (!state.isActiveGroup(group_description)) {
					while (state.containsGroup(group_itemize)) {
						tmp.append(state.popGrouping());
					}
					while (state.containsGroup(group_enumerate)) {
						tmp.append(state.popGrouping());
					}
					state.pushGrouping(group_description);
					tmp.append("\n<!-- description --><ul class=\"ul_dl\">");
				}
				int g = state.size();
				tmp.append(begin_description(iline, state));
				while (state.size() > g) {
					tmp.append(state.popGrouping());
				}
				break whichparsing;
			}
			if (line.startsWith("\\begin_layout ")) {
				String cls = StringUtils
						.substringAfter(line, "\\begin_layout ");
				tmp.append(begin_layout(cls, iline, state));
				break whichparsing;
			}
			if (line.equals("\\begin_inset space ~")) {
				tmp.append("&nbsp;");
				discardUntil("\\end_inset", iline, state);
				break whichparsing;
			}
			if (line.equals("\\begin_inset Quotes eld")) {
				tmp.append("&ldquo;");
				discardUntil("\\end_inset", iline, state);
				break whichparsing;
			}
			if (line.equals("\\begin_inset Quotes erd")) {
				tmp.append("&rdquo;");
				discardUntil("\\end_inset", iline, state);
				break whichparsing;
			}
			if (line.equals("\\begin_inset Newline newline")) {
				tmp.append("<br/>");
				discardUntil("\\end_inset", iline, state);
				break whichparsing;
			}
			if (line.equals("\\begin_inset CommandInset line")) {
				tmp.append("<hr/>");
				discardUntil("\\end_inset", iline, state);
				break whichparsing;
			}
			if (line.equals("\\lang english")) {
				break whichparsing;
			}
			if (line.equals("\\lang american")) {
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Newpage")) {
				discardUntil("\\end_inset", iline, state);
				while (state.hasGroupsToClose()) {
					tmp.append(state.popGrouping());
				}
				tmp.append("<!-- epub:clear page -->");
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset VSpace")) {
				// while (state.hasGroupsToClose()) {
				// tmp.append(state.popGrouping());
				// }
				discardUntil("\\end_inset", iline, state);
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset CommandInset toc")) {
				// while (state.hasGroupsToClose()) {
				// tmp.append(state.popGrouping());
				// }
				tmp.append("<!-- TOC -->");
				discardUntil("\\end_inset", iline, state);
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset CommandInset index_print")) {
				// while (state.hasGroupsToClose()) {
				// tmp.append(state.popGrouping());
				// }
				tmp.append("<!-- INDEX -->");
				discardUntil("\\end_inset", iline, state);
				break whichparsing;
			}
			if (line.startsWith("\\begin_inset Graphics")) {
				boolean current_svg_mode = settings.svg_mode;
				String src = "";
				String lyx_width = "";
				String lyx_height = "";
				String lyx_groupId = "";
				while (iline.hasNext()) {
					String param = StringUtils.strip(iline.next());
					if (param.startsWith("filename ")) {
						src = StringUtils.substringAfter(param, "filename ");
						tmp.append("<!-- filename: " + src + " -->");
						continue;
					}
					if (param.startsWith("width ")) {
						lyx_width = StringUtils.substringAfter(param, "width ");
						tmp.append("<!-- width: " + lyx_width + " -->");
						lyx_width = lyx_width.replace("col%", "%");
						continue;
					}
					if (param.startsWith("height ")) {
						lyx_height = StringUtils.substringAfter(param,
								"height ");
						tmp.append("<!-- height: " + lyx_height + " -->");
						lyx_height = lyx_height.replace("pheight%", "%");
						continue;
					}
					if (param.startsWith("groupId")) {
						lyx_groupId = StringUtils.substringAfter(param,
								"groupId ");
						lyx_groupId = lyx_groupId.replaceAll("[^a-zA-Z0-9]",
								"_");
						tmp.append("<!-- groupId: " + lyx_groupId + " -->");
					}

					if (param.equals("\\end_inset")) {
						if (lyx_height.endsWith("em")) {
							current_svg_mode = false;
						}
						if ("dice".equals(lyx_groupId)) {
							current_svg_mode = false;
						}
						if ("comic_half".equals(lyx_groupId)) {
							current_svg_mode = false;
						}
						String template;
						if (!current_svg_mode) {
							String style = "";
							if (!StringUtils.isEmpty(lyx_height)
									|| !StringUtils.isEmpty(lyx_width)) {
								setstyle: {
									if (StringUtils.isEmpty(lyx_height)) {
										style = " style=\"width: " + lyx_width
												+ ";\"";
										break setstyle;
									}
									style = " style=\"height: " + lyx_height
											+ ";\"";
								}
							}

							template = "<div "
									+ style
									+ " class=\"lyximgdiv\">"
									+ "<img "
									+ (lyx_height.endsWith("em") ? style : "")
									+ " alt=\"_IMG_\""
									+ " class=\"lyximg"
									+ (StringUtils.isEmpty(lyx_height) ? ""
											: " autowidth") + "\""
									+ " src=\"_IMG_\" /></div>";
						} else {
							template = "<!-- epub:clear page -->"
									+ "<div class=\"svg_outer\">"
									+ "<div class=\"svg_inner\">"
									+ "<svg xmlns=\"http://www.w3.org/2000/svg\""
									+ " height=\"100%\""
									+ " preserveAspectRatio=\"xMidYMid meet\""
									+ " version=\"1.1\""
									+ " viewBox=\"0 0 _WIDTH_ _HEIGHT_\""
									+ " width=\"100%\""
									+ " xmlns:xlink=\"http://www.w3.org/1999/xlink\">"
									+ "<image height=\"_HEIGHT_\""
									+ " width=\"_WIDTH_\""
									+ " xlink:href=\"_IMG_\"/>" + "</svg>"
									+ "</div>" + "</div>";
							template += "<!-- epub:clear page -->";
						}

						File img = new File(settings.sourcedir, src);
						String iext = FilenameUtils.getExtension(img.getName());
						String simg = String.format("%03d.%s", img_counter,
								iext);
						File local_img = new File("output", simg);
						img_counter++;
						if (!img.canRead()) {
							break;
						}
						if (skipimages) {
							break;
						}
						try {
							// if ()
							BufferedImage bimg, scaled;
							bimg = ImageIO.read(img);
							/*
							 * automatic landscape vs portrait scaling
							 */
							int bw = bimg.getWidth();
							int bh = bimg.getHeight();
							int wanted_imgHeight = IMG_HEIGHT;
							int wanted_imgWidth = IMG_WIDTH;
							boolean byheight = false;
							if (!StringUtils.isEmpty(lyx_height)) {
								if (lyx_height.endsWith("em")) {
									String shtmp = lyx_height;
									if (shtmp.contains(".")) {
										shtmp = StringUtils.substringBefore(
												shtmp, ".");
									}
									shtmp = shtmp.replaceAll("[^0-9]", "");
									int htmp;
									try {
										htmp = Integer.parseInt(shtmp);
										wanted_imgHeight = 18 * (htmp + 1);
										if (wanted_imgHeight > IMG_HEIGHT) {
											wanted_imgHeight = IMG_HEIGHT;
										}
									} catch (NumberFormatException e) {
										e.printStackTrace();
									}
									byheight = true;
								}
							}
							doscaling: {
								if (byheight) {
									scaled = Scalr.resize(bimg,
											Method.ULTRA_QUALITY,
											Mode.FIT_TO_HEIGHT,
											wanted_imgWidth, wanted_imgHeight);
									break doscaling;
								}
								if (bw > bh) {
									scaled = Scalr.resize(bimg,
											Method.ULTRA_QUALITY,
											Mode.FIT_TO_HEIGHT,
											wanted_imgHeight, wanted_imgWidth);
									if (scaled.getWidth() > wanted_imgHeight) {
										scaled.flush();
										scaled = Scalr.resize(bimg,
												Method.ULTRA_QUALITY,
												Mode.FIT_TO_WIDTH,
												wanted_imgWidth,
												wanted_imgHeight);
									}
									break doscaling;
								}

								scaled = Scalr.resize(bimg,
										Method.ULTRA_QUALITY,
										Mode.FIT_TO_HEIGHT, wanted_imgWidth,
										wanted_imgHeight);
								if (scaled.getWidth() > wanted_imgWidth) {
									scaled.flush();
									scaled = Scalr.resize(bimg,
											Method.ULTRA_QUALITY,
											Mode.FIT_TO_WIDTH, wanted_imgWidth,
											wanted_imgHeight);
								}
								break doscaling;

							}
							bimg.flush();
							final String ext = FilenameUtils.getExtension(img
									.getName());
							FileUtils.touch(local_img);
							ImageIO.write(scaled, ext, local_img);

							int w = scaled.getWidth();
							int h = scaled.getHeight();
							scaled.flush();
							template = template.replace("_WIDTH_", w + "");
							template = template.replace("_HEIGHT_", h + "");
							final String srcname = local_img.getName();
							template = template.replace("_IMG_", "../"
									+ Consts.IMAGES + srcname);
							tmp.append("<!-- IMG: ");
							tmp.append(srcname);
							tmp.append(" -->");
							tmp.append(template);
						} catch (IOException e) {
							e.printStackTrace();
							break;
						}
						break;
					}
				}
				break whichparsing;
			}
			if (line.equals("\\backslash")) {
				tmp.append("\\");
				break whichparsing;
			}
			/*
			 * FAILSAFE DIE
			 */
			if (line.startsWith("\\") || line.startsWith("<")) {
				System.err
						.println("FATAL. Unhandled statement: '" + line + "'");
				System.exit(-1);
			}
			tmp.append(minimalEscape(line));
		}
		return tmp.toString();
	}

	private String parseErt(ListIterator<String> iline) {
		StateObject state = new StateObject();
		StringBuilder tmp = new StringBuilder();
		if (!iline.hasNext()) {
			return "";
		}
		discardUntil("\\begin_layout Plain Layout", iline, state);
		String inset = parseUntil("\\end_layout", iline, state);
		parseert: {
			if (inset.startsWith("\\rule{")) {
				StringBuilder style = new StringBuilder();
				String[] parms = StringUtils.substringsBetween(inset, "{", "}");
				if (parms != null) {
					int w = Integer.parseInt(parms[0].replaceAll("[^0-9]", ""));
					String percent = ((w * 100) / 6) + "%";
					style.append("width: " + percent + ";");
					if (parms.length > 1) {
						style.append("border-height: " + parms[1] + ";");
					}
				}
				tmp.append("<hr style=\"border-style: solid;");
				tmp.append(style.toString());
				tmp.append("\"/>");
				break parseert;
			}
			if (inset.startsWith("%")) {
				break parseert;
			}
			if (inset.startsWith("\\frontmatter")) {
				break parseert;
			}
			if (inset.startsWith("\\pagestyle")) {
				break parseert;
			}
			if (inset.startsWith("\\mainmatter")) {
				break parseert;
			}
			if (inset.startsWith("\\pagenumbering")) {
				break parseert;
			}
			if (inset.startsWith("\\thispagestyle")) {
				break parseert;
			}
			if (inset.startsWith("\\begin{")) {
				break parseert;
			}
			if (inset.startsWith("\\end{")) {
				break parseert;
			}
			if (inset.startsWith("\\ThisCenterWallPaper")) {
				break parseert;
			}
			System.out.println("'" + inset + "'");
			tmp.append(inset);

		}
		discardUntil("\\end_inset", iline, state);
		return tmp.toString();

	}

	private String processCell(String line, ListIterator<String> iline,
			StateObject state) {
		int span = 1;
		StringBuilder td = new StringBuilder();
		StringBuilder style = new StringBuilder();
		// if (state.columns!=0) {
		// int min = 50/state.columns;
		// String minWidth=min+"%";
		// style.append("min-width: ");
		// style.append(minWidth);
		// style.append(";");
		// }
		String tmp = StringUtils.substringBetween(line, "alignment=\"", "\"");
		if (!StringUtils.isEmpty(tmp)) {
			style.append("text-align: ");
			style.append(tmp);
			style.append(";");
		}
		tmp = StringUtils.substringBetween(line, "multicolumn=\"", "\"");
		if (!StringUtils.isEmpty(tmp)) {
			if (tmp.equals("2")) {
				discardUntil("</cell>", iline, new StateObject());
				return "";
			}
		}
		if (line.contains("topline=\"true\"")) {
			style.append("border-top-style: solid; border-top-width: medium;");
		}
		if (line.contains("bottomline=\"true\"")) {
			style.append("border-bottom-style: solid; border-bottom-width: medium;");
		}
		if (line.contains("leftline=\"true\"")) {
			style.append("border-left-style: solid; border-left-width: medium;");
		}
		if (line.contains("rightline=\"true\"")) {
			style.append("border-right-style: solid; border-right-width: medium;");
		}

		final StateObject cell_state = new StateObject();
		String contents = parseUntil("</cell>", iline, cell_state);

		// scan for and absorb following empty merge cells
		while (iline.hasNext()) {
			if (!iline.next().startsWith("<cell multicolumn=\"2\"")) {
				break;
			}
			span++;
			discardUntil("</cell>", iline, new StateObject());
		}
		iline.previous();

		// build final html for cell
		td.append("\n<td colspan=\"" + span + "\" style=\"" + style.toString()
				+ "\">");
		td.append(contents);
		while (cell_state.hasGroupsToClose()) {
			td.append(cell_state.popGrouping());
		}
		td.append("</td>");
		return td.toString();
	}

	private String parseRow(ListIterator<String> iline, StateObject state) {
		int g = state.size();
		state.pushGrouping("</tr>");
		StringBuilder row = new StringBuilder();
		row.append("<tr>");
		row.append(parseUntil("</row>", iline, state));
		while (state.size() > g) {
			row.append(state.popGrouping());
		}
		return row.toString();
	}

	private String begin_itemize(ListIterator<String> iline, StateObject state) {
		StringBuilder tmp = new StringBuilder();
		if (!iline.hasNext()) {
			return "";
		}
		int g = state.size();
		state.pushGrouping("</li>");
		String item = parseUntil("\\end_layout", iline, state);
		tmp.append("<li>");
		tmp.append(item);
		while (state.size() > g) {
			tmp.append(state.popGrouping());
		}
		return tmp.toString();
	}

	private String fixSpecials(String line) {
		if (!line.contains("\\")) {
			return line;
		}
		line = line.replace("\\SpecialChar \\ldots{}", "…");
		return line;
	}

	private void discardUntil(String marker, ListIterator<String> iline,
			StateObject state) {
		parseUntil(marker, iline, new StateObject());
	}

	private String parseUntil(String marker, ListIterator<String> iline,
			StateObject state) {
		StringBuilder tmp = new StringBuilder();
		while (iline.hasNext()) {
			String nextline = iline.next();
			if (nextline.equals(marker)) {
				break;
			}
			tmp.append(parse(nextline, iline, state));
		}
		return tmp.toString();
	}

	private String begin_description(ListIterator<String> iline,
			StateObject state) {
		StringBuilder tmp = new StringBuilder();
		if (!iline.hasNext()) {
			return "";
		}
		int g = state.size();
		String definition = parseUntil("\\end_layout", iline, state);
		while (state.size() > g) {
			definition += state.popGrouping();
		}
		String tmpdefinition = definition;

		final String defined = StringUtils.substringBefore(tmpdefinition, " ");

		if (!StringUtils.isEmpty(StringUtils.strip(defined))
				&& !defined.equals("&nbsp;")) {
			tmp.append("<li class=\"li_dt chr\"><span class=\"dt\">");
			tmp.append(defined);
			tmp.append("</span></li>");
		}
		if (definition.contains(" ")) {
			tmp.append("<li class=\"li_dd chr\">");
			tmp.append(StringUtils.substringAfter(definition, " "));
			tmp.append("</li>");
		}
		return tmp.toString();
	}

	private String begin_layout(String cls, ListIterator<String> iline,
			StateObject state) {
		System.out.println("Processing layout: '" + cls + "'");
		StringBuilder tmp = new StringBuilder();
		/*
		 * generic layouts should be top level elements
		 */
		while (state.hasGroupsToClose()) {
			tmp.append(state.popGrouping());
		}
		/*
		 * treat all other layouts as a standard div ...
		 */
		cls = cls.replaceAll("([^a-zA-Z])", "_");
		String opentag = "\n<div class=\"" + cls + "\">";
		String closetag = "</div><!-- " + cls + " -->";

		final boolean same_layout = state.isActiveGroup(closetag);

		if (same_layout) {
			opentag = "";
			closetag = "";
		}
		int g = state.size();
		if (!StringUtils.isEmpty(closetag)) {
			state.pushGrouping(closetag);
		}
		final String parsed = parseUntil("\\end_layout", iline, state);
		// if (!StringUtils.isEmpty(parsed)) {
		tmp.append(opentag);
		tmp.append(parsed);
		// }
		while (state.size() > g) {
			tmp.append(state.popGrouping());
		}
		return tmp.toString();
	}

	private static class StateObject {

		public StateObject() {
		}

		public boolean hasGroupsToClose() {
			return grouping_stack.size() > 0;
		}

		public int size() {
			return grouping_stack.size();
		}

		public boolean containsGroup(String group) {
			if (grouping_stack.size() == 0) {
				return false;
			}
			return grouping_stack.contains(group);
		}

		public boolean isActiveGroup(String group) {
			if (grouping_stack.size() == 0) {
				return false;
			}
			return grouping_stack.get(grouping_stack.size() - 1).equals(group);
		}

		public String lastGrouping() {
			if (grouping_stack.size() != 0) {
				return grouping_stack.get(grouping_stack.size() - 1);
			}
			return "";
		}

		public String popGrouping() {
			if (grouping_stack.size() == 0) {
				return "";
			}
			return grouping_stack.remove(grouping_stack.size() - 1);
		}

		public int pushGrouping(String grouping) {
			grouping_stack.add(grouping);
			return grouping_stack.size();
		}

		final private List<String> grouping_stack = new ArrayList<>();
	}

	public static HashMap<String, String> translationMaps() {
		int ix = 0;
		String letter;
		String prefix;
		char chrStart = 'Ꭰ';
		String[] vowels = new String[6];

		HashMap<String, String> syllabary2latin = new HashMap<String, String>();

		vowels[0] = "a";
		vowels[1] = "e";
		vowels[2] = "i";
		vowels[3] = "o";
		vowels[4] = "u";
		vowels[5] = "v";

		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, vowels[ix]);
			syllabary2latin.put(vowels[ix], letter);
		}

		syllabary2latin.put("ga", "Ꭶ");
		syllabary2latin.put("Ꭶ", "ga");

		syllabary2latin.put("ka", "Ꭷ");
		syllabary2latin.put("Ꭷ", "ka");

		prefix = "k";
		chrStart = 'Ꭸ';
		for (ix = 1; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix - 1));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "g";
		chrStart = 'Ꭸ';
		for (ix = 1; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix - 1));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "h";
		chrStart = 'Ꭽ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "l";
		chrStart = 'Ꮃ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "m";
		chrStart = 'Ꮉ';
		for (ix = 0; ix < 5; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		syllabary2latin.put("Ꮎ", "na");
		syllabary2latin.put("na", "Ꮎ");
		syllabary2latin.put("Ꮏ", "hna");
		syllabary2latin.put("hna", "Ꮏ");
		syllabary2latin.put("Ꮐ", "nah");
		syllabary2latin.put("nah", "Ꮐ");

		prefix = "n";
		chrStart = 'Ꮑ';
		for (ix = 1; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix - 1));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "qu";
		chrStart = 'Ꮖ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "gw";
		chrStart = 'Ꮖ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		syllabary2latin.put("Ꮜ", "sa");
		syllabary2latin.put("sa", "Ꮜ");
		syllabary2latin.put("Ꮝ", "s");
		syllabary2latin.put("s", "Ꮝ");

		prefix = "s";
		chrStart = 'Ꮞ';
		for (ix = 1; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix - 1));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		syllabary2latin.put("da", "Ꮣ");
		syllabary2latin.put("Ꮣ", "da");
		syllabary2latin.put("ta", "Ꮤ");
		syllabary2latin.put("Ꮤ", "ta");
		syllabary2latin.put("de", "Ꮥ");
		syllabary2latin.put("Ꮥ", "de");
		syllabary2latin.put("te", "Ꮦ");
		syllabary2latin.put("Ꮦ", "te");
		syllabary2latin.put("di", "Ꮧ");
		syllabary2latin.put("Ꮧ", "di");
		syllabary2latin.put("ti", "Ꮨ");
		syllabary2latin.put("Ꮨ", "ti");
		syllabary2latin.put("do", "Ꮩ");
		syllabary2latin.put("Ꮩ", "do");
		syllabary2latin.put("to", "Ꮩ");
		syllabary2latin.put("Ꮩ", "to");
		syllabary2latin.put("du", "Ꮪ");
		syllabary2latin.put("Ꮪ", "du");
		syllabary2latin.put("tu", "Ꮪ");
		syllabary2latin.put("Ꮪ", "tu");
		syllabary2latin.put("dv", "Ꮫ");
		syllabary2latin.put("Ꮫ", "dv");
		syllabary2latin.put("tv", "Ꮫ");
		syllabary2latin.put("Ꮫ", "tv");
		syllabary2latin.put("dla", "Ꮬ");
		syllabary2latin.put("Ꮬ", "dla");

		prefix = "tl";
		chrStart = 'Ꮭ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "hl";
		chrStart = 'Ꮭ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "ts";
		chrStart = 'Ꮳ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "j";
		chrStart = 'Ꮳ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "w";
		chrStart = 'Ꮹ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}

		prefix = "y";
		chrStart = 'Ꮿ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			syllabary2latin.put(letter, prefix + vowels[ix]);
			syllabary2latin.put(prefix + vowels[ix], letter);
		}
		return syllabary2latin;
	}

	public static HashMap<String, String> lat2chrMap() {
		int ix = 0;
		String letter;
		String prefix;
		char chrStart = 'Ꭰ';
		String[] vowels = new String[6];

		HashMap<String, String> latin2syllabary = new HashMap<String, String>();

		vowels[0] = "a";
		vowels[1] = "e";
		vowels[2] = "i";
		vowels[3] = "o";
		vowels[4] = "u";
		vowels[5] = "v";

		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(letter, vowels[ix]);
			latin2syllabary.put(vowels[ix], letter);
		}

		latin2syllabary.put("ga", "Ꭶ");

		latin2syllabary.put("ka", "Ꭷ");

		prefix = "g";
		chrStart = 'Ꭸ';
		for (ix = 1; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix - 1));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "k";
		chrStart = 'Ꭸ';
		for (ix = 1; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix - 1));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "h";
		chrStart = 'Ꭽ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "l";
		chrStart = 'Ꮃ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "m";
		chrStart = 'Ꮉ';
		for (ix = 0; ix < 5; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		latin2syllabary.put("na", "Ꮎ");
		latin2syllabary.put("hna", "Ꮏ");
		latin2syllabary.put("nah", "Ꮐ");

		prefix = "n";
		chrStart = 'Ꮑ';
		for (ix = 1; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix - 1));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "qu";
		chrStart = 'Ꮖ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "gw";
		chrStart = 'Ꮖ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		latin2syllabary.put("sa", "Ꮜ");
		latin2syllabary.put("s", "Ꮝ");

		prefix = "s";
		chrStart = 'Ꮞ';
		for (ix = 1; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix - 1));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		latin2syllabary.put("da", "Ꮣ");
		latin2syllabary.put("ta", "Ꮤ");
		latin2syllabary.put("de", "Ꮥ");
		latin2syllabary.put("te", "Ꮦ");
		latin2syllabary.put("di", "Ꮧ");
		latin2syllabary.put("ti", "Ꮨ");
		latin2syllabary.put("do", "Ꮩ");
		latin2syllabary.put("to", "Ꮩ");
		latin2syllabary.put("du", "Ꮪ");
		latin2syllabary.put("tu", "Ꮪ");
		latin2syllabary.put("dv", "Ꮫ");
		latin2syllabary.put("tv", "Ꮫ");
		latin2syllabary.put("dla", "Ꮬ");

		prefix = "hl";
		chrStart = 'Ꮭ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "tl";
		chrStart = 'Ꮭ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "j";
		chrStart = 'Ꮳ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "ts";
		chrStart = 'Ꮳ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "w";
		chrStart = 'Ꮹ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}

		prefix = "y";
		chrStart = 'Ꮿ';
		for (ix = 0; ix < 6; ix++) {
			letter = Character.toString((char) (chrStart + ix));
			latin2syllabary.put(prefix + vowels[ix], letter);
		}
		return latin2syllabary;
	}
}
