
// JavaObjClientView.java ObjecStram 기반 Client
//실질적인 채팅 창
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class JavaGameClientView extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtInput;
	private String UserName;
	private JButton btnSend;
	private static final int BUF_LEN = 128; // Windows 처럼 BUF_LEN 을 정의
	private Socket socket; // 연결소켓

	private ObjectInputStream ois;
	private ObjectOutputStream oos;

	private JLabel lblUserName;
	// private JTextArea textArea;
	private JTextPane textArea;

	private Frame frame;
	private FileDialog fd;
	private JButton imgBtn;

	JPanel panel;
	private JLabel lblMouseEvent;
	private Graphics gc;
	private int pen_size = 2; // minimum 2
	// 그려진 Image를 보관하는 용도, paint() 함수에서 이용한다.
	private Image panelImage = null;
	private Graphics gc2 = null;
	private Graphics gc3 = null;
	private Graphics gc4 = null;

	JLabel timerLabel;
	JLabel turnLabel;

	// 서버에서 받은 블록 목록
	private int myBlock[] = new int[18];
	private int preClickedBlock[] = { -1, -1, 0 };
	private int clickedBlock[] = { -1, -1, 0 };
	private boolean isChecked = false;
	private int[] pyramid = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

	// 서버에서 받은 입장순서
	private int userSeq;
	private int newUser;
	private String recvData = "";
	private String recvSeq = "";
	private String recvUser = "";
	private String recvTimer = "";
	private String recvTurn = "";

	private int blockSelected = 0;

	private Lock lock = new Lock();

	/**
	 * Create the frame.
	 */

	// @throws BadLocationException
	public JavaGameClientView(String username, String ip_addr, String port_no) {
		/**************************************************
		 * 메인 프레임
		 *************************************************/
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1024, 768);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		/*************************************************
		 * 게임 배경
		 *************************************************/
		panel = new JPanel();
		panel.setLayout(null);
		panel.setBorder(new LineBorder(new Color(0, 0, 0)));
		panel.setBackground(Color.WHITE);
		panel.setBounds(10, 10, 640, 610);
		contentPane.add(panel);
		gc = panel.getGraphics();

		Image GameBackImage = new ImageIcon("src/GameBackImage.png").getImage();
		JLabel GameBackgroundLabel = new JLabel();
		GameBackgroundLabel = new JLabel(new ImageIcon(GameBackImage));
		GameBackgroundLabel.setBounds(0, 0, 640, 610);
		panel.add(GameBackgroundLabel, 0);

		// Image 영역 보관용. paint() 에서 이용한다.
//		panelImage = createImage(panel.getWidth(), panel.getHeight());
//		gc2 = panelImage.getGraphics();
//		gc2.setColor(panel.getBackground());
//		gc2.fillRect(0, 0, panel.getWidth(), panel.getHeight());
//		gc2.setColor(Color.BLACK);
//		gc2.drawRect(0, 0, panel.getWidth() - 1, panel.getHeight() - 1);

		/**************************************************
		 * 마우스 좌표
		 *************************************************/
		lblMouseEvent = new JLabel("<dynamic>");
		lblMouseEvent.setHorizontalAlignment(SwingConstants.CENTER);
		lblMouseEvent.setFont(new Font("굴림", Font.BOLD, 14));
		lblMouseEvent.setBorder(new LineBorder(new Color(0, 0, 0)));
		lblMouseEvent.setBackground(Color.WHITE);
//		lblMouseEvent.setBounds(10, 680, 190, 40);
		contentPane.add(lblMouseEvent);

		/**************************************************
		 * 유저가 가진 블럭
		 *************************************************/
		blockPanelBack = new JPanel();
		blockPanelBack.setBackground(SystemColor.menu);
		blockPanelBack.setBounds(210, 630, 440, 90);
		contentPane.add(blockPanelBack);
		blockPanelBack.setLayout(null);
		gc3 = blockPanelBack.getGraphics();
		// gc3.fillRect(210, 630, 440, 90);

		/**************************************************
		 * 타이머
		 *************************************************/

		timerLabel = new JLabel("제한시간");
		timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
		timerLabel.setFont(new Font("굴림", Font.BOLD, 14));
		timerLabel.setBorder(new LineBorder(new Color(0, 0, 0)));
		timerLabel.setBackground(Color.WHITE);
		timerLabel.setBounds(10, 630, 190, 40);
		contentPane.add(timerLabel);
		
		/**************************************************
		 * 자기 차례
		 *************************************************/
		
		turnLabel = new JLabel("순서");
		turnLabel.setHorizontalAlignment(SwingConstants.CENTER);
		turnLabel.setFont(new Font("굴림", Font.BOLD, 14));
		turnLabel.setBorder(new LineBorder(new Color(0, 0, 0)));
		turnLabel.setBackground(Color.WHITE);
		turnLabel.setBounds(10, 680, 190, 40);
		contentPane.add(turnLabel);

		/**************************************************
		 * 접속중인 유저
		 *************************************************/
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				userPanels[i * 2 + j] = new JPanel();
				userPanels[i * 2 + j].setBorder(new LineBorder(new Color(0, 0, 0)));
				userPanels[i * 2 + j].setBackground(Color.WHITE);
				userPanels[i * 2 + j].setBounds(665 + 170 * j, 10 + 185 * i, 160, 175);
				contentPane.add(userPanels[i * 2 + j]);
			}
		}

		/**************************************************
		 * 채팅창(채팅창 - +버튼 - 입력창 - send - 닉네임 - 종료)
		 *************************************************/
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(665, 380, 330, 240);
		contentPane.add(scrollPane);

		textArea = new JTextPane();
		textArea.setEditable(false);
		textArea.setFont(new Font("굴림체", Font.PLAIN, 14));
		scrollPane.setViewportView(textArea);

		btnSend = new JButton("Send");
		btnSend.setFont(new Font("굴림", Font.PLAIN, 14));
		btnSend.setBounds(925, 630, 70, 40);
		contentPane.add(btnSend);

		txtInput = new JTextField();
		txtInput.setBounds(715, 630, 200, 40);
		contentPane.add(txtInput);
		txtInput.setColumns(10);

		imgBtn = new JButton("+");
		imgBtn.setFont(new Font("굴림", Font.PLAIN, 16));
		imgBtn.setBounds(665, 630, 40, 40);
		contentPane.add(imgBtn);

		lblUserName = new JLabel("Name");
		lblUserName.setBorder(new LineBorder(new Color(0, 0, 0)));
		lblUserName.setBackground(Color.WHITE);
		lblUserName.setFont(new Font("굴림", Font.BOLD, 14));
		lblUserName.setHorizontalAlignment(SwingConstants.CENTER);
		lblUserName.setBounds(665, 680, 160, 40);
		contentPane.add(lblUserName);
		setVisible(true);

		JButton btnNewButton = new JButton("종 료");
		btnNewButton.setFont(new Font("굴림", Font.PLAIN, 14));
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ChatMsg msg = new ChatMsg(UserName, "400", "Bye");
				SendObject(msg);
				System.exit(0);
			}
		});
		btnNewButton.setBounds(835, 680, 160, 40);
		contentPane.add(btnNewButton);

		/**************************************************
		 * 현재 유저 접속정보 알림
		 *************************************************/
		AppendText("User " + username + " connecting " + ip_addr + " " + port_no);
		UserName = username;
		lblUserName.setText(username);

		try {
			socket = new Socket(ip_addr, Integer.parseInt(port_no));

			oos = new ObjectOutputStream(socket.getOutputStream());
			oos.flush();
			ois = new ObjectInputStream(socket.getInputStream());

			// SendMessage("/login " + UserName);
			ChatMsg obcm = new ChatMsg(UserName, "100", "Hello");
			SendObject(obcm);

			ListenNetwork net = new ListenNetwork();
			net.start();
			TextSendAction action = new TextSendAction();
			btnSend.addActionListener(action);
			txtInput.addActionListener(action);
			txtInput.requestFocus();

			ImageSendAction action2 = new ImageSendAction();
			imgBtn.addActionListener(action2);

			MyMouseEvent mouse = new MyMouseEvent("G");
			panel.addMouseMotionListener(mouse);
			panel.addMouseListener(mouse);

			MyMouseEvent mouse2 = new MyMouseEvent("P");
			blockPanelBack.addMouseMotionListener(mouse2);
			blockPanelBack.addMouseListener(mouse2);

//			MyMouseWheelEvent wheel = new MyMouseWheelEvent();
//			panel.addMouseWheelListener(wheel);

		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
			AppendText("connect error");
		}

	}

//	public void paint(Graphics g) {
//		super.paint(g);
//		// Image 영역이 가려졌다 다시 나타날 때 그려준다.
//		gc.drawImage(panelImage, 0, 0, this);
//	}

	// Server Message를 수신해서 화면에 표시
	class ListenNetwork extends Thread {
		public void run() {
			while (true) {
				try {
					Object obcm = null;
					String msg = null;
					ChatMsg cm;
					try {
						obcm = ois.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						break;
					}
					if (obcm == null)
						break;
					if (obcm instanceof ChatMsg) {
						cm = (ChatMsg) obcm;
						msg = String.format("[%s]\n%s", cm.UserName, cm.data);
					} else
						continue;
					switch (cm.code) {

					case "120": // 유저 이미지 띄우기
						recvSeq(cm);
						userSeq = Integer.parseInt(recvSeq);

						// userPanel 배열화
						for (int i = 0; i <= userSeq; i++) {
							Image userImg = null;
							userImg = new ImageIcon("src/userImage/User" + (i + 1) + ".JPG").getImage();
							if (userImg != null) {
								userLabel[i] = new JLabel();
								userLabel[i].setBorder(new LineBorder(new Color(0, 0, 0)));
								userLabel[i] = new JLabel(new ImageIcon(userImg));
								userPanels[i].add(userLabel[i]);
							}
						}
						repaint();
						break;

					case "150": // 새로운 유저입장
						recvUser(cm);
						newUser = Integer.parseInt(recvUser);
						for (int i = 0; i <= newUser; i++) {
							Image userImg = null;
							userImg = new ImageIcon("src/userImage/User" + (i + 1) + ".JPG").getImage();
							if (userImg != null) {
								userLabel[i] = new JLabel();
								userLabel[i].setBorder(new LineBorder(new Color(0, 0, 0)));
								userLabel[i] = new JLabel(new ImageIcon(userImg));
								userPanels[i].add(userLabel[i]);
							}
						}
						repaint();
						break;

					case "170": // 타이머 작동
						recvTimer(cm);

						// timerLabel.setText("");
						timerLabel.setText(recvTimer);

						break;
						
					case "180": // 자기 차례
						recvTurn(cm);
						
						turnLabel.setText(recvTurn);						
						break;						

					case "200": // chat message
						if (cm.UserName.equals(UserName))
							AppendTextR(msg); // 내 메세지는 우측에
						else
							AppendText(msg);
						break;

					case "300": // Image 첨부
						if (cm.UserName.equals(UserName))
							AppendTextR("[" + cm.UserName + "]");
						else
							AppendText("[" + cm.UserName + "]");
						AppendImage(cm.img);
						break;

					case "500": // Mouse Event 수신
						DoMouseEvent(cm);
						break;

					case "301": // 클릭한 블럭 서버로부터 수신
						for (int i = 0; i < 3; i++) {
							preClickedBlock[i] = clickedBlock[i];
						}
						recvBlock(cm);
						String[] repStr = recvData.replaceAll("\\[", "").replaceAll("]", "").split(",");
						try {
							clickedBlock[0] = Integer.parseInt(repStr[0].trim());
							clickedBlock[1] = Integer.parseInt(repStr[1].trim());
							clickedBlock[2] = Integer.parseInt(repStr[2].trim());
						} catch (Exception e) {
							System.out.println("Unable to parse string to int: " + e.getMessage());
						}
						AppendText("클릭한 블럭 " + Arrays.toString(clickedBlock));

						// 클릭한 블럭이 있다면,
						if (preClickedBlock[0] != -1 && preClickedBlock[1] != -1) {
							blockLabel[preClickedBlock[0]][preClickedBlock[1]].setBorder(new EmptyBorder(0, 0, 0, 0));
						}
						// 테두리 만들어서 클릭 표시
						blockLabel[clickedBlock[0]][clickedBlock[1]].setBorder(new LineBorder(new Color(0, 0, 0), 3));

						break;

					case "302": // 블럭을 놓을 수 있는 위치가 반영된 배경 수신
						recvBlock(cm);

						String[] recvBackU = recvData.replaceAll("\\[", "").replaceAll("]", "").split(",");
						try {
							for (int i = 0; i < 64; i++) {
								pyramid[i] = Integer.parseInt(recvBackU[i].trim());
							}
						} catch (Exception e) {
							System.out.println("Unable to get ServerBlock" + e.getMessage());
						}

						// 제대로 수신했는지 확인
						AppendText("받은 게임 배경" + Arrays.toString(pyramid));

						try {
							lock.lock();
						} catch (InterruptedException e1) {
						}
						panel.repaint();
						// 값이 6인 블럭을 guidBlock.png 으로 변경
						for (int i = 63; i > 0; i--) {
							// 배열 상에서, 대각선 포함, 아래 블럭만 출력
							if ((i / 8) >= (i % 8)) {
								Image gameBlockImage = new ImageIcon("src/blockColor/Block" + (pyramid[i] + 1) + ".png")
										.getImage();
								gameLabel[i / 8][i % 8] = new JLabel(new ImageIcon(gameBlockImage));
								gamePanel[i / 8][i % 8] = new JPanel();
								gamePanel[i / 8][i % 8].setBounds(56 * (i % 8) - 28 * (i / 8) + 300, 56 * (i / 8) + 138,
										40, 40);
								gamePanel[i / 8][i % 8].add(gameLabel[i / 8][i % 8]);
								panel.add(gamePanel[i / 8][i % 8]);
							}
						}
						lock.unlock();

						break;

					case "311": // 게임화면 클릭했을 때
						recvBlock(cm);

//						repStr = recvData.split(" ");
//						int[] gameBlock = new int[3];
//						gameBlock[0] = Integer.parseInt(repStr[0].trim());
//						gameBlock[1] = Integer.parseInt(repStr[1].trim());
//						gameBlock[2] = Integer.parseInt(repStr[2].trim());

						String[] recvBackG = recvData.replaceAll("\\[", "").replaceAll("]", "").split(",");
						try {
							for (int i = 0; i < 64; i++) {
								pyramid[i] = Integer.parseInt(recvBackG[i].trim());
							}
						} catch (Exception e) {
							System.out.println("Unable to get ServerBlock" + e.getMessage());
						}

						// 제대로 수신했는지 확인
						AppendText("받은 게임 배경" + Arrays.toString(pyramid));

						try {
							lock.lock();
						} catch (InterruptedException e1) {
						}
						panel.repaint();
						// 값이 6인 블럭을 guidBlock.png 으로 변경
						for (int i = 63; i > 0; i--) {
							// 배열 상에서, 대각선 포함, 아래 블럭만 출력
							if ((i / 8) >= (i % 8)) {
								Image gameBlockImage = new ImageIcon("src/blockColor/Block" + (pyramid[i] + 1) + ".png")
										.getImage();
								gameLabel[i / 8][i % 8] = new JLabel(new ImageIcon(gameBlockImage));
								gamePanel[i / 8][i % 8] = new JPanel();
								gamePanel[i / 8][i % 8].setBounds(56 * (i % 8) - 28 * (i / 8) + 300, 56 * (i / 8) + 138,
										40, 40);
								gamePanel[i / 8][i % 8].add(gameLabel[i / 8][i % 8]);
								panel.add(gamePanel[i / 8][i % 8]);
							}
						}
						lock.unlock();

//						pyramid[gameBlock[0] * 8 + gameBlock[1]] = gameBlock[2];
//
//						// 게임화면에 블럭 그려주기
//						Image gameBlockImage = new ImageIcon("src/blockColor/Block" + (gameBlock[2] + 1) + ".png")
//								.getImage();
//						gameLabel[gameBlock[0]][gameBlock[1]] = new JLabel(new ImageIcon(gameBlockImage));
//						gamePanel[gameBlock[0]][gameBlock[1]] = new JPanel();
//						gamePanel[gameBlock[0]][gameBlock[1]].setBounds(56 * gameBlock[1] - 28 * gameBlock[0] + 300,
//								56 * gameBlock[0] + 138, 40, 40);
//						gamePanel[gameBlock[0]][gameBlock[1]].add(gameLabel[gameBlock[0]][gameBlock[1]]);
//						panel.add(gamePanel[gameBlock[0]][gameBlock[1]]);
//
//						AppendText("변경후" + Arrays.toString(pyramid));

						break;

					case "350": // 잘못된 위치 클릭
						recvBlock(cm);

						repStr = recvData.split(" ");
						int[] clickedUserBlock = new int[3];
						clickedUserBlock[0] = Integer.parseInt(repStr[0].trim());
						clickedUserBlock[1] = Integer.parseInt(repStr[1].trim());
						clickedUserBlock[2] = Integer.parseInt(repStr[2].trim());

						myBlock[clickedUserBlock[0] * 8 + clickedUserBlock[1]] = clickedUserBlock[2];

						blockLabel[clickedUserBlock[0]][clickedUserBlock[1]].setBorder(new EmptyBorder(0, 0, 0, 0));

						break;

					case "321": // 가진 블럭 갱신
						// 유저 블럭 갱신
						Image reBackImg = null;
						reBackImg = new ImageIcon("src/blockColor/noBlock.png").getImage();
						blockLabel[clickedBlock[0]][clickedBlock[1]].setIcon(new ImageIcon(reBackImg));
						blockLabel[clickedBlock[0]][clickedBlock[1]].setBorder(new EmptyBorder(0, 0, 0, 0));

						break;

					case "360": // 플레이어가 부족함
						AppendText(msg);
						break;

					case "370": // 게임 시작
						AppendText(msg);
						break;

					case "351": // 아직 차례가 아님
						AppendText(msg);
						break;

					case "371": // 최초 Block 수신
						/********************************************
						 * 블록 수신, 출력
						 *******************************************/
						recvBlock(cm);
						// AppendText("변경전" + recvData);

						repStr = recvData.replaceAll("\\[", "").replaceAll("]", "").split(",");
						myBlock = new int[repStr.length];
						for (int i = 0; i < repStr.length; i++) {
							try {
								myBlock[i] = Integer.parseInt(repStr[i].trim());
							} catch (Exception e) {
								System.out.println("Unable to parse string to int: " + e.getMessage());
							}
						}
						AppendText("변경후" + Arrays.toString(myBlock));

						for (int i = 0; i < 2; i++) {
							for (int j = 0; j < 9; j++) {
								Image backImg = null;
								backImg = new ImageIcon("src/blockColor/Block" + (myBlock[i * 9 + j] + 1) + ".png")
										.getImage();
								if (backImg != null) {
									blockLabel[i][j] = new JLabel();
									blockLabel[i][j] = new JLabel(new ImageIcon(backImg));
									blockLabel[i][j].setBounds(j * 50, i * 50, 40, 40);
									blockPanelBack.add(blockLabel[i][j]);
								}
							}
						}
						repaint();
						break;
					}
				} catch (IOException e) {
					AppendText("ois.readObject() error");
					try {
						ois.close();
						oos.close();
						socket.close();

						break;
					} catch (Exception ee) {
						break;
					} // catch문 끝
				} // 바깥 catch문끝

			}
		}
	}

	// recv Block 처리
	private void recvBlock(ChatMsg cm) {
		recvData = cm.data;
	}

	// recv Seq 처리
	private void recvSeq(ChatMsg cm) {
		recvSeq = cm.data;
	}

	// recv User 처리
	private void recvUser(ChatMsg cm) {
		recvUser = cm.data;
	}

	// recv Timer 처리
	private void recvTimer(ChatMsg cm) {
		recvTimer = cm.data;
	}
	
	// recv Turn 처리
	private void recvTurn(ChatMsg cm) {
		recvTurn = cm.data;
	}

	// Mouse Event 수신 처리
	public void DoMouseEvent(ChatMsg cm) {
		Color c;
		if (cm.UserName.matches(UserName)) // 본인 것은 이미 Local 로 그렸다.
			return;
		c = new Color(255, 0, 0); // 다른 사람 것은 Red
		gc2.setColor(c);
		gc2.fillOval(cm.mouse_e.getX() - pen_size / 2, cm.mouse_e.getY() - cm.pen_size / 2, cm.pen_size, cm.pen_size);
		gc.drawImage(panelImage, 0, 0, panel);
	}

	public void SendMouseEvent(MouseEvent e) {
		ChatMsg cm = new ChatMsg(UserName, "500", "MOUSE");
		cm.mouse_e = e;
		cm.pen_size = pen_size;
		SendObject(cm);
	}

//	class MyMouseWheelEvent implements MouseWheelListener {
//		@Override
//		public void mouseWheelMoved(MouseWheelEvent e) {
//			if (e.getWheelRotation() < 0) { // 위로 올리는 경우 pen_size 증가
//				if (pen_size < 20)
//					pen_size++;
//			} else {
//				if (pen_size > 2)
//					pen_size--;
//			}
//			lblMouseEvent.setText("mouseWheelMoved Rotation=" + e.getWheelRotation() + " pen_size = " + pen_size + " "
//					+ e.getX() + "," + e.getY());
//
//		}
//	}

	// Mouse Event Handler
	class MyMouseEvent implements MouseListener, MouseMotionListener {
		private String name;

		public MyMouseEvent(String name) {
			this.name = name;
		}

		@Override
		public void mouseDragged(MouseEvent e) {
//			lblMouseEvent.setText(e.getButton() + " mouseDragged " + e.getX() + "," + e.getY());// 좌표출력가능
//			Color c = new Color(0, 0, 255);
//			gc2.setColor(c);
//			gc2.fillOval(e.getX() - pen_size / 2, e.getY() - pen_size / 2, pen_size, pen_size);
//			// panelImnage는 paint()에서 이용한다.
//			gc.drawImage(panelImage, 0, 0, panel);
//			SendMouseEvent(e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			lblMouseEvent.setText(name + " " + e.getButton() + " mouseMoved " + e.getX() + "," + e.getY());
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			lblMouseEvent.setText(name + " " + e.getButton() + " mouseClicked " + e.getX() + "," + e.getY());
//			Color c = new Color(0, 0, 255);
//			gc2.setColor(c);
//			gc2.fillOval(e.getX() - pen_size / 2, e.getY() - pen_size / 2, pen_size, pen_size);
//			gc.drawImage(panelImage, 0, 0, panel);
//			SendMouseEvent(e);

			if (name.equals("P")) {
				int x = -1, y = -1;
				int reClicked = 0;
				blockSelected = 1;

				// 블럭 좌표 계산해서, 몇 번째 블록인지 전송
				y = e.getX() / 50;
				x = e.getY() / 50;

				if (myBlock[x * 8 + y] != -1) {
					// String msg = name + " " + e.getX() + " " + e.getY();

					if (clickedBlock[0] == y && clickedBlock[1] == x) {
						reClicked = 1;
					}

					String msg = x + " " + y + " " + myBlock[x * 9 + y];
					ChatMsg obcm = new ChatMsg(UserName, "300", msg);
					SendObject(obcm);

				} else {
					return;
				}
			}

			if (name.equals("G") && blockSelected == 1) {
				int x = -1, y = -1;

				x = (e.getY() - 138) / 56;
				y = ((e.getX() - 104) - (28 * (7 - x))) / 56;

				myBlock[clickedBlock[0] * 8 + clickedBlock[1]] = -1;

				String msg = x + " " + y;
				ChatMsg obcm = new ChatMsg(UserName, "310", msg);
				SendObject(obcm);

				blockSelected = 0;
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			lblMouseEvent.setText(name + " " + e.getButton() + " mouseEntered " + e.getX() + "," + e.getY());
			// panel.setBackground(Color.YELLOW);

		}

		@Override
		public void mouseExited(MouseEvent e) {
			lblMouseEvent.setText(name + " " + e.getButton() + " mouseExited " + e.getX() + "," + e.getY());
			// panel.setBackground(Color.CYAN);

		}

		@Override
		public void mousePressed(MouseEvent e) {
			lblMouseEvent.setText(name + " " + e.getButton() + " mousePressed " + e.getX() + "," + e.getY());

		}

		@Override
		public void mouseReleased(MouseEvent e) {
			lblMouseEvent.setText(name + " " + e.getButton() + " mouseReleased " + e.getX() + "," + e.getY());
			// 드래그중 멈출시 보임

		}
	}

	// keyboard enter key 치면 서버로 전송
	class TextSendAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			// Send button을 누르거나 메시지 입력하고 Enter key 치면
			if (e.getSource() == btnSend || e.getSource() == txtInput) {
				String msg = null;
				// msg = String.format("[%s] %s\n", UserName, txtInput.getText());
				msg = txtInput.getText();
				SendMessage(msg);
				txtInput.setText(""); // 메세지를 보내고 나면 메세지 쓰는창을 비운다.
				txtInput.requestFocus(); // 메세지를 보내고 커서를 다시 텍스트 필드로 위치시킨다
				if (msg.contains("/exit")) // 종료 처리
					System.exit(0);
			}
		}
	}

	class ImageSendAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			// 액션 이벤트가 sendBtn일때 또는 textField 에세 Enter key 치면
			if (e.getSource() == imgBtn) {
				frame = new Frame("이미지첨부");
				fd = new FileDialog(frame, "이미지 선택", FileDialog.LOAD);
				// frame.setVisible(true);
				// fd.setDirectory(".\\");
				fd.setVisible(true);
				// System.out.println(fd.getDirectory() + fd.getFile());
				if (fd.getDirectory().length() > 0 && fd.getFile().length() > 0) {
					ChatMsg obcm = new ChatMsg(UserName, "171", "IMG");
					ImageIcon img = new ImageIcon(fd.getDirectory() + fd.getFile());
					obcm.img = img;
					SendObject(obcm);
				}
			}
		}
	}

	ImageIcon icon1 = new ImageIcon("src/sampleImage/icon1.jpg");
	private JPanel blockPanelBack;
	private JPanel timerPanelBack;
	private JPanel turnPanelBack;
	private JPanel gamePanel[][] = new JPanel[8][8];
	private JPanel userPanels[] = new JPanel[4];
	private JLabel blockLabel[][] = new JLabel[2][9];
	private JLabel userLabel[] = new JLabel[4];
	private JLabel gameLabel[][] = new JLabel[8][8];

	public void AppendIcon(ImageIcon icon) {
		int len = textArea.getDocument().getLength();
		// 끝으로 이동
		textArea.setCaretPosition(len);
		textArea.insertIcon(icon);
	}

	// 화면에 출력
	public void AppendText(String msg) {
		// textArea.append(msg + "\n");
		// AppendIcon(icon1);
		msg = msg.trim(); // 앞뒤 blank와 \n을 제거한다.

		StyledDocument doc = textArea.getStyledDocument();
		SimpleAttributeSet left = new SimpleAttributeSet();
		StyleConstants.setAlignment(left, StyleConstants.ALIGN_LEFT);
		StyleConstants.setForeground(left, Color.BLACK);
		doc.setParagraphAttributes(doc.getLength(), 1, left, false);
		try {
			doc.insertString(doc.getLength(), msg + "\n", left);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		int len = textArea.getDocument().getLength();
		// 끝으로 이동
		textArea.setCaretPosition(len);
		textArea.replaceSelection(msg + "\n");

	}

	// 화면 우측에 출력
	public void AppendTextR(String msg) {
		msg = msg.trim(); // 앞뒤 blank와 \n을 제거한다.

		StyledDocument doc = textArea.getStyledDocument();
		SimpleAttributeSet right = new SimpleAttributeSet();
		StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
		StyleConstants.setForeground(right, Color.BLUE);
		doc.setParagraphAttributes(doc.getLength(), 1, right, false);
		try {
			doc.insertString(doc.getLength(), msg + "\n", right);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		int len = textArea.getDocument().getLength();
		// 끝으로 이동
		textArea.setCaretPosition(len);
		textArea.replaceSelection(msg + "\n");

	}

	public void AppendImage(ImageIcon ori_icon) {
		int len = textArea.getDocument().getLength();
		textArea.setCaretPosition(len); // place caret at the end (with no selection)
		Image ori_img = ori_icon.getImage();
		Image new_img;
		ImageIcon new_icon;
		int width, height;
		double ratio;
		width = ori_icon.getIconWidth();
		height = ori_icon.getIconHeight();
		// Image가 너무 크면 최대 가로 또는 세로 200 기준으로 축소시킨다.
		if (width > 200 || height > 200) {
			if (width > height) { // 가로 사진
				ratio = (double) height / width;
				width = 200;
				height = (int) (width * ratio);
			} else { // 세로 사진
				ratio = (double) width / height;
				height = 200;
				width = (int) (height * ratio);
			}
			new_img = ori_img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			new_icon = new ImageIcon(new_img);
			textArea.insertIcon(new_icon);
		} else {
			textArea.insertIcon(ori_icon);
			new_img = ori_img;
		}
		len = textArea.getDocument().getLength();
		textArea.setCaretPosition(len);
		textArea.replaceSelection("\n");
		// ImageViewAction viewaction = new ImageViewAction();
		// new_icon.addActionListener(viewaction); // 내부클래스로 액션 리스너를 상속받은 클래스로
		// panelImage = ori_img.getScaledInstance(panel.getWidth(), panel.getHeight(),
		// Image.SCALE_DEFAULT);

		// gc2.drawImage(ori_img, 0, 0, panel.getWidth(), panel.getHeight(), panel);
		// gc.drawImage(panelImage, 0, 0, panel.getWidth(), panel.getHeight(), panel);
	}

	// Windows 처럼 message 제외한 나머지 부분은 NULL 로 만들기 위한 함수
	public byte[] MakePacket(String msg) {
		byte[] packet = new byte[BUF_LEN];
		byte[] bb = null;
		int i;
		for (i = 0; i < BUF_LEN; i++)
			packet[i] = 0;
		try {
			bb = msg.getBytes("euc-kr");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(0);
		}
		for (i = 0; i < bb.length; i++)
			packet[i] = bb[i];
		return packet;
	}

	// Server에게 network으로 전송
	public void SendMessage(String msg) {
		try {
			ChatMsg obcm = new ChatMsg(UserName, "200", msg);
			oos.writeObject(obcm);
		} catch (IOException e) {
			AppendText("oos.writeObject() error");
			try {
				ois.close();
				oos.close();
				socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(0);
			}
		}
	}

	public void SendObject(Object ob) { // 서버로 메세지를 보내는 메소드
		try {
			oos.writeObject(ob);
		} catch (IOException e) {
			AppendText("SendObject Error");
		}
	}

	// 화면 갱신 중 lock을 걸기 위한 내부클래스
	class Lock {
		private boolean isLocked = false;

		public synchronized void lock() throws InterruptedException {
			while (isLocked) {
				wait();
			}
			isLocked = true;
		}

		public synchronized void unlock() {
			isLocked = false;
			notify();
		}
	}
}
