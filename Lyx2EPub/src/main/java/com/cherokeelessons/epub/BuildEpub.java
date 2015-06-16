package com.cherokeelessons.epub;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Date;
import nl.siegmann.epublib.domain.Identifier;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.cherokeelessons.converter.Consts;

public class BuildEpub {

	private static final String ISBN_EPUB = "XXX";
	private FullBookData bd;
	private Book epub;

	public BuildEpub(FullBookData bd) {
		this.bd = bd;
		epub = new Book();
		Resources res = epub.getResources();
		Spine spine = epub.getSpine();

		Metadata meta = epub.getMetadata();
		setMetadata(meta);

		Resource cover = getFrontCoverImage();
		epub.setCoverImage(cover);

		// res.add(getAppleXml());
		res.add(getFont("FreeSerif.ttf"));
		res.add(getFont("FreeSerifBold.ttf"));
		res.add(getFont("FreeSerifBoldItalic.ttf"));
		res.add(getFont("FreeSerifItalic.ttf"));

		// res.add(getFont("FreeSans.otf"));
		// res.add(getFont("FreeSansBold.otf"));
		// res.add(getFont("FreeSansBoldOblique.otf"));
		// res.add(getFont("FreeSansOblique.otf"));
		//
		// res.add(getFont("FreeMono.otf"));
		// res.add(getFont("FreeMonoBold.otf"));
		// res.add(getFont("FreeMonoBoldOblique.otf"));
		// res.add(getFont("FreeMonoOblique.otf"));

		Resource sheet = getDefaultStylesheet();
		res.add(sheet);

		Resource coverPage = getCover();
		spine.addResource(res.add(coverPage));
		epub.setCoverPage(coverPage);

		Resource halfTitle = getHalfTitle();
		epub.addSection("Front Matter", halfTitle);

		Resource logo = getPublisherLogo();
		res.add(logo);

		Resource fullTitlePage = getFullTitle();
		epub.addSection("Title Page", fullTitlePage);

		Resource copyrightPage = getCopyright();
		epub.addSection("Copyright Page", copyrightPage);

		Resource introductionPage = getIntroduction();
		epub.addSection("Introduction", introductionPage);
	}

	private void setMetadata(Metadata meta) {

		List<Identifier> idList = meta.getIdentifiers();

		Identifier url = new Identifier();
		url.setScheme(Identifier.Scheme.URL);
		url.setValue("http://www.CherokeeLessons.com/");
		idList.add(url);

		Identifier uid = new Identifier();
		uid.setScheme("uid");
		uid.setValue(ISBN_EPUB);
		idList.add(uid);

		Identifier isbn = new Identifier();
		isbn.setScheme(Identifier.Scheme.ISBN);
		isbn.setValue(ISBN_EPUB);
		isbn.setBookId(true);
		idList.add(isbn);

		Author author = new Author("Michael Joyner");
		meta.addAuthor(author);

		Date date = new Date(Calendar.getInstance().getTime(),
				Date.Event.MODIFICATION);
		meta.addDate(date);

		String description = "ᏅᏙᎯ ᏥᏍᏚ, ᏅᏙᎯ ᏥᏍᏚ, ᎭᏢ ᎮᎪ?<br/>"
				+ "Rabbit in the moon, rabbit in the moon, where are you going?<br/><br/>"
				+ "“ᎦᎸᎳᏗᏜ ᎦᎵᏌᎳᏗ ᎡᎳᏗ ᏓᎩᎪᏩᏛᏗ”<br/>"
				+ "“Towards high I am rising to see all about.”<br/><br/>"
				+ "ᏅᏙᎯ ᏥᏍᏚ, ᏅᏙᎯ ᏥᏍᏚ, ᎦᏙᎲ ᎮᎪ?<br/>"
				+ "Rabbit in the moon, rabbit in the moon, why are you going?<br/><br/>"
				+ "“ᎦᎵᏘ. ᏩᎭᏯ ᎠᏆᏰᏍᏗ ᎤᏚᎵ. ᎨᏡᎲᏍᎦ...”<br/>"
				+ "“I am running away. The wolf wants to eat me. He is howling...”<br/><br/>"
				+ "This is an exampe of one of the many stories within that are "
				+ "written in a fashion similar to what you would find in "
				+ "an English reader designed for reading to a young child.<br/><br/>"
				+ "Includes both a special section on grammar structures introduced "
				+ "in this volume and  a complete vocabulary to assist with word lookup.<br/><br/>";
		meta.addDescription(description);

		meta.setLanguage("en");

		ArrayList<String> rights = new ArrayList<String>();
		rights.add(Consts.copy + "Michael Joyner, All Rights Reserved.");

		ArrayList<String> subjList = new ArrayList<String>();
		subjList.add("FOR031000");
		subjList.add("FOREIGN LANGUAGE STUDY");
		subjList.add("Native American Languages");
		subjList.add("Cherokee");
		subjList.add("Cherokee Language");
		subjList.add("ᏣᎳᎩ");
		subjList.add("ᏣᎳᎩ ᎦᏬᏂᎯᏍᏗ");
		meta.setSubjects(subjList);

		meta.addTitle("Michael's Cherokee Reader - ᎹᎦᎵ ᎤᏤᎵ ᏣᎳᎩ ᎠᎪᎵᏰᏗ");
		meta.addPublisher("Smashwords, Inc.");
	}

	private Resource getFrontCoverImage() {
		Resource cover = null;
		try {
			InputStream cd;
			cd = getClass().getResourceAsStream(
					"/data/images/" + Consts.CoverImage);
			cover = new Resource(cd, Consts.IMAGES + Consts.CoverImage);
			IOUtils.closeQuietly(cd);
			cover.setTitle("Cover Image");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return cover;
	}

	private Resource getDefaultStylesheet() {
		Resource sheet = null;
		try {
			InputStream css;
			css = getClass().getResourceAsStream(Consts.StyleSheet);
			sheet = new Resource(css, Consts.STYLES + "stylesheet.css");
			IOUtils.closeQuietly(css);
			sheet.setTitle("StyleSheet");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return sheet;
	}

	private Resource getCover() {
		Resource coverPage = null;
		try {
			InputStream is_coverXhtml = getClass().getResourceAsStream(
					"/data/epub/cover.xhtml");
			String xhtml = IOUtils.toString(is_coverXhtml);
			InputStream is_coverImg;
			is_coverImg = getClass().getResourceAsStream(
					"/data/images/" + Consts.CoverImage);
			BufferedImage bimg = ImageIO.read(is_coverImg);
			int width = bimg.getWidth();
			int height = bimg.getHeight();
			xhtml = xhtml.replace("_WIDTH_", width + "").replace("_HEIGHT_",
					height + "");
			xhtml = xhtml.replace("_COVER_IMG_", Consts.CoverImage);
			InputStream is = IOUtils.toInputStream(xhtml);
			coverPage = new Resource(is, Consts.TEXT + "cover.xhtml");
			IOUtils.closeQuietly(is_coverXhtml);
			IOUtils.closeQuietly(is_coverImg);
			IOUtils.closeQuietly(is);
			coverPage.setTitle("Cover Page");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return coverPage;
	}

	private Resource getHalfTitle() {
		final String XHTML_RSRC = "/data/epub/half-title-page.xhtml";
		final String href = Consts.TEXT + "halfTitlePage.xhtml";
		String halfPage = "";
		Resource res = null;

		InputStream is = getClass().getResourceAsStream(XHTML_RSRC);
		try {
			halfPage = IOUtils.toString(is, "UTF-8");
			IOUtils.closeQuietly(is);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		String page = halfPage;
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
		page = page
				.replace("_bookTitle_", "ᎹᎦᎵ ᎤᏤᎵ ᏣᎳᎩ ᎠᎪᎵᏰᏗ");
		page = page.replace("_bookEdition_",
				sdf.format(Calendar.getInstance().getTime()));
		page = page.replace("_bookSubtitle_",
				"Michael's Cherokee Reader");
		// page = Util.titleSplitByDiv(bte.title, page);
		res = new Resource(page, href);
		res.setTitle("Half Title Page");
		return res;
	}

	private Resource getIntroduction() {
		final String XHTML_RSRC = "/data/epub/preface.xhtml";
		final String href = Consts.TEXT + "preface.xhtml";
		String preface = "";
		Resource res = null;

		InputStream is = getClass().getResourceAsStream(XHTML_RSRC);
		try {
			preface = IOUtils.toString(is, "UTF-8");
			IOUtils.closeQuietly(is);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		res = new Resource(preface, href);
		res.setTitle("Preface");
		return res;
	}

	private Resource getResourceImage(String name) {
		Resource image = null;
		try {
			InputStream cd;
			cd = getClass().getResourceAsStream("/data/images/" + name);
			image = new Resource(cd, Consts.IMAGES + name);
			IOUtils.closeQuietly(cd);
			image.setTitle(name);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return image;
	}

	public static final MediaType X_FONT_TTF = new MediaType(
			"application/x-font-ttf", ".ttf");

	private Resource getFont(String name) {
		Resource fontfile = null;
		try {
			InputStream cd;
			cd = getClass().getResourceAsStream("/data/epub/fonts/" + name);
			fontfile = new Resource(cd, Consts.FONTS + name);
			if (name.endsWith(".ttf")) {
				fontfile.setMediaType(X_FONT_TTF);
			}
			IOUtils.closeQuietly(cd);
			fontfile.setTitle(name);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return fontfile;
	}

	/*
	 * doesn't work! Gets shoved into OEBPS no matter what I do!
	 */
	// private Resource getAppleXml() {
	// Resource otf = null;
	// try {
	// InputStream cd;
	// final String appleXml = "com.apple.ibooks.display-options.xml";
	// cd = getClass().getResourceAsStream("/data/epub/" + appleXml);
	// otf = new Resource(cd, Consts.META_INF + appleXml);
	// otf.setHref("");
	// IOUtils.closeQuietly(cd);
	// otf.setTitle(appleXml);
	// } catch (IOException e) {
	// e.printStackTrace();
	// System.exit(-1);
	// }
	// return otf;
	// }

	private Resource getPublisherLogo() {
		Resource res;
		res = getResourceImage("coyote.png");
		res.setTitle("Publisher Logo");
		return res;
	}

	private Resource getFullTitle() {
		String fullPage = "";
		Resource res = null;

		InputStream is = getClass().getResourceAsStream(
				"/data/epub/full-title-page.xhtml");
		try {
			fullPage = IOUtils.toString(is, "UTF-8");
			IOUtils.closeQuietly(is);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		String page = fullPage;

		page = page.replace("_bookAuthorTitle_", "Editor");
		page = page.replace("_bookAuthor_", "Michael Joyner");
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
		page = page
				.replace("_bookTitle_", "ᎹᎦᎵ ᎤᏤᎵ ᏣᎳᎩ ᎠᎪᎵᏰᏗ");
		page = page.replace("_bookEdition_",
				sdf.format(Calendar.getInstance().getTime()));
		page = page.replace("_bookSubtitle_",
				"Michael's Cherokee Reader");

		/**
		 * check length of longest word in TITLE and set TITLE size
		 * appropriately
		 */
		String bookTitleSize;
		String bookSubtitleSize;
		String bookAuthorSize;
		int bookTitleWordLength = 0;
		String[] bookTitleWords = StringUtils
				.split("ᎹᎦᎵ ᎤᏤᎵ ᏣᎳᎩ ᎠᎪᎵᏰᏗ");
		for (int i = 0; i < bookTitleWords.length; i++) {
			int l = bookTitleWords[i].length();
			if (l > bookTitleWordLength) {
				bookTitleWordLength = l;
			}
		}
		do {
			if (bookTitleWordLength > 40) {
				bookTitleSize = "h4";
				bookSubtitleSize = "h5";
				bookAuthorSize = "h6";
				break;
			}
			if (bookTitleWordLength > 30) {
				bookTitleSize = "h3";
				bookSubtitleSize = "h4";
				bookAuthorSize = "h5";
				break;
			}
			if (bookTitleWordLength > 20) {
				bookTitleSize = "h2";
				bookSubtitleSize = "h3";
				bookAuthorSize = "h4";
				break;
			}
			bookTitleSize = "h1";
			bookSubtitleSize = "h2";
			bookAuthorSize = "h3";
		} while (false);
		page = page.replace("_bookTitleSize_", bookTitleSize);
		page = page.replace("_bookSubtitleSize_", bookSubtitleSize);
		page = page.replace("_bookAuthorSize_", bookAuthorSize);

		String bookTitlePageLogo = "coyote.png";
		page = page.replace("_bookTitlePageLogo_", bookTitlePageLogo);

		res = new Resource(page, Consts.TEXT + "fullTitlePage.xhtml");
		res.setTitle("Full Title Page");
		return res;
	}

	private Resource getCopyright() {
		final String XHTML_RSRC = "/data/epub/copyright-page.xhtml";
		final String href = Consts.TEXT + "copyrightPage.xhtml";

		final String website = "<a href='http://www.CherokeeLessons.com/'>http://www.CherokeeLessons.com/</a>";
		Calendar c = Calendar.getInstance();
		final String year = c.get(Calendar.YEAR) + "";
		Resource res = null;
		String copyPage = "";
		InputStream is = getClass().getResourceAsStream(XHTML_RSRC);
		try {
			copyPage = IOUtils.toString(is, "UTF-8");
			IOUtils.closeQuietly(is);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		String page = copyPage;
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
		page = page
				.replace("_bookTitle_", "ᎹᎦᎵ ᎤᏤᎵ ᏣᎳᎩ ᎠᎪᎵᏰᏗ");
		page = page.replace("_bookEdition_",
				sdf.format(Calendar.getInstance().getTime()));
		page = page.replace("_bookSubtitle_",
				"Michael's Cherokee Reader");
		page = page.replace("_publisherTM_", "");
		page = page.replace("_docType_", "");
		page = page.replace("_year_", year);
		page = page.replace("_website_", website);
		page = page.replace("_isbn_", formattedIsbn(ISBN_EPUB));

		res = new Resource(page, href);
		res.setTitle("Copyright Page");
		return res;
	}

	private String formattedIsbn(String isbn13) {
		isbn13 = isbn13.trim();
		while (isbn13.length() < 13)
			isbn13 = "0" + isbn13;
		StringBuilder sb = new StringBuilder();
		sb.append(isbn13.substring(0, 3));
		sb.append("-");
		sb.append(isbn13.substring(3, 4));
		sb.append("-");
		sb.append(isbn13.substring(4, 7));
		sb.append("-");
		sb.append(isbn13.substring(7, 12));
		sb.append("-");
		sb.append(isbn13.substring(12, 13));
		return sb.toString();
	}

	public void build() {
		ArrayList<String> book_order = new ArrayList<>();
		HashMap<String, BookData> books = new HashMap<>();
		// StringBuilder xhtml=new StringBuilder();
		String pbook = "";
		String pchapt = "";
		BookData activebook = null;
		StringBuilder activeChapter = null;
		for (String line : bd.cherokee_bc) {
			if (!line.contains("||")) {
				continue;
			}
			String verse_lookup = StringUtils.substringBefore(line, "||");
			String book_code = StringUtils.substringBefore(line, "|");
			if (!book_code.equals(pbook)) {
				pbook = book_code;
				book_order.add(book_code);
				activebook = new BookData();
				activebook.code = book_code;
				activebook.cherokee_name = StringEscapeUtils
						.escapeHtml4(bd.book_mapping.get(book_code));
				activebook.english_name = StringEscapeUtils
						.escapeHtml4(bd.book_mapping.get(book_code + "_eng"));
				books.put(activebook.code, activebook);
				pchapt = "";
			}
			String cchapt = StringUtils.substringBetween(line, "|", "|");
			if (!cchapt.equals(pchapt)) {
				int n = Integer.valueOf(cchapt);
				pchapt = cchapt;
				while (activebook.chapters.size() < n + 1)
					activebook.chapters.add(new StringBuilder());
				activeChapter = activebook.chapters.get(n);
				// if (n == 2) {
				// System.out.println(activebook.chapters.get(1));
				// }
			}
			String verse_number = StringUtils.substringAfterLast(verse_lookup,
					"|");
			String text = StringUtils.substringAfter(line, "||");
			text = StringEscapeUtils.escapeHtml4(text);
			for (String word : StringUtils.split(text)) {
				if (word.length() > 5) {
					text = text.replaceAll("([Ꭰ-Ᏼ]{3})([Ꭰ-Ᏼ]{3})", "$1 _-_ $2");
					text = text.replace(" _-_ ", Consts.HtmlSoftHyphen);
				}
			}
			activeChapter.append("<p class='chr'>");
			activeChapter.append("<span class='verse_number'>");
			activeChapter.append(verse_number);
			activeChapter.append("</span>");
			activeChapter.append(" ");
			text = text.replaceAll("( *[Ꭰ-Ᏼ][Ꭰ-Ᏼ ,\\.\\!\\&;a-zA-Z:]*)",
					"_S1_$1_S2_");
			text = text.replace("_S1_", "<span class='chr'>");
			text = text.replace("_S2_", "</span>");
			activeChapter.append(text);
			activeChapter.append("</p>\n");
			// ENGLISH
			line = bd.english_bc.get(verse_lookup);
			if (!StringUtils.isEmpty(line)) {
				text = StringEscapeUtils.escapeHtml4(StringUtils
						.substringAfter(line, "||"));
				verse_number = StringUtils.substringBetween(line, "|", "||");
				verse_number = StringUtils
						.substringAfterLast(verse_number, "|");
				activeChapter.append("<p class='chr'>");
				activeChapter.append("<span class='verse_number'>");
				activeChapter.append(verse_number);
				activeChapter.append("</span>");
				activeChapter.append(" ");
				activeChapter.append(text);
				activeChapter.append("</p>\n");
			} else {
				System.out.println("WARN NO ENG: " + verse_lookup);
			}
		}
		StringBuilder xhtml = new StringBuilder();
		for (String code : book_order) {
			final BookData bookData = books.get(code);
			final ArrayList<StringBuilder> chapters = bookData.chapters;
			int c = 0;
			TOCReference parentToc = null;
			for (StringBuilder chapter : chapters) {
				if (chapter.length() == 0) {
					continue;
				}
				c++;
				xhtml.setLength(0);

				xhtml.append(Consts.STOCK_HEADER);
				if (c == 1) {
					xhtml.append("<h1>");
					xhtml.append(bookData.cherokee_name);
					xhtml.append("</h1>");
					xhtml.append("<h2>");
					xhtml.append(bookData.english_name);
					xhtml.append("</h2>");
					xhtml.append("<h3>ᎠᏯᏙᎸᎢ ");
					xhtml.append(c);
					xhtml.append("</h3>");
				} else {
					xhtml.append("<h3>");
					xhtml.append("ᎠᏯᏙᎸᎢ ");
					xhtml.append(c);
					xhtml.append("  &mdash; ");
					xhtml.append(bookData.cherokee_name.replace("ᎣᏍᏛ ᎧᏃᎮᏛ, ",
							""));
					xhtml.append("</h3>");
				}
				xhtml.append(chapter);
				xhtml.append(Consts.STOCK_FOOTER);

				String name = String.format("%s-%02d.xhtml", bookData.code, c);
				Resource chapter_text = new Resource(xhtml.toString(),
						Consts.TEXT + name);
				chapter_text.setTitle(bookData.english_name + " " + c);
				if (c == 1) {
					String cname = bookData.cherokee_name;
					if (cname.contains(",")) {
						cname = StringUtils.trim(StringUtils.substringAfter(
								cname, ","));
					}
					parentToc = epub.addSection(bookData.english_name + " - "
							+ bookData.cherokee_name, chapter_text);
				} else {
					epub.addSection(parentToc, "ᎠᏯᏙᎸᎢ " + c, chapter_text);
				}
			}
		}
		save();
	}

	private void save() {
		FileOutputStream fos;
		try {
			EpubWriter writer = new EpubWriter();
			fos = FileUtils.openOutputStream(new File("output/" + bd.filename
					+ ".epub"));
			writer.write(epub, fos);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
