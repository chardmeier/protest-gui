update meta_data set tag_value='PROTEST 1.1' where tag='file_version';

create table tag_annotations (
	candidate int not null,
	annotator_id int not null,
	tag text not null,
	primary key (candidate, annotator_id, tag));

