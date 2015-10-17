/**
 * Mars Simulation Project
 * MainMenu.java
 * @version 3.08 2015-04-09
 * @author Manny Kung
 */

package org.mars_sim.msp.javafx;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javafx.animation.FadeTransition;
import javafx.scene.input.MouseEvent;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.shape.Rectangle;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.AmbientLight;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Modality;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.mars.OrbitInfo;
import org.mars_sim.msp.javafx.configEditor.ScenarioConfigEditorFX.SimulationTask;
import org.mars_sim.msp.javafx.controller.MainMenuController;
import org.mars_sim.msp.networking.MultiplayerMode;
import org.mars_sim.msp.ui.javafx.MainScene;

/*
 * The MainMenu class creates the Main Menu and the spinning Mars Globe for MSP
 */
public class MainMenu {

    /** default logger. */
    private static Logger logger = Logger.getLogger(MainMenu.class.getName());

    private static final int WIDTH = 768-20;
    private static final int HEIGHT = 768-20;

	// Data members
    private double fileSize;

    public static String screen1ID = "main";
    public static String screen1File = "/fxui/fxml/MainMenu.fxml";
    public static String screen2ID = "configuration";
    public static String screen2File = "/fxui/fxml/Configuration.fxml";
    public static String screen3ID = "credits";
    public static String screen3File = "/fxui/fxml/Credits.fxml";

    private double anchorX;
    private double rate;

    private boolean isDone;

    //private boolean cleanUI = true;

    private final DoubleProperty sunDistance = new SimpleDoubleProperty(100);
    private final BooleanProperty sunLight = new SimpleBooleanProperty(true);
    private final BooleanProperty diffuseMap = new SimpleBooleanProperty(true);
    private final BooleanProperty specularMap = new SimpleBooleanProperty(true);
    private final BooleanProperty bumpMap = new SimpleBooleanProperty(true);
    //private final BooleanProperty selfIlluminationMap = new SimpleBooleanProperty(true);


    private RotateTransition rt;
    private Sphere mars;
    private PhongMaterial material;
    private PointLight sun;

    //private Group root;
    private StackPane root;
	private Stage mainSceneStage, mainMenuStage, circleStage;
	public Scene mainMenuScene, mainSceneScene;

	public MainMenu mainMenu;
	public MainScene mainScene;
	public ScreensSwitcher screen;

	private MarsProjectFX marsProjectFX;
	private transient ThreadPoolExecutor executor;
	private MultiplayerMode multiplayerMode;

	public MainMenuController mainMenuController;

    public MainMenu(MarsProjectFX marsProjectFX) {
    	this.marsProjectFX = marsProjectFX;
    	mainMenu = this;
    	//logger.info("MainMenu's constructor is on " + Thread.currentThread().getName() + " Thread");
	}

    /*
     * Sets up and shows the MainMenu and prepare the stage for MainScene
     */
	void initAndShowGUI(Stage mainMenuStage) {
	   //logger.info("MainMenu's initAndShowGUI() is on " + Thread.currentThread().getName() + " Thread");

	   this.mainMenuStage = mainMenuStage;

    	Platform.setImplicitExit(false);

    	mainMenuStage.setOnCloseRequest(e -> {
			boolean isExit = screen.exitDialog(mainMenuStage);
			if (!isExit) {
				e.consume();
			}
			else {
				Platform.exit();
	    		System.exit(0);
			}
		});

       screen = new ScreensSwitcher(this);
       screen.loadScreen(MainMenu.screen1ID, MainMenu.screen1File);
       screen.loadScreen(MainMenu.screen2ID, MainMenu.screen2File);
       screen.loadScreen(MainMenu.screen3ID, MainMenu.screen3File);
       screen.setScreen(MainMenu.screen1ID);
       
       if ( screen.lookup("#menuOptionBox") == null)
			System.out.println("Warning: menu option box is not found");
       
       VBox menuOptionBox = ((VBox) screen.lookup("#menuOptionBox"));
/*       
       if (mouseEvent.getSource().equals(menuOptionBox)){
    	   System.out.println("hovering over menu option box");
    	   mainMenuScene.setCursor(Cursor.HAND);
       }
*/       
       Rectangle rect = new Rectangle(WIDTH, HEIGHT);//, Color.rgb(179,53,0));//rgb(69, 56, 35));//rgb(86,70,44));//SADDLEBROWN);
       rect.setArcWidth(30);
       rect.setArcHeight(30);
       rect.setEffect(new DropShadow(30,5,5, Color.BLACK));//TAN)); // rgb(27,8,0)));// for bottom right edge

       root = new StackPane();
       root.setPrefHeight(WIDTH);
       root.setPrefWidth(HEIGHT);
       root.setStyle(
    		   //"-fx-border-style: none; "
    		   //"-fx-background-color: #231d12; "
       			"-fx-background-color: transparent; "
       			//+ "-fx-background-radius: 1px;"
    		   );
       
       Parent globe = createMarsGlobe();
       
       root.getChildren().addAll(rect, globe, screen);
       
       mainMenuScene = new Scene(root, Color.DARKGOLDENROD);//TRANSPARENT);//, Color.TAN);//MAROON); //TRANSPARENT);//DARKGOLDENROD);
       mainMenuScene.getStylesheets().add(this.getClass().getResource("/fxui/css/mainmenu.css").toExternalForm() );
       //mainMenuScene.setFill(Color.BLACK); // if using Group, a black border will remain
       //mainMenuScene.setFill(Color.TRANSPARENT); // if using Group, a white border will remain
       mainMenuScene.setCursor(Cursor.HAND);

       mainMenuScene.setOnMouseEntered(new EventHandler<MouseEvent>(){
    	   
           public void handle(MouseEvent mouseEvent){
               FadeTransition fadeTransition 
                       = new FadeTransition(Duration.millis(1000), menuOptionBox);
               fadeTransition.setFromValue(0.0);
               fadeTransition.setToValue(1.0);
               fadeTransition.play();

           }
       });
        
       mainMenuScene.setOnMouseExited(new EventHandler<MouseEvent>(){

           public void handle(MouseEvent mouseEvent){
               FadeTransition fadeTransition 
                       = new FadeTransition(Duration.millis(1000), menuOptionBox);
               fadeTransition.setFromValue(1.0);
               fadeTransition.setToValue(0.0);
               fadeTransition.play();

           }
       });
       
      
       // 2015-09-26 Added adjustRotation()
       adjustRotation(mainMenuScene, globe);

       // Enable dragging on the undecorated stage
       EffectUtilities.makeDraggable(mainMenuStage, screen);
       
       //mainMenuStage.initStyle(StageStyle.UTILITY);
       mainMenuStage.initStyle(StageStyle.TRANSPARENT);
       mainMenuStage.initStyle(StageStyle.UNDECORATED);
       mainMenuStage.centerOnScreen();
       mainMenuStage.setResizable(false);
	   mainMenuStage.setTitle(Simulation.WINDOW_TITLE);
       mainMenuStage.setScene(mainMenuScene);
       mainMenuStage.getIcons().add(new Image(this.getClass().getResource("/icons/lander_hab64.png").toExternalForm()));//toString()));
       //mainSceneStage.getIcons().add(new Image(this.getClass().getResource("/icons/lander_hab.svg").toString()));
       mainMenuStage.show();

       createProgressCircle();
       circleStage.hide();

       // Starts a new stage for MainScene
	   mainSceneStage = new Stage();
	   //mainSceneStage.initModality(Modality.NONE);
	   mainSceneStage.setMinWidth(1024);
	   mainSceneStage.setMinHeight(600);
	    
	   //mainSceneStage.setMaxWidth(1920);
	   //mainSceneStage.setMinHeight(1200);

	   mainSceneStage.setTitle(Simulation.WINDOW_TITLE);

   }

   public Stage getStage() {
	   return mainMenuStage;
   }

   public MainScene getMainScene() {
	   return mainScene;
   }
   
   public MultiplayerMode getMultiplayerMode() {
	   return multiplayerMode;
	}

   public void runOne() {
	   //logger.info("MainMenu's runOne() is on " + Thread.currentThread().getName() + " Thread");
	   mainMenuStage.setIconified(true); //hide();
	   mainScene = new MainScene(mainSceneStage);
	   marsProjectFX.handleNewSimulation();
   }

   public void runTwo() {
		Future future = Simulation.instance().getSimExecutor().submit(new LoadSimulationTask());
		//System.out.println("desktop is " + mainMenu.getMainScene().getDesktop());
		
	   //logger.info("MainMenu's runTwo() is on " + Thread.currentThread().getName() + " Thread");
		Platform.runLater(() -> {
			mainMenuScene.setCursor(Cursor.WAIT);
			// TODO: the ring won't turn until a couple of seconds later
			circleStage.show();
			circleStage.requestFocus();
		});	
		
		mainMenuStage.setIconified(true);//hide();
		mainScene = new MainScene(mainSceneStage);		   

		// 2015-10-13 Set up a Task Thread
		Task task = new Task() {
            @Override
            protected Integer call() throws Exception {
				try {
					   TimeUnit.MILLISECONDS.sleep(2000L);
					   // Note 1: The delay time for launching the JavaFX UI should also be based on the file size of the default.sim
					   long delay_time = (long) (fileSize * 4000L);
					   TimeUnit.MILLISECONDS.sleep(delay_time);
					
					   //System.out.println("desktop is " + mainMenu.getMainScene().getDesktop());
					   //System.out.println("isMainSceneDone is " + mainScene.isMainSceneDone());
					   
					   //while (!future.isDone() && !mainScene.isMainSceneDone()) {
					   while ( !future.isDone() && mainMenu.getMainScene().getDesktop() == null) {						   
			        		long delay = (long) (fileSize * 1000L);
							//System.out.println("Wait for " + delay/1000D + " secs inside the while loop");
							TimeUnit.MILLISECONDS.sleep(delay);
			        		//System.out.println("desktop is " + mainMenu.getMainScene().getDesktop());
			        		//System.out.println("desktop is " + mainScene.isMainSceneDone());
			     		   // Note: java8u60 requires a longer time (than 8u45) such as 4 secs instead of 2 secs on some machines.
			 			   // or else it will proceed to prepareStage() below prematurely and results in NullPointerException
			 			   // as it tries to read people data before it was properly populated in UnitManager.
			 			   //service.shutdownNow();
			 			   //try {
			 			   //   service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			 			   //} catch (InterruptedException e) {}
					   }
					   //System.out.println("future.get() is " + future.get());
					   //System.out.println("future.isDone() is " + future.isDone());
	       				
					   TimeUnit.MILLISECONDS.sleep(2000L);
					   Platform.runLater(() -> {
						   prepareStage();
						   circleStage.close();
						});
					   
				} catch (Exception e) {
					e.printStackTrace();
				}

                return 0;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        mainMenuScene.setCursor(Cursor.DEFAULT); //Change cursor to default style


/*
		
	   // 2015-10-15 Set up a Task Thread
	   isDone = true;
	   Task task = new Task() {
           @Override
           protected Integer call() throws Exception {
               
        	   Future future = Simulation.instance().getSimExecutor().submit(new LoadSimulationTask());
        	   
      			System.out.println("future.get() is " + future.get());
      			System.out.println("future.isDone() is " + future.isDone());

      			try {
					while (!future.isDone()) {
						System.out.println("still inside the while loop");
						System.out.println("fileSize is " + fileSize);
						fileSize = Simulation.instance().getFileSize();
						long delay_time = (long) (fileSize * 4000L);
						TimeUnit.MILLISECONDS.sleep(delay_time);
					}					
	       			System.out.println("future.get() is " + future.get());
	       			System.out.println("future.isDone() is " + future.isDone());

					Platform.runLater(() -> {
						prepareStage();						
						circleStage.close();
					});
	
				} catch (Exception e) {
					e.printStackTrace();
				}

               return 0;
           }
       };
       Thread th = new Thread(task);
       th.setDaemon(true);
       th.start();
*/       
   }

	public class LoadSimulationTask implements Runnable {
		public void run() {
			fileSize = 1;
			logger.info("Loading settlement data from the default saved simulation...");
			Simulation.instance().loadSimulation(null); // null means loading "default.sim"
			System.out.println("done with loadSimulation(null)");
			logger.info("Restarting " + Simulation.WINDOW_TITLE);
			Simulation.instance().start();
			Simulation.instance().stop();
			Simulation.instance().start();
			Platform.runLater(() -> {
				prepareScene();
				System.out.println("done with prepareScene()");
			});
		}
	}

	public void prepareScene() {
	   //logger.info("MainMenu's prepareStage() is on " + Thread.currentThread().getName() + " Thread");

	   // prepare main scene
	   mainScene.prepareMainScene();
	   mainSceneScene = mainScene.initializeScene();
	   
	}
	
	public void prepareStage() {
	
	   mainScene.prepareOthers();

	   // prepare stage
	   //stage.getIcons().add(new Image(this.getClass().getResource("/icons/lander_hab.svg").toString()));
       mainSceneStage.getIcons().add(new Image(this.getClass().getResource("/icons/lander_hab64.png").toExternalForm()));//.toString()));
	   mainSceneStage.setResizable(true);
	   //stage.setFullScreen(true);
	   mainSceneStage.setScene(mainSceneScene);
	   mainSceneStage.show();
	   mainSceneStage.requestFocus();
	   
	   circleStage.hide();
	   //mainScene.getMarsNode().createSettlementWindow();
	   //mainScene.getMarsNode().createJMEWindow(stage);
	   mainScene.initializeTheme();

	   //return true;
	   //logger.info("done with stage.show() in MainMenu's prepareStage()");
	}

   public void runThree() {
	   //logger.info("MainMenu's runThree() is on " + Thread.currentThread().getName() + " Thread");
	   Simulation.instance().getSimExecutor().submit(new MultiplayerTask());
	   mainMenuStage.setIconified(true);//hide();
/*
	   try {
			multiplayerMode = new MultiplayerMode(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1); // newCachedThreadPool();
		executor.execute(multiplayerMode.getModeTask());
*/
	    //pool.shutdown();
	     //mainSceneStage.toFront();
   		//modtoolScene = new SettlementScene().createSettlementScene();
	    //stage.setScene(modtoolScene);
	    //stage.show();
   }

	public class MultiplayerTask implements Runnable {
		public void run() {
			try {
				multiplayerMode = new MultiplayerMode(mainMenu);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1); // newCachedThreadPool();
			executor.execute(multiplayerMode.getModeTask());
		}
	}

   //public void changeScene(int toscene) {
	   //switch (toscene) {
		   	//case 1:
		   		//scene = new MenuScene().createScene();
		   	//	break;
	   		//case 2:
	   			//Scene scene = mainScene.createMainScene();
	   			//break;
	   		//case 3:
	   			//scene = modtoolScene.createScene(stage);
	   		//	break;
	   		//stage.setScene(scene);
	   //}
   //}


   public Parent createMarsGlobe() {
	   logger.info("Is ConditionalFeature.SCENE3D supported on this platform? " + Platform.isSupported(ConditionalFeature.SCENE3D));

	   Image sImage = new Image(this.getClass().getResource("/maps/rgbmars-spec1k.jpg").toExternalForm());
       Image dImage = new Image(this.getClass().getResource("/maps/MarsV3Shaded1k.jpg").toExternalForm());
       Image nImage = new Image(this.getClass().getResource("/maps/MarsNormal1k.png").toExternalForm()); //.toString());
       //Image siImage = new Image(this.getClass().getResource("/maps/rgbmars-names-2k.jpg").toExternalForm()); //.toString());

       material = new PhongMaterial();
       material.setDiffuseColor(Color.WHITE);
       material.diffuseMapProperty().bind(Bindings.when(diffuseMap).then(dImage).otherwise((Image) null));
       material.setSpecularColor(Color.TRANSPARENT);
       material.specularMapProperty().bind(Bindings.when(specularMap).then(sImage).otherwise((Image) null));
       material.bumpMapProperty().bind(Bindings.when(bumpMap).then(nImage).otherwise((Image) null));
       //material.selfIlluminationMapProperty().bind(Bindings.when(selfIlluminationMap).then(siImage).otherwise((Image) null));

       mars = new Sphere(4);
       mars.setMaterial(material);
       mars.setRotationAxis(Rotate.Y_AXIS);

       // Create and position camera
       PerspectiveCamera camera = new PerspectiveCamera(true);
       camera.getTransforms().addAll(
               new Rotate(-10, Rotate.Y_AXIS),
               new Rotate(-10, Rotate.X_AXIS),
               new Translate(0, 0, -10));

       sun = new PointLight(Color.rgb(255, 243, 234));
       sun.translateXProperty().bind(sunDistance.multiply(-0.82));
       sun.translateYProperty().bind(sunDistance.multiply(-0.41));
       sun.translateZProperty().bind(sunDistance.multiply(-0.41));
       sun.lightOnProperty().bind(sunLight);

       AmbientLight ambient = new AmbientLight(Color.rgb(1, 1, 1));

       // Build the Scene Graph
       Group globeComponents = new Group();
       //globeComponents.setStyle("-fx-border-color: rgba(0, 0, 0, 0);");
       //globeComponents.setScaleX(.75);
       //globeComponents.setScaleY(.75);
       globeComponents.getChildren().add(camera);
       globeComponents.getChildren().add(mars);
       globeComponents.getChildren().add(sun);
       globeComponents.getChildren().add(ambient);

       // Increased the speed of rotation 500 times as dictated by the value of time-ratio in simulation.xml
       rt = new RotateTransition(Duration.seconds(OrbitInfo.SOLAR_DAY/500D), mars);
       //rt.setByAngle(360);
       rt.setInterpolator(Interpolator.LINEAR);
       rt.setCycleCount(Animation.INDEFINITE);
       rt.setAxis(Rotate.Y_AXIS);
       rt.setFromAngle(360);
       rt.setToAngle(0);
       //rt.setCycleCount(RotateTransition.INDEFINITE);
       rt.play();

       // Use a SubScene
       SubScene subScene = new SubScene(globeComponents, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
       subScene.setId("sub");
       subScene.setCamera(camera);

       return new Group(subScene);
   }

   /*
    * Adjusts Mars Globe's rotation rate according to how much the user's drags his mouse across the globe
    */
   // 2015-09-26 Added adjustRotation()

   private void adjustRotation(Scene scene, Parent parent) {

	   // 2015-09-26 Changes Mars Globe's rotation rate if a user drags his mouse across the globe
	   scene.setOnMousePressed((event) -> {
		   // 2015-10-13 Detected right mouse button pressed
           if (event.isSecondaryButtonDown())
        	   anchorX = event.getSceneX();
	   });

	   scene.setOnMouseDragged((event) -> {
		   // 2015-10-13 Detected right mouse button drag
		   if (event.isSecondaryButtonDown()) {

			   double a = anchorX - event.getSceneX();
			   double rotationMultipler;

			   if (a < 0) { // left to right
				   rate = Math.abs(a/100D);
			   }
			   else { // right to left
				   rate = Math.abs(100D/a) ;
			   }

			   if (rate < 100) { // This prevents unrealistic and excessive rotation rate that creates spurious result (such as causing the direction of rotation to "flip"
				   rt.setRate(rate);
				   anchorX = 0;
				   rotationMultipler = rate * 500D;

				   //System.out.println("rate is " + rate + "   rotationMultipler is "+ rotationMultipler);

				   mainMenuController.setRotation((int)rotationMultipler);
			   }
           }
	   });
   }

   /*
    * Sets the main menu controller at the start of the sim.
    */
   // 2015-09-27 Added setController()
   public void setController(ControlledScreen myScreenController) {
	   if (mainMenuController == null)
		   if (myScreenController instanceof MainMenuController )
			   mainMenuController = (MainMenuController) myScreenController;
   }

   /*
    * Resets the rotational rate back to 500X
    */
   // 2015-09-27 Added setDefaultRotation()
   public void setDefaultRotation() {
	   rt.setRate(1.0);
   }

/*
	public boolean exitDialog(Stage stage) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.initOwner(stage);
		alert.setTitle("Exiting MSP");//("Confirmation Dialog");
		alert.setHeaderText("Do you really want to exit MPS?");
		//alert.initModality(Modality.APPLICATION_MODAL);
		alert.setContentText("Warning: exiting the main menu will terminate any running simultion without saving it.");
		ButtonType buttonTypeYes = new ButtonType("Yes");
		ButtonType buttonTypeNo = new ButtonType("No");
	   	alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);
	   	Optional<ButtonType> result = alert.showAndWait();
	   	if (result.get() == buttonTypeYes){
	   		if (multiplayerMode != null)
	   			if (multiplayerMode.getChoiceDialog() != null)
	   				multiplayerMode.getChoiceDialog().close();
	   		alert.close();
	   		return true;
	   	} else {
	   		alert.close();
	   	    return false;
	   	}
   	}
*/
   /*
    * Create the progress circle animation while waiting for loading the main scene
    */
	public void createProgressCircle() {

		StackPane stackPane = new StackPane();

		//BorderPane controlsPane = new BorderPane();
		//controlsPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		//stackPane.getChildren().add(controlsPane);
		//controlsPane.setCenter(new TableView<Void>());

		ProgressIndicator indicator = new ProgressIndicator();
		indicator.setMaxSize(120, 120);
		//indicator.setProgress(50);
		ColorAdjust adjust = new javafx.scene.effect.ColorAdjust();
		adjust.setHue(-0.07); // -.07, -0.1 cyan; 3, 17 = red orange; -0.4 = bright green
		indicator.setEffect(adjust);
		stackPane.getChildren().add(indicator);
		StackPane.setAlignment(indicator, Pos.CENTER);
		StackPane.setMargin(indicator, new Insets(20));
		stackPane.setBackground(javafx.scene.layout.Background.EMPTY);

		circleStage = new Stage();
		Scene scene = new Scene(stackPane, 120, 120);
		scene.setFill(Color.TRANSPARENT);
		circleStage.initStyle (StageStyle.TRANSPARENT);
		circleStage.setScene(scene);
        circleStage.show();
/*

		final Float[] values = new Float[] {-1.0f, 0f, 0.6f, 1.0f};
		final Label [] labels = new Label[values.length];
		final ProgressBar[] pbs = new ProgressBar[values.length];
		final ProgressIndicator[] pins = new ProgressIndicator[values.length];
		final HBox hbs [] = new HBox [values.length];


		circleStage = new Stage();
		Group root = new Group();
		Scene scene = new Scene(root, 300, 250);
		circleStage.setScene(scene);
		circleStage.setTitle("Progress Controls");

        for (int i = 0; i < values.length; i++) {
            final Label label = labels[i] = new Label();
            label.setText("progress:" + values[i]);

            final ProgressBar pb = pbs[i] = new ProgressBar();
            pb.setProgress(values[i]);

            final ProgressIndicator pin = pins[i] = new ProgressIndicator();
            pin.setProgress(values[i]);
            final HBox hb = hbs[i] = new HBox();
            hb.setSpacing(5);
            hb.setAlignment(Pos.CENTER);
            hb.getChildren().addAll(label, pb, pin);
        }

        final VBox vb = new VBox();
        vb.setSpacing(5);
        vb.getChildren().addAll(hbs);
        scene.setRoot(vb);
        circleStage.show();


	    circleStage = new Stage();

		RingProgressIndicator indicator = new RingProgressIndicator();
		//Slider slider = new Slider(0, 100, 50);

		final ProgressIndicator ring = new ProgressIndicator();

		//slider.valueProperty().addListener((o, oldVal, newVal) -> indicator.setProgress(newVal.intValue()));
		//VBox main = new VBox(1, indicator, slider);
		VBox main = new VBox(ring); //2,inindicator, ring);
		//indicator.setProgress(Double.valueOf(slider.getValue()).intValue());
		Group root = new Group();
		//root.getChildren().add(main);
        Scene scene = new Scene(root);//, 200, 150);
		//circleStage.initStyle(StageStyle.TRANSPARENT);
        //scene.setFill(Color.BLACK);
        //scene.setCursor(Cursor.WAIT);
        //scene.setFill(Color.TRANSPARENT); // needed to eliminate the white border
        //circleStage.initStyle (StageStyle.TRANSPARENT); //(StageStyle.UTILITY); //or
        circleStage.initStyle (StageStyle.UTILITY);
		circleStage.setScene(scene);
		scene.setRoot(main);
		circleStage.setTitle("Processing");
		circleStage.show();
*/

	}

	public Stage getCircleStage() {
		return circleStage;
	}

	public ScreensSwitcher getScreensSwitcher() {
		return screen;
	}

	public void destroy() {

		rt = null;
		mars = null;
		material = null;
		sun = null;
		root = null;
		screen = null;
		mainSceneStage = null;
		mainMenuStage = null;
		circleStage = null;
		mainMenuScene = null;
		mainMenu = null;
		mainScene = null;
		marsProjectFX = null;
		executor = null;
		multiplayerMode = null;
		mainMenuController = null;
	}

}
