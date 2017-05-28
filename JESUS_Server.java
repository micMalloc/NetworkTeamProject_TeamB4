package NTP;

import java.sql.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class JESUS_Server {

	private static Connection conn = null;
	/* PORT number that the JESUS Server listen on */
	private static final int PORT = 1919;
	/* Current Login ID list */
	private static HashSet<String> IDs = new HashSet<String>();

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		ServerSocket listener = new ServerSocket(PORT);
		try {
			while (true) {
				new Handler(listener.accept()).start();
			}
		} finally {
			listener.close();
		}
	}

	private static class Handler extends Thread {

		private String id;
		private Socket socket;
		private BufferedReader in;
		private PrintWriter out;
		private Queue<Integer> ansList = new LinkedList<Integer>();
		public int correct, incorrect, ans_cnt;

		public Handler(Socket socket) {
			this.socket = socket;
		}

		/*
		 * Description : Listen Client's Request Message and Handle it by Using
		 * The Proper Static Method e.g. Response Message Input : None Output :
		 * None
		 */
		public void run() {
			try {

				Class.forName("com.mysql.jdbc.Driver");// ����̹� �ε�:
				// DriverManager�� ���
				String jdbcUrl = "jdbc:mysql://localhost/jesus";// ����ϴ� �����ͺ��̽�����
				// ������ url
				String userId = "root";// ����ڰ���
				String userPass = "1234";// ����� �н�����

				conn = DriverManager.getConnection(jdbcUrl, userId, userPass);// Connection
				// ��ü��
				// ��
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);

				while (true) {
					String inMsg = in.readLine();
					/* Log-In */
					if (inMsg.startsWith("LOGIN")) {
						int state;
						String[] split = inMsg.split(" ");
						// state means latest question that was requested by
						// Current Client
						state = loginCheck(split[1], split[2]);
						if (state >= -1)
							id = split[1];

						out.println("" + state);
					} /* Sign Up */
					else if (inMsg.startsWith("SIGNUP")) {
						String[] split = inMsg.split(" ");

						if (signUpHandler(split[1], split[2])) {
							out.println("SUCCESS");
						} else {
							out.println("ALREADY");
						}
					} /* Give Questions Given Level */
					else if (inMsg.startsWith("LEVELCHOICE")) {
						String[] split = inMsg.split(" ");
						ArrayList<String> rs = getChoiceLevel(Integer.parseInt(split[1]));

						for (String e : rs) {
							out.println(e);
						}
						out.println("END");
					} /* Get a Request Question */
					else if (inMsg.startsWith("QCHOICE")) {
						String[] split = inMsg.split(" ");
						String answer = null;
						int Q_NO = Integer.parseInt(split[1]);
						ArrayList<String> rs = getChoiceQuestion(Q_NO);
						ArrayList<Integer> already = new ArrayList<Integer>();

						/* Random Print Output (��û ������ instruction) */
						int idx, count = 0;
						while (count < rs.size()) {
							idx = (int) (Math.random() * rs.size());
							if (already.contains(idx)) {
								continue;
							} else {
								out.println(rs.get(idx));
								already.add(idx);
								count++;
							}
						}
						out.println("END");

						/* Setting for Feedback */
						/*
						 * Get Current Request Question's Answer from Database
						 * for Feedback
						 */
						answer = getAnswer(Integer.parseInt(split[1]));
						split = answer.split(" ");
						for (int i = 0; i < split.length; ++i) {
							System.out.println(Integer.parseInt(split[i]) + "���� Ȯ��");
							ansList.add(Integer.parseInt(split[i]));
						}
						/* Set Time Interval Variable */
						ans_cnt = ansList.size();
						correct = incorrect = 0;
						/*
						 * Save Current Client's Latest Request Question for
						 * Next Log-in
						 */
						saveState(id, Q_NO);
					} /* Check Submitted Answer */
					else if (inMsg.startsWith("CHECKANS")) {
						String iMsg = inMsg.substring(inMsg.indexOf(" ") + 1);
						String qNO, submitList;
						
						qNO = iMsg.substring(0, iMsg.indexOf(" "));
						submitList = iMsg.substring(iMsg.indexOf(" ") + 1);
						System.out.println(qNO + " " + submitList);
						if (checkSubmitAnswer(Integer.parseInt(qNO), submitList, id))
							out.println("CORRECT");
						else
							out.println("INCORRECT");
					} /* ������ ��� */
					else if (inMsg.startsWith("STATS")) {
						ArrayList<String> resultSet = null;
						resultSet = dataStatistics();
					} /* Real-Time Feedback */
					else if (inMsg.startsWith("FEEDBACK")) {
						String[] split = inMsg.split(" ");
						String feedback = null;

						int input = Integer.parseInt(split[1]);

						if (input == ansList.peek()) {
							ansList.remove();
							/* Send Ī �� �޽��� */
							//out.println("PRAISE " + "���Ҿ�!");
							correct++;
							System.out.println(correct);
							if (correct == 3) {
								out.println("PRAISE " + "���� �� �ϰ� �־�!");
							} else if (correct == 5) {
								out.println("PRAISE " + "�峭 �ƴѵ�?");
							} else if (correct == ans_cnt - 1) {
								out.println("PRAISE " + "���� ���� �� �Ծ�!");
							} else {
								out.println("PRAISE " + "���Ҿ�!");
							}
						} else {
							/* Get and Send Feedback Message */
							feedback = getFeedbackMSG(ansList.peek());
							out.println("FEEDBACK " + feedback);
							incorrect++;
							System.out.println(incorrect);
							if (incorrect == 3) {
								out.println("FEEDBACK " + "������ �����ϰ�!");
							} else if (incorrect == 5) {
								out.println("FEEDBACK " + "���! ���ݱ��� ������ �͵��� �ٽ� �ѹ� ����?");
							} else if (incorrect == ans_cnt - 1) {
								out.println("FEEDBACK " + "�̷��� �ؼ� ������ ���� �� ����!");
							}
						}
						out.println("END");
					}
				}
			} catch (ClassNotFoundException e) {
				System.err.print("ClassNotFoundException: ");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (id != null) {
					/* Log-Out for Current ID client */
					IDs.remove(id);
				}
				try {
					socket.close();
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * Description : Check If Current Given ID and Password can log-in or not
	 * Input : ID and Password Output : If can log-in return Client's State If
	 * can not return error integers
	 */
	public static int loginCheck(String id, String password) throws Exception {
		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn.createStatement();// Statement ��ü�� ��
			rs = stmt.executeQuery("select PASSWORD, STATE from client where ID = '" + id + "'");
			if (rs.next()) {
				/* Check if current input id is log-in or not */
				int state = rs.getInt("STATE");
				if (password.equals(rs.getString("PASSWORD"))) {
					synchronized (id) {
						if (!IDs.contains(id)) {
							IDs.add(id);
						} else
							return -4;// Current Input ID Already log-in
					}

					return state;// Login Success Return Client's State
				}
				return -3;// Wrong Password Input
			} else {
				return -2;// Wrong ID input
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}

		return 0;
	}

	/*
	 * Description : Handle Sign-Up Request and Update Client Database Input :
	 * Given ID and Password Output : If The Database is updated, return TRUE If
	 * not, return False
	 */
	public static boolean signUpHandler(String id, String password) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn.createStatement();// Statement ��ü�� ��
			rs = stmt.executeQuery("select ID from client where ID = '" + id + "'");
			if (rs.next()) {
				return false;
			} else {
				stmt.executeUpdate("insert into CLIENT values ('" + id + "', '" + password + "', -1)");
				initClientData(id);
				return true;
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}

		return false;
	}
	
	/*
	 * Description : Initialize Client's Data
	 * Input : Current ID
	 * Output : None
	 */
	public static void initClientData (String id) throws SQLException {
		Statement stmt = null;
		
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("insert into client_management values('" + id + "', 0, 0, 0)");
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}
	}

	/*
	 * Description : Save Client Latest State Input : Client's ID and Latest
	 * Requested Question's Number Output : None
	 */
	public static void saveState(String id, int qNo) throws SQLException {
		Statement stmt = null;

		try {

			stmt = conn.createStatement();// Statement ��ü�� ��
			stmt.executeUpdate("update client set state = " + qNo + " where id = '" + id + "'");

		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}

	}

	/*
	 * Description : Get Question's lists that Selected Level Input : Selected
	 * Level Output : String Array List
	 */
	public static ArrayList<String> getChoiceLevel(int level) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;

		try {

			ArrayList<String> result = new ArrayList<String>();

			stmt = conn.createStatement();// Statement ��ü�� ��
			rs = stmt.executeQuery("select Q_NO, TITLE from question where LEVEL = " + level);

			while (rs.next()) {
				result.add(rs.getInt("Q_NO") + " " + rs.getString("TITLE"));
			}
			return result;

		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}

		return null;
	}

	/*
	 * Description : Get Instructions of Requested Question Input : Requested
	 * Question Number Output : String Array List of Instructions
	 */
	public static ArrayList<String> getChoiceQuestion(int qNo) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;

		try {
			ArrayList<String> result = new ArrayList<String>();
			stmt = conn.createStatement();// Statement ��ü�� ��
			rs = stmt.executeQuery("select INS_NO, INS_CONTENT from INSTRUCTION where Q_NO = " + qNo);
			while (rs.next()) {
				result.add(rs.getInt("INS_NO") + " " + rs.getString("INS_CONTENT"));
			}
			return result;

		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}
		return null;
	}

	/*
	 * Description : Check if Current Given AnswerList is Correct or Not Input :
	 * String Submitted Answer List Output : If correct, Return TRUE If
	 * incorrect, Return FALSE
	 */
	public static boolean checkSubmitAnswer(int q_no, String submitAns, String id) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn.createStatement();// Statement ��ü�� ��
			stmt.executeUpdate("update client_management set submit_count = submit_count + 1 where id = '" + id + "'");
			
			rs = stmt.executeQuery("select ANSWER from question where Q_NO = " + q_no);
			if (rs.next()) {
				if (submitAns.equals(rs.getString("ANSWER"))) {
					stmt.executeUpdate("update CLIENT_MANAGEMENT set problem_count = num_of_problems + 1 where ID = '"  + id + "'");
					stmt.executeUpdate("update CLIENT_MANAGEMENT set hit_count = hit_count + 1 where ID = '"  + id + "'");
					return true;
				}
				else
					return false;
			}

		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}
		
		return false;
	}

	/*
	 * Description : Get Answer for Feedback Input : Requested Question Number
	 * Output : String Answer
	 */
	public static String getAnswer(int qNo) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		String anwser = null;

		try {
			stmt = conn.createStatement();// Statement ��ü�� ��
			rs = stmt.executeQuery("select ANSWER from QUESTION where Q_NO = " + qNo);

			while (rs.next()) {
				anwser = rs.getString("ANSWER");
			}
			return (anwser);
			
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}
		return null;
	}

	/*
	 * Description : Get the Proper Feedback Message Input : Instruction Number
	 * Output : String Feedback Message
	 */
	public static String getFeedbackMSG(int insNO) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		String feedback = null;

		try {
			stmt = conn.createStatement();// Statement ��ü�� ��
			rs = stmt.executeQuery("select CONTENT from FEEDBACK where INS_NO = " + insNO);

			while (rs.next()) {
				feedback = rs.getString("CONTENT");
			}
			return (feedback);

		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}

		return null;
	}
	/* ��� �ڷ� ��� */
	public static ArrayList<String> dataStatistics () throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			ArrayList<String> result = new ArrayList<String> ();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select ID, PROBLEM_COUNT from client_management order by problem_count desc");
			while (rs.next()) {
				result.add(rs.getString("ID") + " " + rs.getString("PROBLEM_COUNT"));
			} return result;
			
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		} finally {
			stmt.close();
		}
		return null;
	}
}