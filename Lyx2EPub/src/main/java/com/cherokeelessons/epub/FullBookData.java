package com.cherokeelessons.epub;

import java.util.ArrayList;
import java.util.HashMap;

import com.cherokeelessons.converter.Settings;

public class FullBookData {
	public HashMap<String, String> book_mapping=new HashMap<>();
	public ArrayList<String> cherokee_bc=new ArrayList<>();
	public HashMap<String, String> english_bc=new HashMap<>();
	public String filename;
	public Settings settings;
}
