package ticTacToe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class CMainForm extends JFrame implements Runnable {
    private JPanel mainPanel;
    private JPanel jPanel1;
    private JButton restartButton;
    private JButton STARTButton;
    private JTextField ipTextField;
    private JGamePanel gamePanel;
    ServerSocket serverSocket;
    Socket socket;
    DataOutputStream dos;
    Thread thread;
    DataInputStream dis;

    private void createUIComponents() {
        jPanel1 = new JGamePanel();
        gamePanel = (JGamePanel) jPanel1;
        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                mousePressedEvent(e);
            }
        });
    }

    void buttonStartClick() {
        CConfig.ip = ipTextField.getText().trim();
        if (!connectToServer()) initializeServer();
        thread = new Thread(this, "TicTacToe");
        CConfig.threadRunning = true;
        thread.start();
        STARTButton.setEnabled(false);
        if (CConfig.circle) setTitle("TicTacToe - O");
        else setTitle("TicTacToe - X");
    }

    ;

    void buttonRestartClick() {
        CConfig.reset();
        Toolkit.getDefaultToolkit().sync();
        try {
            dos.writeInt(999);
            dos.flush();
        } catch (IOException e) {
            CConfig.errors++;
        }
        CConfig.yourTurn = false;
        gamePanel.repaint();
    }

    ;

    void mousePressedEvent(java.awt.event.MouseEvent e) {
        if (CConfig.accepted) {
            if (CConfig.yourTurn) {
                if (!CConfig.comError && !CConfig.won && !CConfig.enemyWon) {
                    int x = 3 * e.getX() / CConfig.WIDTH;
                    int y = 3 * e.getY() / CConfig.HEIGHT;
                    int position = y * 3 + x;
                    if (CConfig.board[position] == null) {
                        if (!CConfig.circle) CConfig.board[position] = "X";
                        else CConfig.board[position] = "O";
                        CConfig.yourTurn = false;
                        Toolkit.getDefaultToolkit().sync();
                        try {
                            dos.writeInt(position);
                            dos.flush();
                        } catch (IOException ex) {
                            CConfig.errors++;
                        }
                        if (checkWin(true) || checkTie()) restartButton.setEnabled(true);
                        gamePanel.repaint();
                    }
                }
            }
        }
    }

    ;

    public CMainForm(String title) throws HeadlessException {
        super(title);
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        restartButton.setEnabled(false);
        STARTButton.addActionListener(actionEvent -> buttonStartClick());
        restartButton.addActionListener(actionEvent -> buttonRestartClick());
    }

    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(CConfig.port, 8, InetAddress.getByName(CConfig.ip));
        } catch (Exception e) {
            e.printStackTrace();
        }
        CConfig.yourTurn = true;
        CConfig.circle = false;
    }

    private void listenForServerRequest() {
        try {
            socket = serverSocket.accept();
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            CConfig.accepted = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean connectToServer() {
        try {
            socket = new Socket(CConfig.ip, CConfig.port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            CConfig.accepted = true;
            gamePanel.repaint();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean checkTie() {
        for (String s : CConfig.board) if (s == null) return false;
        CConfig.tie = true;
        return true;
    }

    public boolean checkWin(boolean myWin) {
        String str;
        if (myWin) {
            if (CConfig.circle) str = "O";
            else str = "X";
        } else {
            if (CConfig.circle) str = "X";
            else str = "O";
        }
        for (int[] win : CConfig.wins) {
            if (CConfig.board[win[0]] == null || CConfig.board[win[1]] == null || CConfig.board[win[2]] == null)
                continue;
            if (CConfig.board[win[0]].equals(str) && CConfig.board[win[1]].equals(str) && CConfig.board[win[2]].equals(str)) {
                CConfig.line[0] = win[0];
                CConfig.line[1] = win[2];
                if (myWin) CConfig.won = true;
                else CConfig.enemyWon = true;
                return true;
            }
        }
        return false;
    }

    public void run() {
        while (CConfig.threadRunning) {
            if (CConfig.errors >= 10) CConfig.comError = true;
            if (!CConfig.yourTurn && !CConfig.comError) {
                try {
                    int space = dis.readInt();
                    if ((space > -1) && (space < 9)) {
                        if (CConfig.circle) CConfig.board[space] = "X";
                        else CConfig.board[space] = "O";
                        CConfig.yourTurn = true;
                        if (checkWin(false) || checkTie()) {
                            restartButton.setEnabled(true);
                            CConfig.yourTurn = false;
                        }
                    } else if (space == 999) {
                        CConfig.reset();
                        CConfig.yourTurn = true;
                        gamePanel.repaint();
                    }
                } catch (IOException e) {
                    CConfig.errors++;
                }
            }
            gamePanel.repaint();
            if (!CConfig.circle && !CConfig.accepted) listenForServerRequest();
        }
    }
}

