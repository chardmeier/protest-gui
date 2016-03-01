package protest;

import java.beans.PropertyVetoException;

import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.jdbcdslog.ConnectionLoggingProxy;

public class Database {
	private final static String FILE_VERSION = "PROTEST 1.1";

	private DataSource db_;
	private String dbfile_;

	public Database(String dbfile) throws DatabaseException {
		ComboPooledDataSource cpds = new ComboPooledDataSource();
		try {
			cpds.setDriverClass("org.sqlite.JDBC");
			cpds.setJdbcUrl("jdbc:sqlite:" + dbfile);
			cpds.setMinPoolSize(2);
			cpds.setAcquireIncrement(2);
			cpds.setMaxPoolSize(10);
			cpds.setMaxStatements(50);
		} catch(PropertyVetoException e) {
			throw new DatabaseException("Error setting up DB connection", e);
		}
		db_ = cpds;

		dbfile_ = dbfile;

		String version = getMetadata("file_version");
		if(!version.equals(FILE_VERSION)) {
			throw new DatabaseException("File version " + version + " not supported.");
		}
	}

	public Connection getConnection() throws SQLException {
		return ConnectionLoggingProxy.wrap(db_.getConnection());
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

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select c.id as category_no, c.description as description, " +
						"a.conflict_status as conflict_status, p.example_no as example_no, " +
						"p.srccorpus as srccorpus, p.tgtcorpus as tgtcorpus " +
					"from categories as c left outer join pro_candidates as p on c.id=p.category_no " +
						"left outer join annotations as a on p.id=a.candidate " +
					whereClause + " " +
					"order by description, conflict_status, sequence_no");
			int lastcat = -1;
			AnnotationCategory catobj = null;
			while(rs.next()) {
				int id = rs.getInt("category_no");
				String cat = rs.getString("description");
				String conflict = rs.getString("conflict_status");

				if(lastcat == -1 || id != lastcat) {
					lastcat = id;
					catobj = new AnnotationCategory(id, cat);
					catlist.add(catobj);
				}

				int srccorpus = rs.getInt("srccorpus");
				int tgtcorpus = rs.getInt("tgtcorpus");
				int example_no = rs.getInt("example_no");

				if(!rs.wasNull()) {
					TestSuiteExample exmpl = new TestSuiteExample(this, srccorpus, tgtcorpus, example_no);

					if(conflict == null)
						catobj.addExample(AnnotationCategory.NEW, exmpl);
					else if(new ConflictStatus(conflict).hasConflict())
						catobj.addExample(AnnotationCategory.CONFLICT, exmpl);
					else
						catobj.addExample(AnnotationCategory.DONE, exmpl);
				}
			}
			rs.close();
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}

		return catlist;
	}

	public List<TargetCorpus> getTargetCorpora() {
		ArrayList<TargetCorpus> crplist = new ArrayList<TargetCorpus>();

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select corpora.id as id, corpora.name as name, count(*) as cnt " +
					"from corpora, pro_candidates " +
					"where corpora.id=pro_candidates.tgtcorpus group by name order by name");
			while(rs.next())
				crplist.add(new TargetCorpus(rs.getInt("id"), rs.getString("name"), rs.getInt("cnt")));
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}

		return crplist;
	}

	public int getFilteredExampleCount(int[] tgtcorpora, int[] categories) {
		if(tgtcorpora.length == 0 || categories.length == 0)
			return 0;

		Connection conn = null;
		Statement stmt = null;

		int cnt = 0;
		try {
			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery("select count(*) from pro_candidates " +
					"where tgtcorpus in " + makeInList(tgtcorpora) + " " +
					"and category_no in " + makeInList(categories));
			rs.next();
			cnt = rs.getInt(1);
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}

		return cnt;
	}

	public List<String> getTags() {
		ArrayList<String> out = new ArrayList<String>();
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct tag from tag_annotations order by tag");
			while(rs.next())
				out.add(rs.getString("tag"));
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}

		return out;
	}

	public HashMap<String,String> getMetadata() {
		HashMap<String,String> md = null;
		Connection conn = null;
		try {
			conn = getConnection();
			md = getMetadata(conn);
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			Database.close(conn);
		}
		return md;
	}

	private HashMap<String,String> getMetadata(Connection conn) {
		HashMap<String,String> metadata = new HashMap<String,String>();

		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select tag, tag_value from meta_data");
			while(rs.next())
				metadata.put(rs.getString("tag"), rs.getString("tag_value"));
		} catch(SQLException e) {
			metadata = null;
		} finally {
			Database.close(stmt);
		}

		return metadata;
	}

	public String getMetadata(String tag) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = getConnection();

			ps = conn.prepareStatement("select tag_value from meta_data where tag=?");
			ps.setString(1, tag);

			ResultSet rs = ps.executeQuery();
			if(!rs.next())
				return "";
			else
				return rs.getString("tag_value");
		} catch(SQLException e) {
			e.printStackTrace();
		} finally {
			Database.close(ps);
			Database.close(conn);
		}

		return "";
	}

	public boolean tasksetExists(String label) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = getConnection();
			ps = conn.prepareStatement("select count(*) from task_definition where taskset=?");

			ps.setString(1, label);
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1) > 0;
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			Database.close(ps);
			Database.close(conn);
		}

		return false; // to make compiler happy
	}

	public void createAnnotationTasks(String taskset, int[] tgtcorpora, int[] categories, int ntasks, int iaa) {
		Connection conn = null;
		Statement stmt = null;
		ArrayList<PreparedStatement> ps_to_close = new ArrayList<PreparedStatement>();

		try {
			conn = getConnection();
			stmt = conn.createStatement();

			conn.setAutoCommit(false);

			PreparedStatement ps_descr = conn.prepareStatement(
					"insert into task_definition (taskset, label) values (?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps_to_close.add(ps_descr);
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
				ps_descr.setString(2, Integer.toString(i + 1));
				ps_descr.execute();
				ResultSet rs = ps_descr.getGeneratedKeys();
				rs.next();
				task_ids[i] = rs.getInt(1);
			}

			ResultSet rs = stmt.executeQuery("select tgtcorpus, category_no, count(*) as cnt from pro_candidates " +
					"where tgtcorpus in " + makeInList(tgtcorpora) + " and category_no in " + makeInList(categories) + " " +
					"group by tgtcorpus, category_no order by tgtcorpus, category_no");
			HashMap<List<Integer>,Integer> counts = new HashMap<List<Integer>,Integer>();
			int totalcnt = 0;
			while(rs.next()) {
				List<Integer> key = Arrays.asList(Integer.valueOf(rs.getInt("tgtcorpus")),
						Integer.valueOf(rs.getInt("category_no")));
				int cnt = rs.getInt("cnt");
				counts.put(key, Integer.valueOf(cnt));
				totalcnt += cnt;
			}

			// There shouldn't be any such records, but let's make sure.
			stmt.execute("delete from annotation_tasks where task_no < 0");

			PreparedStatement ps_select = conn.prepareStatement(
					"insert into annotation_tasks (task_no, candidate) " +
					"select -1, id from pro_candidates where tgtcorpus=? and category_no=?");
			ps_to_close.add(ps_select);
			PreparedStatement ps_assign = conn.prepareStatement(
					"update annotation_tasks set task_no=? " + 
					"where task_no=-1 and candidate in " +
						"(select candidate from annotation_tasks where task_no=-1 order by random() limit ?)");
			ps_to_close.add(ps_assign);
			for(int corpus : tgtcorpora) {
				ps_select.setInt(1, corpus);
				for(int cat : categories) {
					ps_select.setInt(2, cat);
					ps_select.execute();

					List<Integer> key = Arrays.asList(Integer.valueOf(corpus), Integer.valueOf(cat));
					int cnt = counts.get(key).intValue();

					if(iaa > 0) {
						double prop = ((double) cnt) / ((double) totalcnt);
						int nx = Math.max(1, (int) Math.round(prop * iaa));
						ps_assign.setInt(1, iaa_id);
						ps_assign.setInt(2, nx);
						ps_assign.execute();
						cnt -= nx;
					}

					if(ntasks > 0 && cnt > 0) {
						int nx = (int) Math.ceil(((double) cnt) / ((double) ntasks));
						ps_assign.setInt(2, nx);
						for(int i = 0; i < ntasks; i++) {
							ps_assign.setInt(1, Integer.valueOf(task_ids[i]));
							ps_assign.execute();
						}
					}
				}
			}

			conn.commit();
		} catch(SQLException e) {
			if(conn != null) {
				try {
					conn.rollback();
				} catch(SQLException e2) {}
			}
			e.printStackTrace();
			System.exit(1);
		} finally {
			for(PreparedStatement ps : ps_to_close)
				Database.close(ps);
			Database.close(stmt);
			Database.close(conn);
		}
	}

	public void createAnnotationBatch(String outfile, int annotator, int[] tasks) throws SQLException {
		if(tasks.length == 0)
			return;

		new File(outfile).delete();

		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + outfile);
		Statement stmt = null;
		ArrayList<PreparedStatement> ps_to_close = new ArrayList<PreparedStatement>();

		try {
			stmt = conn.createStatement();
			conn.setAutoCommit(false);

			// copy DB schema -- totally specific to sqlite, using internal data structures!
			// The order by clause makes sure all tables are created before we start adding indices.
			// We fetch the schema from the main DB connection and execute the create statements on a
			// connection that only has the new DB attached so we don't have to manipulate them
			// to add DB identifiers.
			Statement maindb_stmt = null;
			try {
				maindb_stmt = getConnection().createStatement();
				ResultSet rs = maindb_stmt.executeQuery("select sql from sqlite_master " +
						"where tbl_name not like 'sqlite_%' and sql is not null order by type desc");
				while(rs.next())
					stmt.execute(rs.getString(1));
			} finally {
				Database.close(maindb_stmt);
			}

			// must turn off transactions now because ATTACH doesn't work within a transaction
			conn.commit();
			conn.setAutoCommit(true);

			// now attach the main DB to the same connection as the DB being created to make data
			// transfer easier
			stmt.execute("attach database \"" + dbfile_ + "\" as master");

			// and turn on transactions again
			conn.setAutoCommit(false);

			String taskInList = makeInList(tasks);
			String andTaskNo = " and t.task_no in " + taskInList + " ";

			// meta_data
			stmt.execute("insert into main.meta_data (tag, tag_value) values ('file_version', 'PROTEST 1.0')");
			stmt.execute("insert into main.meta_data (tag, tag_value) values ('file_type', 'annotation_batch')");
			stmt.execute("insert into main.meta_data (tag, tag_value) " +
					"select 'time_created', datetime('now')");
			stmt.execute("insert into main.meta_data (tag, tag_value) " +
					"select tag, tag_value from master.meta_data where tag in ('master_id', 'master_description')");

			PreparedStatement ps;

			ps = conn.prepareStatement("insert into main.meta_data (tag, tag_value) values ('annotator_id', ?)");
			ps_to_close.add(ps);
			ps.setInt(1, annotator);
			ps.execute();

			ps = conn.prepareStatement("insert into main.meta_data (tag, tag_value) " +
					"select 'annotator_name', name from master.annotators where id=?");
			ps_to_close.add(ps);
			ps.setInt(1, annotator);
			ps.execute();

			// annotation_tasks
			stmt.execute("insert into main.annotation_tasks " +
					"select * from master.annotation_tasks as t " +
					"where t.task_no in " + taskInList);

			// annotations
			stmt.execute("insert into main.annotations " +
					"select a.* from master.annotations as a, master.annotation_tasks as t " +
					"where a.candidate=t.candidate" + andTaskNo);

			// annotators
			ps = conn.prepareStatement("insert into main.annotators " +
					"select * from master.annotators where id=?");
			ps_to_close.add(ps);
			ps.setInt(1, annotator);
			ps.execute();

			// categories
			stmt.execute("insert into main.categories select * from master.categories");

			// corpora
			stmt.execute("insert into main.corpora " +
					"select distinct c.* from master.corpora as c, master.pro_candidates as e, master.annotation_tasks as t " +
					"where (c.id=e.srccorpus or c.id=e.tgtcorpus) and e.id=t.candidate" + andTaskNo);

			// documents
			stmt.execute("insert into main.documents " +
					"select d.* from master.documents as d, main.corpora as c "+
					"where d.corpus=c.id");
			
			// pro_antecedents
			stmt.execute("insert into main.pro_antecedents " +
					"select a.* from master.pro_antecedents as a, master.pro_candidates as e, master.annotation_tasks as t " +
					"where e.id=t.candidate and a.srccorpus=e.srccorpus and a.tgtcorpus=e.tgtcorpus and " +
					"a.example_no=e.example_no" + andTaskNo);

			// pro_candidates
			stmt.execute("insert into main.pro_candidates " +
					"select e.* from master.pro_candidates as e, master.annotation_tasks as t " +
					"where e.id=t.candidate" + andTaskNo);

			// sentences
			stmt.execute("insert into main.sentences " +
					"select s.* from master.sentences as s, main.corpora as c "+
					"where s.corpus=c.id");

			// token_annotations
			stmt.execute("insert into main.token_annotations " +
					"select a.* from master.token_annotations as a, master.annotation_tasks as t " +
					"where a.candidate=t.candidate" + andTaskNo);

			// translations
			stmt.execute("insert into main.translations " +
					"select tr.* from master.translations as tr, master.annotation_tasks as t, master.pro_candidates as c " +
					"where t.candidate=c.id and tr.example_no=c.example_no and tr.tgtcorpus=c.tgtcorpus " + andTaskNo);

			conn.commit();
			Database.close(conn);
		} catch(SQLException e) {
			if(conn != null) {
				try {
					conn.rollback();
					conn.close();
					new File(outfile).delete();
				} catch(SQLException e2) {}
			}
			throw e;
		} finally {
			for(PreparedStatement ps : ps_to_close)
				Database.close(ps);
			Database.close(stmt);
			Database.close(conn);
		}
	}

	public PrecheckReport precheckAnnotationBatch(String infile) {
		PrecheckReport ret = new PrecheckReport();

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = ConnectionLoggingProxy.wrap(DriverManager.getConnection("jdbc:sqlite:" + infile));

			HashMap<String,String> mastermd = getMetadata();
			HashMap<String,String> annmd = getMetadata(conn);

			if(!checkMetadata(mastermd, "file_type", "master")) {
				ret.setError("Import target must be a master DB.");
				return ret;
			}
			if(!checkMetadata(annmd, "file_type", "annotation_batch")) {
				ret.setError("Import source must be an annotator DB.");
				return ret;
			}
			if(!checkMetadata(mastermd, annmd, "master_id")) {
				ret.setError("Corpus IDs don't match.");
				return ret;
			}

			stmt = conn.createStatement();
			stmt.execute("attach database \"" + dbfile_ + "\" as master");

			ResultSet rs;

			rs = stmt.executeQuery("select count(*) from main.annotations as a, master.annotations as b " +
					"where a.candidate=b.candidate and a.annotator_id=b.annotator_id");
			rs.next();
			ret.setInstanceDuplicates(rs.getInt(1));

			rs = stmt.executeQuery("select count(distinct a.candidate) from main.token_annotations as a, master.token_annotations as b " +
					"where a.candidate=b.candidate and a.annotator_id=b.annotator_id");
			rs.next();
			ret.setTokenDuplicates(rs.getInt(1));
		} catch(SQLException e) {
			ret.setError(e.getMessage());
		} finally {
			Database.close(stmt);
			Database.close(conn);
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

	public void importAnnotationBatch(String infile) throws DatabaseException {
		Connection conn = null;
		Statement stmt = null;

		try {
			conn = ConnectionLoggingProxy.wrap(DriverManager.getConnection("jdbc:sqlite:" + infile));
			stmt = conn.createStatement();

			stmt.execute("attach database \"" + dbfile_ + "\" as master");
			
			conn.setAutoCommit(false);

			stmt.execute("delete from master.annotations where id in " +
					"(select a.id from main.annotations as a, master.annotations as b " +
					"where a.candidate=b.candidate and a.annotator_id=b.annotator_id)");

			stmt.execute("delete from master.token_annotations where id in " +
					"(select a.id from main.token_annotations as a, master.token_annotations as b " +
					"where a.candidate=b.candidate and a.annotator_id=b.annotator_id)");

			stmt.execute("delete from master.tag_annotations where id in " +
					"(select a.id from main.tag_annotations as a, master.tag_annotations as b " +
					"where a.candidate=b.candidate and a.annotator_id=b.annotator_id)");

			stmt.execute("insert into master.annotations " +
					"(candidate, annotator_id, ant_annotation, anaph_annotation, remarks, confict_status) " +
					"select candidate, annotator_id, ant_annotation, anaph_annotation, remarks, conflict_status from main.annotations");

			stmt.execute("insert into master.token_annotations " +
					"(candidate, line, token, annotation, annotator_id) " +
					"select candidate, line, token, annotation, annotator_id from main.token_annotations");

			stmt.execute("insert into master.tag_annotations (candidate, annotator_id, tag) " +
					"select candidate, annotator_id, tag from main.tag_annotations");

			conn.commit();
		} catch(SQLException e) {
			try {
				conn.rollback();
			} catch(SQLException e2) {}
			throw new DatabaseException(e);
		} finally {
			Database.close(stmt);
			Database.close(conn);
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

	public static void close(Connection conn) {
		try {
			if(conn != null)
				conn.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}

	public static void close(Statement conn) {
		try {
			if(conn != null)
				conn.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws DatabaseException, SQLException {
		Database db = new Database(args[0]);
		db.createAnnotationBatch(args[0] + ".batchMiryam", 1, new int[] { 1 });
		db.createAnnotationBatch(args[0] + ".batchMarie", 2, new int[] { 1 });
	}
}

