package com.photino.dnr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GUI extends JFrame implements ActionListener 
{
	JLabel title = new JLabel("Photino DNR Node");
	private static JLabel version = new JLabel("0.0.0.1");
	public static JLabel status = new JLabel("Status: Initializing");
	JLabel ipAddressLbl = new JLabel("IP Address: ");
	public static JLabel ipAddress = new JLabel("0.0.0.0");
	//JTextField ipAddress = new JTextField(20);
	JLabel startPortInLbl = new JLabel("Starting Port In: ");
	public static JLabel startPortIn = new JLabel("33000");
	JLabel endPortInLbl = new JLabel("End Port In: ");
	public static JLabel endPortIn = new JLabel("33000");
	
	JLabel startPortOutLbl = new JLabel("Starting Port Out: ");
	public static JLabel startPortOut = new JLabel("43000");
	JLabel endPortOutLbl = new JLabel("End Port Out: ");
	public static JLabel endPortOut = new JLabel("43000");
	
	JLabel dnrActiveLbl = new JLabel("DNRs Active: ");
	public static JLabel dnrActive = new JLabel("1");
	
	JLabel gatewaysActiveLbl = new JLabel("Gateways Active: ");
	public static JLabel gatewaysActive = new JLabel("0");
	
	JLabel topicsActiveLbl = new JLabel("Topics Active: ");
	public static JLabel topicsActive = new JLabel("0");
	
	JLabel quesActiveLbl = new JLabel("Ques Active: ");
	public static JLabel quesActive = new JLabel("0");
	
	JPanel listPanel = new JPanel();
	
	JLabel comboLbl = new JLabel("Topics");
	public static DefaultListModel topicListModel=new DefaultListModel();
	
	public static JTextArea textArea = new JTextArea(10, 30);
	
    JTextField textField = new JTextField(20);
    JButton set = new JButton("Set Text");
    JButton get = new JButton("Get Text");
    

    public GUI() {
        super();
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	setTitle("Photino DNR");
    	setSize(500,400);
    	setResizable(false);
    	//This will center the JFrame in the middle of the screen
    	setLocationRelativeTo(null);
    	
    	JPanel comboPanel = new JPanel();;
    	listPanel.setBounds(22, 31, 448, 214);
    	
    	listPanel.setVisible(true);
    	
    	topicListModel.addElement("NO TOPICS");
    	getContentPane().setLayout(null);
    	listPanel.setLayout(null);
    	status.setBounds(2, 2, 444, 30);
    	status.setForeground(Color.DARK_GRAY);
    	status.setFont(new Font("Tahoma", Font.BOLD, 12));
    	status.setHorizontalAlignment(SwingConstants.CENTER);
    	listPanel.add(status);
    	ipAddressLbl.setBounds(2, 48, 74, 30);
    	listPanel.add(ipAddressLbl);
    	    	ipAddress.setBounds(120, 48, 124, 30);
    	
    	    	ipAddress.setText("192.168.1.1");
    	    	listPanel.add(ipAddress);
    	startPortInLbl.setBounds(2, 92, 94, 30);
    	listPanel.add(startPortInLbl);
    	startPortIn.setBounds(120, 90, 74, 30);
    	listPanel.add(startPortIn);
    	endPortInLbl.setBounds(194, 90, 94, 30);
    	listPanel.add(endPortInLbl);
    	endPortIn.setBounds(298, 90, 74, 30);
    	listPanel.add(endPortIn);
    	startPortOutLbl.setBounds(2, 122, 108, 30);
    	listPanel.add(startPortOutLbl);
    	startPortOut.setBounds(120, 120, 74, 30);
    	listPanel.add(startPortOut);
    	endPortOutLbl.setBounds(194, 120, 94, 30);
    	listPanel.add(endPortOutLbl);
    	endPortOut.setBounds(298, 120, 74, 30);
    	listPanel.add(endPortOut);
    	
    	dnrActiveLbl.setBounds(194, 150, 94, 30);
    	listPanel.add(dnrActiveLbl);
    	dnrActive.setBounds(298, 150, 74, 30);
    	listPanel.add(dnrActive);
    	
    	gatewaysActiveLbl.setBounds(2, 150, 108, 30);
    	listPanel.add(gatewaysActiveLbl);
    	gatewaysActive.setBounds(120, 150, 74, 30);
    	listPanel.add(gatewaysActive);
    	
    	topicsActiveLbl.setBounds(2, 180, 108, 30);
    	listPanel.add(topicsActiveLbl);
    	topicsActive.setBounds(120, 180, 74, 30);
    	listPanel.add(topicsActive);

    	//guiFrame.add(comboPanel, BorderLayout.NORTH);
    	getContentPane().add(listPanel);
    	
    	
    	quesActiveLbl.setBounds(194, 180, 94, 30);
    	listPanel.add(quesActiveLbl);
    	quesActive.setBounds(298, 180, 74, 30);
    	listPanel.add(quesActive);

    	JScrollPane scrollPane = new JScrollPane(); 
    	scrollPane.setBounds(0, 246, 494, 125);
    	getContentPane().add(scrollPane);
    	scrollPane.setViewportView(textArea);
    	textArea.setEditable(false);
    	
    	JLabel label = new JLabel(new ImageIcon("logo2.png"));
    	label.setBounds(0, 0, 20, 20);
    	getContentPane().add(label);
    	title.setHorizontalAlignment(SwingConstants.CENTER);
    	title.setFont(new Font("Tahoma", Font.BOLD, 14));
    	title.setBounds(20, 0, 448, 23);
    	getContentPane().add(title);
    	version.setBounds(435, 0, 59, 20);
    	getContentPane().add(version);
    	version.setHorizontalAlignment(SwingConstants.CENTER);

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
		//if ( textArea.getCaretPosition() == textArea.getDocument().getLength() )
		//{
			textArea.setCaretPosition(textArea.getDocument().getLength());
		//}
    	return true;
    }
    public static Boolean updateStats(String numofdnr, String numofgateway, String numoftopics, String numofques)
    {
    	dnrActive.setText(numofdnr);
    	gatewaysActive.setText(numofgateway);
    	topicsActive.setText(numoftopics);
    	quesActive.setText(numofques);
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
    public static Boolean updateIPPort(String ip, String startportin, String endportin, String startportout, String endportout )
    {
    	ipAddress.setText(ip);
    	startPortIn.setText(startportin);
    	endPortIn.setText(endportin);
    	startPortOut.setText(startportout);
    	endPortOut.setText(endportout);
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
