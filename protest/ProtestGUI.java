package protest;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

	private JList categoryList_;
	private InstanceWindow instWindow_;

	public ProtestGUI(String dbfile) throws SQLException {
		db_ = DriverManager.getConnection("jdbc:sqlite:" + dbfile);

		categoryNames_ = new ArrayList<String>();
		examplesByCategory_ = new ArrayList<ArrayList<TestSuiteExample>>();
		Statement stmt = db_.createStatement();
		ResultSet rs = stmt.executeQuery("select distinct s.name, t.name, categories.description, " +
				"      srccorpus, tgtcorpus, category_no " +
				"from pro_examples, categories, corpora as s, corpora as t " +
				"where pro_examples.category_no=categories.id and " +
				"      srccorpus=s.id and tgtcorpus=t.id " +
				"order by s.name, t.name, categories.description");
		while(rs.next()) {
			String name = rs.getString(1) + " - " + rs.getString(2) + " - " + rs.getString(3);
			categoryNames_.add(name);
			PreparedStatement ps = db_.prepareStatement("select distinct example_no from pro_examples " +
					"where srccorpus=? and tgtcorpus=? and category_no=?");
			int srccorpus = rs.getInt("srccorpus");
			int tgtcorpus = rs.getInt("tgtcorpus");
			ps.setInt(1, srccorpus);
			ps.setInt(2, tgtcorpus);
			ps.setInt(3, rs.getInt("category_no"));
			ResultSet exrs = ps.executeQuery();
			ArrayList<TestSuiteExample> exs = new ArrayList<TestSuiteExample>();
			while(exrs.next())
				exs.add(new TestSuiteExample(db_, srccorpus, tgtcorpus, exrs.getInt(1)));
			examplesByCategory_.add(exs);
		}

		instWindow_ = new InstanceWindow();
	}

	public void run() {
		JFrame frame = new JFrame("PROTEST Browser");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		((BorderLayout) frame.getContentPane().getLayout()).setVgap(15);

		categoryList_ = new JList(categoryNames_.toArray(new String[0]));
		categoryList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		categoryList_.addListSelectionListener(this);
		frame.getContentPane().add(new JScrollPane(categoryList_), BorderLayout.CENTER);

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
