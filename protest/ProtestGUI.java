package protest;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Color;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.SwingUtilities;

public class ProtestGUI implements Runnable, ListSelectionListener {
	private Connection db_;

	private ArrayList<String> categoryNames_;
	private ArrayList<ArrayList<TestSuiteExample>> examplesByCategory_;
    private ArrayList<ArrayList<TestSuiteExample>> newExamplesByCategory_;
    private ArrayList<ArrayList<TestSuiteExample>> doneExamplesByCategory_;

	private JList categoryList_;
	private InstanceWindow instWindow_;

	public ProtestGUI(String dbfile) throws SQLException {
		db_ = DriverManager.getConnection("jdbc:sqlite:" + dbfile);

		categoryNames_ = new ArrayList<String>();
		examplesByCategory_ = new ArrayList<ArrayList<TestSuiteExample>>();
        newExamplesByCategory_ = new ArrayList<ArrayList<TestSuiteExample>>();
        doneExamplesByCategory_ = new ArrayList<ArrayList<TestSuiteExample>>();
		Statement stmt = db_.createStatement();
        ResultSet rs = stmt.executeQuery("select id as category_no, description from categories");
        while(rs.next()) {
            String name = rs.getString(2);
			categoryNames_.add(name);
            PreparedStatement ps = db_.prepareStatement("select example_no, srccorpus, tgtcorpus from pro_examples where category_no=?");
            PreparedStatement done_ps = db_.prepareStatement("select p.example_no as example_no, p.srccorpus as srccorpus, p.tgtcorpus as tgtcorpus from pro_examples as p left join annotations as a where p.id=a.example and p.category_no=?");
            PreparedStatement new_ps = db_.prepareStatement("select p.example_no as example_no, p.srccorpus as srccorpus, p.tgtcorpus as tgtcorpus from pro_examples as p where p.id not in (select distinct(example) from annotations) and p.category_no=?");
            ps.setInt(1, rs.getInt("category_no"));
            new_ps.setInt(1, rs.getInt("category_no"));
            done_ps.setInt(1, rs.getInt("category_no"));
			ResultSet exrs = ps.executeQuery();
            ResultSet newExrs = new_ps.executeQuery();
            ResultSet doneExrs = done_ps.executeQuery();
            examplesByCategory_.add(addExamples(exrs));
            newExamplesByCategory_.add(addExamples(newExrs));
            doneExamplesByCategory_.add(addExamples(doneExrs));
		}

		instWindow_ = new InstanceWindow();
	}
    
    public ArrayList<TestSuiteExample> addExamples(ResultSet rs) throws SQLException {
        ArrayList<TestSuiteExample> result = new ArrayList<TestSuiteExample>();
        while(rs.next()) {
            int srccorpus = rs.getInt("srccorpus");
            int tgtcorpus = rs.getInt("tgtcorpus");
            result.add(new TestSuiteExample(db_, srccorpus, tgtcorpus, rs.getInt(1)));
        }
        return result;
    }

	public void run() {
		JFrame frame = new JFrame("PROTEST Browser");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		((BorderLayout) frame.getContentPane().getLayout()).setVgap(15);

		categoryList_ = new JList(categoryNames_.toArray(new String[0]));
		categoryList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		categoryList_.addListSelectionListener(this);
		frame.getContentPane().add(new JScrollPane(categoryList_), BorderLayout.PAGE_START);
        
        JPanel catButtonsPanel_ = new JPanel(new GridLayout(14,1));
        frame.getContentPane().add(new JScrollPane(catButtonsPanel_), BorderLayout.LINE_START);
        
        for(String c : categoryNames_) {
            JPanel rowPanel_ = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel catLabel = new JLabel(c);
            JButton newButton = new JButton("New");
            newButton.setBackground(Color.YELLOW);
            newButton.setOpaque(true);
            JButton doneButton = new JButton("Done");
            doneButton.setBackground(Color.GREEN);
            doneButton.setOpaque(true);
            JButton conflictsButton = new JButton("Conflicts");
            conflictsButton.setBackground(Color.RED);
            conflictsButton.setOpaque(true);
            newButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    instWindow_.setData(c, newExamplesByCategory_.get(categoryNames_.indexOf(c)));
                    instWindow_.setVisible(true);
                }});
            doneButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    instWindow_.setData(c, doneExamplesByCategory_.get(categoryNames_.indexOf(c)));
                    instWindow_.setVisible(true);
                }});
            conflictsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    instWindow_.setData(c, examplesByCategory_.get(categoryNames_.indexOf(c)));
                    instWindow_.setVisible(true);
                }});
            rowPanel_.add(catLabel);
            rowPanel_.add(newButton);
            rowPanel_.add(doneButton);
            rowPanel_.add(conflictsButton);
            catButtonsPanel_.add(rowPanel_);
        }

		JButton quitButton = new JButton("Quit");
		frame.getContentPane().add(quitButton, BorderLayout.PAGE_END);
		quitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		frame.pack();
		frame.setVisible(true);
	}

	public void valueChanged(ListSelectionEvent e) {
		if(e.getValueIsAdjusting())
			return;
		int idx = categoryList_.getMinSelectionIndex();
		instWindow_.setData(categoryNames_.get(idx), examplesByCategory_.get(idx));
		instWindow_.setVisible(true);
	}

	public static void main(String[] args) throws SQLException {
		if(args.length != 1) {
			System.err.println("Usage: ProtestGUI dbfile");
			System.exit(1);
		}

		SwingUtilities.invokeLater(new ProtestGUI(args[0]));
	}
}
