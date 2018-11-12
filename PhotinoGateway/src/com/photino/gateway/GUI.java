package com.photino.gateway;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GUI extends JFrame implements ActionListener 
{
	public static JLabel title = new JLabel("Photino Gateway Node");
	public static JLabel version = new JLabel("0.0.0.1");
	public static JLabel status = new JLabel("Status: NOT Validated, NOT Processing");
	JLabel ipAddressLbl = new JLabel("IP Address: ");
	public static JLabel ipAddress = new JLabel("0.0.0.0");
	//JTextField ipAddress = new JTextField(20);
	JLabel startPortLbl = new JLabel("Starting Port: ");
	public static JLabel startPort = new JLabel("33000");
	JLabel endPortLbl = new JLabel("End Port: ");
	public static JLabel endPort = new JLabel("33000");
	
	JPanel listPanel = new JPanel();
	
	JLabel comboLbl = new JLabel("Topics");
	JLabel listLbl = new JLabel("Listening for Topics");
	public static DefaultListModel topicListModel=new DefaultListModel();
	JList topics = new JList(topicListModel);
	
	public static JTextArea textArea = new JTextArea(10, 30);
	
    JTextField textField = new JTextField(20);
    JButton set = new JButton("Set Text");
    JButton get = new JButton("Get Text");

    public GUI() {
        super();
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	setTitle("Photino Gateway");
    	setSize(500,400);
    	setResizable(false);
    	//This will center the JFrame in the middle of the screen
    	setLocationRelativeTo(null);
    	
    	JPanel comboPanel = new JPanel();
    	ipAddress.setBounds(165, 60, 113, 14);

    	ipAddress.setText("192.168.1.1");
    	listPanel.setBounds(0, 27, 494, 213);
    	
    	listPanel.setVisible(true);
    	
    	topicListModel.addElement("NO TOPICS");
    	getContentPane().setLayout(null);
    	listPanel.setLayout(null);
    	
    	JLabel label = new JLabel(new ImageIcon("logo2.png"));
    	label.setBounds(31, 12, -1, -1);
    	listPanel.add(label);
    	status.setFont(new Font("Tahoma", Font.BOLD, 11));
    	status.setHorizontalAlignment(SwingConstants.CENTER);
    	status.setBounds(10, 17, 474, 14);
    	listPanel.add(status);
    	ipAddressLbl.setBounds(31, 60, 124, 14);
    	listPanel.add(ipAddressLbl);
    	listPanel.add(ipAddress);
    	startPortLbl.setBounds(31, 85, 124, 14);
    	listPanel.add(startPortLbl);
    	startPort.setBounds(165, 85, 48, 14);
    	listPanel.add(startPort);
    	endPortLbl.setBounds(263, 85, 77, 14);
    	listPanel.add(endPortLbl);
    	endPort.setBounds(362, 85, 53, 14);
    	listPanel.add(endPort);
    	listLbl.setBounds(31, 121, 124, 14);
    	listPanel.add(listLbl);

    	//guiFrame.add(comboPanel, BorderLayout.NORTH);
    	getContentPane().add(listPanel);
    	
    	JScrollPane topics_scrollPane = new JScrollPane();
    	topics_scrollPane.setBounds(165, 121, 250, 81);
    	listPanel.add(topics_scrollPane);
    	topics_scrollPane.setViewportView(topics);
    	topics.setLayoutOrientation(JList.HORIZONTAL_WRAP);

    	JScrollPane scrollPane = new JScrollPane(); 
    	scrollPane.setBounds(0, 239, 494, 132);
    	getContentPane().add(scrollPane);
    	scrollPane.setViewportView(textArea);
    	textArea.setEditable(false);
    	title.setHorizontalAlignment(SwingConstants.CENTER);
    	title.setFont(new Font("Tahoma", Font.BOLD, 14));
    	title.setBounds(10, 5, 484, 14);
    	getContentPane().add(title);
    	
    	
    	version.setHorizontalAlignment(SwingConstants.CENTER);
    	version.setBounds(446, 5, 48, 14);
    	getContentPane().add(version);

    	//make sure the JFrame is visible
    	setVisible(true);

        //add(textField);
        //set.addActionListener(this); //this tells the program that the button actually triggers an event
        //add(set);
        //get.addActionListener(this);
        //add(get);
        //pack();;
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == set) {
            textField.setText(JOptionPane.showInputDialog(null, "Enter a new word for the text field:"));
        } else {
            System.out.println(textField.getText());
        }
    }

    public static Boolean outputText(String output)
    {
    	int lines = textArea.getText().split("\n").length;
    	if ( lines > 1000 ) textArea.setText("");
		textArea.append(output+"\n");
	    textArea.setCaretPosition(textArea.getDocument().getLength());
    	return true;
    }
    public static Boolean updateTitle(String newTitle)
    {
    	title.setText(newTitle);
    	return true;
    }
    public static Boolean updateVersion(String ver)
    {
    	version.setText(ver);
    	return true;
    }
    public static Boolean updateStatus(String newStatus)
    {
    	status.setText(newStatus);
    	return true;
    }
    public static Boolean updateIPPort(String ip, String startport, String endport)
    {
    	ipAddress.setText(ip);
    	startPort.setText(startport);
    	endPort.setText(endport);
    	return true;
    }
    public static Boolean updateMyTopics(String topiclists)
    {
    	topicListModel.removeAllElements();
    	if ( topiclists.equals("") )
    	{
    		topicListModel.addElement("NO TOPICS");
    	}
    	else
    	{
    		String[] tlist = topiclists.split(",");
    		for ( int i=0; i < tlist.length; i++ )
    		{
    			topicListModel.addElement(tlist[i]);
    		}	
    	}
    	return true;
    }
    
    public static void main(String[] args) {
        GUI tt = new GUI();
    }
}