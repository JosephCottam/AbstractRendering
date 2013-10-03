# Re-encode text files as hbin files in the data directory
# By default, all of these are commented out because usually only one or two need to be re-run
# Assumes the core jar file exists

 # java -cp ../AbstractRendering/AR.jar ar.util.MemMapEncoder -in ../data/circlepoints.csv -out ../data/circlepoints.hbin -skip 1 -types xxddi -direct true
 # java -cp ../AbstractRendering/AR.jar ar.util.MemMapEncoder -in ../data/wiki-adj.csv -out ../data/wiki-adj.hbin -skip 0 -types llx -direct true
 # java -cp ../AbstractRendering/AR.jar ar.util.MemMapEncoder -in ../data/kiva.txt -out ../data/kiva.hbin -skip 1 -types illld -direct true
 # java -cp ../AbstractRendering/AR.jar ar.util.MemMapEncoder -in ../data/kiva.txt -out ../data/kiva-adj.hbin -skip 1 -types xllxx -direct true
 # java -cp ../AbstractRendering/AR.jar ar.util.MemMapEncoder -in ../data/MemVisScaled.csv -out ../data/MemVisScaled.hbin -skip 0 -types dds -direct true
 # java -cp ../AbstractRendering/AR.jar ar.util.MemMapEncoder -in ../data/CharityNet-DateStateXY.csv -out ../data/CharityNet-DateStateXY.hbin -skip 1 -types ii -direct true
 # java -cp ../AbstractRendering/AR.jar ar.util.MemMapEncoder -in ../data/census/RaceTractDenorm.csv -out ../data/census/RaceTractDenorm.hbin -skip 1 -types ddii -direct true
