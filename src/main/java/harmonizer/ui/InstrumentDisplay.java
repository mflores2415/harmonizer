package harmonizer.ui;

import java.util.List;

import harmonizer.model.InstrumentPart;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class InstrumentDisplay {
	private final VBox view = new VBox(10);
	private final TableView<InstrumentRow> table = new TableView<>();

	public InstrumentDisplay() {
		Label heading = new Label("INSTRUMENTS PLAYING");
		heading.getStyleClass().add("section-title");

		TableColumn<InstrumentRow, String> instrumentColumn = new TableColumn<>("Instrument / part");
		instrumentColumn.setCellValueFactory(
				cell -> new ReadOnlyStringWrapper(cell.getValue().instrument()));
		TableColumn<InstrumentRow, String> notesColumn = new TableColumn<>("Notes at playhead");
		notesColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().notes()));
		table.getColumns().add(instrumentColumn);
		table.getColumns().add(notesColumn);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setPlaceholder(new Label("No instrument note events found."));
		table.setItems(FXCollections.observableArrayList());
		VBox.setVgrow(table, Priority.ALWAYS);

		view.getStyleClass().add("instrument-display");
		view.getChildren().addAll(heading, table);
	}

	public Node getView() {
		return view;
	}

	public void update(List<InstrumentPart> parts, long tick) {
		table.getItems().setAll(parts.stream()
				.map(part -> new InstrumentRow(
						part.getName(),
						part.getNotesAt(tick).stream()
								.map(InstrumentPart::formatNote)
								.collect(java.util.stream.Collectors.joining(", "))))
				.toList());
	}

	private record InstrumentRow(String instrument, String notes) {
	}
}
