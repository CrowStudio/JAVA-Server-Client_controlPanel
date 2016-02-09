/************************************************************************
	ClientGUI is the client for ServerGUI.
	This software, combined with ServerGUI, is a remote control that is 
	meant to turn ON/OFF LEDs on a Raspberry Pi. However, at the moment
	it only runs as a visual interface on a PC.
	
	Copyright (C) 2016  Daniel Arvidsson & Johanna Baecklund

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
	
	ClientGUI Copyright (C) 2016  Daniel Arvidsson & Johanna Baecklund
	This program comes with ABSOLUTELY NO WARRANTY!!!
	This is free software, and you are welcome to redistribute it, but
	please credit the authors.
	
	Contact: Daniel Arvidsson:	daniel.arvidsson@fripost.org
		Johanna Baecklund:	j.baecklund@gmail.com

************************************************************************
	Things to do:
	
	On server: low-level GPIO communication:
	It is possible to run the ServerGUI from Raspberry PI to control 
	LEDs or other things connected to the GPIO. However some further 
	implementation of the library PI4J and some change of code is 
	needed. The PI4J library can be found here: 
	<http://pi4j.com/example/control.html>
	
	On server: print name of disconnected client.
	
	On client: map switches to keyboard:
	Make it possible for the client to switch LEDs with addressed
	buttons on keyboard.
	
	On client: server status indicator in GUI:
	Add an icon to indicate if server is online or offline. If server
	is offline, the client should try X times to connect.
	
	
			======== Known bugs: ========
	
	General bug 1.1:	Lack of UTF8 encoding when using cross
				platforms.
	
	Symptoms:		Weird ASCII signs when writing å, ä, ö.
	
	Suggested solution:	Forced UTF8 encoding when sending strings.
	
	
	ServerGUI bug 2.1:	If disconnecting a client, the server still 
				keeps the client in memory.
						
	Symptoms: 		The server keeps printing "Sent to: 
				<disconnected client>". Probably also tries
				to send the message to the disconnected client.
						
	Suggested solution: 	Implementing deletion of client information
				when disconnecting.

************************************************************************
	Warning! Due to lack of secure protocol, only run on local network!
	
	How to use:
	First - From host-unit start the ServerGUI from terminal, enter:
			$ java ServerGUI
	
	Second - On remote-unit start the ClientGUI from terminal, enter:
			$ java ClientGUI <name>
		
***********************************************************************/
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;


// ClientGUI class - initialize variables and graphical elements 
class ClientGUI extends JFrame implements ActionListener
{
	// Variables to set IP and PORT on ServerGUI
	public static String IP = "localhost";
	public static int PORT = 8000;
	
	// Ignore! Actually only needed when serializing, we do not use that for the moment! 
	private static final long serialVersionUID = 6944207967525678757L;
	
	// Imports switch icons
	Icon on  = new ImageIcon(getClass().getResource("on.png"));
	Icon off = new ImageIcon(getClass().getResource("off.png"));
	
	// Imports LED icons
	Icon LEDoff		= new ImageIcon(getClass().getResource("LEDoff.png"));
	Icon LEDred		= new ImageIcon(getClass().getResource("LEDred.png"));
	Icon LEDgreen	= new ImageIcon(getClass().getResource("LEDgreen.png"));
	Icon LEDyellow	= new ImageIcon(getClass().getResource("LEDyellow.png"));
	
	// Creates and initiates JButtons - switches
	private JButton red		= new JButton("Red", off);
	private JButton green	= new JButton("Green", off);
	private JButton yellow	= new JButton("Yellow", off);
	
	// Creates and initiates JLabels - LEDs
	private JLabel redLED		= new JLabel(LEDoff);
	private JLabel greenLED		= new JLabel(LEDoff);
	private JLabel yellowLED	= new JLabel(LEDoff);
	
	// Declaration of variables
	public int redState, greenState, yellowState;
	
	public String client, message, serverString, clientName = "";
	
	public BufferedReader	echoServer;
	public PrintWriter		writeToServer;
	
	public Socket socket;
	
	
	// ClientGUI constructor - adds graphic and displays window
	public ClientGUI()
	{
		// Sets window layout - grid style, 2 rows and 3 columns 
		setLayout(new GridLayout(2,3));
		
		// Adds LEDs
		add(redLED);
		add(yellowLED);
		add(greenLED);
		
		// Adds switches
		add(red);
		add(yellow);
		add(green);
		
		// Sets background color
		getContentPane().setBackground(Color.white);
		
		// Creates action listeners for switches
		red.addActionListener(this);
		green.addActionListener(this);
		yellow.addActionListener(this);
		
		// Placement of window - setBounds(placementX, placementY, sizeX, sizeY)
		setBounds(0, 200, 630, 420);
		
		// Makes window visible (false = not visible)
		setVisible(true);
		
		// Exit program when closing window
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	// Socket method - initialize Socket, receives client name, sends client name to server and sets current LED states
	public void initSocket(String clientName) throws IOException
	{
		// Creates new socket - Socket("IP address", Port)
		socket = new Socket(IP, PORT);
		
		// Declaration of variables
		// BufferedReader = reads data from server and PrintWriter = prints data to server
		echoServer		= new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writeToServer	= new PrintWriter(socket.getOutputStream(), true);
		
		// Writes client name to server
		writeToServer.println(clientName);
		
		// For-loop that initializes LEDs and switches
		for (int i=0; i<3; i++)
		{
			// Reads LEDstate 1-3 (i) from server
			message = echoServer.readLine();
			
			switch (message)
			{
			// If message = '1' - set redState, lit red LED, set red switch to ON, print current state to terminal
			case "1":	redState = 1;
						redLED.setIcon(LEDred);
						red.setIcon(on);
						System.out.println("Current Red value:    ON");
						break;
			// If message = '0' - set redState, turn off red LED, set red switch to OFF, print current state to terminal
			case "0":	redState = 0;
						redLED.setIcon(LEDoff);
						red.setIcon(off);
						System.out.println("Current Red value:    OFF");
						break;
			// If message = '3' - set greenState, lit green LED, set green switch to ON, print current state to terminal
			case "3":	greenState = 3;
						greenLED.setIcon(LEDgreen);
						green.setIcon(on);
						System.out.println("Current Green value:  ON");
						break;
			// If message = '2' - set greenState, turn off green LED, set green switch to OFF, print current state to terminal
			case "2":	greenState = 2;
						greenLED.setIcon(LEDoff);
						green.setIcon(off);
						System.out.println("Current Green value:  OFF");
						break;
			// If message = '5' - set yellowState, lit yellow LED, set yellow switch to ON, print current state to terminal
			case "5":	yellowState = 5;
						yellowLED.setIcon(LEDyellow);
						yellow.setIcon(on);
						System.out.println("Current Yellow value: ON\n");
						break;
			// If message = '4' - set redState, turn off yellow LED, set yellow switch to OFF, print current state to terminal
			case "4":	yellowState = 4;	
						yellowLED.setIcon(LEDoff);
						yellow.setIcon(off);
						System.out.println("Current Yellow value: OFF\n");
						break;
			}
		}
	}
	
	// Action handler method - receives event if switch is clicked, sets switch position and sends state to server
	public void actionPerformed(ActionEvent event)
	{
		// If red switch is clicked
		if (event.getSource() == red)
		{
			// Sets redState, sets red switch ON
			if (redState == 0)
			{
				redState = 1;
				red.setIcon(on);
			}
			
			// Sets redState, sets red switch OFF
			else
			{
				redState = 0;
				red.setIcon(off);
			}
			// Writes redState and clientName to server
			writeToServer.println(redState + clientName);
		}
		
		// If green switch is clicked
		else if (event.getSource() == green)
		{
			// Sets greenState, sets green switch ON
			if (greenState == 2)
			{
				greenState = 3;
				green.setIcon(on);
			}
			
			// Sets greenState, sets green switch OFF
			else
			{
				greenState = 2;
				green.setIcon(off);
			}
			
			// Writes greenState and clientName to server
			writeToServer.println(greenState + clientName);
		}
		
		// If yellow switch is clicked
		else if (event.getSource() == yellow)
		{
			// Sets yellowState, sets yellow switch ON
			if (yellowState == 4)
			{
				yellowState = 5;
				yellow.setIcon(on);
			}
			
			// Sets yellowState, sets yellow switch OFF
			else
			{
				yellowState = 4;
				yellow.setIcon(off);
			}
			
			// Writes yellowState and clientName to server
			writeToServer.println(yellowState + clientName);
		}
	}
	
	
	// Main program - reads client name from terminal input, starts ClientGUI and connects to server
	public static void main(String[] args) throws IOException
	{
		// If program is started correct from terminal
		if (args.length == 1)
		{
			// User-set IP number from input dialog
			String input = JOptionPane.showInputDialog("Enter server IP number:\n<html><i>(Default IP = localhost)</i></html>");
			// If user pressed OK or Cancel
			if (input.equals("") || input.equals(null));
			else IP = input;
			
			// User-set PORT number from input dialog
			input = JOptionPane.showInputDialog("Enter server PORT number:\n<html><i>(Default PORT = 8000)</i></html>");
			// If user pressed OK or Cancel
			if (input.equals("") || input.equals(null));
			else PORT = Integer.parseInt(input);
			
			// Print current IP and PORT number to terminal
			if (!IP.equals("localhost") && PORT != 8000) System.out.println("\nIP: " + IP + "\nPORT: " + PORT + "\n");
			else if (!IP.equals("localhost") && PORT == 8000) System.out.println("\nIP: " + IP + "\nDefault PORT: " + PORT + "\n");
			else if (IP.equals("localhost") && PORT != 8000) System.out.println("\nDefault IP: " + IP + "\nPORT: " + PORT + "\n");
			else System.out.println("\nDefault IP:   " + IP + "\nDefault PORT: " + PORT + "\n");
			
			// Initializes ClientGUI object
			ClientGUI startGUI = new ClientGUI();
			
			// Stores client name entered in terminal
			startGUI.clientName = args[0];
			
			// Error handling - try this if server is online
			try
			{
				// Connects to server
				startGUI.initSocket(startGUI.clientName);
				
				// Main while-loop - reads LED state from server, sets LED state, turn LED ON/OFF,
				// sets switch position and prints LED state to terminal
				while((startGUI.message = startGUI.echoServer.readLine()) != null)
				{
					switch (startGUI.message)
					{
					// If message = '1' - set redState, lit red LED, set red switch to ON, print current state to terminal
					case "1":	startGUI.redState = 1;
								startGUI.redLED.setIcon(startGUI.LEDred);
								startGUI.red.setIcon(startGUI.on);
								System.out.println("Red value:    ON");
								break;
					// If message = '0' - set redState, turn off red LED, set red switch to OFF, print current state to terminal
					case "0":	startGUI.redState = 0;
								startGUI.redLED.setIcon(startGUI.LEDoff);
								startGUI.red.setIcon(startGUI.off);
								System.out.println("Red value:    OFF");
								break;
					// If message = '3' - set greenState, lit green LED, set green switch to ON, print current state to terminal
					case "3":	startGUI.greenState = 3;
								startGUI.greenLED.setIcon(startGUI.LEDgreen);
								startGUI.green.setIcon(startGUI.on);
								System.out.println("Green value:  ON");
								break;
					// If message = '2' - set greenState, turn off green LED, set green switch to OFF, print current state to terminal
					case "2":	startGUI.greenState = 2;
								startGUI.greenLED.setIcon(startGUI.LEDoff);
								startGUI.green.setIcon(startGUI.off);
								System.out.println("Green value:  OFF");
								break;
					// If message = '5' - set yellowState, lit yellow LED, set yellow switch to ON, print current state to terminal
					case "5":	startGUI.yellowState = 5;	
								startGUI.yellowLED.setIcon(startGUI.LEDyellow);
								startGUI.yellow.setIcon(startGUI.on);
								System.out.println("Yellow value: ON");
								break;
					// If message = '4' - set redState, turn off yellow LED, set yellow switch to OFF, print current state to terminal
					case "4":	startGUI.yellowState = 4;	
								startGUI.yellowLED.setIcon(startGUI.LEDoff);
								startGUI.yellow.setIcon(startGUI.off);
								System.out.println("Yellow value: OFF");
								break;
					}
				}

				// Linux specific - if ServerGUI is offline or shuts down, shut off all clients
				// (Mac - if ServerGUI shuts down)
				System.out.println("\nServer offline!\n");
				System.exit(1);
			}
			
			
			// Windows specific - if ServerGUI is offline or shuts down, shut off all clients 
			// (Mac - if ServerGUI is offline when trying to connect a client)
			catch (IOException e)
			{
				System.out.println("\nServer offline!\n");
				System.exit(1);
			}
		}
		
		// Else if program is started incorrectly, print how to use
		else
		{
			System.out.println("How to use: $ java ClientGUI <name>");
		}
	}
}
