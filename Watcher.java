import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.*;

public class Watcher extends Thread{
	CastingSocket sock;
	InetAddress groupIP;
	int port;
	
	public Watcher(CastingSocket sock, InetAddress groupIP, int port){
		super("Watcher");
		this.sock = sock;
		try {
			this.sock.setBroadcast(true);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		this.groupIP = groupIP;
		this.port = port;
	}
	
	public void run(){
			int count = 0; // for catching first point of a line 
		//	try {
				
				Object resp = null;
				int[] type = new int[1];
				
				while(true){
				try{
					resp = sock.receive(type);
				
				//	if(type[0] != ClientUDP.DRAW && resp == null) break;
					System.out.println("Message type: " + type[0]);
					switch(type[0]){
						case ClientUDP.START_DRAW:
							point2d start_point = (point2d)resp;
							if(start_point != null){
								Platform.runLater(new Runnable() {
							         @Override
							         public void run() {
										ClientUDP.gc.beginPath();
										ClientUDP.gc.moveTo(start_point.x, start_point.y);
										ClientUDP.gc.setStroke(start_point.user.color);
										ClientUDP.gc.setLineWidth(start_point.width);
						             }
							    });
							}
							break;
						
						case ClientUDP.DRAW:
							point2d point = (point2d)resp;
							Platform.runLater(new Runnable() {
						         @Override
						         public void run() {
									ClientUDP.gc.lineTo(point.x, point.y);
									ClientUDP.gc.stroke();
						         }
						    });
							break;
							
						case ClientUDP.END_DRAW:
							break;
					
						case ClientUDP.I_AM_NEW:
							User user1 = (User)resp;
							System.out.println("Greetings from: " + ClientUDP.me.ID);
							sock.cast(ClientUDP.GREETINGS, ClientUDP.allUsers);
							break;

						case ClientUDP.SUBMIT_ME:
							User newUser = (User)resp;
							System.out.println(newUser.color.getBlue());
							System.out.println("FOR SUBMISSION: " + newUser.ID);
							if(newUser.ID != ClientUDP.me.ID){ 
								ClientUDP.allUsers.add(newUser);
							
								Platform.runLater(new Runnable() {
					                @Override
					                public void run() {
					                   HBox container = new HBox(7);
					                   Circle icon = new Circle(9);
					                   icon.setFill(newUser.color);
					                   Label label = new Label("just connected");
					                   container.getChildren().addAll(icon, label);
					                   ClientUDP.logStack.getChildren().add(container);
					                }
								});
							}
							break;
						
						case ClientUDP.CLEAR_CANVAS:
							User initiator = (User)resp;
							ClearCanvasHandler(initiator);
							break;
							
						case ClientUDP.CLEAR_CANVAS_RESPONSE:
							processDecissionsForCLEAR_CANVAS((Boolean)resp);
							break;
							
						case ClientUDP.GOODBYE:
							User byeUser = (User)resp;
							Goodbye(byeUser);
							break;
						
						default: System.out.println("default");
							break;
							
					} // switch end
				}
				catch(IOException ex){ System.out.println("Failed to receive datagram in Watcher thread!"); } 
				catch (ClassNotFoundException e) { System.out.println("Failed to send datagram in Watcher thread!"); } 
				catch(NullPointerException e) {System.out.println("Null object casted!");}
			}// while end
	} // end run()

	
	private void Goodbye(User aboutToLeave){
            ClientUDP.allUsers.remove(aboutToLeave);
			Platform.runLater(new Runnable() {
                @Override
                public void run() {
                   HBox container = new HBox(5);
                   Circle icon = new Circle(9);
                   icon.setFill(aboutToLeave.color);
                   container.getChildren().addAll(icon, new Label("disconnected :("));
                   ClientUDP.logStack.getChildren().add(container);                  
                }
            });
	}
	
	private void ClearCanvasHandler(User initiator){
			Platform.runLater(new Runnable() {
                @Override
                public void run() {
                   HBox container = new HBox(5);
                   Circle icon = new Circle(9);
                   try{
                   if(initiator.ID == ClientUDP.me.ID){
                	   icon.setFill(ClientUDP.me.color);
                	   container.getChildren().addAll(icon, new Label("I want a reset"));
                   }
                   else{
                	   icon.setFill(initiator.color);
                	   container.getChildren().addAll(icon, new Label("wants a reset"));
                   }
                   ClientUDP.logStack.getChildren().add(container);
                   }catch(NullPointerException ex){System.out.println(ex.getMessage());}
                }
            });
			System.out.println("Me: " + ClientUDP.me.ID);
			System.out.println("Clear Initiator: " + initiator.ID);
			if(ClientUDP.me.ID != initiator.ID) 
				decisionMaking(initiator.color);
	}
	
	
	void decisionMaking(Color userColor){
		Platform.runLater(new Runnable() {
            @Override
            public void run() {
               Stage decisionWindow = new Stage();               
               VBox decisionLayout = new VBox(5);
               Scene decisionScene = new Scene(decisionLayout, 200, 100);
               
               // position the decision window in the middle of the parrent window
               decisionWindow.setX(ClientUDP.window.getX() + ClientUDP.window.getWidth()/2  - decisionScene.getWidth()/2);
               decisionWindow.setY(ClientUDP.window.getY() + ClientUDP.window.getHeight()/2 - decisionScene.getHeight()/2);
               
               HBox container1 = new HBox(5);
               container1.setAlignment(Pos.CENTER);
   			   Circle icon = new Circle(10);
   			   icon.setFill(userColor);
               container1.getChildren().addAll(icon, new Label("initiated Canvas Clear"));
               
               HBox container2 = new HBox(5);
               Button btnYES = new Button("YES");
               Button btnNO = new Button("NO");
               
               btnYES.setOnAction(e -> {
					sock.cast(ClientUDP.CLEAR_CANVAS_RESPONSE, new Boolean(true));
            	   decisionWindow.close();
               });
               
               btnNO.setOnAction(e -> {
            	   sock.cast(ClientUDP.CLEAR_CANVAS_RESPONSE, new Boolean(false));
            	   decisionWindow.close();
               });
               
               container2.setAlignment(Pos.CENTER);
               container2.getChildren().addAll(btnYES, btnNO);
               decisionLayout.setAlignment(Pos.CENTER);
               decisionLayout.getChildren().addAll(container1, new Label("Do you agree with this user?"), container2);
               decisionWindow.setScene(decisionScene);
               decisionWindow.showAndWait();
            }
        });
	}
	
	
	private void processDecissionsForCLEAR_CANVAS(Boolean resp){
			int decission = 0;
			ArrayList<Boolean> answers = new ArrayList<Boolean>();
			answers.add(new Boolean(true)); // initiator voted YES automatically
			answers.add(resp);
			
			int[] type = new int[1];
			while(true){
				if(answers.size() == ClientUDP.allUsers.size()) break;
				type[0] = -1;
				try {
					Object obj = sock.receive(type);
					if(type[0] != ClientUDP.CLEAR_CANVAS_RESPONSE) {System.out.println("Not a CLEAR_CANVAS_RESPONSE! Skipped!"); continue;}
					else answers.add((Boolean)obj);
				} catch (ClassNotFoundException | IOException e) { System.out.println("Failed to get Clear Canvas Response"); }
			}
			
			for(Boolean b : answers) if(b.booleanValue()) decission++;	// counts the YES votes
			
			if(decission > (ClientUDP.allUsers.size() / 2)){ 
				Platform.runLater(new Runnable() {
	                @Override
	                public void run() {
	                   ClientUDP.gc.clearRect(0, 0, ClientUDP.CANVAS_WIDTH, ClientUDP.CANVAS_HEIGHT);
	                   ClientUDP.logStack.getChildren().add(new Label("Board has been cleared!"));
	                }
	            });
			}
	}
}
