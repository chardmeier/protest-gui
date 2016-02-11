package protest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Database {
	private Connection db_;
	private String dbfile_;

	public Database(String dbfile) throws SQLException {
		db_ = DriverManager.getConnection("jdbc:sqlite:" + dbfile);
		dbfile_ = dbfile;
	}

	public String getName() {
		return dbfile_;
	}

	public List<AnnotationCategory> getCategories() {
		ArrayList<AnnotationCategory> catlist = new ArrayList<AnnotationCategory>();

		try {
			Statement stmt = db_.createStatement();
			ResultSet rs = stmt.executeQuery("select c.id as category_no, c.description as description, " +
						"a.conflict_status as conflict_status, p.example_no as example_no, " +
						"p.srccorpus as srccorpus, p.tgtcorpus as tgtcorpus " +
					"from categories as c left outer join pro_examples as p on c.id=p.category_no " +
						"left outer join annotations as a on p.id=a.example order by description, conflict_status");
			String lastcat = null;
			AnnotationCategory catobj = null;
			while(rs.next()) {
				String cat = rs.getString("description");
				String conflict = rs.getString("conflict_status");

				if(lastcat == null || !cat.equals(lastcat)) {
					lastcat = cat;
					catobj = new AnnotationCategory(cat);
					catlist.add(catobj);
				}

				int srccorpus = rs.getInt("srccorpus");
				int tgtcorpus = rs.getInt("tgtcorpus");
				int example_no = rs.getInt("example_no");

				if(!rs.wasNull()) {
					TestSuiteExample exmpl = new TestSuiteExample(db_, srccorpus, tgtcorpus, example_no);

					if(conflict == null)
						catobj.addExample(AnnotationCategory.NEW, exmpl);
					else if(conflict.equals("ana_ok ant_ok"))
						catobj.addExample(AnnotationCategory.DONE, exmpl);
					else
						catobj.addExample(AnnotationCategory.CONFLICT, exmpl);
				}
			}
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return catlist;
	}

	public HashMap<String,String> getMetadata() {
		HashMap<String,String> metadata = new HashMap<String,String>();

		try {
			Statement stmt = db_.createStatement();
			ResultSet rs = stmt.executeQuery("select tag, tag_value from meta_data");
			while(rs.next())
				metadata.put(rs.getString("tag"), rs.getString("tag_value"));
		} catch(SQLException e) {
			metadata = null;
		}

		return metadata;
	}

	public void createAnnotatorDB(String outfile, int annotator, int task) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + outfile);
		setupTables(outdb);

		conn.setAutoCommit(false);

		Statement stmt = conn.createStatement();
		stmt.execute("attach database \"" + dbfile_ + "\" as master");

		PreparedStatement ps;

		// annotation_tasks
		ps = conn.prepareStatement("insert into main.annotation_tasks " +
				"select * from master.annotation_tasks " +
				"where annotator_id=? and task_no=?");
		ps.setInt(1, annotator);
		ps.setInt(2, task);
		ps.execute();

		// annotations
		ps = conn.prepareStatement("insert into main.annotations " +
				"select a.* from master.annotations as a, master.annotation_tasks as t " +
				"where t.annotator_id=? and t.task_no=? and a.example=t.example");
		ps.setInt(1, annotator);
		ps.setInt(2, task);
		ps.execute();

		// annotators
		ps = conn.prepareStatement("insert into main.annotators " +
				"select * from master.annotators where id=?");
		ps.setInt(1, annotator);
		ps.execute();

		// categories
		stmt.execute("insert into main.categories select * from master.categories");

		// corpora
		ps = conn.prepareStatement("insert into main.corpora " +
				"select c.* from master.corpora as c, master.pro_examples as e, master.annotation_tasks as t " +
				"where (c.id=e.srccorpus or c.id=e.tgtcorpus) and e.example_no=t.example and " +
				"t.annotator_id=? and t.task_no=?");
		ps.setInt(1, annotator);
		ps.setInt(2, task);
		ps.execute();

		// documents
		stmt.execute("insert into main.documents " +
				"select d.* from master.documents as d, main.corpora as c "+
				"where d.corpus=c.id");
		
		// pro_antecedents
		ps = conn.prepareStatement("insert into main.pro_antecedents " +
				"select a.* from master.pro_antecedents as a, master.annotation_tasks as t " +
				"where t.annotator_id=? and t.task_no=? and a.example_no=t.example");
		ps.setInt(1, annotator);
		ps.setInt(2, task);
		ps.execute();

		// pro_examples
		ps = conn.prepareStatement("insert into main.pro_examples " +
				"select e.* from master.pro_examples as e, master.annotation_tasks as t " +
				"where t.annotator_id=? and t.task_no=? and e.example_no=t.example");
		ps.setInt(1, annotator);
		ps.setInt(2, task);
		ps.execute();

		// sentences
		stmt.execute("insert into main.sentences " +
				"select s.* from master.sentences as s, main.corpora as c "+
				"where s.corpus=c.id");

		// token_annotations
		ps = conn.prepareStatement("insert into main.token_annotations " +
				"select a.* from master.token_annotations as a, master.annotation_tasks as t " +
				"where t.annotator_id=? and t.task_no=? and a.example=t.example");
		ps.setInt(1, annotator);
		ps.setInt(2, task);
		ps.execute();

		// translations
		ps = conn.prepareStatement("insert into main.translations " +
				"select tr.* from master.translations, master.annotation_tasks as t " +
				"where t.annotator_id=? and t.task_no=? and tr.example_no=t.example");
		ps.setInt(1, annotator);
		ps.setInt(2, task);
		ps.execute();
	}
}

