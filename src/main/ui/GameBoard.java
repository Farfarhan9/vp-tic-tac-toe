package main.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class GameBoard extends JPanel {
    private JButton[][] gridButtons;
    private int size;
    private ActionListener cellClickListener;

    public GameBoard(int size, ActionListener cellClickListener) {
        this.size = size;
        this.cellClickListener = cellClickListener;
        initializeBoard();
    }

    public void initializeBoard() {
        removeAll();
        setLayout(new GridLayout(size, size, 8, 8));
        setBackground(new Color(245, 245, 245));
        
        gridButtons = new JButton[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                JButton button = new JButton("");
                button.setFont(new Font("SansSerif", Font.BOLD, 28));
                button.setBackground(Color.WHITE);
                button.setForeground(new Color(40, 40, 40));
                button.setBorder(new LineBorder(new Color(210, 210, 210), 1));
                button.setFocusPainted(false);
                button.putClientProperty("row", r);
                button.putClientProperty("col", c);
                button.addActionListener(cellClickListener);
                gridButtons[r][c] = button;
                add(button);
            }
        }
        revalidate();
        repaint();
    }

    public void updateCell(int row, int col, String symbol) {
        if (row >= 0 && row < size && col >= 0 && col < size) {
            gridButtons[row][col].setText(symbol);
            if (symbol.equals("X")) {
                gridButtons[row][col].setForeground(new Color(50, 100, 250));
            } else if (symbol.equals("O")) {
                gridButtons[row][col].setForeground(new Color(240, 80, 80));
            }
        }
    }

    /**
     * ★ NEW: Clear a specific cell (used when restoring a session to remove old marks).
     */
    public void clearCell(int row, int col) {
        if (row >= 0 && row < size && col >= 0 && col < size) {
            gridButtons[row][col].setText("");
            gridButtons[row][col].setForeground(new Color(40, 40, 40));
            gridButtons[row][col].setBackground(Color.WHITE);
            gridButtons[row][col].setEnabled(true);
        }
    }

    public void highlightWinningCells(int[][] coordinates) {
        if (coordinates == null) return;
        Color tealBackground = new Color(0, 128, 128);
        for (int[] point : coordinates) {
            int r = point[0];
            int c = point[1];
            gridButtons[r][c].setBackground(tealBackground);
            gridButtons[r][c].setForeground(Color.WHITE);
        }
    }

    public void freezeBoard() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                gridButtons[r][c].setEnabled(false);
            }
        }
    }

    public void updateBoardSize(int newSize) {
        this.size = newSize;
        initializeBoard();
    }

    public int getBoardSize() {
        return size;
    }
}