package harmonizer;

import harmonizer.controller.HarmonizerController;
import harmonizer.player.MusicPlayer;

import javafx.application.Application;
import javafx.stage.Stage;

public final class HarmonizerApplication extends Application {
	private HarmonizerController controller;

	@Override
	public void start(Stage primaryStage) {
		controller = new HarmonizerController(primaryStage, new MusicPlayer());
		controller.show();
	}

	@Override
	public void stop() {
		if (controller != null) {
			controller.closePlayer();
		}
	}
}
