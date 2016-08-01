package com.cherokeelessons.converter;

import com.cherokeelessons.gui.MainWindow;
import com.cherokeelessons.gui.MainWindow.Config;

public class Main {

	public static void main(String[] args) {
		MainWindow.Config config = new Config() {
			@Override
			public String getApptitle() {
				return "Lyx2ePub";
			}
			
			@Override
			public Runnable getApp(String... args) throws Exception {
				return new MainApp(args);
			}
		};
	}

}
