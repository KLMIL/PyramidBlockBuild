
//JavaObjServer.java ObjectStream 기반 채팅 Server

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class JavaGameServer extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextArea textArea;
	private JTextField txtPortNumber;

	private ServerSocket socket; // 서버소켓
	private Socket client_socket; // accept() 에서 생성된 client 소켓
	private static final int BUF_LEN = 128; // Windows 처럼 BUF_LEN 을 정의

	@SuppressWarnings("rawtypes")
	private Vector UserVec = new Vector(); // 연결된 사용자를 저장할 벡터

	private int[] sBlock = { 0, 0, 0, 0, 0, 6 };; // 한 게임에 할당되는 블럭(5 X 7ea + 1ea)
	private int[][] pyramid = new int[8][8]; // 배경에 배치되는 블록 배열(8 x 8)
	private int deadUser = 0;
	private int gameSeq = 0;
	private int tCount = 11;

	/****************************************************************
	 * Launch the application.
	 ***************************************************************/
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JavaGameServer frame = new JavaGameServer();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/****************************************************************
	 * Create the frame
	 ***************************************************************/
	public JavaGameServer() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 338, 440);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 10, 300, 298);
		contentPane.add(scrollPane);

		textArea = new JTextArea();
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);

		JLabel lblNewLabel = new JLabel("Port Number");
		lblNewLabel.setBounds(13, 318, 87, 26);
		contentPane.add(lblNewLabel);

		txtPortNumber = new JTextField();
		txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
		txtPortNumber.setText("30000");
		txtPortNumber.setBounds(112, 318, 199, 26);
		contentPane.add(txtPortNumber);
		txtPortNumber.setColumns(10);

		JButton btnServerStart = new JButton("Server Start");
		btnServerStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					socket = new ServerSocket(Integer.parseInt(txtPortNumber.getText()));
				} catch (NumberFormatException | IOException e1) {
					e1.printStackTrace();
				}
				AppendText("Chat Server Running..");
				btnServerStart.setText("Chat Server Running..");
				btnServerStart.setEnabled(false); // 서버를 더이상 실행시키지 못 하게 막는다
				txtPortNumber.setEnabled(false); // 더이상 포트번호 수정못 하게 막는다
				AcceptServer accept_server = new AcceptServer();
				accept_server.start();
			}
		});
		btnServerStart.setBounds(12, 356, 300, 35);
		contentPane.add(btnServerStart);

		// GameBlock 초기화
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				pyramid[i][j] = -1;
			}
		}
	}

	// 새로운 참가자 accept() 하고 user thread를 새로 생성한다.
	class AcceptServer extends Thread {
		@SuppressWarnings("unchecked")
		public void run() {
			while (true) { // 사용자 접속을 계속해서 받기 위해 while문
				try {
					AppendText("Waiting new clients ...");
					client_socket = socket.accept(); // accept가 일어나기 전까지는 무한 대기중
					AppendText("새로운 참가자 from " + client_socket);
					// User 당 하나씩 Thread 생성
					UserService new_user = new UserService(client_socket);
					UserVec.add(new_user); // 새로운 참가자 배열에 추가
					new_user.start(); // 만든 객체의 스레드 실행
					AppendText("현재 참가자 수 " + UserVec.size());
				} catch (IOException e) {
					AppendText("accept() error");
				}
			}
		}
	}

	/****************************************************************
	 * textArea에 출력
	 ***************************************************************/

	public void AppendText(String str) {
		textArea.append(str + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	public void AppendObject(ChatMsg msg) {

		textArea.append("code = " + msg.code + "\n");
		textArea.append("id = " + msg.UserName + "\n");
		textArea.append("data = " + msg.data + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	// User 당 생성되는 Thread
	// Read One 에서 대기 -> Write All
	class UserService extends Thread {
		private ObjectInputStream ois;
		private ObjectOutputStream oos;

		private Socket client_socket;
		@SuppressWarnings("rawtypes")
		private Vector user_vc;
		private String UserName = "";
		private String UserStatus;
		private String Seq;

		private boolean isAssigned = false;
		private int[] userBlock = new int[18];
		private Random random = new Random();

		private int[] cuBlock = { -1, -1, 0 };
		private int[] cgBlock = { -1, -1 };

		private boolean isDead = false;

		public UserService(Socket client_socket) {
			// 매개변수로 넘어온 자료 저장
			this.client_socket = client_socket;
			this.user_vc = UserVec;
			try {
				oos = new ObjectOutputStream(client_socket.getOutputStream());
				oos.flush();
				ois = new ObjectInputStream(client_socket.getInputStream());
			} catch (Exception e) {
				AppendText("userService error");
			}
		}

		public void run() {
			while (true) { // 사용자 접속을 계속해서 받기 위해 while문
				try {
					Object obcm = null;
					String msg = null;
					ChatMsg cm = null;
					if (socket == null)
						break;

					try {
						obcm = ois.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}

					if (obcm == null)
						break;

					if (obcm instanceof ChatMsg) {
						cm = (ChatMsg) obcm;
						AppendObject(cm);
					} else {
						continue;
					}

					/**************************************
					 * Protocol 처리부분 시작
					 **************************************/
					switch (cm.code) {
					case "100": // 최초 접속
						UserName = cm.UserName;
						UserStatus = "O"; // Online 상태
						Login();

						// 게임 시작
						if (UserVec.size() == 3) {
							assignUserBlock();
							String tempStr = "피라미드 블럭쌓기 게임을 시작합니다.";
							obcm = new ChatMsg("SERVER", "370", tempStr);
							WriteAllObject(obcm);
							timer();
							turn();							
						}
						break;

					case "200": // 일반 메시지
						msg = String.format("[%s] %s", cm.UserName, cm.data);
						userChat(msg, cm);
						break;

					case "300": // 유저블럭 클릭 처리
						if (UserVec.size() < 3) {
							String tempStr = "3명이 입장해야 게임이 시작됩니다.";
							oos.writeObject(new ChatMsg("SERVER", "360", tempStr));
						} else {
							userBlockClicked(cm);
							// obcm = userBlockClicked(cm);
							// oos.writeObject(obcm);
						}
						break;

					case "310": // 게임화면 블럭 클릭 처리
						if (UserVec.size() < 3) {
							String tempStr = "3명이 입장해야 게임이 시작됩니다.";
							oos.writeObject(new ChatMsg("SERVER", "360", tempStr));
						} else {
							if (this == UserVec.elementAt(gameSeq % 3)) {
								obcm = gameBlockClicked(cm);
								if (obcm != null) {
									WriteAllObject(obcm);
									oos.writeObject(new ChatMsg("SERVER", "321", ""));

									// 차례가 넘어감. 넘어갔을 때, 낼 수 있는 블럭이 없다면 죽음..
									gameSeq++;
									turn();									
									tCount = 11;
									if (isPlayerDead()) {
										gameSeq++;
										tCount = 11;
									}

								} else {
									String tempStr = cuBlock[0] + " " + cuBlock[1] + " " + cuBlock[2];
									oos.writeObject(new ChatMsg("SERVER", "350", tempStr));
								}
							} else {
								String tempStr = "아직 차례가 아닙니다.";
								oos.writeObject(new ChatMsg("SERVER", "351", tempStr));

								tempStr = cuBlock[0] + " " + cuBlock[1] + " " + cuBlock[2];
								oos.writeObject(new ChatMsg("SERVER", "350", tempStr));
							}
						}
						break;

					case "400": // 로그아웃
						Logout();
						break;

					default:
						WriteAllObject(cm);
					}
					/**************************************
					 * Protocol 처리부분 끝
					 **************************************/
				} catch (IOException e) {
					AppendText("ois.readObject() error");
					try {
						ois.close();
						oos.close();
						client_socket.close();
						Logout(); // 에러가난 현재 객체를 벡터에서 지운다
						break;
					} catch (Exception ee) {
						break;
					} // catch문 끝
				} // 바깥 catch문끝
			} // while
		} // run

		private boolean isPlayerDead() {
			UserService user = (UserService) UserVec.elementAt(gameSeq % 3);
			boolean[] doHaveBlock = { false, false, false, false, false, false };

			// 가지고 있는 블럭 표시
			for (int i = 0; i < 12; i++) {
				if (user.userBlock[i] != -1) {
					doHaveBlock[user.userBlock[i]] = true;
				}
			}

			// 놓을 수 있는 위치가 있다면 false return
			for (int k = 0; k < 6; k++) {
				if (doHaveBlock[k] == true) {
					for (int i = 7; i > 0; i--) {
						for (int j = 0; j <= i; j++) {
							// 제일 아랫칸은 놓을 수 있다.
							if (i == 7 && pyramid[i][j] == -1) {
								return false;
							} else if (pyramid[i][j] == -1) {
								// 아래 블럭 중 하나라도 현재 선택한 블럭과 같다면 놓을 수 있다. 보라색 블럭은 무조건 놓을 수 있다.
								if (pyramid[i + 1][j] == k || pyramid[i + 1][j + 1] == k || k == 5) {
									// 아래 블럭중 하나라도 놓여있지 않다면 놓을 수 없다.
									if (pyramid[i + 1][j] != -1 && pyramid[i + 1][j + 1] != -1 && pyramid[i + 1][j] != 6
											&& pyramid[i + 1][j + 1] != 6) {
										return false;
									}
								}
							}
						}
					}
				}
			}

			deadUser++;
			user.isDead = true;
			return true;
		}

		private Object gameBlockClicked(ChatMsg cm) {
			String[] cuBlockXY = cm.data.split(" ");
			cgBlock[0] = Integer.parseInt(cuBlockXY[0].trim());
			cgBlock[1] = Integer.parseInt(cuBlockXY[1].trim());

			String tempMsg = Arrays.toString(cgBlock);
			AppendText("(G)클릭한 블럭 " + tempMsg);

			// 가이드 블럭 초기화
			for (int i = 7; i > 0; i--) {
				for (int j = 0; j <= i; j++) {
					if (pyramid[i][j] == 6) {
						pyramid[i][j] = -1;
					}
				}
			}

			// 제일 아랫칸 or 아랫쪽이 같은 색의 블럭일 때
			// (i, j)의 자식노드 -> (i + 1, j), (i + 1, j + 1)

			if (cgBlock[0] == 7 || pyramid[cgBlock[0] + 1][cgBlock[1]] == cuBlock[2]
					|| pyramid[cgBlock[0] + 1][cgBlock[1] + 1] == cuBlock[2] || cuBlock[2] == 5) {
				// 아래칸중 한곳이라도 블럭이 없으면 return
				if (cgBlock[0] != 7) {
					if (pyramid[cgBlock[0] + 1][cgBlock[1]] == -1 || pyramid[cgBlock[0] + 1][cgBlock[1] + 1] == -1) {
						return null;
					}
				}
				if (cuBlock[0] != -1 && cuBlock[1] != -1 && pyramid[cgBlock[0]][cgBlock[1]] == -1) {
					pyramid[cgBlock[0]][cgBlock[1]] = userBlock[cuBlock[0] * 9 + cuBlock[1]];
					// 가진 블럭, 클릭한 블럭 초기화
					userBlock[cuBlock[0] * 9 + cuBlock[1]] = -1;
					cuBlock[0] = cuBlock[1] = -1;

					// 게임화면 배열 전달
//					String tempStr = cgBlock[0] + " " + cgBlock[1] + " " + cuBlock[2];
//					Object obcm = new ChatMsg("SERVER", "311", tempStr);

					String tempStr = Arrays.deepToString(pyramid);
					Object obcm = new ChatMsg("SERVER", "311", tempStr);

					return obcm;
				}
			}
			return null;
		}

		private void userBlockClicked(ChatMsg cm) {
			String[] cuBlockXY = cm.data.split(" ");
			cuBlock[0] = Integer.parseInt(cuBlockXY[0].trim());
			cuBlock[1] = Integer.parseInt(cuBlockXY[1].trim());
			cuBlock[2] = Integer.parseInt(cuBlockXY[2].trim());

			// 유저가 클릭한 블럭 부분
			AppendText("(P)클릭된 블럭 종류 " + cuBlock);

			String tempStr1 = Arrays.toString(cuBlock);
			AppendText("(P)클릭한 블럭 " + tempStr1);

			// 유저가 블럭을 놓을 수 있는 위치 판별 -> "6" 으로 표시
			// 아랫층부터 꼭데기층까지,
			for (int i = 7; i > 0; i--) {
				for (int j = 0; j <= i; j++) {
					// 제일 아랫칸은 놓을 수 있다.
					if (i == 7 && pyramid[i][j] == -1) {
						pyramid[i][j] = 6;
					} else if (pyramid[i][j] == -1) {
						// 아래 블럭 중 하나라도 현재 선택한 블럭과 같다면 놓을 수 있다. 보라색 블럭은 무조건 놓을 수 있다.
						if (pyramid[i + 1][j] == cuBlock[2] || pyramid[i + 1][j + 1] == cuBlock[2] || cuBlock[2] == 5) {
							// 아래 블럭중 하나라도 놓여있지 않다면 놓을 수 없다.
							if (pyramid[i + 1][j] != -1 && pyramid[i + 1][j + 1] != -1 && pyramid[i + 1][j] != 6
									&& pyramid[i + 1][j + 1] != 6) {
								pyramid[i][j] = 6;
							}
						}
					}
				}
			}

			String tempStr2 = Arrays.deepToString(pyramid);

			// 클릭한 블럭과 게임 배경 각각 전송
			Object obcm1 = new ChatMsg("SERVER", "301", tempStr1);
			Object obcm2 = new ChatMsg("SERVER", "302", tempStr2);

			try {
				oos.writeObject(obcm1);
				oos.writeObject(obcm2);
			} catch (IOException e) {
				e.printStackTrace();
			}

			return;
		}

		private void userChat(String msg, ChatMsg cm) {
			AppendText(msg); // server 화면에 출력
			String[] args = msg.split(" "); // 단어들을 분리한다.

			if (args.length == 1) { // Enter key 만 들어온 경우 Wakeup 처리만 한다.
				UserStatus = "O";
			} else if (args[1].matches("/exit")) {
				Logout();
				return;
			} else if (args[1].matches("/list")) {
				WriteOne("User list\n");
				WriteOne("Name\tStatus\n");
				WriteOne("-----------------------------\n");
				for (int i = 0; i < user_vc.size(); i++) {
					UserService user = (UserService) user_vc.elementAt(i);
					WriteOne(user.UserName + "\t" + user.UserStatus + "\n");
				}
				WriteOne("-----------------------------\n");
			} else if (args[1].matches("/sleep")) {
				UserStatus = "S";
			} else if (args[1].matches("/wakeup")) {
				UserStatus = "O";
			} else if (args[1].matches("/to")) { // 귓속말
				for (int i = 0; i < user_vc.size(); i++) {
					UserService user = (UserService) user_vc.elementAt(i);
					if (user.UserName.matches(args[2]) && user.UserStatus.matches("O")) {
						String msg2 = "";
						for (int j = 3; j < args.length; j++) {// 실제 message 부분
							msg2 += args[j];
							if (j < args.length - 1)
								msg2 += " ";
						}
						// /to 빼고.. [귓속말] [user1] Hello user2..
						user.WritePrivate(args[0] + " " + msg2 + "\n");
						break;
					}
				}
			} else { // 일반 채팅 메시지
				UserStatus = "O";
				WriteAllObject(cm);
			}
		}

		public void assignUserBlock() {
			// 3번째 유저가 입장하면 모든 유저에게 블럭 할당
			for (int i = 0; i < 3; i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (!user.isAssigned) {
					// 유저가 가진 블럭 목록 초기화
					for (int j = 0; j < 18; j++) {
						user.userBlock[j] = -1;
					}
					AppendText((i + 1) + "번째 블럭 초기화 확인" + Arrays.toString(user.userBlock));

					// 랜덤으로 블럭 할당
					for (int j = 0; j < 12; j++) {
						int num;
						do {
							num = random.nextInt(6);
						} while (sBlock[num] >= 7);
						user.userBlock[j] = num;
						sBlock[num]++;
					}
					AppendText((i + 1) + "번째 블럭 할당" + Arrays.toString(user.userBlock));

					// 할당된 블럭 정렬, 12번째까지만
					Arrays.sort(user.userBlock, 0, 12);
					AppendText((i + 1) + "번째 블럭 정렬" + Arrays.toString(user.userBlock));

					user.isAssigned = true;
				}

				// 할당된 블럭 전송
				try {
					ChatMsg obcm = new ChatMsg("SERVER", "371", Arrays.toString(user.userBlock));
					user.oos.writeObject(obcm);
				} catch (IOException e) {
					AppendText("dos.writeObject() error");
					try {
						user.ois.close();
						user.oos.close();
						user.client_socket.close();
						user.client_socket = null;
						user.ois = null;
						user.oos = null;
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					user.Logout(); // 에러가난 현재 객체를 벡터에서 지운다
				}
			}
		}

		// 유저 이미지 띄우기
		public void userImage() {
			// 클라이언트 userPanel을 배열화 하기 위해 size() - 1을 넣어줌
			Seq = Integer.toString(UserVec.size() - 1);
			try {
				ChatMsg obcm = new ChatMsg("Server", "120", Seq);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}

		// 새로운 유저 입장
		public void newUser() {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				// 클라이언트 userPanel을 배열화 하기 위해 size() - 1을 넣어줌
				Seq = Integer.toString(UserVec.size() - 1);
				try {
					ChatMsg obcm = new ChatMsg("Server", "150", Seq);
					user.WriteOneObject(obcm);
				} catch (Exception e) {
					AppendText("dos.writeObject() error");
					try {
						ois.close();
						oos.close();
						client_socket.close();
						client_socket = null;
						ois = null;
						oos = null;
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					Logout(); // 에러가난 현재 객체를 벡터에서 지운다
				}
			}
		}

		// 타이머 작동
		// int tCount = 11;

		public void timer() {
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				public void run() {
					if (tCount >= 1) {
						tCount--;
						String alarm = "제한시간 : " + tCount + "초";
						ChatMsg obcm = new ChatMsg("Server", "170", alarm);
						WriteAllObject(obcm);

					} else {
						tCount = 11;

						gameSeq++;
					}
				}
			};
			timer.schedule(task, 1000, 1000);
			AppendText("타이머가 작동됩니다.");
		}

		// 자기 차례

		public void turn() {
				UserService user = (UserService) UserVec.elementAt(gameSeq % 3);
				String myTurn = user.UserName + "님의 차례";
				ChatMsg obcm = new ChatMsg("Server", "180", myTurn);
				WriteAllObject(obcm);											
		}

		// case 로그인 후 "101"일때 UserImage
		public void Login() {

			AppendText("새로운 참가자 " + UserName + " 입장.");
			WriteOne("Welcome to Java chat server\n");
			WriteOne(UserName + "님 환영합니다.\n"); // 연결된 사용자에게 정상접속을 알림
			userImage();

			String msg = "[" + UserName + "]님이 입장 하였습니다.\n";
			WriteOthers(msg); // 아직 user_vc에 새로 입장한 user는 포함되지 않았다.
			newUser();
		}

		public void Logout() {
			String msg = "[" + UserName + "]님이 퇴장 하였습니다.\n";
			UserVec.removeElement(this); // Logout한 현재 객체를 벡터에서 지운다
			WriteAll(msg); // 나를 제외한 다른 User들에게 전송
			AppendText("사용자 " + "[" + UserName + "] 퇴장. 현재 참가자 수 " + UserVec.size());
		}

		/************************************************************
		 * Write 함수들
		 ***********************************************************/

		// 모든 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteAll(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.WriteOne(str);
			}
		}

		// 모든 User들에게 Object를 방송. 채팅 message와 image object를 보낼 수 있다
		public void WriteAllObject(Object ob) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.WriteOneObject(ob);
			}
		}

		// 나를 제외한 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteOthers(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user != this && user.UserStatus == "O")
					user.WriteOne(str);
			}
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
			}
			for (i = 0; i < bb.length; i++)
				packet[i] = bb[i];
			return packet;
		}

		// UserService Thread가 담당하는 Client 에게 1:1 전송
		public void WriteOne(String msg) {
			try {
				ChatMsg obcm = new ChatMsg("SERVER", "200", msg);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}

		// 귓속말 전송
		public void WritePrivate(String msg) {
			try {
				ChatMsg obcm = new ChatMsg("귓속말", "200", msg);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}

		public void WriteOneObject(Object ob) {
			try {
				oos.writeObject(ob);
			} catch (IOException e) {
				AppendText("oos.writeObject(ob) error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout();
			}
		}
	}
}
