package com.cherokeelessons.converter;

import java.awt.EventQueue;
import java.io.IOException;

import com.cherokeelessons.gui.MainWindow;
import com.cherokeelessons.gui.MainWindow.Config;

public class Main {
	public static void main(String[] args) throws IOException {
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
		EventQueue.invokeLater(new MainWindow(config, args));
	}
}
