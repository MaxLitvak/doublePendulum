import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.awt.Button;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class doublePendulum implements Runnable {

	final int WIDTH = 800;
	final int HEIGHT = 700;
	JFrame frame;
	Canvas canvas;
	BufferStrategy bufferStrategy;

	private double a1 = Math.toRadians(90); // starting theta1
	private double a2 = Math.toRadians(90); // starting theta2
	private int l1 = 150; // length of string1 in meters
	private int l2 = 150; // length of string2 in meters
	private int m1 = 2; // mass of ball1 in kilograms
	private int m2 = 2; // mass of ball2 in kilograms
	private double g = 9.81; // graviational acceleration
	private int[] origin = {WIDTH/2, 300}; // position of origin [x,y]
	private double[] ball1 = ball1Pos(); // position of ball1 [x,y]
	private double[] ball2 = ball2Pos(); // position of ball2 [x,y]
	private double ball1Vel = 0; // starting velocity for ball1
	private double ball2Vel = 0; // starting velocity for ball2
	private double ball1Acc = 0; // starting acceleration for ball1
	private double ball2Acc = 0; // starting acceleration for ball2
	private double t = 0; // time
	private double h = 0.025; // delta time
	private int showTrace = 0; // when to show balls' paths
	private ArrayList<double[]> ball1Points = new ArrayList<double[]>(); // history of where ball1 has been
	private ArrayList<double[]> ball2Points = new ArrayList<double[]>(); // history of where ball2 has been
	private int speed = 1; // speed of simulation
	/*Use tihis line to switch between euler and RK4*/ private boolean isEuler = true; // set equal to true for euler and false for RK4

	public static void main(String[] args) {
		doublePendulum ex = new doublePendulum();
		new Thread(ex).start();
	}

	public doublePendulum() {
		// set up frame to show simulation
		frame = new JFrame("Pendulum simulation");

		JPanel panel = (JPanel) frame.getContentPane();
		panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		panel.setLayout(new BorderLayout());

		canvas = new Canvas();
		canvas.setBounds(0, 0, WIDTH, HEIGHT);
		canvas.setIgnoreRepaint(true);

		panel.add(canvas);

		Button stop = new Button("Stop and show paths");
		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { 
		    showTrace = 1;
		  }
		});
		panel.add(stop, BorderLayout.SOUTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setResizable(false);
		frame.setVisible(true);

		canvas.createBufferStrategy(2);
		bufferStrategy = canvas.getBufferStrategy();

		canvas.requestFocus();
	}

	public void run() {
		while (true) {
			if (showTrace != 2) {
				render();
			}
			double[] ball1Vals = calcBall1(); // calculate new values with the previous ones as inputs
			double[] ball2Vals = calcBall2(); // calculate new values with the previous ones as inputs
			// assign values
			ball1Acc = ball1Vals[0];
			ball1Vel = ball1Vals[1];
			a1 = ball1Vals[2];
			ball2Acc = ball2Vals[0];
			ball2Vel = ball2Vals[1];
			a2 = ball2Vals[2];
			ball2 = ball2Pos(); // set new position
			ball1 = ball1Pos(); // set new position
			t += h;
			try {
				Thread.sleep(speed);
			} catch (InterruptedException e) {}
		}
	}

	private void render() {
		// draw the simulation
		Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
		g.clearRect(0, 0, WIDTH, HEIGHT);
		// draw lines
		g.drawLine(origin[0]-200, origin[1], origin[0]+200, origin[1]);
		g.drawLine(origin[0], origin[1], (int)ball1[0], (int)ball1[1]);
		g.drawLine((int)ball1[0], (int)ball1[1], (int)ball2[0], (int)ball2[1]);
		// draw balls
		int radius = 20;
		g.setColor(Color.BLUE);
		g.fillOval((int)ball1[0]-radius, (int)ball1[1]-radius, 2*radius, 2*radius);
		g.setColor(Color.ORANGE);
		g.fillOval((int)ball2[0]-radius, (int)ball2[1]-radius, 2*radius, 2*radius);
		// draw timer
		g.setColor(Color.BLACK);
		g.setFont(new Font("", Font.PLAIN, 20)); 
		g.drawString(String.valueOf((int)t)+" seconds", WIDTH-200, HEIGHT-100);
		// draw the paths
		if (showTrace == 1) {
			for (int x=0;x<ball1Points.size();x++) {
				int pointRadius = 1;
				g.setColor(Color.BLUE);
				g.fillOval((int)ball1Points.get(x)[0]-pointRadius, (int)ball1Points.get(x)[1]-pointRadius, 2*pointRadius, 2*pointRadius);
				g.setColor(Color.ORANGE);
				g.fillOval((int)ball2Points.get(x)[0]-pointRadius, (int)ball2Points.get(x)[1]-pointRadius, 2*pointRadius, 2*pointRadius);
				showTrace = 2;
			}
			// writeToFile("1.txt", ball1Points);
			// writeToFile("2.txt", ball2Points);
		}
		g.dispose();
		bufferStrategy.show();
	}

	private double[] ball1Pos() {
		double x = origin[0]+(l1*Math.sin(a1));
		double y = origin[1]+(l1*Math.cos(a1));
		if (ball1Points != null) {
			ball1Points.add(new double[] {x,y,t});
		}
		return new double[]{x,y,t};	// new ball1 position
	}

	private double[] ball2Pos() {
		double x = ball1[0]+(l2*Math.sin(a2));
		double y = ball1[1]+(l2*Math.cos(a2));
		if (ball2Points != null) {
			ball2Points.add(new double[] {x,y,t});
		}
		return new double[]{x, y};	// new ball2 position
	}

	private double[] calcBall1() {
		double newAcc = calcAcc1(a1,a2,ball1Vel,ball2Vel);
		double[] newVals;
		if (isEuler) {
			newVals = euler(newAcc,ball1Vel,a1,h);
		} else {
			newVals = RK4(true);
		}
		return new double[] {newVals[0],newVals[1],newVals[2]};
	}

	private double calcAcc1(double angle1,double angle2,double vel1,double vel2) {
		return -(-(g)*(2*m1+m2)*Math.sin(angle1)-m2*g*Math.sin(angle1-2*angle2)-2*Math.sin(angle1-angle2)*m2*(Math.pow(vel2,2)*l2+Math.pow(vel1,2)*l1*Math.cos(angle1-angle2)))/(l1*(2*m1+m2-m2*Math.cos(2*angle1-2*angle2)));
	}

	private double[] calcBall2() {
		double newAcc = calcAcc2(a1,a2,ball1Vel,ball2Vel);
		double[] newVals;
		if (isEuler) {
			newVals = euler(newAcc,ball2Vel,a2,h);
		} else {
			newVals = RK4(false);
		}
		return new double[] {newVals[0],newVals[1],newVals[2]};
	}

	private double calcAcc2(double angle1,double angle2,double vel1,double vel2) {
		return -(2*Math.sin(angle1-angle2)*(Math.pow(vel1,2)*l1*(m1+m2)+g*(m1+m2)*Math.cos(a1)+Math.pow(vel2,2)*l2*m2*Math.cos(angle1-angle2)))/(l2*(2*m1+m2-m2*Math.cos(2*angle1-2*angle2)));
	}

	// euler method
	private double[] euler(double acc, double vel, double theta, double step) {
		double newVel = vel+acc*h;
		double newTheta = theta-vel*h;
		return new double[] {acc, newVel,newTheta};
	}

	// runge-kutta method - not sure if this is correct
	public double[] RK4(boolean accFn) {
		List<Double> elems = new ArrayList<Double>();
		elems.add(a1);
		elems.add(a2);
		elems.add(ball1Vel);
		elems.add(ball2Vel);
		Vector<Double> vec = new Vector<Double>(elems);
    Vector<Double> k1 = func(t, vec, h, l1, l2, m1, m2);        
    Vector<Double> k2 = func(t + (h/13), vectorSum(vec,(vectorX(constVec(h/13),k1))), h, l1, l2, m1, m2);
    Vector<Double> k3 = func(t + (2*h/13), vectorSum(vectorDiff(vec,(vectorX(constVec(h/13),k1))),vectorX(constVec(h),k2)), h, l1, l2, m1, m2);
    Vector<Double> k4 = func(t + h, vectorSum(vectorDiff(vectorSum(vec,(vectorX(constVec(h),k1))),vectorX(constVec(h),k2)),vectorX(constVec(h),k3)), h, l1, l2, m1, m2);
    // optimal equation from https://physics-labs.com/coding/chaotic-double-pendulum/
    Vector newVals = vectorSum(vec,vectorX(constVec(h/18),(vectorSum(vectorSum(k1,vectorX(k2,constVec(3))),vectorSum(vectorX(k3,constVec(3)),k4)))));
		if (accFn) {
			double newTheta = a1-(double)newVals.get(2);
			return new double[]{(double)newVals.get(0),(double)newVals.get(2),newTheta};
		} else {
			double newTheta = a2-(double)newVals.get(3);
			return new double[]{(double)newVals.get(1),(double)newVals.get(3),newTheta};
		}
	}

	public Vector<Double> func(double t, Vector<Double> x, double h, double l1, double l2, double m1, double m2) {
    List<Double> elems = new ArrayList<Double>();
		elems.add((double)x.get(2));
		elems.add((double)x.get(3));
		elems.add(calcAcc1((double)x.get(0),(double)x.get(1),(double)x.get(2),(double)x.get(3)));
		elems.add(calcAcc2((double)x.get(0),(double)x.get(1),(double)x.get(2),(double)x.get(3)));
    Vector<Double> dxdt = new Vector(elems);

    return dxdt;
	}

	private Vector<Double> vectorX(Vector<Double> vec1, Vector<Double> vec2) {
		List<Double> elems = new ArrayList<Double>();
		for (int x=0;x<4;x++) {
			double val = vec1.get(x)*vec2.get(x);
			elems.add(val);
		}
		return new Vector<Double>(elems);
	}

	private Vector<Double> vectorSum(Vector<Double> vec1, Vector<Double> vec2) {
		List<Double> elems = new ArrayList<Double>();
		for (int x=0;x<4;x++) {
			double val = vec1.get(x)+vec2.get(x);
			elems.add(val);
		}
		return new Vector<Double>(elems);
	}

	private Vector<Double> vectorDiff(Vector<Double> vec1, Vector<Double> vec2) {
		List<Double> elems = new ArrayList<Double>();
		for (int x=0;x<4;x++) {
			double val = vec1.get(x)-vec2.get(x);
			elems.add(val);
		}
		return new Vector<Double>(elems);
	}

	private Vector<Double> constVec(double val) {
		List<Double> elems = new ArrayList<Double>();
		for (int x=0;x<4;x++) {
			elems.add(val);
		}
		return new Vector<Double>(elems);
	}

	private void writeToFile(String name, ArrayList<double[]> arr) {
		try {
      File myObj = new File(name);
      if (myObj.createNewFile()) {
        try {
		      FileWriter myWriter = new FileWriter(name);
		      myWriter.write("[");
		      for (int x=0;x<arr.size();x++) {
						myWriter.write("["+String.valueOf(arr.get(x)[0])+","+String.valueOf(arr.get(x)[1])+","+String.valueOf(arr.get(x)[2])+"]");
						if (x+1 < arr.size()) {
							myWriter.write(",");
						}
		      }
		      myWriter.write("]");
		      //
		      myWriter.close();
		    } catch (IOException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		    }
      }
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
	}
}