update meta_data set tag_value='PROTEST 1.1' where tag='file_version';

create table tag_annotations (
	id integer not null primary key autoincrement,
	candidate integer not null,
	annotator_id integer not null,
	tag text not null);
