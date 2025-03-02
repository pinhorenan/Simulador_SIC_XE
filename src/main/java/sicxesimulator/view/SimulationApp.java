package sicxesimulator.view;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.css.Size;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import sicxesimulator.controller.SimulationController;
import sicxesimulator.model.SimulationModel;
import sicxesimulator.machine.Machine;
import sicxesimulator.assembler.Assembler;
import sicxesimulator.loader.Loader;
import sicxesimulator.machine.cpu.Register;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class SimulationApp extends Application {
    private SimulationController controller;

    private Stage primaryStage;
    private TextArea outputArea;
    private TextArea inputField;
    private TableView<RegisterEntry> registerTable;
    private TableView<MemoryEntry> memoryTable;
    private TableView<SymbolEntry> symbolTable;

    // Records auxiliares para as tabelas
    public record RegisterEntry(String name, String value) { }
    public record MemoryEntry(String address, String value) { }
    public record SymbolEntry(String symbol, String address) { }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Inicialização do modelo e controlador
        Machine virtualMachine = new Machine();
        SimulationModel model = new SimulationModel(
                virtualMachine,
                new Assembler(),
                new Loader()
        );
        controller = new SimulationController(model, this);

        primaryStage.setTitle("SIC/XE Simulator v2.1");
        primaryStage.setWidth(900);
        primaryStage.setHeight(600);
        primaryStage.getIcons().add(new Image("https://img.icons8.com/?size=100&id=rd2k11wyt7We&format=png&color=000000"));

        BorderPane root = new BorderPane();
        root.setTop(createMenuBar());

        // Coluna Esquerda: entrada, botões de ação e saída
        VBox leftColumn = new VBox(10);
        leftColumn.setPadding(new Insets(10));

        TitledPane inputPane = new TitledPane("Código Assembly", createInputArea());
        inputPane.setCollapsible(false);

        HBox actionButtons = new HBox(10);
        actionButtons.setPadding(new Insets(5));

        Button assembleButton = new Button("Montar");
        assembleButton.setOnAction(e -> {
            String inputText = inputField.getText();
            if (!inputText.trim().isEmpty()) {
                List<String> sourceLines = Arrays.asList(inputText.split("\\r?\\n"));
                controller.handleAssembleAction(sourceLines);
            }
        });

        Button showObjectCodeButton = new Button("Mostrar Código Objeto");
        showObjectCodeButton.setOnAction(e -> controller.handleShowObjectCodeAction());

        Button runButton = new Button("Executar");
        runButton.setOnAction(e -> controller.handleRunAction());

        Button nextButton = new Button("Próximo");
        nextButton.setOnAction(e -> controller.handleNextAction());

        Button resetButton = new Button("Resetar");
        resetButton.setOnAction(e -> controller.handleResetAction());

        actionButtons.getChildren().addAll(assembleButton, runButton, showObjectCodeButton, nextButton, resetButton);

        TitledPane outputPane = new TitledPane("Saída", createOutputAreaPane());
        outputPane.setCollapsible(false);

        leftColumn.getChildren().addAll(inputPane, actionButtons, outputPane);
        VBox.setVgrow(inputPane, Priority.ALWAYS);
        VBox.setVgrow(outputPane, Priority.ALWAYS);

        // Coluna Direita: tabelas de memória, registradores e símbolos
        VBox rightColumn = new VBox(10);
        rightColumn.setPadding(new Insets(10));
        TitledPane memoryPane = new TitledPane("Memória", createMemoryTablePane());
        memoryPane.setCollapsible(false);
        TitledPane registersPane = new TitledPane("Registradores", createRegisterTablePane());
        registersPane.setCollapsible(false);
        TitledPane symbolsPane = new TitledPane("Símbolos", createSymbolTablePane());
        symbolsPane.setCollapsible(false);
        rightColumn.getChildren().addAll(memoryPane, registersPane, symbolsPane);
        VBox.setVgrow(memoryPane, Priority.ALWAYS);
        VBox.setVgrow(registersPane, Priority.ALWAYS);
        VBox.setVgrow(symbolsPane, Priority.ALWAYS);

        HBox mainContent = new HBox(10, leftColumn, rightColumn);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        root.setCenter(mainContent);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        updateAllTables();
        showWelcomeMessage();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // Menu "Arquivo"
        Menu fileMenu = new Menu("Arquivo");

        MenuItem importAssemblyFile = new MenuItem("Importar .asm");
        importAssemblyFile.setOnAction(e -> System.out.println("Importar .asm acionado"));

        MenuItem loadExampleASM = new MenuItem("Carregar código exemplo");
        loadExampleASM.setOnAction(e -> loadExampleCode());

        fileMenu.getItems().addAll(importAssemblyFile, loadExampleASM);

        // Menu "Configurações"
        Menu optionsMenu = new Menu("Opções");

        MenuItem memorySizeItem = new MenuItem("Tamanho da memória");
        memorySizeItem.setOnAction(e -> {}); // TODO: implementar configurações

        MenuItem executionSpeedItem = new MenuItem("Velocidade de execução");
        executionSpeedItem.setOnAction(e -> {}); // TODO: implementar configurações

        optionsMenu.getItems().addAll(memorySizeItem, executionSpeedItem);

        // Menu "Exibição"
        Menu viewMenu = new Menu("Exibição");

        MenuItem hexadecimalView = new MenuItem("Hexadecimal");
        hexadecimalView.setOnAction(e -> {}); // TODO: implementação da exibição hexadecimal

        MenuItem octalView = new MenuItem("Octal");
        octalView.setOnAction(e -> {}); // TODO: implementação da exibição octal

        MenuItem decimalView = new MenuItem("Decimal");
        decimalView.setOnAction(e -> {}); // TODO: implementação da exibição decimal

        viewMenu.getItems().addAll(hexadecimalView, octalView, decimalView);

        // Menu "Ajuda"
        Menu helpMenu = new Menu("Ajuda");
        helpMenu.setOnAction(e -> {}); // TODO: Abrir janela mostrando funcionalidades suportadas, comandos e tutorial.

        // Menu "Sobre"
        Menu aboutMenu = new Menu("Sobre");

        MenuItem repository = new MenuItem("Repositório");
        repository.setOnAction(e -> getHostServices().showDocument("https://github.com/pinhorenan/SIC-XE-Simulator"));

        MenuItem info = new MenuItem("Informações");
        info.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sobre");
            alert.setHeaderText("SIC/XE Simulator v2.1");
            alert.setContentText("""
                    © 2025 SIC/XEd
                    Time ROCK LEE VS GAARA
                    Ícone: https://icons8.com/icon/NAL2lztANaO6/rust""");
            alert.showAndWait();
        });

        aboutMenu.getItems().addAll(info, repository);

        // Menu "Créditos"
        Menu creditsMenu = new Menu("Créditos");
        MenuItem renanPinho = new MenuItem("Renan Pinho");
        renanPinho.setOnAction(e -> getHostServices().showDocument("https://github.com/pinhorenan"));
        MenuItem luisRasch = new MenuItem("Luis Rasch");
        luisRasch.setOnAction(e -> getHostServices().showDocument("https://github.com/LuisEduardoRasch"));
        MenuItem gabrielMoura = new MenuItem("Gabriel Moura");
        gabrielMoura.setOnAction(e -> getHostServices().showDocument("https://github.com/gbrimoura"));
        MenuItem fabricioBartz = new MenuItem("Fabricio Bartz");
        fabricioBartz.setOnAction(e -> getHostServices().showDocument("https://github.com/FabricioBartz"));
        MenuItem arthurAlves = new MenuItem("Arthur Alves");
        arthurAlves.setOnAction(e -> getHostServices().showDocument("https://github.com/arthursa21"));
        MenuItem leonardoBraga = new MenuItem("Leonardo Braga");
        leonardoBraga.setOnAction(e -> getHostServices().showDocument("https://github.com/braga0425"));
        creditsMenu.getItems().addAll(renanPinho, luisRasch, gabrielMoura, arthurAlves, fabricioBartz, leonardoBraga);

        menuBar.getMenus().addAll(fileMenu, optionsMenu, viewMenu, helpMenu, aboutMenu, creditsMenu);
        return menuBar;
    }

    private ScrollPane createInputArea() {
        inputField = new TextArea();
        inputField.setPromptText("Insira seu código assembly aqui...");
        inputField.setWrapText(true);
        inputField.setStyle("-fx-font-family: Consolas; -fx-font-size: 14;");
        ScrollPane inputScroll = new ScrollPane(inputField);
        inputScroll.setFitToWidth(true);
        inputScroll.setFitToHeight(true);
        return inputScroll;
    }

    private ScrollPane createOutputAreaPane() {
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle("-fx-font-family: monospace; -fx-text-fill: #006400; -fx-font-size: 14;");
        ScrollPane outputScroll = new ScrollPane(outputArea);
        outputScroll.setFitToWidth(true);
        outputScroll.setFitToHeight(true);
        return outputScroll;
    }

    private ScrollPane createRegisterTablePane() {
        setupRegisterTable();
        ScrollPane registerScroll = new ScrollPane(registerTable);
        registerScroll.setFitToWidth(true);
        return registerScroll;
    }

    private ScrollPane createMemoryTablePane() {
        setupMemoryTable();
        ScrollPane memoryScroll = new ScrollPane(memoryTable);
        memoryScroll.setFitToWidth(true);
        return memoryScroll;
    }

    private ScrollPane createSymbolTablePane() {
        setupSymbolTable();
        ScrollPane symbolScroll = new ScrollPane(symbolTable);
        symbolScroll.setFitToWidth(true);
        return symbolScroll;
    }

    private void setupRegisterTable() {
        registerTable = new TableView<>();
        TableColumn<RegisterEntry, String> nameCol = new TableColumn<>("Registrador");
        nameCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().name()));
        TableColumn<RegisterEntry, String> valueCol = new TableColumn<>("Valor");
        valueCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().value()));
        registerTable.getColumns().addAll(nameCol, valueCol);
        registerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupMemoryTable() {
        memoryTable = new TableView<>();
        TableColumn<MemoryEntry, String> addressCol = new TableColumn<>("Endereço");
        addressCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().address()));
        TableColumn<MemoryEntry, String> valueCol = new TableColumn<>("Valor");
        valueCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().value()));
        memoryTable.getColumns().addAll(addressCol, valueCol);
        memoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupSymbolTable() {
        symbolTable = new TableView<>();
        TableColumn<SymbolEntry, String> symbolCol = new TableColumn<>("Símbolo");
        symbolCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().symbol()));
        TableColumn<SymbolEntry, String> addressCol = new TableColumn<>("Endereço");
        addressCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().address()));
        symbolTable.getColumns().addAll(symbolCol, addressCol);
        symbolTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void updateRegisterTable() {
        registerTable.getItems().clear();
        for (Register reg : controller.getSimulationModel().getMachine().getControlUnit().getCurrentRegisters()) {
            registerTable.getItems().add(new RegisterEntry(reg.getName(), reg.getHexValue()));
        }
    }

    public void updateMemoryTable() {
        memoryTable.getItems().clear();
        for (int address = 0; address < controller.getSimulationModel().getMachine().getMemoryState().getSize(); address++) {
            int byteValue = controller.getSimulationModel().getMachine().getMemoryState().readByte(address);
            String value = String.format("%02X", byteValue);
            memoryTable.getItems().add(new MemoryEntry(String.format("%04X", address), value));
        }
    }

    public void updateSymbolTable() {
        symbolTable.getItems().clear();
        Map<String, Integer> symbols = controller.getSimulationModel().getAssembler().getSymbolTable();
        symbols.forEach((name, address) ->
                symbolTable.getItems().add(new SymbolEntry(name, String.format("%04X", address)))
        );
    }

    public void updateAllTables() {
        updateRegisterTable();
        updateMemoryTable();
        updateSymbolTable();
    }

    private void loadExampleCode() {
        // Código de exemplo
        String exampleCode =
                "COPY START 1000\n" +
                        "FIRST  LDA   FIVE\n" +
                        "       ADD   FOUR\n" +
                        "       STA   RESULT\n" +
                        "       RSUB\n" +
                        "FIVE   WORD  5\n" +
                        "FOUR   WORD  4\n" +
                        "RESULT RESW  1";

        // Coloca o código exemplo no campo de entrada
        inputField.setText(exampleCode);

        // Atualiza o título da janela (opcional)
        primaryStage.setTitle("Simulador SIC/XE - Exemplo Carregado");

        // Exibe uma mensagem (opcional)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Código de Exemplo");
        alert.setHeaderText("Código Assembly de Exemplo Carregado");
        alert.setContentText("O código de exemplo foi carregado no campo de entrada.");
        alert.showAndWait();
    }

    public void appendOutput(String message) {
        Platform.runLater(() -> outputArea.appendText("> " + message + "\n"));
    }

    private void showWelcomeMessage() {
        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
        pause.setOnFinished(e -> {
            outputArea.appendText("╔══════════════════════════════════════╗\n");
            outputArea.appendText("║      SIC/XE Simulator v2.1           ║\n");
            outputArea.appendText("║      © 2025 SIC/XE Rock Lee vs Gaara ║\n");
            outputArea.appendText("╚══════════════════════════════════════╝\n\n");
            outputArea.appendText("Instruções:\n");
            outputArea.appendText("1. Digite o código assembly na área de edição\n");
            outputArea.appendText("2. Utilize os botões abaixo para Montar, Executar, Próximo ou Reiniciar\n");
            outputArea.appendText("3. Utilize os menus para importar/exportar, configurar, obter ajuda ou visualizar informações\n");
            outputArea.appendText("4. Visualize registradores, memória e símbolos em tempo real\n\n");
        });
        pause.play();
    }


    public void showError(String errorMessage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro de Simulação");
            alert.setHeaderText("Ocorreu um erro durante a execução");
            alert.setContentText(errorMessage);
            alert.showAndWait();
        });
    }

    ///  GETTERS

    public Stage getStage() {
        return primaryStage;
    }

    public TextArea getOutputArea() {
        return outputArea;
    }

    public TextArea getInputField() {
        return inputField;
    }

    public TableView<RegisterEntry> getRegisterTable() {
        return registerTable;
    }

    public TableView<MemoryEntry> getMemoryTable() {
        return memoryTable;
    }

    public TableView<SymbolEntry> getSymbolTable() {
        return symbolTable;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
