/*
 * Copyright (c) 2016 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mars_sim.javafx.tools;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;
import java.util.Random;

import eu.hansolo.medusa.Clock;
import eu.hansolo.medusa.Clock.ClockSkinType;
import eu.hansolo.medusa.ClockBuilder;
import eu.hansolo.medusa.FGauge;
import eu.hansolo.medusa.FGaugeBuilder;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.GaugeDesign;
import eu.hansolo.medusa.Section;
import eu.hansolo.medusa.events.UpdateEvent;
import eu.hansolo.medusa.events.UpdateEvent.EventType;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;



/**
 * User: hansolo
 * Date: 04.01.16
 * Time: 06:31
 */
public class MedusaTempGauge extends Application {
    private static final Random          RND       = new Random();
    private static       int             noOfNodes = 0;
    private              FGauge          fgauge;
    private              Gauge           gauge;
    private              Clock           clock;
    private              long            lastTimerCall;
    private              AnimationTimer  timer;
    private              DoubleProperty  value;
    private              long            epochSeconds;
    private              BooleanProperty toggle;


    @Override public void init() {
        NumberFormat numberFormat = NumberFormat.getInstance(new Locale("da", "DK"));
        numberFormat.setRoundingMode(RoundingMode.HALF_DOWN);
        numberFormat.setMinimumIntegerDigits(3);
        numberFormat.setMaximumIntegerDigits(3);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(0);

        value  = new SimpleDoubleProperty(0);
        toggle = new SimpleBooleanProperty(false);

        fgauge = FGaugeBuilder.create()
                              .gaugeDesign(GaugeDesign.NONE)
                              .build();


        gauge = GaugeBuilder.create()
                            .skinType(SkinType.TILE_TEXT_KPI)//TILE_KPI)//.KPI)//.GAUGE)
                            //.prefSize(400, 400)
                            .minValue(-50)
                            .maxValue(50)
                            .animated(true)
                            //.checkThreshold(true)
                            //.onThresholdExceeded(e -> System.out.println("threshold exceeded"))
                            //.lcdVisible(true)
                            //.locale(Locale.GERMANY)
                            //.numberFormat(numberFormat)
                            .title("Martian Temperature")
                            .unit("\u00B0C")
                            .subTitle("Alpha Basis")
                            //.interactive(true)
                            //.onButtonPressed(o -> System.out.println("Button pressed"))
                            .sections(new Section(-50, -11, Color.BLUE),
                                      new Section(-10, 27, Color.GREEN),
                                      new Section(28, 37, Color.YELLOW),
                                      new Section(38, 50, Color.RED))
                            .sectionsVisible(true)
                            .highlightSections(true)
                            .autoScale(false)
                            //.averagingEnabled(true)
                            //.averagingPeriod(10)
                            //.averageVisible(true)
                            .build();

        //gauge.setAlert(true);

        // Calling bind() directly sets a value to gauge
        gauge.valueProperty().bind(value);

        gauge.getSections().forEach(section -> section.setOnSectionUpdate(sectionEvent -> gauge.fireUpdateEvent(new UpdateEvent(MedusaTempGauge.this, EventType.REDRAW))));

        //gauge.valueVisibleProperty().bind(toggle);

        epochSeconds = Instant.now().getEpochSecond();

        clock = ClockBuilder.create()
                            .skinType(ClockSkinType.INDUSTRIAL)//TILE)//.
                            .locale(Locale.GERMANY)
                            .shadowsEnabled(true)
                            //.discreteSeconds(false)
                            //.discreteMinutes(false)
                            .running(true)
                            //.backgroundPaint(Color.web("#1f1e23"))
                            //.hourColor(Color.web("#dad9db"))
                            //.minuteColor(Color.web("#dad9db"))
                            //.secondColor(Color.web("#d1222b"))
                            //.hourTickMarkColor(Color.web("#9f9fa1"))
                            //.minuteTickMarkColor(Color.web("#9f9fa1"))
                            .build();

        lastTimerCall = System.nanoTime();
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now > lastTimerCall + 3_000_000_000l) {
                    double v = RND.nextDouble() * gauge.getRange() + gauge.getMinValue();
                    value.set(v);
                    //System.out.println(v);
                    //gauge.setValue(v);

                    //System.out.println("MovingAverage over " + gauge.getAveragingWindow().size() + " values: " + gauge.getAverage() + "  last value = " + v);

                    //toggle.set(!toggle.get());

                    //System.out.println(gauge.isValueVisible());

                    //gauge.setValue(v);

                    //epochSeconds+=20;
                    //clock.setTime(epochSeconds);

                    lastTimerCall = now;
                }
            }
        };
    }

    @Override public void start(Stage stage) {
        StackPane pane = new StackPane(gauge);
        pane.setPadding(new Insets(20));
        pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        LinearGradient gradient = new LinearGradient(0, 0, 0, pane.getLayoutBounds().getHeight(),
                                                     false, CycleMethod.NO_CYCLE,
                                                     new Stop(0.0, Color.rgb(38, 38, 38)),
                                                     new Stop(1.0, Color.rgb(15, 15, 15)));
        //pane.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        //pane.setBackground(new Background(new BackgroundFill(Color.rgb(39,44,50), CornerRadii.EMPTY, Insets.EMPTY)));
        //pane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        //pane.setBackground(new Background(new BackgroundFill(Gauge.DARK_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        Scene scene = new Scene(pane);
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        stage.setTitle("mars-sim");
        stage.setScene(scene);
        stage.show();

        //gauge.setValue(50);

        // Calculate number of nodes
        calcNoOfNodes(pane);
        System.out.println(noOfNodes + " Nodes in SceneGraph");

        timer.start();

        //gauge.getSections().get(0).setStart(10);
        //gauge.getSections().get(0).setStop(90);
    }

    @Override public void stop() {
        System.exit(0);
    }



    // ******************** Misc **********************************************
    private static void calcNoOfNodes(Node node) {
        if (node instanceof Parent) {
            if (((Parent) node).getChildrenUnmodifiable().size() != 0) {
                ObservableList<Node> tempChildren = ((Parent) node).getChildrenUnmodifiable();
                noOfNodes += tempChildren.size();
                for (Node n : tempChildren) { calcNoOfNodes(n); }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
