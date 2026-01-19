
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// ==========================================
// 1. DATA MODEL (The Room Object)
// ==========================================
class Room {
    private String roomNo;
    private int capacity;
    private boolean hasAC;
    private boolean hasAttachedWashroom;

    public Room(String roomNo, int capacity, boolean hasAC, boolean hasAttachedWashroom) {
        this.roomNo = roomNo;
        this.capacity = capacity;
        this.hasAC = hasAC;
        this.hasAttachedWashroom = hasAttachedWashroom;
    }

    public String getRoomNo() { return roomNo; }
    public int getCapacity() { return capacity; }
    public boolean isHasAC() { return hasAC; }
    public boolean isHasAttachedWashroom() { return hasAttachedWashroom; }

    @Override
    public String toString() {
        return String.format("Room: %-5s | Capacity: %-2d | AC: %-3s | Washroom: %-3s", 
            roomNo, capacity, (hasAC ? "Yes" : "No"), (hasAttachedWashroom ? "Yes" : "No"));
    }
}

// ==========================================
// 2. CONTROLLER (Business Logic)
// ==========================================
class HostelManager {
    private List<Room> rooms;

    public HostelManager() {
        this.rooms = new ArrayList<>();
        // Pre-loading dummy data for immediate testing
        addRoom("101", 1, true, true);
        addRoom("102", 2, false, true);
        addRoom("103", 4, true, false);
        addRoom("104", 2, true, true);
        addRoom("201", 6, false, false);
    }

    public boolean addRoom(String roomNo, int capacity, boolean ac, boolean washroom) {
        // Validation: Check for duplicate room number
        for (Room r : rooms) {
            if (r.getRoomNo().equalsIgnoreCase(roomNo)) {
                return false; // Duplicate found
            }
        }
        rooms.add(new Room(roomNo, capacity, ac, washroom));
        return true;
    }

    public List<Room> getAllRooms() {
        return rooms;
    }

    public List<Room> searchRooms(int minCapacity, boolean requireAC, boolean requireWashroom) {
        // Java Streams to filter the list based on criteria
        return rooms.stream()
                .filter(r -> r.getCapacity() >= minCapacity)
                .filter(r -> !requireAC || r.isHasAC()) // If AC required, room must have AC
                .filter(r -> !requireWashroom || r.isHasAttachedWashroom()) // If Washroom required, room must have it
                .collect(Collectors.toList());
    }

    // ALGORITHM: Allocate Smallest Possible Room
    public Room allocateRoom(int students, boolean needsAC, boolean needsWashroom) {
        // 1. Find all candidates that meet criteria
        List<Room> candidates = searchRooms(students, needsAC, needsWashroom);

        if (candidates.isEmpty()) {
            return null; // No suitable room
        }

        // 2. Sort by Capacity (Ascending) to find the "smallest fit"
        candidates.sort(Comparator.comparingInt(Room::getCapacity));

        // 3. Return the first one (which is the smallest suitable room)
        return candidates.get(0);
    }
}

// ==========================================
// 3. VIEW (User Interface)
// ==========================================
public class SmartHostelApp extends JFrame {
    
    private HostelManager manager = new HostelManager();
    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JTextArea outputArea;

    public SmartHostelApp() {
        // Main Window Setup
        setTitle("Smart Hostel Room Allocation System");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen

        // Create Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

tabbedPane.addTab(" Add Room ", createAddRoomPanel());
        tabbedPane.addTab(" View All Rooms ", createViewRoomsPanel());
        tabbedPane.addTab(" Search & Allocate ", createSearchAllocatePanel());

        // Auto-refresh the list when clicking the "View All Rooms" tab
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) { // Index 1 is View Rooms
                refreshTable();
            }
        });

        add(tabbedPane);
    }

    // --- Tab 1: Add Room Form ---
    private JPanel createAddRoomPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // UI Components
        JTextField txtRoomNo = new JTextField(15);
        JTextField txtCapacity = new JTextField(15);
        JCheckBox chkAC = new JCheckBox("Air Conditioning (AC)");
        JCheckBox chkWashroom = new JCheckBox("Attached Washroom");
        JButton btnAdd = new JButton("Save Room");
        btnAdd.setBackground(new Color(70, 130, 180)); // Steel Blue
        btnAdd.setForeground(Color.WHITE);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Room Number:"), gbc);
        gbc.gridx = 1; panel.add(txtRoomNo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Capacity (Students):"), gbc);
        gbc.gridx = 1; panel.add(txtCapacity, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Facilities:"), gbc);
        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkPanel.add(chkAC);
        checkPanel.add(Box.createHorizontalStrut(15));
        checkPanel.add(chkWashroom);
        gbc.gridx = 1; panel.add(checkPanel, gbc);

        gbc.gridx = 1; gbc.gridy = 3; panel.add(btnAdd, gbc);

        // Logic
        btnAdd.addActionListener(e -> {
            try {
                String rNo = txtRoomNo.getText().trim();
                String capStr = txtCapacity.getText().trim();

                if (rNo.isEmpty() || capStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int cap = Integer.parseInt(capStr);
                if (cap <= 0) {
                    JOptionPane.showMessageDialog(this, "Capacity must be greater than 0.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                boolean success = manager.addRoom(rNo, cap, chkAC.isSelected(), chkWashroom.isSelected());
                
                if (success) {
                    JOptionPane.showMessageDialog(this, "Room " + rNo + " added successfully!");
                    // Reset fields
                    txtRoomNo.setText("");
                    txtCapacity.setText("");
                    chkAC.setSelected(false);
                    chkWashroom.setSelected(false);
                } else {
                    JOptionPane.showMessageDialog(this, "Room Number " + rNo + " already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Capacity must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    // --- Tab 2: View All Rooms (Table) ---
    private JPanel createViewRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"Room No", "Capacity", "AC Available", "Washroom Available"};
        
        // Disable cell editing

tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        roomTable = new JTable(tableModel);
        roomTable.setRowHeight(25);
        roomTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        
        JScrollPane scrollPane = new JScrollPane(roomTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void refreshTable() {
        tableModel.setRowCount(0); // Clear table
        for (Room r : manager.getAllRooms()) {
            tableModel.addRow(new Object[]{
                r.getRoomNo(),
                r.getCapacity(),
                r.isHasAC() ? "Yes" : "No",
                r.isHasAttachedWashroom() ? "Yes" : "No"
            });
        }
    }

    // --- Tab 3: Search & Allocate ---
    private JPanel createSearchAllocatePanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Control Panel
        JPanel controls = new JPanel(new GridLayout(4, 2, 8, 8));
        controls.setBorder(BorderFactory.createTitledBorder("Criteria"));

        JTextField txtStudents = new JTextField();
        JCheckBox chkAC = new JCheckBox("Require AC");
        JCheckBox chkWashroom = new JCheckBox("Require Attached Washroom");
        JButton btnSearch = new JButton("Search Available Rooms");
        JButton btnAllocate = new JButton("Allocate Best Room");

        // Styling Buttons
        btnAllocate.setBackground(new Color(60, 179, 113)); // Medium Sea Green
        btnAllocate.setForeground(Color.WHITE);

        controls.add(new JLabel("Number of Students:"));
        controls.add(txtStudents);
        controls.add(new JLabel("Facilities Required:"));
        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkPanel.add(chkAC);
        checkPanel.add(chkWashroom);
        controls.add(checkPanel);
        controls.add(btnSearch);
        controls.add(btnAllocate);

        // Output Log Area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Results & Allocation Status"));

        mainPanel.add(controls, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // SEARCH BUTTON LOGIC
        btnSearch.addActionListener(e -> {
            outputArea.setText(""); // Clear previous
            try {
                int cap = Integer.parseInt(txtStudents.getText().trim());
                List<Room> results = manager.searchRooms(cap, chkAC.isSelected(), chkWashroom.isSelected());
                
                outputArea.append("--- SEARCH RESULTS ---\n");
                outputArea.append("Criteria: Min Capacity " + cap + ", AC: " + chkAC.isSelected() + ", Washroom: " + chkWashroom.isSelected() + "\n\n");
                
                if (results.isEmpty()) {
                    outputArea.append("No rooms found matching these criteria.");
                } else {
                    for (Room r : results) {
                        outputArea.append(r.toString() + "\n");
                    }
                    outputArea.append("\nTotal found: " + results.size());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for students.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
// ALLOCATE BUTTON LOGIC
        btnAllocate.addActionListener(e -> {
            outputArea.setText(""); // Clear previous
            try {
                int cap = Integer.parseInt(txtStudents.getText().trim());
                Room allocated = manager.allocateRoom(cap, chkAC.isSelected(), chkWashroom.isSelected());
                
                outputArea.append("--- ALLOCATION RESULT ---\n");
                if (allocated != null) {
                    outputArea.append("SUCCESS: Room Allocated!\n\n");
                    outputArea.append("Selected Room Details:\n");
                    outputArea.append(allocated.toString());
                    outputArea.append("\n\nAlgorithm Note: This is the smallest room capacity that meets your requirements.");
                } else {
                    outputArea.append("FAILURE: No suitable room available.\n");
                    outputArea.append("Try reducing requirements or adding new rooms.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for students.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return mainPanel;
    }

    public static void main(String[] args) {
        // Run UI in the Event Dispatch Thread (Best Practice)
        SwingUtilities.invokeLater(() -> {
            try {
                // Make it look like the native OS (Windows/Mac/Linux style)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new SmartHostelApp().setVisible(true);
        });
    }
}