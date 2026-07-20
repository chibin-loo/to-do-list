import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import java.awt.BorderLayout;
import java.util.List;

public class Window {
    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Failed to set look and feel: " + ex);
        }
        JFrame frame = new JFrame("My To-Do App");
        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        List<Event> events;
        try {
            events = Main.buildEventList();
        } catch (Exception e) {
            System.err.println("Failed to build event list: " + e);
            return;
        }

        DefaultListModel<String> model = new DefaultListModel<>();
        for (Event e : events) {
            // Build the text for each row, same style as your terminal app
            String when = e.time.isBlank() ? e.date : (e.date + " " + e.time);
            String mark = e.done ? " [done]" : "";
            model.addElement(when + "   " + e.name + mark);
        }
        JList<String> list = new JList<>(model);

        // Wrap it in a scroll pane so long lists can scroll
        JScrollPane scroll = new JScrollPane(list);

        // Put the scrollable list into the window
        frame.add(scroll);

        frame.setVisible(true);
    }
}