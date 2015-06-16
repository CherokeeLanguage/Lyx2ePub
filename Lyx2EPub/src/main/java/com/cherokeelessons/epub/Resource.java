package com.cherokeelessons.epub;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import nl.siegmann.epublib.domain.MediaType;

@SuppressWarnings("serial")
public class Resource extends nl.siegmann.epublib.domain.Resource {
	public Resource(String body, String href) {
		super(href);
		try {
			this.setData(body.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public Resource(byte[] data, MediaType mediaType) {
		super(data, mediaType);
		
	}

	public Resource(byte[] data, String href) {
		super(data, href);
		
	}

	public Resource(InputStream in, String href) throws IOException {
		super(in, href);
		
	}

	public Resource(Reader in, String href) throws IOException {
		super(in, href);
		
	}

	public Resource(String id, byte[] data, String href, MediaType mediaType,
			String inputEncoding) {
		super(id, data, href, mediaType, inputEncoding);
		
	}

	public Resource(String id, byte[] data, String href, MediaType mediaType) {
		super(id, data, href, mediaType);
		
	}

	public Resource(String fileName, long size, String href) {
		super(fileName, size, href);
		
	}

	public Resource(String href) {
		super(href);
		
	}

	
	

}
