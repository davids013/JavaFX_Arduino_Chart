package com.example.demofx;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.util.List;

public class HelloController {
    private final byte UPDATE_RATE = 2;
    private String com;
    private SerialPort serialPort;
    private XYChart.Series<Number, Number> series = new XYChart.Series<>();
    @FXML
    private Label welcomeText;
    @FXML
    private Button buttonConnect;
    @FXML
    private ChoiceBox<String> choiceBoxCom;
    @FXML
    private LineChart<Number, Number> chart;

    @FXML
    private void initialize() {
        buttonConnect.setStyle("-fx-background-color: #ff7777; ");
        chart.getXAxis().setAutoRanging(true);
        chart.getYAxis().setAutoRanging(true);
        chart.setCreateSymbols(false);
        chart.getData().add(series);

        final Thread updater = new Thread(null,
                () -> {
                    while (true) {
                        if (serialPort != null && serialPort.isOpened()) {
                            if (!getPortList().contains(serialPort.getPortName())) {
                                try {
                                    serialPort.closePort();
                                } catch (SerialPortException e) {
                                    e.printStackTrace();
                                }
                                onChoiceBoxClick();
                                continue;
                            }
                            Platform.runLater(() -> {
                                buttonConnect.setStyle("-fx-background-color: #33ff33; ");
                                buttonConnect.setText("Disconnect");
                                try {
                                    String input = serialPort.readString();
                                    if (input != null) {
                                        String[] inputArray = input.split("\n");
                                        input = inputArray[inputArray.length - 1];
                                        input = input.trim().replaceAll("[&&[^0-9.]]", "");
                                        System.out.println(input);
                                        String finalInput = input;
                                        try {
                                            final double distance = Double.parseDouble(finalInput);
                                            Platform.runLater(() -> {
                                                welcomeText.setText(finalInput);
                                                series.getData().add(new XYChart.Data(series.getData().size() + 1, distance));
                                                if (series.getData().size() == 1)
                                                    series.getNode().setStyle("-fx-stroke: #33ff33;");
                                            });
                                        } catch (NumberFormatException e) {
                                            System.out.println();
                                        }
                                    }
                                } catch (SerialPortException e) {
                                    e.printStackTrace();
                                }
                            });
                        } else {
                            Platform.runLater(() -> {
                                buttonConnect.setStyle("-fx-background-color: #ff7777; ");
                                buttonConnect.setText("Connect");
                            });
                        }
                        try {
                            Thread.sleep(1000 / UPDATE_RATE);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                },"Updater");
        updater.setDaemon(true);
        updater.start();
    }

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    private List<String> getPortList() {
        return List.of(SerialPortList.getPortNames());
    }

    @FXML
    protected void onChoiceBoxClick() {
        ObservableList<String> ports = FXCollections.observableArrayList();
        ports.clear();
        ports.addAll(getPortList());
        System.out.println("Found COM ports: " + ports.size());
        if (ports.size() > 0) {
            choiceBoxCom.setItems(ports);
            if (ports.contains(com)) choiceBoxCom.setValue(com);
        } else {
            Platform.runLater(() -> {
                choiceBoxCom.setItems(ports);
                choiceBoxCom.setValue("Connection lost");
            });
        }
    }

    @FXML
    protected void onButtonConnectClick() {
        final String comPort = choiceBoxCom.getValue();
        if (buttonConnect.getText().equals("Connect")) {
            if (comPort == null || !comPort.contains("COM")) return;
            serialPort = new SerialPort(comPort);
            com = comPort;
            try {
                serialPort.openPort();
                serialPort.setParams(
                        SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                System.out.println(com + " status: " + serialPort.isOpened());
            } catch (SerialPortException e) {
                e.printStackTrace();
                serialPort = null;
            }
        } else {
            if (serialPort.isOpened()) {
                try {
                    serialPort.closePort();
                } catch (SerialPortException e) {
                    e.printStackTrace();
                    serialPort = null;
                }
            }
        }
        System.out.println(com + " status: " + serialPort.isOpened());
    }
}