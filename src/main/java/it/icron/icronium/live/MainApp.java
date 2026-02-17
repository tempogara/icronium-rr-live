package it.icron.icronium.live;

import java.awt.Desktop;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainApp extends Application {

	static {
		System.setProperty("https.protocols", "TLSv1.3,TLSv1.2");
	}

	// =================================================
	// CONFIG
	// =================================================

	public static final Path WORK_DIR = Paths.get(System.getProperty("user.home"), "icronium-live-work");

	private static final int LIVE_INTERVAL = 10; // sec

	// =================================================
	// STATE
	// =================================================

	private final ObservableList<IpRow> rows = FXCollections.observableArrayList();

	private final ExecutorService pool = Executors.newFixedThreadPool(4);

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

	private TableView<IpRow> table;

	TextField rrIdField;

	private ConnectorContext connectorContext;

	// =================================================
	// START
	// =================================================

	@Override
	public void start(Stage stage) throws Exception {

		Files.createDirectories(WORK_DIR);

		stage.setTitle("ICRONIUM LIVE");

		// =================================================
		// TOOLBAR
		// =================================================

		rrIdField = new TextField();
		rrIdField.setPromptText("");
		rrIdField.setPrefWidth(400);

		Button connectBtn = new Button("SYNC");
		Button addLiveBtn = new Button("+ Api");
		Button addLiveHomeBtn = new Button("ICRONLive");

		ToolBar bar = new ToolBar(
				new Label("RR Link:"), rrIdField, connectBtn,
				new Separator(), addLiveBtn, new Separator(), addLiveHomeBtn);

		addLiveBtn.setOnAction(e -> showAddLiveDialog(stage));

		addLiveHomeBtn.setOnAction(e -> {
			try {

				if (!Desktop.isDesktopSupported()) {
					System.err.println("Desktop non supportato");
					return;
				}

				Desktop desktop = Desktop.getDesktop();

				if (!desktop.isSupported(Desktop.Action.BROWSE)) {
					System.err.println("Browse non supportato");
					return;
				}

				desktop.browse(new URI("https://www.icron.it/live/event/" + connectorContext.getRrId()));

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		);

		connectBtn.setOnAction(e -> {
			try {
				connectorContext = RRConnectorService.connect(rrIdField.getText());
				StatusBus.set("Configurazione caricata");

				List<IpRow> _rows = mapToRows(connectorContext.getApis());

				// key già presenti
				java.util.Set<String> existingKeys = rows.stream()
						.map(IpRow::getUrl) // <-- assicurati che IpRow abbia getKey()
						.filter(java.util.Objects::nonNull)
						.collect(java.util.stream.Collectors.toSet());

				// aggiungi solo nuove
				_rows.stream()
						.filter(r -> r.getUrl() != null && !existingKeys.contains(r.getUrl()))
						.forEach(rows::add);

				table.refresh();

			} catch (Exception ex) {
				new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
			}
		});

		// =================================================
		// TABLE
		// =================================================

		table = new TableView<>(rows);

		table.setColumnResizePolicy(
				TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

		// Nome
		TableColumn<IpRow, String> nameCol = new TableColumn<>("Nome");

		nameCol.setCellValueFactory(d -> d.getValue().nameProperty());

		// URL
		TableColumn<IpRow, String> urlCol = new TableColumn<>("URL");

		urlCol.setCellValueFactory(d -> d.getValue().urlProperty());

		// Stato
		TableColumn<IpRow, String> statusCol = new TableColumn<>("Stato");

		statusCol.setCellValueFactory(d -> d.getValue().statusProperty());

		// Ultimo update
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

		TableColumn<IpRow, LocalDateTime> lastCol = new TableColumn<>("Ultimo update");

		lastCol.setCellValueFactory(d -> d.getValue().lastUpdateProperty());

		lastCol.setCellFactory(col -> new TableCell<>() {

			@Override
			protected void updateItem(
					LocalDateTime item,
					boolean empty) {

				super.updateItem(item, empty);

				setText(
						empty || item == null
								? ""
								: fmt.format(item));
			}
		});

		// Bytes
		TableColumn<IpRow, Number> sizeCol = new TableColumn<>("Bytes");

		sizeCol.setCellValueFactory(d -> d.getValue().receivedBytesProperty());

		// Progresso
		TableColumn<IpRow, Double> progCol = new TableColumn<>("Progresso");

		progCol.setCellValueFactory(d -> d.getValue().progressProperty().asObject());

		progCol.setCellFactory(
				ProgressBarTableCell.forTableColumn());

		// =================================================
		// ACTIONS
		// =================================================

		TableColumn<IpRow, Void> actionCol = new TableColumn<>("Azioni");

		actionCol.setMinWidth(150);

		actionCol.setCellFactory(col -> new TableCell<>() {

			private final Button play = new Button("▶");

			private final Button stop = new Button("⏹");

			private final Button reset = new Button("🔄");

			private final Button del = new Button("🗑");

			private final HBox box = new HBox(4, play, stop, reset, del);

			{

				box.setAlignment(Pos.CENTER);

				play.setOnAction(e -> {

					IpRow r = getRow();

					r.start();

					try {
						startLive(r);
						r.statusProperty().set("Attivo");
						refreshButtons(r);
					} catch (Exception e1) {
						AppLogger.log(
								"LIVE error (" +
										r.nameProperty().get() + "): " + e1.getMessage()
						);
						new Alert(Alert.AlertType.ERROR, "Codice RR Id assente. Ettettuare la sync").showAndWait();

						return;
					}

				});

				stop.setOnAction(e -> {

					IpRow r = getRow();

					r.stop();

					stopLive(r);

					r.statusProperty()
							.set("Fermato");

					refreshButtons(r);
				});

				reset.setOnAction(e -> {

					IpRow r = getRow();

					r.reset();

					stopLive(r);

					refreshButtons(r);
				});

				del.setOnAction(e -> {

					IpRow r = getRow();

					Alert a = new Alert(
							Alert.AlertType.CONFIRMATION,
							"Eliminare riga?",
							ButtonType.OK,
							ButtonType.CANCEL);

					a.showAndWait()
							.ifPresent(res -> {

								if (res == ButtonType.OK) {

									stopLive(r);

									rows.remove(r);
								}
							});
				});
			}

			private IpRow getRow() {

				return getTableView()
						.getItems()
						.get(getIndex());
			}

			private void refreshButtons(IpRow r) {

				boolean on = r.isActive();

				play.setDisable(on);
				stop.setDisable(!on);
				reset.setDisable(on);
			}
			
			private void disableButtons(IpRow r) {
				play.setDisable(true);
				stop.setDisable(true);
				reset.setDisable(true);
			}

			@Override
			protected void updateItem(
					Void item,
					boolean empty) {

				super.updateItem(item, empty);

				if (empty) {
					setGraphic(null);
				} else {
					
					IpRow r = getRow();
					if (connectorContext == null) {
						disableButtons(r);
					}	else {
						refreshButtons(r);
					}


					setGraphic(box);
				}
			}
		});

		table.getColumns().addAll(
				actionCol,
				nameCol,
				urlCol,
				statusCol,
				lastCol,
				sizeCol,
				progCol);

		AppConfigService.LoadedConfig cfg = AppConfigService.load();
		rows.addAll(cfg.rows);
		StatusBus.set("Configurazione caricata (" + rows.size() + ")");

		// =================================================
		// STATUS BAR
		// =================================================

		Label statusBar = new Label();

		statusBar.textProperty()
				.bind(StatusBus.statusProperty());

		statusBar.setMaxWidth(Double.MAX_VALUE);

		statusBar.setStyle(
				"-fx-padding:6 10;" +
						"-fx-border-color:#ccc;" +
						"-fx-border-width:1 0 0 0;" +
						"-fx-background-color:#f6f6f6;");

		// =================================================
		// ROOT
		// =================================================

		BorderPane root = new BorderPane(table);

		root.setTop(bar);
		root.setBottom(statusBar);
		root.setPadding(new Insets(8));

		stage.setScene(
				new Scene(root, 1100, 450));

		stage.show();

		StatusBus.set("Pronto");
	}

	// =================================================
	// LIVE CONTROL
	// =================================================

	private void startLive(IpRow row) throws Exception {

		if (row.getFuture() != null)
			return;


		ScheduledFuture<?> f = scheduler.scheduleAtFixedRate(
				new LiveTask(row, connectorContext.getRrId()),
				0,
				LIVE_INTERVAL,
				TimeUnit.SECONDS);

		row.setFuture(f);

		AppLogger.log("LIVE START: " +
				row.nameProperty().get());
	}

	private void stopLive(IpRow row) {

		row.clearFuture();

		try {
			LiveUploader.resetCache(row, connectorContext.getRrId());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		AppLogger.log("LIVE STOP: " + row.nameProperty().get());

	}

	// =================================================
	// POPUP ADD LIVE
	// =================================================

	private void showAddLiveDialog(Stage owner) {

		Stage dlg = new Stage();

		dlg.initOwner(owner);
		dlg.initModality(Modality.APPLICATION_MODAL);

		dlg.setTitle("Aggiungi LIVE");

		TextField name = new TextField();
		name.setPromptText("Nome");

		TextField url = new TextField();
		url.setPromptText("URL");

		Button ok = new Button("OK");
		Button cancel = new Button("Annulla");

		ok.setDisable(true);

		Runnable validate = () -> ok.setDisable(
				name.getText().isBlank() ||
						url.getText().isBlank());

		name.textProperty()
				.addListener((o, a, b) -> validate.run());

		url.textProperty()
				.addListener((o, a, b) -> validate.run());

		ok.setOnAction(e -> {

			IpRow r = new IpRow(
					name.getText().trim(),
					url.getText().trim());

			rows.add(r);

			StatusBus.set(
					"LIVE aggiunto: " +
							r.nameProperty().get());

			dlg.close();
		});

		cancel.setOnAction(e -> dlg.close());

		VBox box = new VBox(
				10,
				new Label("Nome"), name,
				new Label("URL"), url,
				new HBox(8, ok, cancel));

		box.setPadding(new Insets(12));

		dlg.setScene(new Scene(box, 420, 220));

		dlg.showAndWait();
	}

	// =================================================
	// STOP
	// =================================================

	@Override
	public void stop() {

		for (IpRow r : rows) {
			r.clearFuture();
		}

		scheduler.shutdownNow();
		pool.shutdownNow();

		AppConfigService.save(rows);

		AppLogger.log("APP STOP");
	}

	// =================================================
	// UI SAFE
	// =================================================

	public List<IpRow> mapToRows(
			List<RREndpoint> endpoints) {

		List<IpRow> rows = new ArrayList<>();

		for (RREndpoint e : endpoints) {

			// salta disabilitati
			if (e.disabled)
				continue;

			if (!e.url.startsWith("lists/create")) {
				continue;
			}

			// nome da Label (fallback su Key)
			String name = e.label;

			if (name == null || name.isBlank()) {
				name = e.key;
			}

			// url completo
			String url = connectorContext.getBaseUrl() + e.key;

			IpRow row = new IpRow(name, url);

			row.statusProperty().set("Pronto");
			row.activeProperty().set(false);

			rows.add(row);
		}

		return rows;
	}

	public static void safeUi(Runnable r) {

		if (Platform.isFxApplicationThread())
			r.run();
		else
			Platform.runLater(r);
	}

	// =================================================
	// MAIN
	// =================================================

	public static void main(String[] args) {
		launch(args);
	}
}
