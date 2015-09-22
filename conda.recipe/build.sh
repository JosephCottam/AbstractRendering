mkdir -vp ${PREFIX}/bin;
mkdir -vp ${PREFIX}/lib/abstractrendering-java/data;
mkdir -vp ${PREFIX}/lib/abstractrendering-java/java;

pushd java
ant fetch-ext ext
popd

cp -r data/* ${PREFIX}/lib/abstractrendering-java/data/;
cp -r java/* ${PREFIX}/lib/abstractrendering-java/java/;
cp ${RECIPE_DIR}/abstractrendering-java ${PREFIX}/lib/abstractrendering-java/java/

chmod +x ${PREFIX}/lib/abstractrendering-java/java/abstractrendering-java

pushd "${PREFIX}/bin"
ln -vs "../lib/abstractrendering-java/java/abstractrendering-java" abstractrendering-java
