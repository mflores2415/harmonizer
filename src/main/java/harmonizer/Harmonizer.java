package harmonizer;

import java.awt.EventQueue;

import harmonizer.controller.HarmonizerController;

public final class Harmonizer {
	private Harmonizer() {
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> new HarmonizerController().show());
	}
}
