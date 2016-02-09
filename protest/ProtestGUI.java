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
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

//For message dialogs
import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class ProtestGUI implements Runnable{
	private Connection db_;

	private ArrayList<String> categoryNames_;
	private ArrayList<ArrayList<TestSuiteExample>> conflictExamplesByCategory_;
    private ArrayList<ArrayList<TestSuiteExample>> newExamplesByCategory_;
    private ArrayList<ArrayList<TestSuiteExample>> doneExamplesByCategory_;

	private InstanceWindow instWindow_;
    private JFrame frame_;
    

    public ProtestGUI(String dbfile) throws SQLException {
        db_ = DriverManager.getConnection("jdbc:sqlite:" + dbfile);

        refreshData();

        instWindow_ = new InstanceWindow();
    }


    public void refreshData() {
        categoryNames_ = new ArrayList<String>();
        conflictExamplesByCategory_ = new ArrayList<ArrayList<TestSuiteExample>>();
        newExamplesByCategory_ = new ArrayList<ArrayList<TestSuiteExample>>();
        doneExamplesByCategory_ = new ArrayList<ArrayList<TestSuiteExample>>();
        try {
            Statement stmt = db_.createStatement();
            ResultSet rs = stmt.executeQuery("select id as category_no, description from categories");
            while(rs.next()) {
                String name = rs.getString(2);
                categoryNames_.add(name);
                String conflictCondition = "(select distinct(a.example) from annotations as a left join pro_examples as p on p.id=a.example left join token_annotations as ta on ta.example=a.example left join translations as t on p.example_no=t.example_no and p.tgtcorpus=t.tgtcorpus and ta.token=t.tgtpos and ta.line=t.line where (t.ant_no IS NOT NULL and a.ant_annotation IS NOT 'ok') or (a.ant_annotation IS 'ok' and ta.annotation IS NULL) UNION select distinct(a.example) from annotations as a left join pro_examples as p on p.id=a.example left join token_annotations as ta on ta.example=a.example left join translations as t on p.example_no=t.example_no and p.tgtcorpus=t.tgtcorpus and ta.token=t.tgtpos and ta.line=t.line where (t.ant_no IS NULL and a.anaph_annotation IS NOT 'ok' and NOT (a.anaph_annotation IN ('','bad') and ta.annotation IS NULL)) or (a.anaph_annotation IS 'ok' and ta.annotation IS NULL))";
                PreparedStatement conflict_ps = db_.prepareStatement("select example_no, srccorpus, tgtcorpus from pro_examples where id in " + conflictCondition + " and category_no=?");
                PreparedStatement done_ps = db_.prepareStatement("select p.example_no as example_no, p.srccorpus as srccorpus, p.tgtcorpus as tgtcorpus from pro_examples as p left join annotations as a where p.id=a.example and p.id not in " + conflictCondition + " and p.category_no=?");
                PreparedStatement new_ps = db_.prepareStatement("select p.example_no as example_no, p.srccorpus as srccorpus, p.tgtcorpus as tgtcorpus from pro_examples as p where p.id not in (select distinct(example) from annotations) and p.category_no=?");
                conflict_ps.setInt(1, rs.getInt("category_no"));
                new_ps.setInt(1, rs.getInt("category_no"));
                done_ps.setInt(1, rs.getInt("category_no"));
                ResultSet conflictExrs = conflict_ps.executeQuery();
                ResultSet newExrs = new_ps.executeQuery();
                ResultSet doneExrs = done_ps.executeQuery();
                conflictExamplesByCategory_.add(addExamples(conflictExrs));
                newExamplesByCategory_.add(addExamples(newExrs));
                doneExamplesByCategory_.add(addExamples(doneExrs));
                }
            }
        catch(SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
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
   
    
    public void onButtonClick(List<TestSuiteExample> listInstances, String c, String msg) {
        if (listInstances.isEmpty()){
            JDialog.setDefaultLookAndFeelDecorated(true);
            JOptionPane.showMessageDialog(frame_, msg);
        }
        else {
            instWindow_.setData(c, listInstances);
            instWindow_.setVisible(true);
        }
    }

    
	public void run() {
		frame_ = new JFrame("PROTEST Browser");
		frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		((BorderLayout) frame_.getContentPane().getLayout()).setVgap(15);
        
        JLabel instructionLabel_ = new JLabel("<html><b>Select set of instances, for annotation / browsing:</b></html>");
        JPanel instructionPanel_ = new JPanel();
        instructionPanel_.setLayout(new FlowLayout(FlowLayout.LEFT));
        instructionPanel_.add(instructionLabel_, BorderLayout.LINE_START);
        frame_.getContentPane().add(instructionPanel_, BorderLayout.PAGE_START);
        
        JPanel catButtonsPanel_ = new JPanel(new GridLayout(14,1));
        frame_.getContentPane().add(new JScrollPane(catButtonsPanel_), BorderLayout.LINE_START);
        
        for(final String c : categoryNames_) {
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
                    refreshData();
                    List<TestSuiteExample> listInstances = newExamplesByCategory_.get(categoryNames_.indexOf(c));
                    onButtonClick(listInstances, c, "No unannotated examples for this category");
                }});
            doneButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    refreshData();
                    List<TestSuiteExample> listInstances = doneExamplesByCategory_.get(categoryNames_.indexOf(c));
                    onButtonClick(listInstances, c, "No annotated examples for this category");
                }});
            conflictsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    refreshData();
                    List<TestSuiteExample> listInstances = conflictExamplesByCategory_.get(categoryNames_.indexOf(c));
                    onButtonClick(listInstances, c, "No examples with conflicting annotations for this category");
                }});
            rowPanel_.add(catLabel);
            rowPanel_.add(newButton);
            rowPanel_.add(doneButton);
            rowPanel_.add(conflictsButton);
            catButtonsPanel_.add(rowPanel_);
        }

		JButton quitButton = new JButton("Quit");
		frame_.getContentPane().add(quitButton, BorderLayout.PAGE_END);
		quitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		frame_.pack();
		frame_.setVisible(true);
	}


	public static void main(String[] args) throws SQLException {
		if(args.length != 1) {
			System.err.println("Usage: ProtestGUI dbfile");
			System.exit(1);
		}

		SwingUtilities.invokeLater(new ProtestGUI(args[0]));
	}
}
