package MafiaG;

import java.nio.charset.StandardCharsets;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class PlayUI extends JFrame implements ActionListener {
    static Socket sock;
    static BufferedWriter bw = null;
    static BufferedReader br = null;

    private RankingPanel rankingPanel;
    private DefaultListModel<String> participantModel;
    private JTextField chatInput;
    private JTextPane chatPane;
    private StyledDocument doc;
    private JButton startButton;
    private JComboBox<String> voteChoice;
    private JButton voteBtn;
    private JLabel timerLabel;

    private String myColor = "";
    private boolean gameStarted = false;

    private final Map<String, String> colorToNameMap = new HashMap<String, String>() {{
        put("#FF6B6B", "빨간 유저");
        put("#6BCB77", "청룡 유저");
        put("#4D96FF", "파랑 유저");
        put("#FFC75F", "노랑 유저");
        put("#A66DD4", "보라 유저");
        put("#FF9671", "오렌지 유저");
        put("#00C9A7", "청록 유저");
    }};
    private final Map<String, String> nameToColorMap = new HashMap<>();

    public PlayUI() {
        setTitle("MafiaG");
        setSize(1200, 800);
        ImageIcon icon = new ImageIcon("src/img/logo.png"); // 로고 경로

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        setupUI();
        connectToServer();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(
                    PlayUI.this,
                    "정말 종료하시겠습니까?",
                    "종료 확인",
                    JOptionPane.YES_NO_OPTION
                );
                
                System.out.println("종료 요청 확인됨");
                
                if (result == JOptionPane.YES_OPTION) {
                	System.out.println("정리 작업 시작");
                	
                 // 네트워크 자원 정리
                    closeConnection();
                    System.out.println("연결 종료");
                    sendToServer("{\"type\":\"quit\"}");
                    try {
                        Thread.sleep(1000); // 자원 해제 대기
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    
                 // 창 종료
                    dispose();
                    
                 // 모든 스레드 정리 후 강제 종료
                    System.exit(0);
                    System.out.println("완전 종료");
                    
                }
            }
        });
        setLocationRelativeTo(null);
    }

	private void setupUI() {
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(new Color(238, 238, 238));
		header.setBorder(new EmptyBorder(10, 20, 10, 20));
		header.add(new JLabel("MafiaG", SwingConstants.LEFT), BorderLayout.WEST);
		add(header, BorderLayout.NORTH);

		JPanel mainPanel = new JPanel(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);

		JPanel sidebar = new JPanel(new BorderLayout());
		sidebar.setPreferredSize(new Dimension(200, 0));
		sidebar.setBackground(new Color(240, 234, 255));

		// 랭킹창
		JPanel sidebarContent = new JPanel(new BorderLayout());  // 선언 먼저
//		rankingArea = new JTextArea("랭킹\n", 5, 20);
//		rankingArea.setEditable(false);
//		JScrollPane rankingScroll = new JScrollPane(rankingArea);
//		rankingScroll.setBorder(BorderFactory.createTitledBorder("랭킹"));
		rankingPanel = new RankingPanel();
		sidebarContent.add(rankingPanel, BorderLayout.NORTH);

		// 참여자 명단
		participantModel = new DefaultListModel<>();
		JList<String> participantList = new JList<>(participantModel);
		JScrollPane participantScroll = new JScrollPane(participantList);
		participantScroll.setBorder(BorderFactory.createTitledBorder("참여자 명단"));

		startButton = new JButton("Start");
		startButton.setEnabled(true);
		startButton.setPreferredSize(new Dimension(200, 50));
		startButton.addActionListener(e -> {
			startButton.setEnabled(false);
			sendToServer("{\"type\":\"start\"}");
		});

//		JPanel sidebarContent = new JPanel(new BorderLayout());
//		sidebarContent.add(rankingScroll, BorderLayout.NORTH);
		sidebarContent.add(participantScroll, BorderLayout.CENTER);
		sidebar.add(sidebarContent, BorderLayout.CENTER);
		sidebar.add(startButton, BorderLayout.SOUTH);
		mainPanel.add(sidebar, BorderLayout.WEST);

		JPanel chatContainer = new JPanel(new BorderLayout());
		chatPane = new JTextPane();
		chatPane.setEditable(false);
		doc = chatPane.getStyledDocument();
		JScrollPane chatScroll = new JScrollPane(chatPane);
		chatContainer.add(chatScroll, BorderLayout.CENTER);

		JPanel inputPanel = new JPanel(new BorderLayout());
		chatInput = new JTextField();
		chatInput.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
		chatInput.setEnabled(false);
		chatInput.setBackground(Color.LIGHT_GRAY);
		chatInput.addActionListener(this);
		inputPanel.add(chatInput, BorderLayout.CENTER);
		chatContainer.add(inputPanel, BorderLayout.SOUTH);
		mainPanel.add(chatContainer, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		voteChoice = new JComboBox<>();
		voteChoice.setPreferredSize(new Dimension(150, 30));
		voteChoice.setEnabled(false);

		voteBtn = new JButton("투표");
		voteBtn.setEnabled(false);
		voteBtn.addActionListener(e -> {
			String selectedLabel = (String) voteChoice.getSelectedItem();
			if (selectedLabel != null) {
				String selectedColor = nameToColorMap.get(selectedLabel);
				if (selectedColor != null) {
					sendToServer("{\"type\":\"vote\",\"target\":\"" + selectedColor + "\"}");
					voteChoice.setEnabled(false);
					voteBtn.setEnabled(false);
				}
			}
		});

		timerLabel = new JLabel("남은 시간: 20초");
		bottomPanel.add(new JLabel("투표 대상:"));
		bottomPanel.add(voteChoice);
		bottomPanel.add(voteBtn);
		bottomPanel.add(timerLabel);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);
	}

    public void actionPerformed(ActionEvent e) {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            sendToServer("{\"type\":\"ANSWER_SUBMIT\",\"message\":\"" + msg + "\"}");
            appendAnonymousChat(myColor, msg);
            chatInput.setText("");
        }
    }

    private void appendAnonymousChat(String colorCode, String msg) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        try {
            StyleConstants.setForeground(attr, Color.decode(colorCode));
        } catch (NumberFormatException e) {
            StyleConstants.setForeground(attr, Color.GRAY);
        }
        StyleConstants.setFontSize(attr, 16);
        try {
            doc.insertString(doc.getLength(), msg + "\n", attr);
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private void connectToServer() {
        try {
            sock = new Socket("172.30.1.54", 3579);
            br = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
            bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));

            Thread serverThread = new Thread(() -> {
                String line;
                try {
                    while ((line = br.readLine()) != null) {
                        String finalLine = line;
                        handleServerMessage(finalLine);
                    }
                } catch (IOException e) {
                    System.out.println("서버 연결 종료됨");
                    closeConnection();
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "서버 연결 실패", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleServerMessage(String msg) {
        System.out.println("SERVER: " + msg);
    }

    private void sendToServer(String message) {
        try {
            if (bw != null) {
                bw.write(message + "\n");
                bw.flush();
            }
        } catch (IOException ex) {
            System.out.println("메시지 전송 실패");
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            if (sock != null && !sock.isClosed()) {
                sock.shutdownInput();
                sock.shutdownOutput();
                sock.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try { if (br != null) br.close(); } catch (IOException ignored) {}
        try { if (bw != null) bw.close(); } catch (IOException ignored) {}
    }

    private String extractValue(String json, String key) {
        String pattern = String.format("\"%s\":\"(.*?)\"", key);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
    
}
