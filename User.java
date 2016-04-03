import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class User implements Serializable{
	
	private static final long serialVersionUID = 1L;
	public int ID;
	Color color;

	public User(){
		this.ID = -1; // unsubmitted user
		this.color = new Color(0,0,0,1); // black, by default
	}
	
	public void setID(int ID){ this.ID = ID; }
	public void setColor(Color color) { this.color = color; }
	
	// implementing Serializable interface
	private void writeObject(final ObjectOutputStream out) throws IOException {  
	      out.writeInt(this.ID);  
	      out.writeDouble(this.color.getRed());
	      out.writeDouble(this.color.getGreen());
	      out.writeDouble(this.color.getBlue());
	      out.writeDouble(this.color.getOpacity());
	}  
	   
	private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {  
	      this.ID = in.readInt();
		  this.color = new Color(in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble());  
	}  
}
