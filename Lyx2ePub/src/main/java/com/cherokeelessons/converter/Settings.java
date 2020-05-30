package com.cherokeelessons.converter;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({ "serial", "javadoc", "unqualified-field-access" })
public class Settings implements Serializable {
	public String description;
	public String ISBN_formatted;

	public String ISBN_EPUB_META() {
		return ISBN_formatted.replaceAll("[^0-9]", "");
	}

	public String destdir;
	public String sourcedir;
	public String sourcelyx;
	public String dest_epub;
	public String dest_epub_kindle;

	public String coverImage;
	public String title;
	public String url;
	public List<String> authors;
	
	public String copyright;
	public List<String> subjects;
	public boolean svg_mode;
	public String image_tmp;

	@JsonProperty("epub_css")
	private String epubCss;
	public String getEpubCss() {
		return epubCss;
	}

	public void setEpubCss(String epubCss) {
		this.epubCss = epubCss;
	}

	public String getKindleCss() {
		return kindleCss;
	}

	public void setKindleCss(String kindleCss) {
		this.kindleCss = kindleCss;
	}

	public boolean isEmbed_free_serif() {
		return embed_free_serif;
	}

	public void setEmbed_free_serif(boolean embed_free_serif) {
		this.embed_free_serif = embed_free_serif;
	}

	@JsonProperty("kindle_css")
	private String kindleCss;
	private boolean embed_free_serif;

	public Settings() {
		description = "... description ...";
		ISBN_formatted = "ISBN: 978-0-000-00000-0";

		sourcedir = "./";
		sourcelyx = "input.lyx";
		dest_epub = "epub/ouput.epub";
		dest_epub_kindle = "epub/output-Kindle.epub";
		
		image_tmp = new File(new File(dest_epub).getParent(),"image-tmp").getPath();

		coverImage = "artwork/ebook-cover-784x1024.png";
		title = "... title ...";

		url = "http://www.CherokeeLessons.com/";
		authors = new ArrayList<>();
		authors.add("Michael Joyner");
		copyright = "Michael Joyner, All Rights Reserved. (CC BY 3.0 US)";
		subjects = new ArrayList<>();
		subjects.add("FOR031000");
		subjects.add("Foreign Language Study, Native American Languages");
		subjects.add("LAN012000");
		subjects.add("Language Arts and Disciplines, Readers");
		subjects.add("LAN021000");
		subjects.add("Language Arts and Disciplines, Vocabulary");
		subjects.add("FIC000000");
		subjects.add("Fiction, General");
		subjects.add("Native American Languages");
		subjects.add("Cherokee");
		subjects.add("Cherokee Language");
		subjects.add("ᏣᎳᎩ");
		subjects.add("ᎦᏬᏂᎯᏍᏗ");
		subjects.add("ᎠᎪᎵᏰᏗ");
		subjects.add("ᏧᏬᎵᏗ ᏗᎪᏪᎵ");
		subjects.add("ᎤᏬᎵᏗ ᎪᏪᎵ");
		subjects.add("ᏗᎪᏪᎵ");
		
		svg_mode=false;
		
		epubCss = "epub-default.css";
		kindleCss = "epub-kindle.css";
		
		embed_free_serif = true;
	}
}
