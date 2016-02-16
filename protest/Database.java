package protest;

import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Collections;
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
		return doGetCategories("");
	}

	public List<AnnotationCategory> getCategoriesForCorpora(int[] tgtcorpora) {
		if(tgtcorpora.length == 0)
			return Collections.<AnnotationCategory>emptyList();

		return doGetCategories("where tgtcorpus in " + makeInList(tgtcorpora));
	}

	private List<AnnotationCategory> doGetCategories(String whereClause) {
		ArrayList<AnnotationCategory> catlist = new ArrayList<AnnotationCategory>();

		try {
			Statement stmt = db_.createStatement();
			ResultSet rs = stmt.executeQuery("select c.id as category_no, c.description as description, " +
						"a.conflict_status as conflict_status, p.example_no as example_no, " +
						"p.srccorpus as srccorpus, p.tgtcorpus as tgtcorpus " +
					"from categories as c left outer join pro_examples as p on c.id=p.category_no " +
						"left outer join annotations as a on p.id=a.example " +
					whereClause + " " +
					"order by description, conflict_status");
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

	public List<TargetCorpus> getTargetCorpora() {
		ArrayList<TargetCorpus> crplist = new ArrayList<TargetCorpus>();

		try {
			Statement stmt = db_.createStatement();
			ResultSet rs = stmt.executeQuery("select corpora.name as name, count(*) as cnt from corpora, pro_examples " +
				       "where corpora.id=pro_examples.tgtcorpus group by name order by name");
			while(rs.next())
				crplist.add(new TargetCorpus(rs.getString("name"), rs.getInt("cnt")));
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return crplist;
	}

	public int getFilteredExampleCount(int[] tgtcorpora, int[] categories) {
		if(tgtcorpora.length == 0 || categories.length == 0)
			return 0;

		int cnt = 0;
		try {
			Statement stmt = db_.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from pro_examples " +
					"where tgtcorpus in " + makeInList(tgtcorpora) + " " +
					"and category_no in " + makeInList(categories));
			rs.next();
			cnt = rs.getInt(1);
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return cnt;
	}

	public HashMap<String,String> getMetadata() {
		return getMetadata(db_);
	}

	private HashMap<String,String> getMetadata(Connection conn) {
		HashMap<String,String> metadata = new HashMap<String,String>();

		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select tag, tag_value from meta_data");
			while(rs.next())
				metadata.put(rs.getString("tag"), rs.getString("tag_value"));
		} catch(SQLException e) {
			metadata = null;
		}

		return metadata;
	}

	public boolean tasksetExists(String label) {
		try {
			PreparedStatement ps = db_.prepareStatement("select count(*) from task_definition where taskset=?");
			ps.setString(1, label);
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1) > 0;
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return false; // to make compiler happy
	}

	public void createAnnotationTasks(String taskset, int[] tgtcorpora, int[] categories, int ntasks, int iaa) {
		try {
			db_.setAutoCommit(false);

			Statement stmt = db_.createStatement();

			PreparedStatement ps_descr = db_.prepareStatement(
					"insert into task_definition (taskset, label) values (?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps_descr.setString(1, taskset);

			int iaa_id = -1;
			if(iaa > 0) {
				ps_descr.setString(2, "IAA");
				ps_descr.execute();
				ResultSet rs = ps_descr.getGeneratedKeys();
				rs.next();
				iaa_id = rs.getInt(1);
			}

			int[] task_ids = new int[ntasks];
			for(int i = 0; i < ntasks; i++) {
				ps_descr.setString(2, Integer.toString(i));
				ps_descr.execute();
				ResultSet rs = ps_descr.getGeneratedKeys();
				rs.next();
				task_ids[i] = rs.getInt(1);
			}

			ResultSet rs = stmt.executeQuery("select tgtcorpus, category_no, count(*) as cnt from pro_examples" +
					"where tgtcorpus in " + makeInList(tgtcorpora) + " and category_no in " + makeInList(categories) + " " +
					"group by tgtcorpus, category_no order by tgtcorpus, category_no");
			HashMap<int[],Integer> proportions = new HashMap<int[],Integer>();
			int totalcnt = 0;
			while(rs.next()) {
				int[] key = { rs.getInt("tgtcorpus"), rs.getInt("category_no") };
				int cnt = rs.getInt("cnt");
				proportions.put(key, Integer.valueOf(rs.getInt(cnt)));
				totalcnt += cnt;
			}

			PreparedStatement ps = db_.prepareStatement("insert into annotation_tasks (task_no, example) " +
					"select ?, id from pro_examples where tgtcorpus=? and category_no=? " + 
					"order by random() limit ?");
			for(int corpus : tgtcorpora) {
				for(int cat : categories) {
					ps.setInt(2, corpus);
					ps.setInt(3, cat);

					int[] key = { corpus, cat };
					int cnt = proportions.get(key).intValue();

					if(iaa > 0) {
						double prop = ((double) cnt) / ((double) totalcnt);
						int nx = (int) Math.ceil(prop * iaa);
						ps.setInt(1, iaa_id);
						ps.setInt(3, nx);
						ps.execute();
						cnt -= nx;
					}

					if(ntasks > 0 && cnt > 0) {
						int nx = (int) Math.ceil(((double) cnt) / ((double) ntasks));
						ps.setInt(3, nx);
						for(int i = 0; i < ntasks; i++) {
							ps.setInt(1, Integer.valueOf(task_ids[i]));
							ps.execute();
						}
					}
				}
			}

			db_.commit();
		} catch(SQLException e) {
			try {
				db_.rollback();
			} catch(SQLException e2) {}
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void createAnnotationBatch(String outfile, int annotator, int task) throws SQLException {
		new File(outfile).delete();

		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + outfile);

		conn.setAutoCommit(false);

		try {
			// copy DB schema -- totally specific to sqlite, using internal data structures!
			// The order by clause makes sure all tables are created before we start adding indices.
			// We fetch the schema from the main DB connection and execute the create statements on a
			// connection that only has the new DB attached so we don't have to manipulate them
			// to add DB identifiers.
			Statement stmt = conn.createStatement();
			Statement maindb_stmt = db_.createStatement();
			ResultSet rs = maindb_stmt.executeQuery("select sql from sqlite_master " +
					"where tbl_name not like 'sqlite_%' order by type desc");
			while(rs.next())
				stmt.execute(rs.getString(1));

			// must turn off transactions now because ATTACH doesn't work within a transaction
			conn.commit();
			conn.setAutoCommit(true);

			// now attach the main DB to the same connection as the DB being created to make data
			// transfer easier
			stmt.execute("attach database \"" + dbfile_ + "\" as master");

			// and turn on transactions again
			conn.setAutoCommit(false);

			PreparedStatement ps;

			// meta_data
			stmt.execute("insert into main.meta_data (tag, tag_value) values ('file_version', 'PROTEST 1.0')");
			stmt.execute("insert into main.meta_data (tag, tag_value) values ('file_type', 'annotation_batch')");
			stmt.execute("insert into main.meta_data (tag, tag_value) " +
					"select 'time_created', datetime('now')");
			stmt.execute("insert into main.meta_data (tag, tag_value) " +
					"select tag, tag_value from master.meta_data where tag in ('master_id', 'master_description')");

			ps = conn.prepareStatement("insert into main.meta_data (tag, tag_value) values ('annotator_id', ?)");
			ps.setInt(1, annotator);
			ps.execute();

			ps = conn.prepareStatement("insert into main.meta_data (tag, tag_value) " +
					"select 'annotator_name', name from master.annotators where id=?");
			ps.setInt(1, annotator);
			ps.execute();

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
					"select distinct c.* from master.corpora as c, master.pro_examples as e, master.annotation_tasks as t " +
					"where (c.id=e.srccorpus or c.id=e.tgtcorpus) and e.id=t.example and " +
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
					"select a.* from master.pro_antecedents as a, master.pro_examples as e, master.annotation_tasks as t " +
					"where t.annotator_id=? and t.task_no=? and " +
					"e.id=t.example and a.srccorpus=e.srccorpus and a.tgtcorpus=e.tgtcorpus and a.example_no=e.example_no");
			ps.setInt(1, annotator);
			ps.setInt(2, task);
			ps.execute();

			// pro_examples
			ps = conn.prepareStatement("insert into main.pro_examples " +
					"select e.* from master.pro_examples as e, master.annotation_tasks as t " +
					"where t.annotator_id=? and t.task_no=? and e.id=t.example");
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
					"select tr.* from master.translations as tr, master.annotation_tasks as t " +
					"where t.annotator_id=? and t.task_no=? and tr.example_no=t.example");
			ps.setInt(1, annotator);
			ps.setInt(2, task);
			ps.execute();

			conn.commit();
			conn.close();
		} catch(SQLException e) {
			try {
				conn.rollback();
				conn.close();
				new File(outfile).delete();
			} catch(SQLException e2) {}
			throw e;
		}
	}

	public PrecheckReport precheckAnnotationBatch(String infile) {
		PrecheckReport ret = new PrecheckReport();

		try {
			Connection conn = DriverManager.getConnection("jdbc:sqlite:" + infile);

			HashMap<String,String> mastermd = getMetadata(db_);
			HashMap<String,String> annmd = getMetadata(conn);

			if(!checkMetadata(mastermd, "file_type", "master")) {
				ret.setError("Import target must be a master DB.");
				return ret;
			}
			if(!checkMetadata(annmd, "file_type", "annotator")) {
				ret.setError("Import source must be an annotator DB.");
				return ret;
			}
			if(!checkMetadata(mastermd, annmd, "master_id")) {
				ret.setError("Corpus IDs don't match.");
				return ret;
			}

			Statement stmt = conn.createStatement();
			stmt.execute("attach database \"" + dbfile_ + "\" as master");

			ResultSet rs;

			rs = stmt.executeQuery("select count(*) from main.annotations as a, master.annotations as b " +
					"where a.example=b.example and a.annotator_id=b.annotator_id");
			rs.next();
			ret.setInstanceDuplicates(rs.getInt(1));

			rs = stmt.executeQuery("select count(distinct a.example) from main.token_annotations as a, master.token_annotations as b " +
					"where a.example=b.example and a.annotator_id=b.annotator_id");
			rs.next();
			ret.setTokenDuplicates(rs.getInt(1));

			conn.close();
		} catch(SQLException e) {
			ret.setError(e.getMessage());
		}

		return ret;
	}

	public boolean checkMetadata(HashMap<String,String> md, String tag, String expected) {
		String value = md.get(tag);
		if(md == null || !value.equals(expected))
			return false;
		else
			return true;
	}

	public boolean checkMetadata(HashMap<String,String> md1, HashMap<String,String> md2, String tag) {
		String val1 = md1.get(tag);
		String val2 = md2.get(tag);
		if(val1 == null && val2 == null)
			return true;
		else
			return val1.equals(val2);
	}

	public void importAnnotationBatch(String infile) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + infile);

		try {
			Statement stmt = conn.createStatement();

			stmt.execute("attach database \"" + dbfile_ + "\" as master");
			
			conn.setAutoCommit(false);

			stmt.execute("delete from master.annotations where id in " +
					"(select a.id from main.annotations as a, master.annotations as b " +
					"where a.example=b.example and a.annotator_id=b.annotator_id)");

			stmt.execute("delete from master.token_annotations where id in " +
					"(select a.id from main.token_annotations as a, master.token_annotations as b " +
					"where a.example=b.example and a.annotator_id=b.annotator_id)");

			stmt.execute("insert into master.annotations select * from main.annotations");

			stmt.execute("insert into master.token_annotations select * from main.token_annotations");

			conn.commit();
			conn.close();
		} catch(SQLException e) {
			try {
				conn.rollback();
			} catch(SQLException e2) {}
			throw e;
		}
	}

	private String makeInList(int[] ids) {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		sb.append(ids[0]);
		for(int i = 1; i < ids.length; i++)
			sb.append(',').append(ids[i]);
		sb.append(')');
		return sb.toString();
	}

	public static void main(String[] args) throws SQLException {
		Database db = new Database("protestsuite.db");
		PrecheckReport rep = db.precheckAnnotationBatch("tstann.db");
		System.err.println(rep.getMessage());
		if(rep.canImport())
			db.importAnnotationBatch("tstann.db");
	}
}

