#! /bin/bash

if [ $# -ne 2 ]
then
	echo "Usage: $0 masterdb.in filtered.out" 1>&2
	exit 1
fi
masterdb=$1
filtered=$2

if [ -a $filtered ]
then
	echo "$filtered already exists." 1>&2
	exit 1
fi

cp $masterdb $filtered

sqlite3 $filtered <<EOF
-- Annotator 1 and 2 are the original annotations, annotator 3 is the adjudicated
-- annotation of those examples where annotator 1 and 2 disagreed.
-- Where both annotators agree in their main annotations, we arbitrarily take the
-- tag and token annotations from annotator 2 and delete the annotations of
-- annotator 1, so the highest-numbered annotator always prevails.

delete from annotations where id in (
	select a.id from annotations as a, annotations as b
	where a.annotator_id<b.annotator_id and a.candidate=b.candidate);

-- At this point, the annotations table only contains annotations of a single
-- annotator per example, and we just remove tag and token annotations of
-- all other annotators.

delete from token_annotations where id in (
	select t.id from token_annotations as t, annotations as a
	where t.candidate=a.candidate and t.annotator_id!=a.annotator_id);
delete from tag_annotations where id in (
	select t.id from tag_annotations as t, annotations as a
	where t.candidate=a.candidate and t.annotator_id!=a.annotator_id);

-- Corpus 3/4 is a duplicate of 1/2. Corpus 7/8 is the A3-108 system that should be excluded.
-- The DB doesn't contain annotations corresponding to either of these systems.

delete from pro_candidates where srccorpus in (3, 7);
EOF
