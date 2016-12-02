/**
 * Mars Simulation Project
 * TabPanelPersonality.java
 * @version 3.1.0 2016-11-05
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.unit_window.person;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
//import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonalityTraitType;
import org.mars_sim.msp.core.person.ai.PersonalityType;
import org.mars_sim.msp.ui.javafx.QualityGauge;
import org.mars_sim.msp.ui.steelseries.gauges.Radial2Top;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.Conversion;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

import com.jfoenix.controls.JFXTextField;

import eu.hansolo.medusa.Section;
import eu.hansolo.medusa.skins.BulletChartSkin;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;

import javafx.scene.control.ContentDisplay;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.event.EventHandler;
import javafx.scene.text.Font;
import javafx.scene.control.TextArea;
import javafx.scene.text.FontWeight;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
/**
 * The TabPanelPersonality is a tab panel about the personality, emotional state and mood of a person.
 */
public class TabPanelPersonality
extends TabPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private JFXPanel jfxpanel;
	private Scene scene;
	private StackPane stack;
	private Person person;
	
	/**
	 * Constructor.
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	@SuppressWarnings("restriction")
	public TabPanelPersonality(Unit unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelPersonality.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelPersonality.tooltip"), //$NON-NLS-1$
			unit, desktop
		);

		this.person = (Person) unit;

		//createSwingGUI();
		
		jfxpanel = new JFXPanel();

		int width = 400;
		int height = 700;

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				stack = new StackPane();
				stack.setStyle("-fx-border-style: 2px; " + "-fx-background-color: #c1bf9d;"
						+ "-fx-border-color: #c1bf9d;" + "-fx-background-radius: 2px;");

				scene = new Scene(stack, width, height);
				scene.setFill(Color.TRANSPARENT);// .BLACK);
				jfxpanel.setScene(scene);

				Label title = new Label(Msg.getString("TabPanelPersonality.title"));
				//Reflection reflection = new Reflection();
				//title.setEffect(reflection);
				//reflection.setTopOffset(0.0);
				title.setAlignment(Pos.TOP_CENTER);
				title.setContentDisplay(ContentDisplay.TOP);
				title.setPadding(new Insets(5, 5, 5, 5));
				title.setFont(Font.font("Cambria", FontWeight.BOLD, 16));

				VBox topBox = new VBox();
				//vBox0.setStyle("-fx-border-style: 2px; " + "-fx-background-color: #c1bf9d;"
				//		+ "-fx-border-color: #c1bf9d;" + "-fx-background-radius: 2px;");
				topBox.setAlignment(Pos.TOP_CENTER);

				HBox hBox0 = new HBox();
				hBox0.setAlignment(Pos.TOP_LEFT);
				hBox0.setPadding(new Insets(5, 5, 5, 5));
				
				// Prepare personality name label
				Label label0 = new Label();
				label0.setPadding(new Insets(5, 0, 0, 0));
				label0.setFont(Font.font("Cambria", FontWeight.NORMAL, 16));
				//label0.setPrefSize(150, 20);
				//label0.setAlignment(Pos.BOTTOM_RIGHT);
				label0.setText(Msg.getString("TabPanelPersonality.MBTI") + " "); //$NON-NLS-1$
				Tooltip i0 = new Tooltip("Myers-Briggs Type Indicator (MBTI)");
				label0.setTooltip(i0);
				setQuickToolTip(label0, i0);
				
				// Prepare personality label
				String personality = person.getMind().getMBTI().getTypeString();
				//JLabel personalityLabel = new JLabel(personality, JLabel.RIGHT);
				JFXTextField tf0 = new JFXTextField(personality);
				tf0.setFont(Font.font("Cambria", FontWeight.NORMAL, 16));
				//tf0.setAlignment(Pos.TOP_LEFT);
				tf0.setEditable(false);
				tf0.setPrefSize(70, 20);
				
				String type1 = personality.substring(0,1);
				if (type1.equals("E"))
					type1 = "Extrovert (E)";
				else
					type1 = "Introvert (I)";

				String type2 = personality.substring(1,2);
				if (type2.equals("N"))
					type2 = "Intuitive (N)";
				else
					type2 = "Sensing (S)";

				String type3 = personality.substring(2,3);
				if (type3.equals("F"))
					type3 = "Feeler (F)";
				else
					type3 = "Thinker (T)";

				String type4 = personality.substring(3,4);
				if (type4.equals("J"))
					type4 = "Judger (J)";
				else
					type4 = "Perceiver (P)";

				Tooltip i1 = new Tooltip(type1  + System.lineSeparator()
										+ type2  + System.lineSeparator() 
										+ type3  + System.lineSeparator()
										+ type4 );
				
				tf0.setTooltip(i1);
				setQuickToolTip(tf0, i1);

				VBox chartBox = new VBox();
				chartBox.setAlignment(Pos.TOP_LEFT);
				chartBox.setPadding(new Insets(5, 5, 15, 5));
				
				// Build gauges for MBTI			
				VBox barBox = new VBox();
				//barBox.setStyle("-fx-border-style: 2px; " + "-fx-background-color: #c1bf9d;"
				//		+ "-fx-border-color: #c1bf9d;" + "-fx-background-radius: 2px;");
				//barBox.setAlignment(Pos.TOP_LEFT);
				
				List<Gauge> bars = new ArrayList<Gauge>();
				for (int i=0; i< 4; i++) {
						Gauge bar = GaugeBuilder.create()
		                        .skinType(SkinType.BULLET_CHART)
		                        .sections(new Section(0, 16.66666, "0", Color.web("#11632f")),
		                                  new Section(16.66666, 33.33333, "1", Color.web("#36843d")),
		                                  new Section(33.33333, 50.0, "2", Color.web("#67a328")),
		                                  new Section(50.0, 66.66666, "3", Color.web("#80b940")),
		                                  new Section(66.66666, 83.33333, "4", Color.web("#95c262")),
		                                  new Section(83.33333, 100.0, "5", Color.web("#badf8d")))
		                        .threshold(50)
		                        .titleColor(Color.SADDLEBROWN)
		                        //.subTitle("score")
		                        //.subTitleColor(Color.MAROON)
		                        .thresholdColor(Color.GREY)
		                        .valueColor(Color.BROWN)
		                        .animated(false)
		                        .build();
						bars.add(bar);
						barBox.getChildren().addAll(bar);
				}
				
				  
				Map<Integer, Integer> scores = person.getMind().getMBTI().getScores();

				Gauge b0 = bars.get(0);
				b0.setValue(scores.get(PersonalityType.INTROVERSION_EXTRAVERSION));
				b0.setTitle("I - E  ");
				Tooltip t0 = new Tooltip("Introversion vs. Extraversion");
				b0.setTooltip(t0);
				setQuickToolTip(b0, t0);

				Gauge b1 = bars.get(1);
				b1.setTitle("N - S  ");
				b1.setValue(scores.get(PersonalityType.INTUITION_SENSATION));
				Tooltip t1 = new Tooltip("Intuition vs. Sensation");
				b1.setTooltip(t1);
				setQuickToolTip(b1, t1);
				
				Gauge b2 = bars.get(2);
				b2.setTitle("F - T  ");
				b2.setValue(scores.get(PersonalityType.FEELING_THINKING));
				Tooltip t2 = new Tooltip("Feeling vs. Thinking");
				b2.setTooltip(t2);
				setQuickToolTip(b2, t2);
				
				Gauge b3 = bars.get(3);
				b3.setTitle("J - P  ");
				b3.setValue(scores.get(PersonalityType.JUDGING_PERCEIVING));
				Tooltip t3 = new Tooltip("Judging vs. Perceiving");
				b3.setTooltip(t3);
				setQuickToolTip(b3, t3);
				
				
				// Build gauges for Big Five Personality	
				VBox gaugeBox = new VBox();
				gaugeBox.setPadding(new Insets(15,5,0,5));
				//gaugeBox.setSpacing(value);
				gaugeBox.setAlignment(Pos.TOP_LEFT);			
			
				HBox hBox1 = new HBox();
				hBox1.setAlignment(Pos.TOP_LEFT);
				hBox1.setPadding(new Insets(5, 5, 5, 5));
				
				Label label1 = new Label(Msg.getString("TabPanelPersonality.bigFive"));
				label1.setPadding(new Insets(5, 5, 5, 5));
				label1.setFont(Font.font("Cambria", FontWeight.NORMAL, 16));
		
				Tooltip i3 = new Tooltip("also known as Five Factor Model (FFM)");
				// Note: OCC Model is for emotion synthesis, by Ortony, Clore, & Collins, 1988

				label1.setTooltip(i3);
				setQuickToolTip(label1, i3);
			
				JFXTextField tf1 = new JFXTextField("O.C.E.A.N.");
				tf1.setFont(Font.font("Cambria", FontWeight.NORMAL, 16));
				tf1.setAlignment(Pos.TOP_LEFT);
				tf1.setEditable(false);
				tf1.setPrefSize(90, 20);
				
				Tooltip i2 = new Tooltip("Openness"  + System.lineSeparator()
					+ "Conscientiousness"  + System.lineSeparator() 
					+ "Extraversion"  + System.lineSeparator()
					+ "Agreeableness"  + System.lineSeparator()
					+ "Neuroticism" );

				tf1.setTooltip(i2);
				setQuickToolTip(tf1, i2);
				
				GridPane grid = new GridPane();			
				List<VBox> vboxes = new ArrayList<VBox>();
				List<QualityGauge> gauges = new ArrayList<QualityGauge>();		
				for (int i=0; i<3; i++) {
					for (int j=0; j<2; j++) {
						QualityGauge g = new QualityGauge();
						VBox box = new VBox(g);
						box.setAlignment(Pos.CENTER);
						g.setMinSize(140, 80);
						vboxes.add(box);
						gauges.add(g);
						grid.add(box, j, i);
					}
				}
				
				vboxes.get(0).getChildren().add(createLabel("Openness"));
				vboxes.get(1).getChildren().add(createLabel("Conscientiousness"));
				vboxes.get(2).getChildren().add(createLabel("Extraversion"));
				vboxes.get(3).getChildren().add(createLabel("Aggreeableness"));
				vboxes.get(4).getChildren().add(createLabel("Neuroticism"));

				grid.getChildren().remove(vboxes.get(5));
				vboxes.remove(gauges.get(5));
				gauges.remove(5);
				
				Map<PersonalityTraitType, Integer> points = person.getMind().getPersonalityTraitManager().getPersonalityTraitMap();

				QualityGauge g0 = gauges.get(0);
				g0.setValue((int)(Math.round(points.get(PersonalityTraitType.OPENNESS)/10d)));
				Tooltip tt0 = new Tooltip("Openness is willingness to make a shift of standards in new situations");
				Tooltip.install(g0, tt0);
				setQuickToolTip(g0, tt0);

				QualityGauge g1 = gauges.get(1);
				g1.setValue((int)(Math.round(points.get(PersonalityTraitType.CONSCIENTIOUSNESS)/10d)));
				Tooltip tt1 = new Tooltip("Conscientiousness is planning ahead rather than being spontaneous");
				Tooltip.install(g1, tt1);
				setQuickToolTip(g1, tt1);
				
				QualityGauge g2 = gauges.get(2);
				g2.setValue((int)(Math.round(points.get(PersonalityTraitType.EXTRAVERSION)/10d)));
				Tooltip tt2 = new Tooltip("Extraversion is willingness to communicate and socialize with people");
				Tooltip.install(g2, tt2);
				setQuickToolTip(g2, tt2);
				
				QualityGauge g3 = gauges.get(3);
				g3.setValue((int)(Math.round(points.get(PersonalityTraitType.AGREEABLENESS)/10d)));
				Tooltip tt3 = new Tooltip("Aggreeablene is adaptiveness to other people and adopting goals in favor of others");
				Tooltip.install(g3, tt3);
				setQuickToolTip(g3, tt3);
	
				QualityGauge g4 = gauges.get(4);
				g4.setValue((int)(Math.round(points.get(PersonalityTraitType.NEUROTICISM)/10d)));
				Tooltip tt4 = new Tooltip("Neuroticism is a person's emotional sensitivity and sense of security to the situation.");
				Tooltip.install(g4, tt4);
				setQuickToolTip(g4, tt4);		

				hBox0.getChildren().addAll(label0, tf0);
				chartBox.getChildren().addAll(hBox0, barBox);
			
				hBox1.getChildren().addAll(label1, tf1);
				gaugeBox.getChildren().addAll(hBox1, grid);
								
				topBox.getChildren().addAll(title, chartBox, gaugeBox);

				stack.getChildren().addAll(topBox);
			}
		});

		centerContentPanel.add(jfxpanel);
		this.setSize(new Dimension(width, height));
		this.setVisible(true);
	}
	
	/**
	 * Speeds up the time it takes to display JavaFX's tooltip 
	 * @param node
	 * @param tooltip
	 */
	@SuppressWarnings("restriction")
	public void setQuickToolTip(Node n, Tooltip tt) {
		
		tt.getStyleClass().add("ttip");

		n.setOnMouseEntered(new EventHandler<MouseEvent>() {
			
		    @Override
		    public void handle(MouseEvent event) {
		        Point2D p = n.localToScreen(n.getLayoutBounds().getMaxX(), n.getLayoutBounds().getMaxY()); //I position the tooltip at bottom right of the node (see below for explanation)
		        tt.show(n, p.getX(), p.getY());
		    }
		});
		n.setOnMouseExited(new EventHandler<MouseEvent>() {
		 
		    @Override
		    public void handle(MouseEvent event) {
		        tt.hide();
		    }
		});
		
	}
	
	@SuppressWarnings("restriction")
	public Label createLabel(String title) {
		Label l = new Label(title);
		l.setMinSize(140, 20);
		l.setAlignment(Pos.TOP_CENTER);
		l.setContentDisplay(ContentDisplay.TOP);
		l.setFont(Font.font("Cambria", FontWeight.LIGHT, 15));
		return l;
	}
	
	public void createSwingGUI() {
		
		// Create general label panel.
		//JPanel generalLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));//LEFT));
		//topContentPanel.add(generalLabelPanel);

		// Prepare general label
		//JLabel generalLabel = new JLabel(Msg.getString("TabPanelPersonality.label"), JLabel.CENTER); //LEFT)); //$NON-NLS-1$
		//generalLabel.setFont(new Font("Serif", Font.BOLD, 16));
		//generalLabelPanel.add(generalLabel);

		// Prepare info panel.
		JPanel infoPanel = new JPanel(new GridLayout(1, 2, 0, 0));//FlowLayout(FlowLayout.LEFT));//
		infoPanel.setBorder(new MarsPanelBorder());
		centerContentPanel.add(infoPanel, BorderLayout.NORTH);

		// Prepare personality name label
		JLabel personalityNameLabel = new JLabel(Msg.getString("TabPanelGeneral.personalityMBTI"), JLabel.RIGHT); //$NON-NLS-1$
		personalityNameLabel.setSize(5, 2);
		personalityNameLabel.setToolTipText("<html>Myers-Briggs Type Indicator (MBTI) <br> as a metric for personality type</html>");
		infoPanel.add(personalityNameLabel);

		// Prepare personality label
		String personality = person.getMind().getMBTI().getTypeString();
		//JLabel personalityLabel = new JLabel(personality, JLabel.RIGHT);
		JTextField personalityTF = new JTextField(personality);
		personalityTF.setEditable(false);
		personalityTF.setColumns(12);
		
		String type1 = personality.substring(0,1);
		if (type1.equals("E"))
			type1 = "Extrovert (E)";
		else
			type1 = "Introvert (I)";

		String type2 = personality.substring(1,2);
		if (type2.equals("N"))
			type2 = "Intuitive (N)";
		else
			type2 = "Sensing (S)";

		String type3 = personality.substring(2,3);
		if (type3.equals("F"))
			type3 = "Feeler (F)";
		else
			type3 = "Thinker (T)";

		String type4 = personality.substring(3,4);
		if (type4.equals("J"))
			type4 = "Judger (J)";
		else
			type4 = "Perceiver (P)";

		personalityTF.setToolTipText("<html>" + type1 + " : " + "<br>" + type2 + "<br>" + type3 + "<br>" + type4 
				+ "<br>" + "Note: see the 4 scores below"+ "<br>" + "</html>");
		infoPanel.add(personalityTF);

		// Prepare gauge panel.
		JPanel gaugePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));//GridLayout(4, 1, 0, 0));
		//gaugePanel.setBorder(new MarsPanelBorder());
		centerContentPanel.add(gaugePanel, BorderLayout.CENTER);		

		Map<Integer, Integer> scores = person.getMind().getMBTI().getScores();
		
		List<Radial2Top> radials = new ArrayList<Radial2Top>();
		for (int i=0; i< 4; i++) {
			Radial2Top r = new Radial2Top();
			radials.add(r);
			gaugePanel.add(r);
		}
		
		radials.get(0).setTitle("Introversion vs. Extravsersion");
		radials.get(0).setToolTipText("Introversion vs. Extravsersion");
		radials.get(0).setValue(scores.get(PersonalityType.INTROVERSION_EXTRAVERSION));

		radials.get(1).setTitle("Intuition vs. Sensation");
		radials.get(1).setToolTipText("Intuition vs. Sensation");
		radials.get(1).setValue(scores.get(PersonalityType.INTUITION_SENSATION));
		
		radials.get(2).setTitle("Feeling vs. Thinking");		
		radials.get(2).setToolTipText("Feeling vs. Thinking");
		radials.get(2).setValue(scores.get(PersonalityType.FEELING_THINKING));

		radials.get(3).setTitle("Judging vs. Perceiving");
		radials.get(3).setToolTipText("Judging vs. Perceiving");
		radials.get(3).setValue(scores.get(PersonalityType.JUDGING_PERCEIVING));

		
		for (Radial2Top r : radials) {
			r.setUnitString("");
			r.setLedBlinking(false);
			r.setMajorTickSpacing(20);
			r.setMinorTickmarkVisible(false);
			r.setMinValue(0);
			r.setMaxValue(100);
			r.setSize(new Dimension(350, 350));
			r.setMaximumSize(new Dimension(350, 350));
			r.setPreferredSize(new Dimension(350, 350));
			r.setVisible(true);
		}
	}

	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
		// Person person = (Person) unit;
		// Fill in as we have more to update on this panel.
	}
}