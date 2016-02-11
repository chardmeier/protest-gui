ALTER TABLE pro_examples ADD COLUMN sequence_no integer;

DROP TABLE IF EXISTS annotators;
CREATE TABLE annotators (
	id integer primary key autoincrement,
	name text not null
);

ALTER TABLE annotations ADD COLUMN annotator_id integer;
ALTER TABLE annotations ADD COLUMN conflict_status text;

ALTER TABLE token_annotations ADD COLUMN annotator_id integer;

DROP TABLE IF EXISTS meta_data;
CREATE TABLE meta_data (
	id integer primary key autoincrement,
	tag text not null,
	tag_value text not null
);

INSERT INTO meta_data (tag,tag_value) VALUES ("file_type","MasterDB");
INSERT INTO meta_data (tag,tag_value) VALUES ("master_id","DiscoMT2015");

DROP TABLE IF EXISTS annotation_tasks;
CREATE TABLE annotation_tasks (
	id integer primary key autoincrement,
	task_no integer not null,
	annotator_id integer not null,
	example integer not null
);

