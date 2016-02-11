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
	private String name_;

	public Database(String dbfile) throws SQLException {
		db_ = DriverManager.getConnection("jdbc:sqlite:" + dbfile);
		name_ = dbfile;
	}

	public String getName() {
		return name_;
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
}

