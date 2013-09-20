# Re-encode text files as hbin files in the data directory
# By default, all of these are commented out because usually only one or two need to be re-run
# Assumes the core jar file exists

# java -cp ../AbstractRendering/AR.jar ar.util.MemMapEncoder -in ../data/circlepoints.csv -out ../data/circlepoints.hbin -skip 1 -types xxddi -direct true
# java -cp ../AbstractRendering/AR.jar ar.util.MemMapEncoder -in ../data/wiki.full.txt -out ../data/wiki.full.hbin -skip 0 -types llx -direct true
