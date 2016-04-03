import java.io.Serializable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class point2d implements Serializable{
	private static final long serialVersionUID = 1L; // that's some mandatory stuff for Serializable implementation
	double x;
	double y;
	double width;
	User user; // who sends the point
	
	public point2d(double x, double y, User user){
		this.x = x;
		this.y = y;
		this.width = 1;
		this.user = user;
	}
	
	public point2d setStroke(double width){ 
		this.width = width; 
		return this; 
	}
}