package com.cherokeelessons.converter;

public class Settings {
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

	public Settings() {
		description = "... description ...";
		ISBN_formatted = "ISBN: 978-0-000-00000-0";

		sourcedir = "./";
		sourcelyx = "input.lyx";
		dest_epub = "epub/ouput.epub";
		dest_epub_kindle = "epub/output-Kindle.epub";

		coverImage = "artwork/ebook-cover-784x1024.png";
		title = "... title ...";
	}
}
