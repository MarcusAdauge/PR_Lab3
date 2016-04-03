import java.net.*;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.io.*;

public class ClientUDP extends Application { 
	
	// message types
	final static int START_DRAW = 0;
	final static int DRAW = 1;
	final static int END_DRAW = 2;
	final static int I_AM_NEW = 3;
	final static int GREETINGS = 4;
	final static int SUBMIT_ME = 5;
	final static int CLEAR_CANVAS = 6;
	final static int CLEAR_CANVAS_RESPONSE = 7;
	final static int GOODBYE = 8;
	
	// GUI-related instance variables
	final static double CANVAS_WIDTH = 450;
	final static double CANVAS_HEIGHT = 400;
	final static double SCENE_WIDTH = 600;
	final static double SCENE_HEIGHT = 400;
	static Stage window;
	static Scene scene;
	BorderPane root = new BorderPane();
	ScrollPane logPane = new ScrollPane();
	static volatile VBox logStack = new VBox(5);
	public static volatile Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
    public static final GraphicsContext gc = canvas.getGraphicsContext2D();
    private double current_stroke_width = 1;
    Slider strokeSlider = new Slider(1, 10, 1);
    public volatile Button btnClear = new Button("CLEAR CANVAS");
    final Image cursorIcon = new Image("file:///D:/JavaWorkspace/LastCopy_SharedBoard/res/pencilCursor.png");
	
    // Networking-related instance variables
	static CastingSocket socket = null;
	static User me;
    static volatile ArrayList<User> allUsers = new ArrayList<User>();
    static InetAddress groupIP;
    final static int PORT = 6666;
	
    
	@Override
	public void start(Stage primaryStage) throws Exception {
		window = primaryStage;
		window.setTitle("Shared White-Board");
		
		logPane.setPrefSize(150, canvas.getHeight() - 40);
		logPane.setStyle("-fx-background: rgb(200,200,200);");
		logPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		logPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		logPane.setContent(logStack);
		logStack.setPadding(new Insets(0,0,0,10));
		
		canvas.setCursor(new ImageCursor(cursorIcon));
		
		strokeSlider.valueProperty().addListener(new ChangeListener<Object>() {
            public void changed(ObservableValue obsValue, Object arg1, Object arg2) {
                current_stroke_width = strokeSlider.getValue();
            }
        });
		
		// Print on the log pane the online users
		logStack.getChildren().add(new Label("Online users:"));
        HBox container = new HBox(5);
        for(User u : allUsers){
      	   Circle c = new Circle(9);
             c.setFill(u.color); 
             container.getChildren().add(c);
        }
        logStack.getChildren().add(container);
        Separator separator = new Separator();
        separator.setPrefWidth(115);
        logStack.getChildren().add(separator);
		
		Painting();
		Watcher watcher = new Watcher(socket, groupIP, PORT);
		watcher.start();
		
		//safely closing client (interrupting its Watcher thread)
		window.setOnCloseRequest(e -> {
			e.consume();
			watcher.interrupt();
			try {
				socket.leaveGroup(groupIP);
			} catch (Exception ex) { System.out.println("Failed to leave the group!"); }
			
			// say everyone that you're leaving, and they will update their allUsers list
			try {
				if(allUsers.size() > 1) socket.send(GOODBYE, me);
			} catch (Exception ex) { System.out.println("Failed to send GOODBYE message!"); }
			
			window.close();
			System.exit(0);
		});	
		
		VBox leftVBox = new VBox(5);
		
		btnClear.setStyle("-fx-font-family: Arial; " +
				"-fx-text-fill: #005872; " + 
				"-fx-border-color: #005872; " +
				"-fx-font-size: 11pt;");
		btnClear.setPrefSize(145, 30);
		
		btnClear.setOnAction(e -> {
			if(allUsers.size() > 1) socket.send(CLEAR_CANVAS, me);
			else {
				gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                logStack.getChildren().add(new Label("Board has been cleared!"));
			}
		});
		
		strokeSlider.setPrefWidth(145);
		VBox sliderContainer = new VBox();
		sliderContainer.setAlignment(Pos.CENTER);
		sliderContainer.getChildren().addAll(new Label("Stroke Width"), strokeSlider);
		sliderContainer.setStyle("-fx-background-color: rgb(200,200,200);");
		leftVBox.getChildren().addAll(logPane, sliderContainer, btnClear);
		
        root.setCenter(canvas);
        root.setRight(leftVBox);
        
        scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        window.setScene(scene);
        window.show();
	}
	
	
	public void Painting(){
		canvas.setOnMousePressed((MouseEvent e) -> {
        	gc.beginPath();
			gc.moveTo(e.getX(), e.getY());
			socket.send(ClientUDP.START_DRAW, new point2d(e.getX(), e.getY(), ClientUDP.me).setStroke(current_stroke_width));
        });
		
		canvas.setOnMouseDragged((MouseEvent e) -> {
			gc.lineTo(e.getX(), e.getY());
			socket.send(ClientUDP.DRAW, new point2d(e.getX(), e.getY(), ClientUDP.me));
        });
        
		canvas.setOnMouseReleased((MouseEvent e) -> socket.send(ClientUDP.END_DRAW, null));
	}
	
	
	public static void main(String[] args) throws IOException{
		me = new User();
	//	me.setColor(new Color(Math.random(), Math.random(), Math.random(), 1));
		gc.setStroke(me.color);
		groupIP = InetAddress.getByName("228.5.6.7");
		socket = new CastingSocket(groupIP, PORT);
		submission();
		
		launch(args);
	}
	
	@SuppressWarnings("unchecked")
	public static void submission(){
		socket.send(I_AM_NEW, null);	// introduce yourself
		Object obj = null;
		int[] type = new int[1];
		long endTimeMillis = System.currentTimeMillis() + 2000;

		while(true){			
			if(System.currentTimeMillis() > endTimeMillis) break; // break after 2 second
			try {
				obj = socket.receive(type);
				if(type[0] != GREETINGS) continue;
				else{
					allUsers = (ArrayList<User>)obj;
					break;
				}
			} catch (ClassNotFoundException | IOException e) { System.out.println("Failed to receive messages in Submission phase!"); }
		} // end while
		
		if(allUsers.isEmpty()) {
			System.out.println("User 0: first in the session!");
			me.setID(0);
		}
		else me.setID(allUsers.size()); // next max ID
		
		//choose unique color
		Color myColor = new Color(Math.random(), Math.random(), Math.random(), 1);
		for(int i = 0; i < allUsers.size(); i++){
			if(isColorSimilar(myColor, allUsers.get(i).color)){
				i = -1;
				// try another color
				myColor = new Color(Math.random(), Math.random(), Math.random(), 1);
			}
		}
		me.setColor(myColor);
		
		allUsers.add(me);
		for(User u : allUsers) System.out.println("allUsers[] = " + u.ID);
		socket.send(SUBMIT_ME, me);
	}
	
	private static boolean isColorSimilar(Color mine, Color notMine){
		boolean result = false;
		// it is OK if at least one of the RGB value is +-0.2 from the other color's values
		result = ((mine.getRed() < (notMine.getRed()-0.2)) || (mine.getRed() > (notMine.getRed()+0.2)) ||
				(mine.getGreen() < (notMine.getGreen()-0.2)) || (mine.getGreen() > (notMine.getGreen()+0.2)) ||
				(mine.getBlue() < (notMine.getBlue()-0.2)) || (mine.getBlue() > (notMine.getBlue()+0.2))) ? false : true;
		
		return result;
	}

}
