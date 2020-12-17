package client;

//hi
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;

public class ClientUI extends JFrame implements Event {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	CardLayout card;
	ClientUI self;
	JPanel textArea;
	JPanel userPanel;
	List<User> users = new ArrayList<User>();
	static ArrayList<String> history = new ArrayList<String>();

	private final static Logger log = Logger.getLogger(ClientUI.class.getName());
	Dimension windowSize = Toolkit.getDefaultToolkit().getScreenSize();
	String username;
	RoomsPanel roomsPanel;
	JMenuBar menu;

	public static File generateFile(String filename) {
		File file = new File(filename);
		try {
			if (file.createNewFile()) {
				System.out.println("Didn't exist, created new");
			} else {
				System.out.println("File already exists");
			}
		} catch (IOException ie) {
			ie.printStackTrace();
		}

		return file;

	}

	public static void exportChat(String filename) {
		try (FileWriter fw = new FileWriter(filename)) {
			for (String msg : history) {
				fw.append(msg);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ClientUI(String title) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		menu = new JMenuBar();
		JMenu roomsMenu = new JMenu("Rooms");
		JMenuItem roomsSearch = new JMenuItem("Search");
		roomsSearch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("clicked");
				goToPanel("rooms");
			}

		});
		roomsMenu.add(roomsSearch);
		menu.add(roomsMenu);
		windowSize.width *= .2;
		windowSize.height *= .4;
		setPreferredSize(windowSize);
		setSize(windowSize);// This is needed for setLocationRelativeTo()
		setLocationRelativeTo(null);
		self = this;
		setTitle(title);
		card = new CardLayout();
		setLayout(card);
		createConnectionScreen();
		createUserInputScreen();

		createPanelRoom();
		createPanelUserList();
		this.setJMenuBar(menu);
		// TODO remove
		createRoomsPanel();
		showUI();
	}

	void createConnectionScreen() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel hostLabel = new JLabel("Host:");
		JTextField host = new JTextField("127.0.0.1");
		panel.add(hostLabel);
		panel.add(host);
		JLabel portLabel = new JLabel("Port:");
		JTextField port = new JTextField("3000");
		panel.add(portLabel);
		panel.add(port);
		JButton button = new JButton("Next");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String _host = host.getText();
				String _port = port.getText();
				if (_host.length() > 0 && _port.length() > 0) {
					try {
						connect(_host, _port);
						self.next();
					} catch (IOException e1) {
						e1.printStackTrace();
						// TODO handle error properly
						log.log(Level.SEVERE, "Error connecting");
					}
				}
			}

		});
		panel.add(button);
		this.add(panel, "login");
	}

	void createUserInputScreen() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel userLabel = new JLabel("Username:");
		JTextField username = new JTextField();
		panel.add(userLabel);
		panel.add(username);
		JButton button = new JButton("Join");
		ClientUI self = this;
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String name = username.getText();
				if (name != null && name.length() > 0) {
					// need external ref since "this" context is the action event, not ClientUI
					self.username = name;
					// this order matters
					pack();
					self.setTitle(self.getTitle() + " - " + self.username);
					SocketClient.INSTANCE.setUsername(self.username);

					self.next();
				}
			}

		});
		panel.add(button);
		this.add(panel, "details");
	}

	void createPanelRoom() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		textArea = new JPanel();
		textArea.setLayout(new BoxLayout(textArea, BoxLayout.Y_AXIS));
		textArea.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		panel.add(scroll, BorderLayout.CENTER);

		JPanel input = new JPanel();
		input.setLayout(new BoxLayout(input, BoxLayout.X_AXIS));
		JTextField text = new JTextField();
		input.add(text);
		JButton button = new JButton("Send");
		JButton Exportbutton = new JButton("Export");
		text.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendAction");
		text.getActionMap().put("sendAction", new AbstractAction() {
			public void actionPerformed(ActionEvent actionEvent) {
				button.doClick();
			}
		});

		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (text.getText().length() > 0) {
					SocketClient.INSTANCE.sendMessage(text.getText());
					text.setText("");
				}
			}

		});

		Exportbutton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				generateFile("chatHistory.txt" + roomsPanel.getName());
				exportChat("chatHistory.txt");
			}

		});

		input.add(button);
		input.add(Exportbutton);
		panel.add(input, BorderLayout.SOUTH);
		this.add(panel, "lobby");
	}

	void createPanelUserList() {
		userPanel = new JPanel();
		userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
		userPanel.setAlignmentY(Component.TOP_ALIGNMENT);

		JScrollPane scroll = new JScrollPane(userPanel);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		Dimension d = new Dimension(100, windowSize.height);
		scroll.setPreferredSize(d);

		textArea.getParent().getParent().getParent().add(scroll, BorderLayout.EAST);
	}

	void createRoomsPanel() {
		roomsPanel = new RoomsPanel(this);
		this.add(roomsPanel, "rooms");
	}

	void addClient(String name) {
		User u = new User(name);
		Dimension p = new Dimension(userPanel.getSize().width, 30);
		u.setPreferredSize(p);
		u.setMinimumSize(p);
		u.setMaximumSize(p);
		userPanel.add(u);
		users.add(u);
		pack();
	}

	void removeClient(User client) {
		userPanel.remove(client);
		client.removeAll();
		userPanel.revalidate();
		userPanel.repaint();
	}

	/***
	 * Attempts to calculate the necessary dimensions for a potentially wrapped
	 * string of text. This isn't perfect and some extra whitespace above or below
	 * the text may occur
	 * 
	 * @param str
	 * @return
	 */
	int calcHeightForText(String str) {
		FontMetrics metrics = self.getGraphics().getFontMetrics(self.getFont());
		int hgt = metrics.getHeight();
		int adv = metrics.stringWidth(str);
		final int PIXEL_PADDING = 6;
		Dimension size = new Dimension(adv, hgt + PIXEL_PADDING);
		final float PADDING_PERCENT = 1.1f;
		// calculate modifier to line wrapping so we can display the wrapped message
		int mult = (int) Math.floor(size.width / (textArea.getSize().width * PADDING_PERCENT));
		// System.out.println(mult);
		mult++;
		return size.height * mult;
	}

	void addMessage(String str) {
		JEditorPane entry = new JEditorPane();
		history.add(str + System.lineSeparator());
		entry.setContentType("text/html");
		if (str.indexOf('*') != -1)
			str = makeBold(str);
		if (str.indexOf('`') != -1)
			str = makeItalic(str);
		if (str.indexOf('#') != -1)
			str = underline(str);
		str = makeColors(str);
		entry.setText(str);
		entry.setEditable(false);
		Dimension d = new Dimension(textArea.getSize().width, calcHeightForText(str));
		// attempt to lock all dimensions
		entry.setMinimumSize(d);
		entry.setPreferredSize(d);
		entry.setMaximumSize(d);
		textArea.add(entry);
		pack();
		// System.out.println(entry.getSize());
		JScrollBar sb = ((JScrollPane) textArea.getParent().getParent()).getVerticalScrollBar();
		sb.setValue(sb.getMaximum());
	}

	String makeBold(String str) {
		int boldCount = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '*') {
				boldCount++;
			}
		}
		if (boldCount >= 2) {
			str = str + " ";
			str = str.replace("*", "<b>");
			str = str.replace("<b> ", "</b> ");
		}
		return str;
	}

	String makeItalic(String str) {
		int boldCount = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '`') {
				boldCount++;
			}
		}
		if (boldCount >= 2) {
			str = str + " ";
			str = str.replace("`", "<i>");
			str = str.replace("<i> ", "</i> ");
		}
		return str;
	}

	String underline(String str) {
		int boldCount = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '#') {
				boldCount++;
			}
		}
		if (boldCount >= 2) {
			str = str + " ";
			str = str.replace("#", "<u>");
			str = str.replace("<u> ", "</u> ");
		}
		return str;
	}

	String makeColors(String str) {
		String[] message = str.split(" ");
		String word;
		String finalStr = "";
		int i;

		if (message.length < 1) {

			for (i = 0; i < message.length; i++) {
				word = message[i];
				if (word.contains("red^") & word.contains("^red")) {
					word = word.replace("red^", "<font color=\"red\">");
					word = word.replace("^red", "</font>");
					message[i] = word;
				}

				if (word.contains("blue^") & word.contains("^blue")) {
					word = word.replace("blue^", "<font color=\"blue\">");
					word = word.replace("^blue", "</font>");
					message[i] = word;
				}

				if (word.contains("green^") & word.contains("^green")) {
					word = word.replace("green^", "<font color=\"green\">");
					word = word.replace("^green", "</font>");
					message[i] = word;
				}

				if (word.contains("yellow^") & word.contains("^yellow")) {
					word = word.replace("yellow^", "<font color=\"yellow\">");
					word = word.replace("^yellow", "</font>");
					message[i] = word;
				}
				for (i = 0; i < message.length; i++) {
					word = message[i];
					finalStr = finalStr + " " + word;
				}
				return finalStr;
			}
		} else {

			if (str.contains("yellow^") & str.contains("^yellow")) {
				str = str.replace("yellow^", "<font color=\"yellow\">");
				str = str.replace("^yellow", "</font>");
				return finalStr;
			}

			if (str.contains("red^") & str.contains("^red")) {
				str = str.replace("red^", "<font color=\"red\">");
				str = str.replace("^red", "</font>");
				finalStr = str;
				return finalStr;
			}

			if (str.contains("blue^") & str.contains("^blue")) {
				str = str.replace("blue^", "<font color=\"blue\">");
				str = str.replace("^blue", "</font>");
				finalStr = str;
				return finalStr;
			}

			if (str.contains("green^") & str.contains("^green")) {
				str = str.replace("green^", "<font color=\"green\">");
				str = str.replace("^green", "</font>");
				finalStr = str;
				return finalStr;
			}

		}
		return str;
	}

	void next() {
		card.next(this.getContentPane());
	}

	void previous() {
		card.previous(this.getContentPane());
	}

	void goToPanel(String panel) {
		switch (panel) {
		case "rooms":
			// TODO get rooms
			roomsPanel.removeAllRooms();
			SocketClient.INSTANCE.sendGetRooms(null);
			break;
		default:
			// no need to react
			break;
		}
		card.show(this.getContentPane(), panel);
	}

	void connect(String host, String port) throws IOException {
		SocketClient.INSTANCE.registerCallbackListener(this);
		SocketClient.INSTANCE.connectAndStart(host, port);
	}

	void showUI() {
		pack();
		Dimension lock = textArea.getSize();
		textArea.setMaximumSize(lock);
		lock = userPanel.getSize();
		userPanel.setMaximumSize(lock);
		setVisible(true);
	}

	@Override
	public void onClientConnect(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		addClient(clientName);
		if (message != null && !message.isBlank()) {
			self.addMessage(String.format("%s: %s", clientName, message));
		}
	}

	@Override
	public void onClientDisconnect(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		Iterator<User> iter = users.iterator();
		while (iter.hasNext()) {
			User u = iter.next();
			if (u.getName() == clientName) {
				removeClient(u);
				iter.remove();
				self.addMessage(String.format("%s: %s", clientName, message));
				break;
			}

		}
	}

	// https://github.com/MattToegel/IT114/blob/Feature-GameLogic/ProjectPart8/src/client/ClientUI.java
	// Added for feature to show user muted on client List
	public void isMuted(String clientName, boolean isMuted) {
		Iterator<User> iter = users.iterator();
		while (iter.hasNext()) {
			User user = iter.next();
			if (user.getName().equalsIgnoreCase(clientName)) {
				if (isMuted) {
					user.setName(clientName, "red^ ^red");
				} else {
					user.setName(clientName, "%s");
				}
				break;
			}
		}
	}

	// Added for feature to show user muted on client List
	@Override
	public void onMessageReceive(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		self.addMessage(String.format("%s: %s", clientName, message));
	}

	@Override
	public void onChangeRoom() {
		Iterator<User> iter = users.iterator();
		while (iter.hasNext()) {
			User u = iter.next();
			removeClient(u);
			iter.remove();
		}
		goToPanel("lobby");
	}

	public static void main(String[] args) {
		ClientUI ui = new ClientUI("My UI");
		if (ui != null) {
			log.log(Level.FINE, "Started");
		}
	}

	@Override
	public void onSyncDirection(String clientName, Point direction) {
		// TODO Auto-generated method stub
		// no need to sync this for ClientUI
	}

	@Override
	public void onSyncPosition(String clientName, Point position) {
		// TODO Auto-generated method stub
		// no need to sync this for ClientUI
	}

	@Override
	public void onGetRoom(String roomName) {
		// TODO Auto-generated method stub
		if (roomsPanel != null) {
			roomsPanel.addRoom(roomName);
			pack();
		}
	}
}