/************************************************************************
	ServerGUI is the server for ClientGUI. 
	This software, combined with ClientGUI, is a server that is meant 
	turn ON/OFF LEDs on its host, a Raspberry Pi. However, at the moment 
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
	
	ServerGUI Copyright (C) 2016  Daniel Arvidsson & Johanna Baecklund
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
	
	Symptoms:			Weird ASCII signs when writing å, ä, ö.
	
	Suggested solution:	Forced UTF8 encoding when sending strings.
	
	
	ServerGUI bug 2.1:	If disconnecting a client, the server still 
						keeps the client in memory.
						
	Symptoms: 			The server keeps printing "Sent to: 
						<disconnected client>". Probably also tries
						to send the message to the disconnected client.
						
	Suggested solution: Implementing deletion of client information
						when disconnecting.

************************************************************************
	Warning! Due to lack of secure protocol, only run on local network!
	
	How to use:
	First - From host-unit start the ServerGUI from terminal, enter:
			$ java ClientGUI <name>
	
	Second - In ClinetGUI you have to make a change in the public class
	ClientGUI and enter IP address of the host-server and the PORT used.
	
	Third - On remote-unit start the ClientGUI from terminal, enter:
			$ java ClientGUI <name>

***********************************************************************/
import java.awt.Color;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;


// ServerGUI class - initialize variables and graphical elements 
class ServerGUI extends JFrame
{
	// Variable to set PORT used to accept clients
	public static int PORT = 8000;
	
	// Ignore! Actually only needed when serializing, we do not use that for the moment! 
	private static final long serialVersionUID = 6838516569816488043L;
	
	// Imports LED icons
	Icon LEDoff		= new ImageIcon(getClass().getResource("LEDoff.png"));
	Icon LEDred		= new ImageIcon(getClass().getResource("LEDred.png"));
	Icon LEDgreen	= new ImageIcon(getClass().getResource("LEDgreen.png"));
	Icon LEDyellow	= new ImageIcon(getClass().getResource("LEDyellow.png"));
	
	// Creates and initiates JLabels - LEDs
	private JLabel redLED		= new JLabel(LEDoff);
	private JLabel greenLED		= new JLabel(LEDoff);
	private JLabel yellowLED	= new JLabel(LEDoff);
	
	// Declaration of variables
	public String redState = "1", greenState = "3", yellowState = "5", initLED = "", user;
	
	public PrintWriter		writeToFile,	currentClient;
	public BufferedReader	readFromFile,	clientInput;
	public Socket			currentSock,	socket;
	
	public static ArrayList<Socket> userConnections = new ArrayList<Socket>();
	public static ArrayList<String> currentUsers	= new ArrayList<String>();
	
	
	// ServerGUI constructor - adds graphic and displays window
	public ServerGUI()
	{
		// Sets window layout - grid style, 1 row and 3 columns
		setLayout(new GridLayout(1,3));
		
		// Adds LEDs
		add(redLED);
		add(yellowLED);
		add(greenLED);
		
		// Sets background color
		getContentPane().setBackground(Color.white);
		
		// Placement of window - setBounds(placementX, placementY, sizeX, sizeY)
		setBounds(0, 50, 360, 150);
		
		// Makes window visible (false = not visible)
		setVisible(true);
		
		// Exit program when closing window
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	
	// Main program - starts ServerGUI and runs the client server
	public static void main (String[] args) throws IOException
	{
		// User-set PORT number from input dialog
		String input = JOptionPane.showInputDialog("Enter PORT number:\n<html><i>(Default PORT = 8000)</i></html>");
		// If user pressed OK or Cancel
		if (input.equals("") || input.equals(null));
		else PORT = Integer.parseInt(input);
		
		// Print current PORT number to terminal
		if (PORT != 8000) System.out.println("\nPORT: " + PORT + "\n");
		else System.out.println("\nDefault PORT: " + PORT + "\n");
		
		new ServerGUI().runServer();
	}
	
	
	// ServerGUI method - initializes Sockets, accepts client connections,
	// stores connected sockets and users in arrays, starts threading of connected clients
	public void runServer() throws IOException
	{
		// BufferedReader = reads data from file
		readFromFile = new BufferedReader(new FileReader("LEDstatus.txt"));
		
		// Reads saved values from file
		initLED = readFromFile.readLine();
		
		// For-loop - retrieves one value at a time from the string saved to file
		for(int i=0; i<3; i++)
		{
			switch (initLED.charAt(i))
			{
			// If saved value = '1' - lit red LED, print current state to terminal
			case '1':	redLED.setIcon(LEDred);
						System.out.println("Current Red value:    ON");
						break;
			// If saved value = '0' - turn off red LED, print current state to terminal
			case '0':	redLED.setIcon(LEDoff);
						System.out.println("Current Red value:    OFF");
						break;
			// If saved value = '3' - lit green LED, print current state to terminal
			case '3':	greenLED.setIcon(LEDgreen);
						System.out.println("Current Green value:  ON");
						break;
			// If saved value = '2' - turn off green LED, print current state to terminal
			case '2':	greenLED.setIcon(LEDoff);
						System.out.println("Current Green value:  OFF");
						break;
			// If saved value = '5' - lit yellow LED, print current state to terminal
			case '5':	yellowLED.setIcon(LEDyellow);
						System.out.println("Current Yellow value: ON");
						break;
			// If saved value = '4' - turn off yellow LED, print current state to terminal
			case '4':	yellowLED.setIcon(LEDoff);
						System.out.println("Current Yellow value: OFF");
						break;
			}
		}
		
		@SuppressWarnings("resource")
		// Creates server socket and opens port for client connections
		ServerSocket serverSocket = new ServerSocket(PORT);
		System.out.println("\nServer ready and waiting for connection...");
		
		// While-loop that initializes client connections
		while(true)
		{
			// Accepts client connection
			socket = serverSocket.accept();
			
			// Registers connected socket in userConnections
			userConnections.add(socket);
			
			// BufferedReader = reads data from new client
			clientInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			// Receives new client name and registers it in currentUsers
			user = clientInput.readLine();
			currentUsers.add(user);
			
			System.out.println("\nClient connected: " + user);
			
			// Initializes threading of newly connected client
			Thread X = new Thread(new ClientThread(socket));
			X.start();
		}
	}
	
	
	// ClientThread class - creates a new thread for clients to enable multiple client connections
	public class ClientThread extends Thread 
	{
		// Declaration of variables
		Socket socket;
		
		String message = "";
		
		BufferedReader clientInput;
		
		
		// Creates a new thread for client
		ClientThread(Socket socket)
		{
			this.socket = socket;
		}
		
		
		// ClientThread method - reads saved LEDstate values from file, sends them to new client, reads new input from clients,
		// sets LEDstate, stores LEDstate to file, returns received client input to all clients
		public void run() 
		{
			// Error handling - try this when clients are connected and/or server is online
			try
			{
				// BufferedReader = reads data from clients
				clientInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				// BufferedReader = reads data from file
				readFromFile = new BufferedReader(new FileReader("LEDstatus.txt"));
				
				// Reads saved values from file
				initLED = readFromFile.readLine();
				
				// For-loop - retrieves one value at a time from the string saved to file
				for(int i=0; i<3; i++)
				{
					// Takes the last connected socket from userConnections
					currentSock = (Socket) ServerGUI.userConnections.get(userConnections.size()-1);
					// Creates a PrintWriter for the last connected socket
					currentClient = new PrintWriter(currentSock.getOutputStream(), true);
					// Sends current value to last connected client
					currentClient.println(initLED.charAt(i));
				}
				
				System.out.println("Current state sent to " + currentUsers.get(currentUsers.size()-1) + "\n");
				
				readFromFile.close();
				
				// While-loop - reads client input
				while((message = clientInput.readLine()) != null)
				{
					// First character is the LEDstate, the following characters is the client name
					char LEDmsg = message.charAt(0);
					
					switch (LEDmsg)
					{
					// If LEDstate = '1' - lit red LED, prints current state to terminal, stores redState, writes LEDstates to file
					case '1':	redLED.setIcon(LEDred);
								System.out.println(message.substring(1, message.length()) + " turned: Red    ON");
								redState = String.valueOf(LEDmsg);
								writeToFile = new PrintWriter("LEDstatus.txt", "UTF-8");
								writeToFile.println((redState + greenState + yellowState));
								writeToFile.close();
								break;
					// If LEDstate = '0' - turns off red LED, prints current state to terminal, stores redState, writes LEDstates to file
					case '0':	redLED.setIcon(LEDoff);
								System.out.println(message.substring(1, message.length()) + " turned: Red    OFF");
								redState = String.valueOf(LEDmsg);
								writeToFile = new PrintWriter("LEDstatus.txt", "UTF-8");
								writeToFile.println((redState + greenState + yellowState));
								writeToFile.close();
								break;
					// If LEDstate = '3' - lit green LED, prints current state to terminal, stores redState, writes LEDstates to file
					case '3':	greenLED.setIcon(LEDgreen);
								System.out.println(message.substring(1, message.length()) + " turned: Green  ON");
								greenState = String.valueOf(LEDmsg);
								writeToFile = new PrintWriter("LEDstatus.txt", "UTF-8");
								writeToFile.println((redState + greenState + yellowState));
								writeToFile.close();
								break;
					// If LEDstate = '2' - turns off green LED, prints current state to terminal, stores redState, writes LEDstates to file
					case '2':	greenLED.setIcon(LEDoff);
								System.out.println(message.substring(1, message.length()) + " turned: Green  OFF");
								greenState = String.valueOf(LEDmsg);
								writeToFile = new PrintWriter("LEDstatus.txt", "UTF-8");
								writeToFile.println((redState + greenState + yellowState));
								writeToFile.close();
								break;
					// If LEDstate = '5' - lits yellow LED, prints current state to terminal, stores redState, writes LEDstates to file
					case '5':	yellowLED.setIcon(LEDyellow);
								System.out.println(message.substring(1, message.length()) + " turned: Yellow ON");
								yellowState = String.valueOf(LEDmsg);
								writeToFile = new PrintWriter("LEDstatus.txt", "UTF-8");
								writeToFile.println((redState + greenState + yellowState));
								writeToFile.close();
								break;
					// If LEDstate = '4' - turns off yellow LED, prints current state to terminal, stores redState, writes LEDstates to file
					case '4':	yellowLED.setIcon(LEDoff);
								System.out.println(message.substring(1, message.length()) + " turned: Yellow OFF");
								yellowState = String.valueOf(LEDmsg);
								writeToFile = new PrintWriter("LEDstatus.txt", "UTF-8");
								writeToFile.println((redState + greenState + yellowState));
								writeToFile.close();
								break;
					}
					
					// For-loop - sends LEDmsg to all registered clients
					for(int i = 1; i <= ServerGUI.userConnections.size(); i++)
					{
						// Reads socket 1-X (i) from userConnections
						currentSock = (Socket) ServerGUI.userConnections.get(i-1);
						// Creates a PrintWriter for the socket
						currentClient = new PrintWriter(currentSock.getOutputStream(), true);
						// Sends changed value to the client
						currentClient.println(LEDmsg);
						System.out.println("Sent to " + currentUsers.get(i-1));
					}
				}
				
				// Linux specific - if file not closed, LEDstate will be incorrect for next client or
				// next time server is started
				try
				{
					writeToFile.close();
				}

				catch (NullPointerException f) {}
				
				System.out.println("\nClient disconnected!\n");
			}
			
			// Windows specific - if file not closed, LEDstate will be incorrect for next client or
			// next time server is started
			catch (IOException e)
			{
				try
				{
					writeToFile.close();
				}
				
				catch (NullPointerException f) {}
				
				System.out.println("\nClient disconnected!\n");
			}
		}
	}
}
