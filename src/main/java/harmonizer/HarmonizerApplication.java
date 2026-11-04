package harmonizer;

import harmonizer.controller.HarmonizerController;

import javafx.application.Application;
import javafx.stage.Stage;

public final class HarmonizerApplication extends Application {
	private HarmonizerController controller;

	@Override
	public void start(Stage primaryStage) {
		controller = new HarmonizerController(primaryStage);
		controller.show();
	}

	@Override
	public void stop() {
		if (controller != null) {
			controller.closePlayer();
		}
	}
}
