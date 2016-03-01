update meta_data set tag_value='PROTEST 1.1' where tag='file_version';

create table tag_annotations (
	id int not null primary key,
	candidate int not null,
	annotator_id int not null,
	tag text not null);
