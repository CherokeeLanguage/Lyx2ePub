package com.cherokeelessons.converter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Settings implements Serializable {
	public String description;
	public String ISBN_formatted;

	public String ISBN_EPUB_META() {
		return ISBN_formatted.replaceAll("[^0-9]", "");
	}

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

	public Settings() {
		description = "... description ...";
		ISBN_formatted = "ISBN: 978-0-000-00000-0";

		sourcedir = "./";
		sourcelyx = "input.lyx";
		dest_epub = "epub/ouput.epub";
		dest_epub_kindle = "epub/output-Kindle.epub";

		coverImage = "artwork/ebook-cover-784x1024.png";
		title = "... title ...";

		url = "http://www.CherokeeLessons.com/";
		authors = new ArrayList<String>();
		authors.add("Michael Joyner");
		copyright = "Michael Joyner, All Rights Reserved. (CC BY 3.0 US)";
		subjects = new ArrayList<String>();
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
	}
}
